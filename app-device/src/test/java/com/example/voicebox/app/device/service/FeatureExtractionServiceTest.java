package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.ConversationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 特征提取服务测试
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@SpringBootTest
public class FeatureExtractionServiceTest {
    
    @Autowired
    private FeatureExtractionService featureExtractionService;
    
    @Test
    public void testExtractBasicFeatures() {
        // 测试基础特征提取
        String message = "你好，我想学习Java编程，请问有什么好的教程推荐吗？";
        
        ConversationFeature feature = featureExtractionService.extractFeatures(
            1L, 1L, 1L, message
        );
        
        assertNotNull(feature);
        assertEquals(1L, feature.getUserId());
        assertEquals(1L, feature.getSessionId());
        assertEquals(1L, feature.getMessageId());
        
        // 验证消息长度
        assertTrue(feature.getMessageLength() > 0);
        
        // 验证词数
        assertTrue(feature.getWordCount() > 0);
        
        // 验证句子数
        assertTrue(feature.getSentenceCount() > 0);
    }
    
    @Test
    public void testSentimentAnalysis() {
        // 测试正面情感
        String positiveMessage = "太好了！这个功能很棒，我很喜欢！";
        ConversationFeature positiveFeature = featureExtractionService.extractFeatures(
            1L, 1L, 2L, positiveMessage
        );
        
        assertNotNull(positiveFeature.getSentimentScore());
        assertTrue(positiveFeature.getSentimentScore().compareTo(BigDecimal.ZERO) > 0,
            "正面消息应该有正面情感分数");
        
        // 测试负面情感
        String negativeMessage = "这个太差了，很失望，不好用。";
        ConversationFeature negativeFeature = featureExtractionService.extractFeatures(
            1L, 1L, 3L, negativeMessage
        );
        
        assertNotNull(negativeFeature.getSentimentScore());
        assertTrue(negativeFeature.getSentimentScore().compareTo(BigDecimal.ZERO) < 0,
            "负面消息应该有负面情感分数");
    }
    
    @Test
    public void testIntentDetection() {
        // 测试问题意图
        String questionMessage = "这个怎么用？为什么会这样？";
        ConversationFeature questionFeature = featureExtractionService.extractFeatures(
            1L, 1L, 4L, questionMessage
        );
        
        assertEquals("question", questionFeature.getIntent());
        assertTrue(questionFeature.getQuestionCount() > 0);
        
        // 测试请求意图
        String requestMessage = "请帮我解决这个问题。";
        ConversationFeature requestFeature = featureExtractionService.extractFeatures(
            1L, 1L, 5L, requestMessage
        );
        
        assertEquals("request", requestFeature.getIntent());
    }
    
    @Test
    public void testTopicExtraction() {
        // 测试技术主题
        String techMessage = "我在学习Java编程和数据库开发。";
        ConversationFeature techFeature = featureExtractionService.extractFeatures(
            1L, 1L, 6L, techMessage
        );
        
        assertNotNull(techFeature.getTopicList());
        assertTrue(techFeature.getTopicList().contains("技术"));
        
        // 测试学习主题
        String studyMessage = "我需要学习新的课程，准备考试。";
        ConversationFeature studyFeature = featureExtractionService.extractFeatures(
            1L, 1L, 7L, studyMessage
        );
        
        assertNotNull(studyFeature.getTopicList());
        assertTrue(studyFeature.getTopicList().contains("学习"));
    }
    
    @Test
    public void testCodeBlockDetection() {
        // 测试代码块检测
        String codeMessage = "这是示例代码：```java\npublic class Test {}\n```";
        ConversationFeature codeFeature = featureExtractionService.extractFeatures(
            1L, 1L, 8L, codeMessage
        );
        
        assertTrue(codeFeature.getCodeBlockCount() > 0);
        // assertTrue(codeFeature.hasCode()); // 方法不存在，暂时注释
    }
    
    @Test
    public void testEmojiDetection() {
        // 测试表情符号检测
        String emojiMessage = "太棒了！:) 很开心 ^_^";
        ConversationFeature emojiFeature = featureExtractionService.extractFeatures(
            1L, 1L, 9L, emojiMessage
        );
        
        assertTrue(emojiFeature.getEmojiCount() > 0);
        assertTrue(emojiFeature.getExclamationCount() > 0);
        assertTrue(emojiFeature.isEmotionallyExpressive());
    }
    
    @Test
    public void testVocabularyRichness() {
        // 测试词汇丰富度
        String richMessage = "编程语言包括Java、Python、JavaScript等多种选择。";
        ConversationFeature richFeature = featureExtractionService.extractFeatures(
            1L, 1L, 10L, richMessage
        );
        
        assertNotNull(richFeature.getVocabularyRichness());
        assertTrue(richFeature.getVocabularyRichness().compareTo(BigDecimal.ZERO) > 0);
        
        // 重复词汇的消息应该有较低的丰富度
        String repetitiveMessage = "好好好好好好好好好好";
        ConversationFeature repetitiveFeature = featureExtractionService.extractFeatures(
            1L, 1L, 11L, repetitiveMessage
        );
        
        assertNotNull(repetitiveFeature.getVocabularyRichness());
        assertTrue(repetitiveFeature.getVocabularyRichness().compareTo(richFeature.getVocabularyRichness()) < 0,
            "重复消息应该有较低的词汇丰富度");
    }
    
    @Test
    public void testEmptyMessage() {
        // 测试空消息处理
        String emptyMessage = "";
        ConversationFeature emptyFeature = featureExtractionService.extractFeatures(
            1L, 1L, 12L, emptyMessage
        );
        
        assertNotNull(emptyFeature);
        assertEquals(0, emptyFeature.getMessageLength());
    }
    
    @Test
    public void testMixedLanguage() {
        // 测试中英文混合
        String mixedMessage = "我在学习Spring Boot框架，it's very useful!";
        ConversationFeature mixedFeature = featureExtractionService.extractFeatures(
            1L, 1L, 13L, mixedMessage
        );
        
        assertNotNull(mixedFeature);
        assertTrue(mixedFeature.getWordCount() > 0);
        assertTrue(mixedFeature.getMessageLength() > 0);
    }
}
