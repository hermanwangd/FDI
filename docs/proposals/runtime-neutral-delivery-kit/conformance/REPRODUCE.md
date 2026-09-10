# Reproduce package checks

Use any approved JSON Schema Draft 2020-12 validator; the local author check used
jsonschema 4.26.0. No dependency installation or framework runner is shipped.

1. Check every contracts/*.schema.json for valid schema syntax; resolve sibling
   references locally through contracts/contracts.schema.json.
2. Validate examples/ExecutionPlan.json, examples/Envelope.json,
   examples/WorkItemResult.json, three examples/WorkItem-*.json and both adapter
   profiles against their matching definitions: eight positive fixtures.
3. Load schema-negative-vectors.json. For each record, validate input against
   contracts/contracts.schema.json#/$defs/<schema> and require rejection: twelve
   negative fixtures. Report every result; do not stop after one success.
4. Validate all local Markdown links and catalog paths/dependencies. Verify workflow
   role/skill mappings and every SOURCE-MANIFEST digest.
5. Verify package-lock.json lists exactly all files except itself, reject unsafe
   paths/symlinks, and compare size and SHA-256 for each file. Trust the lock root
   separately; it does not authenticate itself.

These checks reproduce G0 only. Implement CT-01–12 and cases.json in the approved
company Java/runtime harness before claiming G1 or later gates. Zero commit IDs
in synthetic examples are deliberately shape-valid but admission-invalid.
