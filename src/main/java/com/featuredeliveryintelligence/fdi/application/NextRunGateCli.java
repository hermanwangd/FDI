package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.nextrun.NextRunGate;
import com.featuredeliveryintelligence.fdi.validation.nextrun.NextRunReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Packaged CLI for the PKB-001 v0.2 next-run readiness gate. Ports the
 * observable contract of the transitional Python CLI
 * {@code pkb001_next_run_gate.py}: {@code --root <dir> --request <path>
 * --report <path>}; the deterministic report is exclusively created through a
 * canonical repository-relative path with directory-fd semantics (mkdir
 * parents, no symlink following, {@code O_CREAT|O_EXCL}); exit 0 on
 * {@code READY}, 1 on {@code BLOCKED} or any report-path/write failure, and 2
 * on usage errors.
 */
public final class NextRunGateCli {
    private static final String COMMAND = "next-run-validate";
    private static final String USAGE =
            "usage: next-run-validate [--root <dir>] --request <path> --report <path>";
    private static final ObjectMapper JSON = new ObjectMapper();

    private NextRunGateCli() { }

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

        Path root = options.root();
        Path rootReal = realPathOr(root, root);
        JsonNode request = null;
        Path requestPath = resolveRequestFile(root, rootReal, options.request());
        if (requestPath != null) {
            try {
                request = JSON.readTree(Files.readAllBytes(requestPath));
            } catch (IOException | RuntimeException failure) {
                request = null;
            }
        }
        NextRunReport report = new NextRunGate().validate(root, request);
        if (NextRunGate.canonicalRelative(options.report()) == null) {
            stderr.println("report path must be canonical and repository-relative");
            return 1;
        }
        try {
            writeReport(root, options.report(), report.toJsonBytes());
        } catch (IOException | RuntimeException failure) {
            stderr.println("cannot exclusively create report: " + failure.getMessage());
            return 1;
        }
        return "READY".equals(report.status()) ? 0 : 1;
    }

    private static Path resolveRequestFile(Path root, Path rootReal, String relative) {
        if (NextRunGate.canonicalRelative(relative) == null) {
            return null;
        }
        Path candidate = root.resolve(relative);
        Path resolved;
        try {
            resolved = candidate.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return null;
        }
        if (!resolved.startsWith(rootReal)) {
            return null;
        }
        try {
            if (Files.isSymbolicLink(candidate) || !Files.isRegularFile(resolved)) {
                return null;
            }
        } catch (SecurityException failure) {
            return null;
        }
        return resolved;
    }

    /**
     * Writes the report like the Python dir-fd walk: creates each missing
     * parent with 0755, refuses symlinked directory components like
     * {@code O_NOFOLLOW}, and exclusively creates the final file (0644,
     * {@code O_CREAT|O_EXCL|O_NOFOLLOW}) through a secure directory stream
     * when the platform provides one.
     */
    private static void writeReport(Path root, String relative, byte[] content) throws IOException {
        writeAt(root, relative.split("/", -1), content, 0);
    }

    private static void writeAt(Path directory, String[] parts, byte[] content, int index)
            throws IOException {
        String part = parts[index];
        Path current = directory.resolve(part);
        if (index < parts.length - 1) {
            try {
                Files.createDirectory(current, directoryAttributes());
            } catch (FileAlreadyExistsException ignored) {
                // existing parent directory is reused like the Python walk
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw new FileSystemException(current.toString(), null, "not a directory");
            }
            writeAt(current, parts, content, index + 1);
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            if (stream instanceof SecureDirectoryStream<Path> secure) {
                try (SeekableByteChannel channel = secure.newByteChannel(Path.of(part),
                        Set.<OpenOption>of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                                LinkOption.NOFOLLOW_LINKS),
                        fileAttributes())) {
                    writeFully(channel, content);
                }
            } else {
                try (SeekableByteChannel channel = Files.newByteChannel(current,
                        Set.<OpenOption>of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                                LinkOption.NOFOLLOW_LINKS),
                        fileAttributes())) {
                    writeFully(channel, content);
                }
            }
        }
    }

    private static void writeFully(SeekableByteChannel channel, byte[] content) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static FileAttribute<?>[] directoryAttributes() {
        try {
            return new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")) };
        } catch (UnsupportedOperationException nonPosix) {
            return new FileAttribute<?>[0];
        }
    }

    private static FileAttribute<?>[] fileAttributes() {
        try {
            return new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")) };
        } catch (UnsupportedOperationException nonPosix) {
            return new FileAttribute<?>[0];
        }
    }

    private static Path realPathOr(Path path, Path fallback) {
        try {
            return path.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return fallback;
        }
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            String value = null;
            int equalsAt = option.indexOf('=');
            if (option.startsWith("--") && equalsAt > 2) {
                value = option.substring(equalsAt + 1);
                option = option.substring(0, equalsAt);
            } else if (index + 1 < args.length) {
                value = args[++index];
            }
            if (!"--root".equals(option) && !"--request".equals(option) && !"--report".equals(option)) {
                throw new CliArgumentsException("unknown option " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            if (value == null || value.isBlank()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        if (!values.containsKey("--request")) {
            throw new CliArgumentsException("the following arguments are required: --request");
        }
        if (!values.containsKey("--report")) {
            throw new CliArgumentsException("the following arguments are required: --report");
        }
        Path root = Path.of(values.getOrDefault("--root", ".")).toAbsolutePath().normalize();
        return new Options(root, values.get("--request"), values.get("--report"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path root, String request, String report) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
