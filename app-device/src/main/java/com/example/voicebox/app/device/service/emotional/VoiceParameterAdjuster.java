package com.example.voicebox.app.device.service.emotional;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 语音参数调整服务
 * 根据用户语速和音调特征调整合成参数
 */
@Service
public class VoiceParameterAdjuster {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceParameterAdjuster.class);
    
    // 参数范围限制
    private static final double MIN_SPEED = 0.5;
    private static final double MAX_SPEED = 2.0;
    private static final double MIN_PITCH = 0.5;
    private static final double MAX_PITCH = 2.0;
    private static final double MIN_VOLUME = 0.0;
    private static final double MAX_VOLUME = 1.0;
    
    // 自然度阈值
    private static final double NATURAL_SPEED_MIN = 0.8;
    private static final double NATURAL_SPEED_MAX = 1.3;
    private static final double NATURAL_PITCH_MIN = 0.9;
    private static final double NATURAL_PITCH_MAX = 1.2;
    
    /**
     * 根据用户语速匹配合成语速
     * 需求: 10.1
     */
    public double matchSpeed(VoiceFeatureAnalyzer.VoiceFeatures userFeatures) {
        logger.info("匹配用户语速");
        
        if (userFeatures == null || userFeatures.getSpeechRate() == null) {
            return 1.0; // 默认语速
        }
        
        double userSpeed = userFeatures.getSpeechRate();
        
        // 将用户语速映射到合成语速范围
        // 假设用户语速范围: 2.0-6.0 字/秒
        // 映射到合成语速: 0.8-1.3
        double normalizedSpeed;
        if (userSpeed < 3.0) {
            normalizedSpeed = 0.8; // 慢速
        } else if (userSpeed > 5.0) {
            normalizedSpeed = 1.3; // 快速
        } else {
            // 线性映射
            normalizedSpeed = 0.8 + (userSpeed - 3.0) * (1.3 - 0.8) / (5.0 - 3.0);
        }
        
        return limitSpeed(normalizedSpeed);
    }
    
    /**
     * 根据用户音调匹配合成音调
     * 需求: 10.2
     */
    public double matchPitch(VoiceFeatureAnalyzer.VoiceFeatures userFeatures, String gender) {
        logger.info("匹配用户音调: gender={}", gender);
        
        if (userFeatures == null || userFeatures.getPitchMean() == null) {
            return 1.0; // 默认音调
        }
        
        double userPitch = userFeatures.getPitchMean();
        
        // 根据性别和音高匹配音调
        // 男性平均音高: 100-150 Hz
        // 女性平均音高: 180-250 Hz
        double normalizedPitch;
        
        if ("MALE".equalsIgnoreCase(gender)) {
            if (userPitch < 110) {
                normalizedPitch = 0.9; // 低音
            } else if (userPitch > 140) {
                normalizedPitch = 1.1; // 高音
            } else {
                normalizedPitch = 0.9 + (userPitch - 110) * (1.1 - 0.9) / (140 - 110);
            }
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            if (userPitch < 190) {
                normalizedPitch = 0.9; // 低音
            } else if (userPitch > 240) {
                normalizedPitch = 1.2; // 高音
            } else {
                normalizedPitch = 0.9 + (userPitch - 190) * (1.2 - 0.9) / (240 - 190);
            }
        } else {
            normalizedPitch = 1.0; // 中性
        }
        
        return limitPitch(normalizedPitch);
    }
    
    /**
     * 调整参数以保持自然度
     * 需求: 10.3, 10.5
     */
    public AdjustedParameters adjustForNaturalness(double speed, double pitch, double volume) {
        logger.info("调整参数以保持自然度: speed={}, pitch={}, volume={}", speed, pitch, volume);
        
        AdjustedParameters adjusted = new AdjustedParameters();
        
        // 调整语速
        if (speed < NATURAL_SPEED_MIN) {
            adjusted.setSpeed(NATURAL_SPEED_MIN);
            adjusted.addWarning("语速过慢，已调整到最小自然值");
        } else if (speed > NATURAL_SPEED_MAX) {
            adjusted.setSpeed(NATURAL_SPEED_MAX);
            adjusted.addWarning("语速过快，已调整到最大自然值");
        } else {
            adjusted.setSpeed(speed);
        }
        
        // 调整音调
        if (pitch < NATURAL_PITCH_MIN) {
            adjusted.setPitch(NATURAL_PITCH_MIN);
            adjusted.addWarning("音调过低，已调整到最小自然值");
        } else if (pitch > NATURAL_PITCH_MAX) {
            adjusted.setPitch(NATURAL_PITCH_MAX);
            adjusted.addWarning("音调过高，已调整到最大自然值");
        } else {
            adjusted.setPitch(pitch);
        }
        
        // 调整音量
        adjusted.setVolume(limitVolume(volume));
        
        // 检查自然度
        adjusted.setNatural(isNatural(adjusted.getSpeed(), adjusted.getPitch()));
        
        return adjusted;
    }
    
    /**
     * 验证参数是否在有效范围内
     * 需求: 10.4
     */
    public boolean validateParameters(double speed, double pitch, double volume) {
        boolean valid = true;
        
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            logger.warn("语速超出范围: {}", speed);
            valid = false;
        }
        
        if (pitch < MIN_PITCH || pitch > MAX_PITCH) {
            logger.warn("音调超出范围: {}", pitch);
            valid = false;
        }
        
        if (volume < MIN_VOLUME || volume > MAX_VOLUME) {
            logger.warn("音量超出范围: {}", volume);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * 检查参数是否自然
     * 需求: 10.5
     */
    public boolean isNatural(double speed, double pitch) {
        return speed >= NATURAL_SPEED_MIN && speed <= NATURAL_SPEED_MAX &&
               pitch >= NATURAL_PITCH_MIN && pitch <= NATURAL_PITCH_MAX;
    }
    
    /**
     * 限制语速在有效范围内
     */
    private double limitSpeed(double speed) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }
    
    /**
     * 限制音调在有效范围内
     */
    private double limitPitch(double pitch) {
        return Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
    }
    
    /**
     * 限制音量在有效范围内
     */
    private double limitVolume(double volume) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }
    
    /**
     * 根据情绪调整参数
     */
    public AdjustedParameters adjustForEmotion(double baseSpeed, double basePitch, 
                                              EmotionRecognitionService.EmotionType emotion,
                                              double intensity) {
        logger.info("根据情绪调整参数: emotion={}, intensity={}", emotion, intensity);
        
        double speed = baseSpeed;
        double pitch = basePitch;
        
        if (emotion != null) {
            switch (emotion) {
                case HAPPY:
                case EXCITED:
                    speed *= (1.0 + 0.1 * intensity);
                    pitch *= (1.0 + 0.1 * intensity);
                    break;
                case SAD:
                    speed *= (1.0 - 0.1 * intensity);
                    pitch *= (1.0 - 0.1 * intensity);
                    break;
                case ANGRY:
                    speed *= (1.0 + 0.15 * intensity);
                    pitch *= (1.0 + 0.15 * intensity);
                    break;
                case CALM:
                    speed *= (1.0 - 0.05 * intensity);
                    break;
                case ANXIOUS:
                    speed *= (1.0 + 0.12 * intensity);
                    pitch *= (1.0 + 0.05 * intensity);
                    break;
            }
        }
        
        return adjustForNaturalness(speed, pitch, 1.0);
    }
    
    /**
     * 调整后的参数类
     */
    public static class AdjustedParameters {
        private double speed;
        private double pitch;
        private double volume;
        private boolean natural;
        private java.util.List<String> warnings;
        
        public AdjustedParameters() {
            this.speed = 1.0;
            this.pitch = 1.0;
            this.volume = 1.0;
            this.natural = true;
            this.warnings = new java.util.ArrayList<>();
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        
        // Getters and Setters
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        
        public double getPitch() { return pitch; }
        public void setPitch(double pitch) { this.pitch = pitch; }
        
        public double getVolume() { return volume; }
        public void setVolume(double volume) { this.volume = volume; }
        
        public boolean isNatural() { return natural; }
        public void setNatural(boolean natural) { this.natural = natural; }
        
        public java.util.List<String> getWarnings() { return warnings; }
        public void setWarnings(java.util.List<String> warnings) { this.warnings = warnings; }
        
        @Override
        public String toString() {
            return String.format("AdjustedParameters{speed=%.2f, pitch=%.2f, volume=%.2f, natural=%s, warnings=%d}",
                speed, pitch, volume, natural, warnings.size());
        }
    }
}
