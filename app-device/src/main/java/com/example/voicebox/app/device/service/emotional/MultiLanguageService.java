package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * 多语言支持服务
 * 负责管理语言配置、模型切换和音色选择
 */
@Service
public class MultiLanguageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiLanguageService.class);
    
    // 支持的语言列表
    private static final Set<String> SUPPORTED_LANGUAGES;
    
    // 默认语言
    private static final String DEFAULT_LANGUAGE = "zh-CN";
    
    // 语言对应的模型配置
    private static final Map<String, String> LANGUAGE_MODELS;
    
    // 语言对应的音色配置
    private static final Map<String, Map<String, String>> LANGUAGE_VOICES;
    
    static {
        // 初始化支持的语言
        Set<String> languages = new HashSet<>();
        languages.add("zh-CN");
        languages.add("en-US");
        SUPPORTED_LANGUAGES = Collections.unmodifiableSet(languages);
        
        // 初始化语言模型配置
        Map<String, String> models = new HashMap<>();
        models.put("zh-CN", "chinese-emotion-model-v1");
        models.put("en-US", "english-emotion-model-v1");
        LANGUAGE_MODELS = Collections.unmodifiableMap(models);
        
        // 初始化语言音色配置
        Map<String, String> zhVoices = new HashMap<>();
        zhVoices.put("male", "zh_male_voice_1");
        zhVoices.put("female", "zh_female_voice_1");
        
        Map<String, String> enVoices = new HashMap<>();
        enVoices.put("male", "en_male_voice_1");
        enVoices.put("female", "en_female_voice_1");
        
        Map<String, Map<String, String>> voices = new HashMap<>();
        voices.put("zh-CN", Collections.unmodifiableMap(zhVoices));
        voices.put("en-US", Collections.unmodifiableMap(enVoices));
        LANGUAGE_VOICES = Collections.unmodifiableMap(voices);
    }
    
    /**
     * 验证语言是否支持
     */
    public boolean isLanguageSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(language);
    }
    
    /**
     * 获取支持的语言列表
     */
    public Set<String> getSupportedLanguages() {
        return new HashSet<>(SUPPORTED_LANGUAGES);
    }
    
    /**
     * 获取默认语言
     */
    public String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * 根据语言获取对应的模型
     */
    public String getModelForLanguage(String language) {
        if (!isLanguageSupported(language)) {
            logger.warn("不支持的语言: {}, 使用默认语言", language);
            language = DEFAULT_LANGUAGE;
        }
        return LANGUAGE_MODELS.get(language);
    }
    
    /**
     * 根据语言和性别获取对应的音色
     */
    public String getVoiceForLanguage(String language, String gender) {
        if (!isLanguageSupported(language)) {
            logger.warn("不支持的语言: {}, 使用默认语言", language);
            language = DEFAULT_LANGUAGE;
        }
        
        Map<String, String> voices = LANGUAGE_VOICES.get(language);
        if (voices == null) {
            logger.error("语言 {} 没有配置音色", language);
            return null;
        }
        
        String voice = voices.get(gender.toLowerCase());
        if (voice == null) {
            logger.warn("语言 {} 没有 {} 性别的音色，使用默认", language, gender);
            voice = voices.get("female"); // 默认使用女声
        }
        
        return voice;
    }
    
    /**
     * 获取语言的显示名称
     */
    public String getLanguageDisplayName(String language) {
        switch (language) {
            case "zh-CN":
                return "简体中文";
            case "en-US":
                return "English";
            default:
                return language;
        }
    }
    
    /**
     * 获取所有语言的配置信息
     */
    public Map<String, Object> getLanguageConfig(String language) {
        if (!isLanguageSupported(language)) {
            language = DEFAULT_LANGUAGE;
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("language", language);
        config.put("displayName", getLanguageDisplayName(language));
        config.put("model", getModelForLanguage(language));
        config.put("voices", LANGUAGE_VOICES.get(language));
        
        return config;
    }
    
    /**
     * 获取所有支持的语言配置
     */
    public List<Map<String, Object>> getAllLanguageConfigs() {
        List<Map<String, Object>> configs = new ArrayList<>();
        for (String lang : SUPPORTED_LANGUAGES) {
            configs.add(getLanguageConfig(lang));
        }
        return configs;
    }
}
