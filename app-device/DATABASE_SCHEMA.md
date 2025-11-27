# Database Schema Documentation

## Overview

This document describes the database schema implemented for the VoiceBox UI optimization project, including user profiles, tags, interactions, and device management.

## Tables Created

### 1. users
Stores user profile information with encrypted sensitive data.

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,              -- Encrypted (PII)
    password_hash VARCHAR(255),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP,
    preferences JSON,
    INDEX idx_username (username),
    INDEX idx_email (email)
);
```

**Features:**
- Email field is encrypted using AES-128 for PII protection
- Tracks user activity with `last_active_at`
- Stores user preferences as JSON

### 2. user_tags
Stores tags assigned to users for personalization with confidence scores.

```sql
CREATE TABLE user_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,          -- e.g., 'semantic', 'behavioral', 'preference'
    tag_name VARCHAR(100) NOT NULL,
    confidence DECIMAL(5,4) DEFAULT 0.5000,
    source VARCHAR(50),                     -- 'auto', 'manual', 'admin'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_tag_name (tag_name)
);
```

**Features:**
- Confidence scores (0.0000 to 1.0000) for tag reliability
- Categorized tags for different personalization aspects
- Tracks tag source (auto-generated, manual, or admin-assigned)

### 3. interactions
Tracks user behavior and interactions for analytics.

```sql
CREATE TABLE interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    interaction_type VARCHAR(50),           -- 'message', 'click', 'scroll', 'voice'
    interaction_data JSON,
    device_info JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
);
```

**Features:**
- Flexible JSON storage for interaction details
- Device information tracking
- Indexed by time for efficient analytics queries

### 4. devices
Manages hardware devices (e.g., Raspberry Pi) connected to user accounts.

```sql
CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_type VARCHAR(50),                -- 'web', 'mobile', 'raspberry_pi'
    device_name VARCHAR(100),
    api_key VARCHAR(255) UNIQUE,
    last_sync_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    device_metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_api_key (api_key)
);
```

**Features:**
- Auto-generated API keys for device authentication
- Tracks device sync status
- Supports multiple device types per user

### 5. Enhanced chat_session
Extended with user association and personalization context.

**New Fields:**
- `user_id BIGINT` - Associates sessions with users
- `tags JSON` - Session-specific tags
- `personalization_context JSON` - Context for AI personalization

### 6. Enhanced chat_message
Extended with sentiment analysis and topic extraction.

**New Fields:**
- `sentiment VARCHAR(20)` - Message sentiment (positive, negative, neutral)
- `extracted_topics JSON` - Topics extracted from message
- `response_time_ms INT` - AI response time tracking

## Java Implementation

### Domain Models
- `User.java` - User entity
- `UserTag.java` - User tag entity
- `Interaction.java` - Interaction tracking entity
- `Device.java` - Device entity

### Repositories
- `UserRepository.java` - CRUD operations for users with encryption
- `UserTagRepository.java` - Tag management
- `InteractionRepository.java` - Interaction tracking
- `DeviceRepository.java` - Device management

### Services
- `UserProfileService.java` - Business logic for user profiles, tags, and analytics

### Controllers
- `UserProfileController.java` - REST API endpoints for user management

## API Endpoints

### User Profile
- `GET /api/users/{id}/profile` - Get user profile
- `POST /api/users` - Create user
- `PUT /api/users/{id}/profile` - Update user profile
- `DELETE /api/users/{id}` - Delete user

### User Tags
- `GET /api/users/{id}/tags` - Get user tags
- `POST /api/users/{id}/tags` - Add tag to user
- `DELETE /api/users/{id}/tags/{tagId}` - Remove tag

### User Stats
- `GET /api/users/{id}/stats` - Get user statistics

### Admin
- `GET /api/users` - List all users

## Security Features

1. **Data Encryption**: Email addresses are encrypted using AES-128
2. **Cascade Deletion**: User deletion removes all associated data
3. **API Key Generation**: Secure random API keys for devices
4. **CORS Support**: Cross-origin requests enabled for frontend

## Migration Support

The implementation includes automatic migration support:
- Tables are created if they don't exist
- New columns are added to existing tables without data loss
- Backward compatibility with existing chat_session and chat_message tables

## Data Retention

The `InteractionRepository` includes a cleanup method to remove old interactions based on retention policy (default: 90 days).

## Requirements Validation

This implementation satisfies:
- **Requirement 12.1**: Message storage with content, timestamp, and session context
- **Requirement 12.2**: Device information recording
- **Requirement 12.5**: Data encryption for sensitive fields (email)
- **Requirement 13.4**: Tag storage with confidence scores and timestamps
- **Requirement 16.5**: Multi-device session support
