package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic class-plus-method Spring route-handler index over
 * exact-revision production source (SF-BL-002 Task 2B). Combines class-level
 * {@code @RequestMapping} with method-level {@code @RequestMapping},
 * {@code @GetMapping}, and {@code @PostMapping} annotations — the mapping
 * forms present at the bound revision — and emits immutable
 * {@link RouteHandler} records (schema
 * {@code software-factory.sf-bl002-route-handler.v0.3}).
 *
 * <p>Only statically recoverable literal route values are indexed; a mapping
 * whose route or HTTP method cannot be read from the annotation without
 * executing code stays an explicit gap. Classes without a
 * {@code @Controller}/{@code @RestController} stereotype are not Spring
 * handlers and are never indexed. A route literal that is present but unsafe
 * (query string, backslash) fails the build closed.
 *
 * <p>Resolution is structurally exact: handler placeholders bind exactly one
 * observed segment each, so {@code /owners/1}, {@code /owners/{variable}},
 * and {@code /owners/{ownerId}} all resolve against a single
 * {@code /owners/{ownerId}} handler, while a literal handler segment only
 * matches an exactly equal observed segment. A unique match resolves, zero
 * matches stay {@link RouteResolution.Status#UNRESOLVED}, more than one stays
 * {@link RouteResolution.Status#AMBIGUOUS}; a resolution never falls back to a
 * guessed handler.
 *
 * <p>The build is revision-bound: the checkout must resolve to an exact
 * commit, and every production file must be a real-path-inside, tracked,
 * HEAD-clean source, so each emitted handler digest is trustworthy at the
 * resolved revision.
 */
public final class SpringRouteHandlerIndex {

    /** Method-level mapping annotations with their fixed HTTP methods, at the bound revision. */
    private static final Map<String, List<HttpMethod>> FIXED_METHOD_MAPPINGS = Map.of(
            "GetMapping", List.of(HttpMethod.GET),
            "PostMapping", List.of(HttpMethod.POST));

    private static final List<HttpMethod> ALL_METHODS = List.of(HttpMethod.values());

    private static final java.util.regex.Pattern FULL_SHA = java.util.regex.Pattern.compile("[0-9a-f]{40}");

    private final String sourceRevision;
    private final List<RouteHandler> handlers;

    private SpringRouteHandlerIndex(String sourceRevision, List<RouteHandler> handlers) {
        this.sourceRevision = sourceRevision;
        this.handlers = handlers;
    }

    /**
     * Builds the index from the given production sources. Relative entries in
     * {@code productionFiles} resolve against {@code checkout}; every file
     * must be a canonical {@code .java} file that is inside the checkout by
     * real path, tracked at {@code HEAD}, and clean, so the index reflects
     * the exact resolved revision. The build fails closed on missing,
     * duplicate, unsafe, unverifiable, or unparsable inputs.
     */
    public static SpringRouteHandlerIndex build(Path checkout, List<Path> productionFiles) {
        if (checkout == null || !Files.isDirectory(checkout)) {
            throw new RuntimeContractException("checkout must be an existing directory");
        }
        if (productionFiles == null) {
            throw new RuntimeContractException("productionFiles must be non-null");
        }
        String sourceRevision = resolveSourceRevision(checkout);
        Path root = checkout.toAbsolutePath().normalize();
        Path realRoot = toRealPath(root, "checkout");
        Map<String, RouteHandler> byRef = new HashMap<>();
        Set<Path> seen = new HashSet<>();
        for (Path entry : productionFiles) {
            if (entry == null) {
                throw new RuntimeContractException("productionFiles must not contain null entries");
            }
            Path file = entry.isAbsolute() ? entry : root.resolve(entry);
            file = file.normalize();
            if (!file.startsWith(root)) {
                throw new RuntimeContractException("production file escapes the checkout: " + entry);
            }
            if (!file.getFileName().toString().endsWith(".java")) {
                throw new RuntimeContractException("production file must be a .java source: " + entry);
            }
            if (!Files.isRegularFile(file)) {
                throw new RuntimeContractException("production file does not exist: " + entry);
            }
            if (!seen.add(file)) {
                throw new RuntimeContractException("duplicate production file: " + entry);
            }
            String relative = root.relativize(file).toString().replace(File.separatorChar, '/');
            verifyFileProvenance(root, realRoot, file, relative);
            indexFile(file, relative, byRef);
        }
        List<RouteHandler> ordered = new ArrayList<>(byRef.values());
        ordered.sort(Comparator.comparing(RouteHandler::handlerRef));
        return new SpringRouteHandlerIndex(sourceRevision, List.copyOf(ordered));
    }

    /**
     * Resolves one observed HTTP method and normalized route template. Method
     * parsing is case-insensitive; unknown methods and malformed routes are
     * rejected instead of guessed. A handler binds only when the HTTP method
     * matches and every route segment matches exactly: a handler placeholder
     * binds exactly one non-empty observed segment (literal or placeholder),
     * a literal handler segment requires exact equality, and the segment
     * counts must be equal. Multiple structural matches stay
     * {@link RouteResolution.Status#AMBIGUOUS}; there is no ranking or fuzzy
     * fallback.
     */
    public RouteResolution resolve(String httpMethod, String normalizedRouteTemplate) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new RuntimeContractException("httpMethod must be non-blank");
        }
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(httpMethod.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeContractException("unsupported httpMethod: " + httpMethod);
        }
        HttpBehaviorObservation.requireNormalizedRoute(normalizedRouteTemplate);

        List<RouteHandler> matches = handlers.stream()
                .filter(handler -> handler.httpMethods().contains(method)
                        && structurallyMatches(handler.normalizedRouteTemplate(), normalizedRouteTemplate))
                .toList();
        return switch (matches.size()) {
            case 0 -> RouteResolution.unresolved(method, normalizedRouteTemplate);
            case 1 -> RouteResolution.resolved(method, normalizedRouteTemplate, matches.get(0));
            default -> RouteResolution.ambiguous(method, normalizedRouteTemplate, matches);
        };
    }

    /** The exact commit revision this index was built against. */
    public String sourceRevision() {
        return sourceRevision;
    }

    /** All indexed handlers, ordered deterministically by {@code handlerRef}. */
    public List<RouteHandler> handlers() {
        return handlers;
    }

    /**
     * Structural route match: equal segment count, exact literal equality on
     * literal handler segments, and exactly one non-empty observed segment
     * bound per handler placeholder, independent of placeholder names.
     */
    private static boolean structurallyMatches(String handlerRoute, String observedRoute) {
        List<String> handlerSegments = routeSegments(handlerRoute);
        List<String> observedSegments = routeSegments(observedRoute);
        if (handlerSegments.size() != observedSegments.size()) {
            return false;
        }
        for (int i = 0; i < handlerSegments.size(); i++) {
            String handlerSegment = handlerSegments.get(i);
            if (isPlaceholder(handlerSegment)) {
                if (observedSegments.get(i).isEmpty()) {
                    return false;
                }
                continue;
            }
            if (!handlerSegment.equals(observedSegments.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> routeSegments(String route) {
        if (route.equals("/")) {
            return List.of();
        }
        return List.of(route.substring(1).split("/", -1));
    }

    private static boolean isPlaceholder(String segment) {
        return segment.length() > 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}'
                && segment.substring(1, segment.length() - 1).matches("[A-Za-z][A-Za-z0-9_]*");
    }

    private static void indexFile(Path file, String relative, Map<String, RouteHandler> byRef) {
        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read production source: " + relative, e);
        }
        String digest = HexFormat.of().formatHex(sha256(content));

        JavaParser parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
        CompilationUnit unit;
        var result = parser.parse(new String(content, StandardCharsets.UTF_8));
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new RuntimeContractException("production source does not parse: " + relative);
        }
        unit = result.getResult().get();

        for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!isController(type)) {
                continue;
            }
            Optional<List<String>> classRoutes = classLevelRoutes(type);
            if (classRoutes.isEmpty()) {
                // class-level @RequestMapping present but not statically recoverable:
                // composing a guessed prefix would fabricate routes
                continue;
            }
            for (MethodDeclaration method : type.getMethods()) {
                for (AnnotationExpr annotation : method.getAnnotations()) {
                    Optional<Mapping> mapping = readMapping(annotation);
                    if (mapping.isEmpty()) {
                        continue;
                    }
                    for (String classRoute : classRoutes.get()) {
                        for (String route : mapping.get().routes()) {
                            String normalized = normalizeRoute(compose(classRoute, route));
                            List<HttpMethod> methods = mapping.get().httpMethods();
                            String identity = type.getFullyQualifiedName()
                                    .orElseThrow(() -> new RuntimeContractException(
                                            "controller type requires a package: " + relative))
                                    + "#" + method.getNameAsString();
                            String handlerRef = identity + " " + methodsCsv(methods) + " " + normalized;
                            RouteHandler handler = new RouteHandler(handlerRef, methods, normalized,
                                    identity, relative + ":" + annotationLine(annotation, method), digest);
                            RouteHandler previous = byRef.putIfAbsent(handlerRef, handler);
                            if (previous != null && !previous.equals(handler)) {
                                throw new RuntimeContractException(
                                        "conflicting handlers for " + handlerRef + " in " + relative);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isController(ClassOrInterfaceDeclaration type) {
        for (AnnotationExpr annotation : type.getAnnotations()) {
            String simple = simpleName(annotation);
            if (simple.equals("Controller") || simple.equals("RestController")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Class-level route prefixes. Present {@code @RequestMapping} with
     * literal routes contributes those routes; an absent annotation
     * contributes the empty prefix; a non-literal value yields
     * {@link Optional#empty()} so the class is skipped rather than guessed.
     */
    private static Optional<List<String>> classLevelRoutes(ClassOrInterfaceDeclaration type) {
        for (AnnotationExpr annotation : type.getAnnotations()) {
            if (!simpleName(annotation).equals("RequestMapping")) {
                continue;
            }
            return readMapping(annotation)
                    .map(Mapping::routes)
                    .map(routes -> routes.isEmpty() ? List.of("") : routes);
        }
        return Optional.of(List.of(""));
    }

    /** One method-level mapping annotation, or empty when unsupported or non-literal. */
    private static Optional<Mapping> readMapping(AnnotationExpr annotation) {
        String simple = simpleName(annotation);
        List<HttpMethod> fixed = FIXED_METHOD_MAPPINGS.get(simple);
        if (fixed == null && !simple.equals("RequestMapping")) {
            return Optional.empty();
        }
        Expression valueExpr = null;
        Expression methodExpr = null;
        if (annotation.isSingleMemberAnnotationExpr()) {
            valueExpr = annotation.asSingleMemberAnnotationExpr().getMemberValue();
        } else if (annotation.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normal = annotation.asNormalAnnotationExpr();
            for (MemberValuePair pair : normal.getPairs()) {
                switch (pair.getNameAsString()) {
                    case "value", "path" -> valueExpr = pair.getValue();
                    case "method" -> methodExpr = pair.getValue();
                    default -> {
                        // produces, headers, consumes, params: not part of the route identity
                    }
                }
            }
        }
        Optional<List<String>> routes = literalStrings(valueExpr);
        if (routes.isEmpty()) {
            // a non-literal route value is not statically recoverable; never guess
            return Optional.empty();
        }
        return Optional.of(new Mapping(routes.get(), fixed != null ? fixed : requestMethods(methodExpr)));
    }

    private static List<HttpMethod> requestMethods(Expression methodExpr) {
        if (methodExpr == null) {
            return ALL_METHODS;
        }
        List<Expression> elements = methodExpr.isArrayInitializerExpr()
                ? methodExpr.asArrayInitializerExpr().getValues()
                : List.of(methodExpr);
        if (elements.isEmpty()) {
            return ALL_METHODS;
        }
        Set<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
        for (Expression element : elements) {
            String constant = element.isFieldAccessExpr()
                    ? element.asFieldAccessExpr().getNameAsString()
                    : element.toString();
            try {
                methods.add(HttpMethod.valueOf(constant));
            } catch (IllegalArgumentException e) {
                throw new RuntimeContractException("unsupported RequestMethod constant: " + constant);
            }
        }
        List<HttpMethod> ordered = new ArrayList<>(methods);
        ordered.sort(null);
        return List.copyOf(ordered);
    }

    /** Literal strings from a single value or array; empty when any element is non-literal. */
    private static Optional<List<String>> literalStrings(Expression expr) {
        if (expr == null) {
            return Optional.of(List.of(""));
        }
        List<Expression> elements = expr.isArrayInitializerExpr()
                ? expr.asArrayInitializerExpr().getValues()
                : List.of(expr);
        Set<String> values = new LinkedHashSet<>();
        for (Expression element : elements) {
            if (!element.isStringLiteralExpr()) {
                return Optional.empty();
            }
            values.add(element.asStringLiteralExpr().asString());
        }
        return Optional.of(List.copyOf(values));
    }

    private static String compose(String classRoute, String route) {
        String prefix = classRoute == null ? "" : classRoute;
        String path = route == null ? "" : route;
        if (prefix.isEmpty()) {
            return path.isEmpty() ? "/" : path;
        }
        if (path.isEmpty()) {
            return prefix;
        }
        return prefix + "/" + path;
    }

    private static String normalizeRoute(String raw) {
        if (raw.indexOf('\\') >= 0 || raw.indexOf('?') >= 0) {
            throw new RuntimeContractException("unsafe route literal in production mapping: " + raw);
        }
        StringBuilder normalized = new StringBuilder();
        for (String segment : raw.split("/", -1)) {
            if (!segment.isEmpty()) {
                normalized.append('/').append(segment);
            }
        }
        String route = normalized.length() == 0 ? "/" : normalized.toString();
        HttpBehaviorObservation.requireNormalizedRoute(route);
        return route;
    }

    private static String simpleName(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static int annotationLine(AnnotationExpr annotation, MethodDeclaration method) {
        return annotation.getBegin()
                .map(position -> position.line)
                .orElseGet(() -> method.getBegin().map(position -> position.line).orElse(1));
    }

    private static String methodsCsv(List<HttpMethod> methods) {
        StringBuilder csv = new StringBuilder();
        for (HttpMethod method : methods) {
            if (csv.length() > 0) {
                csv.append(',');
            }
            csv.append(method.name());
        }
        return csv.toString();
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Verifies that one production file is trustworthy at the resolved
     * revision: its real path must stay inside the real checkout root (no
     * symbolic-link escape), it must be tracked at {@code HEAD}, and it must
     * be clean (no untracked, modified, staged, or HEAD-mismatched content).
     */
    private static void verifyFileProvenance(Path root, Path realRoot, Path file, String relative) {
        Path realFile = toRealPath(file, relative);
        if (!realFile.startsWith(realRoot)) {
            throw new RuntimeContractException(
                    "production file escapes the checkout through a symbolic link: " + relative);
        }
        if (runGit(root, List.of("ls-files", "--error-unmatch", "--", relative)) != 0) {
            throw new RuntimeContractException("production file is not tracked at HEAD: " + relative);
        }
        String status = runGitOutput(root, List.of(
                "status", "--porcelain=v1", "--untracked-files=all", "--", relative));
        if (!status.isEmpty()) {
            throw new RuntimeContractException(
                    "production file is not clean at HEAD: " + relative + " (" + status.trim() + ")");
        }
    }

    private static Path toRealPath(Path path, String label) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new RuntimeContractException("cannot resolve real path of " + label, e);
        }
    }

    private static String resolveSourceRevision(Path checkout) {
        Path root = checkout.toAbsolutePath().normalize();
        String output = runGitOutput(root, List.of("rev-parse", "HEAD"));
        if (FULL_SHA.matcher(output).matches()) {
            return output;
        }
        throw new RuntimeContractException(
                "checkout does not resolve to an exact commit revision: " + root);
    }

    private static int runGit(Path root, List<String> args) {
        try {
            return runGitProcess(root, args).exit();
        } catch (IOException e) {
            throw new RuntimeContractException("git is required to verify production provenance", e);
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
            throw new RuntimeContractException("git is required to verify production provenance", e);
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

    private record Mapping(List<String> routes, List<HttpMethod> httpMethods) {
    }
}
