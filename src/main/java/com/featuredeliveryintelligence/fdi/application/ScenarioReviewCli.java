package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.scenarioreview.ScenarioReview;
import com.featuredeliveryintelligence.fdi.validation.scenarioreview.ScenarioReviewException;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 scenario proposal review renderer. Ports the
 * observable contract of the transitional Python CLI
 * {@code pkb001_scenario_review.py}: success prints one compact ASCII JSON
 * line ({@code {"status": "RENDERED", ...}}) and exits 0; a
 * {@link ScenarioReviewException} prints
 * {@code {"status": "BLOCKED", "reasons": [...]}} on stderr and exits 1;
 * argparse-style usage errors exit 2 with a usage line on stderr;
 * unexpected failures print a stack trace and exit 1.
 */
public final class ScenarioReviewCli {
    private static final String COMMAND = "scenario-review-render";
    private static final String USAGE = "usage: " + COMMAND
            + " [-h] --root ROOT --proposal PROPOSAL --json-output JSON_OUTPUT"
            + " --markdown-output MARKDOWN_OUTPUT";

    private ScenarioReviewCli() { }

    public static void main(String[] args) {
        if (handles(args)) {
            System.exit(run(args, System.out, System.err));
        }
        System.err.println(USAGE);
        System.exit(2);
    }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Map<String, String> options;
        try {
            options = parse(args);
        } catch (CliArgumentsException | InvalidPathException failure) {
            stderr.println(USAGE);
            stderr.println(COMMAND + ": error: " + failure.getMessage());
            return 2;
        }

        try {
            ScenarioReview.OutputPaths paths = ScenarioReview.writeReviewOutputs(
                    Path.of(options.get("--root")),
                    Path.of(options.get("--proposal")),
                    Path.of(options.get("--json-output")),
                    Path.of(options.get("--markdown-output")));
            stdout.println("{\"status\": \"RENDERED\", \"json\": \""
                    + escape(paths.json().toString()) + "\", \"markdown\": \""
                    + escape(paths.markdown().toString()) + "\"}");
            return 0;
        } catch (ScenarioReviewException failure) {
            stderr.println("{\"status\": \"BLOCKED\", \"reasons\": "
                    + reasonsJson(failure.getReasons()) + "}");
            return 1;
        } catch (RuntimeException failure) {
            failure.printStackTrace(stderr);
            return 1;
        }
    }

    private static Map<String, String> parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            String value = null;
            int equalsAt = option.indexOf('=');
            if (option.startsWith("--") && equalsAt > 2) {
                value = option.substring(equalsAt + 1);
                option = option.substring(0, equalsAt);
            } else if (option.startsWith("--") && index + 1 < args.length) {
                value = args[++index];
            } else {
                positional.add(option);
                continue;
            }
            if (!"--root".equals(option) && !"--proposal".equals(option)
                    && !"--json-output".equals(option) && !"--markdown-output".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            if (value == null || value.isEmpty()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        if (!positional.isEmpty()) {
            throw new CliArgumentsException("unrecognized arguments: " + String.join(" ", positional));
        }
        List<String> missing = new ArrayList<>();
        for (String required : List.of("--root", "--proposal", "--json-output", "--markdown-output")) {
            if (!values.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new CliArgumentsException(
                    "the following arguments are required: " + String.join(", ", missing));
        }
        return values;
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private static String reasonsJson(List<String> reasons) {
        StringBuilder out = new StringBuilder("[");
        for (int index = 0; index < reasons.size(); index++) {
            if (index > 0) {
                out.append(", ");
            }
            out.append('"').append(escape(reasons.get(index))).append('"');
        }
        return out.append(']').toString();
    }

    /** Python {@code json.dumps} ASCII string escaping (ensure_ascii=True). */
    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else if (character < 0x80) {
                        out.append(character);
                    } else if (Character.isHighSurrogate(character)
                            && index + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(index + 1))) {
                        int codePoint = Character.toCodePoint(character, value.charAt(index + 1));
                        index++;
                        out.append(String.format("\\u%04x\\u%04x",
                                (int) (char) (codePoint >> 10 | 0xd800),
                                (int) (char) (codePoint & 0x3ff | 0xdc00)));
                    } else if (Character.isSurrogate(character)) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(String.format("\\u%04x", (int) character));
                    }
                }
            }
        }
        return out.toString();
    }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
