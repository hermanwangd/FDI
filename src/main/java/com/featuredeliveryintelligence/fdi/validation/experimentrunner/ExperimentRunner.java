package com.featuredeliveryintelligence.fdi.validation.experimentrunner;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Deterministic PKB-001 experiment runner. Ports the observable behavior of the
 * transitional Python consumer {@code pkb001_runner.py}: the exact F1/R1/R2/R3
 * arm input allowlists under {@code inputs/<category>/}, the fail-closed
 * path/symlink/ground-truth prohibitions, the verified READY readiness report
 * binding (SHA-256 digest of {@code phase0-readiness.json} recorded in
 * {@code phase0-readiness.sha256}), the prohibited-command vocabulary (build
 * tools and shells), the {@code PKB_NETWORK_ISOLATION=ENFORCED} evidence
 * requirement, the sensitive-environment-key stripping, and the
 * {@code /usr/bin/sandbox-exec} subprocess construction with protected-path
 * deny rules, a private {@code .pkb-tmp} directory, and a 300-second timeout.
 * {@link IllegalArgumentException} mirrors the Python {@code ValueError}
 * vocabulary; {@link IOException} mirrors the Python {@code OSError} surface.
 */
public final class ExperimentRunner {
    static final Map<String, Set<String>> ALLOWED = Map.of(
            "R1", Set.of("structure"),
            "R2", Set.of("history"),
            "R3", Set.of("structure", "history"),
            "F1", Set.of("semantics", "structure"));
    static final Pattern SENSITIVE_KEY =
            Pattern.compile("(?i)(token|secret|password|credential|api[_-]?key)");
    static final Set<String> PROHIBITED_COMMANDS = Set.of(
            "mvn", "mvnw", "gradle", "gradlew", "npm", "npx", "yarn", "pnpm",
            "make", "cmake", "ant");
    static final Set<String> SHELLS = Set.of("bash", "sh", "zsh");
    static final String SANDBOX_EXEC = "/usr/bin/sandbox-exec";
    static final int TIMEOUT_SECONDS = 300;

    /** Launch seam mirroring the Python test's monkeypatched {@code subprocess.run}. */
    public interface CommandLauncher {
        int launch(List<String> argv, Path cwd, Map<String, String> env, int timeoutSeconds)
                throws IOException;
    }

    private static final CommandLauncher PROCESS_LAUNCHER = (argv, cwd, env, timeoutSeconds) -> {
        ProcessBuilder builder = new ProcessBuilder(argv);
        builder.directory(cwd.toFile());
        builder.environment().clear();
        builder.environment().putAll(env);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("command timed out after "
                        + timeoutSeconds + " seconds: " + String.join(" ", argv));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("interrupted while waiting for command", interrupted);
        }
        return process.exitValue();
    };

    /** Ports {@code validate_arm_inputs}: exact arm allowlists and fail-closed input paths. */
    public List<Path> validateArmInputs(Path workspace, String arm, List<String> inputs)
            throws IOException {
        if (arm == null || !ALLOWED.containsKey(arm)) {
            throw new IllegalArgumentException("unknown PKB-001 arm: " + arm);
        }
        Path root = workspace.toRealPath();
        List<Path> resolved = new ArrayList<>();
        for (String relative : inputs) {
            List<String> parts = pyParts(relative);
            if (isAbsolute(relative) || parts.contains("..")) {
                throw new IllegalArgumentException("prohibited input: " + relative);
            }
            Path candidate = root.resolve(relative);
            Path actual;
            try {
                actual = candidate.toRealPath();
            } catch (IOException | RuntimeException error) {
                throw new IllegalArgumentException("prohibited input: " + relative, error);
            }
            if (!actual.startsWith(root)) {
                throw new IllegalArgumentException("prohibited input: " + relative);
            }
            if (Files.isSymbolicLink(candidate) || !Files.isRegularFile(actual)) {
                throw new IllegalArgumentException("prohibited input: " + relative);
            }
            if (parts.size() < 3 || !"inputs".equals(parts.get(0))
                    || !ALLOWED.get(arm).contains(parts.get(1))) {
                throw new IllegalArgumentException("prohibited input for " + arm + ": " + relative);
            }
            resolved.add(actual);
        }
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("at least one arm input is required");
        }
        return List.copyOf(resolved);
    }

    /** Ports {@code execute_arm} with the real {@code sandbox-exec} launcher. */
    public int executeArm(List<String> command, Path workspace, Map<String, String> env,
            List<Path> protectedPaths) throws IOException {
        return executeArm(command, workspace, env, protectedPaths, PROCESS_LAUNCHER);
    }

    /** Ports {@code execute_arm} with an explicit launch seam for characterization tests. */
    int executeArm(List<String> command, Path workspace, Map<String, String> env,
            List<Path> protectedPaths, CommandLauncher launcher) throws IOException {
        Objects.requireNonNull(env, "env");
        Path root = workspace.toRealPath();
        verifiedReadiness(root);
        safeCommand(command);
        if (!"ENFORCED".equals(env.get("PKB_NETWORK_ISOLATION"))) {
            throw new IllegalArgumentException("network isolation evidence is required");
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (entry.getKey() != null && !SENSITIVE_KEY.matcher(entry.getKey()).find()) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        Path temporary = root.resolve(".pkb-tmp");
        Files.createDirectories(temporary);
        sanitized.put("PKB_NETWORK_ISOLATION", "ENFORCED");
        sanitized.put("TMPDIR", temporary.toString());
        List<Path> resolvedProtected = new ArrayList<>();
        for (Path path : protectedPaths) {
            resolvedProtected.add(path.toRealPath());
        }
        StringBuilder profile = new StringBuilder("(version 1) (allow default) (deny network*) ");
        for (int index = 0; index < resolvedProtected.size(); index++) {
            if (index > 0) {
                profile.append(' ');
            }
            profile.append("(deny file-read* file-write* (subpath (param \"PROTECTED_")
                    .append(index).append("\")))");
        }
        List<String> sandboxed = new ArrayList<>(List.of(SANDBOX_EXEC));
        for (int index = 0; index < resolvedProtected.size(); index++) {
            sandboxed.add("-D");
            sandboxed.add("PROTECTED_" + index + "=" + resolvedProtected.get(index));
        }
        sandboxed.add("-p");
        sandboxed.add(profile.toString());
        sandboxed.addAll(command);
        return launcher.launch(sandboxed, root, sanitized, TIMEOUT_SECONDS);
    }

    /** Ports {@code _verified_readiness}: digest-verified READY report, fail closed. */
    private void verifiedReadiness(Path root) throws IOException {
        Path reportPath = root.resolve("phase0-readiness.json");
        Path digestPath = root.resolve("phase0-readiness.sha256");
        String expected;
        byte[] reportBytes;
        JsonNode report;
        try {
            expected = Files.readString(digestPath).strip();
            reportBytes = Files.readAllBytes(reportPath);
            report = PythonJson.readTree(reportBytes);
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            // Python collapses OSError and JSONDecodeError into one ValueError.
            throw new IllegalArgumentException("verified READY report is required", failure);
        }
        String actual = sha256Hex(reportBytes);
        if (!Pattern.compile("[0-9a-f]{64}").matcher(expected).matches()
                || !expected.equals(actual)) {
            throw new IllegalArgumentException("readiness report digest mismatch");
        }
        if (!report.isObject() || !report.has("status")
                || !report.get("status").isTextual()
                || !"READY".equals(report.get("status").asText())) {
            throw new IllegalArgumentException("verified READY report is required");
        }
    }

    /** Ports {@code _safe_command}: build tools and shells are prohibited. */
    static void safeCommand(List<String> command) {
        if (command == null || command.isEmpty()
                || command.stream().anyMatch(part -> part == null || part.isEmpty())) {
            throw new IllegalArgumentException("command must be a non-empty string tuple");
        }
        String executable = baseName(command.get(0)).toLowerCase(Locale.ROOT);
        String normalized = executable.startsWith("./") ? executable.substring(2) : executable;
        boolean prohibitedArgument = false;
        for (String part : command) {
            if (PROHIBITED_COMMANDS.contains(baseName(part).toLowerCase(Locale.ROOT))) {
                prohibitedArgument = true;
                break;
            }
        }
        if (PROHIBITED_COMMANDS.contains(normalized) || prohibitedArgument) {
            throw new IllegalArgumentException("prohibited command: " + String.join(" ", command));
        }
        boolean shellArgument = false;
        for (String part : command) {
            if (part.endsWith(".sh") || part.endsWith(".command")) {
                shellArgument = true;
                break;
            }
        }
        if (SHELLS.contains(executable) || shellArgument) {
            throw new IllegalArgumentException("prohibited command: " + String.join(" ", command));
        }
    }

    /** Python {@code Path.name}: trailing separators dropped, last path component. */
    static String baseName(String path) {
        int end = path.length();
        while (end > 0 && path.charAt(end - 1) == '/') {
            end--;
        }
        if (end == 0) {
            return "";
        }
        int start = path.lastIndexOf('/', end - 1);
        return path.substring(start + 1, end);
    }

    /** Python {@code PurePosixPath.parts}: split on '/', drop empties and '.', keep '..'. */
    static List<String> pyParts(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/", -1)) {
            if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        return parts;
    }

    private static boolean isAbsolute(String path) {
        return path != null && path.startsWith("/");
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(data);
            StringBuilder out = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                out.append(Character.forDigit((value >> 4) & 0xf, 16));
                out.append(Character.forDigit(value & 0xf, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
