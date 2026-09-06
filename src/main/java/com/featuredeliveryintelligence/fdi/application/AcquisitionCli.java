package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.acquisition.AcquisitionValidator;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 acquisition validator. Ports the observable
 * contract of the transitional Python consumer
 * {@code tooling/validation/pkb001_acquisition.py} behind
 * {@code acquisition-validate --root <dir> --manifest <path>}: the manifest is
 * parsed with the Python {@code json.loads} failure surface, the calibration
 * tree under {@code --root} is validated read-only, and the deterministic
 * {@code json.dumps}-style result is printed to stdout; exit 0 on
 * {@code VALIDATED}; exit 1 with the compact
 * {@code {"status": "ERROR", "error": ...}} JSON on stdout for caught
 * failures (validation violations, missing/unreadable/malformed manifest, the
 * Python {@code ValueError} vocabulary); exit 2 on usage errors.
 */
public final class AcquisitionCli {
    static final int MAX_INPUT_BYTES = 32 * 1024 * 1024;
    private static final String COMMAND = "acquisition-validate";
    private static final String USAGE =
            "usage: acquisition-validate --root <dir> --manifest <path>";

    private AcquisitionCli() { }

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
            byte[] data = readInput(Path.of(options.manifest()));
            JsonNode manifest = PythonJson.readTree(data);
            ObjectNode result = new AcquisitionValidator()
                    .validateAcquisition(Path.of(options.root()), manifest);
            stdout.println(render(result));
            return 0;
        } catch (UncheckedIOException unchecked) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(pythonErrorMessage(unchecked.getCause())) + "\"}");
            return 1;
        } catch (IllegalArgumentException | IllegalStateException | IOException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(pythonErrorMessage(failure)) + "\"}");
            return 1;
        }
    }

    private static byte[] readInput(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] data = stream.readNBytes(MAX_INPUT_BYTES + 1);
            if (data.length > MAX_INPUT_BYTES) {
                throw new IllegalArgumentException("input exceeds " + MAX_INPUT_BYTES
                        + " byte limit: '" + path + "'");
            }
            return data;
        }
    }

    /** Renders the result like Python {@code json.dumps(...)}: {@code ", "} and {@code ": "} separators. */
    static String render(JsonNode value) {
        StringBuilder out = new StringBuilder();
        renderInto(out, value);
        return out.toString();
    }

    private static void renderInto(StringBuilder out, JsonNode value) {
        if (value == null || value.isNull()) {
            out.append("null");
        } else if (value.isTextual()) {
            out.append('"').append(CodeBaselineResult.escape(value.asText())).append('"');
        } else if (value.isBoolean()) {
            out.append(value.asBoolean() ? "true" : "false");
        } else if (value.isNumber()) {
            out.append(value.bigIntegerValue().toString());
        } else if (value.isArray()) {
            out.append('[');
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) {
                    out.append(", ");
                }
                renderInto(out, value.get(index));
            }
            out.append(']');
        } else {
            out.append('{');
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                out.append('"').append(CodeBaselineResult.escape(field.getKey())).append("\": ");
                renderInto(out, field.getValue());
                if (fields.hasNext()) {
                    out.append(", ");
                }
            }
            out.append('}');
        }
    }

    /** Mirrors the Python {@code str(error)} text for the caught failure classes. */
    private static String pythonErrorMessage(Throwable failure) {
        if (failure instanceof NoSuchFileException missing) {
            return "[Errno 2] No such file or directory: '" + missing.getFile() + "'";
        }
        if (failure instanceof AccessDeniedException denied) {
            return "[Errno 13] Permission denied: '" + denied.getFile() + "'";
        }
        if (failure instanceof NotDirectoryException notDirectory) {
            return "[Errno 20] Not a directory: '" + notDirectory.getFile() + "'";
        }
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
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
                throw new CliArgumentsException("argument " + printable(option) + ": expected one argument");
            }
            if (!"--root".equals(option) && !"--manifest".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            values.put(option, value);
        }
        if (!values.containsKey("--root")) {
            throw new CliArgumentsException("the following arguments are required: --root");
        }
        if (!values.containsKey("--manifest")) {
            throw new CliArgumentsException("the following arguments are required: --manifest");
        }
        return new Options(values.get("--root"), values.get("--manifest"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String root, String manifest) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
