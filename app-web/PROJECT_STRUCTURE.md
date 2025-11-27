# VoiceBox UI - Project Structure

## Overview

This document describes the refactored project structure for the VoiceBox UI application. The project has been reorganized into logical modules following Vue 3 best practices with Composition API, Pinia for state management, and a comprehensive testing setup.

## Directory Structure

```
app-web/
├── src/
│   ├── components/
│   │   ├── chat/              # Chat module components
│   │   │   ├── ChatContainer.vue
│   │   │   ├── SessionSidebar.vue
│   │   │   ├── MessageList.vue
│   │   │   ├── MessageItem.vue
│   │   │   ├── InputArea.vue
│   │   │   └── TypingIndicator.vue
│   │   ├── video/             # Video module components
│   │   │   └── VideoGenerator.vue
│   │   ├── profile/           # User profile components (future)
│   │   ├── common/            # Shared/common components (future)
│   │   └── layout/            # Layout components
│   │       ├── AppShell.vue
│   │       └── ModuleNav.vue
│   ├── stores/                # Pinia stores
│   │   ├── chat.js
│   │   ├── theme.js
│   │   └── ui.js
│   ├── services/              # API services
│   │   ├── api.js
│   │   └── chatService.js
│   ├── composables/           # Vue composables (future)
│   ├── utils/                 # Utility functions
│   │   ├── storage.js
│   │   ├── format.js
│   │   └── __tests__/
│   ├── styles/                # Global styles
│   │   ├── variables.css      # CSS variables for theming
│   │   ├── themes.css         # Theme-specific styles
│   │   └── responsive.css     # Responsive design utilities
│   ├── App.vue
│   └── main.js
├── vitest.config.js           # Vitest configuration
└── package.json
```

## Key Features

### 1. Component Organization

Components are organized by feature/module:
- **chat/**: All chat-related components
- **video/**: Video generation components
- **layout/**: Application shell and navigation
- **common/**: Shared components (to be added)
- **profile/**: User profile components (to be added)

### 2. State Management (Pinia)

Three main stores:
- **chatStore**: Manages chat sessions, messages, and chat state
- **themeStore**: Handles theme switching and persistence
- **uiStore**: Manages UI state (sidebar, current module)

### 3. CSS Variables System

Comprehensive theming system using CSS variables:
- Light and dark theme support
- Consistent spacing, colors, and typography
- Easy theme customization
- Smooth transitions between themes

### 4. Testing Setup

- **Vitest**: Fast unit testing framework
- **fast-check**: Property-based testing library
- **@vue/test-utils**: Vue component testing utilities
- **happy-dom**: Lightweight DOM implementation

### 5. Services Layer

Centralized API communication:
- **api.js**: Base API service with error handling
- **chatService.js**: Chat-specific API methods
- Extensible for additional services

### 6. Utilities

Helper functions for common tasks:
- **storage.js**: LocalStorage wrapper
- **format.js**: Date, file size, and text formatting

## CSS Variables

### Theme Colors

```css
/* Light Theme */
--bg-color: #f9fafb
--text-primary: #111827
--accent-color: #10a37f

/* Dark Theme */
--bg-color: #1a1a1a
--text-primary: #e5e5e5
--accent-color: #10a37f
```

### Spacing

```css
--spacing-xs: 0.25rem
--spacing-sm: 0.5rem
--spacing-md: 1rem
--spacing-lg: 1.5rem
--spacing-xl: 2rem
```

### Border Radius

```css
--radius-sm: 0.375rem
--radius-md: 0.5rem
--radius-lg: 0.75rem
--radius-xl: 1rem
--radius-full: 9999px
```

## Responsive Design

The application uses a mobile-first approach with three breakpoints:
- **Mobile**: < 768px
- **Tablet**: 768px - 1024px
- **Desktop**: > 1024px

## Scripts

```bash
# Development
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with UI
npm run test:ui

# Run tests with coverage
npm run test:coverage
```

## Testing

### Unit Tests

Located in `__tests__` directories next to the code they test:
- `src/utils/__tests__/format.test.js`
- `src/stores/__tests__/theme.test.js`

### Property-Based Tests

Will be added using fast-check for testing universal properties across all inputs.

## Future Enhancements

1. **Composables**: Reusable composition functions
2. **Profile Components**: User profile and settings
3. **Common Components**: Shared UI components (buttons, modals, etc.)
4. **More Services**: User service, analytics service, etc.
5. **E2E Tests**: End-to-end testing with Playwright

## Migration Notes

The original `VoiceBox.vue` component has been split into:
- `ChatContainer.vue`: Main chat logic
- `SessionSidebar.vue`: Session management
- `MessageList.vue`: Message display
- `MessageItem.vue`: Individual message
- `InputArea.vue`: Message input
- `TypingIndicator.vue`: Loading indicator

The original `VideoGenerator.vue` has been moved to `components/video/`.

All functionality remains the same, but the code is now more maintainable and testable.
