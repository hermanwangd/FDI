package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioTextValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only view of the sealed reverse proposal package for evaluator-only
 * comparison (PKB-BL-009 slice F). This class consumes the byte-stable
 * document produced by
 * {@link com.featuredeliveryintelligence.fdi.reverse.generator.ReverseProposalPackageJson}
 * and re-validates every authority, revision, and digest invariant before any
 * comparison runs. It is part of the evaluation path only: generation code
 * must never load a sealed package, and this class never reads evaluator
 * truth.
 *
 * <p>Fail-closed invariants: schema version {@code 1}; repository id and full
 * Git canonical revision; every Capability is {@code HYP-CAPABILITY-*} with
 * {@code authority == PROPOSAL_ONLY} and {@code semantic_publication_allowed
 * == false}; every Scenario is {@code HYP-SCENARIO-*}, references an existing
 * sealed Capability, and stays {@code UNREVIEWED}; every citation carries a
 * known evidence channel, a non-blank observation reference, and a SHA-256
 * evidence digest. The digest-to-channel binding against the accepted
 * evidence bundle is enforced separately by
 * {@link ReverseProposalComparator}.
 */
public record ReverseSealedPackage(
        String repositoryId,
        String canonicalRevision,
        List<SealedCapability> capabilities,
        List<SealedScenario> scenarios) {

    /** Supported sealed-package schema version. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    private static final ObjectMapper JSON = new ObjectMapper();

    /** One sealed citation: evidence channel, observation reference, and input digest. */
    public record SealedCitation(String channel, String observationRef, String evidenceDigest) {

        public SealedCitation {
            channel = ReverseContractValidation.requiredText(channel, "sealed citation channel");
            observationRef = ReverseContractValidation.requiredText(
                    observationRef, "sealed citation observation reference");
            evidenceDigest = ReverseContractValidation.sha256Hex(
                    evidenceDigest, "sealed citation " + observationRef);
        }

        /** Distinct channels cited by the given citations, sorted by name. */
        static Set<String> channels(List<SealedCitation> citations) {
            Set<String> channels = new TreeSet<>();
            for (SealedCitation citation : citations) channels.add(citation.channel());
            return channels;
        }
    }

    /** One sealed Capability proposal as seen by the evaluator. */
    public record SealedCapability(String id, String title, List<SealedCitation> citations) {

        public SealedCapability {
            id = ReverseContractValidation.requiredText(id, "sealed capability id");
            title = ReverseContractValidation.requiredText(title, "sealed capability title");
            citations = List.copyOf(citations == null ? List.of() : citations);
        }

        /** Distinct evidence channels cited by this Capability, sorted by name. */
        public Set<String> channels() {
            return SealedCitation.channels(citations);
        }
    }

    /** One sealed Behavior Scenario proposal as seen by the evaluator. */
    public record SealedScenario(
            String id, String capabilityId, String title, String when, List<SealedCitation> citations) {

        public SealedScenario {
            id = ReverseContractValidation.requiredText(id, "sealed scenario id");
            capabilityId = ReverseContractValidation.requiredText(
                    capabilityId, "sealed scenario capability reference");
            title = ReverseContractValidation.requiredText(title, "sealed scenario title");
            when = ReverseContractValidation.requiredText(when, "sealed scenario when");
            citations = List.copyOf(citations == null ? List.of() : citations);
        }

        /** Distinct evidence channels cited by this Scenario, sorted by name. */
        public Set<String> channels() {
            return SealedCitation.channels(citations);
        }
    }

    public ReverseSealedPackage {
        repositoryId = ReverseContractValidation.requiredText(repositoryId, "sealed repository id");
        canonicalRevision = ReverseContractValidation.canonicalRevision(canonicalRevision);
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        scenarios = List.copyOf(scenarios == null ? List.of() : scenarios);
    }

    /**
     * Parses and re-validates a sealed proposal package document. Any
     * authority violation, unsupported schema version, malformed revision or
     * digest, identifier-bearing scenario text, or dangling scenario
     * reference fails closed with the stable {@link ReverseFailure} vocabulary.
     */
    public static ReverseSealedPackage load(byte[] document) {
        if (document == null || document.length == 0)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "sealed proposal package document must not be empty");

        JsonNode root;
        try {
            root = JSON.readTree(document);
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "sealed proposal package must be parseable JSON");
        }
        if (!root.isObject())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "sealed proposal package must be a JSON object");

        ReverseContractValidation.supportedSchemaVersion(
                text(root, "schema_version", "sealed package schema version"),
                SUPPORTED_SCHEMA_VERSION);
        String repositoryId = ReverseContractValidation.requiredText(
                text(root, "repository_id", "sealed package repository id"),
                "sealed package repository id");
        String canonicalRevision = ReverseContractValidation.canonicalRevision(
                text(root, "canonical_revision", "sealed package canonical revision"));

        JsonNode capabilityNodes = requiredArray(root, "capabilities");
        JsonNode scenarioNodes = requiredArray(root, "scenarios");

        List<SealedCapability> capabilities = new ArrayList<>();
        Set<String> capabilityIds = new TreeSet<>();
        for (int index = 0; index < capabilityNodes.size(); index++) {
            JsonNode node = capabilityNodes.get(index);
            String where = "sealed capability at /capabilities/" + index;
            if (!node.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, where + " must be an object");

            String id = requiredText(node, "id", where);
            if (!id.matches("HYP-CAPABILITY-[0-9]{4}"))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, where + " has a non-proposal capability id: " + id);
            if (!capabilityIds.add(id))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, where + " duplicates capability id " + id);

            String title = requiredText(node, "title", where);
            requireCleanText(title, where + " title");

            requireAuthority(node, where);
            String sourceRevision = ReverseContractValidation.canonicalRevision(
                    requiredText(node, "source_revision", where));
            if (!canonicalRevision.equals(sourceRevision))
                throw new ReverseContractException(
                        ReverseFailure.REVISION_MISMATCH,
                        where + " source revision " + sourceRevision
                                + " disagrees with package revision " + canonicalRevision);

            capabilities.add(new SealedCapability(id, title, citations(node, where)));
        }

        List<SealedScenario> scenarios = new ArrayList<>();
        Set<String> scenarioIds = new TreeSet<>();
        for (int index = 0; index < scenarioNodes.size(); index++) {
            JsonNode node = scenarioNodes.get(index);
            String where = "sealed scenario at /scenarios/" + index;
            if (!node.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, where + " must be an object");

            String id = requiredText(node, "id", where);
            if (!id.matches("HYP-SCENARIO-[0-9]{4}"))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, where + " has a non-proposal scenario id: " + id);
            if (!scenarioIds.add(id))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, where + " duplicates scenario id " + id);

            String capabilityId = requiredText(node, "capability_id", where);
            if (!capabilityIds.contains(capabilityId))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        where + " references capability " + capabilityId + " that is not sealed");

            String title = requiredText(node, "title", where);
            requireCleanText(title, where + " title");
            requireCleanText(requiredText(node, "given", where), where + " given");
            String when = requiredText(node, "when", where);
            requireCleanText(when, where + " when");
            requireCleanText(requiredText(node, "then", where), where + " then");

            String status = requiredText(node, "scenario_status", where);
            if (!"UNREVIEWED".equals(status))
                throw new ReverseContractException(
                        ReverseFailure.AUTHORITY_VIOLATION,
                        where + " scenario_status must remain UNREVIEWED but was " + status);
            requireAuthority(node, where);
            String sourceRevision = ReverseContractValidation.canonicalRevision(
                    requiredText(node, "source_revision", where));
            if (!canonicalRevision.equals(sourceRevision))
                throw new ReverseContractException(
                        ReverseFailure.REVISION_MISMATCH,
                        where + " source revision " + sourceRevision
                                + " disagrees with package revision " + canonicalRevision);

            scenarios.add(new SealedScenario(id, capabilityId, title, when, citations(node, where)));
        }

        return new ReverseSealedPackage(repositoryId, canonicalRevision, capabilities, scenarios);
    }

    private static void requireAuthority(JsonNode node, String where) {
        String authority = requiredText(node, "authority", where);
        if (!"PROPOSAL_ONLY".equals(authority))
            throw new ReverseContractException(
                    ReverseFailure.AUTHORITY_VIOLATION,
                    where + " authority must remain PROPOSAL_ONLY but was " + authority);
        JsonNode publication = node.path("semantic_publication_allowed");
        if (!publication.isBoolean() || publication.asBoolean())
            throw new ReverseContractException(
                    ReverseFailure.AUTHORITY_VIOLATION,
                    where + " semantic_publication_allowed must remain false");
    }

    private static void requireCleanText(String text, String what) {
        if (!ScenarioTextValidator.validate(text).isEmpty())
            throw new ReverseContractException(
                    ReverseFailure.SCENARIO_TEXT_IDENTIFIER,
                    what + " contains an implementation identifier: " + text);
    }

    private static List<SealedCitation> citations(JsonNode node, String where) {
        JsonNode citationNodes = requiredArray(node, "citations");
        List<SealedCitation> citations = new ArrayList<>();
        Set<String> seen = new TreeSet<>();
        for (int index = 0; index < citationNodes.size(); index++) {
            String citationWhere = where + "/citations/" + index;
            JsonNode citation = citationNodes.get(index);
            if (!citation.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, citationWhere + " must be an object");
            String channel = requiredText(citation, "channel", citationWhere);
            if (!channel.equals("STRUCTURAL") && !channel.equals("TEST_BEHAVIOR")
                    && !channel.equals("DELIVERY_HISTORY"))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        citationWhere + " has an unknown evidence channel: " + channel);
            String observationRef = requiredText(citation, "observation_ref", citationWhere);
            String evidenceDigest = ReverseContractValidation.sha256Hex(
                    requiredText(citation, "evidence_digest", citationWhere),
                    "sealed citation " + observationRef);
            if (!seen.add(channel + "|" + observationRef + "|" + evidenceDigest))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY,
                        citationWhere + " duplicates an earlier citation of " + observationRef);
            citations.add(new SealedCitation(channel, observationRef, evidenceDigest));
        }
        return citations;
    }

    private static JsonNode requiredArray(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "sealed package must contain a " + field + " array");
        return value;
    }

    private static String text(JsonNode node, String field, String what) {
        JsonNode value = node.path(field);
        if (!value.isTextual())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must be a plain string");
        return value.asText();
    }

    private static String requiredText(JsonNode node, String field, String where) {
        return ReverseContractValidation.requiredText(text(node, field, where + " " + field), where + " " + field);
    }
}
