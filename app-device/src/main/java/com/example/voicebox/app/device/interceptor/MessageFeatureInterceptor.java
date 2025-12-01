package com.example.voicebox.app.device.interceptor;

import com.example.voicebox.app.device.domain.ConversationFeature;
import com.example.voicebox.app.device.repository.ConversationFeatureRepository;
import com.example.voicebox.app.device.service.FeatureExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 消息特征提取拦截器
 * 在用户发送消息时自动提取特征并保存
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Component
public class MessageFeatureInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageFeatureInterceptor.class);
    
    @Autowired
    private FeatureExtractionService featureExtractionService;
    
    @Autowired
    private ConversationFeatureRepository conversationFeatureRepository;
    
    /**
     * 异步处理消息特征提取
     * 不阻塞主流程
     */
    @Async
    public void processMessage(Long userId, Long sessionId, Long messageId, String messageContent) {
        try {
            logger.info("开始提取消息特征 - userId: {}, messageId: {}", userId, messageId);
            
            // 检查是否已经提取过
            if (conversationFeatureRepository.existsByMessageId(messageId)) {
                logger.debug("消息特征已存在，跳过提取 - messageId: {}", messageId);
                return;
            }
            
            // 提取特征
            ConversationFeature feature = featureExtractionService.extractFeatures(
                userId, sessionId, messageId, messageContent
            );
            
            // 保存到数据库
            conversationFeatureRepository.create(feature);
            
            logger.info("消息特征提取完成 - messageId: {}, 词数: {}, 情感: {}", 
                messageId, feature.getWordCount(), feature.getSentimentScore());
            
        } catch (Exception e) {
            logger.error("消息特征提取失败 - messageId: " + messageId, e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 同步处理消息特征提取（用于需要立即获取结果的场景）
     */
    public ConversationFeature processMessageSync(Long userId, Long sessionId, Long messageId, String messageContent) {
        try {
            logger.info("同步提取消息特征 - userId: {}, messageId: {}", userId, messageId);
            
            // 检查是否已经提取过
            ConversationFeature existing = conversationFeatureRepository.findByMessageId(messageId);
            if (existing != null) {
                logger.debug("返回已存在的消息特征 - messageId: {}", messageId);
                return existing;
            }
            
            // 提取特征
            ConversationFeature feature = featureExtractionService.extractFeatures(
                userId, sessionId, messageId, messageContent
            );
            
            // 保存到数据库
            return conversationFeatureRepository.create(feature);
            
        } catch (Exception e) {
            logger.error("同步消息特征提取失败 - messageId: " + messageId, e);
            return null;
        }
    }
    
    /**
     * 批量处理历史消息
     */
    @Async
    public void processHistoricalMessages(Long userId, Long sessionId, 
                                         java.util.List<java.util.Map<String, Object>> messages) {
        try {
            logger.info("开始批量处理历史消息 - userId: {}, 消息数: {}", userId, messages.size());
            
            java.util.List<ConversationFeature> features = new java.util.ArrayList<>();
            
            for (java.util.Map<String, Object> msg : messages) {
                Long messageId = (Long) msg.get("id");
                String content = (String) msg.get("content");
                
                // 跳过已处理的消息
                if (conversationFeatureRepository.existsByMessageId(messageId)) {
                    continue;
                }
                
                ConversationFeature feature = featureExtractionService.extractFeatures(
                    userId, sessionId, messageId, content
                );
                features.add(feature);
            }
            
            // 批量保存
            if (!features.isEmpty()) {
                conversationFeatureRepository.batchInsert(features);
                logger.info("历史消息批量处理完成 - 处理数量: {}", features.size());
            }
            
        } catch (Exception e) {
            logger.error("批量处理历史消息失败 - userId: " + userId, e);
        }
    }
}
