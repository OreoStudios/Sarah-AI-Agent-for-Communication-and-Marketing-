package com.oreostudios.sarah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sarah AI Agent — autonomous multi-agent marketing platform for Oreo Studios.
 *
 * <p>Implemented as a modular monolith with Spring Modulith. Each top-level package
 * under {@code com.oreostudios.sarah} is an application module with explicit boundaries;
 * modules communicate through published interfaces and Spring application events.
 */
@Modulithic(systemName = "Sarah AI Agent")
@SpringBootApplication
@EnableScheduling
public class SarahApplication {

    public static void main(String[] args) {
        SpringApplication.run(SarahApplication.class, args);
    }
}
