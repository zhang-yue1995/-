<template>
  <div
    class="kpi-card"
    :class="{ [`kpi-card--${color}`]: color, 'is-clickable': clickable }"
    :role="clickable ? 'button' : null"
    :tabindex="clickable ? 0 : null"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <div class="kpi-card__header">
      <div class="kpi-card__icon" :style="{ backgroundColor: iconBgColor }">
        <i :class="icon"></i>
      </div>
      <div class="kpi-card__trend" :class="trendClass">
        <i :class="trendIcon"></i>
        <span>{{ trendText }}</span>
      </div>
    </div>

    <div class="kpi-card__body">
      <div class="kpi-card__value">
        {{ formattedValue }}
        <span v-if="unit" class="kpi-card__unit">{{ unit }}</span>
      </div>
      <div class="kpi-card__title">{{ title }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'KpiCard',

  props: {
    title: {
      type: String,
      required: true
    },
    value: {
      type: [Number, String],
      required: true
    },
    unit: {
      type: String,
      default: ''
    },
    trend: {
      type: Number,
      default: 0 // 正数表示上升，负数表示下降，0表示无变化
    },
    trendText: {
      type: String,
      default: ''
    },
    icon: {
      type: String,
      default: 'el-icon-data-line'
    },
    color: {
      type: String,
      default: 'primary', // primary, success, warning, danger
      validator: value => ['primary', 'success', 'warning', 'danger'].includes(value)
    },
    clickable: {
      type: Boolean,
      default: false
    }
  },

  methods: {
    handleClick() {
      if (this.clickable) this.$emit('click')
    }
  },

  computed: {
    formattedValue() {
      if (typeof this.value === 'number') {
        // 如果是百分比类型
        if (this.unit === '%') {
          return this.value.toFixed(1)
        }
        // 大数字格式化（千分位）
        if (Math.abs(this.value) >= 10000) {
          return this.value.toLocaleString('zh-CN')
        }
        return this.value.toString()
      }
      return this.value
    },

    trendIcon() {
      if (this.trend > 0) return 'el-icon-top'
      if (this.trend < 0) return 'el-icon-bottom'
      return 'el-icon-minus'
    },

    trendClass() {
      if (this.trend > 0) return 'is-up'
      if (this.trend < 0) return 'is-down'
      return ''
    },

    iconBgColor() {
      const colorMap = {
        primary: 'rgba(61, 124, 240, 0.1)',
        success: 'rgba(32, 169, 107, 0.1)',
        warning: 'rgba(243, 168, 59, 0.1)',
        danger: 'rgba(227, 93, 106, 0.1)'
      }
      return colorMap[this.color] || colorMap.primary
    }
  }
}
</script>

<style scoped>
.kpi-card {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
  transition: all 0.3s ease;
  cursor: default;
  position: relative;
  overflow: hidden;
}

.kpi-card.is-clickable {
  cursor: pointer;
}

.kpi-card.is-clickable:focus-visible {
  outline: 3px solid rgba(14, 143, 120, 0.25);
  outline-offset: 2px;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(27, 61, 78, 0.12);
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--card-accent, #3d7cf0), var(--card-accent-light, #5fa3ff));
}

.kpi-card--primary {
  --card-accent: #3d7cf0;
  --card-accent-light: #5fa3ff;
}

.kpi-card--success {
  --card-accent: #20a96b;
  --card-accent-light: #42c98a;
}

.kpi-card--warning {
  --card-accent: #f3a83b;
  --card-accent-light: #f5ba5e;
}

.kpi-card--danger {
  --card-accent: #e35d6a;
  --card-accent-light: #e87d88;
}

.kpi-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.kpi-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--card-accent, #3d7cf0);
}

.kpi-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 999px;
}

.kpi-card__trend.is-up {
  color: #20a96b;
  background-color: rgba(32, 169, 107, 0.08);
}

.kpi-card__trend.is-down {
  color: #e35d6a;
  background-color: rgba(227, 93, 106, 0.08);
}

.kpi-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kpi-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #10212b;
  line-height: 1.2;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.kpi-card__unit {
  font-size: 14px;
  font-weight: 500;
  color: #6c7d89;
}

.kpi-card__title {
  font-size: 13px;
  color: #6c7d89;
  font-weight: 500;
}
</style>
