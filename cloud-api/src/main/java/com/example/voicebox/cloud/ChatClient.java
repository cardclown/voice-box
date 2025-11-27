package com.example.voicebox.cloud;

import java.util.function.Consumer;

public interface ChatClient {

    ChatResponse chat(ChatRequest request);

    /**
     * Default implementation that falls back to blocking chat if not overridden.
     * Implementations should override this to support true streaming.
     */
    default void streamChat(ChatRequest request, Consumer<String> onToken, Consumer<Throwable> onError, Runnable onComplete) {
        try {
            ChatResponse response = chat(request);
            onToken.accept(response.getText());
            onComplete.run();
        } catch (Exception e) {
            onError.accept(e);
        }
    }
}
