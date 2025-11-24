package com.example.voicebox.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.voicebox")
public class VoiceBoxWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceBoxWebApplication.class, args);
    }
}

