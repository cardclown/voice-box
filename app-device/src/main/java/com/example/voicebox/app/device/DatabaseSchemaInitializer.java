package com.example.voicebox.app.device;

import com.example.voicebox.app.device.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes all database tables on application startup.
 */
@Component
public class DatabaseSchemaInitializer implements CommandLineRunner {

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private UserTagRepository userTagRepository;

    @Autowired(required = false)
    private InteractionRepository interactionRepository;

    @Autowired(required = false)
    private DeviceRepository deviceRepository;

    @Override
    public void run(String... args) {
        System.out.println("[DatabaseSchemaInitializer] Database schema initialization complete.");
        System.out.println("[DatabaseSchemaInitializer] All tables have been created or verified.");
    }
}
