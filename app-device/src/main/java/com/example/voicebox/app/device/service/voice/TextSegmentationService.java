package com.example.voicebox.app.device.service.voice;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本分段服务
 * 将长文本分割成适合流式TTS的小段
 */
@Service
public class TextSegmentationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TextSegmentationService.class);
    
    // 句子结束标点
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？.!?;；]");
    
    // 最大段落长度（字符数）
    private static final int MAX_SEGMENT_LENGTH = 100;
    
    // 最小段落长度（字符数）
    private static final int MIN_SEGMENT_LENGTH = 10;
    
    /**
     * 将文本分段
     * 优先按句子分段，如果句子过长则按长度分段
     * 
     * @param text 原始文本
     * @return 文本段落列表
     */
    public List<String> segmentText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> segments = new ArrayList<>();
        String trimmedText = text.trim();
        
        // 首先尝试按句子分段
        List<String> sentences = splitBySentence(trimmedText);
        
        for (String sentence : sentences) {
            if (sentence.length() <= MAX_SEGMENT_LENGTH) {
                // 句子长度合适，直接添加
                segments.add(sentence);
            } else {
                // 句子过长，按长度分段
                segments.addAll(splitByLength(sentence, MAX_SEGMENT_LENGTH));
            }
        }
        
        logger.debug("分段完成: 原文本长度={}, 段落数={}", trimmedText.length(), segments.size());
        return segments;
    }
    
    /**
     * 按句子分割文本
     */
    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_END.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            int end = matcher.end();
            String sentence = text.substring(lastEnd, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = end;
        }
        
        // 添加最后一段（如果有）
        if (lastEnd < text.length()) {
            String lastSentence = text.substring(lastEnd).trim();
            if (!lastSentence.isEmpty()) {
                sentences.add(lastSentence);
            }
        }
        
        return sentences;
    }
    
    /**
     * 按长度分割文本
     * 尽量在标点符号处分割
     */
    private List<String> splitByLength(String text, int maxLength) {
        List<String> segments = new ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            
            // 如果不是最后一段，尝试在标点符号处分割
            if (end < text.length()) {
                int punctuationPos = findLastPunctuation(text, start, end);
                if (punctuationPos > start + MIN_SEGMENT_LENGTH) {
                    end = punctuationPos + 1;
                }
            }
            
            String segment = text.substring(start, end).trim();
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
            
            start = end;
        }
        
        return segments;
    }
    
    /**
     * 查找指定范围内最后一个标点符号的位置
     */
    private int findLastPunctuation(String text, int start, int end) {
        String punctuation = "，,、；;：:";
        for (int i = end - 1; i >= start; i--) {
            if (punctuation.indexOf(text.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 估算文本的TTS时长（秒）
     * 基于平均语速：中文约4字/秒，英文约2.5词/秒
     */
    public double estimateDuration(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        // 简单估算：假设平均4字/秒
        int charCount = text.length();
        return charCount / 4.0;
    }
}
