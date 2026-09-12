package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Bounded, strict snapshot reads specific to the evaluator trust boundary. */
final class MethodPairArtifactIO {
    static final int MAX_BYTES = 2 * 1024 * 1024;
    static final JsonMapper JSON = JsonMapper.builder(JsonFactory.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build()).build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private MethodPairArtifactIO() { }

    static <T> T parse(byte[] bytes, Class<T> type) {
        try {
            T result = JSON.readValue(bytes, type);
            require(result != null, "NULL_DOCUMENT");
            return result;
        } catch (IOException failure) {
            throw new IllegalArgumentException("INVALID_INPUT_SCHEMA", failure);
        }
    }

    static byte[] read(Path root, String relative, String expectedDigest) throws IOException {
        path(relative);
        require(expectedDigest != null && expectedDigest.matches("[a-f0-9]{64}"), "INVALID_DIGEST");
        Path resolved = root.resolve(relative);
        Path current = root;
        for (Path part : Path.of(relative)) {
            current = current.resolve(part);
            require(!Files.isSymbolicLink(current), "SYMLINK_INPUT");
        }
        require(Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)
                && resolved.toRealPath().startsWith(root), "INVALID_INPUT_PATH");
        byte[] bytes;
        try (var input = Files.newInputStream(resolved, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        require(bytes.length <= MAX_BYTES, "INPUT_TOO_LARGE");
        require(sha(bytes).equals(expectedDigest), "DIGEST_MISMATCH");
        return bytes;
    }

    static void path(String path) {
        require(path != null && !path.isBlank() && !path.contains("\\") && !path.contains(":")
                && !path.chars().anyMatch(Character::isISOControl), "INVALID_PATH");
        require(!Path.of(path).isAbsolute(), "ABSOLUTE_PATH");
        for (String segment : path.split("/", -1)) {
            require(!segment.isBlank() && !segment.equals(".") && !segment.equals(".."), "NONCANONICAL_PATH");
        }
    }

    static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static void require(boolean condition, String reason) {
        if (!condition) {
            throw new IllegalArgumentException(reason);
        }
    }
}
