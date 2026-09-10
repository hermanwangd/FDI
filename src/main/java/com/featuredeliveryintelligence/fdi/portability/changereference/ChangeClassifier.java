package com.featuredeliveryintelligence.fdi.portability.changereference;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic change classification with fixed exclusions (SF-BL-004 Task 1). The
 *  category depends only on committed path and blob bytes, never on environment state.
 *  Denylisted paths and secret signatures fail closed; binary/LFS payloads are
 *  metadata-only so their bytes never enter excerpts. */
enum Category {
    CODE, TEST, DOCUMENTATION, CONTROL, CONTRACT, CONFIGURATION, SKILL, EVIDENCE, BINARY
}

record PathClassification(Category category, boolean excerptAllowed, boolean excluded, FailureCode denial) {
}

final class ChangeClassifier {

    private static final Set<String> DENIED_SEGMENTS = Set.of(
            ".git", "target", "build", "node_modules", ".gradle", ".idea", "dist", "out", "__pycache__");
    private static final Pattern DENIED_SUFFIX = Pattern.compile(
            "\\.(pem|key|p12|pfx|jks|keystore|zip|jar|war|tar|tgz|gz|rar|7z|class)$");
    private static final Set<String> DENIED_NAMES = Set.of(
            "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519", "credentials", ".netrc");
    private static final Set<String> CONTROL_FILES = Set.of(
            "PROJECT-OVERVIEW.md", "FRAMEWORK-SPEC.md", "BACKLOG.md", "IMPLEMENTATION-PLAN.md", "STATUS.json",
            "AGENTS.md");
    private static final Pattern CONFIG_EXTENSION = Pattern.compile(
            "\\.(properties|ya?ml|toml|xml|json|conf|cfg|ini)$");
    private static final Pattern DOC_EXTENSION = Pattern.compile("\\.(md|txt|rst|adoc)$");
    private static final Pattern BINARY_EXTENSION = Pattern.compile("\\.(png|jpe?g|gif|ico|pdf|woff2?|exe|dll|so)$");
    private static final List<Pattern> SECRET_SIGNATURES = List.of(
            Pattern.compile("-----BEGIN ([A-Z0-9]+ )*PRIVATE KEY"),
            Pattern.compile("AKIA[0-9A-Z]{16}"));

    PathClassification classify(ChangedPath change) {
        FailureCode denial = deniedPath(change.newPath()) || deniedPath(change.oldPath()) ? FailureCode.PATH_UNSAFE
                : containsSecret(change.oldBytes()) || containsSecret(change.newBytes())
                ? FailureCode.SECRET_DETECTED : null;
        if (denial != null) return new PathClassification(Category.BINARY, false, true, denial);
        byte[] content = change.newBytes() == null ? change.oldBytes() : change.newBytes();
        if (isLfsPointer(change.oldBytes()) || isLfsPointer(change.newBytes()) || isBinary(content))
            return new PathClassification(Category.BINARY, false, false, null);
        Category category = category(change.newPath() == null ? change.oldPath() : change.newPath());
        return new PathClassification(category, category != Category.BINARY, false, null);
    }

    private static boolean deniedPath(String path) {
        if (path == null) {
            return false;
        }
        String[] parts = path.replace('\\', '/').split("/");
        String name = parts[parts.length - 1];
        if (DENIED_NAMES.contains(name) || DENIED_SUFFIX.matcher(name).find()) {
            return true;
        }
        for (String part : parts) {
            if (DENIED_SEGMENTS.contains(part) || part.startsWith(".env")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSecret(byte[] bytes) {
        if (bytes == null) {
            return false;
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return SECRET_SIGNATURES.stream().anyMatch(signature -> signature.matcher(text).find());
    }

    private static boolean isBinary(byte[] bytes) {
        for (int i = 0; bytes != null && i < Math.min(bytes.length, 8192); i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLfsPointer(byte[] bytes) {
        return bytes != null && new String(bytes, 0, Math.min(bytes.length, 64), StandardCharsets.UTF_8)
                .startsWith("version https://git-lfs");
    }

    private static Category category(String path) {
        String normalized = path.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (CONTROL_FILES.contains(name)) return Category.CONTROL;
        if (name.equals("SKILL.md") || normalized.startsWith("skills/") || normalized.contains("/skills/")) return Category.SKILL;
        if (normalized.startsWith("contracts/") || normalized.contains("/contracts/") || name.endsWith(".schema.json")) return Category.CONTRACT;
        if (normalized.startsWith("validation/") || normalized.contains("/validation/") || name.endsWith(".evidence.json")) return Category.EVIDENCE;
        if (normalized.contains("src/test/") || normalized.startsWith("tests/") || normalized.contains("/tests/")
                || name.matches(".*(Tests?|IT)\\.java$") || name.startsWith("test_")) return Category.TEST;
        if (CONFIG_EXTENSION.matcher(name).find()) return Category.CONFIGURATION;
        if (DOC_EXTENSION.matcher(name).find()) return Category.DOCUMENTATION;
        return BINARY_EXTENSION.matcher(name).find() ? Category.BINARY : Category.CODE;
    }
}
