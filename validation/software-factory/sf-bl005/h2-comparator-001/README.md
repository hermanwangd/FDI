# H2 overlap evidence comparator

Run the packaged Java17 CLI with `-Xmx1g -Dloader.main=com.featuredeliveryintelligence.fdi.application.H2ExposureCompareCli -cp target/fdi-0.4.8.3.jar org.springframework.boot.loader.launch.PropertiesLauncher --input INPUT.json --output NEW_OUTPUT.json`. The output path must not already exist.

Use the synthetic examples in inputs/. Supply extracted evidence as content plus SHA256 of its exact UTF8 bytes. Pair ids are unique; candidateId is opaque. The tool verifies supplied bytes; it cannot authenticate upstream extraction or prove a global history complete.

Every result includes all seven dimensions. Exact nonempty duplicate extracts cause MATCH and INELIGIBLE. Other comparisons remain UNKNOWN and NOT_PROVEN_INDEPENDENT. Text similarity uses case-preserving token5gram sets; intersection/union at least4/5 flags NEAR_DUPLICATE_REVIEW_REQUIRED. This is a diagnostic threshold, not a sealed independence threshold. Missing dimensions remain UNKNOWN. selectionAuthorized is always false.

Limits:4MiB JSON input,64 comparisons,16KiB per artifact,20000 tokens per artifact. Strict schemas, duplicate keys, trailing data, malformed UTF8, symlinks and existing output paths fail closed. Output omits raw evidence contents and uses deterministic serialization.

This tool does not produce NO_MATCH or PROVEN_INDEPENDENT. Formal H2 eligibility still needs complete disclosure, coverage/lineage review and an authorized sealed comparison policy. No new repository was selected or inspected to develop this tool. Existing H0/H1 remain bound to709 and its separately stored JAR; this comparator build has its own identity.
