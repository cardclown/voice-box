package com.example.voicebox.app.device.controller;

import com.example.voicebox.app.device.service.emotional.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 情感语音分析API控制器
 * 
 * 提供语音情感分析、标签生成、用户画像、语音合成等功能的REST API
 */
@RestController
@RequestMapping("/api/emotional-voice")
@CrossOrigin(origins = "*")
public class EmotionalVoiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionalVoiceController.class);
    
    @Autowired
    private VoiceFeatureAnalyzer voiceFeatureAnalyzer;
    
    @Autowired
    private PersonalityRecognitionService personalityService;
    
    @Autowired
    private EmotionRecognitionService emotionService;
    
    @Autowired
    private ToneStyleAnalyzer toneAnalyzer;
    
    @Autowired
    private GenderRecognitionService genderService;
    
    @Autowired
    private EmotionalTagGenerator tagGenerator;
    
    @Autowired
    private UserEmotionalProfileService profileService;
    
    @Autowired
    private EmotionalVoiceSynthesisService synthesisService;
    
    @Autowired
    private MultiLanguageService multiLanguageService;
    
    @Autowired
    private UserLanguagePreferenceService languagePreferenceService;
    
    /**
     * 分析语音文件的情感特征
     * 需求: 1.1, 2.1, 3.1, 4.1, 5.1
     * 
     * @param audioFile 语音文件
     * @param text 对应的文本内容（可选）
     * @param userId 用户ID（可选，用于更新用户画像）
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeVoice(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "userId", required = false) String userId) {
        
        logger.info("开始分析语音文件: {}, 文本: {}, 用户: {}", 
            audioFile.getOriginalFilename(), text, userId);
        
        try {
            // 验证输入
            if (audioFile.isEmpty()) {
                return createErrorResponse("语音文件不能为空");
            }
            
            if (audioFile.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return createErrorResponse("语音文件过大，最大支持10MB");
            }
            
            Map<String, Object> result = new HashMap<>();
            long startTime = System.currentTimeMillis();
            
            // 1. 提取语音特征
            logger.debug("开始提取语音特征");
            VoiceFeatureAnalyzer.VoiceFeatures features = 
                voiceFeatureAnalyzer.extractFeatures(audioFile.getBytes());
            result.put("voiceFeatures", features);
            
            // 2. 性格识别
            logger.debug("开始性格识别");
            PersonalityRecognitionService.PersonalityTraits personality = 
                personalityService.analyzePersonality(features, text);
            result.put("personality", personality);
            
            // 3. 情绪识别
            logger.debug("开始情绪识别");
            EmotionRecognitionService.EmotionResult emotion = 
                emotionService.recognizeEmotion(features, text);
            result.put("emotion", emotion);
            
            // 4. 语气分析
            logger.debug("开始语气分析");
            ToneStyleAnalyzer.ToneAnalysisResult tone = 
                toneAnalyzer.analyzeTone(features, text);
            result.put("tone", tone);
            
            // 5. 性别识别
            logger.debug("开始性别识别");
            GenderRecognitionService.GenderAnalysisResult gender = 
                genderService.analyzeGender(features);
            result.put("gender", gender);
            
            // 6. 生成情感标签
            logger.debug("开始生成情感标签");
            EmotionalTagGenerator.TagGenerationResult tags = 
                tagGenerator.generateTags(features, text);
            result.put("tags", tags);
            
            // 7. 如果提供了用户ID，更新用户画像
            if (userId != null && !userId.isEmpty()) {
                logger.debug("更新用户画像: {}", userId);
                UserEmotionalProfileService.UserEmotionalProfile profile = 
                    profileService.buildProfile(userId, features, text);
                result.put("profileUpdated", true);
                result.put("profileSummary", createProfileSummary(profile));
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("status", "success");
            result.put("message", "语音情感分析完成");
            result.put("processingTime", processingTime);
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("语音情感分析完成，耗时: {}ms", processingTime);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("输入参数错误: {}", e.getMessage());
            return createErrorResponse("输入参数错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("语音情感分析失败: {}", e.getMessage(), e);
            return createErrorResponse("语音情感分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建用户画像摘要
     */
    private Map<String, Object> createProfileSummary(
            UserEmotionalProfileService.UserEmotionalProfile profile) {
        Map<String, Object> summary = new HashMap<>();
        if (profile != null) {
            summary.put("userId", profile.getUserId());
            summary.put("totalInteractions", profile.getTotalInteractions());
            summary.put("lastUpdated", profile.getLastUpdated());
            if (profile.getPersonalityProfile() != null) {
                summary.put("dominantPersonality", 
                    profile.getPersonalityProfile().getDominantTrait());
            }
            if (profile.getEmotionProfile() != null) {
                summary.put("dominantEmotion", 
                    profile.getEmotionProfile().getDominantEmotion());
            }
        }
        return summary;
    }
    
    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "error");
        errorResult.put("message", message);
        errorResult.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.badRequest().body(errorResult);
    }
    
    /**
     * 创建简单错误响应（用于多语言API）
     */
    private Map<String, Object> createSimpleErrorMap(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        return response;
    }
    
    /**
     * 构建或更新用户情感画像
     * 
     * @param userId 用户ID
     * @param audioFile 语音文件
     * @param text 对应的文本内容（可选）
     * @return 用户画像
     */
    @PostMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> buildUserProfile(
            @PathVariable String userId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "text", required = false) String text) {
        
        logger.info("开始构建用户{}的情感画像", userId);
        
        try {
            // 提取语音特征
            VoiceFeatureAnalyzer.VoiceFeatures features = 
                voiceFeatureAnalyzer.extractFeatures(audioFile.getBytes());
            
            // 构建用户画像
            UserEmotionalProfileService.UserEmotionalProfile profile = 
                profileService.buildProfile(userId, features, text);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "用户情感画像构建完成");
            result.put("profile", profile);
            
            logger.info("用户{}的情感画像构建完成", userId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("构建用户画像失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "构建用户画像失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 获取用户情感画像
     * 
     * @param userId 用户ID
     * @return 用户画像
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId) {
        
        logger.info("获取用户{}的情感画像", userId);
        
        try {
            UserEmotionalProfileService.UserEmotionalProfile profile = 
                profileService.getProfile(userId);
            
            Map<String, Object> result = new HashMap<>();
            if (profile != null) {
                result.put("status", "success");
                result.put("message", "获取用户画像成功");
                result.put("profile", profile);
            } else {
                result.put("status", "not_found");
                result.put("message", "用户画像不存在");
                result.put("profile", null);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取用户画像失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "获取用户画像失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 清除用户历史数据和画像
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> clearUserProfile(@PathVariable String userId) {
        
        logger.info("清除用户{}的情感画像", userId);
        
        try {
            profileService.clearHistory(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "用户画像已清除");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("清除用户画像失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "清除用户画像失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 仅分析文本的情感特征（无语音）
     * 
     * @param text 文本内容
     * @return 分析结果
     */
    @PostMapping("/analyze-text")
    public ResponseEntity<Map<String, Object>> analyzeText(@RequestParam("text") String text) {
        
        logger.info("开始分析文本情感: {}", text);
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 创建默认的语音特征（用于文本分析）
            VoiceFeatureAnalyzer.VoiceFeatures defaultFeatures = 
                new VoiceFeatureAnalyzer.VoiceFeatures();
            
            // 性格识别（基于文本）
            PersonalityRecognitionService.PersonalityTraits personality = 
                personalityService.analyzePersonality(defaultFeatures, text);
            result.put("personality", personality);
            
            // 情绪识别（基于文本）
            EmotionRecognitionService.EmotionResult emotion = 
                emotionService.recognizeEmotion(defaultFeatures, text);
            result.put("emotion", emotion);
            
            // 语气分析（基于文本）
            ToneStyleAnalyzer.ToneAnalysisResult tone = 
                toneAnalyzer.analyzeTone(defaultFeatures, text);
            result.put("tone", tone);
            
            // 生成情感标签
            EmotionalTagGenerator.TagGenerationResult tags = 
                tagGenerator.generateTags(defaultFeatures, text);
            result.put("tags", tags);
            
            result.put("status", "success");
            result.put("message", "文本情感分析完成");
            
            logger.info("文本情感分析完成");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("文本情感分析失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "文本情感分析失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 生成情感化语音
     * 
     * @param userId 用户ID
     * @param text 要合成的文本
     * @param targetEmotion 目标情绪（可选）
     * @return 语音合成结果
     */
    @PostMapping("/synthesize/{userId}")
    public ResponseEntity<Map<String, Object>> synthesizeEmotionalVoice(
            @PathVariable String userId,
            @RequestParam("text") String text,
            @RequestParam(value = "emotion", required = false) String targetEmotion) {
        
        logger.info("开始为用户{}生成情感化语音，文本: {}, 目标情绪: {}", userId, text, targetEmotion);
        
        try {
            EmotionRecognitionService.EmotionType emotion = null;
            if (targetEmotion != null && !targetEmotion.isEmpty()) {
                try {
                    emotion = EmotionRecognitionService.EmotionType.valueOf(targetEmotion.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的情绪类型: {}", targetEmotion);
                }
            }
            
            EmotionalVoiceSynthesisService.VoiceSynthesisResult result = 
                synthesisService.synthesizeEmotionalVoice(userId, text, emotion);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "情感化语音生成完成");
            response.put("audioFormat", result.getAudioFormat());
            response.put("duration", result.getDuration());
            response.put("params", result.getParams());
            response.put("metadata", result.getMetadata());
            
            // 注意：实际项目中，音频数据通常通过单独的接口返回或存储到文件系统
            response.put("audioDataSize", result.getAudioData().length);
            response.put("audioDataBase64", Base64.getEncoder().encodeToString(result.getAudioData()));
            
            logger.info("用户{}的情感化语音生成完成", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("情感化语音生成失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "情感化语音生成失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 获取推荐的语音合成参数
     * 
     * @param userId 用户ID
     * @return 推荐的合成参数
     */
    @GetMapping("/synthesize/params/{userId}")
    public ResponseEntity<Map<String, Object>> getRecommendedParams(@PathVariable String userId) {
        
        logger.info("获取用户{}的推荐合成参数", userId);
        
        try {
            EmotionalVoiceSynthesisService.VoiceSynthesisParams params = 
                synthesisService.getRecommendedParams(userId, null);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "获取推荐参数成功");
            result.put("params", params);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取推荐参数失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "获取推荐参数失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 批量生成情感化语音
     * 
     * @param userId 用户ID
     * @param request 批量请求（包含文本列表和目标情绪）
     * @return 批量合成结果
     */
    @PostMapping("/synthesize/batch/{userId}")
    public ResponseEntity<Map<String, Object>> batchSynthesize(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        
        logger.info("开始为用户{}批量生成情感化语音", userId);
        
        try {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            String emotionStr = (String) request.get("emotion");
            
            if (texts == null || texts.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "error");
                errorResult.put("message", "文本列表不能为空");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            EmotionRecognitionService.EmotionType emotion = null;
            if (emotionStr != null && !emotionStr.isEmpty()) {
                try {
                    emotion = EmotionRecognitionService.EmotionType.valueOf(emotionStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的情绪类型: {}", emotionStr);
                }
            }
            
            List<EmotionalVoiceSynthesisService.VoiceSynthesisResult> results = 
                synthesisService.batchSynthesize(userId, texts, emotion);
            
            // 转换结果为简化格式
            List<Map<String, Object>> simplifiedResults = new ArrayList<>();
            for (EmotionalVoiceSynthesisService.VoiceSynthesisResult result : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("text", result.getText());
                item.put("duration", result.getDuration());
                item.put("audioFormat", result.getAudioFormat());
                item.put("audioDataSize", result.getAudioData().length);
                item.put("params", result.getParams());
                simplifiedResults.add(item);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "批量语音生成完成");
            response.put("totalCount", texts.size());
            response.put("successCount", results.size());
            response.put("results", simplifiedResults);
            
            logger.info("用户{}的批量语音生成完成，成功: {}/{}", userId, results.size(), texts.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量语音生成失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "批量语音生成失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "healthy");
        result.put("message", "情感语音分析服务运行正常");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    // ==================== 多语言支持 API ====================
    
    /**
     * 获取支持的语言列表
     * 验收标准 19.1: 系统应支持中文和英文
     */
    @GetMapping("/languages")
    public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
        try {
            List<Map<String, Object>> languages = multiLanguageService.getAllLanguageConfigs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("languages", languages);
            response.put("defaultLanguage", multiLanguageService.getDefaultLanguage());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取支持的语言列表失败", e);
            return ResponseEntity.internalServerError()
                .body(createSimpleErrorMap(e.getMessage()));
        }
    }
    
    /**
     * 获取用户的语言偏好
     */
    @GetMapping("/language/{userId}")
    public ResponseEntity<Map<String, Object>> getUserLanguage(@PathVariable Long userId) {
        try {
            Map<String, Object> config = languagePreferenceService.getUserLanguageConfig(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("config", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户语言偏好失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(createSimpleErrorMap(e.getMessage()));
        }
    }
    
    /**
     * 设置用户的语言偏好
     * 验收标准 19.4: 显示界面应使用用户选择的语言
     * 验收标准 19.5: 切换语言时保持用户画像数据
     */
    @PostMapping("/language/{userId}")
    public ResponseEntity<Map<String, Object>> setUserLanguage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String language = request.get("language");
            
            if (language == null || language.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createSimpleErrorMap("语言参数不能为空"));
            }
            
            // 切换语言（保持画像数据）
            languagePreferenceService.switchUserLanguage(userId, language);
            
            // 获取更新后的配置
            Map<String, Object> config = languagePreferenceService.getUserLanguageConfig(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "语言偏好已更新");
            response.put("config", config);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("设置用户语言失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(createSimpleErrorMap(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置用户语言失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(createSimpleErrorMap(e.getMessage()));
        }
    }
    
    /**
     * 获取指定语言的模型配置
     * 验收标准 19.2: 根据语言选择相应的模型
     */
    @GetMapping("/language/{language}/model")
    public ResponseEntity<Map<String, Object>> getLanguageModel(@PathVariable String language) {
        try {
            if (!multiLanguageService.isLanguageSupported(language)) {
                return ResponseEntity.badRequest()
                    .body(createSimpleErrorMap("不支持的语言: " + language));
            }
            
            String model = multiLanguageService.getModelForLanguage(language);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("language", language);
            response.put("model", model);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取语言模型失败: language={}", language, e);
            return ResponseEntity.internalServerError()
                .body(createSimpleErrorMap(e.getMessage()));
        }
    }
    
    /**
     * 获取指定语言的音色配置
     * 验收标准 19.3: 使用对应语言的音色
     */
    @GetMapping("/language/{language}/voices")
    public ResponseEntity<Map<String, Object>> getLanguageVoices(@PathVariable String language) {
        try {
            if (!multiLanguageService.isLanguageSupported(language)) {
                return ResponseEntity.badRequest()
                    .body(createSimpleErrorMap("不支持的语言: " + language));
            }
            
            Map<String, Object> config = multiLanguageService.getLanguageConfig(language);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("language", language);
            response.put("voices", config.get("voices"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取语言音色失败: language={}", language, e);
            return ResponseEntity.internalServerError()
                .body(createSimpleErrorMap(e.getMessage()));
        }
    }
}
