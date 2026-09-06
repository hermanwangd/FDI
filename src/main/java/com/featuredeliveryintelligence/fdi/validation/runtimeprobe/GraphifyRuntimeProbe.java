package com.featuredeliveryintelligence.fdi.validation.runtimeprobe;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java port of the transitional Python
 * {@code tooling/validation/graphify_runtime_probe.py} {@code inspect_runtime}
 * contract. Discovers a Graphify executable ({@code --command}, else
 * {@code graphify} / {@code graphify-cli} on the search path), binds its
 * resolved path and SHA-256, and optionally validates an exported interface
 * descriptor. Statuses, field names, and the {@code "Graphify descriptor is
 * incomplete"} failure vocabulary match the Python consumer; no Graphify
 * behavior is changed or assumed.
 */
public final class GraphifyRuntimeProbe {

    /**
     * Mirrors {@code inspect_runtime(command, descriptor)}. A {@code null} or
     * non-regular-file command reports {@code runtime_found: false}; a valid
     * descriptor always moves the status to {@code INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND}
     * and an incomplete or malformed descriptor raises
     * {@link IllegalArgumentException} with the Python failure text.
     */
    public GraphifyRuntimeProbeResult inspectRuntime(Path command, Path descriptor) throws IOException {
        Path executable = null;
        if (command != null && Files.isRegularFile(command)) {
            executable = command.toRealPath();
        }
        String sha256 = executable == null ? null : sha256Hex(Files.readAllBytes(executable));
        boolean found = executable != null;
        if (descriptor == null) {
            return new GraphifyRuntimeProbeResult(found, executable == null ? null : executable.toString(),
                    sha256, found ? "DISCOVERED_NOT_VERIFIED" : "NOT_VERIFIED",
                    List.of(), List.of(), null, null, null, null);
        }
        return describeInterface(found, executable == null ? null : executable.toString(), sha256, descriptor);
    }

    /** Validates the exported interface descriptor and returns the described-interface result. */
    private static GraphifyRuntimeProbeResult describeInterface(
            boolean found, String runtimePath, String sha256, Path descriptor) throws IOException {
        JsonNode data = PythonJson.readTree(Files.readAllBytes(descriptor));
        String[] required = {"runtime_identity", "runtime_version", "transport", "wire_version"};
        List<String> values = new ArrayList<>(required.length);
        if (data == null || !data.isObject()) {
            throw new IllegalArgumentException("Graphify descriptor is incomplete");
        }
        for (String key : required) {
            JsonNode value = data.get(key);
            if (value == null || !value.isTextual() || value.asText().isEmpty()) {
                throw new IllegalArgumentException("Graphify descriptor is incomplete");
            }
            values.add(value.asText());
        }
        JsonNode operations = data.get("supported_operations");
        if (operations == null || !operations.isArray() || operations.isEmpty()) {
            throw new IllegalArgumentException("Graphify descriptor is incomplete");
        }
        List<String> supported = new ArrayList<>(operations.size());
        for (JsonNode operation : operations) {
            if (!operation.isTextual() || operation.asText().isEmpty()) {
                throw new IllegalArgumentException("Graphify descriptor is incomplete");
            }
            supported.add(operation.asText());
        }
        return new GraphifyRuntimeProbeResult(found, runtimePath, sha256,
                "INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND", List.copyOf(supported), List.of(),
                values.get(0), values.get(1), values.get(2), values.get(3));
    }

    /**
     * Mirrors the CLI's {@code shutil.which('graphify') or shutil.which('graphify-cli')}
     * fallback over the given search directories (an empty directory means the
     * current directory, like an empty {@code PATH} component). Returns the
     * resolved path of the first executable regular file, else {@code null}.
     */
    public Path discoverExecutable(List<Path> searchDirectories) {
        for (String name : new String[] {"graphify", "graphify-cli"}) {
            for (Path directory : searchDirectories) {
                Path candidate = (directory == null || directory.toString().isEmpty()
                        ? Path.of("") : directory).resolve(name);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    try {
                        return candidate.toRealPath();
                    } catch (IOException failure) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                out.append(String.format("%02x", value & 0xff));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
