package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.DeviceApp;
import com.example.voicebox.app.device.chat.ChatMessage;
import com.example.voicebox.app.device.chat.ChatSession;
import com.example.voicebox.app.device.chat.ChatSessionRepository;
import com.example.voicebox.app.device.controller.dto.ChatMessageResponse;
import com.example.voicebox.app.device.controller.dto.ChatSessionResponse;
import com.example.voicebox.app.device.controller.dto.CreateSessionRequest;
import com.example.voicebox.app.device.controller.dto.WebChatRequest;
import com.example.voicebox.app.device.controller.dto.WebChatResponse;
import com.example.voicebox.app.device.controller.dto.WebTtsRequest;
import com.example.voicebox.app.device.db.DatabaseLogger;
import com.example.voicebox.cloud.http.HttpChatClient;
import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.ChatRequest;
import com.example.voicebox.cloud.ChatResponse;
import com.example.voicebox.cloud.VoiceStyle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DeviceApiController {

    // Cache for dynamic clients
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final ChatSessionRepository chatSessions;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper mapper = new ObjectMapper();

    public DeviceApiController(ChatSessionRepository chatSessions) {
        this.chatSessions = chatSessions;
    }

    @PostMapping("/chat")
    public WebChatResponse chat(@RequestBody WebChatRequest request) {
        // ... (Existing implementation kept for backward compatibility if needed)
        // For brevity, I'm replacing it with the same logic but ideally we use stream for everything.
        // But to answer the user request "Backend modified to dynamic return", let's keep this as fallback 
        // or just redirect to streaming if the client supports it? 
        // The user specifically asked "Can backend be modified to return content dynamically?".
        // So I will implement a new endpoint and update frontend.
        
        String normalizedModel = normalizeModel(request.getModel());
        try {
            ChatClient client = getClientForModel(normalizedModel);
            ChatSession session = chatSessions.ensureSession(
                    request.getSessionId(),
                    request.getText(),
                    normalizedModel,
                    request.getDeviceInfo()
            );

            chatSessions.saveMessage(session.getId(), "USER", request.getText());
            
            ChatResponse response = client.chat(new ChatRequest(Collections.emptyList(), request.getText()));
            chatSessions.saveMessage(session.getId(), "AI", response.getText());
            chatSessions.updateTitleIfEmpty(session.getId(), request.getText());
            
            try {
                DatabaseLogger.getInstance().logConversation(
                    "User: " + request.getText() + " | AI: " + response.getText(), 
                    "NEUTRAL"
                );
            } catch (Exception ignore) {}

            return new WebChatResponse(response.getText(), session.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter streamChat(@RequestBody WebChatRequest request) {
        // Long timeout for AI generation
        SseEmitter emitter = new SseEmitter(180_000L); 
        
        String normalizedModel = normalizeModel(request.getModel());
        
        executor.submit(() -> {
            try {
                ChatClient client = getClientForModel(normalizedModel);
                ChatSession session = chatSessions.ensureSession(
                        request.getSessionId(),
                        request.getText(),
                        normalizedModel,
                        request.getDeviceInfo()
                );

                chatSessions.saveMessage(session.getId(), "USER", request.getText());

                // Send session info first
                emitter.send(SseEmitter.event().name("session").data(
                    mapper.writeValueAsString(Collections.singletonMap("sessionId", session.getId()))
                ));

                StringBuilder aiResponseBuilder = new StringBuilder();

                client.streamChat(
                    new ChatRequest(Collections.emptyList(), request.getText()),
                    token -> {
                        try {
                            aiResponseBuilder.append(token);
                            // Send delta as JSON to handle special characters safely
                            emitter.send(SseEmitter.event().name("delta")
                                .data(Collections.singletonMap("text", token)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    error -> {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            emitter.completeWithError(error);
                        } catch (IOException ignored) {}
                    },
                    () -> {
                        // Complete
                        String fullText = aiResponseBuilder.toString();
                        chatSessions.saveMessage(session.getId(), "AI", fullText);
                        chatSessions.updateTitleIfEmpty(session.getId(), request.getText());
                        
                        try {
                            DatabaseLogger.getInstance().logConversation(
                                "User: " + request.getText() + " | AI: " + fullText, 
                                "NEUTRAL"
                            );
                        } catch (Exception ignore) {}
                        
                        emitter.complete();
                    }
                );

            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private ChatClient getClientForModel(String modelKey) {
        // Default to global engine if no model specified or "default"
        if (modelKey == null || modelKey.isEmpty() || "default".equalsIgnoreCase(modelKey)) {
            if (DeviceApp.GLOBAL_ONLINE_ENGINE == null) {
                throw new RuntimeException("Device Engine not initialized");
            }
            return DeviceApp.GLOBAL_ONLINE_ENGINE.getChatClient();
        }

        // Check cache
        if (clientCache.containsKey(modelKey)) {
            return clientCache.get(modelKey);
        }

        // Create new client for specific model
        String endpointId = System.getProperty("voicebox.chat.model." + modelKey.toLowerCase());
        if (endpointId == null) {
            // Fallback or error? Let's try to use the key as ID if property missing, or default
            System.out.println("[API] No explicit config for model '" + modelKey + "', using default.");
            if (DeviceApp.GLOBAL_ONLINE_ENGINE == null) {
                 throw new RuntimeException("Device Engine not initialized");
            }
            return DeviceApp.GLOBAL_ONLINE_ENGINE.getChatClient();
        }

        String url = System.getProperty("voicebox.chat.url");
        String apiKey = System.getProperty("voicebox.chat.apiKey");

        System.out.println("[API] Creating dynamic client for model: " + modelKey + " (" + endpointId + ")");
        ChatClient newClient = new HttpChatClient(url, apiKey, endpointId);
        clientCache.put(modelKey, newClient);
        return newClient;
    }

    @PostMapping("/tts")
    public byte[] tts(@RequestBody WebTtsRequest request) {
        if (DeviceApp.GLOBAL_ONLINE_ENGINE == null) {
             throw new RuntimeException("Device Engine not initialized");
        }
        try {
            return DeviceApp.GLOBAL_ONLINE_ENGINE.getTtsClient().synthesize(request.getText(), VoiceStyle.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException("TTS failed", e);
        }
    }

    @GetMapping("/chat/sessions")
    public List<ChatSessionResponse> listSessions() {
        return chatSessions.listSessions().stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/chat/sessions")
    public ChatSessionResponse createSession(@RequestBody CreateSessionRequest request) {
        String title = request.getTitle() == null || request.getTitle().isEmpty()
                ? "New Chat"
                : request.getTitle();
        ChatSession session = chatSessions.createSession(title, normalizeModel(request.getModel()), request.getDeviceInfo());
        return toSessionResponse(session);
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> sessionMessages(@PathVariable Long sessionId) {
        ChatSession session = chatSessions.findById(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        return chatSessions.listMessages(sessionId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        ChatSessionResponse resp = new ChatSessionResponse();
        resp.setId(session.getId());
        resp.setTitle(session.getTitle());
        resp.setModel(session.getModel());
        resp.setDeviceInfo(session.getDeviceInfo());
        resp.setCreatedAt(toIso(session.getCreatedAt()));
        resp.setUpdatedAt(toIso(session.getUpdatedAt()));
        return resp;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse resp = new ChatMessageResponse();
        resp.setId(message.getId());
        resp.setSessionId(message.getSessionId());
        resp.setRole(message.getRole());
        resp.setContent(message.getContent());
        resp.setCreatedAt(toIso(message.getCreatedAt()));
        return resp;
    }

    private String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private String normalizeModel(String modelKey) {
        if (modelKey == null || modelKey.trim().isEmpty() || "default".equalsIgnoreCase(modelKey)) {
            return "doubao";
        }
        return modelKey;
    }
}
