package com.example.voicebox.cloud.http;

import com.example.voicebox.cloud.TtsClient;
import com.example.voicebox.cloud.VoiceStyle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 一个面向阿里云语音合成（TTS）的简单 HTTP 客户端示例。
 *
 * 说明：
 * - 真正的阿里云 TTS 接口可能返回二进制音频流或 JSON+Base64，请务必结合官方文档调整本实现。
 * - 这里采用「可配置 + 通用 JSON 请求，二进制响应」的方式，方便你在服务端或网关中做适配。
 *
 * 约定请求体（可根据官方文档修改）：
 * {
 *   "app_key": "...",
 *   "token": "...",
 *   "text": "...",
 *   "voice": "xiaoyun",
 *   "format": "wav",
 *   "sample_rate": 16000
 * }
 *
 * 响应体预期：
 * - 直接返回音频二进制流（推荐在网关层把阿里云返回的音频流原样透传给本客户端）。
 */
public class AliyunTtsClient implements TtsClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final String appKey;
    private final String token;
    private final String voice;
    private final String format;
    private final int sampleRate;

    public AliyunTtsClient(String endpoint,
                           String appKey,
                           String token,
                           String voice,
                           String format,
                           int sampleRate) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun TTS endpoint must not be empty");
        }
        if (appKey == null || appKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun TTS appKey must not be empty");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Aliyun TTS token must not be empty");
        }
        this.endpoint = endpoint.trim();
        this.appKey = appKey.trim();
        this.token = token.trim();
        this.voice = (voice != null && !voice.trim().isEmpty()) ? voice.trim() : "xiaoyun";
        this.format = (format != null && !format.trim().isEmpty()) ? format.trim() : "wav";
        this.sampleRate = sampleRate > 0 ? sampleRate : 16000;
    }

    public static AliyunTtsClient fromEnv() {
        String endpoint = firstNonEmpty(
                System.getProperty("voicebox.aliyun.tts.url"),
                System.getenv("ALIYUN_TTS_URL")
        );
        String appKey = firstNonEmpty(
                System.getProperty("voicebox.aliyun.appKey"),
                System.getenv("ALIYUN_APP_KEY")
        );
        String token = firstNonEmpty(
                System.getProperty("voicebox.aliyun.token"),
                System.getenv("ALIYUN_TOKEN")
        );
        String voice = firstNonEmpty(
                System.getProperty("voicebox.aliyun.tts.voice"),
                System.getenv("ALIYUN_TTS_VOICE")
        );
        String format = firstNonEmpty(
                System.getProperty("voicebox.aliyun.tts.format"),
                System.getenv("ALIYUN_TTS_FORMAT")
        );
        String sampleRateStr = firstNonEmpty(
                System.getProperty("voicebox.aliyun.tts.sampleRate"),
                System.getenv("ALIYUN_TTS_SAMPLE_RATE")
        );
        int sampleRate = 16000;
        if (sampleRateStr != null) {
            try {
                sampleRate = Integer.parseInt(sampleRateStr);
            } catch (NumberFormatException ignore) {
                // keep default
            }
        }
        return new AliyunTtsClient(endpoint, appKey, token, voice, format, sampleRate);
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
    public byte[] synthesize(String text, VoiceStyle style) {
        try {
            String payload = buildRequestJson(text, style);
            return doPostForBytes(payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Aliyun TTS API", e);
        }
    }

    private String buildRequestJson(String text, VoiceStyle style) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("app_key", appKey);
        root.put("token", token);
        root.put("text", text);
        root.put("voice", chooseVoice(style));
        root.put("format", format);
        root.put("sample_rate", sampleRate);
        return MAPPER.writeValueAsString(root);
    }

    private String chooseVoice(VoiceStyle style) {
        // 这里简单用同一个 voice，你可以根据 style 映射到阿里云不同音色
        return voice;
    }

    private byte[] doPostForBytes(String payload) throws IOException {
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
            byte[] audio = readAllBytes(is);
            if (status < 200 || status >= 300) {
                String msg = new String(audio, StandardCharsets.UTF_8);
                throw new IOException("HTTP " + status + " from Aliyun TTS API: " + msg);
            }
            return audio;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            return new byte[0];
        }
        try (InputStream in = is; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        }
    }
}


