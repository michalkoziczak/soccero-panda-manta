package com.leanforge.soccero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.leanforge"})
@EnableScheduling
public class SocceroSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocceroSpringApplication.class, args);
    }

}
