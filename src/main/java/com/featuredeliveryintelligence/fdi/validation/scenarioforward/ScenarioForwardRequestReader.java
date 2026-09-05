package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ScenarioForwardRequestReader {
    public static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Set<String> REQUEST_KEYS = Set.of("inputs", "proposal");
    private static final Set<String> INPUT_KEYS = Set.of("kind", "path", "sha256");
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build())
            .build());

    public ScenarioForwardRequest read(Path trustedRoot, Path requestPath) {
        if (requestPath == null) {
            throw new RuntimeContractException("request path must not be null");
        }
        Path root = validatedRoot(trustedRoot);
        if (!requestPath.isAbsolute() && !canonicalRelative(requestPath.toString())) {
            throw new RuntimeContractException("relative request path must be canonical");
        }
        Path target = requestPath.isAbsolute()
                ? requestPath.toAbsolutePath().normalize()
                : root.resolve(requestPath).normalize();
        if (!target.startsWith(root)) {
            throw new RuntimeContractException("request path must remain under trusted root");
        }
        byte[] bytes = readChecked(root, target);
        try {
            JsonNode document = JSON.readTree(bytes);
            if (document == null || !document.isObject()) {
                throw new RuntimeContractException("request root must be an object");
            }
            if (!fieldNames(document).equals(REQUEST_KEYS)) {
                throw new RuntimeContractException("request must contain exactly inputs and proposal");
            }
            JsonNode inputNodes = document.get("inputs");
            JsonNode proposal = document.get("proposal");
            if (!inputNodes.isArray() || proposal == null || proposal.isNull()) {
                throw new RuntimeContractException("request inputs must be an array and proposal must not be null");
            }
            var inputs = new java.util.ArrayList<ScenarioForwardRequest.BoundInput>();
            for (JsonNode input : inputNodes) {
                if (!input.isObject() || !fieldNames(input).equals(INPUT_KEYS)) {
                    throw new RuntimeContractException("bound input must contain exactly kind, path and sha256");
                }
                String relativePath = requiredText(input, "path");
                if (!canonicalRelative(relativePath)) {
                    throw new RuntimeContractException("bound input path must be canonical and relative");
                }
                inputs.add(new ScenarioForwardRequest.BoundInput(
                        requiredText(input, "kind"), relativePath, requiredText(input, "sha256")));
            }
            return new ScenarioForwardRequest(inputs, proposal);
        } catch (RuntimeContractException failure) {
            throw failure;
        } catch (IOException | IllegalArgumentException failure) {
            throw new RuntimeContractException("invalid scenario forward request JSON");
        }
    }

    public byte[] readBoundFile(Path trustedRoot, String relativePath) {
        if (!canonicalRelative(relativePath)) {
            throw new RuntimeContractException("bound input path must be canonical and relative");
        }
        Path root = validatedRoot(trustedRoot);
        return readChecked(root, root.resolve(relativePath));
    }

    public static String sha256(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static boolean canonicalRelative(String value) {
        if (value == null || value.isBlank() || value.startsWith("/") || value.contains("\\")
                || value.endsWith("/") || value.matches("^[A-Za-z]:.*")) {
            return false;
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static Path validatedRoot(Path trustedRoot) {
        if (trustedRoot == null) {
            throw new RuntimeContractException("trusted root must not be null");
        }
        Path root = trustedRoot.toAbsolutePath().normalize();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    root, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw new RuntimeContractException("trusted root must be an existing directory");
            }
            return root;
        } catch (IOException failure) {
            throw new RuntimeContractException("trusted root must be an existing directory");
        }
    }

    private static byte[] readChecked(Path root, Path target) {
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new RuntimeContractException("input path must remain under trusted root");
        }
        List<Path> components = new java.util.ArrayList<>();
        root.relativize(normalized).forEach(components::add);
        if (components.isEmpty()) {
            throw new RuntimeContractException("input must name a file under trusted root");
        }
        try (DirectoryStream<Path> openedRoot = Files.newDirectoryStream(root)) {
            if (openedRoot instanceof SecureDirectoryStream<Path> secureRoot) {
                return readFromDirectory(secureRoot, components, 0);
            }
            return readWithCheckedPaths(root, normalized);
        } catch (RuntimeContractException failure) {
            throw failure;
        } catch (IOException failure) {
            throw new RuntimeContractException("cannot safely read input file");
        }
    }

    private static byte[] readWithCheckedPaths(Path root, Path target) throws IOException {
        inspectComponents(root, target);
        BasicFileAttributes before = attributes(target);
        if (before.isSymbolicLink() || !before.isRegularFile() || before.size() > MAX_BYTES) {
            throw new RuntimeContractException("input must be a regular file no larger than 8 MiB");
        }
        try (SeekableByteChannel channel = Files.newByteChannel(target,
                Set.<OpenOption>of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            byte[] bytes = readBounded(channel, before.size());
            inspectComponents(root, target);
            BasicFileAttributes after = attributes(target);
            if (!sameFile(before, after) || after.isSymbolicLink() || !after.isRegularFile()
                    || after.size() != bytes.length) {
                throw new RuntimeContractException("input changed while reading");
            }
            return bytes;
        }
    }

    private static byte[] readFromDirectory(
            SecureDirectoryStream<Path> directory, List<Path> components, int index) throws IOException {
        Path component = components.get(index);
        BasicFileAttributes attributes = directory.getFileAttributeView(
                component, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
        if (attributes.isSymbolicLink()) {
            throw new RuntimeContractException("symbolic links are not allowed in input paths");
        }
        if (index < components.size() - 1) {
            if (!attributes.isDirectory()) {
                throw new RuntimeContractException("input path component must be a directory");
            }
            try (SecureDirectoryStream<Path> child = directory.newDirectoryStream(
                    component, LinkOption.NOFOLLOW_LINKS)) {
                return readFromDirectory(child, components, index + 1);
            }
        }
        return readFinalFile(directory, component, attributes);
    }

    private static byte[] readFinalFile(
            SecureDirectoryStream<Path> directory, Path component, BasicFileAttributes before) throws IOException {
        if (!before.isRegularFile() || before.size() > MAX_BYTES) {
            throw new RuntimeContractException("input must be a regular file no larger than 8 MiB");
        }
        try (SeekableByteChannel channel = directory.newByteChannel(component,
                Set.<OpenOption>of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            byte[] bytes = readBounded(channel, before.size());
            BasicFileAttributes after = directory.getFileAttributeView(
                    component, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
            if (!sameFile(before, after) || after.isSymbolicLink() || !after.isRegularFile()
                    || after.size() != bytes.length) {
                throw new RuntimeContractException("input changed while reading");
            }
            return bytes;
        }
    }

    private static byte[] readBounded(SeekableByteChannel channel, long expectedSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(expectedSize, MAX_BYTES));
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        long total = 0;
        while (channel.read(buffer) != -1) {
            buffer.flip();
            int count = buffer.remaining();
            total += count;
            if (total > MAX_BYTES) {
                throw new RuntimeContractException("input exceeds 8 MiB while reading");
            }
            output.write(buffer.array(), buffer.position(), count);
            buffer.clear();
        }
        return output.toByteArray();
    }

    private static void inspectComponents(Path root, Path target) {
        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            BasicFileAttributes attributes = attributes(current);
            if (attributes.isSymbolicLink()) {
                throw new RuntimeContractException("symbolic links are not allowed in input paths");
            }
            if (!current.equals(target) && !attributes.isDirectory()) {
                throw new RuntimeContractException("input path component must be a directory");
            }
        }
    }

    private static BasicFileAttributes attributes(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException failure) {
            throw new RuntimeContractException("input path is missing or unreadable");
        }
    }

    private static boolean sameFile(BasicFileAttributes before, BasicFileAttributes after) {
        Object beforeKey = before.fileKey();
        Object afterKey = after.fileKey();
        return beforeKey != null && afterKey != null
                ? Objects.equals(beforeKey, afterKey)
                : before.creationTime().equals(after.creationTime());
    }

    private static Set<String> fieldNames(JsonNode node) {
        var names = new java.util.HashSet<String>();
        node.fieldNames().forEachRemaining(names::add);
        return Set.copyOf(names);
    }

    private static String requiredText(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual()) {
            throw new RuntimeContractException("bound input " + name + " must be a string");
        }
        return value.textValue();
    }
}
