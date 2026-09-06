package com.featuredeliveryintelligence.fdi.validation.experimentrunner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the {@code pkb001_runner.py} characterization cases of
 * {@code tests/test_pkb001_phase0.py} to the Java {@link ExperimentRunner} API:
 * the exact F1/R1/R2/R3 arm input allowlists, the ground-truth and Product
 * semantics prohibitions, the verified READY readiness binding, the command
 * prohibition vocabulary, the network-isolation evidence requirement, and the
 * sandbox-exec subprocess construction with a sanitized environment.
 */
class ExperimentRunnerCharacterizationTests {
    private static final MessageDigest SHA256 = sha256();

    @TempDir Path temp;
    private Path root;
    private Path groundTruth;

    @BeforeEach
    void setUp() throws IOException {
        root = temp.toRealPath();
        for (String category : new String[] {"structure", "history", "semantics"}) {
            Path dir = root.resolve("inputs").resolve(category);
            Files.createDirectories(dir);
            Files.write(dir.resolve("input.json"), "{}".getBytes(StandardCharsets.UTF_8));
        }
        groundTruth = root.resolve("validation/pkb001/ground-truth");
        Files.createDirectories(groundTruth);
        Files.write(groundTruth.resolve("gold.json"), "{}".getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String sha256Hex(byte[] data) {
        synchronized (SHA256) {
            return hex(SHA256.digest(data));
        }
    }

    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte value : data) {
            out.append(Character.forDigit((value >> 4) & 0xf, 16));
            out.append(Character.forDigit(value & 0xf, 16));
        }
        return out.toString();
    }

    private void writeReadiness(String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(root.resolve("phase0-readiness.json"), bytes);
        Files.write(root.resolve("phase0-readiness.sha256"),
                (sha256Hex(bytes) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> enforcedEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PKB_NETWORK_ISOLATION", "ENFORCED");
        return env;
    }

    @Test
    void unknownArmIsRejectedBeforeWorkspaceResolution() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().validateArmInputs(root, "X",
                        List.of("inputs/structure/input.json")));
        assertEquals("unknown PKB-001 arm: X", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R1", "R2", "R3"})
    void reverseArmsRejectGroundTruthAndProductSemantics(String arm) {
        ExperimentRunner runner = new ExperimentRunner();
        IllegalArgumentException ground = assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, arm,
                        List.of("validation/pkb001/ground-truth/gold.json")));
        assertEquals("prohibited input for " + arm
                + ": validation/pkb001/ground-truth/gold.json", ground.getMessage());
        IllegalArgumentException semantics = assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, arm, List.of("inputs/semantics/input.json")));
        assertEquals("prohibited input for " + arm + ": inputs/semantics/input.json",
                semantics.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"R1", "R2", "R3", "F1"})
    void allGenerationArmsRejectEvaluatorGroundTruth(String arm) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().validateArmInputs(root, arm,
                        List.of("validation/pkb001/ground-truth/gold.json")));
        assertEquals("prohibited input for " + arm
                + ": validation/pkb001/ground-truth/gold.json", failure.getMessage());
    }

    @Test
    void armAllowlistsAreExact() throws IOException {
        ExperimentRunner runner = new ExperimentRunner();
        assertEquals(List.of(root.resolve("inputs/structure/input.json")),
                runner.validateArmInputs(root, "R1", List.of("inputs/structure/input.json")));
        assertEquals(List.of(root.resolve("inputs/history/input.json")),
                runner.validateArmInputs(root, "R2", List.of("inputs/history/input.json")));
        assertEquals(List.of(root.resolve("inputs/structure/input.json"),
                        root.resolve("inputs/history/input.json")),
                runner.validateArmInputs(root, "R3",
                        List.of("inputs/structure/input.json", "inputs/history/input.json")));
        assertEquals(List.of(root.resolve("inputs/semantics/input.json"),
                        root.resolve("inputs/structure/input.json")),
                runner.validateArmInputs(root, "F1",
                        List.of("inputs/semantics/input.json", "inputs/structure/input.json")));
        assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, "F1", List.of("inputs/history/input.json")));
        assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, "R1", List.of("inputs/history/input.json")));
        assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, "R2", List.of("inputs/structure/input.json")));
    }

    @Test
    void unsafeInputPathsAreProhibited() {
        ExperimentRunner runner = new ExperimentRunner();
        assertEquals("prohibited input: /tmp/outside", assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, "R1", List.of("/tmp/outside"))).getMessage());
        assertEquals("prohibited input: ../outside", assertThrows(IllegalArgumentException.class,
                () -> runner.validateArmInputs(root, "R1", List.of("../outside"))).getMessage());
        assertEquals("prohibited input: inputs/structure/missing.json",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.validateArmInputs(root, "R1",
                                List.of("inputs/structure/missing.json"))).getMessage());
    }

    @Test
    void symlinksAndDirectoriesAreProhibited() throws IOException {
        Path link = root.resolve("inputs/structure/link.json");
        Files.createSymbolicLink(link, root.resolve("inputs/structure/input.json"));
        ExperimentRunner runner = new ExperimentRunner();
        assertEquals("prohibited input: inputs/structure/link.json",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.validateArmInputs(root, "R1",
                                List.of("inputs/structure/link.json"))).getMessage());
        assertEquals("prohibited input: inputs/structure",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.validateArmInputs(root, "R1",
                                List.of("inputs/structure"))).getMessage());
    }

    @Test
    void inputsRequireThreePartsUnderTheArmCategory() throws IOException {
        // a directory fails the file check before the parts check, like Python
        assertEquals("prohibited input: inputs/structure",
                assertThrows(IllegalArgumentException.class,
                        () -> new ExperimentRunner().validateArmInputs(root, "R1",
                                List.of("inputs/structure"))).getMessage());
        Files.write(root.resolve("inputs/ab"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("wrong/structure"));
        Files.write(root.resolve("wrong/structure/input.json"),
                "{}".getBytes(StandardCharsets.UTF_8));
        ExperimentRunner runner = new ExperimentRunner();
        assertEquals("prohibited input for R1: inputs/ab",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.validateArmInputs(root, "R1", List.of("inputs/ab")))
                        .getMessage());
        assertEquals("prohibited input for R1: wrong/structure/input.json",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.validateArmInputs(root, "R1",
                                List.of("wrong/structure/input.json"))).getMessage());
    }

    @Test
    void atLeastOneArmInputIsRequired() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().validateArmInputs(root, "R1", List.of()));
        assertEquals("at least one arm input is required", failure.getMessage());
    }

    /** Captures the subprocess launch like the Python test's monkeypatched {@code subprocess.run}. */
    private static final class LaunchCapture implements ExperimentRunner.CommandLauncher {
        List<String> argv;
        Path cwd;
        Map<String, String> env;
        int timeoutSeconds;
        int exitCode;

        @Override
        public int launch(List<String> argv, Path cwd, Map<String, String> env, int timeoutSeconds) {
            this.argv = List.copyOf(argv);
            this.cwd = cwd;
            this.env = new LinkedHashMap<>(env);
            this.timeoutSeconds = timeoutSeconds;
            return exitCode;
        }
    }

    @Test
    void executeArmUsesVerifiedReadinessAndSanitizedEnvironment() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        LaunchCapture capture = new LaunchCapture();
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PATH", "/usr/bin");
        env.put("API_TOKEN", "remove-me");
        env.put("PKB_NETWORK_ISOLATION", "ENFORCED");

        int result = new ExperimentRunner().executeArm(List.of("python3", "safe_runner.py"), root, env,
                List.of(groundTruth), capture);

        assertEquals(0, result);
        assertEquals(root, capture.cwd);
        assertEquals(300, capture.timeoutSeconds);
        assertEquals("/usr/bin/sandbox-exec", capture.argv.get(0));
        assertEquals("-D", capture.argv.get(1));
        assertEquals("PROTECTED_0=" + groundTruth, capture.argv.get(2));
        assertEquals("-p", capture.argv.get(3));
        assertEquals("(version 1) (allow default) (deny network*) (deny file-read* file-write* "
                + "(subpath (param \"PROTECTED_0\")))", capture.argv.get(4));
        assertEquals(List.of("python3", "safe_runner.py"),
                capture.argv.subList(5, capture.argv.size()));
        assertEquals("ENFORCED", capture.env.get("PKB_NETWORK_ISOLATION"));
        assertFalse(capture.env.containsKey("API_TOKEN"));
        assertEquals(root.resolve(".pkb-tmp").toString(), capture.env.get("TMPDIR"));
        assertTrue(Files.isDirectory(root.resolve(".pkb-tmp")));
    }

    @Test
    void sandboxProfileHasTrailingSpaceWithoutProtectedPaths() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        LaunchCapture capture = new LaunchCapture();
        new ExperimentRunner().executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(), capture);
        assertEquals("(version 1) (allow default) (deny network*) ", capture.argv.get(2));
    }

    @Test
    void readinessReportDigestMismatchIsRejected() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        Files.write(root.resolve("phase0-readiness.sha256"), "0".repeat(64).getBytes(StandardCharsets.UTF_8));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().executeArm(List.of("/usr/bin/true"), root,
                        enforcedEnv(), List.of(), new LaunchCapture()));
        assertEquals("readiness report digest mismatch", failure.getMessage());
    }

    @Test
    void verifiedReadyReportIsRequired() throws IOException {
        ExperimentRunner runner = new ExperimentRunner();
        // missing readiness files
        assertEquals("verified READY report is required", assertThrows(IllegalArgumentException.class,
                () -> runner.executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(),
                        new LaunchCapture())).getMessage());
        // digest file without the report
        Files.write(root.resolve("phase0-readiness.sha256"), "0".repeat(64).getBytes(StandardCharsets.UTF_8));
        assertEquals("verified READY report is required", assertThrows(IllegalArgumentException.class,
                () -> runner.executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(),
                        new LaunchCapture())).getMessage());
        // malformed report JSON
        Files.write(root.resolve("phase0-readiness.json"), "not-json".getBytes(StandardCharsets.UTF_8));
        assertEquals("verified READY report is required", assertThrows(IllegalArgumentException.class,
                () -> runner.executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(),
                        new LaunchCapture())).getMessage());
        // well-formed but not READY
        writeReadiness("{\"status\": \"BLOCKED\"}");
        assertEquals("verified READY report is required", assertThrows(IllegalArgumentException.class,
                () -> runner.executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(),
                        new LaunchCapture())).getMessage());
        // digest with uppercase hex is not a verified lowercase digest
        writeReadiness("{\"status\": \"READY\"}");
        String mixed = sha256Hex(Files.readAllBytes(root.resolve("phase0-readiness.json")));
        Files.write(root.resolve("phase0-readiness.sha256"),
                mixed.toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        assertEquals("readiness report digest mismatch", assertThrows(IllegalArgumentException.class,
                () -> runner.executeArm(List.of("/usr/bin/true"), root, enforcedEnv(), List.of(),
                        new LaunchCapture())).getMessage());
    }

    @Test
    void networkIsolationEvidenceIsRequired() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PATH", "/usr/bin");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().executeArm(List.of("/usr/bin/true"), root, env,
                        List.of(), new LaunchCapture()));
        assertEquals("network isolation evidence is required", failure.getMessage());
    }

    @Test
    void commandMustBeANonEmptyStringTuple() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        ExperimentRunner runner = new ExperimentRunner();
        assertEquals("command must be a non-empty string tuple",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.executeArm(List.of(), root, enforcedEnv(), List.of(),
                                new LaunchCapture())).getMessage());
        assertEquals("command must be a non-empty string tuple",
                assertThrows(IllegalArgumentException.class,
                        () -> runner.executeArm(List.of("python3", ""), root, enforcedEnv(),
                                List.of(), new LaunchCapture())).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "./mvnw test", "gradle build", "npm install", "bash target-repository-script.sh",
        "mvn test", "make all", "/usr/bin/zsh script.command", "pnpm run build"})
    void targetBuildCommandsAndShellsAreProhibited(String commandLine) throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        List<String> command = List.of(commandLine.split(" ", -1));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRunner().executeArm(command, root, enforcedEnv(), List.of(),
                        new LaunchCapture()));
        assertEquals("prohibited command: " + commandLine, failure.getMessage());
    }

    @Test
    void safeCommandsAreNotProhibited() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        LaunchCapture capture = new LaunchCapture();
        new ExperimentRunner().executeArm(List.of("python3", "safe_runner.py"), root,
                enforcedEnv(), List.of(), capture);
        assertEquals("python3", capture.argv.get(3));
        assertEquals("safe_runner.py", capture.argv.get(4));
    }

    @Test
    void sensitiveEnvironmentKeysAreStrippedCaseInsensitively() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        LaunchCapture capture = new LaunchCapture();
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PATH", "/usr/bin");
        env.put("API_TOKEN", "remove");
        env.put("api_key", "remove");
        env.put("MY-SECRET-VALUE", "remove");
        env.put("some_PASSWORD", "remove");
        env.put("aCredentialB", "remove");
        env.put("service-token", "remove");
        env.put("PKB_NETWORK_ISOLATION", "ENFORCED");
        new ExperimentRunner().executeArm(List.of("/usr/bin/true"), root, env, List.of(), capture);
        assertEquals(List.of("PATH", "PKB_NETWORK_ISOLATION", "TMPDIR"),
                new ArrayList<>(capture.env.keySet()));
    }

    @Test
    void missingProtectedPathPropagatesLikePythonStrictResolve() throws IOException {
        writeReadiness("{\"status\": \"READY\"}");
        assertThrows(IOException.class,
                () -> new ExperimentRunner().executeArm(List.of("/usr/bin/true"), root,
                        enforcedEnv(), List.of(root.resolve("missing")), new LaunchCapture()));
    }
}
