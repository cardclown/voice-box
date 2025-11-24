package com.example.voicebox.cloud;

import java.util.List;

public class ChatRequest {

    private final List<String> history;
    private final String userText;

    public ChatRequest(List<String> history, String userText) {
        this.history = history;
        this.userText = userText;
    }

    public List<String> getHistory() {
        return history;
    }

    public String getUserText() {
        return userText;
    }
}
