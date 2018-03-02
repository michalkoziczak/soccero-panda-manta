package com.leanforge.soccero;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalTime;

@SpringBootApplication(scanBasePackages = {"com.leanforge"})
@EnableScheduling
public class SocceroSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocceroSpringApplication.class, args);
    }


    @Bean
    @Qualifier("resetTime")
    public LocalTime resetTime() {
        return LocalTime.of(5, 30);
    }
}
