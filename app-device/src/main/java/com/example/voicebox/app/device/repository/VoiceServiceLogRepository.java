package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.VoiceServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音服务日志Repository
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
@Repository
public interface VoiceServiceLogRepository extends JpaRepository<VoiceServiceLog, Long> {
    
    /**
     * 根据用户ID查找服务日志
     */
    List<VoiceServiceLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据服务类型查找日志
     */
    List<VoiceServiceLog> findByServiceTypeOrderByCreatedAtDesc(String serviceType);
    
    /**
     * 根据服务提供商查找日志
     */
    List<VoiceServiceLog> findByProviderOrderByCreatedAtDesc(String provider);
    
    /**
     * 根据状态查找日志
     */
    List<VoiceServiceLog> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * 查找指定时间范围内的日志
     */
    List<VoiceServiceLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计服务调用次数
     */
    @Query("SELECT COUNT(v) FROM VoiceServiceLog v WHERE v.serviceType = :serviceType AND v.provider = :provider")
    Long countByServiceTypeAndProvider(@Param("serviceType") String serviceType, @Param("provider") String provider);
    
    /**
     * 统计成功调用次数
     */
    @Query("SELECT COUNT(v) FROM VoiceServiceLog v WHERE v.serviceType = :serviceType AND v.status = 'success'")
    Long countSuccessByServiceType(@Param("serviceType") String serviceType);
    
    /**
     * 统计失败调用次数
     */
    @Query("SELECT COUNT(v) FROM VoiceServiceLog v WHERE v.serviceType = :serviceType AND v.status = 'failed'")
    Long countFailedByServiceType(@Param("serviceType") String serviceType);
    
    /**
     * 计算平均响应时间
     */
    @Query("SELECT AVG(v.durationMs) FROM VoiceServiceLog v WHERE v.serviceType = :serviceType AND v.provider = :provider")
    Double avgDurationByServiceTypeAndProvider(@Param("serviceType") String serviceType, @Param("provider") String provider);
    
    /**
     * 计算总成本
     */
    @Query("SELECT SUM(v.cost) FROM VoiceServiceLog v WHERE v.userId = :userId")
    BigDecimal sumCostByUserId(@Param("userId") Long userId);
    
    /**
     * 删除指定时间之前的日志
     */
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
