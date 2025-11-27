package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.db.DatabaseConfig;
import com.example.voicebox.app.device.domain.UserTag;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for UserTag entity.
 */
@Repository
public class UserTagRepository {

    private static final String DB_URL = DatabaseConfig.jdbcUrl();
    private static final String USER = DatabaseConfig.user();
    private static final String PASS = DatabaseConfig.password();

    public UserTagRepository() {
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user_tags (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "category VARCHAR(50) NOT NULL," +
                    "tag_name VARCHAR(100) NOT NULL," +
                    "confidence DECIMAL(5,4) DEFAULT 0.5000," +
                    "source VARCHAR(50)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_category (category)," +
                    "INDEX idx_tag_name (tag_name)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            System.out.println("[UserTagRepository] User tags table initialized successfully.");
        } catch (Exception e) {
            System.err.println("[UserTagRepository] Failed to initialize user_tags table: " + e.getMessage());
        }
    }

    public UserTag create(UserTag tag) {
        String sql = "INSERT INTO user_tags (user_id, category, tag_name, confidence, source) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, tag.getUserId());
            stmt.setString(2, tag.getCategory());
            stmt.setString(3, tag.getTagName());
            stmt.setBigDecimal(4, tag.getConfidence() != null ? tag.getConfidence() : new BigDecimal("0.5000"));
            stmt.setString(5, tag.getSource() != null ? tag.getSource() : "auto");
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    tag.setId(rs.getLong(1));
                    return findById(tag.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user tag", e);
        }
        throw new RuntimeException("Unable to create user tag");
    }

    public UserTag findById(Long id) {
        if (id == null) {
            return null;
        }
        
        String sql = "SELECT * FROM user_tags WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUserTag(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user tag by id: " + id, e);
        }
        return null;
    }

    public List<UserTag> findByUserId(Long userId) {
        List<UserTag> tags = new ArrayList<>();
        String sql = "SELECT * FROM user_tags WHERE user_id = ? ORDER BY confidence DESC, created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapUserTag(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find tags for user: " + userId, e);
        }
        return tags;
    }

    public List<UserTag> findByUserIdAndCategory(Long userId, String category) {
        List<UserTag> tags = new ArrayList<>();
        String sql = "SELECT * FROM user_tags WHERE user_id = ? AND category = ? ORDER BY confidence DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setString(2, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapUserTag(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find tags for user and category", e);
        }
        return tags;
    }

    public UserTag findByUserIdAndCategoryAndTagName(Long userId, String category, String tagName) {
        String sql = "SELECT * FROM user_tags WHERE user_id = ? AND category = ? AND tag_name = ? LIMIT 1";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setString(2, category);
            stmt.setString(3, tagName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUserTag(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find tag by user, category and name", e);
        }
        return null;
    }

    public UserTag update(UserTag tag) {
        String sql = "UPDATE user_tags SET category = ?, tag_name = ?, confidence = ?, source = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tag.getCategory());
            stmt.setString(2, tag.getTagName());
            stmt.setBigDecimal(3, tag.getConfidence());
            stmt.setString(4, tag.getSource());
            stmt.setLong(5, tag.getId());
            
            stmt.executeUpdate();
            return findById(tag.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user tag", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM user_tags WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user tag", e);
        }
    }

    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM user_tags WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user tags", e);
        }
    }

    private UserTag mapUserTag(ResultSet rs) throws SQLException {
        UserTag tag = new UserTag();
        tag.setId(rs.getLong("id"));
        tag.setUserId(rs.getLong("user_id"));
        tag.setCategory(rs.getString("category"));
        tag.setTagName(rs.getString("tag_name"));
        tag.setConfidence(rs.getBigDecimal("confidence"));
        tag.setSource(rs.getString("source"));
        tag.setCreatedAt(rs.getTimestamp("created_at"));
        tag.setUpdatedAt(rs.getTimestamp("updated_at"));
        return tag;
    }
}
