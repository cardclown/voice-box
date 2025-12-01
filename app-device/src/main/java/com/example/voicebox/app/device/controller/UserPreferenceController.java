package com.example.voicebox.app.device.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户偏好设置控制器
 * 
 * 提供用户偏好的保存和查询功能
 */
@RestController
@RequestMapping("/api/user/preferences")
@CrossOrigin(origins = "*")
public class UserPreferenceController {

    // 简单的内存存储（生产环境应该使用数据库）
    private final Map<String, Map<String, String>> userPreferences = new ConcurrentHashMap<>();

    /**
     * 保存用户偏好
     * 
     * @param request 包含userId, preferenceKey, preferenceValue的请求
     * @return 保存结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> savePreference(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getLongValue(request.get("userId"));
            String preferenceKey = (String) request.get("preferenceKey");
            String preferenceValue = (String) request.get("preferenceValue");

            if (userId == null || preferenceKey == null || preferenceValue == null) {
                response.put("success", false);
                response.put("errorMessage", "缺少必需参数");
                return ResponseEntity.badRequest().body(response);
            }

            // 保存偏好
            String userKey = "user_" + userId;
            userPreferences.computeIfAbsent(userKey, k -> new ConcurrentHashMap<>())
                .put(preferenceKey, preferenceValue);

            response.put("success", true);
            response.put("message", "偏好设置已保存");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("errorMessage", "保存偏好设置失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取用户偏好
     * 
     * @param userId 用户ID
     * @param preferenceKey 偏好键
     * @return 偏好值
     */
    @GetMapping("/{userId}/{preferenceKey}")
    public ResponseEntity<Map<String, Object>> getPreference(
            @PathVariable Long userId,
            @PathVariable String preferenceKey) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            String userKey = "user_" + userId;
            Map<String, String> preferences = userPreferences.get(userKey);

            if (preferences != null && preferences.containsKey(preferenceKey)) {
                response.put("success", true);
                response.put("preferenceKey", preferenceKey);
                response.put("preferenceValue", preferences.get(preferenceKey));
            } else {
                response.put("success", false);
                response.put("errorMessage", "偏好设置不存在");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("errorMessage", "获取偏好设置失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取用户所有偏好
     * 
     * @param userId 用户ID
     * @return 所有偏好设置
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getAllPreferences(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String userKey = "user_" + userId;
            Map<String, String> preferences = userPreferences.get(userKey);

            response.put("success", true);
            response.put("preferences", preferences != null ? preferences : new HashMap<>());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("errorMessage", "获取偏好设置失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 删除用户偏好
     * 
     * @param userId 用户ID
     * @param preferenceKey 偏好键
     * @return 删除结果
     */
    @DeleteMapping("/{userId}/{preferenceKey}")
    public ResponseEntity<Map<String, Object>> deletePreference(
            @PathVariable Long userId,
            @PathVariable String preferenceKey) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            String userKey = "user_" + userId;
            Map<String, String> preferences = userPreferences.get(userKey);

            if (preferences != null) {
                preferences.remove(preferenceKey);
                response.put("success", true);
                response.put("message", "偏好设置已删除");
            } else {
                response.put("success", false);
                response.put("errorMessage", "用户偏好不存在");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("errorMessage", "删除偏好设置失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 辅助方法：安全地获取Long值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
