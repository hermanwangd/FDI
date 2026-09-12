# Sealed-byte whitespace exception

Final staged `git diff --check` reports one extra blank line at EOF in
`evaluator/gold-authoring.md:196`. Its bytes were independently authored and
sealed before generation; the protocol and receipt bind its SHA256. Removing
the blank line now would invalidate that provenance for no behavioral benefit.

The sealed file is deliberately preserved. A scoped staged whitespace check
excluding only that exact file passes; all other files remain subject to the
normal check. This is an explicit evidence-format exception, not a passing
claim for the unqualified whole-diff command. Java 1361/1361, Python 63/63,
reviewed source, producer/evaluator outputs and all recorded hashes are unchanged.
