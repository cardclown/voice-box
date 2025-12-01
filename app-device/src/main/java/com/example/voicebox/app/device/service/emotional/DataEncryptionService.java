package com.example.voicebox.app.device.service.emotional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 数据加密服务
 * 
 * 功能：
 * 1. 音频文件加密
 * 2. 敏感字段加密
 * 3. 加密密钥管理
 * 4. 解密接口
 */
@Service
public class DataEncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataEncryptionService.class);
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    
    // 实际应该从配置文件或密钥管理服务获取
    private static final String SECRET_KEY = "VoiceBoxSecretKey2024!@#$%^";
    
    /**
     * 加密文本数据
     * 
     * @param plainText 明文
     * @return 加密后的Base64字符串
     */
    public String encryptText(String plainText) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("文本加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }
    
    /**
     * 解密文本数据
     * 
     * @param encryptedText 加密的Base64字符串
     * @return 明文
     */
    public String decryptText(String encryptedText) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("文本解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }
    
    /**
     * 加密音频文件
     * 
     * @param audioData 音频数据
     * @return 加密后的音频数据
     */
    public byte[] encryptAudioFile(byte[] audioData) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            return cipher.doFinal(audioData);
        } catch (Exception e) {
            logger.error("音频加密失败", e);
            throw new RuntimeException("音频加密失败", e);
        }
    }
    
    /**
     * 解密音频文件
     * 
     * @param encryptedData 加密的音频数据
     * @return 解密后的音频数据
     */
    public byte[] decryptAudioFile(byte[] encryptedData) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            logger.error("音频解密失败", e);
            throw new RuntimeException("音频解密失败", e);
        }
    }
    
    /**
     * 加密敏感字段
     * 
     * @param fieldValue 字段值
     * @return 加密后的值
     */
    public String encryptSensitiveField(String fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return fieldValue;
        }
        
        return encryptText(fieldValue);
    }
    
    /**
     * 解密敏感字段
     * 
     * @param encryptedValue 加密的值
     * @return 解密后的值
     */
    public String decryptSensitiveField(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return encryptedValue;
        }
        
        return decryptText(encryptedValue);
    }
    
    /**
     * 生成新的加密密钥
     * 
     * @return Base64编码的密钥
     */
    public String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            logger.error("密钥生成失败", e);
            throw new RuntimeException("密钥生成失败", e);
        }
    }
    
    /**
     * 获取密钥
     * 
     * @return SecretKey
     */
    private SecretKey getSecretKey() {
        // 简化实现：使用固定密钥
        // 实际应该从密钥管理服务获取
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        
        // 确保密钥长度为32字节（256位）
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        
        return new SecretKeySpec(key, ALGORITHM);
    }
    
    /**
     * 验证加密完整性
     * 
     * @param originalData 原始数据
     * @param encryptedData 加密数据
     * @return 是否完整
     */
    public boolean verifyEncryptionIntegrity(String originalData, String encryptedData) {
        try {
            String decrypted = decryptText(encryptedData);
            return originalData.equals(decrypted);
        } catch (Exception e) {
            logger.error("完整性验证失败", e);
            return false;
        }
    }
}
