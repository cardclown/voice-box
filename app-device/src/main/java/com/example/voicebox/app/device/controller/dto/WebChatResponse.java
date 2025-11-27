package com.example.voicebox.app.device.controller.dto;

public class WebChatResponse {
    private final String text;
    private final Long sessionId;

    public WebChatResponse(String text, Long sessionId) {
        this.text = text;
        this.sessionId = sessionId;
    }

    public String getText() {
        return text;
    }

    public Long getSessionId() {
        return sessionId;
    }
}

