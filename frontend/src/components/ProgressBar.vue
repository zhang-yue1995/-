<template>
  <div class="progress-bar-wrapper">
    <div class="progress-bar__header" v-if="showLabel || percentage !== undefined">
      <span class="progress-bar__label">{{ label }}</span>
      <span class="progress-bar__value" :class="{ 'is-complete': isComplete }">
        {{ formattedPercentage }}%
      </span>
    </div>

    <div class="progress-bar__outer" :class="{ [`progress-bar__outer--${status}`]: status }">
      <div
        class="progress-bar__inner"
        :style="barStyle"
        :class="{ 'is-animated': animated }"
      >
        <span v-if="textInside && !isComplete" class="progress-bar__innerText">
          {{ formattedPercentage }}%
        </span>
      </div>
    </div>

    <div v-if="description" class="progress-bar__description">
      {{ description }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProgressBar',

  props: {
    // 进度值 (0-100)
    percentage: {
      type: Number,
      default: 0,
      validator: value => value >= 0 && value <= 100
    },

    // 标签文字
    label: {
      type: String,
      default: ''
    },

    // 描述信息
    description: {
      type: String,
      default: ''
    },

    // 状态：success/warning/danger/exception
    status: {
      type: String,
      default: '',
      validator: value => ['', 'success', 'warning', 'danger', 'exception'].includes(value)
    },

    // 是否显示标签
    showLabel: {
      type: Boolean,
      default: true
    },

    // 是否在进度条内显示文字
    textInside: {
      type: Boolean,
      default: false
    },

    // 是否启用动画
    animated: {
      type: Boolean,
      default: true
    },

    // 颜色（支持字符串或数组渐变）
    color: {
      type: [String, Array],
      default: ''
    },

    // 自定义高度
    strokeWidth: {
      type: Number,
      default: 8
    },

    // 是否显示条纹效果
    striped: {
      type: Boolean,
      default: false
    },

    // 条纹是否流动
    stripedFlow: {
      type: Boolean,
      default: false
    }
  },

  computed: {
    barStyle() {
      const style = {
        width: `${this.percentage}%`,
        height: `${this.strokeWidth}px`
      }

      if (this.color) {
        if (Array.isArray(this.color)) {
          style.background = `linear-gradient(to right, ${this.color.join(', ')})`
        } else {
          style.backgroundColor = this.color
        }
      }

      return style
    },

    formattedPercentage() {
      return Math.round(this.percentage)
    },

    isComplete() {
      return this.percentage >= 100
    }
  }
}
</script>

<style scoped>
.progress-bar-wrapper {
  width: 100%;
}

.progress-bar__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-bar__label {
  font-size: 13px;
  color: #3d5563;
  font-weight: 500;
}

.progress-bar__value {
  font-size: 13px;
  font-weight: 600;
  color: #3d5563;
  transition: color 0.3s;
}

.progress-bar__value.is-complete {
  color: #20a96b;
}

.progress-bar__outer {
  width: 100%;
  height: 8px;
  background-color: #eef3f7;
  border-radius: 999px;
  overflow: hidden;
  position: relative;
}

.progress-bar__outer--success .progress-bar__inner {
  background-color: #20a96b;
}

.progress-bar__outer--warning .progress-bar__inner {
  background-color: #f3a83b;
}

.progress-bar__outer--danger .progress-bar__inner {
  background-color: #e35d6a;
}

.progress-bar__outer--exception .progress-bar__inner {
  background-color: #e35d6a;
}

.progress-bar__inner {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #20a96b 0%, #42c98a 100%);
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 2px;
}

.progress-bar__inner.is-animated {
  animation: progress-animation 0.4s ease-out;
}

@keyframes progress-animation {
  from {
    width: 0;
  }
}

/* 条纹效果 */
.progress-bar__inner.striped {
  background-image: linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.15) 25%,
    transparent 25%,
    transparent 50%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.15) 75%,
    transparent 75%,
    transparent
  );
  background-size: 30px 30px;
}

.progress-bar__inner.striped-flow {
  animation: stripes-flow 1s linear infinite;
}

@keyframes stripes-flow {
  from {
    background-position: 30px 0;
  }
  to {
    background-position: 0 0;
  }
}

.progress-bar__innerText {
  color: white;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  padding: 0 8px;
}

.progress-bar__description {
  margin-top: 8px;
  font-size: 12px;
  color: #6c7d89;
  line-height: 1.5;
}
</style>
