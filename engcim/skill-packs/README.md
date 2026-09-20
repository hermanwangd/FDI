# ENGCIM Skill Packs

This directory is the Git-tracked canonical home for ENGCIM skill-pack release baselines.

Each release directory contains:

- the exact sealed release archive;
- a machine-readable `SOURCE-MANIFEST.json`;
- Git-reviewable skill entrypoints and non-Python support assets;
- the role-definition snapshot used to compare runtime bindings.

The archive is authoritative for the complete package byte surface. The extracted files are the reviewable source surface and must agree with the manifest before a validation run.

The first frozen baseline is [RC6](rc6/CANONICAL-SOURCE.md).
