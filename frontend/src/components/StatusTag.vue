<template>
  <el-tag
    :type="tagType"
    :effect="effect"
    :size="size"
    :class="['status-tag', `status-tag--${type}`]"
    disable-transitions
  >
    <i v-if="icon" :class="icon"></i>
    <span>{{ displayText }}</span>
  </el-tag>
</template>

<script>
export default {
  name: 'StatusTag',

  props: {
    type: {
      type: String,
      required: true,
      validator: value => ['success', 'warning', 'danger', 'info', 'default'].includes(value)
    },
    text: {
      type: String,
      default: ''
    },
    icon: {
      type: String,
      default: ''
    },
    effect: {
      type: String,
      default: 'light',
      validator: value => ['dark', 'light', 'plain'].includes(value)
    },
    size: {
      type: String,
      default: 'medium',
      validator: value => ['medium', 'small', 'mini'].includes(value)
    }
  },

  computed: {
    tagType() {
      const typeMap = {
        success: 'success',
        warning: 'warning',
        danger: 'danger',
        info: 'info',
        default: 'info'
      }
      return typeMap[this.type] || 'info'
    },

    displayText() {
      if (this.text) return this.text

      const textMap = {
        success: '成功',
        warning: '警告',
        danger: '异常',
        info: '信息',
        default: '默认'
      }
      return textMap[this.type] || '未知'
    }
  }
}
</script>

<style scoped>
.status-tag {
  border-radius: 999px;
  font-weight: 500;
  letter-spacing: 0.3px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.status-tag i {
  font-size: 12px;
}

/* 成功状态 - 绿色系 */
.status-tag--success {
  --tag-bg: rgba(32, 169, 107, 0.08);
  --tag-border: rgba(32, 169, 107, 0.3);
  --tag-text: #20a96b;
}

.status-tag--success.el-tag--dark {
  background-color: var(--tag-bg);
  border-color: var(--tag-border);
  color: var(--tag-text);
}

/* 警告状态 - 黄色系 */
.status-tag--warning {
  --tag-bg: rgba(243, 168, 59, 0.08);
  --tag-border: rgba(243, 168, 59, 0.3);
  --tag-text: #f3a83b;
}

.status-tag--warning.el-tag--dark {
  background-color: var(--tag-bg);
  border-color: var(--tag-border);
  color: var(--tag-text);
}

/* 危险状态 - 红色系 */
.status-tag--danger {
  --tag-bg: rgba(227, 93, 106, 0.08);
  --tag-border: rgba(227, 93, 106, 0.3);
  --tag-text: #e35d6a;
}

.status-tag--danger.el-tag--dark {
  background-color: var(--tag-bg);
  border-color: var(--tag-border);
  color: var(--tag-text);
}

/* 信息状态 - 蓝色系 */
.status-tag--info {
  --tag-bg: rgba(61, 124, 240, 0.08);
  --tag-border: rgba(61, 124, 240, 0.3);
  --tag-text: #3d7cf0;
}

.status-tag--info.el-tag--dark {
  background-color: var(--tag-bg);
  border-color: var(--tag-border);
  color: var(--tag-text);
}

/* 默认状态 - 灰色系 */
.status-tag--default {
  --tag-bg: rgba(108, 125, 137, 0.08);
  --tag-border: rgba(108, 125, 137, 0.3);
  --tag-text: #6c7d89;
}

.status-tag--default.el-tag--dark {
  background-color: var(--tag-bg);
  border-color: var(--tag-border);
  color: var(--tag-text);
}
</style>
