package com.featuredeliveryintelligence.fdi.portability.changereference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Exact committed change source (SF-BL-004 Task 1). Argument-vector Git only: no shell,
 *  15s timeout, capped output, --end-of-options, NUL path parsing. Failures are stable
 *  {@link FailureCode}s with sanitized diagnostics (codes and arguments, never content). */
final class GitChangeSource {
    private final String gitExecutable;
    private final Duration timeout;
    private final int maxOutputBytes;

    GitChangeSource() {
        this("git", Duration.ofSeconds(15), 1 << 20);
    }

    GitChangeSource(String gitExecutable, Duration timeout, int maxOutputBytes) {
        this.gitExecutable = gitExecutable;
        this.timeout = timeout;
        this.maxOutputBytes = maxOutputBytes;
    }

    List<ChangedPath> changes(GitRange range, int contextLines) {
        Path repository = range.repository();
        if (contextLines < 0 || !Files.isDirectory(repository)) {
            throw new GitChangeException(FailureCode.GIT_COMMAND_FAILED, "repository unavailable");
        }        String from = resolveCommit(repository, range.fromRevision());
        String to = resolveCommit(repository, range.toRevision());
        int ancestry = invoke(repository, "merge-base", "--is-ancestor", "--end-of-options", from, to).exit();
        if (ancestry != 0) {
            throw new GitChangeException(ancestry == 1 ? FailureCode.ANCESTRY_INVALID : FailureCode.GIT_COMMAND_FAILED,
                    "merge-base --is-ancestor exited " + ancestry);
        }
        List<ChangedPath> changes = new ArrayList<>();
        for (RawChange raw : parseRaw(invoke(repository,
                "diff-tree", "--raw", "-r", "-z", "-M", "--no-commit-id", "--end-of-options", from, to).stdout())) {
            changes.add(materialize(repository, from, to, raw, contextLines));
        }
        return List.copyOf(changes);
    }

    private String resolveCommit(Path repository, String revision) {
        if (!revision.matches("[0-9a-fA-F]{40}")) {
            throw new GitChangeException(FailureCode.REVISION_INVALID, "revision must be a full 40-character commit ID");
        }
        Invocation resolved = invoke(repository, "rev-parse", "--verify", "--end-of-options", revision + "^{commit}");
        String fullId = new String(resolved.stdout(), StandardCharsets.UTF_8).trim().toLowerCase(java.util.Locale.ROOT);
        if (resolved.exit() != 0 || !fullId.matches("[0-9a-f]{40}")) {
            throw new GitChangeException(FailureCode.REVISION_INVALID, "revision does not name a commit");
        }
        return fullId;
    }

    private ChangedPath materialize(Path repository, String from, String to, RawChange raw, int contextLines) {
        List<String> args = new ArrayList<>(List.of("diff", "--no-ext-diff", "--no-textconv", "-M",
                "--unified=" + contextLines, "--end-of-options", from, to, "--"));
        args.addAll(raw.operation() == Operation.RENAME ? List.of(raw.oldPath(), raw.newPath())
                : List.of(raw.newPath() == null ? raw.oldPath() : raw.newPath()));
        byte[] diff = invoke(repository, args.toArray(String[]::new)).stdout();
        return new ChangedPath(raw.oldPath(), raw.newPath(), raw.operation(), absent(raw.oldBlob()) ? null : raw.oldBlob(),
                absent(raw.newBlob()) ? null : raw.newBlob(), readBlob(repository, raw.oldBlob()),
                readBlob(repository, raw.newBlob()), new String(diff, StandardCharsets.UTF_8).lines().toList());
    }

    private byte[] readBlob(Path repository, String blob) {
        return blob == null || blob.chars().allMatch(c -> c == '0')
                ? null : invoke(repository, "cat-file", "blob", blob).stdout();
    }

    private static boolean absent(String blob) {
        return blob == null || blob.chars().allMatch(c -> c == '0');
    }

    private record RawChange(String oldPath, String newPath, Operation operation, String oldBlob, String newBlob) {
    }

    private static List<RawChange> parseRaw(byte[] output) {
        List<String> fields = new ArrayList<>();
        for (int start = 0, i = 0; i < output.length; i++) {
            if (output[i] == 0) {
                fields.add(new String(output, start, i - start, StandardCharsets.UTF_8));
                start = i + 1;
            }
        }
        List<RawChange> changes = new ArrayList<>();
        for (int i = 0; i < fields.size(); ) {
            String[] meta = fields.get(i++).split(" ");
            String status = meta[4];
            if (status.startsWith("R")) {
                changes.add(new RawChange(fields.get(i), fields.get(i + 1), Operation.RENAME, meta[2], meta[3]));
                i += 2;
                continue;
            }
            Operation operation = status.charAt(0) == 'A' ? Operation.ADD
                    : status.charAt(0) == 'D' ? Operation.DELETE : Operation.MODIFY;
            String path = fields.get(i++);
            changes.add(new RawChange(operation == Operation.ADD ? null : path,
                    operation == Operation.DELETE ? null : path, operation, meta[2], meta[3]));
        }
        return changes;
    }

    private record Invocation(int exit, byte[] stdout) {
    }

    private static final class Pump {
        Thread thread;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOException failure;
    }

    private Invocation invoke(Path repository, String... args) {
        List<String> command = new ArrayList<>(List.of(gitExecutable, "-C", repository.toString()));
        command.addAll(List.of(args));
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new GitChangeException(FailureCode.GIT_COMMAND_FAILED, ("git " + String.join(" ", args)) + " failed to start");
        }
        Pump stdout = pump(process.getInputStream());
        Pump stderr = pump(process.getErrorStream());
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new GitChangeException(FailureCode.GIT_COMMAND_FAILED, ("git " + String.join(" ", args)) + " timed out");
            }
            for (Pump pump : new Pump[]{stdout, stderr}) {
                pump.thread.join();
                if (pump.failure != null) {
                    FailureCode code = "output-limit-exceeded".equals(pump.failure.getMessage())
                            ? FailureCode.INPUT_LIMIT_EXCEEDED : FailureCode.GIT_COMMAND_FAILED;
                    throw new GitChangeException(code, ("git " + String.join(" ", args)) + " output was not bounded");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new GitChangeException(FailureCode.GIT_COMMAND_FAILED, ("git " + String.join(" ", args)) + " interrupted");
        }
        return new Invocation(process.exitValue(), stdout.buffer.toByteArray());
    }

    private Pump pump(InputStream in) {
        Pump pump = new Pump();
        pump.thread = new Thread(() -> {
            byte[] chunk = new byte[8192];
            try {
                for (int n; (n = in.read(chunk)) != -1; ) {
                    if (pump.buffer.size() + n > maxOutputBytes) {
                        throw new IOException("output-limit-exceeded");
                    }
                    pump.buffer.write(chunk, 0, n);
                }
            } catch (IOException e) {
                pump.failure = e;
            }
        });
        pump.thread.setDaemon(true);
        pump.thread.start();
        return pump;
    }
}

record GitRange(Path repository, String fromRevision, String toRevision) {
}

record ChangedPath(String oldPath, String newPath, Operation operation,
                   String oldBlobId, String newBlobId, byte[] oldBytes,
                   byte[] newBytes, List<String> unifiedDiff) {
}

enum Operation {
    ADD, MODIFY, DELETE, RENAME
}

enum FailureCode {
    REVISION_INVALID, ANCESTRY_INVALID, WORKTREE_DIRTY, PATH_UNSAFE, SECRET_DETECTED,
    INPUT_LIMIT_EXCEEDED, GIT_COMMAND_FAILED, OUTPUT_EXISTS, PACKAGE_VALIDATION_FAILED
}

final class GitChangeException extends RuntimeException {
    private final FailureCode code;

    GitChangeException(FailureCode code, String detail) { super(code.name() + " " + detail); this.code = code; }

    FailureCode code() { return code; }
}