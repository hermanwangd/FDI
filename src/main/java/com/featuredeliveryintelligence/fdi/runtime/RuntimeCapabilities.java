package com.featuredeliveryintelligence.fdi.runtime;

import java.util.*;

public final class RuntimeCapabilities {
    private static final List<String> BOUNDS = List.of("max_depth", "max_nodes", "max_edges", "max_paths", "max_result_bytes");
    private static final Set<String> FORBIDDEN = Set.of("product_asset_ref", "knowledge_role", "authority_dimension", "context_selector");
    private RuntimeCapabilities() {}

    public static List<Map<String, Object>> compile(Map<String, Object> rootSkill, Map<String, Object> featurePlan) {
        String skillId = RuntimeMaps.requiredString(rootSkill, "skill_id");
        String skillRevision = RuntimeMaps.requiredString(rootSkill, "skill_revision");
        Map<String, Map<String, Object>> templates = new LinkedHashMap<>();
        for (Object value : RuntimeMaps.list(rootSkill, "runtime_capability_templates")) {
            Map<String, Object> template = asMap(value, "root runtime template");
            String id = RuntimeMaps.requiredString(template, "template_id"); RuntimeMaps.requiredString(template, "capability");
            if (templates.putIfAbsent(id, template) != null) throw new RuntimeContractException("duplicate root runtime template: " + id);
        }
        String planId = RuntimeMaps.requiredString(featurePlan, "plan_id");
        Object revision = featurePlan.get("revision");
        if (!(revision instanceof Number)) throw new RuntimeContractException("feature plan revision is required");
        List<Map<String, Object>> compiled = new ArrayList<>();
        for (Object value : RuntimeMaps.list(featurePlan, "runtime_capabilities")) {
            Map<String, Object> item = asMap(value, "runtime capability item");
            for (String key : FORBIDDEN) if (item.containsKey(key)) throw new RuntimeContractException("runtime requirement may not carry Product Context fields");
            String templateId = RuntimeMaps.requiredString(item, "template_id");
            Map<String, Object> template = templates.get(templateId);
            if (template == null) throw new RuntimeContractException("runtime template not declared by root Skill: " + templateId);
            if (!Objects.equals(item.get("capability"), template.get("capability"))) throw new RuntimeContractException("feature plan cannot change root runtime capability");
            String mode = RuntimeMaps.requiredString(item, "mode");
            if (!RuntimeMaps.list(template, "allowed_modes").contains(mode)) throw new RuntimeContractException("runtime mode not allowed by root Skill: " + mode);
            if ("REQUIRED".equals(mode) && Boolean.FALSE.equals(template.get("may_promote_to_required"))) throw new RuntimeContractException("feature plan cannot promote runtime capability to REQUIRED");
            Set<Object> operations = new TreeSet<>(Comparator.comparing(Object::toString)); operations.addAll(RuntimeMaps.list(item, "operations"));
            if (operations.isEmpty() || !new HashSet<>(RuntimeMaps.list(template, "allowed_operations")).containsAll(operations)) throw new RuntimeContractException("feature plan requested operation outside root Skill allowance");
            Map<String, Object> requested = RuntimeMaps.requiredMap(item, "bounds"); Map<String, Object> maximum = RuntimeMaps.requiredMap(template, "maximum_bounds");
            Map<String, Object> bounds = new LinkedHashMap<>();
            for (String key : BOUNDS) { int actual = RuntimeMaps.requiredPositiveInt(requested, key); int max = RuntimeMaps.requiredPositiveInt(maximum, key); if (actual > max) throw new RuntimeContractException("runtime bound " + key + " exceeds root Skill maximum"); bounds.put(key, actual); }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("root_skill_id", skillId); row.put("root_skill_revision", skillRevision); row.put("root_runtime_template_id", templateId);
            row.put("feature_plan_id", planId); row.put("feature_plan_revision", revision); row.put("capability", template.get("capability"));
            row.put("mode", mode); row.put("operations", List.copyOf(operations)); row.put("bounds", bounds); row.put("dependent_claims", RuntimeMaps.list(item, "dependent_claims"));
            compiled.add(row);
        }
        return compiled;
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> asMap(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) throw new RuntimeContractException(label + " must be an object"); return (Map<String, Object>) value;
    }
}
