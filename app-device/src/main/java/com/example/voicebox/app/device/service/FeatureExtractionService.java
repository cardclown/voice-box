package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.ConversationFeature;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 特征提取服务
 * 从用户消息中提取各种语言学和语义特征
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Service
public class FeatureExtractionService {
    
    // 正面情感词
    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "好", "棒", "喜欢", "开心", "满意", "优秀", "完美", "赞", "不错", "很好",
        "太好了", "厉害", "牛", "强", "爱", "感谢", "谢谢", "高兴", "快乐", "幸福"
    ));
    
    // 负面情感词
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
        "不好", "差", "讨厌", "难过", "失望", "糟糕", "烂", "垃圾", "坏", "糟",
        "生气", "愤怒", "恼火", "郁闷", "烦", "讨厌", "恨", "痛苦", "悲伤"
    ));
    
    // 主题关键词映射
    private static final Map<String, Set<String>> TOPIC_KEYWORDS = new HashMap<>();
    static {
        TOPIC_KEYWORDS.put("技术", new HashSet<>(Arrays.asList(
            "编程", "代码", "开发", "算法", "数据库", "Java", "Python", "前端", "后端",
            "API", "框架", "库", "工具", "调试", "测试", "部署", "服务器", "云"
        )));
        TOPIC_KEYWORDS.put("学习", new HashSet<>(Arrays.asList(
            "学习", "课程", "教程", "考试", "作业", "练习", "复习", "笔记", "知识",
            "理解", "掌握", "学会", "教", "学", "研究", "探索"
        )));
        TOPIC_KEYWORDS.put("生活", new HashSet<>(Arrays.asList(
            "美食", "旅游", "健康", "运动", "睡觉", "吃饭", "购物", "家", "朋友",
            "家人", "工作", "休息", "娱乐", "放松"
        )));
        TOPIC_KEYWORDS.put("娱乐", new HashSet<>(Arrays.asList(
            "电影", "音乐", "游戏", "小说", "动漫", "综艺", "视频", "直播",
            "玩", "看", "听", "唱", "跳"
        )));
    }
    
    /**
     * 从消息中提取完整特征
     */
    public ConversationFeature extractFeatures(Long userId, Long sessionId, Long messageId, String message) {
        ConversationFeature feature = new ConversationFeature();
        feature.setUserId(userId);
        feature.setSessionId(sessionId);
        feature.setMessageId(messageId);
        
        // 基础统计特征
        feature.setMessageLength(message.length());
        
        // 分词和词数统计
        List<String> words = segmentWords(message);
        feature.setWordCount(words.size());
        
        // 句子数统计
        int sentenceCount = countSentences(message);
        feature.setSentenceCount(sentenceCount);
        
        // 平均词长
        if (!words.isEmpty()) {
            double avgLength = words.stream()
                .mapToInt(String::length)
                .average()
                .orElse(0.0);
            feature.setAvgWordLength(BigDecimal.valueOf(avgLength).setScale(2, RoundingMode.HALF_UP));
        }
        
        // 词汇丰富度（不重复词数/总词数）
        if (!words.isEmpty()) {
            double richness = (double) new HashSet<>(words).size() / words.size();
            feature.setVocabularyRichness(BigDecimal.valueOf(richness).setScale(3, RoundingMode.HALF_UP));
        }
        
        // 主题提取
        List<String> topics = extractTopics(message, words);
        feature.setTopicList(topics);
        
        // 情感分析
        double sentiment = analyzeSentiment(message, words);
        feature.setSentimentScore(BigDecimal.valueOf(sentiment).setScale(3, RoundingMode.HALF_UP));
        
        // 意图识别
        String intent = detectIntent(message);
        feature.setIntent(intent);
        
        // 关键词提取
        List<String> keywords = extractKeywords(words, 5);
        feature.setKeywordList(keywords);
        
        // 对话模式特征
        feature.setQuestionCount(countQuestions(message));
        feature.setExclamationCount(countExclamations(message));
        feature.setEmojiCount(countEmojis(message));
        feature.setCodeBlockCount(countCodeBlocks(message));
        
        return feature;
    }
    
    /**
     * 简单分词（基于空格和标点）
     */
    private List<String> segmentWords(String text) {
        List<String> words = new ArrayList<>();
        
        // 移除标点符号，按空格分词
        String cleaned = text.replaceAll("[\\p{Punct}\\s]+", " ");
        
        // 中文按字符分词，英文按空格分词
        for (String token : cleaned.split("\\s+")) {
            if (token.isEmpty()) continue;
            
            // 如果是中文，按字符分
            if (token.matches(".*[\\u4e00-\\u9fa5].*")) {
                for (char c : token.toCharArray()) {
                    if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                        words.add(Character.toString(c));
                    }
                }
            } else {
                // 英文单词
                words.add(token.toLowerCase());
            }
        }
        
        return words;
    }
    
    /**
     * 统计句子数
     */
    private int countSentences(String text) {
        String[] sentences = text.split("[。！？.!?]+");
        return Math.max(1, sentences.length);
    }
    
    /**
     * 提取主题
     */
    private List<String> extractTopics(String text, List<String> words) {
        Set<String> topics = new HashSet<>();
        
        for (Map.Entry<String, Set<String>> entry : TOPIC_KEYWORDS.entrySet()) {
            String topic = entry.getKey();
            Set<String> keywords = entry.getValue();
            
            // 检查文本中是否包含该主题的关键词
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    topics.add(topic);
                    break;
                }
            }
        }
        
        return new ArrayList<>(topics);
    }
    
    /**
     * 情感分析
     */
    private double analyzeSentiment(String text, List<String> words) {
        int positiveCount = 0;
        int negativeCount = 0;
        
        // 统计正负面词
        for (String word : words) {
            if (POSITIVE_WORDS.contains(word)) {
                positiveCount++;
            } else if (NEGATIVE_WORDS.contains(word)) {
                negativeCount++;
            }
        }
        
        // 检查完整短语
        for (String positive : POSITIVE_WORDS) {
            if (text.contains(positive)) {
                positiveCount++;
            }
        }
        for (String negative : NEGATIVE_WORDS) {
            if (text.contains(negative)) {
                negativeCount++;
            }
        }
        
        int total = positiveCount + negativeCount;
        if (total == 0) return 0.0;
        
        // 返回-1到1之间的分数
        return (double) (positiveCount - negativeCount) / total;
    }
    
    /**
     * 意图识别
     */
    private String detectIntent(String text) {
        // 问题
        if (text.contains("？") || text.contains("?") || 
            text.matches(".*[什么|怎么|如何|为什么|哪里|谁|多少].*")) {
            return "question";
        }
        
        // 请求
        if (text.matches(".*[请|帮|能不能|可以|麻烦].*")) {
            return "request";
        }
        
        // 陈述
        return "statement";
    }
    
    /**
     * 提取关键词（简单的词频统计）
     */
    private List<String> extractKeywords(List<String> words, int topN) {
        // 统计词频
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            if (word.length() > 1) { // 过滤单字
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        // 按频率排序
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordFreq.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // 取前N个
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            keywords.add(sorted.get(i).getKey());
        }
        
        return keywords;
    }
    
    /**
     * 统计问号数量
     */
    private int countQuestions(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '？' || c == '?') {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 统计感叹号数量
     */
    private int countExclamations(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '！' || c == '!') {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 统计表情符号数量
     */
    private int countEmojis(String text) {
        int count = 0;
        // 简单的表情符号检测
        Pattern emojiPattern = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
        Matcher matcher = emojiPattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        
        // 文本表情
        String[] textEmojis = {":)", ":(", ":D", ";)", "^^", "T_T", ">_<", "^_^"};
        for (String emoji : textEmojis) {
            if (text.contains(emoji)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 统计代码块数量
     */
    private int countCodeBlocks(String text) {
        int count = 0;
        
        // Markdown代码块
        Pattern codeBlockPattern = Pattern.compile("```[\\s\\S]*?```");
        Matcher matcher = codeBlockPattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        
        // 行内代码
        Pattern inlineCodePattern = Pattern.compile("`[^`]+`");
        Matcher inlineMatcher = inlineCodePattern.matcher(text);
        while (inlineMatcher.find()) {
            count++;
        }
        
        return count;
    }
}
