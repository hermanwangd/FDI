package com.featuredeliveryintelligence.fdi.reverse.generator;

import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.proposal.CapabilityProposal;
import com.featuredeliveryintelligence.fdi.reverse.proposal.EvidenceCitation;
import com.featuredeliveryintelligence.fdi.reverse.proposal.EvidenceGap;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ReverseProposalPackage;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioProposal;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Byte-stable serializer of one sealed {@link ReverseProposalPackage} into the
 * provider-neutral {@code contracts/reverse-proposal.schema.json} document
 * (PKB-BL-009 Slice E). Emission goes through {@link ReverseJson} so repeated
 * serialization of an equal package is byte-identical: object keys are sorted
 * at every depth, indentation is fixed, and no wall-clock, locale, or
 * HashMap-order dependence exists. The output document remains proposal-only;
 * nothing here can publish Product semantics.
 */
public final class ReverseProposalPackageJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ReverseProposalPackageJson() { }

    /** Serializes the package to canonical bytes; equal packages serialize byte-identically. */
    public static byte[] write(ReverseProposalPackage proposalPackage) {
        return ReverseJson.write(toJson(proposalPackage));
    }

    /** Maps one package to the schema-shaped JSON document without writing it. */
    public static ObjectNode toJson(ReverseProposalPackage proposalPackage) {
        if (proposalPackage == null)
            throw new IllegalArgumentException("proposal package must not be null");
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", ReverseProposalPackage.SUPPORTED_SCHEMA_VERSION);
        document.put("repository_id", proposalPackage.repositoryId());
        document.put("canonical_revision", proposalPackage.canonicalRevision());
        ArrayNode capabilities = document.putArray("capabilities");
        for (CapabilityProposal capability : proposalPackage.capabilities()) {
            ObjectNode node = capabilities.addObject();
            node.put("id", capability.id());
            node.put("title", capability.title());
            node.set("citations", citations(capability.citations()));
            node.put("source_revision", capability.sourceRevision());
            ArrayNode contributing = node.putArray("contributing_evidence_digests");
            capability.contributingEvidenceDigests().forEach(contributing::add);
            node.put("inference_rationale", capability.inferenceRationale());
            ArrayNode limitations = node.putArray("limitations");
            capability.limitations().forEach(limitations::add);
            node.put("confidence", capability.confidenceScale4());
            node.put("authority", capability.authority().name());
            node.put("semantic_publication_allowed", capability.semanticPublicationAllowed());
        }
        ArrayNode scenarios = document.putArray("scenarios");
        for (ScenarioProposal scenario : proposalPackage.scenarios()) {
            ObjectNode node = scenarios.addObject();
            node.put("id", scenario.id());
            node.put("capability_id", scenario.capabilityId());
            node.put("title", scenario.title());
            node.put("given", scenario.given());
            node.put("when", scenario.when());
            node.put("then", scenario.then());
            node.set("citations", citations(scenario.citations()));
            node.put("source_revision", scenario.sourceRevision());
            ArrayNode contributing = node.putArray("contributing_evidence_digests");
            scenario.contributingEvidenceDigests().forEach(contributing::add);
            node.put("inference_rationale", scenario.inferenceRationale());
            ArrayNode limitations = node.putArray("limitations");
            scenario.limitations().forEach(limitations::add);
            node.put("confidence", scenario.confidenceScale4());
            node.put("scenario_status", scenario.scenarioStatus().name());
            node.put("authority", scenario.authority().name());
            node.put("semantic_publication_allowed", scenario.semanticPublicationAllowed());
        }
        ArrayNode gaps = document.putArray("evidence_gaps");
        for (EvidenceGap gap : proposalPackage.evidenceGaps()) {
            ObjectNode node = gaps.addObject();
            node.put("channel", gap.channel().name());
            node.put("observation_ref", gap.observationRef());
            node.put("evidence_digest", gap.evidenceDigest());
            node.put("reason", gap.reason());
        }
        return document;
    }

    private static ArrayNode citations(java.util.List<EvidenceCitation> citations) {
        ArrayNode nodes = JSON.createArrayNode();
        for (EvidenceCitation citation : citations) {
            ObjectNode node = nodes.addObject();
            node.put("channel", citation.channel().name());
            node.put("observation_ref", citation.observationRef());
            node.put("evidence_digest", citation.evidenceDigest());
        }
        return nodes;
    }
}
