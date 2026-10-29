package com.renegade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RenegadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenegadeApplication.class, args);
    }

}
