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
        if (Dev204Cli.handles(args)) return;
        SpringApplication.run(FdiApplication.class, args);
    }
}
