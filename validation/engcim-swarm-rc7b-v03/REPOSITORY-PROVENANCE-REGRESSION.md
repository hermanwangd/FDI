# Repository Provenance Regression

## Result

`PASS` in the local evaluator suite.

The positive test creates an isolated Git repository with a canonical HTTPS
origin, records a baseline commit, creates an actual descendant candidate
commit, and asks the evaluator to resolve both commits and their ancestry.

Negative tests reject an unknown candidate commit and a reconstructed local
repository advertised with a `file://` URL. No network fetch is required for
the local predicate, but a real Multica run still requires a canonical HTTP(S)
or SSH repository registration.
