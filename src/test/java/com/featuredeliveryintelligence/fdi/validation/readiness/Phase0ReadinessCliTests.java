package com.featuredeliveryintelligence.fdi.validation.readiness;

import com.featuredeliveryintelligence.fdi.application.Phase0ReadinessCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packaged-CLI tests for {@code phase0-readiness-validate} with byte-for-byte
 * stdout and exit-code assertions against frozen reference bytes captured from
 * the removed transitional Python CLI {@code tooling/validation/pkb001_gate.py}
 * (run through {@code python3}) at the BL-026 combined integration. The frozen
 * bytes are the Python consumer's actual stdout for each case's exact inputs;
 * they are asserted as immutable reference bytes, never re-executed. Error
 * cases that embed the caller's ephemeral temp paths substitute the test's own
 * {@code @TempDir} path at runtime. The repository-root expectations
 * ({@link #FROZEN_REPOSITORY_BLOCKED_REPORT}, {@link #FROZEN_REPOSITORY_READY_REPORT})
 * are frozen against the repository checkout state at that integration.
 */
class Phase0ReadinessCliTests {
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    /** Capture-time temp root, substituted with the test's own temp dir at runtime. */
    private static final String CAPTURE_TEMP = "/tmp/frozen/p0/tmp";

    private static final String FROZEN_REPOSITORY_BLOCKED_REPORT = """
{
  "experiment": "PKB-001",
  "status": "BLOCKED",
  "readiness_state": "NOT_READY",
  "readiness_flags": {
    "PRODUCT_SEMANTICS_FROZEN": false,
    "LIVE_GRAPHIFY_INTERFACE_VERIFIED": false,
    "PK_S1_EXECUTION_READY": false,
    "PK_S2_EXECUTION_READY": false,
    "CALIBRATION_DATASET_FROZEN": false,
    "GROUND_TRUTH_SEALED": false
  },
  "prerequisites": [
    {
      "id": "P0-01",
      "status": "MISSING",
      "reason": "Product Semantics evidence is absent"
    },
    {
      "id": "P0-02",
      "status": "MISSING",
      "reason": "Graphify evidence is absent"
    },
    {
      "id": "P0-03",
      "status": "MISSING",
      "reason": "Skill evidence is absent"
    },
    {
      "id": "P0-04",
      "status": "MISSING",
      "reason": "calibration dataset evidence is absent"
    },
    {
      "id": "P0-05",
      "status": "MISSING",
      "reason": "evaluator ground truth evidence is absent"
    }
  ],
  "review_state": {
    "phase0_protocol_actors": "UNVERIFIED",
    "non_human_review_completed": false,
    "human_review_status": "UNVERIFIED"
  }
}
""";

    private static final String FROZEN_REPOSITORY_READY_REPORT = """
{
  "experiment": "PKB-001",
  "status": "READY",
  "readiness_state": "READY",
  "readiness_flags": {
    "PRODUCT_SEMANTICS_FROZEN": true,
    "LIVE_GRAPHIFY_INTERFACE_VERIFIED": true,
    "PK_S1_EXECUTION_READY": true,
    "PK_S2_EXECUTION_READY": true,
    "CALIBRATION_DATASET_FROZEN": true,
    "GROUND_TRUTH_SEALED": true
  },
  "prerequisites": [
    {
      "id": "P0-01",
      "status": "SATISFIED",
      "reason": "Product Semantics frozen by Product Team"
    },
    {
      "id": "P0-02",
      "status": "SATISFIED",
      "reason": "exact Graphify binding verified"
    },
    {
      "id": "P0-03",
      "status": "SATISFIED",
      "reason": "PK-S1 and PK-S2 materialized"
    },
    {
      "id": "P0-04",
      "status": "SATISFIED",
      "reason": "calibration dataset frozen"
    },
    {
      "id": "P0-05",
      "status": "SATISFIED",
      "reason": "evaluator ground truth sealed"
    }
  ],
  "review_state": {
    "phase0_protocol_actors": "INDEPENDENT_AI_AGENT_CONTEXTS",
    "non_human_review_completed": true,
    "human_review_status": "PENDING_POST_GENERATION_SECTION_6"
  }
}
""";

    private static final String MISSING_EVIDENCE_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "[Errno 2] No such file or directory: '/tmp/frozen/p0/tmp/absent.json'"}
""";

    private static final String DIRECTORY_EVIDENCE_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "[Errno 21] Is a directory: '/tmp/frozen/p0/tmp'"}
""";

    private static final String NON_OBJECT_EVIDENCE_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "evidence root must be an object"}
""";

    private static final String MALFORMED_EVIDENCE_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "Expecting value: line 1 column 14 (char 13)"}
""";

    private static final String OUTPUT_OUTSIDE_ROOT_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "output path must remain inside repository root"}
""";

    private static final String OUTPUT_THROUGH_SYMLINK_ERROR = """
{"experiment": "PKB-001", "status": "ERROR", "error": "output path must not be a symlink"}
""";


    @TempDir Path temp;

    @Test
    void handlesOnlyPhase0ReadinessValidateCommand() {
        assertFalse(Phase0ReadinessCli.handles(new String[0]));
        assertFalse(Phase0ReadinessCli.handles(new String[] {"unrelated"}));
        assertTrue(Phase0ReadinessCli.handles(new String[] {"phase0-readiness-validate"}));
    }

    @Test
    void repositoryPhase0RemainsBlockedWithoutExternalEvidence() {
        Result java = javaGate(new String[] {"--root", REPOSITORY.toString()});
        assertEquals(2, java.exitCode());
        assertArrayEquals(FROZEN_REPOSITORY_BLOCKED_REPORT.getBytes(StandardCharsets.UTF_8),
                java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8).contains("P0-01"));
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8).contains("P0-04"));
    }

    @Test
    void repositoryPhase0IsReadyWithFrozenPhase0Evidence() {
        Result java = javaGate(new String[] {"--root", REPOSITORY.toString(),
                "--evidence", "validation/pkb001/datasets/phase0-evidence.json"});
        assertEquals(0, java.exitCode(), new String(java.stdout(), StandardCharsets.UTF_8));
        assertArrayEquals(FROZEN_REPOSITORY_READY_REPORT.getBytes(StandardCharsets.UTF_8),
                java.stdout());
    }

    @Test
    void blockedReportWithDefaultEvidenceMatchesPythonBytes() {
        Result java = javaGate(new String[] {"--root", temp.toString()});
        assertEquals(2, java.exitCode());
        assertArrayEquals(FROZEN_REPOSITORY_BLOCKED_REPORT.getBytes(StandardCharsets.UTF_8),
                java.stdout());
    }

    @Test
    void outputFileAndStdoutMatchPythonBytes() throws Exception {
        Path javaOut = temp.resolve("java-report.json");
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--output", javaOut.toString()});
        assertEquals(2, java.exitCode());
        assertArrayEquals(FROZEN_REPOSITORY_BLOCKED_REPORT.getBytes(StandardCharsets.UTF_8),
                java.stdout());
        assertArrayEquals(FROZEN_REPOSITORY_BLOCKED_REPORT.getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(javaOut));
    }

    @Test
    void missingEvidenceFileMatchesPythonErrorBytes() {
        String missing = temp.resolve("absent.json").toString();
        Result java = javaGate(new String[] {"--root", temp.toString(), "--evidence", missing});
        assertEquals(1, java.exitCode());
        String expected = MISSING_EVIDENCE_ERROR
                .replace(CAPTURE_TEMP + "/absent.json", missing);
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("[Errno 2] No such file or directory: '" + missing + "'"));
    }

    @Test
    void directoryEvidenceMatchesPythonIsADirectoryError() {
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--evidence", temp.toString()});
        assertEquals(1, java.exitCode());
        String expected = DIRECTORY_EVIDENCE_ERROR.replace(CAPTURE_TEMP, temp.toString());
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("[Errno 21] Is a directory: '" + temp + "'"));
    }

    @Test
    void nonObjectEvidenceMatchesPythonValueError() throws Exception {
        Path evidence = temp.resolve("array.json");
        Files.writeString(evidence, "[1, 2]\n");
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--evidence", evidence.toString()});
        assertEquals(1, java.exitCode());
        assertArrayEquals(NON_OBJECT_EVIDENCE_ERROR.getBytes(StandardCharsets.UTF_8),
                java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("evidence root must be an object"));
    }

    @Test
    void malformedEvidenceMatchesPythonJsonDecodeError() throws Exception {
        Path evidence = temp.resolve("broken.json");
        Files.writeString(evidence, "{\"unclosed\": ");
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--evidence", evidence.toString()});
        assertEquals(1, java.exitCode());
        assertArrayEquals(MALFORMED_EVIDENCE_ERROR.getBytes(StandardCharsets.UTF_8),
                java.stdout());
    }

    @Test
    void outputOutsideRootMatchesPythonContainmentError() throws Exception {
        Path root = temp.resolve("repo");
        Files.createDirectories(root);
        Path outside = temp.resolve("outside.json");
        Result java = javaGate(new String[] {"--root", root.toString(),
                "--output", outside.toString()});
        assertEquals(1, java.exitCode());
        assertArrayEquals(OUTPUT_OUTSIDE_ROOT_ERROR.getBytes(StandardCharsets.UTF_8),
                java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("output path must remain inside repository root"));
        assertFalse(Files.exists(outside));
    }

    @Test
    void outputThroughSymlinkMatchesPythonSymlinkError() throws Exception {
        Path outside = temp.resolve("outside");
        Files.createDirectory(outside);
        Path link = temp.resolve("report-link.json");
        Files.createSymbolicLink(link, outside.resolve("report.json"));
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--output", "report-link.json"});
        assertEquals(1, java.exitCode());
        assertArrayEquals(OUTPUT_THROUGH_SYMLINK_ERROR.getBytes(StandardCharsets.UTF_8),
                java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("output path must not be a symlink"));
    }

    @Test
    void usageErrorForUnknownOptionExitsTwo() {
        Result result = javaGate(new String[] {"--root", temp.toString(), "--bogus", "x"});
        assertEquals(2, result.exitCode());
        assertTrue(new String(result.stderr(), StandardCharsets.UTF_8)
                .contains("usage: phase0-readiness-validate"));
    }

    private static Result javaGate(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Phase0ReadinessCli.run(withCommand(args),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
    }

    private static String[] withCommand(String[] args) {
        String[] full = new String[args.length + 1];
        full[0] = "phase0-readiness-validate";
        System.arraycopy(args, 0, full, 1, args.length);
        return full;
    }

    private record Result(int exitCode, byte[] stdout, byte[] stderr) { }
}
