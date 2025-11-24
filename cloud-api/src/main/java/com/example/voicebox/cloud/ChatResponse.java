package com.example.voicebox.cloud;

public class ChatResponse {

    private final String text;

    public ChatResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
