package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.db.DatabaseConfig;
import com.example.voicebox.app.device.domain.User;
import org.springframework.stereotype.Repository;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Repository for User entity with encryption support for sensitive fields.
 */
@Repository
public class UserRepository {

    private static final String DB_URL = DatabaseConfig.jdbcUrl();
    private static final String USER = DatabaseConfig.user();
    private static final String PASS = DatabaseConfig.password();
    
    // Simple encryption key - in production, use proper key management
    private static final String ENCRYPTION_KEY = "VoiceBox16ByteKy"; // 16 bytes for AES-128
    private static final String ALGORITHM = "AES";

    public UserRepository() {
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(100) UNIQUE NOT NULL," +
                    "email VARCHAR(255) UNIQUE," +
                    "password_hash VARCHAR(255)," +
                    "avatar_url VARCHAR(500)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "last_active_at TIMESTAMP," +
                    "preferences JSON," +
                    "INDEX idx_username (username)," +
                    "INDEX idx_email (email)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            System.out.println("[UserRepository] Users table initialized successfully.");
        } catch (Exception e) {
            System.err.println("[UserRepository] Failed to initialize users table: " + e.getMessage());
        }
    }

    public User create(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, avatar_url, last_active_at, preferences) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, encrypt(user.getEmail())); // Encrypt email (PII)
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setTimestamp(5, user.getLastActiveAt());
            stmt.setString(6, user.getPreferences());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                    return findById(user.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user", e);
        }
        throw new RuntimeException("Unable to create user");
    }

    public User findById(Long id) {
        if (id == null) {
            return null;
        }
        
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by id: " + id, e);
        }
        return null;
    }

    public User findByUsername(String username) {
        if (username == null) {
            return null;
        }
        
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by username: " + username, e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list users", e);
        }
        return users;
    }

    public User update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, " +
                     "avatar_url = ?, last_active_at = ?, preferences = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, encrypt(user.getEmail())); // Encrypt email (PII)
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setTimestamp(5, user.getLastActiveAt());
            stmt.setString(6, user.getPreferences());
            stmt.setLong(7, user.getId());
            
            stmt.executeUpdate();
            return findById(user.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public void updateLastActive(Long userId) {
        String sql = "UPDATE users SET last_active_at = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[UserRepository] Failed to update last active: " + e.getMessage());
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(decrypt(rs.getString("email"))); // Decrypt email
        user.setPasswordHash(rs.getString("password_hash"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setLastActiveAt(rs.getTimestamp("last_active_at"));
        user.setPreferences(rs.getString("preferences"));
        return user;
    }

    /**
     * Encrypt sensitive data (PII) using AES.
     */
    private String encrypt(String data) {
        if (data == null) {
            return null;
        }
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("[UserRepository] Encryption failed: " + e.getMessage());
            return data; // Fallback to unencrypted
        }
    }

    /**
     * Decrypt sensitive data (PII) using AES.
     */
    private String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[UserRepository] Decryption failed: " + e.getMessage());
            return encryptedData; // Fallback to encrypted value
        }
    }
}
