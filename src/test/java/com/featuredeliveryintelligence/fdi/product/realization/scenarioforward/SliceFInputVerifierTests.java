package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SliceFInputVerifierTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPO = Path.of("").toAbsolutePath();
    private static final String FOLDER = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/";
    @TempDir Path root;

    @Test void verifiesExactReviewedChainThroughTheExistingForwardGate() throws Exception {
        copyInputs();
        assertThat(SliceFInputVerifier.verify(root).snapshotId()).isEqualTo("pkb001-petclinic-reviewed-semantics-004");
    }

    @Test void rejectsWrongBytesDigestPathParentAndAcceptedSetMutations() throws Exception {
        for (String mutation : List.of("bytes", "digest", "path", "parent", "accepted-set")) {
            reset();
            if (mutation.equals("bytes")) Files.writeString(root.resolve(FOLDER + "accepted-semantics-004.json"), "\n", StandardOpenOption.APPEND);
            else if (mutation.equals("digest")) mutate(FOLDER + "acceptance-manifest-004.json", value ->
                    value.with("semantics_artifact").put("sha256", "0".repeat(64)));
            else if (mutation.equals("path")) mutate(FOLDER + "acceptance-manifest-004.json", value ->
                    value.with("semantics_artifact").put("path", "wrong.json"));
            else if (mutation.equals("parent")) mutate(FOLDER + "accepted-semantics-004.json", value ->
                    ((ObjectNode)value.withArray("capabilities").get(0)).put("capability_id", "HYP-CAPABILITY-X"));
            else mutate(FOLDER + "acceptance-manifest-004.json", value -> value.withArray("accepted_scenario_ids").remove(0));
            assertThatThrownBy(() -> SliceFInputVerifier.verify(root)).isInstanceOf(RuntimeContractException.class)
                    .hasMessageContaining("exact input digest mismatch");
        }
    }

    private void mutate(String path, java.util.function.Consumer<ObjectNode> mutation) throws Exception {
        Path file = root.resolve(path); ObjectNode value = (ObjectNode) JSON.readTree(file.toFile()); mutation.accept(value);
        JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
    }
    private void reset() throws Exception {
        if (Files.exists(root)) try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).filter(path -> !path.equals(root)).forEach(path -> {
                try { Files.delete(path); } catch (Exception error) { throw new RuntimeException(error); }
            });
        }
        copyInputs();
    }
    private void copyInputs() throws Exception {
        for (String path : List.of(FOLDER + "accepted-semantics-004.json", FOLDER + "acceptance-manifest-004.json",
                FOLDER + "review-decisions-004.json", FOLDER + "proposal-revision-002.json",
                FOLDER + "freeze-authorization-binding-001.json", FOLDER + "ai-provisional-review-acceptance-001.json",
                FOLDER + "ai-provisional-review-001.json", "validation/pkb001/runtime/graphify-petclinic-live-evidence.json",
                "validation/pkb001/artifacts/petclinic-graph-818c413.json",
                "validation/pkb001/schemas/realization-proposal-v0.3.schema.json",
                "skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md")) {
            Path target = root.resolve(path); Files.createDirectories(target.getParent()); Files.copy(REPO.resolve(path), target,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        run("git", "-C", root.toString(), "init", "-q"); run("git", "-C", root.toString(), "-c", "user.name=Fixture", "-c",
                "user.email=fixture@example.test", "commit", "--allow-empty", "-qm", "fixture");
    }
    private void run(String... command) throws Exception { assertThat(new ProcessBuilder(command).start().waitFor()).isZero(); }
}
