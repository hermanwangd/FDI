package com.featuredeliveryintelligence.fdi.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.featuredeliveryintelligence.fdi.orchestration.OrchestratorReportAudit.*;

/** Checks supplied receipts only. Does not fetch, execute, authorize, or prove trace completeness. */
public final class CurrentWorkPackageAudit {
    private CurrentWorkPackageAudit() {}

    public static AuditResult audit(JsonNode ledger) {
        return new Check().audit(ledger);
    }

    private static final class Check {
        private final List<Fact> facts = new ArrayList<>();
        private final List<Finding> findings = new ArrayList<>();
        private final Set<String> reported = new HashSet<>();
        private JsonNode ledger;
        private Map<String, JsonNode> packages, plans, decisions, operations, evidence;

        AuditResult audit(JsonNode input) {
            ledger = input;
            if (input == null || !input.isObject()) {
                finding("LEDGER_INVALID", "ledger", "Expected an object of normalized receipts.");
                return result();
            }
            for (String field : List.of("packages", "plans", "decisions", "operations", "evidence",
                    "expected_actions", "required_evidence")) {
                if (!input.path(field).isArray()) finding("LEDGER_INVALID", field, "Expected an explicit array.");
            }
            for (String field : List.of("mission_id", "case_id", "amendment_revision", "completion_claim")) {
                if (blank(input, field)) finding("LEDGER_INVALID", field, "Missing ledger identity or completion boundary.");
            }
            if (input.has("current_issue_id") && blank(input, "current_issue_id")) {
                finding("LEDGER_INVALID", "current_issue_id", "Explicit current issue must be a nonempty string; invalid values never fall back to a grant or Mission.");
            }
            for (String field : List.of("expected_actions", "required_evidence")) {
                if (!stringArray(input.path(field), true)) finding("LEDGER_INVALID", field, "Expected explicit nonempty string identities in the array.");
            }
            if (!findings.isEmpty()) return result();
            packages = index("packages", "package_id");
            plans = index("plans", "id");
            decisions = index("decisions", "id");
            operations = index("operations", "id");
            evidence = index("evidence", "id");
            if (!findings.isEmpty()) return result();
            for (String field : List.of("packages", "plans", "decisions")) {
                for (JsonNode receipt : input.path(field)) {
                    for (String binding : List.of("package_id", "action_id", "actor_id", "role", "category", "stage", "scope", "method", "inputs")) {
                        if (!validBinding(binding, receipt.path(binding))) finding("INPUT_OR_DEPENDENCY_MISMATCH", field,
                                "Required binding has invalid type or empty value: " + binding);
                    }
                    if (!field.equals("decisions") && !validBinding("dependencies", receipt.path("dependencies"))) {
                        finding("INPUT_OR_DEPENDENCY_MISMATCH", field, "Dependencies must be an explicit array of nonempty identities.");
                    }
                }
            }
            for (JsonNode operation : input.path("operations")) checkOperation(operation);
            checkScopeClaim();
            if (input.path("operations").isEmpty() && !input.path("expected_actions").isEmpty()) {
                finding("ACTION_EVIDENCE_INCOMPLETE", "operations", "Required work has no operation trace.");
            }
            Set<String> observedActions = new HashSet<>();
            operations.values().stream().filter(o -> !protocol(o) && confirmed(o))
                    .forEach(o -> observedActions.add(text(o, "action_id")));
            for (JsonNode action : input.path("expected_actions")) {
                if (!observedActions.contains(action.asText())) finding("REQUIRED_ACTION_MISSING", action.asText(), "A required action needs confirmed successful consumption; an attempt or protocol work is insufficient.");
            }
            for (JsonNode required : input.path("required_evidence")) {
                JsonNode output = evidence.get(required.asText());
                if (output == null) {
                    finding("REQUIRED_EVIDENCE_MISSING", required.asText(), "Required current output is absent.");
                } else {
                    validateEvidence(output, required.asText());
                    boolean bound = !blank(output, "actor_id") && !blank(output, "run_id")
                            && operations.values().stream().anyMatch(op ->
                                    text(output, "actor_id").equals(text(op, "actor_id"))
                                    && text(output, "run_id").equals(text(op, "run_id"))
                                    && text(op, "mission_id").equals(text(input, "mission_id"))
                                    && text(op, "case_id").equals(text(input, "case_id")));
                    if (!bound) finding("ACTOR_ROLE_MISMATCH", required.asText(), "Required receipt actor/run has no applicable current operation binding.");
                }
            }
            if (!Set.of("COMPLIANT_COMPLETED", "PROTOCOL_ONLY", "PARTIAL", "BLOCKED").contains(text(input, "completion_claim"))) {
                finding("LEDGER_INVALID", "completion_claim", "Unknown completion claim.");
            }
            if (text(input, "completion_claim").equals("PROTOCOL_ONLY") &&
                    (!input.path("expected_actions").isEmpty() || !input.path("required_evidence").isEmpty()
                            || operations.values().stream().anyMatch(o -> !protocol(o)))) {
                finding("LEDGER_INVALID", "completion_claim", "Protocol-only cannot claim substantive actions.");
            }
            facts.add(new Fact("CURRENT_WORK_PACKAGE_LEDGER_CHECKED", text(input, "mission_id"),
                    "Supplied normalized receipts checked; native authenticity, omitted operations and semantic plan quality are not established."));
            return result();
        }

        private Map<String, JsonNode> index(String field, String key) {
            Map<String, JsonNode> result = new HashMap<>();
            for (JsonNode item : ledger.path(field)) {
                if (!item.isObject() || blank(item, key)) {
                    finding("LEDGER_INVALID", field, "Receipt must be an object with a nonempty identity.");
                    continue;
                }
                JsonNode previous = result.putIfAbsent(text(item, key), item);
                if (previous != null && !previous.equals(item)) finding("CONFLICTING_RECEIPTS", text(item, key), "Same identity has conflicting receipt bodies.");
            }
            return result;
        }

        private void checkOperation(JsonNode op) {
            String id = text(op, "id");
            same(op, ledger, List.of("mission_id", "case_id"), "PACKAGE_IDENTITY_MISMATCH", id);
            for (String field : List.of("actor_id", "role", "run_id", "kind", "command", "output_ref", "ack")) {
                if (blank(op, field)) finding("ACTION_EVIDENCE_INCOMPLETE", id, "Missing operation field: " + field);
            }
            if (!op.path("command_truncated").isBoolean() || op.path("command_truncated").asBoolean()
                    || !op.path("consumed").isBoolean() || time(op, "at") == null || !resultReceiptPresent(op)
                    || !Set.of("KNOWN", "UNKNOWN", "NOT_APPLICABLE").contains(text(op, "ack"))) {
                finding("ACTION_EVIDENCE_INCOMPLETE", id, "Unordered, truncated or missing command/result evidence.");
            }
            if (!protocol(op)) {
                facts.add(new Fact("ACTION_ATTEMPT_RECORDED", id, "Attempt retained independently of its result or completion claim."));
                if (confirmed(op)) facts.add(new Fact("ACTION_CONFIRMED", id, "Known successful result and current output were consumed."));
            }
            checkOutput(op);
            JsonNode prior = operations.get(text(op, "repeats_operation_id"));
            if (prior != null) {
                if (prior.path("consumed").asBoolean() && !protocol(prior)) finding("DUPLICATE_CONSUMED_ACTION", id, "A consumed substantive operation was executed again.");
                if (text(prior, "ack").equals("UNKNOWN")) finding("UNCERTAIN_ACK_RETRY", id, "A new submission followed UNKNOWN ACK; reconciliation is required before retry.");
            }
            if (protocol(op)) { checkProtocol(op); return; }
            JsonNode pkg = packages.get(text(op, "package_id"));
            JsonNode plan = plans.get(text(op, "plan_id"));
            JsonNode decision = decisions.get(text(op, "decision_id"));
            if (pkg == null || plan == null) {
                finding("PACKAGE_IDENTITY_MISMATCH", id, "Operation has no bound current package and plan.");
                return;
            }
            same(op, pkg, List.of("package_id", "action_id"), "PACKAGE_IDENTITY_MISMATCH", id);
            same(plan, pkg, List.of("package_id", "action_id"), "PACKAGE_IDENTITY_MISMATCH", id);
            same(plan, ledger, List.of("mission_id", "case_id"), "PACKAGE_IDENTITY_MISMATCH", id);
            same(op, pkg, List.of("actor_id", "role"), "ACTOR_ROLE_MISMATCH", id);
            same(plan, pkg, List.of("actor_id", "role"), "ACTOR_ROLE_MISMATCH", id);
            same(plan, pkg, List.of("category", "stage", "scope", "method", "inputs", "dependencies"), "INPUT_OR_DEPENDENCY_MISMATCH", id);
            if (blank(plan, "stored_body") || !digest(text(plan, "stored_body")).equals(text(plan, "stored_sha256"))
                    || !plan.path("native_revision").isIntegralNumber() || !plan.path("declared_revision").isIntegralNumber()) {
                finding("PLAN_BINDING_MISMATCH", id, "Exact stored plan bytes and separate revisions are required.");
            }
            checkInputs(op, pkg);
            if (decision == null) { finding("APPROVAL_MISSING", id, "No exact independent decision receipt."); return; }
            same(decision, op, List.of("package_id", "action_id", "mission_id", "case_id"), "PACKAGE_IDENTITY_MISMATCH", id);
            same(decision, pkg, List.of("actor_id", "role"), "ACTOR_ROLE_MISMATCH", id);
            same(decision, pkg, List.of("category", "stage", "scope", "method", "inputs"), "INPUT_OR_DEPENDENCY_MISMATCH", id);
            checkNativeReplyContext(decision, id);
            if (!reviewedPlanId(decision).equals(text(plan, "id"))
                    || !decision.path("native_plan_revision").equals(plan.path("native_revision"))
                    || !decision.path("declared_plan_revision").equals(plan.path("declared_revision"))
                    || !text(decision, "plan_stored_sha256").equals(text(plan, "stored_sha256"))) {
                finding("PLAN_BINDING_MISMATCH", id, "Decision is stale or bound to different stored plan/revisions.");
            }
            if (blank(decision, "reviewer_id") || text(decision, "reviewer_id").equals(text(plan, "actor_id"))
                    || text(decision, "reviewer_id").equals(text(op, "actor_id"))) {
                finding("REVIEW_INDEPENDENCE_MISMATCH", id, "Reviewer must differ from author and executor.");
            }
            if (!text(decision, "decision").equals("APPROVE")) finding("ACTION_NOT_APPROVED", id, "Only an explicit current APPROVE permits action; verdict or ACK alone is insufficient.");
            if (!decision.path("conditions").isArray()) finding("APPROVAL_CONDITIONS_UNRESOLVED", id, "Conditions are not explicitly recorded.");
            for (JsonNode condition : decision.path("conditions")) {
                if (!condition.path("satisfied").isBoolean() || !condition.path("satisfied").asBoolean()) finding("APPROVAL_CONDITIONS_UNRESOLVED", id, "Approval condition remains unresolved.");
            }
            Instant planned = time(plan, "created_at"), approved = time(decision, "created_at"), acted = time(op, "at");
            if (planned == null || approved == null || acted == null || !planned.isBefore(approved) || !approved.isBefore(acted)) {
                finding("ACTION_ORDER_UNVERIFIED", id, "Trace must establish plan then decision then operation; equal timestamps are ambiguous.");
            }
            checkDecisionAsOf(op, pkg, plan, decision);
        }

        private void checkDecisionAsOf(JsonNode op, JsonNode pkg, JsonNode plan, JsonNode referenced) {
            String id = text(op, "id");
            Instant acted = time(op, "at"), latest = null;
            List<JsonNode> current = new ArrayList<>();
            if (acted == null) return;
            for (JsonNode candidate : decisions.values()) {
                if (!samePlanSubject(candidate, plan)) continue;
                Instant reviewed = time(candidate, "created_at");
                if (reviewed == null) {
                    finding("ACTION_ORDER_UNVERIFIED", id, "Same-subject review has no ordered timestamp; current authorization is unresolved.");
                    continue;
                }
                if (reviewed.isAfter(acted)) continue; // Later review does not rewrite earlier operation history.
                if (latest == null || reviewed.isAfter(latest)) { latest = reviewed; current.clear(); }
                if (reviewed.equals(latest)) current.add(candidate);
            }
            if (current.isEmpty()) return; // Referenced decision's missing/late timestamp is checked above.
            if (!latest.isBefore(acted)) finding("ACTION_ORDER_UNVERIFIED", id, "Latest same-subject review is simultaneous with the operation.");
            if (current.stream().noneMatch(d -> text(d, "id").equals(text(referenced, "id")))) {
                finding("ACTION_NOT_APPROVED", id, "Referenced approval was superseded before the operation; the exact current decision is required.");
            }
            for (JsonNode decision : current) {
                checkNativeReplyContext(decision, id);
                same(decision, pkg, List.of("category", "stage", "scope", "method", "inputs"), "INPUT_OR_DEPENDENCY_MISMATCH", id);
                if (blank(decision, "reviewer_id") || text(decision, "reviewer_id").equals(text(plan, "actor_id"))
                        || text(decision, "reviewer_id").equals(text(op, "actor_id"))) {
                    finding("REVIEW_INDEPENDENCE_MISMATCH", id, "Current decision lacks independent reviewer identity.");
                }
                if (!text(decision, "decision").equals("APPROVE")) finding("ACTION_NOT_APPROVED", id, "Current same-subject decision is not APPROVE; concurrent disagreement is unresolved.");
                if (!decision.path("conditions").isArray()) finding("APPROVAL_CONDITIONS_UNRESOLVED", id, "Current decision conditions are not explicitly recorded.");
                for (JsonNode condition : decision.path("conditions")) {
                    if (!condition.path("satisfied").isBoolean() || !condition.path("satisfied").asBoolean()) finding("APPROVAL_CONDITIONS_UNRESOLVED", id, "Current decision condition remains unresolved.");
                }
                if (!decision.path("conditions").equals(referenced.path("conditions"))) {
                    finding("ACTION_NOT_APPROVED", id, "Current review conditions differ from the referenced authorization.");
                }
            }
        }

        private boolean samePlanSubject(JsonNode decision, JsonNode plan) {
            if (!reviewedPlanId(decision).equals(text(plan, "id"))
                    || !decision.path("native_plan_revision").equals(plan.path("native_revision"))
                    || !decision.path("declared_plan_revision").equals(plan.path("declared_revision"))
                    || !text(decision, "plan_stored_sha256").equals(text(plan, "stored_sha256"))) return false;
            for (String field : List.of("mission_id", "case_id", "package_id", "action_id", "actor_id", "role")) {
                if (!decision.path(field).equals(plan.path(field))) return false;
            }
            return true;
        }

        private String reviewedPlanId(JsonNode decision) {
            // Explicit subject wins; invalid explicit values never fall back to a publication parent.
            String field = decision.has("reviewed_plan_id") ? "reviewed_plan_id" : "parent_id";
            return blank(decision, field) ? "" : text(decision, field);
        }

        private void checkNativeReplyContext(JsonNode decision, String operationId) {
            JsonNode parent = decision.get("parent_id");
            if (parent == null || (!parent.isNull() && blank(decision, "parent_id"))) {
                finding("PLAN_BINDING_MISMATCH", operationId, "Actual native parent must be recorded as a nonempty ID or an observed top-level null.");
            }
            JsonNode trigger = decision.get("trigger_comment_id");
            if (trigger != null && !trigger.isNull() && (blank(decision, "trigger_comment_id")
                    || !text(decision, "parent_id").equals(text(decision, "trigger_comment_id")))) {
                finding("PLAN_BINDING_MISMATCH", operationId, "Comment-triggered reply parent differs from the recorded native trigger; do not rewrite it to the reviewed plan ID.");
            }
        }

        private void checkInputs(JsonNode op, JsonNode pkg) {
            String id = text(op, "id");
            if (blank(op, "locator") || blank(op, "input_revision") || blank(op, "input_sha256")) {
                finding("INPUT_OR_DEPENDENCY_MISMATCH", id, "Operation requires typed nonempty source/input identities.");
            }
            if (!pkg.path("inputs").isArray() || !pkg.path("dependencies").isArray() || !pkg.path("scope").isArray() || blank(pkg, "method")) {
                finding("INPUT_OR_DEPENDENCY_MISMATCH", id, "Missing explicit scope, method, inputs or dependencies.");
                return;
            }
            boolean bound = false;
            for (JsonNode input : pkg.path("inputs")) {
                if (text(input, "locator").equals(text(op, "locator")) && text(input, "revision").equals(text(op, "input_revision"))
                        && text(input, "sha256").equals(text(op, "input_sha256")) && !blank(input, "sha256")) bound = true;
            }
            if (!bound) finding("INPUT_OR_DEPENDENCY_MISMATCH", id, "Operation's source identity differs from reviewed inputs.");
            boolean scoped = false;
            for (JsonNode scope : pkg.path("scope")) if (scope.asText().equals(text(op, "locator"))) scoped = true;
            if (!scoped) finding("INPUT_OR_DEPENDENCY_MISMATCH", id, "Operation locator is outside exact reviewed scope.");
            for (JsonNode dependency : pkg.path("dependencies")) {
                String dependencyId = dependency.asText();
                boolean completedBefore = operations.values().stream().anyMatch(prior -> text(prior, "package_id").equals(dependencyId)
                        && !protocol(prior) && confirmed(prior)
                        && time(prior, "at") != null && time(op, "at") != null && time(prior, "at").isBefore(time(op, "at")));
                if (!packages.containsKey(dependencyId) || !completedBefore) finding("INPUT_OR_DEPENDENCY_MISMATCH", id, "Dependency lacks prior operation/output evidence: " + dependencyId);
            }
        }

        private boolean resultReceiptPresent(JsonNode op) {
            String resultType = op.has("result_type") ? text(op, "result_type") : "PROCESS";
            if (resultType.equals("TOOL")) {
                return op.path("result_status").isTextual()
                        && Set.of("SUCCESS", "FAILURE", "UNKNOWN").contains(text(op, "result_status"));
            }
            return resultType.equals("PROCESS") && (op.path("exit_status").isIntegralNumber()
                    || (text(op, "ack").equals("UNKNOWN") && op.path("exit_status").isNull()));
        }

        private boolean confirmed(JsonNode op) {
            if (!op.path("consumed").isBoolean() || !op.path("consumed").asBoolean()
                    || !text(op, "ack").equals("KNOWN") || !resultReceiptPresent(op)) return false;
            String resultType = op.has("result_type") ? text(op, "result_type") : "PROCESS";
            boolean success = resultType.equals("TOOL") ? text(op, "result_status").equals("SUCCESS")
                    : op.path("exit_status").isIntegralNumber() && op.path("exit_status").bigIntegerValue().signum() == 0;
            JsonNode output = evidence.get(text(op, "output_ref"));
            return success && output != null && text(output, "kind").equals("NATIVE")
                    && text(op, "evidence_kind").equals("NATIVE")
                    && text(output, "cohort").equals(text(ledger, "amendment_revision"))
                    && text(op, "cohort").equals(text(output, "cohort"))
                    && text(output, "actor_id").equals(text(op, "actor_id"))
                    && text(output, "run_id").equals(text(op, "run_id"))
                    && output.path("content").isTextual() && digest(text(output, "content")).equals(text(output, "sha256"));
        }

        private void checkOutput(JsonNode op) {
            String id = text(op, "id");
            JsonNode output = evidence.get(text(op, "output_ref"));
            if (output == null) { finding("ACTION_EVIDENCE_INCOMPLETE", id, "Operation output receipt is absent."); return; }
            validateEvidence(output, id);
            for (String flag : List.of("output_truncated", "content_truncated")) {
                if (op.has(flag) && (!op.path(flag).isBoolean() || op.path(flag).asBoolean()))
                    finding("ACTION_EVIDENCE_INCOMPLETE", id, "Output completeness flag is missing its false Boolean value: " + flag);
            }
            if (!text(op, "evidence_kind").equals("NATIVE")
                    || !text(op, "cohort").equals(text(ledger, "amendment_revision")) || !text(output, "cohort").equals(text(op, "cohort"))) {
                finding("EVIDENCE_KIND_OR_COHORT_MISMATCH", id, "Local/mock or old-cohort evidence cannot substitute for current native receipts.");
            }
            same(output, op, List.of("run_id", "actor_id"), "ACTOR_ROLE_MISMATCH", id);
        }

        private void validateEvidence(JsonNode output, String id) {
            if (blank(output, "run_id") || blank(output, "actor_id")) {
                finding("ACTOR_ROLE_MISMATCH", id, "Output requires explicit nonempty textual actor/run identities.");
            }
            if (blank(output, "kind") || blank(output, "cohort")
                    || !text(output, "kind").equals("NATIVE") || !text(output, "cohort").equals(text(ledger, "amendment_revision"))) {
                finding("EVIDENCE_KIND_OR_COHORT_MISMATCH", id, "Required or referenced output must be native and from the current amendment cohort.");
            }
            if (!output.path("content").isTextual() || blank(output, "sha256")
                    || !digest(text(output, "content")).equals(text(output, "sha256"))) {
                finding("ACTION_EVIDENCE_INCOMPLETE", id, "Output bytes/digest are not present or do not agree.");
            }
            for (String flag : List.of("output_truncated", "content_truncated")) {
                if (output.has(flag) && (!output.path(flag).isBoolean() || output.path(flag).asBoolean()))
                    finding("ACTION_EVIDENCE_INCOMPLETE", id, "Referenced evidence declares truncation or an invalid completeness flag: " + flag);
            }
            if (text(output, "content").matches("(?s)^\\s*<truncated [0-9]+ (?:lines|bytes)>.*")) {
                finding("ACTION_EVIDENCE_INCOMPLETE", id, "Referenced evidence starts with a tool truncation marker despite its digest or completeness claim.");
            }
        }

        private void checkProtocol(JsonNode op) {
            String id = text(op, "id"), kind = text(op, "kind");
            if (op.path("consumed").asBoolean() || op.has("input_revision") || op.has("input_sha256")) {
                finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Source/product work cannot be relabeled protocol preparation.");
                return;
            }
            switch (kind) {
                case "PROTOCOL_SCRATCH" -> {
                    JsonNode output = evidence.get(text(op, "output_ref"));
                    JsonNode grant = ledger.path("preparation_grant");
                    int byteCap = grant.isObject() && boundedInt(grant.path("max_scratch_bytes"), 32768)
                            ? grant.path("max_scratch_bytes").asInt() : 32768;
                    int fileCap = grant.isObject() && boundedInt(grant.path("max_scratch_files"), 2)
                            ? grant.path("max_scratch_files").asInt() : 2;
                    long paths = operations.values().stream().filter(o -> text(o, "kind").equals("PROTOCOL_SCRATCH"))
                            .map(o -> normalizedPath(text(o, "locator"))).distinct().count();
                    if (!scratchPath(text(op, "locator")) || !grantAllows(op)
                            || output == null || !output.path("content").isTextual()
                            || text(output, "content").getBytes(StandardCharsets.UTF_8).length > byteCap || paths > fileCap
                            || !Set.of("PLAN_REPLY_PREPARATION", "REVIEW_REPLY_PREPARATION").contains(text(op, "purpose"))
                            || !text(op, "filesystem_effect").equals("WRITE") || !op.path("deletion").isBoolean() || op.path("deletion").asBoolean()
                            || blank(op, "content_sha256") || !text(op, "content_sha256").equals(text(evidence.get(text(op, "output_ref")), "sha256"))) {
                        finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Scratch needs an exact named grant, bounded UTF-8 bytes/file count, and no deletion.");
                    } else {
                        facts.add(new Fact("PROTOCOL_SCRATCH_ALLOWED", id, "Supplied path/bytes describe bounded reply preparation; physical symlink containment is not verified."));
                        facts.add(new Fact("FILE_WRITE_RECORDED", id, "Protocol scratch is a file write; it supplies no substantive completion."));
                    }
                }
                case "PROTOCOL_CONTEXT_READ" -> {
                    JsonNode grant = ledger.path("preparation_grant");
                    String target = text(op, "target_kind");
                    boolean scoped = grant.isObject() && grantAllows(op)
                            && !blank(op, "workspace_id") && !blank(op, "target_issue_id")
                            && text(op, "workspace_id").equals(text(grant, "workspace_id"))
                            && text(op, "target_issue_id").equals(text(grant, "issue_id"))
                            && stringArray(op.path("returned_issue_ids"), true)
                            && text(op, "purpose").equals("CURRENT_PLAN_PREPARATION")
                            && text(op, "filesystem_effect").equals("NONE")
                            && op.path("provider_write").isBoolean() && !op.path("provider_write").asBoolean()
                            && op.path("output_truncated").isBoolean() && op.path("content_truncated").isBoolean();
                    for (JsonNode issue : op.path("returned_issue_ids")) {
                        if (!issue.asText().equals(text(grant, "issue_id"))) scoped = false;
                    }
                    if (target.equals("CURRENT_THREAD")) {
                        if (blank(op, "target_thread_id") || !strings(grant.path("thread_ids")).contains(text(op, "target_thread_id"))) scoped = false;
                    } else if (target.equals("IDENTITY_HELP")) {
                        if (!op.path("returned_issue_ids").isEmpty()) scoped = false;
                    } else if (!target.equals("CURRENT_ISSUE")) scoped = false;
                    if (!scoped) finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Preparation may read only granted current issue/thread context or syntax help.");
                    else facts.add(new Fact("PROTOCOL_CONTEXT_ALLOWED", id,
                            "Supplied target/grant are scoped preparation; actual command semantics and complete native context require independent reconciliation."));
                }
                case "ACK_RECONCILIATION" -> {
                    if (!op.path("provider_write").isBoolean() || op.path("provider_write").asBoolean() || blank(op, "target_comment_id")
                            || !text(op, "reconciliation_result").equals("EXACT_MATCH") || blank(op, "submitted_body_sha256")
                            || !text(op, "submitted_body_sha256").equals(text(op, "observed_body_sha256"))
                            || !text(op, "observed_body_sha256").equals(text(evidence.get(text(op, "output_ref")), "sha256"))) {
                        finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Reconciliation must read back the exact comment without resubmission.");
                    } else facts.add(new Fact("PROTOCOL_RECONCILIATION_ONLY", id, "Readback reconciliation records no new substantive action."));
                }
                case "PROTOCOL_COMMENT_SUBMIT" -> {
                    if (blank(op, "submitted_body_sha256") || blank(op, "attempt_id")) finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Submission requires exact body and attempt identities.");
                }
                default -> finding("PROTOCOL_SCOPE_OR_KIND_MISMATCH", id, "Unknown protocol kind cannot exempt substantive work.");
            }
        }

        private boolean insideWorkdir(String locator) {
            try {
                Path workdir = Path.of(text(ledger, "protocol_workdir")).normalize(), path = Path.of(locator).normalize();
                return workdir.isAbsolute() && path.isAbsolute() && !path.equals(workdir) && path.startsWith(workdir);
            } catch (InvalidPathException e) { return false; }
        }

        private Path normalizedPath(String value) {
            try { return Path.of(value).normalize(); } catch (InvalidPathException e) { return null; }
        }

        private boolean scratchPath(String locator) {
            Path dir = normalizedPath(text(ledger, "protocol_workdir")), path = normalizedPath(locator);
            return dir != null && path != null && insideWorkdir(locator)
                    && (path.equals(dir.resolve("protocol/plan.txt")) || path.equals(dir.resolve("protocol/reply.txt")));
        }

        /** Explicit caller grants can narrow the two-file default, never expand it. */
        private boolean grantAllows(JsonNode op) {
            if (!ledger.has("preparation_grant")) return true; // Historical named scratch receipts retain their bounded default.
            JsonNode grant = ledger.path("preparation_grant");
            if (!grant.isObject() || blank(grant, "workspace_id") || blank(grant, "issue_id")
                    || !text(grant, "issue_id").equals(currentIssue())
                    || blank(grant, "actor_id") || blank(grant, "run_id")
                    || !text(grant, "actor_id").equals(text(op, "actor_id")) || !text(grant, "run_id").equals(text(op, "run_id"))
                    || !stringArray(grant.path("thread_ids"), true) || !stringArray(grant.path("scratch_paths"), true)
                    || !boundedInt(grant.path("max_scratch_files"), 2) || !boundedInt(grant.path("max_scratch_bytes"), 32768)
                    || !grant.path("deletion_allowed").isBoolean() || grant.path("deletion_allowed").asBoolean()) return false;
            Set<Path> paths = new HashSet<>();
            for (JsonNode path : grant.path("scratch_paths")) {
                if (!scratchPath(path.asText()) || !paths.add(normalizedPath(path.asText()))) return false;
            }
            return paths.size() <= grant.path("max_scratch_files").asInt()
                    && (!text(op, "kind").equals("PROTOCOL_SCRATCH") || paths.contains(normalizedPath(text(op, "locator"))));
        }

        private boolean boundedInt(JsonNode value, int max) {
            return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() > 0 && value.asInt() <= max;
        }

        /** Current issue is declared independently of the grant; legacy ledgers use Mission identity. */
        private String currentIssue() {
            return text(ledger, ledger.has("current_issue_id") ? "current_issue_id" : "mission_id");
        }

        /** Checks an explicitly supplied strict claim, without fetching or authenticating the trace. */
        private void checkScopeClaim() {
            if (!ledger.has("reported_scope_compliance")) return;
            String subject = "reported_scope_compliance";
            if (!text(ledger, subject).equals("COMPLIANT")) {
                finding("LEDGER_INVALID", subject, "An explicit strict scope claim must be COMPLIANT; partial reports omit this claim.");
                return;
            }
            JsonNode trace = ledger.path("trace_reconciliation");
            if (!trace.isObject() || blank(trace, "issue_id") || blank(trace, "actor_id") || blank(trace, "run_id")) {
                finding("ACTION_EVIDENCE_INCOMPLETE", subject, "Strict scope claim needs current actor/run/issue trace reconciliation.");
                return;
            }
            JsonNode grant = ledger.path("preparation_grant");
            String issue = currentIssue();
            if (!text(trace, "issue_id").equals(issue)) finding("PACKAGE_IDENTITY_MISMATCH", subject, "Reconciled trace belongs to another issue.");
            Set<String> current = new HashSet<>();
            boolean foreignOperation = false;
            for (JsonNode op : operations.values()) {
                current.add(text(op, "id"));
                if (!text(trace, "actor_id").equals(text(op, "actor_id")) || !text(trace, "run_id").equals(text(op, "run_id"))) foreignOperation = true;
            }
            JsonNode raw = evidence.get(text(trace, "raw_receipt_ref"));
            if (current.isEmpty() || foreignOperation || (grant.isObject() && (!text(trace, "actor_id").equals(text(grant, "actor_id"))
                    || !text(trace, "run_id").equals(text(grant, "run_id"))))
                    || (raw != null && (!text(raw, "actor_id").equals(text(trace, "actor_id")) || !text(raw, "run_id").equals(text(trace, "run_id"))))) {
                finding("ACTOR_ROLE_MISMATCH", subject, "Reconciliation actor/run does not match applicable operations, grant and raw receipt.");
                return;
            }
            if (raw == null || blank(trace, "raw_receipt_sha256") || !text(trace, "raw_receipt_sha256").equals(text(raw, "sha256"))) {
                finding("ACTION_EVIDENCE_INCOMPLETE", subject, "Raw reconciliation receipt and exact digest are required.");
            } else validateEvidence(raw, subject);
            if (!trace.path("complete").isBoolean() || !trace.path("complete").asBoolean()
                    || !trace.path("gaps").isArray() || !trace.path("gaps").isEmpty()
                    || !stringArray(trace.path("observed_operation_ids"), false) || !stringArray(trace.path("reconciled_operation_ids"), false)
                    || strings(trace.path("observed_operation_ids")).size() != trace.path("observed_operation_ids").size()
                    || strings(trace.path("reconciled_operation_ids")).size() != trace.path("reconciled_operation_ids").size()
                    || !strings(trace.path("observed_operation_ids")).equals(current)
                    || !strings(trace.path("reconciled_operation_ids")).equals(current)) {
                finding("ACTION_EVIDENCE_INCOMPLETE", subject, "Trace ranges and exact observed/reconciled operation inventories must be complete.");
            }
            if (!trace.path("deviations").isArray()) finding("ACTION_EVIDENCE_INCOMPLETE", subject, "Explicit deviation inventory is required.");
            else if (!trace.path("deviations").isEmpty()) finding("SCOPE_COMPLIANCE_CONTRADICTED", subject, "Supplied trace deviations contradict strict scope compliance.");
            if (ledger.has("reported_file_writes")) {
                long writes = operations.values().stream().filter(o -> text(o, "filesystem_effect").equals("WRITE")).count();
                JsonNode count = ledger.path("reported_file_writes");
                if (!count.isIntegralNumber() || !count.canConvertToLong() || count.asLong() < 0)
                    finding("ACTION_EVIDENCE_INCOMPLETE", subject, "Reported write count requires a nonnegative integer.");
                else if (count.asLong() != writes) finding("SCOPE_COMPLIANCE_CONTRADICTED", subject, "Reported writes contradict supplied operation effects.");
            }
        }

        private Set<String> strings(JsonNode values) {
            Set<String> result = new HashSet<>();
            if (values.isArray()) for (JsonNode value : values) result.add(value.asText());
            return result;
        }

        private void same(JsonNode left, JsonNode right, List<String> fields, String code, String id) {
            for (String field : fields) {
                if (!validBinding(field, left.path(field)) || !validBinding(field, right.path(field))
                        || !left.path(field).equals(right.path(field))) finding(code, id, "Receipt binding differs or has an invalid type/value: " + field);
            }
        }

        private boolean validBinding(String field, JsonNode value) {
            return switch (field) {
                case "scope" -> stringArray(value, false);
                case "dependencies" -> stringArray(value, true);
                case "inputs" -> {
                    if (!value.isArray() || value.isEmpty()) yield false;
                    boolean valid = true;
                    for (JsonNode input : value) {
                        if (!input.isObject() || blank(input, "locator") || blank(input, "revision") || blank(input, "sha256")
                                || !text(input, "sha256").matches("[0-9a-f]{64}")) valid = false;
                    }
                    yield valid;
                }
                default -> value.isTextual() && !value.asText().isBlank();
            };
        }

        private boolean stringArray(JsonNode value, boolean allowEmpty) {
            if (!value.isArray() || (!allowEmpty && value.isEmpty())) return false;
            for (JsonNode item : value) if (!item.isTextual() || item.asText().isBlank()) return false;
            return true;
        }

        private void finding(String code, String subject, String reason) {
            if (reported.add(code + "\u0000" + subject + "\u0000" + reason)) findings.add(new Finding(code, subject, reason));
        }

        private AuditResult result() { return new AuditResult(findings.isEmpty() ? "CLEAN" : "FINDINGS", facts, findings, findings.isEmpty() ? 0 : 1); }
    }

    private static boolean protocol(JsonNode op) { return text(op, "kind").startsWith("PROTOCOL") || text(op, "kind").equals("ACK_RECONCILIATION"); }
    private static String text(JsonNode node, String field) { return node == null ? "" : node.path(field).asText(""); }
    private static boolean blank(JsonNode node, String field) {
        return node == null || !node.path(field).isTextual() || node.path(field).asText().isBlank();
    }
    private static Instant time(JsonNode node, String field) {
        try { return Instant.parse(text(node, field)); } catch (DateTimeParseException e) { return null; }
    }
    private static String digest(String body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("Required JDK SHA-256 unavailable", e); }
    }
}
