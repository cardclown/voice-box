package com.example.voicebox.app.device.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

public class DatabaseLogger {

    private static final String DB_URL = "jdbc:mysql://voice-box-mysql:3306/voicebox_db?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8";
    private static final String USER = "root";
    private static final String PASS = "root";

    private static DatabaseLogger instance;

    private DatabaseLogger() {
        initializeTable();
    }

    public static synchronized DatabaseLogger getInstance() {
        if (instance == null) {
            instance = new DatabaseLogger();
        }
        return instance;
    }

    private void initializeTable() {
        System.out.println("[DatabaseLogger] Initializing database table...");
        // 简单的重试逻辑，等待数据库就绪
        for (int i = 0; i < 10; i++) {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {
                
                String sql = "CREATE TABLE IF NOT EXISTS chat_history (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "created_at DATETIME, " +
                        "operation_type VARCHAR(50), " +
                        "ai_response TEXT, " +
                        "emotion VARCHAR(50)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                
                stmt.executeUpdate(sql);
                System.out.println("[DatabaseLogger] Table 'chat_history' is ready.");
                return;
            } catch (Exception e) {
                System.err.println("[DatabaseLogger] Failed to connect to DB (attempt " + (i + 1) + "/10): " + e.getMessage());
                try {
                    Thread.sleep(2000); // Wait 2s before retry
                } catch (InterruptedException ignored) {}
            }
        }
        System.err.println("[DatabaseLogger] Gave up initializing DB table.");
    }

    public void logConversation(String aiText, String emotion) {
        String sql = "INSERT INTO chat_history (created_at, operation_type, ai_response, emotion) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, "CHAT_TURN"); // 暂时硬编码操作类型
            pstmt.setString(3, aiText);
            pstmt.setString(4, emotion);
            
            pstmt.executeUpdate();
            System.out.println("[DatabaseLogger] Saved chat record to DB.");
            
        } catch (Exception e) {
            System.err.println("[DatabaseLogger] Failed to save record: " + e.getMessage());
        }
    }
}

