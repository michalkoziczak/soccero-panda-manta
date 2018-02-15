package com.leanforge.soccero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.leanforge"})
public class SocceroSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocceroSpringApplication.class, args);
    }

}
