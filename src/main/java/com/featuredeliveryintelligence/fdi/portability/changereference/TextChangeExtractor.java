package com.featuredeliveryintelligence.fdi.portability.changereference;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Bounded text excerpt extraction (SF-BL-004 Task 2). Parses Git unified hunks and
 *  verifies reconstruction against the bound blobs before emitting excerpts. Context
 *  lines are deterministic and bounded; enclosing Markdown headings and conservative
 *  Java declarations are detected mechanically without semantic claims. Invalid UTF-8
 *  and metadata-only inputs produce no excerpt; added files are complete only below
 *  the byte limit and otherwise fall back to hunks marked truncated. Modified and
 *  deleted files are represented by changed hunks only, never whole files. */
final class TextChangeExtractor {
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s+\\S.*");
    private static final Pattern JAVA_DECLARATION = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+|final\\s+|abstract\\s+|static\\s+)*"
                    + "(?:class|interface|enum|record)\\s+[A-Za-z0-9_]+.*$");

    private final int contextLines;
    private final int maxTextBytes;

    TextChangeExtractor(int contextLines, int maxTextBytes) {
        if (contextLines < 0 || maxTextBytes <= 0) {
            throw new IllegalArgumentException("contextLines >= 0 and maxTextBytes > 0 required");
        }
        this.contextLines = contextLines;
        this.maxTextBytes = maxTextBytes;
    }

    List<ChangeExcerpt> extract(ChangedPath change) {
        List<String> oldLines = decode(change.oldBytes());
        List<String> newLines = decode(change.newBytes());
        if (change.operation() == Operation.ADD) {
            if (newLines == null) {
                return List.of();
            }
            return change.newBytes().length <= maxTextBytes
                    ? List.of(wholeFile(change, newLines)) : hunks(change, List.of(), newLines, true);
        }
        if (oldLines == null) {
            return List.of();
        }
        return hunks(change, oldLines, newLines == null ? List.of() : newLines, false);
    }

    private ChangeExcerpt wholeFile(ChangedPath change, List<String> newLines) {
        List<String> diff = new ArrayList<>();
        for (String line : change.unifiedDiff()) {
            if (!line.startsWith("\\")) {
                diff.add(line);
            }
        }
        return new ChangeExcerpt(0, 0, 1, newLines.size(), context(change, newLines, newLines.size() - 1),
                List.of(), List.of(), List.copyOf(diff), false);
    }

    private List<ChangeExcerpt> hunks(ChangedPath change, List<String> oldLines, List<String> newLines, boolean truncated) {
        List<Hunk> parsed = parseHunks(change.unifiedDiff());
        if (parsed.isEmpty()) {
            return List.of();
        }
        verify(parsed, oldLines, newLines, change);
        List<ChangeExcerpt> excerpts = new ArrayList<>();
        for (Hunk hunk : parsed) {
            List<String> before = new ArrayList<>();
            List<String> after = new ArrayList<>();
            List<String> body = hunk.lines();
            int first = 0;
            int last = body.size();
            while (first < last && body.get(first).startsWith(" ")) {
                if (before.size() < contextLines) {
                    before.add(body.get(first).substring(1));
                }
                first++;
            }
            while (last > first && body.get(last - 1).startsWith(" ")) {
                if (after.size() < contextLines) {
                    after.add(0, body.get(last - 1).substring(1));
                }
                last--;
            }
            int removed = 0;
            int added = 0;
            for (int i = first; i < last; i++) {
                if (body.get(i).charAt(0) == '-') {
                    removed++;
                } else if (body.get(i).charAt(0) == '+') {
                    added++;
                }
            }
            int[] oldRange = range(hunk.oldStart() + first, removed);
            int[] newRange = range(hunk.newStart() + first, added);
            List<String> enclosing = added > 0 ? newLines : oldLines;
            int enclosingStart = added > 0 ? newRange[0] : oldRange[0];
            excerpts.add(new ChangeExcerpt(oldRange[0], oldRange[1], newRange[0], newRange[1],
                    context(change, enclosing, enclosingStart - 1),
                    List.copyOf(before), List.copyOf(after), List.copyOf(body), truncated));
        }
        return List.copyOf(excerpts);
    }

    private String context(ChangedPath change, List<String> lines, int aboveIndex) {
        String path = change.newPath() == null ? change.oldPath() : change.newPath();
        Pattern marker = path.endsWith(".md") ? MARKDOWN_HEADING : path.endsWith(".java") ? JAVA_DECLARATION : null;
        if (marker == null) {
            return null;
        }
        for (int i = Math.min(aboveIndex, lines.size() - 1); i >= 0; i--) {
            String line = lines.get(i).trim();
            if (marker.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    private static int[] range(int start, int count) {
        return count == 0 ? new int[]{0, 0} : new int[]{start, start + count - 1};
    }

    private static List<Hunk> parseHunks(List<String> diff) {
        List<Hunk> hunks = new ArrayList<>();
        Hunk current = null;
        for (String line : diff) {
            if (line.startsWith("@@ ")) {
                current = header(line);
                hunks.add(current);
            } else if (line.startsWith("\\")) {
                continue;
            } else if (current != null) {
                if (!line.startsWith(" ") && !line.startsWith("+") && !line.startsWith("-")) {
                    throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "malformed hunk line");
                }
                current.lines().add(line);
            }
        }
        return hunks;
    }

    private static Hunk header(String line) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@").matcher(line);
        if (!m.find()) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "malformed hunk header");
        }
        return new Hunk(Integer.parseInt(m.group(1)), m.group(2) == null ? 1 : Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), m.group(4) == null ? 1 : Integer.parseInt(m.group(4)),
                new ArrayList<>());
    }

    private static void verify(List<Hunk> hunks, List<String> oldLines, List<String> newLines, ChangedPath change) {
        List<String> rebuilt = new ArrayList<>();
        int oldIndex = 0;
        int newIndex = 0;
        try {
            for (Hunk hunk : hunks) {
                while (oldIndex < hunk.oldStart() - 1) {
                    rebuilt.add(oldLines.get(oldIndex));
                    oldIndex++;
                    newIndex++;
                }
                for (String line : hunk.lines()) {
                    char kind = line.charAt(0);
                    String text = line.substring(1);
                    if (kind == ' ') {
                        require(oldLines.get(oldIndex).equals(text) && newLines.get(newIndex).equals(text), change);
                        rebuilt.add(text);
                        oldIndex++;
                        newIndex++;
                    } else if (kind == '-') {
                        require(oldLines.get(oldIndex).equals(text), change);
                        oldIndex++;
                    } else {
                        require(newLines.get(newIndex).equals(text), change);
                        rebuilt.add(text);
                        newIndex++;
                    }
                }
            }
            while (oldIndex < oldLines.size()) {
                rebuilt.add(oldLines.get(oldIndex));
                oldIndex++;
                newIndex++;
            }
        } catch (IndexOutOfBoundsException e) {
            throw inconsistent(change);
        }
        if (!rebuilt.equals(newLines)) {
            throw inconsistent(change);
        }
    }

    private static void require(boolean condition, ChangedPath change) {
        if (!condition) {
            throw inconsistent(change);
        }
    }

    private static GitChangeException inconsistent(ChangedPath change) {
        String path = change.newPath() == null ? change.oldPath() : change.newPath();
        return new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "hunks do not reconstruct " + path);
    }

    private static List<String> decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString().lines().toList();
        } catch (java.nio.charset.CharacterCodingException e) {
            return null;
        }
    }

    private record Hunk(int oldStart, int oldCount, int newStart, int newCount, List<String> lines) {
    }
}

/** Immutable bounded excerpt of one changed region. Positions are 1-based inclusive line
 *  ranges; a zero count is reported as 0,0. {@code context} holds a mechanically detected
 *  enclosing Markdown heading, conservative Java declaration, JSON Pointer hint, or
 *  configuration key, and is null when none applies. */
record ChangeExcerpt(int oldStart, int oldEnd, int newStart, int newEnd,
                     String context, List<String> before, List<String> after,
                     List<String> unifiedDiff, boolean truncated) {
}
