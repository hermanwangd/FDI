package com.featuredeliveryintelligence.fdi.reverse.evidence;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral reverse evidence bundle (PKB-BL-009 Slice A). Binds the
 * analyzed repository identity and exact canonical revision together with all
 * three required evidence channels — STRUCTURAL, TEST_BEHAVIOR, and
 * DELIVERY_HISTORY — each pinned by repository-relative input path, frozen
 * SHA-256 digest, schema version, and provider provenance.
 *
 * <p>Fail-closed validation runs at construction: a missing required channel,
 * a duplicate channel, identity disagreement across channels (repository id
 * or canonical revision), a malformed revision or digest, an escaped input
 * path, or an unsupported schema version raises {@link ReverseContractException}
 * with the stable {@link ReverseFailure} vocabulary. Digest <em>recomputation</em>
 * against the actual input bytes is a separate explicit step,
 * {@link #verifyInputDigests(ReverseEvidenceBundle, Path)}, so binding and
 * verification identities stay distinct.
 *
 * <p>Channels are exposed in {@link ReverseEvidenceChannel} declaration order;
 * no HashMap iteration order is ever observed.
 */
public record ReverseEvidenceBundle(
        String repositoryId,
        String canonicalRevision,
        String schemaVersion,
        List<EvidenceChannelRecord> channels) {

    /** Schema version this contract revision supports. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    public ReverseEvidenceBundle {
        repositoryId = ReverseContractValidation.requiredText(repositoryId, "bundle repository id");
        canonicalRevision = ReverseContractValidation.canonicalRevision(canonicalRevision);
        schemaVersion = ReverseContractValidation.supportedSchemaVersion(schemaVersion, SUPPORTED_SCHEMA_VERSION);
        if (channels == null || channels.isEmpty())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "evidence channels must not be empty");
        Map<ReverseEvidenceChannel, EvidenceChannelRecord> bound = new EnumMap<>(ReverseEvidenceChannel.class);
        for (EvidenceChannelRecord channel : channels) {
            if (channel == null)
                throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "channels must not contain null");
            if (bound.put(channel.channel(), channel) != null)
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, "duplicate evidence channel: " + channel.channel());
            if (!channel.repositoryId().equals(repositoryId))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY,
                        "channel " + channel.channel() + " repository identity disagrees with the bundle identity: "
                                + channel.repositoryId() + " != " + repositoryId);
            if (!channel.canonicalRevision().equals(canonicalRevision))
                throw new ReverseContractException(
                        ReverseFailure.REVISION_MISMATCH,
                        "channel " + channel.channel() + " binds revision " + channel.canonicalRevision()
                                + " but the bundle binds " + canonicalRevision);
        }
        for (ReverseEvidenceChannel required : ReverseEvidenceChannel.values()) {
            if (!bound.containsKey(required))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "required evidence channel is missing: " + required);
        }
        List<EvidenceChannelRecord> ordered = new ArrayList<>();
        for (ReverseEvidenceChannel channel : ReverseEvidenceChannel.values()) ordered.add(bound.get(channel));
        channels = List.copyOf(ordered);
    }

    /** The bound record for one required channel; never null. */
    public EvidenceChannelRecord channel(ReverseEvidenceChannel channel) {
        for (EvidenceChannelRecord record : channels) if (record.channel() == channel) return record;
        throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "required evidence channel is missing: " + channel);
    }

    /**
     * Recomputes the SHA-256 of every channel's bound input file under the
     * given repository root and compares it with the frozen digest. Any
     * unreadable or missing input raises {@link ReverseFailure#MISSING_INPUT};
     * any digest difference raises {@link ReverseFailure#DIGEST_MISMATCH}.
     * This method reads files only; it never writes and never follows paths
     * outside the repository root because channel paths are validated
     * traversal-free at construction.
     */
    public static void verifyInputDigests(ReverseEvidenceBundle bundle, Path repositoryRoot) {
        if (bundle == null || repositoryRoot == null || !Files.isDirectory(repositoryRoot))
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "repository root must be a readable directory");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        for (EvidenceChannelRecord channel : bundle.channels()) {
            Path input = root.resolve(channel.inputPath()).normalize();
            if (!input.startsWith(root) || !Files.isRegularFile(input))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "channel input is not a readable file: " + channel.inputPath());
            String recomputed = sha256(input);
            if (!recomputed.equals(channel.inputSha256()))
                throw new ReverseContractException(
                        ReverseFailure.DIGEST_MISMATCH,
                        "channel " + channel.channel() + " digest mismatch for " + channel.inputPath()
                                + ": bound " + channel.inputSha256() + " but recomputed " + recomputed);
        }
    }

    private static String sha256(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (IOException failure) {
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "channel input is unreadable: " + file);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }
}
