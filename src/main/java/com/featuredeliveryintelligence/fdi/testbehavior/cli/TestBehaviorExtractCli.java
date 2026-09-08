package com.featuredeliveryintelligence.fdi.testbehavior.cli;

import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceRoot;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionRequest;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.testbehavior.extractor.JavaParserTestBehaviorExtractor;
import com.featuredeliveryintelligence.fdi.testbehavior.validation.TestBehaviorEvidenceReport;
import com.featuredeliveryintelligence.fdi.testbehavior.validation.TestBehaviorEvidenceValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Packaged CLI for the PKB-BL-009 provider-neutral test-behavior extractor
 * (Slice E wiring): {@code test-behavior-extract --repository-root PATH
 * --repository-id ID --revision SHA --output PATH [--production-root PATH]
 * [--test-root PATH] [--schema PATH]}. The CLI binds the exact source
 * revision, freezes SHA-256 input digests over the configured source roots,
 * extracts mechanical observations through {@link JavaParserTestBehaviorExtractor},
 * serializes them with {@link TestBehaviorEvidenceJson}, and refuses to write
 * output unless the document passes {@link TestBehaviorEvidenceValidator}.
 * Extraction is deterministic and single-threaded; the emitted document
 * carries no wall-clock timestamp, so two clean runs over identical inputs
 * compare byte-equal. Exit 0 on success; exit 1 with a compact
 * {@code {"status": "ERROR", "error_code": ..., "error": ...}} JSON on
 * stdout for any fail-closed refusal; exit 2 on usage errors. The command
 * emits mechanical evidence only: no Capability names, scenario wording,
 * Product truth, or evaluator labels.
 */
public final class TestBehaviorExtractCli {

    /** CLI command name dispatched from {@code FdiApplication}. */
    public static final String COMMAND = "test-behavior-extract";

    private static final String USAGE = "usage: " + COMMAND
            + " --repository-root PATH --repository-id ID --revision SHA --output PATH"
            + " [--production-root PATH] [--test-root PATH] [--schema PATH]";
    private static final String DEFAULT_PRODUCTION_ROOT = "src/main/java";
    private static final String DEFAULT_TEST_ROOT = "src/test/java";
    private static final String DEFAULT_SCHEMA = "contracts/test-behavior-evidence.schema.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private TestBehaviorExtractCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Options options;
        try {
            options = parse(args);
        } catch (CliArgumentsException failure) {
            stderr.println(USAGE);
            stderr.println(COMMAND + ": error: " + failure.getMessage());
            return 2;
        }
        try {
            List<SourceRoot> sourceRoots = List.of(
                    new SourceRoot(SourceRoot.SourceRootKind.PRODUCTION, options.productionRoot()),
                    new SourceRoot(SourceRoot.SourceRootKind.TEST, options.testRoot()));
            Map<String, String> frozenDigests = frozenInputDigests(
                    options.repositoryRoot(), options.productionRoot(), options.testRoot());
            TestBehaviorExtractionRequest request = new TestBehaviorExtractionRequest(
                    options.repositoryId(),
                    options.revision(),
                    sourceRoots,
                    frozenDigests,
                    TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION);
            TestBehaviorExtractionResult result =
                    new JavaParserTestBehaviorExtractor(options.repositoryRoot()).extract(request);
            byte[] evidence = TestBehaviorEvidenceJson.serialize(result, sourceRoots);
            TestBehaviorEvidenceReport report =
                    new TestBehaviorEvidenceValidator(Path.of(options.schema())).validate(evidence);
            Path output = Path.of(options.output());
            Path parent = output.getParent() == null ? Path.of(".") : output.getParent();
            Files.createDirectories(parent);
            Files.write(output, evidence);
            ObjectNode status = JsonNodeFactory.instance.objectNode();
            status.put("status", "OK");
            status.put("output", output.toString());
            status.put("test_files", report.testFileCount());
            status.put("test_methods", report.testMethodCount());
            status.put("incomplete", report.incomplete());
            status.put("evidence_bytes", evidence.length);
            stdout.println(status.toString());
            return 0;
        } catch (TestBehaviorExtractionException refusal) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("status", "ERROR");
            error.put("error_code", refusal.code().name());
            error.put("error", refusal.getMessage());
            stdout.println(error.toString());
            return 1;
        } catch (IllegalArgumentException | IOException failure) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("status", "ERROR");
            error.put("error_code", TestBehaviorErrorCode.INVALID_EVIDENCE.name());
            error.put("error", failure.getMessage() == null ? failure.toString() : failure.getMessage());
            stdout.println(error.toString());
            return 1;
        }
    }

    /**
     * Freezes the SHA-256 digest of every {@code .java} file under the
     * configured source roots, keyed by normalized repository-relative path.
     * Mirrors the extractor's digest algorithm so the request binding is
     * exact-revision: any drift between freeze and extraction fails closed.
     */
    private static Map<String, String> frozenInputDigests(
            Path repositoryRoot, String productionRoot, String testRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Map<String, String> digests = new TreeMap<>();
        for (String sourceRoot : List.of(productionRoot, testRoot)) {
            Path resolved = root.resolve(sourceRoot).normalize();
            if (!resolved.startsWith(root)) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT,
                    "source root escapes the bound repository checkout: " + sourceRoot);
            if (!Files.isDirectory(resolved)) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.MISSING_SOURCE_ROOT,
                    "source root does not exist in the bound checkout: " + sourceRoot);
            try (Stream<Path> files = Files.walk(resolved)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .sorted()
                        .toList()) {
                    String relative = root.relativize(file.toAbsolutePath().normalize())
                            .toString().replace('\\', '/');
                    digests.put(relative, sha256(file));
                }
            }
        }
        if (digests.isEmpty()) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.MISSING_SOURCE_ROOT,
                "no .java files found under the configured source roots");
        return digests;
    }

    private static String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        } catch (IOException failure) {
            throw new UncheckedIOException("cannot read source file: " + file, failure);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) builder.append(String.format("%02x", value));
        return builder.toString();
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            String value;
            int equalsAt = option.indexOf('=');
            if (option.startsWith("--") && equalsAt > 2) {
                value = option.substring(equalsAt + 1);
                option = option.substring(0, equalsAt);
            } else if (index + 1 < args.length) {
                value = args[++index];
            } else {
                throw new CliArgumentsException("argument " + option + ": expected one argument");
            }
            switch (option) {
                case "--repository-root", "--repository-id", "--revision", "--output",
                        "--production-root", "--test-root", "--schema" -> {
                    if (values.containsKey(option)) {
                        throw new CliArgumentsException("duplicate option " + option);
                    }
                    values.put(option, value);
                }
                default -> throw new CliArgumentsException("unknown argument " + option);
            }
        }
        require(values, "--repository-root");
        require(values, "--repository-id");
        require(values, "--revision");
        require(values, "--output");
        return new Options(
                Path.of(values.get("--repository-root")),
                values.get("--repository-id"),
                values.get("--revision"),
                values.get("--output"),
                values.getOrDefault("--production-root", DEFAULT_PRODUCTION_ROOT),
                values.getOrDefault("--test-root", DEFAULT_TEST_ROOT),
                values.getOrDefault("--schema", DEFAULT_SCHEMA));
    }

    private static void require(Map<String, String> values, String option) {
        if (!values.containsKey(option) || values.get(option).isBlank()) {
            throw new CliArgumentsException("missing required option " + option);
        }
    }

    private record Options(
            Path repositoryRoot,
            String repositoryId,
            String revision,
            String output,
            String productionRoot,
            String testRoot,
            String schema) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
