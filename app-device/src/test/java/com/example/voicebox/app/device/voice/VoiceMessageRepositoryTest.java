package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.domain.VoiceMessage;
import com.example.voicebox.app.device.repository.VoiceMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VoiceMessage Repository 单元测试
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@DataJpaTest
public class VoiceMessageRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private VoiceMessageRepository repository;
    
    @Test
    public void testSaveAndFindById() {
        // 创建测试数据
        VoiceMessage message = createTestVoiceMessage();
        
        // 保存
        VoiceMessage saved = repository.save(message);
        entityManager.flush();
        
        // 验证
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getFileId()).isEqualTo("test-file-id");
        
        // 查询
        Optional<VoiceMessage> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRecognizedText()).isEqualTo("测试语音内容");
    }
    
    @Test
    public void testFindByFileId() {
        // 创建并保存测试数据
        VoiceMessage message = createTestVoiceMessage();
        repository.save(message);
        entityManager.flush();
        
        // 根据fileId查询
        Optional<VoiceMessage> found = repository.findByFileId("test-file-id");
        
        // 验证
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }
    
    @Test
    public void testFindByUserId() {
        // 创建多条测试数据
        VoiceMessage message1 = createTestVoiceMessage();
        message1.setFileId("file-1");
        repository.save(message1);
        
        VoiceMessage message2 = createTestVoiceMessage();
        message2.setFileId("file-2");
        repository.save(message2);
        
        entityManager.flush();
        
        // 查询用户的所有语音消息
        List<VoiceMessage> messages = repository.findByUserIdOrderByCreatedAtDesc(1L);
        
        // 验证
        assertThat(messages).hasSize(2);
    }
    
    @Test
    public void testCountByUserId() {
        // 创建测试数据
        VoiceMessage message1 = createTestVoiceMessage();
        message1.setFileId("file-1");
        repository.save(message1);
        
        VoiceMessage message2 = createTestVoiceMessage();
        message2.setFileId("file-2");
        repository.save(message2);
        
        entityManager.flush();
        
        // 统计
        Long count = repository.countByUserId(1L);
        
        // 验证
        assertThat(count).isEqualTo(2L);
    }
    
    @Test
    public void testSumDurationByUserId() {
        // 创建测试数据
        VoiceMessage message1 = createTestVoiceMessage();
        message1.setFileId("file-1");
        message1.setDuration(10);
        repository.save(message1);
        
        VoiceMessage message2 = createTestVoiceMessage();
        message2.setFileId("file-2");
        message2.setDuration(20);
        repository.save(message2);
        
        entityManager.flush();
        
        // 统计总时长
        Long totalDuration = repository.sumDurationByUserId(1L);
        
        // 验证
        assertThat(totalDuration).isEqualTo(30L);
    }
    
    @Test
    public void testFindBySessionId() {
        // 创建测试数据
        VoiceMessage message = createTestVoiceMessage();
        repository.save(message);
        entityManager.flush();
        
        // 查询
        List<VoiceMessage> messages = repository.findBySessionIdOrderByCreatedAtDesc(100L);
        
        // 验证
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSessionId()).isEqualTo(100L);
    }
    
    @Test
    public void testFindByUserIdAndIsInput() {
        // 创建输入和输出消息
        VoiceMessage inputMessage = createTestVoiceMessage();
        inputMessage.setFileId("input-1");
        inputMessage.setIsInput(true);
        repository.save(inputMessage);
        
        VoiceMessage outputMessage = createTestVoiceMessage();
        outputMessage.setFileId("output-1");
        outputMessage.setIsInput(false);
        repository.save(outputMessage);
        
        entityManager.flush();
        
        // 查询输入消息
        List<VoiceMessage> inputMessages = repository.findByUserIdAndIsInputOrderByCreatedAtDesc(1L, true);
        
        // 验证
        assertThat(inputMessages).hasSize(1);
        assertThat(inputMessages.get(0).getIsInput()).isTrue();
    }
    
    // 辅助方法：创建测试数据
    private VoiceMessage createTestVoiceMessage() {
        VoiceMessage message = new VoiceMessage();
        message.setUserId(1L);
        message.setSessionId(100L);
        message.setMessageId(1000L);
        message.setFileId("test-file-id");
        message.setFilePath("/data/voice/test.mp3");
        message.setFileSize(1024L);
        message.setDuration(10);
        message.setFormat("mp3");
        message.setSampleRate(16000);
        message.setRecognizedText("测试语音内容");
        message.setConfidence(new BigDecimal("0.95"));
        message.setLanguage("zh-CN");
        message.setIsInput(true);
        return message;
    }
}
