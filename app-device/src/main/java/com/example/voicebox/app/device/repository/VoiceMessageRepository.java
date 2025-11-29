package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.VoiceMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 语音消息Repository
 */
@Repository
public interface VoiceMessageRepository extends JpaRepository<VoiceMessage, Long> {

    /**
     * 根据文件ID查找
     */
    Optional<VoiceMessage> findByFileId(String fileId);

    /**
     * 根据用户ID查找所有语音消息
     */
    List<VoiceMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据会话ID查找所有语音消息
     */
    List<VoiceMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    /**
     * 查找指定时间之前创建的语音消息
     */
    List<VoiceMessage> findByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计用户的语音消息数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户的语音输入数量
     */
    long countByUserIdAndIsInput(Long userId, Boolean isInput);
}
