package com.example.voicebox.cloud.http;

import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.ChatRequest;
import com.example.voicebox.cloud.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用 HTTP 版 ChatClient，实现「OpenAI/DeepSeek/豆包风格」的 chat/completions 调用。
 *
 * - 通过 POST 调用你在配置中指定的云端大模型 HTTP 接口
 * - 请求体 JSON 结构：兼容 OpenAI Chat Completions：
 *   {"model": "...", "messages": [{"role":"user","content":"..."}]}
 * - 响应体：期望为 OpenAI 兼容格式，从 choices[0].message.content 里提取最终文本
 *
 * 你可以在运行时通过系统属性或环境变量来配置：
 * - VOICEBOX_CHAT_URL / voicebox.chat.url       ：目标 HTTP 接口地址（必填，按官方文档配置）
 * - VOICEBOX_CHAT_API_KEY / voicebox.chat.apiKey：API Key（必填，按官方文档配置）
 * - VOICEBOX_CHAT_MODEL / voicebox.chat.model   ：模型名称（可选，未配置时由各厂商默认）
 *
 * 注意：具体 endpoint、model 名请以 DeepSeek/豆包官方文档为准，这里只提供通用结构。
 */
public class HttpChatClient implements ChatClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final String apiKey;
    private final String model;

    public String getEndpoint() { return endpoint; }
    public String getModel() { return model; }

    public HttpChatClient(String endpoint, String apiKey, String model) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP ChatClient endpoint must not be empty");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP ChatClient apiKey must not be empty");
        }
        this.endpoint = endpoint.trim();
        this.apiKey = apiKey.trim();
        this.model = model;
    }

    /**
     * 从系统属性 / 环境变量创建客户端，方便在不同云厂商之间切换。
     */
    public static HttpChatClient fromEnv() {
        String endpoint = firstNonEmpty(
                System.getProperty("voicebox.chat.url"),
                System.getenv("VOICEBOX_CHAT_URL")
        );
        String apiKey = firstNonEmpty(
                System.getProperty("voicebox.chat.apiKey"),
                System.getenv("VOICEBOX_CHAT_API_KEY")
        );
        String model = firstNonEmpty(
                System.getProperty("voicebox.chat.model"),
                System.getenv("VOICEBOX_CHAT_MODEL")
        );

        return new HttpChatClient(endpoint, apiKey, model);
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String payload = buildChatCompletionsJson(request.getHistory(), request.getUserText(), false);
            String body = doPost(payload);
            String text = extractContentFromResponse(body);
            return new ChatResponse(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to call cloud chat API", e);
        }
    }

    @Override
    public void streamChat(ChatRequest request, Consumer<String> onToken, Consumer<Throwable> onError, Runnable onComplete) {
        HttpURLConnection conn = null;
        try {
            String payload = buildChatCompletionsJson(request.getHistory(), request.getUserText(), true);
            
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");

            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.connect();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                 // Try to read error body
                InputStream es = conn.getErrorStream();
                String errBody = readAll(es);
                throw new IOException("HTTP " + status + " from stream API: " + errBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            String token = extractContentFromChunk(data);
                            if (token != null && !token.isEmpty()) {
                                onToken.accept(token);
                            }
                        } catch (Exception e) {
                            // Ignore bad chunks
                        }
                    }
                }
            }
            onComplete.run();

        } catch (Exception e) {
            onError.accept(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 构造兼容 OpenAI / DeepSeek / 豆包 的 chat completions JSON。
     */
    private String buildChatCompletionsJson(List<String> history, String userText, boolean stream) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        if (model != null && !model.isEmpty()) {
            root.put("model", model);
        }
        root.put("stream", stream);

        ArrayNode messages = root.putArray("messages");

        // 简单做法：把历史和当前说话拼成一条 user 消息；后续可以根据需要拆成多轮对话。
        StringBuilder contentBuilder = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            for (String h : history) {
                if (h == null || h.trim().isEmpty()) {
                    continue;
                }
                contentBuilder.append(h.trim()).append("\n");
            }
        }
        if (userText != null) {
            contentBuilder.append(userText.trim());
        }

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", contentBuilder.toString());

        return MAPPER.writeValueAsString(root);
    }

    /**
     * 从 OpenAI 风格响应 JSON 中提取 choices[0].message.content。
     */
    private String extractContentFromResponse(String body) {
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                JsonNode message = first.path("message");
                JsonNode content = message.path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
            }
        } catch (Exception ignore) {
        }
        return body;
    }
    
    /**
     * Extract content from stream chunk: choices[0].delta.content
     */
    private String extractContentFromChunk(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                JsonNode delta = first.path("delta");
                JsonNode content = delta.path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
                // DeepSeek sometimes puts reasoning in "reasoning_content"
                JsonNode reasoning = delta.path("reasoning_content");
                if (!reasoning.isMissingNode()) {
                    return reasoning.asText();
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private String doPost(String payload) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.connect();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int status = conn.getResponseCode();
            InputStream is = status >= 200 && status < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String body = readAll(is);
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " from cloud chat API: " + body);
            }
            return body;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // --- Temporary Test Main ---
    public static void main(String[] args) throws Exception {
        // ... (Test code omitted for brevity)
    }
}
