package com.featuredeliveryintelligence.fdi.validation.acquisition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Read-only validator for an immutable PKB-001 calibration tree. Ports the
 * observable behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_acquisition.py}: bounded repository-relative
 * retained paths (rejecting absolute paths, {@code ..} traversal, {@code .git/}
 * prefixes, symlinks, directories, and missing files), the retained-tree
 * SHA-256 ({@code relative\0length\0content-digest} in sorted-path order),
 * ISO-8601 timestamp checks with mandatory timezones, the manifest field and
 * {@code EXCLUDE_AFTER_CUTOFF} policy checks, the frozen positive-integer size
 * limits, the NUL/binary and credential-pattern content checks, and the frozen
 * tree-digest comparison. {@link IllegalArgumentException} mirrors the Python
 * {@code ValueError} vocabulary; a hostile non-object manifest raises
 * {@link IllegalStateException} like the Python consumer's uncaught
 * {@code AttributeError}. The tree is never modified.
 */
public final class AcquisitionValidator {
    private static final Pattern GIT_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(api[_-]?key|password|secret|access[_-]?token)\\s*[:=]\\s*[^\\s]{8,}",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> REQUIRED_FIELDS =
            List.of("acquisition_method", "history_source", "license");
    private static final List<String> LIMIT_FIELDS =
            List.of("max_repository_bytes", "max_file_count", "max_file_bytes");
    private static final DateTimeFormatter LOCAL_TIMESTAMP = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendPattern("['T'][' ']HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            // Decimal fraction (1-9 digits) with an optional '.' separator is
            // NOT wrapped in optionalStart/optionalEnd: a second optional
            // section directly after the [:ss] one is skipped when the first
            // matched, so fractional seconds never engaged. With
            // decimalPoint=true and minWidth 0 the fraction is optional and
            // engages whether or not the optional seconds were present.
            .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();

    private record Entry(String relative, Path path) { }

    /** Ports {@code tree_sha256(root, retained_paths)}. */
    public String treeSha256(Path root, List<String> retainedPaths) {
        MessageDigest digest = sha256();
        for (Entry entry : entries(root, retainedPaths)) {
            byte[] content = readBytes(entry.path());
            digest.update(entry.relative().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Integer.toString(content.length).getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) 0);
            digest.update(sha256().digest(content));
        }
        return toHex(digest.digest());
    }

    /** Ports {@code validate_acquisition(root, manifest)}; the manifest must be a JSON object. */
    public ObjectNode validateAcquisition(Path root, JsonNode manifestNode) {
        if (manifestNode == null || !manifestNode.isObject()) {
            throw new IllegalStateException("manifest is not a JSON object");
        }
        ObjectNode manifest = (ObjectNode) manifestNode;

        JsonNode revisionNode = manifest.get("source_commit_sha");
        if (revisionNode == null || !revisionNode.isTextual()
                || !GIT_SHA.matcher(revisionNode.asText()).matches()) {
            throw new IllegalArgumentException(
                    "source_commit_sha must be a lowercase 40-character Git SHA");
        }
        String revision = revisionNode.asText();

        JsonNode retainedNode = manifest.get("retained_paths");
        if (retainedNode == null || !retainedNode.isArray()) {
            throw new IllegalArgumentException("retained_paths must be a string array");
        }
        List<String> retained = new ArrayList<>();
        for (JsonNode item : retainedNode) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("retained_paths must be a string array");
            }
            retained.add(item.asText());
        }
        List<Entry> entries = entries(root, retained);

        for (String field : REQUIRED_FIELDS) {
            JsonNode value = manifest.get(field);
            if (value == null || !value.isTextual() || value.asText().trim().isEmpty()) {
                throw new IllegalArgumentException(field + " is required");
            }
        }
        requireTimestamp(manifest.get("acquired_at"), "acquired_at");
        requireTimestamp(manifest.get("history_cutoff"), "history_cutoff");
        JsonNode policy = manifest.get("post_cutoff_knowledge_policy");
        if (policy == null || !policy.isTextual()
                || !policy.asText().equals("EXCLUDE_AFTER_CUTOFF")) {
            throw new IllegalArgumentException(
                    "post-cutoff knowledge policy must be EXCLUDE_AFTER_CUTOFF");
        }

        long[] limits = new long[LIMIT_FIELDS.size()];
        for (int index = 0; index < LIMIT_FIELDS.size(); index++) {
            String field = LIMIT_FIELDS.get(index);
            JsonNode value = manifest.get(field);
            if (value == null || value.isBoolean() || !value.isIntegralNumber()
                    || value.longValue() < 1) {
                throw new IllegalArgumentException(field + " must be a positive integer");
            }
            limits[index] = value.longValue();
        }
        long maxRepositoryBytes = limits[0];
        long maxFileCount = limits[1];
        long maxFileBytes = limits[2];

        if (entries.size() > maxFileCount) {
            throw new IllegalArgumentException("file count exceeds frozen limit");
        }
        long total = 0;
        for (Entry entry : entries) {
            byte[] content = readBytes(entry.path());
            if (content.length > maxFileBytes) {
                throw new IllegalArgumentException("file exceeds frozen limit: " + entry.relative());
            }
            if (containsNul(content)) {
                throw new IllegalArgumentException(
                        "binary retained content is prohibited: " + entry.relative());
            }
            if (CREDENTIAL.matcher(new String(content, StandardCharsets.ISO_8859_1)).find()) {
                throw new IllegalArgumentException("credential pattern found: " + entry.relative());
            }
            total += content.length;
        }
        if (total > maxRepositoryBytes) {
            throw new IllegalArgumentException("repository bytes exceed frozen limit");
        }

        JsonNode expectedNode = manifest.get("source_tree_sha256");
        String actual = treeSha256(root, retained);
        if (expectedNode == null || !expectedNode.isTextual()
                || !SHA256.matcher(expectedNode.asText()).matches()
                || !expectedNode.asText().equals(actual)) {
            throw new IllegalArgumentException("source tree digest mismatch");
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("status", "VALIDATED");
        result.put("source_commit_sha", revision);
        result.put("source_tree_sha256", actual);
        result.put("file_count", entries.size());
        result.put("repository_bytes", total);
        return result;
    }

    // ------------------------------------------------------------------
    // Bounded relative paths
    // ------------------------------------------------------------------

    /** Ports {@code _entries}: sorted unique retained paths resolved to safe files. */
    private static List<Entry> entries(Path root, List<String> retainedPaths) {
        List<String> sorted = new ArrayList<>(retainedPaths);
        java.util.Collections.sort(sorted);
        if (sorted.isEmpty() || new LinkedHashSet<>(sorted).size() != sorted.size()) {
            throw new IllegalArgumentException("retained paths must be non-empty and unique");
        }
        List<Entry> entries = new ArrayList<>();
        for (String relative : sorted) {
            entries.add(new Entry(relative, relativeFile(root, relative)));
        }
        return entries;
    }

    /** Ports {@code _relative_file}: bounded, symlink-free, regular file inside {@code root}. */
    private static Path relativeFile(Path root, String relative) {
        Path parsed;
        try {
            parsed = Paths.get(relative);
        } catch (InvalidPathException invalid) {
            throw unsafe(relative);
        }
        if (parsed.isAbsolute()) {
            throw unsafe(relative);
        }
        for (Path part : parsed) {
            if (part.toString().equals("..")) {
                throw unsafe(relative);
            }
        }
        if (relative.startsWith(".git/")) {
            throw unsafe(relative);
        }
        Path rootReal = realPath(root);
        Path candidate = rootReal.resolve(parsed.normalize());
        if (!Files.exists(candidate) || Files.isSymbolicLink(candidate)) {
            throw unsafe(relative);
        }
        Path resolved = realPath(candidate);
        if (!resolved.startsWith(rootReal) || !Files.isRegularFile(resolved)) {
            throw unsafe(relative);
        }
        return resolved;
    }

    private static IllegalArgumentException unsafe(String relative) {
        return new IllegalArgumentException("unsafe retained path: " + relative);
    }

    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    // ------------------------------------------------------------------
    // Timestamps
    // ------------------------------------------------------------------

    /** Ports {@code _timestamp}: ISO-8601 with mandatory timezone, after Python {@code 'Z'} replacement. */
    private static void requireTimestamp(JsonNode value, String field) {
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String text = value.asText().replace("Z", "+00:00");
        java.text.ParsePosition position = new java.text.ParsePosition(0);
        java.time.temporal.TemporalAccessor parsed = LOCAL_TIMESTAMP.parseUnresolved(text, position);
        if (parsed == null || position.getErrorIndex() != -1) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 timestamp");
        }
        String remainder = text.substring(position.getIndex());
        if (remainder.isEmpty()) {
            throw new IllegalArgumentException(field + " must include a timezone");
        }
        try {
            java.time.ZoneOffset.of(remainder);
        } catch (java.time.DateTimeException failure) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 timestamp");
        }
    }

    // ------------------------------------------------------------------
    // Bytes and digests
    // ------------------------------------------------------------------

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static boolean containsNul(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String toHex(byte[] digest) {
        StringBuilder out = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            out.append(Character.forDigit((value >> 4) & 0x0f, 16));
            out.append(Character.forDigit(value & 0x0f, 16));
        }
        return out.toString();
    }
}
