package com.example.voicebox.web.controller;

import com.example.voicebox.core.util.ConfigLoader;
import com.example.voicebox.cloud.*;
import com.example.voicebox.cloud.http.HttpCloudClients;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;

@RestController
@RequestMapping("/api")
public class VoiceBoxController {

    private ChatClient chatClient;
    private TtsClient ttsClient;
    private AsrClient asrClient;

    @PostConstruct
    public void init() {
        // Ensure config is loaded
        ConfigLoader.loadProperties();
        
        // Initialize clients
        this.chatClient = HttpCloudClients.chatClient();
        this.ttsClient = HttpCloudClients.ttsClient();
        this.asrClient = HttpCloudClients.asrClient();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody String message) {
        return chatClient.chat(new ChatRequest(Collections.emptyList(), message));
    }

    @PostMapping("/tts")
    public byte[] tts(@RequestBody String text) {
        return ttsClient.synthesize(text, VoiceStyle.DEFAULT);
    }
    
    // Simple health check
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}

