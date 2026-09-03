from pathlib import Path
import json, sys
from release_metadata import included_files, repository_root

root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/VERIFICATION-SUMMARY.json'
out.parent.mkdir(parents=True, exist_ok=True)
markdown_count=sum(path.suffix.lower()=='.md' for path in included_files(root))
summary={
  "package": "fdi-standalone-project-baseline-v0.4.8.3",
  "verification_scope": "release metadata claim boundaries; execution evidence is not recorded by this generator",
  "markdown_inventory_files": markdown_count,
  "verification_execution": {
    "manifest_integrity": "NOT_RUN",
    "python_compile": "NOT_RUN",
    "standalone_verifier": "NOT_RUN",
    "unit_tests": "NOT_RUN"
  },
  "claims": {
    "standalone_governing_content_available": "NOT_RECORDED",
    "upstream_byte_identity": "NOT_CLAIMED",
    "real_product_binding": "NOT_EXECUTED",
    "live_grafel": "NOT_EXECUTED",
    "DEV204": "NOT_EXECUTED",
    "F001": "NOT_EXECUTED",
    "empirical_uplift": "NOT_ESTABLISHED"
  }
}
out.write_text(json.dumps(summary, indent=2)+'\n')
print(f'wrote {out}: {markdown_count} Markdown files')
