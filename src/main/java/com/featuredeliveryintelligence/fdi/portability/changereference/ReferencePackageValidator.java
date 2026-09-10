package com.featuredeliveryintelligence.fdi.portability.changereference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Canonical package validator (SF-BL-004 Task 3). Recomputes every digest, checks
 *  record/path bijection, manifest/checksum exclusion from the non-circular
 *  packageFiles list, authority markers, truncation declarations, safe relative
 *  paths, canonical ordering, UTF-8/LF content, and the detached manifest checksum
 *  before publication. Any violation fails closed with
 *  {@link FailureCode#PACKAGE_VALIDATION_FAILED}. */
class ReferencePackageValidator {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final List<String> MANIFEST_KEYS = List.of("schemaVersion", "authority",
            "automaticApplicationAllowed", "sharedBaseline", "sourceRepositoryIdentity", "fromRevision",
            "toRevision", "generationMethod", "generatorVersion", "generatedAt", "records", "excludedRecords",
            "packageFiles");
    private static final Set<String> MARKERS = Set.of("REFERENCE_ONLY", "DO_NOT_APPLY_BLINDLY", "NO_SHARED_BASELINE");

    void validate(Path directory) {
        try {
            Path root = directory.toAbsolutePath().normalize();
            JsonNode manifest = MAPPER.readTree(lfUtf8(root.resolve("manifest.json")));
            List<Path> contentFiles = contentFiles(root);
            checkManifest(manifest, root);
            checkRecords(manifest.get("records"), root);
            checkExcluded(manifest.get("excludedRecords"));
            checkPackageFiles(manifest.get("packageFiles"), root, contentFiles);
            checkMarkers(root);
        } catch (GitChangeException e) {
            throw e;
        } catch (Exception e) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "package is unreadable or unsafe");
        }
    }

    private static void checkManifest(JsonNode manifest, Path root) throws IOException {
        List<String> keys = new ArrayList<>();
        manifest.fields().forEachRemaining(entry -> keys.add(entry.getKey()));
        require(keys.equals(MANIFEST_KEYS), "manifest field order is not canonical");
        require("REFERENCE_ONLY".equals(manifest.get("authority").asText()), "authority must be REFERENCE_ONLY");
        require(!manifest.get("automaticApplicationAllowed").asBoolean(), "automatic application must be false");
        require("NO_SHARED_BASELINE".equals(manifest.get("sharedBaseline").asText()),
                "shared baseline must be NO_SHARED_BASELINE");
        require(SAFE_NAME.matcher(manifest.get("sourceRepositoryIdentity").asText()).matches(),
                "source repository identity is unsafe");
        require(!manifest.get("records").isNull() && !manifest.get("excludedRecords").isNull()
                && !manifest.get("packageFiles").isNull(), "manifest record arrays are missing");
        String checksum = ReferencePackageValidator.sha256(Files.readAllBytes(root.resolve("manifest.json")))
                + "  manifest.json\n";
        require(checksum.equals(lfUtf8(root.resolve("manifest.sha256"))), "detached manifest checksum mismatch");
    }

    private static void checkRecords(JsonNode records, Path root) throws IOException {
        require(records.isArray(), "records must be an array");
        String previous = null;
        int index = 0;
        TreeSet<String> rendered = new TreeSet<>();
        for (JsonNode record : records) {
            require(record.get("recordId").asText().equals("CR-" + String.format("%04d", ++index)),
                    "record IDs are not sequential");
            String path = record.get("path").asText();
            require(safeRelative(path), "unsafe record path");
            require(previous == null || previous.compareTo(path) <= 0, "records are not canonically ordered");
            previous = path;
            Path file = root.resolve("changes").resolve(record.get("recordId").asText() + ".md");
            require(Files.isRegularFile(file), "record file missing for " + record.get("recordId").asText());
            String content = lfUtf8(file);
            for (String marker : MARKERS) {
                require(content.contains(marker), "missing authority marker in " + file.getFileName());
            }
            require(record.get("digest").asText().equals(sha256(Files.readAllBytes(file))), "record digest mismatch");
            boolean truncated = false;
            for (JsonNode excerpt : record.get("excerpts")) {
                truncated |= excerpt.get("truncated").asBoolean();
            }
            require(record.get("truncated").asBoolean() == truncated, "truncation declaration mismatch");
            rendered.add(file.getFileName().toString());
        }
        Path changes = root.resolve("changes");
        require(Files.isDirectory(changes), "changes directory is missing");
        try (var stream = Files.list(changes)) {
            require(stream.map(entry -> entry.getFileName().toString()).sorted().toList()
                    .equals(List.copyOf(rendered)), "record/path bijection broken");
        }
    }

    private static void checkExcluded(JsonNode excluded) {
        require(excluded.isArray(), "excludedRecords must be an array");
        for (JsonNode record : excluded) {
            require(safeRelative(record.get("path").asText()), "unsafe excluded path");
            require(!record.get("reason").asText().isBlank(), "missing exclusion reason");
        }
    }

    private static void checkPackageFiles(JsonNode files, Path root, List<Path> contentFiles) throws IOException {
        require(files.isArray() && files.size() == contentFiles.size(), "package file binding count mismatch");
        String previous = null;
        Set<String> bound = new TreeSet<>();
        for (JsonNode file : files) {
            String path = file.get("path").asText();
            require(safeRelative(path), "unsafe package file path");
            require(!"manifest.json".equals(path) && !"manifest.sha256".equals(path),
                    "manifest or checksum is circularly bound");
            require(previous == null || previous.compareTo(path) < 0, "package files are not canonically ordered");
            previous = path;
            require(file.get("sha256").asText().equals(sha256(Files.readAllBytes(root.resolve(path)))),
                    "package file digest mismatch for " + path);
            bound.add(path);
        }
        for (Path content : contentFiles) {
            require(bound.contains(content.toString().replace('\\', '/')), "unbound content file " + content);
        }
    }

    private static void checkMarkers(Path root) throws IOException {
        for (String name : List.of("CHANGE-SUMMARY.md", "IMPORT-PROMPT.md")) {
            String content = lfUtf8(root.resolve(name));
            for (String marker : MARKERS) {
                require(content.contains(marker), "missing authority marker in " + name);
            }
        }
    }

    private static List<Path> contentFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).map(root::relativize)
                    .filter(path -> !(path.getNameCount() == 1 && ("manifest.json".equals(path.toString())
                            || "manifest.sha256".equals(path.toString()))))
                    .sorted(Comparator.comparing(path -> path.toString().replace('\\', '/')))
                    .toList();
        }
    }

    private static String lfUtf8(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        for (byte b : bytes) {
            require(b != '\r', "non-LF line ending in " + file.getFileName());
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "non-UTF-8 content");
        }
    }

    static boolean safeRelative(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.indexOf('\0') >= 0) {
            return false;
        }
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void require(boolean condition, String detail) {
        if (!condition) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, detail);
        }
    }
}
