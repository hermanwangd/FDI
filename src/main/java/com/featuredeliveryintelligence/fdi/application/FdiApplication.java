package com.featuredeliveryintelligence.fdi.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.featuredeliveryintelligence.fdi")
public class FdiApplication {
    public static void main(String[] args) {
        if (ScenarioForwardCli.handles(args)) {
            System.exit(ScenarioForwardCli.run(args, System.out, System.err));
        }
        if (BlindReviewCli.handles(args)) {
            System.exit(BlindReviewCli.run(args, System.out, System.err));
        }
        if (NextRunGateCli.handles(args)) {
            System.exit(NextRunGateCli.run(args, System.out, System.err));
        }
        if (CodeBaselineCli.handles(args)) {
            System.exit(CodeBaselineCli.run(args, System.out, System.err));
        }
        if (GraphifyRuntimeProbeCli.handles(args)) {
            System.exit(GraphifyRuntimeProbeCli.run(args, System.out, System.err));
        }
        if (DeliveryHistoryCli.handles(args)) {
            System.exit(DeliveryHistoryCli.run(args, System.out, System.err));
        }
        if (AcquisitionCli.handles(args)) {
            System.exit(AcquisitionCli.run(args, System.out, System.err));
        }
        if (ExperimentRunnerValidateCli.handles(args)) {
            System.exit(ExperimentRunnerValidateCli.run(args, System.out, System.err));
        }
        if (ExperimentRunnerExecuteCli.handles(args)) {
            System.exit(ExperimentRunnerExecuteCli.run(args, System.out, System.err));
        }
        if (Task7EvaluateCli.handles(args)) {
            System.exit(Task7EvaluateCli.run(args, System.out, System.err));
        }
        if (Phase0ReadinessCli.handles(args)) {
            System.exit(Phase0ReadinessCli.run(args, System.out, System.err));
        }
        if (BlindEvaluationCli.handles(args)) {
            System.exit(BlindEvaluationCli.run(args, System.out, System.err));
        }
        if (GraphifyLiveVerifierCli.handles(args)) {
            System.exit(GraphifyLiveVerifierCli.run(args, System.out, System.err));
        }
        if (ScenarioReviewCli.handles(args)) {
            System.exit(ScenarioReviewCli.run(args, System.out, System.err));
        }
        if (HumanReviewPacketCli.handles(args)) {
            System.exit(HumanReviewPacketCli.run(args, System.out, System.err));
        }
        if (Dev204Cli.handles(args)) return;
        SpringApplication.run(FdiApplication.class, args);
    }
}
