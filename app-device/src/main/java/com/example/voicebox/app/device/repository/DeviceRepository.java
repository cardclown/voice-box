package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.db.DatabaseConfig;
import com.example.voicebox.app.device.domain.Device;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Device entity for hardware integration.
 */
@Repository
public class DeviceRepository {

    private static final String DB_URL = DatabaseConfig.jdbcUrl();
    private static final String USER = DatabaseConfig.user();
    private static final String PASS = DatabaseConfig.password();

    public DeviceRepository() {
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS devices (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "device_type VARCHAR(50)," +
                    "device_name VARCHAR(100)," +
                    "api_key VARCHAR(255) UNIQUE," +
                    "last_sync_at TIMESTAMP," +
                    "is_active BOOLEAN DEFAULT TRUE," +
                    "device_metadata JSON," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_api_key (api_key)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            System.out.println("[DeviceRepository] Devices table initialized successfully.");
        } catch (Exception e) {
            System.err.println("[DeviceRepository] Failed to initialize devices table: " + e.getMessage());
        }
    }

    public Device create(Device device) {
        String sql = "INSERT INTO devices (user_id, device_type, device_name, api_key, last_sync_at, is_active, device_metadata) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, device.getUserId());
            stmt.setString(2, device.getDeviceType());
            stmt.setString(3, device.getDeviceName());
            
            // Generate API key if not provided
            String apiKey = device.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = generateApiKey();
            }
            stmt.setString(4, apiKey);
            
            stmt.setTimestamp(5, device.getLastSyncAt());
            stmt.setBoolean(6, device.getIsActive() != null ? device.getIsActive() : true);
            stmt.setString(7, device.getDeviceMetadata());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    device.setId(rs.getLong(1));
                    device.setApiKey(apiKey);
                    return findById(device.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create device", e);
        }
        throw new RuntimeException("Unable to create device");
    }

    public Device findById(Long id) {
        if (id == null) {
            return null;
        }
        
        String sql = "SELECT * FROM devices WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDevice(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find device by id: " + id, e);
        }
        return null;
    }

    public Device findByApiKey(String apiKey) {
        if (apiKey == null) {
            return null;
        }
        
        String sql = "SELECT * FROM devices WHERE api_key = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, apiKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDevice(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find device by API key", e);
        }
        return null;
    }

    public List<Device> findByUserId(Long userId) {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    devices.add(mapDevice(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find devices for user: " + userId, e);
        }
        return devices;
    }

    public List<Device> findActiveByUserId(Long userId) {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE user_id = ? AND is_active = TRUE ORDER BY last_sync_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    devices.add(mapDevice(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find active devices", e);
        }
        return devices;
    }

    public Device update(Device device) {
        String sql = "UPDATE devices SET device_type = ?, device_name = ?, last_sync_at = ?, " +
                     "is_active = ?, device_metadata = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, device.getDeviceType());
            stmt.setString(2, device.getDeviceName());
            stmt.setTimestamp(3, device.getLastSyncAt());
            stmt.setBoolean(4, device.getIsActive());
            stmt.setString(5, device.getDeviceMetadata());
            stmt.setLong(6, device.getId());
            
            stmt.executeUpdate();
            return findById(device.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update device", e);
        }
    }

    public void updateLastSync(Long deviceId) {
        String sql = "UPDATE devices SET last_sync_at = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setLong(2, deviceId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DeviceRepository] Failed to update last sync: " + e.getMessage());
        }
    }

    public void deactivate(Long deviceId) {
        String sql = "UPDATE devices SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, deviceId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deactivate device", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM devices WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete device", e);
        }
    }

    private Device mapDevice(ResultSet rs) throws SQLException {
        Device device = new Device();
        device.setId(rs.getLong("id"));
        device.setUserId(rs.getLong("user_id"));
        device.setDeviceType(rs.getString("device_type"));
        device.setDeviceName(rs.getString("device_name"));
        device.setApiKey(rs.getString("api_key"));
        device.setLastSyncAt(rs.getTimestamp("last_sync_at"));
        device.setIsActive(rs.getBoolean("is_active"));
        device.setDeviceMetadata(rs.getString("device_metadata"));
        device.setCreatedAt(rs.getTimestamp("created_at"));
        return device;
    }

    private String generateApiKey() {
        return "vb_" + UUID.randomUUID().toString().replace("-", "");
    }
}
