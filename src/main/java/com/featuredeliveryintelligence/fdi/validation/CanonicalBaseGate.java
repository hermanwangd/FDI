package com.featuredeliveryintelligence.fdi.validation;import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;import com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public final class CanonicalBaseGate {
    private CanonicalBaseGate() {}

    public static Map<String, Object> verifyLock(Path baseRoot, Map<String, Object> lock, Set<String> requiredPaths) {
        if (!"FDI_CANONICAL_BASE_LOCK_V1".equals(lock.get("format"))) throw new RuntimeContractException("invalid canonical base lock format");
        List<Object> entries = RuntimeMaps.list(lock, "entries");
        if (entries.isEmpty()) throw new RuntimeContractException("canonical base lock entries must be non-empty");
        Path base = baseRoot.toAbsolutePath().normalize();
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> verified = new ArrayList<>();
        for (Object item : entries) {
            if (!(item instanceof Map<?, ?> raw)) throw new RuntimeContractException("canonical base lock entry must be an object");
            @SuppressWarnings("unchecked") Map<String, Object> entry = (Map<String, Object>) raw;
            String relative = RuntimeMaps.requiredString(entry, "path");
            String expected = RuntimeMaps.requiredString(entry, "sha256");
            Path rel = Path.of(relative);
            if (rel.isAbsolute() || relativePathEscapes(rel)) throw new RuntimeContractException("canonical base lock path must remain under base root");
            String normalized = rel.normalize().toString().replace('\\', '/');
            if (!seen.add(normalized)) throw new RuntimeContractException("duplicate canonical base lock path: " + normalized);
            Path file = base.resolve(rel).normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) throw new RuntimeContractException("canonical base file missing: " + normalized);
            String actual = sha256(file);
            if (!actual.equals(expected)) throw new RuntimeContractException("canonical base digest mismatch: " + normalized);
            verified.add(Map.of("path", normalized, "sha256", actual));
        }
        if (requiredPaths != null && !seen.equals(requiredPaths)) throw new RuntimeContractException("canonical base lock coverage mismatch");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS"); result.put("format", lock.get("format")); result.put("verified", verified);
        result.put("required_path_coverage", requiredPaths == null ? "LOCK_ENTRIES_ONLY" : "EXACT");
        return result;
    }

    public static Map<String, Object> verifyPolicy(Path baseRoot, Map<String, Object> lock, Map<String, Object> protectedSet) {
        if (!"FDI_CANONICAL_BASE_PROTECTED_SET_V1".equals(protectedSet.get("format"))) throw new RuntimeContractException("invalid canonical base protected set format");
        String id = RuntimeMaps.requiredString(protectedSet, "protected_set_id");
        Set<String> paths = new LinkedHashSet<>();
        for (Object value : RuntimeMaps.list(protectedSet, "required_paths")) {
            if (!(value instanceof String text) || text.isBlank() || !paths.add(text)) throw new RuntimeContractException("protected paths must be unique non-empty strings");
        }
        if (paths.isEmpty()) throw new RuntimeContractException("canonical base protected set required_paths must be non-empty");
        Map<String, Object> result = verifyLock(baseRoot, lock, paths); result.put("protected_set_id", id); return result;
    }

    private static boolean relativePathEscapes(Path path) { for (Path part : path) if ("..".equals(part.toString())) return true; return false; }
    private static String sha256(Path path) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
        catch (IOException error) { throw new RuntimeContractException("cannot read canonical base file", error); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
