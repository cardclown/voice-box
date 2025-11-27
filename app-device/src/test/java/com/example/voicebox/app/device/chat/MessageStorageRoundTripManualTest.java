package com.example.voicebox.app.device.chat;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

/**
 * 手动属性测试：消息存储往返一致性
 * 
 * Feature: voicebox-ui-optimization, Property 20: Message storage round-trip
 * 
 * 验证需求 12.1：当用户发送消息时，系统应将消息内容、时间戳和会话上下文存储到数据库中
 * 
 * 属性：对于任何用户发送的消息，它应该被存储到数据库中，包含内容、时间戳和会话上下文，并且可以在之后检索到
 */
public class MessageStorageRoundTripManualTest {

    private static final String[] ROLES = {"user", "assistant", "system"};
    private static final String[] MODELS = {
            "doubao-pro-32k",
            "doubao-lite-32k",
            "gpt-3.5-turbo",
            "gpt-4",
            "claude-2"
    };
    
    private static final Random random = new Random();
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("消息存储往返属性测试");
        System.out.println("Feature: voicebox-ui-optimization");
        System.out.println("Property 20: Message storage round-trip");
        System.out.println("========================================\n");

        ChatSessionRepository repository = new ChatSessionRepository();

        // 运行 100 次属性测试
        System.out.println("运行属性测试 1: 单条消息存储往返...");
        for (int i = 0; i < 100; i++) {
            testMessageStorageRoundTrip(repository, i + 1);
        }

        System.out.println("\n运行属性测试 2: 多条消息存储往返...");
        for (int i = 0; i < 100; i++) {
            testMultipleMessagesStorageRoundTrip(repository, i + 1);
        }

        System.out.println("\n运行属性测试 3: 会话上下文往返...");
        for (int i = 0; i < 100; i++) {
            testSessionContextRoundTrip(repository, i + 1);
        }

        System.out.println("\n运行属性测试 4: 特殊字符处理...");
        for (int i = 0; i < 50; i++) {
            testSpecialCharacterHandling(repository, i + 1);
        }

        // 输出结果
        System.out.println("\n========================================");
        System.out.println("测试结果汇总");
        System.out.println("========================================");
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        System.out.println("总计: " + (passedTests + failedTests));
        System.out.println("成功率: " + (passedTests * 100.0 / (passedTests + failedTests)) + "%");
        
        if (failedTests > 0) {
            System.exit(1);
        }
    }

    /**
     * 属性 1：单条消息存储往返
     */
    private static void testMessageStorageRoundTrip(ChatSessionRepository repository, int iteration) {
        try {
            // 生成随机数据
            String messageContent = generateRandomString(10, 1000);
            String role = ROLES[random.nextInt(ROLES.length)];
            String sessionTitle = generateRandomString(5, 100);
            String model = MODELS[random.nextInt(MODELS.length)];

            // 创建会话
            ChatSession session = repository.createSession(sessionTitle, model, "test-device-" + iteration);
            assertNotNull("会话创建应该成功", session);
            assertNotNull("会话ID应该被分配", session.getId());

            // 保存消息
            repository.saveMessage(session.getId(), role, messageContent);

            // 检索消息
            List<ChatMessage> messages = repository.listMessages(session.getId());

            // 验证
            assertFalse("应该能够检索到保存的消息", messages.isEmpty());

            // 查找我们刚保存的消息
            ChatMessage savedMessage = null;
            for (ChatMessage m : messages) {
                if (m.getContent().equals(messageContent) && m.getRole().equals(role)) {
                    savedMessage = m;
                    break;
                }
            }

            assertNotNull("应该能找到保存的消息", savedMessage);
            assertEquals("消息内容应该与保存时完全一致", messageContent, savedMessage.getContent());
            assertEquals("消息角色应该与保存时一致", role, savedMessage.getRole());
            assertEquals("消息应该关联到正确的会话", session.getId(), savedMessage.getSessionId());
            assertNotNull("消息应该有创建时间戳", savedMessage.getCreatedAt());
            assertNotNull("消息应该有唯一ID", savedMessage.getId());

            passedTests++;
            if (iteration % 20 == 0) {
                System.out.print(".");
            }
        } catch (AssertionError e) {
            failedTests++;
            System.err.println("\n测试失败 (迭代 " + iteration + "): " + e.getMessage());
        } catch (Exception e) {
            failedTests++;
            System.err.println("\n测试异常 (迭代 " + iteration + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 属性 2：多条消息存储往返
     */
    private static void testMultipleMessagesStorageRoundTrip(ChatSessionRepository repository, int iteration) {
        try {
            int messageCount = 2 + random.nextInt(9); // 2-10 条消息
            String sessionTitle = generateRandomString(5, 100);
            String model = MODELS[random.nextInt(MODELS.length)];

            // 创建会话
            ChatSession session = repository.createSession(sessionTitle, model, "test-device-" + iteration);
            assertNotNull("会话ID应该被分配", session.getId());

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
            assertEquals("应该检索到所有保存的消息", messageCount, messages.size());

            // 验证消息顺序
            Timestamp previousTimestamp = null;
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);

                // 验证内容
                assertTrue("消息应该按保存顺序排列", 
                        msg.getContent().startsWith("Message " + i));

                // 验证时间戳顺序
                assertNotNull("每条消息都应该有时间戳", msg.getCreatedAt());
                if (previousTimestamp != null) {
                    assertTrue("消息应该按时间顺序排列",
                            msg.getCreatedAt().compareTo(previousTimestamp) >= 0);
                }
                previousTimestamp = msg.getCreatedAt();
            }

            passedTests++;
            if (iteration % 20 == 0) {
                System.out.print(".");
            }
        } catch (AssertionError e) {
            failedTests++;
            System.err.println("\n测试失败 (迭代 " + iteration + "): " + e.getMessage());
        } catch (Exception e) {
            failedTests++;
            System.err.println("\n测试异常 (迭代 " + iteration + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 属性 3：会话上下文往返
     */
    private static void testSessionContextRoundTrip(ChatSessionRepository repository, int iteration) {
        try {
            String title = generateRandomString(5, 100);
            String model = MODELS[random.nextInt(MODELS.length)];
            String deviceInfo = generateRandomString(10, 200);

            // 创建会话
            ChatSession session = repository.createSession(title, model, deviceInfo);
            assertNotNull("会话ID应该被分配", session.getId());

            // 检索会话
            ChatSession retrieved = repository.findById(session.getId());

            // 验证会话上下文
            assertNotNull("应该能够检索到会话", retrieved);
            assertEquals("会话标题应该与保存时一致", title, retrieved.getTitle());
            assertEquals("会话模型应该与保存时一致", model, retrieved.getModel());
            assertEquals("设备信息应该与保存时一致", deviceInfo, retrieved.getDeviceInfo());
            assertNotNull("会话应该有创建时间", retrieved.getCreatedAt());
            assertNotNull("会话应该有更新时间", retrieved.getUpdatedAt());

            passedTests++;
            if (iteration % 20 == 0) {
                System.out.print(".");
            }
        } catch (AssertionError e) {
            failedTests++;
            System.err.println("\n测试失败 (迭代 " + iteration + "): " + e.getMessage());
        } catch (Exception e) {
            failedTests++;
            System.err.println("\n测试异常 (迭代 " + iteration + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 属性 4：特殊字符处理
     */
    private static void testSpecialCharacterHandling(ChatSessionRepository repository, int iteration) {
        try {
            // 生成包含特殊字符的内容
            String[] specialContents = {
                    "Hello\nWorld",
                    "Tab\tSeparated",
                    "Quote: \"test\"",
                    "Single: 'test'",
                    "中文测试",
                    "Emoji: 😀🎉",
                    "Special: <>&",
                    "SQL: ' OR '1'='1",
                    "JSON: {\"key\": \"value\"}",
                    "Unicode: \u0041\u0042\u0043"
            };
            
            String content = specialContents[random.nextInt(specialContents.length)];
            String role = ROLES[random.nextInt(ROLES.length)];
            String model = MODELS[random.nextInt(MODELS.length)];

            // 创建会话
            ChatSession session = repository.createSession("Special Test " + iteration, model, "test-device");
            assertNotNull("会话ID应该被分配", session.getId());

            // 保存消息
            repository.saveMessage(session.getId(), role, content);

            // 检索消息
            List<ChatMessage> messages = repository.listMessages(session.getId());

            // 验证
            assertFalse("消息应该被保存", messages.isEmpty());
            ChatMessage savedMessage = messages.get(messages.size() - 1);
            assertEquals("消息内容应该完全保留特殊字符", content, savedMessage.getContent());

            passedTests++;
            if (iteration % 10 == 0) {
                System.out.print(".");
            }
        } catch (AssertionError e) {
            failedTests++;
            System.err.println("\n测试失败 (迭代 " + iteration + "): " + e.getMessage());
        } catch (Exception e) {
            failedTests++;
            System.err.println("\n测试异常 (迭代 " + iteration + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 辅助方法 ==========

    private static String generateRandomString(int minLength, int maxLength) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString().trim();
    }

    private static void assertNotNull(String message, Object obj) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}
