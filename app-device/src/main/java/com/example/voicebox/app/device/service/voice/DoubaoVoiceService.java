package com.example.voicebox.app.device.service.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DoubaoVoiceService {

    @Value("${voicebox.doubao.voice.appid}")
    private String appId;

    @Value("${voicebox.doubao.voice.token}")
    private String token;

    @Value("${voicebox.doubao.voice.secret}")
    private String secret;

    @Value("${voicebox.doubao.voice.url:wss://openspeech.bytedance.com/api/v1/tts}")
    private String ttsUrl;

    @Value("${voicebox.doubao.voice.stt.url:wss://openspeech.bytedance.com/api/v1/asr}")
    private String sttUrl;

    private final OkHttpClient httpClient;

    public DoubaoVoiceService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public CompletableFuture<String> speechToText(InputStream audioStream, String language) {
        log.info("开始语音识别 - language: {}", language);
        
        final CompletableFuture<String> resultFuture = new CompletableFuture<>();
        
        try {
            final byte[] audioData = readAllBytes(audioStream);
            String wsUrl = buildSTTUrl(language);
            
            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();
            
            WebSocketListener listener = new WebSocketListener() {
                private final StringBuilder textBuilder = new StringBuilder();
                
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("STT WebSocket连接已建立");
                    webSocket.send(ByteString.of(audioData));
                    webSocket.close(1000, "完成");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("收到STT响应: {}", text);
                    try {
                        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                        if (json.has("text")) {
                            textBuilder.append(json.get("text").getAsString());
                        }
                    } catch (Exception e) {
                        log.warn("解析STT响应失败: {}", text, e);
                        textBuilder.append(text);
                    }
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("STT WebSocket正在关闭 - code: {}, reason: {}", code, reason);
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("STT WebSocket已关闭 - code: {}, reason: {}", code, reason);
                    String result = textBuilder.toString();
                    if (result.isEmpty()) {
                        resultFuture.completeExceptionally(new RuntimeException("识别结果为空"));
                    } else {
                        resultFuture.complete(result);
                    }
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    String errorMsg = "STT WebSocket失败";
                    if (response != null) {
                        errorMsg += " - HTTP状态: " + response.code();
                        try {
                            if (response.body() != null) {
                                errorMsg += " - 响应: " + response.body().string();
                            }
                        } catch (Exception e) {
                            log.warn("无法读取响应体", e);
                        }
                    }
                    log.error(errorMsg, t);
                    resultFuture.completeExceptionally(new RuntimeException(errorMsg, t));
                }
            };
            
            httpClient.newWebSocket(request, listener);
                    
        } catch (Exception e) {
            log.error("语音识别失败", e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    public CompletableFuture<byte[]> textToSpeech(String text, String language, String voiceName) {
        log.info("开始语音合成 - text: {}, language: {}, voice: {}", text, language, voiceName);
        
        final CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();
        
        try {
            String wsUrl = buildTTSUrl(language, voiceName);
            
            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();
            
            WebSocketListener listener = new WebSocketListener() {
                private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
                
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("TTS WebSocket连接已建立");
                    webSocket.send(text);
                    webSocket.close(1000, "完成");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    log.debug("收到TTS音频数据: {} bytes", bytes.size());
                    try {
                        audioBuffer.write(bytes.toByteArray());
                    } catch (Exception e) {
                        log.error("写入音频缓冲区失败", e);
                    }
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("TTS WebSocket正在关闭 - code: {}, reason: {}", code, reason);
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("TTS WebSocket已关闭 - code: {}, reason: {}", code, reason);
                    byte[] audioData = audioBuffer.toByteArray();
                    if (audioData.length == 0) {
                        resultFuture.completeExceptionally(new RuntimeException("合成结果为空"));
                    } else {
                        log.info("语音合成完成: {} bytes", audioData.length);
                        resultFuture.complete(audioData);
                    }
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("TTS WebSocket失败", t);
                    resultFuture.completeExceptionally(t);
                }
            };
            
            httpClient.newWebSocket(request, listener);
                    
        } catch (Exception e) {
            log.error("语音合成失败", e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    private String buildSTTUrl(String language) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateSignature(timestamp);
        
        return String.format("%s?appid=%s&token=%s&timestamp=%d&signature=%s&language=%s",
                sttUrl, appId, token, timestamp, signature, language);
    }

    private String buildTTSUrl(String language, String voiceName) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateSignature(timestamp);
        
        return String.format("%s?appid=%s&token=%s&timestamp=%d&signature=%s&language=%s&voice=%s",
                ttsUrl, appId, token, timestamp, signature, language, voiceName);
    }

    private String generateSignature(long timestamp) {
        try {
            String data = appId + token + timestamp;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        buffer.flush();
        return buffer.toByteArray();
    }

    public interface AudioDataCallback {
        void onAudioData(byte[] data, boolean isLast);
        void onError(Throwable error);
        void onComplete();
    }
}
