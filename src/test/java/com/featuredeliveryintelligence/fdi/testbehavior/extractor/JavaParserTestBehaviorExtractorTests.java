package com.featuredeliveryintelligence.fdi.testbehavior.extractor;

import com.featuredeliveryintelligence.fdi.testbehavior.api.BehaviorKind;
import com.featuredeliveryintelligence.fdi.testbehavior.api.BehaviorObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ReferenceKind;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ReferencedProductionSymbol;
import com.featuredeliveryintelligence.fdi.testbehavior.api.RelationshipBasis;
import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceRoot;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionRequest;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestFileObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestMethodObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.UnresolvedKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused tests for the JavaParser test-behavior extractor (PKB-BL-009
 * Slice B). Every fixture repository is written into a temporary directory
 * so the tests stay self-contained, deterministic, and free of any target
 * repository build step.
 */
class JavaParserTestBehaviorExtractorTests {

    private static final String REVISION = "c".repeat(40);

    @TempDir
    Path repositoryRoot;

    @Test
    void extractsFixturesActionsAssertionsAndSamePackageProductionReference() throws IOException {
        write("src/main/java/demo/Owner.java", """
                package demo;
                public class Owner {
                    public Owner() { }
                    public String name() { return "n"; }
                }
                """);
        write("src/main/java/demo/OwnerRepository.java", """
                package demo;
                public class OwnerRepository {
                    public Owner findByName(String name) { return new Owner(); }
                }
                """);
        write("src/test/java/demo/OwnerRepositoryTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class OwnerRepositoryTests {
                    private final OwnerRepository repository = new OwnerRepository();
                    @Test
                    void findsOwner() {
                        Owner owner = givenOwner();
                        Owner found = repository.findByName(owner.name());
                        assertOwnerFound(found);
                    }
                    private Owner givenOwner() { return new Owner(); }
                    private void assertOwnerFound(Owner owner) { }
                }
                """);

        TestBehaviorExtractionResult result = extract();

        assertThat(result.provenance().providerId())
                .isEqualTo(JavaParserTestBehaviorExtractor.PROVIDER_ID);
        assertThat(result.canonicalRevision()).isEqualTo(REVISION);
        assertThat(result.testFiles()).hasSize(1);
        TestFileObservation file = result.testFiles().get(0);
        assertThat(file.repositoryRelativePath()).isEqualTo("src/test/java/demo/OwnerRepositoryTests.java");
        assertThat(file.testClassName()).isEqualTo("demo.OwnerRepositoryTests");
        assertThat(file.nestedContainer()).isFalse();

        TestMethodObservation method = file.testMethods().get(0);
        assertThat(method.methodName()).isEqualTo("findsOwner");
        assertThat(method.declarationLocation().repositoryRelativePath())
                .isEqualTo("src/test/java/demo/OwnerRepositoryTests.java");
        assertThat(method.declarationLocation().line()).isGreaterThan(0);

        // Same-compilation-unit helper call is fixture evidence.
        assertThat(method.fixtures()).hasSize(1);
        BehaviorObservation fixture = method.fixtures().get(0);
        assertThat(fixture.observedExpression()).contains("givenOwner()");
        assertThat(fixture.referencedSymbol()).isEmpty();

        // Production method calls resolved from the same package source root;
        // the nested owner.name() call is a separate observation.
        assertThat(method.actions()).hasSize(2);
        BehaviorObservation action = method.actions().stream()
                .filter(o -> o.observedExpression().contains("repository.findByName"))
                .findFirst()
                .orElseThrow();
        Optional<ReferencedProductionSymbol> reference = action.referencedSymbol();
        assertThat(reference).isPresent();
        assertThat(reference.get().kind()).isEqualTo(ReferenceKind.METHOD);
        assertThat(reference.get().declaringType()).isEqualTo("demo.OwnerRepository");
        assertThat(reference.get().symbolName()).isEqualTo("findByName");
        assertThat(reference.get().basis()).isEqualTo(RelationshipBasis.SAME_PACKAGE_SOURCE_ROOT);

        // Same-compilation-unit assertion helper is assertion evidence.
        assertThat(method.assertions()).hasSize(1);
        assertThat(method.assertions().get(0).observedExpression()).contains("assertOwnerFound");
        assertThat(method.unresolvedReferences()).isEmpty();
        assertThat(result.incomplete()).isFalse();
    }

    @Test
    void importedProductionTypeIsRecordedWithImportedBasis() throws IOException {
        write("src/main/java/demo/OwnerService.java", """
                package demo;
                public class OwnerService {
                    public OwnerService() { }
                    public String loadName() { return "n"; }
                }
                """);
        write("src/test/java/other/OwnerServiceTests.java", """
                package other;
                import demo.OwnerService;
                import org.junit.jupiter.api.Test;
                class OwnerServiceTests {
                    @Test
                    void loadsName() {
                        OwnerService service = new OwnerService();
                        service.loadName();
                    }
                }
                """);

        TestBehaviorExtractionResult result = extract();
        TestMethodObservation method = result.testFiles().get(0).testMethods().get(0);

        Optional<ReferencedProductionSymbol> constructor = method.actions().stream()
                .filter(o -> o.observedExpression().contains("new OwnerService"))
                .map(BehaviorObservation::referencedSymbol)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(constructor).isPresent();
        assertThat(constructor.get().kind()).isEqualTo(ReferenceKind.CONSTRUCTOR);
        assertThat(constructor.get().declaringType()).isEqualTo("demo.OwnerService");
        assertThat(constructor.get().basis()).isEqualTo(RelationshipBasis.IMPORTED_SOURCE_ROOT);

        Optional<ReferencedProductionSymbol> methodCall = method.actions().stream()
                .filter(o -> o.observedExpression().contains("service.loadName()"))
                .map(BehaviorObservation::referencedSymbol)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        assertThat(methodCall).isPresent();
        assertThat(methodCall.get().basis()).isEqualTo(RelationshipBasis.IMPORTED_SOURCE_ROOT);
    }

    @Test
    void unresolvedExternalCallIsPreservedAsEvidenceGap() throws IOException {
        write("src/test/java/demo/ExternalCallTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class ExternalCallTests {
                    private ExternalDependency external;
                    @Test
                    void callsExternal() {
                        external.ping();
                    }
                }
                """);

        TestBehaviorExtractionResult result = extract();
        TestMethodObservation method = result.testFiles().get(0).testMethods().get(0);

        assertThat(method.unresolvedReferences()).hasSize(1);
        assertThat(method.unresolvedReferences().get(0).kind())
                .isEqualTo(UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED);
        assertThat(method.unresolvedReferences().get(0).location().line()).isGreaterThan(0);
        assertThat(result.incomplete()).isTrue();
        assertThat(result.diagnostics()).anyMatch(d -> d.contains("unresolved references"));
    }

    @Test
    void repeatedExtractionOverIdenticalInputsIsDeterministic() throws IOException {
        write("src/main/java/demo/Owner.java", """
                package demo;
                public class Owner {
                    public Owner() { }
                    public String name() { return "n"; }
                }
                """);
        write("src/test/java/demo/OwnerTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class OwnerTests {
                    @Test
                    void readsName() {
                        Owner owner = new Owner();
                        owner.name();
                    }
                }
                """);

        TestBehaviorExtractionResult first = extract();
        TestBehaviorExtractionResult second = extract();
        assertThat(second).isEqualTo(first);
        assertThat(second.toString()).isEqualTo(first.toString());
    }

    @Test
    void nestedTestContainerIsFlagged() throws IOException {
        write("src/test/java/demo/NestedTests.java", """
                package demo;
                import org.junit.jupiter.api.Nested;
                import org.junit.jupiter.api.Test;
                class NestedTests {
                    @Nested
                    class Inner {
                        @Test
                        void innerTest() { }
                    }
                }
                """);

        TestBehaviorExtractionResult result = extract();
        TestFileObservation file = result.testFiles().get(0);
        assertThat(file.nestedContainer()).isTrue();
        assertThat(file.testMethods()).hasSize(1);
        assertThat(file.testMethods().get(0).methodName()).isEqualTo("innerTest");
    }

    @Test
    void tamperedFrozenDigestFailsClosed() throws IOException {
        write("src/test/java/demo/SoloTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class SoloTests {
                    @Test
                    void solo() { }
                }
                """);
        Map<String, String> digests = frozenDigests();
        String path = "src/test/java/demo/SoloTests.java";
        digests.put(path, "0".repeat(64));

        assertThatThrownBy(() -> extractWithDigests(digests))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void sourceFileMissingFromFrozenDigestsFailsClosed() throws IOException {
        write("src/test/java/demo/SoloTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class SoloTests {
                    @Test
                    void solo() { }
                }
                """);
        Map<String, String> digests = frozenDigests();
        digests.remove("src/test/java/demo/SoloTests.java");

        assertThatThrownBy(() -> extractWithDigests(digests))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void missingSourceRootDirectoryFailsClosed() throws IOException {
        write("src/test/java/demo/SoloTests.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                class SoloTests {
                    @Test
                    void solo() { }
                }
                """);
        TestBehaviorExtractionRequest request = new TestBehaviorExtractionRequest(
                "repo-fixture",
                REVISION,
                List.of(new SourceRoot(SourceRoot.SourceRootKind.TEST, "src/does-not-exist")),
                frozenDigests(),
                TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION);

        assertThatThrownBy(() -> new JavaParserTestBehaviorExtractor(repositoryRoot).extract(request))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.MISSING_SOURCE_ROOT);
    }

    @Test
    void repositoryWithoutTestMethodsFailsClosed() throws IOException {
        write("src/test/java/demo/HelperOnly.java", """
                package demo;
                class HelperOnly {
                    void helper() { }
                }
                """);

        assertThatThrownBy(this::extract)
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void missingRepositoryCheckoutFailsClosed(@TempDir Path emptyDirectory) {
        Path absent = emptyDirectory.resolve("no-such-checkout");
        assertThatThrownBy(() -> new JavaParserTestBehaviorExtractor(absent))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.MISSING_SOURCE_ROOT);
    }

    private TestBehaviorExtractionResult extract() throws IOException {
        return extractWithDigests(frozenDigests());
    }

    private TestBehaviorExtractionResult extractWithDigests(Map<String, String> digests) {
        List<SourceRoot> roots = new java.util.ArrayList<>();
        if (Files.isDirectory(repositoryRoot.resolve("src/main/java"))) {
            roots.add(new SourceRoot(SourceRoot.SourceRootKind.PRODUCTION, "src/main/java"));
        }
        roots.add(new SourceRoot(SourceRoot.SourceRootKind.TEST, "src/test/java"));
        TestBehaviorExtractionRequest request = new TestBehaviorExtractionRequest(
                "repo-fixture",
                REVISION,
                roots,
                digests,
                TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION);
        return new JavaParserTestBehaviorExtractor(repositoryRoot).extract(request);
    }

    /** Frozen SHA-256 digest of every fixture source file, keyed repository-relative. */
    private Map<String, String> frozenDigests() throws IOException {
        Map<String, String> digests = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(repositoryRoot)) {
            for (Path file : walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                digests.put(repositoryRoot.relativize(file).toString().replace('\\', '/'), sha256(file));
            }
        }
        return digests;
    }

    private void write(String repositoryRelativePath, String content) throws IOException {
        Path target = repositoryRoot.resolve(repositoryRelativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(file));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
