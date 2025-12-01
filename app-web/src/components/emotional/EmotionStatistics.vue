<template>
  <div class="emotion-statistics">
    <!-- 统计概览 -->
    <div class="statistics-overview">
      <div class="overview-card">
        <div class="card-icon">📊</div>
        <div class="card-content">
          <div class="card-value">{{ totalAnalyses }}</div>
          <div class="card-label">总分析次数</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">😊</div>
        <div class="card-content">
          <div class="card-value">{{ dominantEmotion }}</div>
          <div class="card-label">主导情绪</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">📈</div>
        <div class="card-content">
          <div class="card-value">{{ averageConfidence }}%</div>
          <div class="card-label">平均置信度</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="card-icon">🏷️</div>
        <div class="card-content">
          <div class="card-value">{{ totalTags }}</div>
          <div class="card-label">情感标签数</div>
        </div>
      </div>
    </div>

    <!-- 情绪分布图 -->
    <div class="chart-section">
      <h3 class="chart-title">情绪分布</h3>
      <div class="chart-container">
        <svg class="pie-chart" viewBox="0 0 200 200">
          <g transform="translate(100, 100)">
            <path
              v-for="(segment, index) in pieSegments"
              :key="index"
              :d="segment.path"
              :fill="segment.color"
              :class="['pie-segment', { active: hoveredSegment === index }]"
              @mouseenter="hoveredSegment = index"
              @mouseleave="hoveredSegment = null"
            >
              <title>{{ segment.label }}: {{ segment.percentage }}%</title>
            </path>
          </g>
        </svg>
        <div class="chart-legend">
          <div 
            v-for="(item, index) in emotionDistribution"
            :key="item.emotion"
            class="legend-item"
          >
            <div 
              class="legend-color"
              :style="{ backgroundColor: getEmotionColor(item.emotion) }"
            ></div>
            <span class="legend-label">{{ getEmotionName(item.emotion) }}</span>
            <span class="legend-value">{{ item.count }} ({{ item.percentage }}%)</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 情绪趋势图 -->
    <div class="chart-section">
      <h3 class="chart-title">情绪趋势</h3>
      <div class="chart-container">
        <svg class="line-chart" viewBox="0 0 400 200">
          <!-- 网格线 -->
          <defs>
            <pattern id="grid-pattern" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="var(--border-color)" stroke-width="0.5" opacity="0.3"/>
            </pattern>
          </defs>
          <rect width="400" height="200" fill="url(#grid-pattern)" />
          
          <!-- 趋势线 -->
          <polyline
            v-for="(line, emotion) in trendLines"
            :key="emotion"
            :points="line.points"
            fill="none"
            :stroke="getEmotionColor(emotion)"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          
          <!-- 数据点 -->
          <g v-for="(line, emotion) in trendLines" :key="`points-${emotion}`">
            <circle
              v-for="(point, index) in line.pointsArray"
              :key="index"
              :cx="point.x"
              :cy="point.y"
              r="3"
              :fill="getEmotionColor(emotion)"
              stroke="white"
              stroke-width="1"
            >
              <title>{{ getEmotionName(emotion) }}: {{ point.value }}</title>
            </circle>
          </g>
        </svg>
        <div class="chart-legend">
          <div 
            v-for="emotion in Object.keys(trendLines)"
            :key="emotion"
            class="legend-item"
          >
            <div 
              class="legend-color"
              :style="{ backgroundColor: getEmotionColor(emotion) }"
            ></div>
            <span class="legend-label">{{ getEmotionName(emotion) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 性格雷达图 -->
    <div v-if="personalityData" class="chart-section">
      <h3 class="chart-title">性格特征</h3>
      <div class="chart-container">
        <svg class="radar-chart" viewBox="0 0 300 300">
          <g transform="translate(150, 150)">
            <!-- 背景网格 -->
            <g v-for="level in 5" :key="`grid-${level}`">
              <polygon
                :points="getRadarPolygonPoints(level * 20)"
                fill="none"
                stroke="var(--border-color)"
                stroke-width="0.5"
                opacity="0.3"
              />
            </g>
            
            <!-- 轴线 -->
            <g v-for="(axis, index) in radarAxes" :key="`axis-${index}`">
              <line
                x1="0"
                y1="0"
                :x2="axis.x"
                :y2="axis.y"
                stroke="var(--border-color)"
                stroke-width="0.5"
              />
              <text
                :x="axis.labelX"
                :y="axis.labelY"
                text-anchor="middle"
                dominant-baseline="middle"
                font-size="12"
                fill="var(--text-primary)"
              >
                {{ axis.label }}
              </text>
            </g>
            
            <!-- 数据区域 -->
            <polygon
              :points="radarDataPoints"
              fill="var(--primary-color)"
              fill-opacity="0.3"
              stroke="var(--primary-color)"
              stroke-width="2"
            />
            
            <!-- 数据点 -->
            <circle
              v-for="(point, index) in radarDataPointsArray"
              :key="index"
              :cx="point.x"
              :cy="point.y"
              r="4"
              fill="var(--primary-color)"
              stroke="white"
              stroke-width="2"
            >
              <title>{{ radarAxes[index].label }}: {{ point.value }}</title>
            </circle>
          </g>
        </svg>
      </div>
    </div>

    <!-- 时间段分析 -->
    <div class="chart-section">
      <h3 class="chart-title">时间段分析</h3>
      <div class="time-analysis">
        <div 
          v-for="period in timePeriods"
          :key="period.label"
          class="time-period-card"
        >
          <div class="period-label">{{ period.label }}</div>
          <div class="period-emotion">
            <span class="period-emoji">{{ getEmotionEmoji(period.dominantEmotion) }}</span>
            <span class="period-name">{{ getEmotionName(period.dominantEmotion) }}</span>
          </div>
          <div class="period-count">{{ period.count }} 次分析</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

// 悬停的饼图段
const hoveredSegment = ref(null)

// 情绪名称和颜色映射
const emotionNames = {
  HAPPY: '开心',
  SAD: '悲伤',
  ANGRY: '愤怒',
  CALM: '平静',
  ANXIOUS: '焦虑',
  EXCITED: '兴奋',
  NEUTRAL: '中性'
}

const emotionEmojis = {
  HAPPY: '😊',
  SAD: '😢',
  ANGRY: '😠',
  CALM: '😌',
  ANXIOUS: '😰',
  EXCITED: '🤩',
  NEUTRAL: '😐'
}

const emotionColors = {
  HAPPY: '#f39c12',
  SAD: '#3498db',
  ANGRY: '#e74c3c',
  CALM: '#2ecc71',
  ANXIOUS: '#34495e',
  EXCITED: '#e67e22',
  NEUTRAL: '#95a5a6'
}

const getEmotionName = (emotion) => emotionNames[emotion] || emotion
const getEmotionEmoji = (emotion) => emotionEmojis[emotion] || '😐'
const getEmotionColor = (emotion) => emotionColors[emotion] || '#95a5a6'

// 统计概览数据
const totalAnalyses = computed(() => props.data?.totalAnalyses || 0)

const dominantEmotion = computed(() => {
  if (!props.data?.emotionDistribution || props.data.emotionDistribution.length === 0) {
    return '无'
  }
  const dominant = props.data.emotionDistribution[0]
  return getEmotionName(dominant.emotion)
})

const averageConfidence = computed(() => {
  return Math.round((props.data?.averageConfidence || 0) * 100)
})

const totalTags = computed(() => props.data?.totalTags || 0)

// 情绪分布数据
const emotionDistribution = computed(() => {
  return props.data?.emotionDistribution || []
})

// 饼图段数据
const pieSegments = computed(() => {
  const segments = []
  let currentAngle = -90 // 从顶部开始
  
  emotionDistribution.value.forEach((item, index) => {
    const percentage = item.percentage || 0
    const angle = (percentage / 100) * 360
    const endAngle = currentAngle + angle
    
    const path = createArcPath(currentAngle, endAngle, 80)
    
    segments.push({
      path,
      color: getEmotionColor(item.emotion),
      label: getEmotionName(item.emotion),
      percentage: percentage.toFixed(1)
    })
    
    currentAngle = endAngle
  })
  
  return segments
})

// 创建弧形路径
const createArcPath = (startAngle, endAngle, radius) => {
  const start = polarToCartesian(0, 0, radius, endAngle)
  const end = polarToCartesian(0, 0, radius, startAngle)
  const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1'
  
  return [
    'M', 0, 0,
    'L', start.x, start.y,
    'A', radius, radius, 0, largeArcFlag, 0, end.x, end.y,
    'Z'
  ].join(' ')
}

// 极坐标转笛卡尔坐标
const polarToCartesian = (centerX, centerY, radius, angleInDegrees) => {
  const angleInRadians = (angleInDegrees - 90) * Math.PI / 180.0
  return {
    x: centerX + (radius * Math.cos(angleInRadians)),
    y: centerY + (radius * Math.sin(angleInRadians))
  }
}

// 趋势线数据
const trendLines = computed(() => {
  const trends = props.data?.emotionTrends || {}
  const lines = {}
  
  Object.keys(trends).forEach(emotion => {
    const data = trends[emotion] || []
    const points = data.map((value, index) => {
      const x = (index / (data.length - 1 || 1)) * 380 + 10
      const y = 190 - (value * 180)
      return `${x},${y}`
    }).join(' ')
    
    const pointsArray = data.map((value, index) => ({
      x: (index / (data.length - 1 || 1)) * 380 + 10,
      y: 190 - (value * 180),
      value: Math.round(value * 100)
    }))
    
    lines[emotion] = { points, pointsArray }
  })
  
  return lines
})

// 性格数据
const personalityData = computed(() => props.data?.personality)

// 雷达图轴
const radarAxes = computed(() => {
  if (!personalityData.value) return []
  
  const axes = [
    { label: '外向性', value: personalityData.value.extroversion || 0 },
    { label: '开放性', value: personalityData.value.openness || 0 },
    { label: '情绪稳定性', value: personalityData.value.stability || 0 },
    { label: '亲和性', value: personalityData.value.agreeableness || 0 },
    { label: '责任心', value: personalityData.value.conscientiousness || 0 }
  ]
  
  return axes.map((axis, index) => {
    const angle = (index / axes.length) * 2 * Math.PI - Math.PI / 2
    const radius = 100
    const labelRadius = 120
    
    return {
      label: axis.label,
      value: axis.value,
      x: Math.cos(angle) * radius,
      y: Math.sin(angle) * radius,
      labelX: Math.cos(angle) * labelRadius,
      labelY: Math.sin(angle) * labelRadius
    }
  })
})

// 雷达图多边形点
const getRadarPolygonPoints = (radius) => {
  const count = radarAxes.value.length
  return radarAxes.value.map((_, index) => {
    const angle = (index / count) * 2 * Math.PI - Math.PI / 2
    const x = Math.cos(angle) * radius
    const y = Math.sin(angle) * radius
    return `${x},${y}`
  }).join(' ')
}

// 雷达图数据点
const radarDataPoints = computed(() => {
  if (!radarAxes.value.length) return ''
  
  return radarAxes.value.map(axis => {
    const angle = radarAxes.value.indexOf(axis) / radarAxes.value.length * 2 * Math.PI - Math.PI / 2
    const radius = axis.value * 100
    const x = Math.cos(angle) * radius
    const y = Math.sin(angle) * radius
    return `${x},${y}`
  }).join(' ')
})

const radarDataPointsArray = computed(() => {
  if (!radarAxes.value.length) return []
  
  return radarAxes.value.map(axis => {
    const angle = radarAxes.value.indexOf(axis) / radarAxes.value.length * 2 * Math.PI - Math.PI / 2
    const radius = axis.value * 100
    return {
      x: Math.cos(angle) * radius,
      y: Math.sin(angle) * radius,
      value: Math.round(axis.value * 100)
    }
  })
})

// 时间段分析
const timePeriods = computed(() => {
  return props.data?.timePeriods || []
})

watch(() => props.data, (newData) => {
  console.log('统计数据已更新:', newData)
}, { deep: true })
</script>

<style scoped>
.emotion-statistics {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.statistics-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-md);
}

.overview-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background-color: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
}

.overview-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-icon {
  font-size: 2.5rem;
  flex-shrink: 0;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.card-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary-color);
}

.card-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.chart-section {
  background-color: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
}

.chart-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-lg) 0;
  padding-bottom: var(--spacing-sm);
  border-bottom: 2px solid var(--primary-color);
}

.chart-container {
  display: flex;
  gap: var(--spacing-lg);
  align-items: center;
}

.pie-chart,
.line-chart,
.radar-chart {
  flex-shrink: 0;
}

.pie-chart {
  width: 200px;
  height: 200px;
}

.line-chart {
  width: 100%;
  max-width: 400px;
  height: 200px;
}

.radar-chart {
  width: 300px;
  height: 300px;
}

.pie-segment {
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.pie-segment:hover {
  opacity: 0.8;
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 0.875rem;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  color: var(--text-primary);
}

.legend-value {
  color: var(--text-secondary);
  font-weight: 500;
}

.time-analysis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-md);
}

.time-period-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  text-align: center;
}

.period-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.period-emotion {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
}

.period-emoji {
  font-size: 2rem;
}

.period-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.period-count {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

/* 响应式设计 */
@media (max-width: 767px) {
  .statistics-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-container {
    flex-direction: column;
  }
  
  .pie-chart,
  .radar-chart {
    width: 100%;
    max-width: 300px;
  }
  
  .time-analysis {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .statistics-overview {
    grid-template-columns: 1fr;
  }
  
  .time-analysis {
    grid-template-columns: 1fr;
  }
}
</style>
