package com.featuredeliveryintelligence.fdi.testbehavior.extractor;

import com.featuredeliveryintelligence.fdi.testbehavior.api.BehaviorKind;
import com.featuredeliveryintelligence.fdi.testbehavior.api.BehaviorObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ExtractionProvenance;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ReferenceKind;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ReferencedProductionSymbol;
import com.featuredeliveryintelligence.fdi.testbehavior.api.RelationshipBasis;
import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceLocation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceRoot;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorEvidenceProvider;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionRequest;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestFileObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestMethodObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.UnresolvedKind;
import com.featuredeliveryintelligence.fdi.testbehavior.api.UnresolvedReference;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserConstructorDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserRecordDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * JavaParser-backed {@link TestBehaviorEvidenceProvider} (PKB-BL-009 Slice B).
 * Extracts mechanical test-behavior observations from the configured source
 * roots of a bound repository checkout: {@code @Test} identities and source
 * locations, fixture/action/assertion observations, referenced production
 * symbols recovered through source-root resolution, and unresolved references
 * preserved as explicit evidence gaps.
 *
 * <p>Resolution is restricted to the configured source roots plus JDK
 * reflection types; the target repository is never built, its annotation
 * processors are never run, and its dependencies are never downloaded.
 * Extraction is deterministic and single-threaded because the symbol solver
 * facade is not thread-safe. Output contains no wall-clock timestamps;
 * repeated runs over identical inputs produce equal results. Observations are
 * mechanical evidence only: no Capability names, scenario wording, Product
 * truth, or evaluator labels are produced.
 */
public final class JavaParserTestBehaviorExtractor implements TestBehaviorEvidenceProvider {

    /** Stable provider identity bound into every extraction result. */
    public static final String PROVIDER_ID = "fdi-testbehavior-javaparser";
    /** Stable extractor name bound into every extraction result. */
    public static final String EXTRACTOR_NAME = "javaparser-test-behavior-extractor";
    /** Stable extractor version bound into every extraction result. */
    public static final String EXTRACTOR_VERSION = "1.0.0";

    private static final String TEST_ANNOTATION_SIMPLE_NAME = "Test";
    private static final String NESTED_ANNOTATION_SIMPLE_NAME = "Nested";

    private final Path repositoryRoot;

    /**
     * Binds the extractor to an existing repository checkout. The checkout is
     * read-only input; extraction never writes to it.
     */
    public JavaParserTestBehaviorExtractor(Path repositoryRoot) {
        if (repositoryRoot == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "repository root must not be null");
        if (!Files.isDirectory(repositoryRoot)) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.MISSING_SOURCE_ROOT, "repository root is not a directory: " + repositoryRoot);
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
    }

    /** Repository checkout this extractor is bound to. */
    public Path repositoryRoot() {
        return repositoryRoot;
    }

    @Override
    public TestBehaviorExtractionResult extract(TestBehaviorExtractionRequest request) {
        if (request == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "extraction request must not be null");

        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        List<Path> testRoots = new ArrayList<>();
        List<Path> productionRoots = new ArrayList<>();
        for (SourceRoot sourceRoot : request.sourceRoots()) {
            Path root = resolveContained(sourceRoot.repositoryRelativePath());
            if (!Files.isDirectory(root)) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.MISSING_SOURCE_ROOT,
                    "source root does not exist in the bound checkout: " + sourceRoot.repositoryRelativePath());
            typeSolver.add(new JavaParserTypeSolver(root));
            if (sourceRoot.kind() == SourceRoot.SourceRootKind.TEST) testRoots.add(root);
            else productionRoots.add(root);
        }
        if (testRoots.isEmpty()) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.MISSING_SOURCE_ROOT, "at least one TEST source root is required");

        Map<String, String> actualDigests = frozenInputDigests(request.sourceRoots());
        verifyFrozenDigests(request.inputDigests(), actualDigests);

        Extraction extraction = new Extraction(
                typeSolver, productionRoots, testRoots, new JavaParser(parserConfiguration(typeSolver)));
        List<TestFileObservation> testFiles = new ArrayList<>();
        for (Path file : listJavaFiles(testRoots)) {
            TestFileObservation observation = extraction.extractFile(file);
            if (observation != null) testFiles.add(observation);
        }
        if (testFiles.isEmpty()) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE,
                "no @Test methods found under the configured test source roots");

        boolean incomplete = !extraction.unresolved.isEmpty();
        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("extracted test files: " + testFiles.size()
                + ", test methods: " + testFiles.stream().mapToInt(f -> f.testMethods().size()).sum());
        if (incomplete) {
            Map<UnresolvedKind, Integer> counts = new EnumMap<>(UnresolvedKind.class);
            for (UnresolvedReference gap : extraction.unresolved) {
                counts.merge(gap.kind(), 1, Integer::sum);
            }
            StringBuilder summary = new StringBuilder("unresolved references preserved as evidence gaps: ");
            counts.forEach((kind, count) -> summary.append(kind).append('=').append(count).append(' '));
            diagnostics.add(summary.toString().trim());
            diagnostics.add("external API calls remain syntactic observations; no relationship edge was inferred");
        }

        return new TestBehaviorExtractionResult(
                new ExtractionProvenance(PROVIDER_ID, EXTRACTOR_NAME, EXTRACTOR_VERSION),
                request.repositoryId(),
                request.canonicalRevision(),
                request.inputDigests(),
                testFiles,
                incomplete,
                diagnostics);
    }

    private Path resolveContained(String repositoryRelativePath) {
        Path resolved = repositoryRoot.resolve(repositoryRelativePath).normalize();
        if (!resolved.startsWith(repositoryRoot)) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT,
                "source root escapes the bound repository checkout: " + repositoryRelativePath);
        return resolved;
    }

    private String relativize(Path file) {
        return repositoryRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** Computes the SHA-256 digest of every {@code .java} file under the configured roots. */
    private Map<String, String> frozenInputDigests(List<SourceRoot> sourceRoots) {
        Map<String, String> digests = new TreeMap<>();
        for (SourceRoot sourceRoot : sourceRoots) {
            for (Path file : listJavaFiles(List.of(resolveContained(sourceRoot.repositoryRelativePath())))) {
                digests.put(relativize(file), sha256(file));
            }
        }
        return digests;
    }

    /** Fails closed unless the frozen input digests match the bound checkout exactly. */
    private void verifyFrozenDigests(Map<String, String> expected, Map<String, String> actual) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String computed = actual.get(entry.getKey());
            if (computed == null) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH,
                    "frozen digest bound for a path absent from the source roots: " + entry.getKey());
            if (!computed.equalsIgnoreCase(entry.getValue())) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH,
                    "frozen input digest does not match the bound checkout: " + entry.getKey());
        }
        for (String path : actual.keySet()) {
            if (!expected.containsKey(path)) throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH,
                    "source file missing from the frozen input digests: " + path);
        }
    }

    private static List<Path> listJavaFiles(List<Path> roots) {
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .forEach(files::add);
            } catch (IOException e) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.INVALID_EVIDENCE, "cannot walk source root: " + root, e);
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    private static String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(file));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot digest " + file, e);
        }
    }

    private static ParserConfiguration parserConfiguration(CombinedTypeSolver typeSolver) {
        return new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver))
                .setAttributeComments(false);
    }

    private static boolean hasAnnotation(Node node, String simpleName) {
        if (node instanceof MethodDeclaration method) {
            return method.getAnnotations().stream().anyMatch(a -> annotationMatches(a.getNameAsString(), simpleName));
        }
        if (node instanceof TypeDeclaration<?> type) {
            return type.getAnnotations().stream().anyMatch(a -> annotationMatches(a.getNameAsString(), simpleName));
        }
        return false;
    }

    private static boolean annotationMatches(String annotationName, String simpleName) {
        return annotationName.equals(simpleName) || annotationName.endsWith("." + simpleName);
    }

    private static boolean isAssertionCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        return name.startsWith("assert") || name.equals("verify") || name.equals("fail");
    }

    private static boolean isUnder(List<Path> roots, Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        return roots.stream().anyMatch(normalized::startsWith);
    }

    /**
     * Per-run extraction context: one symbol solver, one parser, the resolved
     * source roots, and the accumulators for the current file. A fresh instance
     * is created per {@code extract} call; nothing is shared across calls or
     * threads.
     */
    private final class Extraction {
        private final JavaParserFacade facade;
        private final List<Path> productionRoots;
        private final List<Path> testRoots;
        private final JavaParser parser;
        private final Map<String, String> fileDigests = new TreeMap<>();
        private final List<UnresolvedReference> unresolved = new ArrayList<>();
        private String currentPath;

        Extraction(CombinedTypeSolver typeSolver, List<Path> productionRoots,
                   List<Path> testRoots, JavaParser parser) {
            this.facade = JavaParserFacade.get(typeSolver);
            this.productionRoots = productionRoots;
            this.testRoots = testRoots;
            this.parser = parser;
            for (Path file : listJavaFiles(testRoots)) {
                fileDigests.put(relativize(file), sha256(file));
            }
        }

        TestFileObservation extractFile(Path file) {
            ParseResult<CompilationUnit> parsed;
            try {
                parsed = parser.parse(file);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot parse " + file, e);
            }
            Optional<CompilationUnit> result = parsed.getResult()
                    .filter(unit -> unit.getPrimaryTypeName().isPresent());
            if (result.isEmpty()) return null;
            CompilationUnit unit = result.get();
            currentPath = relativize(file);
            String testClassName = qualifiedName(unit);
            boolean nestedContainer = unit.findAll(TypeDeclaration.class).stream()
                    .anyMatch(type -> hasAnnotation(type, NESTED_ANNOTATION_SIMPLE_NAME));

            List<TestMethodObservation> methods = new ArrayList<>();
            for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
                if (!hasAnnotation(method, TEST_ANNOTATION_SIMPLE_NAME)) continue;
                methods.add(extractMethod(unit, method));
            }
            if (methods.isEmpty()) return null;
            return new TestFileObservation(
                    currentPath,
                    fileDigests.get(currentPath),
                    testClassName,
                    nestedContainer,
                    methods,
                    List.of());
        }

        private String qualifiedName(CompilationUnit unit) {
            String primary = unit.getPrimaryTypeName().orElseThrow();
            return unit.getPackageDeclaration()
                    .map(p -> p.getNameAsString() + "." + primary)
                    .orElse(primary);
        }

        private TestMethodObservation extractMethod(CompilationUnit unit, MethodDeclaration method) {
            // The declaration location binds the method-name identifier (the
            // pinned discovery matrix and the Slice D golden identities use
            // that convention); an annotated method's node begin would point
            // at the annotation instead.
            SourceLocation declaration = new SourceLocation(
                    currentPath,
                    method.getName().getBegin().map(p -> p.line)
                            .or(() -> method.getBegin().map(p -> p.line)).orElse(1),
                    method.getName().getBegin().map(p -> p.column)
                            .or(() -> method.getBegin().map(p -> p.column)).orElse(1));

            List<BehaviorObservation> fixtures = new ArrayList<>();
            List<BehaviorObservation> actions = new ArrayList<>();
            List<BehaviorObservation> assertions = new ArrayList<>();
            List<UnresolvedReference> methodUnresolved = new ArrayList<>();

            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                observeCall(unit, call, fixtures, actions, assertions, methodUnresolved);
            }
            for (ObjectCreationExpr creation : method.findAll(ObjectCreationExpr.class)) {
                observeCreation(unit, creation, actions, methodUnresolved);
            }
            unresolved.addAll(methodUnresolved);

            return new TestMethodObservation(
                    method.getNameAsString(),
                    declaration,
                    fixtures,
                    actions,
                    assertions,
                    methodUnresolved);
        }

        private void observeCall(CompilationUnit unit, MethodCallExpr call,
                                 List<BehaviorObservation> fixtures, List<BehaviorObservation> actions,
                                 List<BehaviorObservation> assertions, List<UnresolvedReference> methodUnresolved) {
            SourceLocation location = locationOf(call);
            BehaviorKind kind = isAssertionCall(call) ? BehaviorKind.ASSERTION : BehaviorKind.ACTION;
            try {
                SymbolReference<ResolvedMethodDeclaration> solved = facade.solve(call);
                if (solved.isSolved()) {
                    ResolvedMethodDeclaration declaration = solved.getCorrespondingDeclaration();
                    Optional<ReferencedProductionSymbol> referenced = productionSymbol(unit, declaration);
                    if (kind != BehaviorKind.ASSERTION && isSameCompilationUnitHelper(unit, declaration)) {
                        kind = BehaviorKind.FIXTURE;
                    }
                    addObservation(kind, call.toString(), location, referenced, fixtures, actions, assertions);
                    return;
                }
                addObservation(kind, call.toString(), location, Optional.empty(),
                        fixtures, actions, assertions);
                methodUnresolved.add(new UnresolvedReference(
                        call.toString(), location, classifyUnresolved(call.getScope().orElse(null))));
            } catch (RuntimeException failure) {
                addObservation(kind, call.toString(), location, Optional.empty(),
                        fixtures, actions, assertions);
                methodUnresolved.add(new UnresolvedReference(
                        call.toString(), location, classifyUnresolved(call.getScope().orElse(null))));
            }
        }

        private void observeCreation(CompilationUnit unit, ObjectCreationExpr creation,
                                     List<BehaviorObservation> actions, List<UnresolvedReference> methodUnresolved) {
            SourceLocation location = locationOf(creation);
            try {
                SymbolReference<ResolvedConstructorDeclaration> solved = facade.solve(creation);
                if (solved.isSolved()
                        && solved.getCorrespondingDeclaration() instanceof JavaParserConstructorDeclaration constructor
                        && sourcePathOf(constructor)
                                .filter(path -> isUnder(productionRoots, path))
                                .isPresent()) {
                    ResolvedReferenceTypeDeclaration declaringType = constructor.declaringType();
                    actions.add(new BehaviorObservation(
                            BehaviorKind.ACTION,
                            creation.toString(),
                            location,
                            Optional.of(new ReferencedProductionSymbol(
                                    ReferenceKind.CONSTRUCTOR,
                                    declaringType.getQualifiedName(),
                                    declaringType.getName(),
                                    basisOf(unit, declaringType)))));
                    return;
                }
                actions.add(new BehaviorObservation(
                        BehaviorKind.ACTION, creation.toString(), location, Optional.empty()));
            } catch (RuntimeException failure) {
                actions.add(new BehaviorObservation(
                        BehaviorKind.ACTION, creation.toString(), location, Optional.empty()));
                methodUnresolved.add(new UnresolvedReference(
                        creation.toString(), location, classifyUnresolved(creation.getScope().orElse(null))));
            }
        }

        private boolean isSameCompilationUnitHelper(CompilationUnit unit, ResolvedMethodDeclaration declaration) {
            return declaration instanceof JavaParserMethodDeclaration parserMethod
                    && parserMethod.getWrappedNode().findAncestor(CompilationUnit.class)
                            .map(cu -> cu == unit).orElse(false);
        }

        private Optional<ReferencedProductionSymbol> productionSymbol(
                CompilationUnit unit, ResolvedMethodDeclaration declaration) {
            if (!(declaration instanceof JavaParserMethodDeclaration parserMethod)) return Optional.empty();
            return sourcePathOf(parserMethod)
                    .filter(path -> isUnder(productionRoots, path))
                    .map(path -> new ReferencedProductionSymbol(
                            ReferenceKind.METHOD,
                            parserMethod.declaringType().getQualifiedName(),
                            parserMethod.getName(),
                            basisOf(unit, parserMethod.declaringType())));
        }

        /** Source file a source-root-resolved declaration was parsed from, when it is file-backed. */
        private Optional<Path> sourcePathOf(JavaParserMethodDeclaration declaration) {
            return declaration.getWrappedNode().findAncestor(CompilationUnit.class)
                    .flatMap(unit -> unit.getStorage().map(storage -> storage.getPath().toAbsolutePath().normalize()));
        }

        /** Source file a source-root-resolved constructor was parsed from, when it is file-backed. */
        private Optional<Path> sourcePathOf(JavaParserConstructorDeclaration<?> declaration) {
            return declaration.getWrappedNode().findAncestor(CompilationUnit.class)
                    .flatMap(unit -> unit.getStorage().map(storage -> storage.getPath().toAbsolutePath().normalize()));
        }

        private SourceLocation locationOf(Node node) {
            return new SourceLocation(
                    currentPath,
                    node.getBegin().map(p -> p.line).orElse(1),
                    node.getBegin().map(p -> p.column).orElse(1));
        }

        private UnresolvedKind classifyUnresolved(Expression scope) {
            if (scope == null) return UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED;
            try {
                ResolvedType scopeType = facade.getType(scope);
                Optional<? extends ResolvedReferenceTypeDeclaration> declaration =
                        scopeType.asReferenceType().getTypeDeclaration();
                if (declaration.isPresent() && isSourceRootDeclaration(declaration.get())) {
                    return UnresolvedKind.MISSING_SOURCE;
                }
                return UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED;
            } catch (RuntimeException notASourceRootType) {
                return UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED;
            }
        }

        /** True when the declaration was parsed from a configured source root rather than reflection/JDK. */
        private boolean isSourceRootDeclaration(ResolvedReferenceTypeDeclaration declaration) {
            return declaration instanceof JavaParserClassDeclaration
                    || declaration instanceof JavaParserEnumDeclaration
                    || declaration instanceof JavaParserInterfaceDeclaration
                    || declaration instanceof JavaParserRecordDeclaration;
        }

        private RelationshipBasis basisOf(CompilationUnit unit, ResolvedReferenceTypeDeclaration type) {
            String simpleName = type.getName();
            boolean imported = unit.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().equals(type.getQualifiedName())
                            || i.getNameAsString().endsWith("." + simpleName));
            return imported ? RelationshipBasis.IMPORTED_SOURCE_ROOT : RelationshipBasis.SAME_PACKAGE_SOURCE_ROOT;
        }

        private void addObservation(BehaviorKind kind, String expression, SourceLocation location,
                                    Optional<ReferencedProductionSymbol> referenced,
                                    List<BehaviorObservation> fixtures, List<BehaviorObservation> actions,
                                    List<BehaviorObservation> assertions) {
            BehaviorObservation observation =
                    new BehaviorObservation(kind, expression, location, referenced);
            if (kind == BehaviorKind.FIXTURE) fixtures.add(observation);
            else if (kind == BehaviorKind.ASSERTION) assertions.add(observation);
            else actions.add(observation);
        }
    }
}
