#!/bin/bash
# 修复服务器上损坏的DoubaoVoiceService.java文件

SERVER="root@129.211.180.183"
TARGET_FILE="/opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/service/voice/DoubaoVoiceService.java"

echo "正在修复DoubaoVoiceService.java..."

# 使用cat和heredoc直接在服务器上创建文件
# 这样可以避免文件传输过程中的编码问题

ssh $SERVER bash << 'ENDSSH'
cat > /opt/voicebox/app-device/src/main/java/com/example/voicebox/app/device/service/voice/DoubaoVoiceService.java << 'EOFFILE'
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
        log.info("Starting speech recognition - language: {}", language);
        
        final CompletableFuture<String> resultFuture = new CompletableFuture<>();
        
        try {
            final byte[] audioData = readAllBytes(audioStream);
            String wsUrl = buildSTTUrl(language);
            Request request = new Request.Builder().url(wsUrl).build();
            
            WebSocketListener listener = new WebSocketListener() {
                private final StringBuilder textBuilder = new StringBuilder();
                
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("STT WebSocket connected");
                    webSocket.send(ByteString.of(audioData));
                    webSocket.close(1000, "Done");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("Received STT response: {}", text);
                    try {
                        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                        if (json.has("text")) {
                            textBuilder.append(json.get("text").getAsString());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse STT response: {}", text, e);
                        textBuilder.append(text);
                    }
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("STT WebSocket closing - code: {}, reason: {}", code, reason);
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("STT WebSocket closed - code: {}, reason: {}", code, reason);
                    String result = textBuilder.toString();
                    if (result.isEmpty()) {
                        resultFuture.completeExceptionally(new RuntimeException("Empty recognition result"));
                    } else {
                        resultFuture.complete(result);
                    }
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("STT WebSocket failed", t);
                    resultFuture.completeExceptionally(t);
                }
            };
            
            httpClient.newWebSocket(request, listener);
                    
        } catch (Exception e) {
            log.error("Speech recognition failed", e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    public CompletableFuture<byte[]> textToSpeech(String text, String language, String voiceName) {
        log.info("Starting speech synthesis - text: {}, language: {}, voice: {}", text, language, voiceName);
        
        final CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();
        
        try {
            String wsUrl = buildTTSUrl(language, voiceName);
            Request request = new Request.Builder().url(wsUrl).build();
            
            WebSocketListener listener = new WebSocketListener() {
                private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
                
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("TTS WebSocket connected");
                    webSocket.send(text);
                    webSocket.close(1000, "Done");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    log.debug("Received TTS audio data: {} bytes", bytes.size());
                    try {
                        audioBuffer.write(bytes.toByteArray());
                    } catch (Exception e) {
                        log.error("Failed to write audio buffer", e);
                    }
                }
                
                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("TTS WebSocket closing - code: {}, reason: {}", code, reason);
                    webSocket.close(1000, null);
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("TTS WebSocket closed - code: {}, reason: {}", code, reason);
                    byte[] audioData = audioBuffer.toByteArray();
                    if (audioData.length == 0) {
                        resultFuture.completeExceptionally(new RuntimeException("Empty synthesis result"));
                    } else {
                        log.info("Speech synthesis completed: {} bytes", audioData.length);
                        resultFuture.complete(audioData);
                    }
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("TTS WebSocket failed", t);
                    resultFuture.completeExceptionally(t);
                }
            };
            
            httpClient.newWebSocket(request, listener);
                    
        } catch (Exception e) {
            log.error("Speech synthesis failed", e);
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
            log.error("Failed to generate signature", e);
            throw new RuntimeException("Failed to generate signature", e);
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
EOFFILE

echo "File created successfully"
ENDSSH

echo "✓ DoubaoVoiceService.java 已修复"
echo "现在编译项目..."

ssh $SERVER 'cd /opt/voicebox && mvn clean install -DskipTests'

if [ $? -eq 0 ]; then
    echo "✓ 编译成功！"
    echo "重启服务..."
    ssh $SERVER 'systemctl restart voicebox-backend'
    sleep 10
    echo "✓ 服务已重启"
    echo ""
    echo "测试API..."
    ssh $SERVER 'curl -s http://localhost:10088/actuator/health'
    echo ""
else
    echo "✗ 编译失败"
    exit 1
fi
