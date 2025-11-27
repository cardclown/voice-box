package com.example.voicebox.app.device.chat;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 属性测试：消息存储往返一致性
 * 
 * Feature: voicebox-ui-optimization, Property 20: Message storage round-trip
 * 
 * 验证需求 12.1：当用户发送消息时，系统应将消息内容、时间戳和会话上下文存储到数据库中
 * 
 * 属性：对于任何用户发送的消息，它应该被存储到数据库中，包含内容、时间戳和会话上下文，并且可以在之后检索到
 */
public class MessageStorageRoundTripPropertyTest {

    private ChatSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ChatSessionRepository();
    }

    /**
     * 属性 20：消息存储往返一致性
     * 
     * 对于任何消息（包含内容、角色和会话ID），当它被保存到数据库后再检索出来时，
     * 应该保持所有关键信息的一致性：
     * - 消息内容应该完全相同
     * - 角色（user/assistant）应该相同
     * - 会话ID应该相同
     * - 时间戳应该被正确记录
     */
    @Property(tries = 100)
    void messageStorageRoundTrip(
            @ForAll @NotBlank @StringLength(min = 1, max = 1000) String messageContent,
            @ForAll("validRoles") String role,
            @ForAll @NotBlank @StringLength(min = 1, max = 100) String sessionTitle,
            @ForAll("validModels") String model
    ) {
        // 创建会话
        ChatSession session = repository.createSession(sessionTitle, model, "test-device");
        assertNotNull(session, "会话创建应该成功");
        assertNotNull(session.getId(), "会话ID应该被分配");
        
        // 保存消息
        repository.saveMessage(session.getId(), role, messageContent);
        
        // 检索消息
        List<ChatMessage> messages = repository.listMessages(session.getId());
        
        // 验证：至少应该有一条消息
        assertFalse(messages.isEmpty(), "应该能够检索到保存的消息");
        
        // 查找我们刚保存的消息
        ChatMessage savedMessage = messages.stream()
                .filter(m -> m.getContent().equals(messageContent) && m.getRole().equals(role))
                .findFirst()
                .orElse(null);
        
        // 验证往返一致性
        assertNotNull(savedMessage, "应该能找到保存的消息");
        assertEquals(messageContent, savedMessage.getContent(), 
                "消息内容应该与保存时完全一致");
        assertEquals(role, savedMessage.getRole(), 
                "消息角色应该与保存时一致");
        assertEquals(session.getId(), savedMessage.getSessionId(), 
                "消息应该关联到正确的会话");
        assertNotNull(savedMessage.getCreatedAt(), 
                "消息应该有创建时间戳");
        assertNotNull(savedMessage.getId(), 
                "消息应该有唯一ID");
    }

    /**
     * 属性：多条消息的存储和检索
     * 
     * 对于同一个会话中的多条消息，所有消息都应该能够被正确存储和检索，
     * 并且保持正确的顺序（按创建时间排序）
     */
    @Property(tries = 100)
    void multipleMessagesStorageRoundTrip(
            @ForAll @IntRange(min = 2, max = 10) int messageCount,
            @ForAll @NotBlank @StringLength(min = 1, max = 100) String sessionTitle,
            @ForAll("validModels") String model
    ) {
        // 创建会话
        ChatSession session = repository.createSession(sessionTitle, model, "test-device");
        assertNotNull(session.getId());
        
        // 保存多条消息
        for (int i = 0; i < messageCount; i++) {
            String role = (i % 2 == 0) ? "user" : "assistant";
            String content = "Message " + i + ": " + sessionTitle;
            repository.saveMessage(session.getId(), role, content);
            
            // 添加小延迟以确保时间戳不同
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 检索所有消息
        List<ChatMessage> messages = repository.listMessages(session.getId());
        
        // 验证消息数量
        assertEquals(messageCount, messages.size(), 
                "应该检索到所有保存的消息");
        
        // 验证消息顺序（应该按创建时间升序排列）
        Timestamp previousTimestamp = null;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            
            // 验证内容
            assertTrue(msg.getContent().startsWith("Message " + i), 
                    "消息应该按保存顺序排列");
            
            // 验证时间戳顺序
            assertNotNull(msg.getCreatedAt(), "每条消息都应该有时间戳");
            if (previousTimestamp != null) {
                assertTrue(msg.getCreatedAt().compareTo(previousTimestamp) >= 0,
                        "消息应该按时间顺序排列");
            }
            previousTimestamp = msg.getCreatedAt();
        }
    }

    /**
     * 属性：会话上下文的保存和检索
     * 
     * 对于任何会话，其上下文信息（标题、模型、设备信息）应该能够被正确保存和检索
     */
    @Property(tries = 100)
    void sessionContextRoundTrip(
            @ForAll @NotBlank @StringLength(min = 1, max = 100) String title,
            @ForAll("validModels") String model,
            @ForAll @NotBlank @StringLength(min = 1, max = 200) String deviceInfo
    ) {
        // 创建会话
        ChatSession session = repository.createSession(title, model, deviceInfo);
        assertNotNull(session.getId());
        
        // 检索会话
        ChatSession retrieved = repository.findById(session.getId());
        
        // 验证会话上下文
        assertNotNull(retrieved, "应该能够检索到会话");
        assertEquals(title, retrieved.getTitle(), 
                "会话标题应该与保存时一致");
        assertEquals(model, retrieved.getModel(), 
                "会话模型应该与保存时一致");
        assertEquals(deviceInfo, retrieved.getDeviceInfo(), 
                "设备信息应该与保存时一致");
        assertNotNull(retrieved.getCreatedAt(), 
                "会话应该有创建时间");
        assertNotNull(retrieved.getUpdatedAt(), 
                "会话应该有更新时间");
    }

    /**
     * 属性：空消息内容的边界情况
     * 
     * 虽然我们使用 @NotBlank 约束，但测试系统应该能够处理各种边界情况
     */
    @Property(tries = 50)
    void messageWithWhitespaceHandling(
            @ForAll @StringLength(min = 1, max = 100) String content,
            @ForAll("validRoles") String role,
            @ForAll("validModels") String model
    ) {
        // 创建会话
        ChatSession session = repository.createSession("Test Session", model, "test-device");
        assertNotNull(session.getId());
        
        // 保存消息（即使包含空白字符）
        repository.saveMessage(session.getId(), role, content);
        
        // 检索消息
        List<ChatMessage> messages = repository.listMessages(session.getId());
        
        // 验证消息被保存
        assertFalse(messages.isEmpty(), "消息应该被保存");
        ChatMessage savedMessage = messages.get(messages.size() - 1);
        assertEquals(content, savedMessage.getContent(), 
                "消息内容应该完全保留，包括空白字符");
    }

    // ========== 生成器 ==========

    @Provide
    Arbitrary<String> validRoles() {
        return Arbitraries.of("user", "assistant", "system");
    }

    @Provide
    Arbitrary<String> validModels() {
        return Arbitraries.of(
                "doubao-pro-32k",
                "doubao-lite-32k",
                "gpt-3.5-turbo",
                "gpt-4",
                "claude-2"
        );
    }
}
