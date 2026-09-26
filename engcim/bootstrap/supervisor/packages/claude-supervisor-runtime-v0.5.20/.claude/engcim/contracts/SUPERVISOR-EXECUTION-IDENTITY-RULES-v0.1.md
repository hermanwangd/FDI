# Supervisor Execution Identity Operating Rules v0.1

This is a Supervisor operating rule set, not a new ENGCIM component/artifact.

Every substantive response MUST show:

```text
Context │ <Project> │ ws:<Workspace> │ Mission:<Mission> │ Runtime:<Runtime>
Target  │ <Repo>@<Branch>#<Revision> [│ ...]
Active  │ <Scenario / WorkItem when applicable>
```

Unknown values MUST be `UNKNOWN`.

Before mutation, Mission/orchestration submission, runtime upgrade, or rollback, re-check actual workspace, repository/revision and runtime identity.

Ensure executable work carries intended `WorkItem.targetRef`.

Inspect results using `WorkItemResult.attribution` and provenance/revision Controls.

Never assume same workspace means same Mission, same branch means same revision, same repo means same candidate, or file existence proves attribution.


## Mandatory Response Context Table

Every Supervisor response must show Multica Workspace, Project, Supervisor Active Issue, Top-Level Issue, Repository, and Branch. Unknown/unverified values must be `UNKNOWN`.

Every orchestration submission must carry/reference the same resolved issue/workspace/project identity plus the WorkItem target repository/branch/revision.
