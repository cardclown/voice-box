package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.db.DatabaseConfig;
import com.example.voicebox.app.device.domain.Interaction;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Interaction entity for behavior tracking.
 */
@Repository
public class InteractionRepository {

    private static final String DB_URL = DatabaseConfig.jdbcUrl();
    private static final String USER = DatabaseConfig.user();
    private static final String PASS = DatabaseConfig.password();

    public InteractionRepository() {
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS interactions (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "session_id BIGINT," +
                    "interaction_type VARCHAR(50)," +
                    "interaction_data JSON," +
                    "device_info JSON," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_session_id (session_id)," +
                    "INDEX idx_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            System.out.println("[InteractionRepository] Interactions table initialized successfully.");
        } catch (Exception e) {
            System.err.println("[InteractionRepository] Failed to initialize interactions table: " + e.getMessage());
        }
    }

    public Interaction create(Interaction interaction) {
        String sql = "INSERT INTO interactions (user_id, session_id, interaction_type, interaction_data, device_info) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, interaction.getUserId());
            if (interaction.getSessionId() != null) {
                stmt.setLong(2, interaction.getSessionId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, interaction.getInteractionType());
            stmt.setString(4, interaction.getInteractionData());
            stmt.setString(5, interaction.getDeviceInfo());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    interaction.setId(rs.getLong(1));
                    return findById(interaction.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create interaction", e);
        }
        throw new RuntimeException("Unable to create interaction");
    }

    public Interaction findById(Long id) {
        if (id == null) {
            return null;
        }
        
        String sql = "SELECT * FROM interactions WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapInteraction(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find interaction by id: " + id, e);
        }
        return null;
    }

    public List<Interaction> findByUserId(Long userId, int limit) {
        List<Interaction> interactions = new ArrayList<>();
        String sql = "SELECT * FROM interactions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interactions.add(mapInteraction(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find interactions for user: " + userId, e);
        }
        return interactions;
    }

    public List<Interaction> findBySessionId(Long sessionId) {
        List<Interaction> interactions = new ArrayList<>();
        String sql = "SELECT * FROM interactions WHERE session_id = ? ORDER BY created_at ASC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interactions.add(mapInteraction(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find interactions for session: " + sessionId, e);
        }
        return interactions;
    }

    public List<Interaction> findByUserIdAndType(Long userId, String interactionType, int limit) {
        List<Interaction> interactions = new ArrayList<>();
        String sql = "SELECT * FROM interactions WHERE user_id = ? AND interaction_type = ? " +
                     "ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setString(2, interactionType);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interactions.add(mapInteraction(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find interactions by type", e);
        }
        return interactions;
    }

    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM interactions WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to count interactions", e);
        }
        return 0;
    }

    public void deleteOlderThan(Timestamp timestamp) {
        String sql = "DELETE FROM interactions WHERE created_at < ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, timestamp);
            int deleted = stmt.executeUpdate();
            System.out.println("[InteractionRepository] Deleted " + deleted + " old interactions.");
        } catch (Exception e) {
            System.err.println("[InteractionRepository] Failed to delete old interactions: " + e.getMessage());
        }
    }

    private Interaction mapInteraction(ResultSet rs) throws SQLException {
        Interaction interaction = new Interaction();
        interaction.setId(rs.getLong("id"));
        interaction.setUserId(rs.getLong("user_id"));
        
        long sessionId = rs.getLong("session_id");
        if (!rs.wasNull()) {
            interaction.setSessionId(sessionId);
        }
        
        interaction.setInteractionType(rs.getString("interaction_type"));
        interaction.setInteractionData(rs.getString("interaction_data"));
        interaction.setDeviceInfo(rs.getString("device_info"));
        interaction.setCreatedAt(rs.getTimestamp("created_at"));
        return interaction;
    }
}
