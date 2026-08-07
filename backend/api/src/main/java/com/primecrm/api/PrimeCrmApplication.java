package com.primecrm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.primecrm")
public class PrimeCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimeCrmApplication.class, args);
    }
}
