# Product Realization Traversal Guard Spec v0.1

**Status:** REQUIRED RUNTIME CONFORMANCE BEFORE DEV-204

## Required traversal controls

A realization-derived candidate view MUST declare and enforce:

```yaml
traversal_policy:
  allowed_relation_types: ["<typed-relations>"]
  max_depth: <positive-integer>
  max_edges_examined: <positive-integer>
  max_paths_per_repository: <positive-integer>
  allowed_product_scope: ["<product/sub-product/capability ids>"]
  lifecycle_required: "PUBLISHED_ACTIVE"
```

No unrestricted organization-wide graph crawl is permitted.

## Fail/stop semantics

- invalid node/relation type → reject relation/profile fail-closed;
- cycle → terminate deterministically and record cycle diagnostic; never recurse indefinitely;
- depth budget exhausted → stop expansion and record bounded incompleteness;
- edge budget exhausted → stop expansion and record bounded incompleteness;
- path budget exceeded → preserve deterministic bounded path subset plus truncation indicator;
- stale/superseded relation → not eligible as active realization evidence;
- relation outside allowed Product scope → excluded unless explicit cross-product scope is authorized;
- caller-injected undeclared relation → ignored/rejected.

## Multi-path provenance

When multiple valid paths reach one repository, the derived view MUST preserve multiple meaningful provenance paths up to `max_paths_per_repository`. Deduplication of the repository must not erase material path provenance.

## Required tests

1. self-cycle;
2. two-node cycle;
3. >max-depth chain;
4. >max-edge fanout;
5. same repo reached by >max-paths;
6. shared repo across two capabilities in allowed scope;
7. shared repo from unauthorized product scope;
8. stale relation;
9. superseded relation;
10. profile-declared relation only;
11. lifecycle change invalidates/recomputes derived view;
12. deterministic output ordering for identical inputs.
