import { describe, it, expect } from 'vitest'
import { formatDate, formatFileSize, truncateText } from '../format'

describe('format utilities', () => {
  describe('formatDate', () => {
    it('should format ISO date string', () => {
      const result = formatDate('2024-01-01T00:00:00.000Z')
      expect(result).toBeTruthy()
      expect(typeof result).toBe('string')
    })

    it('should return empty string for null', () => {
      expect(formatDate(null)).toBe('')
    })

    it('should return empty string for undefined', () => {
      expect(formatDate(undefined)).toBe('')
    })
  })

  describe('formatFileSize', () => {
    it('should format bytes correctly', () => {
      expect(formatFileSize(0)).toBe('0 Bytes')
      expect(formatFileSize(1024)).toBe('1 KB')
      expect(formatFileSize(1048576)).toBe('1 MB')
    })
  })

  describe('truncateText', () => {
    it('should truncate long text', () => {
      const longText = 'a'.repeat(100)
      const result = truncateText(longText, 50)
      expect(result.length).toBe(53) // 50 + '...'
      expect(result.endsWith('...')).toBe(true)
    })

    it('should not truncate short text', () => {
      const shortText = 'hello'
      expect(truncateText(shortText, 50)).toBe('hello')
    })
  })
})
