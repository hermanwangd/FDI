# S06 evaluator-only input boundary

S06 receives the exact candidate and legitimate verification requirements only.
The evaluator may know the frozen Product behavior and FV-003 test, but the S05
producer cannot read this directory, its expected verdicts, its F1 wording, or
any future r2 solution. This directory is excluded by the generation isolation
manifest.
