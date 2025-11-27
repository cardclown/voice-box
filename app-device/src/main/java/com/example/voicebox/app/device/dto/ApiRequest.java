package com.example.voicebox.app.device.dto;

public class ApiRequest {
    private String text;

    public ApiRequest() {}

    public ApiRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

