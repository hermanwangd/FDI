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
        if (Dev204Cli.handles(args)) return;
        SpringApplication.run(FdiApplication.class, args);
    }
}
