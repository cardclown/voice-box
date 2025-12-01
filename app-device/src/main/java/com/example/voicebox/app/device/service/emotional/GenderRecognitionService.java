package com.example.voicebox.app.device.service.emotional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenderRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(GenderRecognitionService.class);
    
    public static class GenderAnalysisResult {
        private String gender;
        private double confidence;
        private String reasoning;
        
        public GenderAnalysisResult() {}
        
        public GenderAnalysisResult(String gender, double confidence, String reasoning) {
            this.gender = gender;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }
    
    public GenderAnalysisResult analyzeGender(VoiceFeatureAnalyzer.VoiceFeatures features) {
        logger.info("开始性别识别分析");
        
        String gender = "UNKNOWN";
        double confidence = 0.5;
        String reasoning = "基于语音特征的性别分析";
        
        if (features.getPitchMean() != null && features.getPitchMean() > 0) {
            if (features.getPitchMean() < 165.0) {
                gender = "MALE";
                confidence = 0.7;
            } else {
                gender = "FEMALE";
                confidence = 0.7;
            }
        }
        
        return new GenderAnalysisResult(gender, confidence, reasoning);
    }
}
