package com.featuredeliveryintelligence.fdi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Rc6CompatibilityTests {
    @Test
    void scenarioFirstRc6SurfaceAndCoreSkillsRemainPresent() throws Exception {
        Path root = Path.of("");
        Path scenarioDoc = root.resolve("engcim/skill-packs/rc6-runtime-baseline-v1/package/engcim-swarm-package-RC6/docs/scenarios.md");
        assertThat(Files.exists(scenarioDoc)).isTrue();
        String scenarios = Files.readString(scenarioDoc);
        for (String scenario : new String[] {"S01", "S02", "S03", "S04", "S05", "S06"}) {
            assertThat(scenarios).as("RC6 scenario %s", scenario).contains(scenario);
        }
        for (String skill : new String[] {"swarm-orchestration", "execution-guard", "product-knowledge", "verification-protocol"}) {
            assertThat(Files.exists(root.resolve("engcim/skill-packs/rc6/skills/" + skill + "/SKILL.md")))
                    .as("RC6 skill %s", skill).isTrue();
        }
        Path fullPackage = root.resolve("engcim/skill-packs/rc6-runtime-baseline-v1/package/engcim-swarm-package-RC6/skills");
        for (String script : new String[] {
                "pm-intention/scripts/validate_intention_spec.py",
                "execution-guard/scripts/check_path.py",
                "execution-guard/scripts/check_command.py",
                "artifact-consistency/scripts/check_traceability.py",
                "test-architecture/scripts/quality_gate.py",
                "performance-benchmark/scripts/compare_metrics.py",
                "post-change-canary/scripts/canary_gate.py",
                "root-cause-debugging/scripts/hypothesis_log.py"}) {
            assertThat(Files.exists(fullPackage.resolve(script))).as("RC6 package script %s", script).isTrue();
        }
    }
}
