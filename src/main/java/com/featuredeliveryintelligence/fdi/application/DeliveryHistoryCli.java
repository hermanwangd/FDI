package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.deliveryhistory.DeliveryHistory;
import com.featuredeliveryintelligence.fdi.validation.deliveryhistory.DeliveryHistoryResult;
import com.featuredeliveryintelligence.fdi.validation.deliveryhistory.GitCommandException;
import com.featuredeliveryintelligence.fdi.validation.deliveryhistory.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 delivery-history reconstruction. Ports the
 * observable contract of the transitional Python CLI
 * {@code pkb001_history.py}: {@code --repo <path> --source-sha <sha> --cutoff
 * <iso> --prs <path> --output <path>}; the PRs document is read as strict
 * JSON and a single non-list document is wrapped into a one-element list; the
 * output is written to the (created) parent directory; exit 0 on success;
 * exit 1 with the compact {@code {"status": "ERROR", "error": ...}} JSON on
 * stdout for caught failures (OSError, ValueError, JSONDecodeError,
 * CalledProcessError); exit 2 with usage on argument errors.
 */
public final class DeliveryHistoryCli {
    static final int MAX_PRS_BYTES = 32 * 1024 * 1024;
    private static final String COMMAND = "delivery-history-generate";
    private static final String USAGE = "usage: delivery-history-generate --repo REPO --source-sha SOURCE_SHA"
            + " --cutoff CUTOFF --prs PRS --output PATH";

    private DeliveryHistoryCli() { }

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
            JsonNode raw = PythonJson.readTree(readBytes(Path.of(options.prs())));
            List<JsonNode> prs = new ArrayList<>();
            if (raw != null && raw.isArray()) {
                raw.forEach(prs::add);
            } else {
                prs.add(raw);
            }
            JsonNode result = new DeliveryHistory().reconstruct(
                    Path.of(options.repo()), options.sourceSha(), options.cutoff(), prs);
            Path output = Path.of(options.output());
            Path parent = output.getParent() == null ? Path.of(".") : output.getParent();
            Files.createDirectories(parent);
            Files.write(output, new DeliveryHistoryResult(result).toJsonBytes());
            return 0;
        } catch (GitCommandException | IllegalArgumentException | IOException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + DeliveryHistoryResult.escape(pythonErrorMessage(failure)) + "\"}");
            return 1;
        }
    }

    private static byte[] readBytes(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] data = stream.readNBytes(MAX_PRS_BYTES + 1);
            if (data.length > MAX_PRS_BYTES) {
                throw new IllegalArgumentException("pull-request input exceeds " + MAX_PRS_BYTES
                        + " byte limit: '" + path + "'");
            }
            return data;
        }
    }

    /** Mirrors the Python {@code str(error)} text for the caught failure classes. */
    private static String pythonErrorMessage(Exception failure) {
        if (failure instanceof NoSuchFileException missing) {
            return "[Errno 2] No such file or directory: '" + missing.getFile() + "'";
        }
        if (failure instanceof NotDirectoryException notDirectory) {
            // subprocess carries the cwd Path object, rendered as PosixPath('...').
            return "[Errno 20] Not a directory: PosixPath('" + notDirectory.getFile() + "')";
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
            if (!"--repo".equals(option) && !"--source-sha".equals(option) && !"--cutoff".equals(option)
                    && !"--prs".equals(option) && !"--output".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (values.put(option, value) != null) {
                throw new CliArgumentsException("duplicate option " + option);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String option : new String[] {"--repo", "--source-sha", "--cutoff", "--prs", "--output"}) {
            if (!values.containsKey(option)) {
                missing.add(option);
            }
        }
        if (!missing.isEmpty()) {
            throw new CliArgumentsException("the following arguments are required: " + String.join(", ", missing));
        }
        return new Options(values.get("--repo"), values.get("--source-sha"), values.get("--cutoff"),
                values.get("--prs"), values.get("--output"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String repo, String sourceSha, String cutoff, String prs, String output) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
