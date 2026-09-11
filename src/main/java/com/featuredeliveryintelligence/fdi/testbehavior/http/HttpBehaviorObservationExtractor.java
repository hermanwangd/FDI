package com.featuredeliveryintelligence.fdi.testbehavior.http;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.ExtractionBasis;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts bounded HTTP behavior observations from exact-revision test
 * sources. The extractor is purely syntactic: it never resolves symbols and
 * never executes code, so external framework calls such as
 * {@code mockMvc.perform} remain external-library diagnostics while the HTTP
 * method and route written in their arguments become recoverable application
 * observations.
 *
 * <p>Supported bounded forms are exactly the constructs present in the frozen
 * Spring Petclinic 818c413 test set:
 *
 * <ul>
 * <li>MockMvc request builders ({@code get}, {@code post}, {@code put},
 * {@code patch}, {@code delete}, {@code head}, {@code options}), statically
 * imported or qualified, appearing as the argument of a {@code perform} call,
 * optionally chained with builder modifiers such as {@code param} or
 * {@code flashAttr};</li>
 * <li>{@code RestTemplate} (or {@code TestRestTemplate}) calls whose method is
 * statically recoverable: {@code getForObject}, {@code getForEntity},
 * {@code postForObject}, {@code postForEntity}, {@code postForLocation},
 * {@code put}, {@code delete}, {@code patchForObject}, and {@code exchange}
 * either with a {@code RequestEntity} factory or with an explicit
 * {@code HttpMethod} constant.</li>
 * </ul>
 *
 * <p>Route normalization separates query strings from the path and preserves
 * declared {@code {placeholder}} names. Expressions concatenated from string
 * literals and simple operands recover their literal structure with each
 * non-literal operand represented as a canonical {@code {variable}} segment.
 * Anything else remains an explicit gap; gaps are never silently dropped.
 * Observations are emitted in a byte-stable order independent of input order.
 *
 * <p>Extraction is revision-bound and fail-closed: the checkout must resolve
 * to an exact commit, and every test file must be inside the checkout by real
 * path (no symbolic-link escape), tracked at {@code HEAD}, and clean (no
 * untracked, modified, staged, or HEAD-mismatched content) before any
 * observation is emitted. A framework call is only recognized when it has a
 * receiver scope: an unqualified call merely named {@code perform},
 * {@code getForObject}, or {@code exchange} is a helper or self-call and is
 * never classified. Scoped calls whose shape is a recognized framework form
 * but is not statically recoverable stay explicit gaps.
 */
public final class HttpBehaviorObservationExtractor {

    private static final Map<String, HttpMethod> HTTP_METHOD_FACTORIES = Map.of(
            "get", HttpMethod.GET,
            "post", HttpMethod.POST,
            "put", HttpMethod.PUT,
            "patch", HttpMethod.PATCH,
            "delete", HttpMethod.DELETE,
            "head", HttpMethod.HEAD,
            "options", HttpMethod.OPTIONS);

    private static final Set<String> MOCK_MVC_BUILDER_MODIFIERS = Set.of(
            "accept", "param", "params", "content", "contentType", "flashAttr", "flashAttrs",
            "header", "headers", "cookie", "cookies", "requestAttr", "sessionAttr", "sessionAttrs",
            "secure", "with", "asyncDispatch", "locale", "characterEncoding");

    private static final Map<String, HttpMethod> REST_TEMPLATE_METHODS = Map.of(
            "getForObject", HttpMethod.GET,
            "getForEntity", HttpMethod.GET,
            "postForObject", HttpMethod.POST,
            "postForEntity", HttpMethod.POST,
            "postForLocation", HttpMethod.POST,
            "put", HttpMethod.PUT,
            "delete", HttpMethod.DELETE,
            "patchForObject", HttpMethod.PATCH);

    private static final Set<String> REQUEST_ENTITY_FACTORIES = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options");

    /** Canonical segment used for every non-literal route operand. */
    private static final String VARIABLE_SEGMENT = "{variable}";

    private static final Pattern SCHEME_AUTHORITY = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*://[^/]*");

    private static final Pattern FULL_SHA = Pattern.compile("[0-9a-f]{40}");

    /**
     * Extracts HTTP behavior observations from the given test files against an
     * exact-revision checkout. The checkout revision is resolved with
     * {@code git rev-parse HEAD} and must be a full 40-character commit, and
     * every test file is verified inside-checkout, tracked, and clean before
     * parsing; extraction fails closed otherwise.
     *
     * @param checkout repository checkout used for revision identity
     * @param testFiles test source files to parse
     * @return immutable extraction result with stable ordering
     */
    public HttpBehaviorExtractionResult extract(Path checkout, List<Path> testFiles) {
        if (checkout == null) {
            throw new RuntimeContractException("checkout must be non-null");
        }
        if (testFiles == null) {
            throw new RuntimeContractException("testFiles must be non-null");
        }
        String sourceRevision = resolveSourceRevision(checkout);
        Path root = checkout.toAbsolutePath().normalize();
        Path realRoot = toRealPath(root, "checkout");

        List<CallSite> callSites = new ArrayList<>();
        List<GapSite> gapSites = new ArrayList<>();
        for (Path testFile : dedupeAbsolute(testFiles)) {
            collectCallSites(root, realRoot, testFile, callSites, gapSites);
        }
        callSites.sort(Comparator
                .comparing((CallSite site) -> site.sourcePath)
                .thenComparingInt(site -> site.line)
                .thenComparingInt(site -> site.column)
                .thenComparing(site -> site.httpMethod.name())
                .thenComparing(site -> site.route.orElse(""))
                .thenComparing(site -> site.basis.name())
                .thenComparing(site -> site.testMethod.orElse("")));
        gapSites.sort(Comparator
                .comparing((GapSite site) -> site.sourcePath)
                .thenComparingInt(site -> site.line)
                .thenComparingInt(site -> site.column)
                .thenComparing(site -> site.reason));

        List<HttpBehaviorObservation> observations = new ArrayList<>(callSites.size());
        List<String> gaps = new ArrayList<>();
        for (CallSite site : callSites) {
            String location = site.sourcePath + ":" + site.line;
            if (site.testMethod.isEmpty()) {
                gaps.add(location + " HTTP observation outside a test method is unsupported");
                continue;
            }
            if (site.route.isEmpty()) {
                gaps.add(location + " unsupported dynamic route expression in " + site.trigger);
                continue;
            }
            observations.add(new HttpBehaviorObservation(
                    "http-behavior-observation-" + String.format("%05d", observations.size() + 1),
                    site.sourcePath,
                    site.testMethod.get(),
                    location,
                    site.httpMethod,
                    site.route.get(),
                    site.basis));
        }
        for (GapSite site : gapSites) {
            gaps.add(site.sourcePath + ":" + site.line + " " + site.reason);
        }
        gaps.sort(String::compareTo);
        return new HttpBehaviorExtractionResult(sourceRevision, observations, gaps);
    }

    private static List<Path> dedupeAbsolute(List<Path> testFiles) {
        Map<String, Path> unique = new LinkedHashMap<>();
        for (Path testFile : testFiles) {
            if (testFile == null) {
                throw new RuntimeContractException("testFiles must not contain null entries");
            }
            unique.putIfAbsent(testFile.toAbsolutePath().normalize().toString(), testFile);
        }
        return List.copyOf(unique.values());
    }

    private void collectCallSites(Path root, Path realRoot, Path testFile,
            List<CallSite> callSites, List<GapSite> gapSites) {
        Path absolute = testFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new RuntimeContractException("test file is unavailable: " + absolute);
        }
        if (!absolute.startsWith(root)) {
            throw new RuntimeContractException("test file is outside the checkout: " + absolute);
        }
        String sourcePath = root.relativize(absolute).toString().replace('\\', '/');
        verifyFileProvenance(root, realRoot, absolute, sourcePath);
        CompilationUnit unit = parse(absolute);

        for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
            Inspection inspection = inspectCall(call);
            Optional<String> testMethod =
                    call.findAncestor(MethodDeclaration.class).map(MethodDeclaration::getNameAsString);
            if (inspection.candidate().isPresent()) {
                Candidate candidate = inspection.candidate().get();
                callSites.add(new CallSite(
                        sourcePath,
                        candidate,
                        testMethod,
                        recoverRoute(candidate.routeExpression)));
            }
            else if (inspection.gap().isPresent()) {
                gapSites.add(new GapSite(
                        sourcePath,
                        call.getBegin().map(position -> position.line).orElse(0),
                        call.getBegin().map(position -> position.column).orElse(0),
                        inspection.gap().get()));
            }
        }
    }

    /**
     * Classifies one method call. Framework recognition requires a receiver
     * scope: an unqualified call merely named {@code perform},
     * {@code getForObject}, {@code exchange}, or another RestTemplate method
     * is a helper or self-call and is ignored, never an observation. Scoped
     * calls in a recognized framework shape that is not statically
     * recoverable return an explicit gap.
     */
    private Inspection inspectCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (name.equals("perform")) {
            if (call.getScope().isEmpty() || call.getArguments().isEmpty()) {
                return Inspection.ignored();
            }
            return inspectMockMvcPerform(call);
        }
        HttpMethod simpleMethod = REST_TEMPLATE_METHODS.get(name);
        if (simpleMethod != null) {
            if (call.getScope().isEmpty() || call.getArguments().isEmpty()) {
                return Inspection.ignored();
            }
            return Inspection.candidate(new Candidate(call, "RestTemplate " + name + " call",
                    call.getArgument(0), simpleMethod, ExtractionBasis.REST_TEMPLATE_CALL));
        }
        if (name.equals("exchange")) {
            if (call.getScope().isEmpty() || call.getArguments().isEmpty()) {
                return Inspection.ignored();
            }
            return inspectRestTemplateExchange(call);
        }
        return Inspection.ignored();
    }

    private Inspection inspectMockMvcPerform(MethodCallExpr perform) {
        Expression expression = perform.getArgument(0);
        while (expression instanceof MethodCallExpr modifier
                && MOCK_MVC_BUILDER_MODIFIERS.contains(modifier.getNameAsString())
                && modifier.getScope().isPresent()) {
            expression = modifier.getScope().get();
        }
        if (expression instanceof MethodCallExpr builder
                && HTTP_METHOD_FACTORIES.containsKey(builder.getNameAsString())
                && !builder.getArguments().isEmpty()) {
            return Inspection.candidate(new Candidate(perform, "MockMvc perform",
                    builder.getArgument(0), HTTP_METHOD_FACTORIES.get(builder.getNameAsString()),
                    ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        }
        return Inspection.gap("MockMvc perform argument is not a statically recoverable request builder");
    }

    private Inspection inspectRestTemplateExchange(MethodCallExpr exchange) {
        Expression first = exchange.getArgument(0);
        if (first instanceof MethodCallExpr build
                && build.getNameAsString().equals("build")
                && build.getScope().isPresent()
                && build.getScope().get() instanceof MethodCallExpr requestEntity
                && REQUEST_ENTITY_FACTORIES.contains(requestEntity.getNameAsString())
                && !requestEntity.getArguments().isEmpty()) {
            return Inspection.candidate(new Candidate(exchange, "RestTemplate exchange call",
                    requestEntity.getArgument(0),
                    HTTP_METHOD_FACTORIES.get(requestEntity.getNameAsString()),
                    ExtractionBasis.REST_TEMPLATE_CALL));
        }
        if (exchange.getArguments().size() >= 2
                && exchange.getArgument(1) instanceof FieldAccessExpr fieldAccess
                && fieldAccess.getScope().toString().equals("HttpMethod")) {
            try {
                HttpMethod method = HttpMethod.valueOf(fieldAccess.getNameAsString());
                return Inspection.candidate(new Candidate(exchange, "RestTemplate exchange call",
                        first, method, ExtractionBasis.REST_TEMPLATE_CALL));
            }
            catch (IllegalArgumentException ignored) {
                return Inspection.ignored();
            }
        }
        return Inspection.gap("RestTemplate exchange call with non-statically-recoverable HttpMethod");
    }

    /**
     * Recovers the raw route string, keeping a canonical {@code {variable}}
     * marker for every non-literal operand of a string concatenation.
     */
    private Optional<String> recoverRoute(Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(literal.getValue());
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            StringBuilder recovered = new StringBuilder();
            boolean hasLiteral = flattenConcatenation(binary, recovered);
            if (hasLiteral && recovered.indexOf("/") >= 0) {
                return Optional.of(recovered.toString());
            }
        }
        return Optional.empty();
    }

    private boolean flattenConcatenation(Expression expression, StringBuilder recovered) {
        if (expression instanceof LiteralStringValueExpr literal) {
            recovered.append(literal.getValue());
            return true;
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            boolean left = flattenConcatenation(binary.getLeft(), recovered);
            boolean right = flattenConcatenation(binary.getRight(), recovered);
            return left || right;
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return flattenConcatenation(enclosed.getInner(), recovered);
        }
        recovered.append(VARIABLE_SEGMENT);
        return false;
    }

    private static Optional<String> normalizeRoute(String rawRoute) {
        String path = rawRoute;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        Matcher scheme = SCHEME_AUTHORITY.matcher(path);
        if (scheme.find()) {
            path = path.substring(scheme.end());
        }
        if (path.isEmpty()) {
            return Optional.of("/");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            return Optional.of("/");
        }
        return Optional.of("/" + String.join("/", segments));
    }

    /**
     * Verifies that one test file is trustworthy at the resolved revision: its
     * real path must stay inside the real checkout root (no symbolic-link
     * escape), it must be tracked at {@code HEAD}, and it must be clean (no
     * untracked, modified, staged, or HEAD-mismatched content).
     */
    private static void verifyFileProvenance(Path root, Path realRoot, Path file, String relative) {
        Path realFile = toRealPath(file, relative);
        if (!realFile.startsWith(realRoot)) {
            throw new RuntimeContractException(
                    "test file escapes the checkout through a symbolic link: " + relative);
        }
        if (runGit(root, List.of("ls-files", "--error-unmatch", "--", relative)) != 0) {
            throw new RuntimeContractException("test file is not tracked at HEAD: " + relative);
        }
        String status = runGitOutput(root, List.of(
                "status", "--porcelain=v1", "--untracked-files=all", "--", relative));
        if (!status.isEmpty()) {
            throw new RuntimeContractException(
                    "test file is not clean at HEAD: " + relative + " (" + status.trim() + ")");
        }
    }

    private static Path toRealPath(Path path, String label) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new RuntimeContractException("cannot resolve real path of " + label, e);
        }
    }

    private CompilationUnit parse(Path file) {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        JavaParser parser = new JavaParser(configuration);
        try {
            var result = parser.parse(file);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult().get();
            }
        }
        catch (IOException e) {
            throw new RuntimeContractException("test file cannot be read: " + file, e);
        }
        throw new RuntimeContractException("test file is not parseable Java: " + file);
    }

    private static String resolveSourceRevision(Path checkout) {
        List<String> command = List.of(
                "git", "-C", checkout.toAbsolutePath().normalize().toString(), "rev-parse", "HEAD");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exit = process.waitFor();
            if (exit == 0 && FULL_SHA.matcher(output).matches()) {
                return output;
            }
        }
        catch (IOException e) {
            throw new RuntimeContractException("git is required to resolve the exact source revision", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeContractException("revision resolution was interrupted", e);
        }
        throw new RuntimeContractException(
                "checkout does not resolve to an exact commit revision: "
                        + checkout.toAbsolutePath().normalize());
    }

    private static int runGit(Path root, List<String> args) {
        try {
            return runGitProcess(root, args).exit();
        } catch (IOException e) {
            throw new RuntimeContractException("git is required to verify test provenance", e);
        }
    }

    private static String runGitOutput(Path root, List<String> args) {
        try {
            GitResult result = runGitProcess(root, args);
            if (result.exit() == 0) {
                return result.output().trim();
            }
            return "";
        } catch (IOException e) {
            throw new RuntimeContractException("git is required to verify test provenance", e);
        }
    }

    private static GitResult runGitProcess(Path root, List<String> args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(root.toString());
        command.addAll(args);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (java.io.InputStream stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            return new GitResult(process.waitFor(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeContractException("git provenance verification was interrupted", e);
        }
    }

    private record GitResult(int exit, String output) {
    }

    /** Classification of one method call: observation candidate, explicit gap, or ignored. */
    private record Inspection(Optional<Candidate> candidate, Optional<String> gap) {

        private static Inspection candidate(Candidate candidate) {
            return new Inspection(Optional.of(candidate), Optional.empty());
        }

        private static Inspection gap(String reason) {
            return new Inspection(Optional.empty(), Optional.of(reason));
        }

        private static Inspection ignored() {
            return new Inspection(Optional.empty(), Optional.empty());
        }
    }

    /** One recognized-but-unsupported framework call site, kept as an explicit gap. */
    private static final class GapSite {
        private final String sourcePath;
        private final int line;
        private final int column;
        private final String reason;

        private GapSite(String sourcePath, int line, int column, String reason) {
            this.sourcePath = sourcePath;
            this.line = line;
            this.column = column;
            this.reason = reason;
        }
    }

    /** One recoverable HTTP call site before normalization. */
    private static final class Candidate {
        private final MethodCallExpr call;
        private final String trigger;
        private final Expression routeExpression;
        private final HttpMethod httpMethod;
        private final ExtractionBasis basis;
        private final int line;
        private final int column;

        private Candidate(MethodCallExpr call, String trigger, Expression routeExpression,
                HttpMethod httpMethod, ExtractionBasis basis) {
            this.call = call;
            this.trigger = trigger;
            this.routeExpression = routeExpression;
            this.httpMethod = httpMethod;
            this.basis = basis;
            this.line = call.getBegin().map(position -> position.line).orElse(0);
            this.column = call.getBegin().map(position -> position.column).orElse(0);
        }
    }

    /** One recoverable HTTP call site with provenance and normalized route. */
    private static final class CallSite {
        private final String sourcePath;
        private final String trigger;
        private final HttpMethod httpMethod;
        private final ExtractionBasis basis;
        private final int line;
        private final int column;
        private final Optional<String> testMethod;
        private final Optional<String> route;

        private CallSite(String sourcePath, Candidate candidate, Optional<String> testMethod,
                Optional<String> route) {
            this.sourcePath = sourcePath;
            this.trigger = candidate.trigger;
            this.httpMethod = candidate.httpMethod;
            this.basis = candidate.basis;
            this.line = candidate.line;
            this.column = candidate.column;
            this.testMethod = testMethod;
            this.route = route.flatMap(HttpBehaviorObservationExtractor::normalizeRoute);
        }
    }
}
