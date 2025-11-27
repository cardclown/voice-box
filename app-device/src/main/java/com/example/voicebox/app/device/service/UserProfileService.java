package com.example.voicebox.app.device.service;

import com.example.voicebox.app.device.domain.Interaction;
import com.example.voicebox.app.device.domain.User;
import com.example.voicebox.app.device.domain.UserTag;
import com.example.voicebox.app.device.repository.InteractionRepository;
import com.example.voicebox.app.device.repository.UserRepository;
import com.example.voicebox.app.device.repository.UserTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing user profiles, tags, and analytics.
 */
@Service
public class UserProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    /**
     * Get user profile by ID.
     */
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Create a new user profile.
     */
    public User createUserProfile(User user) {
        return userRepository.create(user);
    }

    /**
     * Update user profile.
     */
    public User updateUserProfile(Long userId, User user) {
        User existing = userRepository.findById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        user.setId(userId);
        return userRepository.update(user);
    }

    /**
     * Delete user profile and all associated data.
     */
    public void deleteUserProfile(Long userId) {
        userRepository.delete(userId);
    }

    /**
     * Get all tags for a user.
     */
    public List<UserTag> getUserTags(Long userId) {
        return userTagRepository.findByUserId(userId);
    }

    /**
     * Get tags by category for a user.
     */
    public List<UserTag> getUserTagsByCategory(Long userId, String category) {
        return userTagRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * Add a tag to a user.
     */
    public UserTag addUserTag(Long userId, UserTag tag) {
        // Verify user exists
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        tag.setUserId(userId);
        if (tag.getConfidence() == null) {
            tag.setConfidence(new BigDecimal("0.5000"));
        }
        if (tag.getSource() == null) {
            tag.setSource("manual");
        }
        
        return userTagRepository.create(tag);
    }

    /**
     * Update a user tag.
     */
    public UserTag updateUserTag(Long tagId, UserTag tag) {
        UserTag existing = userTagRepository.findById(tagId);
        if (existing == null) {
            throw new IllegalArgumentException("Tag not found: " + tagId);
        }
        tag.setId(tagId);
        tag.setUserId(existing.getUserId());
        return userTagRepository.update(tag);
    }

    /**
     * Remove a tag from a user.
     */
    public void removeUserTag(Long userId, Long tagId) {
        UserTag tag = userTagRepository.findById(tagId);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Tag not found or doesn't belong to user");
        }
        userTagRepository.delete(tagId);
    }

    /**
     * Get user statistics.
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get interaction count
        long totalInteractions = interactionRepository.countByUserId(userId);
        stats.put("totalInteractions", totalInteractions);
        
        // Get recent interactions
        List<Interaction> recentInteractions = interactionRepository.findByUserId(userId, 100);
        stats.put("recentInteractionCount", recentInteractions.size());
        
        // Get tag count
        List<UserTag> tags = userTagRepository.findByUserId(userId);
        stats.put("totalTags", tags.size());
        
        // Count tags by category
        Map<String, Integer> tagsByCategory = new HashMap<>();
        for (UserTag tag : tags) {
            tagsByCategory.put(tag.getCategory(), 
                tagsByCategory.getOrDefault(tag.getCategory(), 0) + 1);
        }
        stats.put("tagsByCategory", tagsByCategory);
        
        // Get user info
        User user = userRepository.findById(userId);
        if (user != null) {
            stats.put("username", user.getUsername());
            stats.put("createdAt", user.getCreatedAt());
            stats.put("lastActiveAt", user.getLastActiveAt());
        }
        
        return stats;
    }

    /**
     * Track user interaction for analytics.
     */
    public void trackInteraction(Long userId, Long sessionId, String interactionType, 
                                  String interactionData, String deviceInfo) {
        Interaction interaction = new Interaction();
        interaction.setUserId(userId);
        interaction.setSessionId(sessionId);
        interaction.setInteractionType(interactionType);
        interaction.setInteractionData(interactionData);
        interaction.setDeviceInfo(deviceInfo);
        
        interactionRepository.create(interaction);
        
        // Update user's last active timestamp
        userRepository.updateLastActive(userId);
    }

    /**
     * Get user interactions by type.
     */
    public List<Interaction> getUserInteractionsByType(Long userId, String interactionType, int limit) {
        return interactionRepository.findByUserIdAndType(userId, interactionType, limit);
    }

    /**
     * Clean up old interactions (data retention policy).
     */
    public void cleanupOldInteractions(int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
        Timestamp cutoffTimestamp = new Timestamp(cutoffTime);
        interactionRepository.deleteOlderThan(cutoffTimestamp);
    }

    /**
     * Get user by username.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * List all users (admin function).
     */
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }
}
