# RC7-A.1 Package Defects and Isolated Fixes

The original ZIP checksum was verified before extraction. The package was not silently modified; the following issues were captured and patched only in the isolated validation copy before the fresh verify run.

1. `skills/pk-repository-analysis/scripts/self-test.sh` lacked its executable bit. It was changed to executable so the packaged self-test could run as declared.
2. `skills/rc6-self-test.sh` lacked its executable bit. It was changed to executable.
3. `verify.sh` contained raw Markdown backticks inside an unquoted heredoc, producing command-not-found noise. The affected descriptive backticks were escaped.
4. The `verify.sh` role-binding probe used `printf '%s'`, which allowed adjacent probes to concatenate. It now emits a newline between probe tokens.
5. The role-binding probe used `tr -d '[:space:]'`, which removed probe-token newlines and caused false binding failures. It now removes only horizontal/control whitespace needed by the probe.

After these isolated fixes, a fresh `bash verify.sh` completed with 27/27 required checks PASS. Four optional external checks remained NOT VERIFIED because the environment lacks the relevant live integrations or dispatch acknowledgement. A patched ZIP and checksum are generated only after the final S05/S06 evidence is closed.
