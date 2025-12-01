package com.example.voicebox.app.device.service.voice;

import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 流式语音服务
 * 实现实时流式TTS播放
 */
@Service
public class StreamingVoiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingVoiceService.class);
    
    @Autowired
    private TextSegmentationService segmentationService;
    
    @Autowired
    private VoiceServiceProxy voiceServiceProxy;
    
    @Autowired
    private VoiceStorageService storageService;
    
    @Autowired
    private VoiceMessageRepository voiceMessageRepository;
    
    // 线程池用于异步处理
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 存储活跃的流式会话
    private final Map<String, StreamingSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * 流式会话
     */
    private static class StreamingSession {
        String sessionId;
        SseEmitter emitter;
        boolean stopped;
        ByteArrayOutputStream audioBuffer;
        List<String> audioSegments;
        
        StreamingSession(String sessionId, SseEmitter emitter) {
            this.sessionId = sessionId;
            this.emitter = emitter;
            this.stopped = false;
            this.audioBuffer = new ByteArrayOutputStream();
            this.audioSegments = new java.util.ArrayList<>();
        }
    }
    
    /**
     * 开始流式TTS
     * 
     * @param sessionId 会话ID
     * @param text 要转换的文本
     * @param userId 用户ID
     * @param language 语言代码
     * @return SSE发射器
     */
    public SseEmitter startStreaming(String sessionId, String text, Long userId, String language) {
        logger.info("开始流式TTS: sessionId={}, textLength={}, userId={}", 
                    sessionId, text.length(), userId);
        
        // 创建SSE发射器（30秒超时）
        SseEmitter emitter = new SseEmitter(30000L);
        
        // 创建会话
        StreamingSession session = new StreamingSession(sessionId, emitter);
        activeSessions.put(sessionId, session);
        
        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            logger.info("流式TTS完成: sessionId={}", sessionId);
            activeSessions.remove(sessionId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("流式TTS超时: sessionId={}", sessionId);
            activeSessions.remove(sessionId);
        });
        
        emitter.onError((ex) -> {
            logger.error("流式TTS错误: sessionId={}", sessionId, ex);
            activeSessions.remove(sessionId);
        });
        
        // 异步处理流式TTS
        executorService.submit(() -> processStreaming(session, text, userId, language));
        
        return emitter;
    }
    
    /**
     * 停止流式TTS
     */
    public void stopStreaming(String sessionId) {
        StreamingSession session = activeSessions.get(sessionId);
        if (session != null) {
            logger.info("停止流式TTS: sessionId={}", sessionId);
            session.stopped = true;
            session.emitter.complete();
            activeSessions.remove(sessionId);
        }
    }
    
    /**
     * 处理流式TTS
     */
    private void processStreaming(StreamingSession session, String text, Long userId, String language) {
        try {
            // 1. 分段文本
            List<String> segments = segmentationService.segmentText(text);
            logger.info("文本分段完成: sessionId={}, segments={}", session.sessionId, segments.size());
            
            // 发送开始事件
            Map<String, Object> startData = new HashMap<>();
            startData.put("totalSegments", segments.size());
            startData.put("estimatedDuration", segmentationService.estimateDuration(text));
            sendEvent(session, "start", startData);
            
            // 2. 逐段转换并发送
            int segmentIndex = 0;
            for (String segment : segments) {
                if (session.stopped) {
                    logger.info("流式TTS已停止: sessionId={}", session.sessionId);
                    break;
                }
                
                try {
                    // 调用TTS服务（使用默认音色）
                    byte[] audioData = voiceServiceProxy.textToSpeech(segment, language, "zh_female_qingxin");
                    
                    if (audioData != null && audioData.length > 0) {
                        // 添加到缓冲区
                        session.audioBuffer.write(audioData);
                        
                        // 保存音频段
                        String segmentPath = storageService.saveAudioSegment(
                            audioData, 
                            String.format("%s_segment_%d", session.sessionId, segmentIndex)
                        );
                        session.audioSegments.add(segmentPath);
                        
                        // 发送音频段事件
                        Map<String, Object> segmentData = new HashMap<>();
                        segmentData.put("index", segmentIndex);
                        segmentData.put("audioUrl", "/api/voice/audio/" + segmentPath);
                        segmentData.put("text", segment);
                        segmentData.put("duration", segmentationService.estimateDuration(segment));
                        sendEvent(session, "segment", segmentData);
                        
                        segmentIndex++;
                        
                        logger.debug("发送音频段: sessionId={}, index={}, size={}", 
                                    session.sessionId, segmentIndex, audioData.length);
                    }
                    
                } catch (Exception e) {
                    logger.error("TTS转换失败: sessionId={}, segment={}", 
                                session.sessionId, segment, e);
                    // 继续处理下一段
                }
            }
            
            // 3. 合并音频并保存
            if (!session.stopped && session.audioBuffer.size() > 0) {
                byte[] completeAudio = session.audioBuffer.toByteArray();
                String completePath = storageService.saveAudioSegment(
                    completeAudio, 
                    session.sessionId + "_complete"
                );
                
                // 保存到数据库
                VoiceMessage voiceMessage = new VoiceMessage();
                voiceMessage.setUserId(userId);
                voiceMessage.setFileId(session.sessionId);
                voiceMessage.setFilePath(completePath);
                voiceMessage.setFileSize((long) completeAudio.length);
                voiceMessage.setDuration((int) segmentationService.estimateDuration(text));
                voiceMessage.setFormat("mp3");
                voiceMessage.setLanguage(language);
                voiceMessage.setRecognizedText(text);
                voiceMessage.setIsInput(false);
                voiceMessage.setCreatedAt(LocalDateTime.now());
                voiceMessageRepository.save(voiceMessage);
                
                // 发送完成事件
                Map<String, Object> completeData = new HashMap<>();
                completeData.put("audioUrl", "/api/voice/audio/" + completePath);
                completeData.put("totalSize", completeAudio.length);
                completeData.put("segments", session.audioSegments.size());
                sendEvent(session, "complete", completeData);
                
                logger.info("流式TTS完成并保存: sessionId={}, size={}", 
                           session.sessionId, completeAudio.length);
            }
            
            // 完成SSE
            session.emitter.complete();
            
        } catch (Exception e) {
            logger.error("流式TTS处理失败: sessionId={}", session.sessionId, e);
            try {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("message", e.getMessage());
                sendEvent(session, "error", errorData);
                session.emitter.completeWithError(e);
            } catch (Exception ex) {
                logger.error("发送错误事件失败", ex);
            }
        }
    }
    
    /**
     * 发送SSE事件
     */
    private void sendEvent(StreamingSession session, String eventType, Map<String, Object> data) {
        try {
            session.emitter.send(SseEmitter.event()
                .name(eventType)
                .data(data));
        } catch (IOException e) {
            logger.error("发送SSE事件失败: sessionId={}, eventType={}", 
                        session.sessionId, eventType, e);
        }
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
