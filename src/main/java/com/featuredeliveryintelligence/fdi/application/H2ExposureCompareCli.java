package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.product.realization.contamination.v1.H2ExposureComparator;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Standalone CLI: --input FILE --output ABSENT_FILE. No workspace mutation beyond the new report. */
public final class H2ExposureCompareCli {
    private H2ExposureCompareCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("usage: --input FILE --output ABSENT_FILE");
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (!Set.of("--input", "--output").contains(args[i]) || args[i+1].isBlank()
                    || options.put(args[i], args[i+1]) != null) throw new IllegalArgumentException("invalid arguments");
        }
        Path input = Path.of(options.get("--input")).toAbsolutePath().normalize();
        Path output = Path.of(options.get("--output")).toAbsolutePath().normalize();
        rejectSymlinks(input); rejectSymlinks(output);
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("output must be absent");
        BasicFileAttributes before = Files.readAttributes(input, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() > H2ExposureComparator.MAX_INPUT_BYTES) throw new IllegalArgumentException("input must be bounded regular file");
        byte[] bytes;
        try (SeekableByteChannel channel = Files.newByteChannel(input, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(); ByteBuffer chunk = ByteBuffer.allocate(8192);
            while (channel.read(chunk) != -1) {
                chunk.flip();
                if (buffer.size() + chunk.remaining() > H2ExposureComparator.MAX_INPUT_BYTES) throw new IllegalArgumentException("input exceeds 4 MiB");
                buffer.write(chunk.array(), 0, chunk.remaining()); chunk.clear();
            }
            bytes = buffer.toByteArray();
        }
        BasicFileAttributes after = Files.readAttributes(input, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.fileKey() == null || !before.fileKey().equals(after.fileKey())) throw new IllegalArgumentException("input changed during read");
        byte[] report = new H2ExposureComparator().compare(bytes);
        rejectSymlinks(output);
        try (SeekableByteChannel channel = Files.newByteChannel(output, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
            ByteBuffer buffer = ByteBuffer.wrap(report); while (buffer.hasRemaining()) channel.write(buffer);
        }
    }

    private static void rejectSymlinks(Path path) throws Exception {
        Path cursor = path.getRoot();
        for (Path component : path) {
            cursor = cursor.resolve(component);
            // macOS temporary directories use the system-owned /var -> /private/var alias.
            if (Files.isSymbolicLink(cursor) && !(cursor.equals(Path.of("/var")) && cursor.toRealPath().equals(Path.of("/private/var"))))
                throw new IllegalArgumentException("symlinks are forbidden");
        }
    }
}
