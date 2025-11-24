package com.example.voicebox.cloud.http;

import com.example.voicebox.cloud.AsrClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 一个面向阿里云语音识别（ASR）的简单 HTTP 客户端示例。
 *
 * 说明：
 * - 真正的阿里云 NLS/智能语音交互接口有较多参数和鉴权细节，请务必结合官方文档调整本实现。
 * - 这里采用「可配置 + 通用 JSON」的方式，你可以在服务端网关或阿里云 API 之间做一层适配。
 *
 * 约定请求体（你可以根据官方文档修改）：
 * {
 *   "app_key": "...",
 *   "token": "...",
 *   "format": "pcm",
 *   "sample_rate": 16000,
 *   "audio": "base64-encoded-pcm"
 * }
 *
 * 响应体预期：
 * { "result": "识别后的文本" }
 */
public class AliyunAsrClient implements AsrClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final String appKey;
    private final String token;
    private final int sampleRate;

    public AliyunAsrClient(String endpoint, String appKey, String token, int sampleRate) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun ASR endpoint must not be empty");
        }
        if (appKey == null || appKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun ASR appKey must not be empty");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun ASR token must not be empty");
        }
        this.endpoint = endpoint.trim();
        this.appKey = appKey.trim();
        this.token = token.trim();
        this.sampleRate = sampleRate;
    }

    public static AliyunAsrClient fromEnv() {
        String endpoint = firstNonEmpty(
                System.getProperty("voicebox.aliyun.asr.url"),
                System.getenv("ALIYUN_ASR_URL")
        );
        String appKey = firstNonEmpty(
                System.getProperty("voicebox.aliyun.appKey"),
                System.getenv("ALIYUN_APP_KEY")
        );
        String token = firstNonEmpty(
                System.getProperty("voicebox.aliyun.token"),
                System.getenv("ALIYUN_TOKEN")
        );
        String sampleRateStr = firstNonEmpty(
                System.getProperty("voicebox.aliyun.asr.sampleRate"),
                System.getenv("ALIYUN_ASR_SAMPLE_RATE")
        );
        int sampleRate = 16000;
        if (sampleRateStr != null) {
            try {
                sampleRate = Integer.parseInt(sampleRateStr);
            } catch (NumberFormatException ignore) {
                // keep default 16000
            }
        }
        return new AliyunAsrClient(endpoint, appKey, token, sampleRate);
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    @Override
    public String recognize(byte[] audioPcm) {
        try {
            String payload = buildRequestJson(audioPcm);
            String body = doPost(payload);
            return extractResult(body);
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Aliyun ASR API", e);
        }
    }

    private String buildRequestJson(byte[] audioPcm) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("app_key", appKey);
        root.put("token", token);
        root.put("format", "pcm");
        root.put("sample_rate", sampleRate);
        String audioBase64 = Base64.getEncoder().encodeToString(audioPcm);
        root.put("audio", audioBase64);
        return MAPPER.writeValueAsString(root);
    }

    private String doPost(String payload) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

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
                throw new IOException("HTTP " + status + " from Aliyun ASR API: " + body);
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

    private String extractResult(String body) {
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode result = root.path("result");
            if (!result.isMissingNode()) {
                return result.asText();
            }
        } catch (Exception ignore) {
            // ignore parse error, fallback to raw body
        }
        return body;
    }
}


