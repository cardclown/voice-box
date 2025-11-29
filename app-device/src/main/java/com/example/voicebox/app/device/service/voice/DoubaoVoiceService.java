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

/**
 * 豆包实时语音服务
 * 提供语音识别(STT)和语音合成(TTS)功能
 */
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

    /**
     * 语音识别 (Speech-to-Text)
     * 
     * @param audioStream 音频流
     * @param language 语言代码 (zh-CN, en-US等)
     * @return 识别的文本
     */
    public CompletableFuture<String> speechToText(InputStream audioStream, String language) {
        log.info("开始语音识别 - language: {}", language);
        
        final CompletableFuture<String> resultFuture = new CompletableFuture<>();
        
        try {
            // 读取音频数据
            final byte[] audioData = readAllBytes(audioStream);
            
            // 构建WebSocket连接URL
            String wsUrl = buildSTTUrl(language);
            
            // 创建WebSocket请求
            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();
            
            // 创建WebSocket监听器
            WebSocketListener listener = new WebSocketListener() {
                private final StringBuilder textBuilder = new StringBuilder();
                
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("STT WebSocket连接已建立");
                    
                    // 发送音频数据
                    webSocket.send(ByteString.of(audioData));
                    webSocket.close(1000, "完成");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("收到STT响应: {}", text);
                    
                    try {
                        // 解析JSON响应
                        JsonObject jsonmplete(result);
                    }
                    
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    log.error("STT WebSocket错误", error);
                    resultFuture.completeExceptionally(error);
                    WebSocket.Listener.super.onError(webSocket, error);
                }
                
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    log.info("STT WebSocket连接关闭 - code: {}, reason: {}", statusCode, reason);
                    if (!resultFuture.isDone()) {
                        resultFuture.complete(textBuilder.toString());
                    }
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
            };
            
            // 建立WebSocket连接
            httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(wsUrl), listener);
                    
        } catch (Exception e) {
            log.error("语音识别失败", e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    /**
     * 语音合成 (Text-to-Speech)
     * 
     * @param text 要合成的文本
     * @param language 语言代码
     * @param voiceName 音色名称
     * @return 音频数据
     */
    public CompletableFuture<byte[]> textToSpeech(String text, String language, String voiceName) {
        log.info("开始语音合成 - text: {}, language: {}, voice: {}", text, language, voiceName);
        
        CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();
        
        try {
            // 构建WebSocket连接URL
            String wsUrl = buildTTSUrl(language, voiceName);
            
            // 创建WebSocket监听器
            WebSocket.Listener listener = new WebSocket.Listener() {
                private ByteBuffer audioBuffer = ByteBuffer.allocate(1024 * 1024); // 1MB缓冲区
                
                @Override
                public void onOpen(WebSocket webSocket) {
                    log.info("TTS WebSocket连接已建立");
                    WebSocket.Listener.super.onOpen(webSocket);
                    
                    // 发送文本数据
                    webSocket.sendText(text, true);
                }
                
                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    log.debug("收到TTS音频数据: {} bytes, last: {}", data.remaining(), last);
                    audioBuffer.put(data);
                    
                    if (last) {
                        byte[] audioData = new byte[audioBuffer.position()];
                        audioBuffer.flip();
                        audioBuffer.get(audioData);
                        
                        log.info("语音合成完成: {} bytes", audioData.length);
                        resultFuture.complete(audioData);
                    }
                    
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    log.error("TTS WebSocket错误", error);
                    resultFuture.completeExceptionally(error);
                    WebSocket.Listener.super.onError(webSocket, error);
                }
                
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    log.info("TTS WebSocket连接关闭 - code: {}, reason: {}", statusCode, reason);
                    if (!resultFuture.isDone()) {
                        byte[] audioData = new byte[audioBuffer.position()];
                        audioBuffer.flip();
                        audioBuffer.get(audioData);
                        resultFuture.complete(audioData);
                    }
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
            };
            
            // 建立WebSocket连接
            httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(wsUrl), listener);
                    
        } catch (Exception e) {
            log.error("语音合成失败", e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    /**
     * 构建STT WebSocket URL
     */
    private String buildSTTUrl(String language) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateSignature(timestamp);
        
        return String.format("%s?appid=%s&token=%s&timestamp=%d&signature=%s&language=%s",
                sttUrl, appId, token, timestamp, signature, language);
    }

    /**
     * 构建TTS WebSocket URL
     */
    private String buildTTSUrl(String language, String voiceName) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateSignature(timestamp);
        
        return String.format("%s?appid=%s&token=%s&timestamp=%d&signature=%s&language=%s&voice=%s",
                ttsUrl, appId, token, timestamp, signature, language, voiceName);
    }

    /**
     * 生成签名
     */
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

    /**
     * 流式语音合成
     * 用于实时播放场景
     * 
     * @param text 要合成的文本
     * @param language 语言代码
     * @param voiceName 音色名称
     * @param callback 音频数据回调
     */
    public void streamTextToSpeech(String text, String language, String voiceName, AudioDataCallback callback) {
        log.info("开始流式语音合成 - text: {}, language: {}, voice: {}", text, language, voiceName);
        
        try {
            String wsUrl = buildTTSUrl(language, voiceName);
            
            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    log.info("流式TTS WebSocket连接已建立");
                    WebSocket.Listener.super.onOpen(webSocket);
                    webSocket.sendText(text, true);
                }
                
                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    byte[] audioChunk = new byte[data.remaining()];
                    data.get(audioChunk);
                    
                    log.debug("收到音频片段: {} bytes, last: {}", audioChunk.length, last);
                    callback.onAudioData(audioChunk, last);
                    
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    log.error("流式TTS WebSocket错误", error);
                    callback.onError(error);
                    WebSocket.Listener.super.onError(webSocket, error);
                }
                
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    log.info("流式TTS WebSocket连接关闭 - code: {}, reason: {}", statusCode, reason);
                    callback.onComplete();
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
            };
            
            httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(wsUrl), listener);
                    
        } catch (Exception e) {
            log.error("流式语音合成失败", e);
            callback.onError(e);
        }
    }

    /**
     * 音频数据回调接口
     */
    public interface AudioDataCallback {
        void onAudioData(byte[] data, boolean isLast);
        void onError(Throwable error);
        void onComplete();
    }
}
