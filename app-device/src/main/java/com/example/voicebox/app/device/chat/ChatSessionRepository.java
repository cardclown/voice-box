package com.example.voicebox.app.device.chat;

import com.example.voicebox.app.device.db.DatabaseConfig;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ChatSessionRepository {

    private static final String DB_URL = DatabaseConfig.jdbcUrl();
    private static final String USER = DatabaseConfig.user();
    private static final String PASS = DatabaseConfig.password();

    private volatile boolean dbAvailable = true;

    private final Map<Long, ChatSession> memorySessions = new ConcurrentHashMap<>();
    private final Map<Long, List<ChatMessage>> memoryMessages = new ConcurrentHashMap<>();
    private final AtomicLong memorySessionId = new AtomicLong(1);
    private final AtomicLong memoryMessageId = new AtomicLong(1);

    public ChatSessionRepository() {
        initializeTables();
    }

    private void initializeTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chat_session (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(200)," +
                    "model VARCHAR(64)," +
                    "device_info TEXT," +
                    "created_at DATETIME," +
                    "updated_at DATETIME," +
                    "user_id BIGINT," +
                    "tags JSON," +
                    "personalization_context JSON," +
                    "INDEX idx_user_id (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chat_message (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "session_id BIGINT NOT NULL," +
                    "role VARCHAR(16)," +
                    "content TEXT," +
                    "created_at DATETIME," +
                    "sentiment VARCHAR(20)," +
                    "extracted_topics JSON," +
                    "response_time_ms INT," +
                    "CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE," +
                    "INDEX idx_sentiment (sentiment)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            // Add new columns if they don't exist (migration support)
            try {
                stmt.executeUpdate("ALTER TABLE chat_session ADD COLUMN user_id BIGINT");
                stmt.executeUpdate("ALTER TABLE chat_session ADD INDEX idx_user_id (user_id)");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE chat_session ADD COLUMN tags JSON");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE chat_session ADD COLUMN personalization_context JSON");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE chat_message ADD COLUMN sentiment VARCHAR(20)");
                stmt.executeUpdate("ALTER TABLE chat_message ADD INDEX idx_sentiment (sentiment)");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE chat_message ADD COLUMN extracted_topics JSON");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                stmt.executeUpdate("ALTER TABLE chat_message ADD COLUMN response_time_ms INT");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            
            System.out.println("[ChatSessionRepository] Database tables initialized successfully (url=" + DB_URL + ").");
        } catch (Exception e) {
            dbAvailable = false;
            System.err.println("[ChatSessionRepository] Failed to initialize tables (fallback to in-memory store, url=" + DB_URL + "): " + e.getMessage());
        }
    }

    public ChatSession createSession(String title, String model, String deviceInfo) {
        if (!dbAvailable) {
            return createSessionInMemory(title, model, deviceInfo);
        }
        String sql = "INSERT INTO chat_session (title, model, device_info, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, title);
            stmt.setString(2, model);
            stmt.setString(3, deviceInfo);
            stmt.setTimestamp(4, now);
            stmt.setTimestamp(5, now);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return findById(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat session", e);
        }
        throw new RuntimeException("Unable to create session");
    }

    public ChatSession findById(Long id) {
        if (id == null) {
            return null;
        }
        if (!dbAvailable) {
            return memorySessions.get(id);
        }
        String sql = "SELECT * FROM chat_session WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapSession(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load session " + id, e);
        }
        return null;
    }

    public List<ChatSession> listSessions() {
        if (!dbAvailable) {
            return new ArrayList<>(memorySessions.values());
        }
        List<ChatSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM chat_session ORDER BY updated_at DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list sessions", e);
        }
        return sessions;
    }

    public List<ChatMessage> listMessages(Long sessionId) {
        if (!dbAvailable) {
            return new ArrayList<>(memoryMessages.getOrDefault(sessionId, new ArrayList<>()));
        }
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM chat_message WHERE session_id = ? ORDER BY created_at ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list messages", e);
        }
        return messages;
    }

    public void saveMessage(Long sessionId, String role, String content) {
        if (!dbAvailable) {
            saveMessageInMemory(sessionId, role, content);
            return;
        }
        String sql = "INSERT INTO chat_message (session_id, role, content, created_at) VALUES (?, ?, ?, ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            stmt.setString(2, role);
            stmt.setString(3, content);
            stmt.setTimestamp(4, now);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message", e);
        }
        touchSession(sessionId, null, null);
    }

    public ChatSession ensureSession(Long sessionId, String fallbackTitle, String model, String deviceInfo) {
        if (sessionId != null) {
            ChatSession existing = findById(sessionId);
            if (existing == null) {
                throw new IllegalArgumentException("Session not found: " + sessionId);
            }
            touchSession(sessionId, model, deviceInfo);
            return existing;
        }
        String title = buildTitle(fallbackTitle);
        return createSession(title, model, deviceInfo);
    }

    public void updateTitleIfEmpty(Long sessionId, String candidateTitle) {
        if (sessionId == null) {
            return;
        }
        if (!dbAvailable) {
            ChatSession session = memorySessions.get(sessionId);
            if (session != null && (session.getTitle() == null || session.getTitle().isEmpty() || "New Chat".equals(session.getTitle()))) {
                session.setTitle(buildTitle(candidateTitle));
            }
            return;
        }
        String sql = "UPDATE chat_session SET title = ? WHERE id = ? AND (title IS NULL OR title = '' OR title = 'New Chat')";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, buildTitle(candidateTitle));
            stmt.setLong(2, sessionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[ChatSessionRepository] Failed to update title: " + e.getMessage());
        }
    }

    private void touchSession(Long sessionId, String model, String deviceInfo) {
        if (!dbAvailable) {
            ChatSession session = memorySessions.get(sessionId);
            if (session != null) {
                session.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                if (model != null) {
                    session.setModel(model);
                }
                if (deviceInfo != null) {
                    session.setDeviceInfo(deviceInfo);
                }
            }
            return;
        }
        String sql = "UPDATE chat_session SET updated_at = ?, model = COALESCE(?, model), device_info = COALESCE(?, device_info) WHERE id = ?";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, model);
            stmt.setString(3, deviceInfo);
            stmt.setLong(4, sessionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[ChatSessionRepository] Failed to touch session: " + e.getMessage());
        }
    }

    private ChatSession mapSession(ResultSet rs) throws SQLException {
        ChatSession session = new ChatSession();
        session.setId(rs.getLong("id"));
        session.setTitle(rs.getString("title"));
        session.setModel(rs.getString("model"));
        session.setDeviceInfo(rs.getString("device_info"));
        session.setCreatedAt(rs.getTimestamp("created_at"));
        session.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        // Handle new fields with null safety
        try {
            long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                session.setUserId(userId);
            }
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        try {
            session.setTags(rs.getString("tags"));
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        try {
            session.setPersonalizationContext(rs.getString("personalization_context"));
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        
        return session;
    }

    private ChatMessage mapMessage(ResultSet rs) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setId(rs.getLong("id"));
        message.setSessionId(rs.getLong("session_id"));
        message.setRole(rs.getString("role"));
        message.setContent(rs.getString("content"));
        message.setCreatedAt(rs.getTimestamp("created_at"));
        
        // Handle new fields with null safety
        try {
            message.setSentiment(rs.getString("sentiment"));
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        try {
            message.setExtractedTopics(rs.getString("extracted_topics"));
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        try {
            int responseTime = rs.getInt("response_time_ms");
            if (!rs.wasNull()) {
                message.setResponseTimeMs(responseTime);
            }
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        
        return message;
    }

    private String buildTitle(String text) {
        if (text == null || text.isEmpty()) {
            return "New Chat";
        }
        String trimmed = text.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) + "..." : trimmed;
    }

    private ChatSession createSessionInMemory(String title, String model, String deviceInfo) {
        long id = memorySessionId.getAndIncrement();
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setTitle(title);
        session.setModel(model);
        session.setDeviceInfo(deviceInfo);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        memorySessions.put(id, session);
        memoryMessages.put(id, new ArrayList<>());
        return session;
    }

    private void saveMessageInMemory(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(memoryMessageId.getAndIncrement());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        memoryMessages.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
        touchSession(sessionId, null, null);
    }
}

