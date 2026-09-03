package com.featuredeliveryintelligence.fdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FdiApplication {
    public static void main(String[] args) {
        if (Dev204Cli.handles(args)) return;
        SpringApplication.run(FdiApplication.class, args);
    }
}
