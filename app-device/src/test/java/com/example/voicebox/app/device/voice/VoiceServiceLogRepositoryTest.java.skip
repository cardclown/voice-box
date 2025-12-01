package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.domain.VoiceServiceLog;
import com.example.voicebox.app.device.repository.VoiceServiceLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VoiceServiceLog Repository 单元测试
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@DataJpaTest
public class VoiceServiceLogRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private VoiceServiceLogRepository repository;
    
    @Test
    public void testSaveAndFindById() {
        // 创建测试数据
        VoiceServiceLog log = createTestServiceLog();
        
        // 保存
        VoiceServiceLog saved = repository.save(log);
        entityManager.flush();
        
        // 验证
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getServiceType()).isEqualTo("STT");
        assertThat(saved.getProvider()).isEqualTo("aliyun");
        assertThat(saved.getStatus()).isEqualTo("success");
    }
    
    @Test
    public void testFindByServiceType() {
        // 创建不同类型的日志
        VoiceServiceLog sttLog = createTestServiceLog();
        sttLog.setServiceType("STT");
        repository.save(sttLog);
        
        VoiceServiceLog ttsLog = createTestServiceLog();
        ttsLog.setServiceType("TTS");
        repository.save(ttsLog);
        
        entityManager.flush();
        
        // 查询STT日志
        List<VoiceServiceLog> sttLogs = repository.findByServiceTypeOrderByCreatedAtDesc("STT");
        
        // 验证
        assertThat(sttLogs).hasSize(1);
        assertThat(sttLogs.get(0).getServiceType()).isEqualTo("STT");
    }
    
    @Test
    public void testCountByServiceTypeAndProvider() {
        // 创建测试数据
        VoiceServiceLog log1 = createTestServiceLog();
        log1.setServiceType("STT");
        log1.setProvider("aliyun");
        repository.save(log1);
        
        VoiceServiceLog log2 = createTestServiceLog();
        log2.setServiceType("STT");
        log2.setProvider("aliyun");
        repository.save(log2);
        
        VoiceServiceLog log3 = createTestServiceLog();
        log3.setServiceType("STT");
        log3.setProvider("tencent");
        repository.save(log3);
        
        entityManager.flush();
        
        // 统计
        Long count = repository.countByServiceTypeAndProvider("STT", "aliyun");
        
        // 验证
        assertThat(count).isEqualTo(2L);
    }
    
    @Test
    public void testCountSuccessByServiceType() {
        // 创建成功和失败的日志
        VoiceServiceLog successLog = createTestServiceLog();
        successLog.setStatus("success");
        repository.save(successLog);
        
        VoiceServiceLog failedLog = createTestServiceLog();
        failedLog.setStatus("failed");
        repository.save(failedLog);
        
        entityManager.flush();
        
        // 统计成功次数
        Long successCount = repository.countSuccessByServiceType("STT");
        
        // 验证
        assertThat(successCount).isEqualTo(1L);
    }
    
    @Test
    public void testAvgDurationByServiceTypeAndProvider() {
        // 创建测试数据
        VoiceServiceLog log1 = createTestServiceLog();
        log1.setDurationMs(1000);
        repository.save(log1);
        
        VoiceServiceLog log2 = createTestServiceLog();
        log2.setDurationMs(2000);
        repository.save(log2);
        
        entityManager.flush();
        
        // 计算平均时长
        Double avgDuration = repository.avgDurationByServiceTypeAndProvider("STT", "aliyun");
        
        // 验证
        assertThat(avgDuration).isEqualTo(1500.0);
    }
    
    @Test
    public void testSumCostByUserId() {
        // 创建测试数据
        VoiceServiceLog log1 = createTestServiceLog();
        log1.setUserId(1L);
        log1.setCost(new BigDecimal("0.01"));
        repository.save(log1);
        
        VoiceServiceLog log2 = createTestServiceLog();
        log2.setUserId(1L);
        log2.setCost(new BigDecimal("0.02"));
        repository.save(log2);
        
        entityManager.flush();
        
        // 计算总成本
        BigDecimal totalCost = repository.sumCostByUserId(1L);
        
        // 验证
        assertThat(totalCost).isEqualByComparingTo(new BigDecimal("0.03"));
    }
    
    @Test
    public void testFindByStatus() {
        // 创建不同状态的日志
        VoiceServiceLog successLog = createTestServiceLog();
        successLog.setStatus("success");
        repository.save(successLog);
        
        VoiceServiceLog failedLog = createTestServiceLog();
        failedLog.setStatus("failed");
        repository.save(failedLog);
        
        entityManager.flush();
        
        // 查询失败日志
        List<VoiceServiceLog> failedLogs = repository.findByStatusOrderByCreatedAtDesc("failed");
        
        // 验证
        assertThat(failedLogs).hasSize(1);
        assertThat(failedLogs.get(0).getStatus()).isEqualTo("failed");
    }
    
    // 辅助方法：创建测试数据
    private VoiceServiceLog createTestServiceLog() {
        VoiceServiceLog log = new VoiceServiceLog();
        log.setUserId(1L);
        log.setServiceType("STT");
        log.setProvider("aliyun");
        log.setRequestId("req-123");
        log.setInputSize(1024L);
        log.setOutputSize(512L);
        log.setDurationMs(1000);
        log.setStatus("success");
        log.setCost(new BigDecimal("0.01"));
        return log;
    }
}
