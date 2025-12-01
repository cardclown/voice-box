package com.example.voicebox.app.device.service.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 语音存储服务
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@Service
public class VoiceStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceStorageService.class);
    
    @Value("${voice.storage.base-path:/data/voicebox/audio}")
    private String basePath;
    
    @Value("${voice.storage.max-file-size:10485760}")
    private long maxFileSize; // 10MB
    
    /**
     * 保存音频文件
     */
    public String saveAudioFile(MultipartFile file, Long userId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 生成文件ID
        String fileId = generateFileId();
        
        // 构建存储路径
        Path storagePath = getStoragePath(userId, fileId, getFileExtension(file));
        
        // 确保目录存在
        Files.createDirectories(storagePath.getParent());
        
        // 保存文件
        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("音频文件已保存: fileId={}, path={}", fileId, storagePath);
        
        return fileId;
    }
    
    /**
     * 保存音频数据
     */
    public String saveAudioData(byte[] audioData, Long userId) throws IOException {
        return saveAudioData(audioData, userId, "mp3");
    }
    
    /**
     * 保存音频数据（指定格式）
     */
    public String saveAudioData(byte[] audioData, Long userId, String format) throws IOException {
        // 生成文件ID
        String fileId = generateFileId();
        
        // 构建存储路径
        Path storagePath = getStoragePath(userId, fileId, format);
        
        // 确保目录存在
        Files.createDirectories(storagePath.getParent());
        
        // 保存文件
        Files.write(storagePath, audioData);
        
        logger.info("音频数据已保存: fileId={}, size={}, format={}", fileId, audioData.length, format);
        
        return fileId;
    }
    
    /**
     * 加载音频文件
     */
    public Resource loadAudioFile(String fileId) throws IOException {
        // 查找文件（需要遍历用户目录）
        Path filePath = findFile(fileId);
        
        if (filePath == null || !Files.exists(filePath)) {
            throw new IOException("文件不存在: " + fileId);
        }
        
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("文件不可读: " + fileId);
        }
        
        return resource;
    }
    
    /**
     * 加载音频数据
     */
    public byte[] loadAudioData(String fileId) throws IOException {
        Path filePath = findFile(fileId);
        
        if (filePath == null || !Files.exists(filePath)) {
            throw new IOException("文件不存在: " + fileId);
        }
        
        return Files.readAllBytes(filePath);
    }
    
    /**
     * 删除音频文件
     */
    public void deleteAudioFile(String fileId) throws IOException {
        Path filePath = findFile(fileId);
        
        if (filePath != null && Files.exists(filePath)) {
            Files.delete(filePath);
            logger.info("音频文件已删除: fileId={}", fileId);
        }
    }
    
    /**
     * 清理过期文件（超过90天）
     */
    public int cleanupOldFiles() throws IOException {
        final java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        
        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            return 0;
        }
        
        // 遍历所有文件
        Files.walk(baseDir)
            .filter(Files::isRegularFile)
            .filter(path -> {
                try {
                    LocalDateTime fileTime = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(path).toInstant(),
                        java.time.ZoneId.systemDefault()
                    );
                    return fileTime.isBefore(cutoffDate);
                } catch (IOException e) {
                    return false;
                }
            })
            .forEach(path -> {
                try {
                    Files.delete(path);
                    deletedCount.incrementAndGet();
                } catch (IOException e) {
                    logger.error("删除文件失败: {}", path, e);
                }
            });
        
        int count = deletedCount.get();
        logger.info("清理完成，删除了{}个过期文件", count);
        return count;
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String fileId) {
        try {
            Path filePath = findFile(fileId);
            return filePath != null && Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取文件路径
     */
    public String getFilePath(String fileId) {
        try {
            Path filePath = findFile(fileId);
            return filePath != null ? filePath.toString() : null;
        } catch (Exception e) {
            logger.error("获取文件路径失败: fileId={}", fileId, e);
            return null;
        }
    }
    
    // 私有辅助方法
    
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IOException("文件大小超过限制: " + maxFileSize);
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IOException("不支持的文件格式: " + contentType);
        }
    }
    
    private String generateFileId() {
        return UUID.randomUUID().toString();
    }
    
    private Path getStoragePath(Long userId, String fileId, String extension) {
        return Paths.get(basePath, userId.toString(), fileId + "." + extension);
    }
    
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return "mp3"; // 默认扩展名
    }
    
    private Path findFile(String fileId) throws IOException {
        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            return null;
        }
        
        // 简单实现：遍历查找（生产环境应该使用数据库索引）
        return Files.walk(baseDir)
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().startsWith(fileId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 保存音频段（用于流式播放）
     * 不需要userId，使用临时存储
     */
    public String saveAudioSegment(byte[] audioData, String segmentId) throws IOException {
        // 构建临时存储路径
        Path storagePath = Paths.get(basePath, "temp", segmentId + ".mp3");
        
        // 确保目录存在
        Files.createDirectories(storagePath.getParent());
        
        // 保存文件
        Files.write(storagePath, audioData);
        
        logger.debug("音频段已保存: segmentId={}, size={}", segmentId, audioData.length);
        
        return segmentId;
    }
}
