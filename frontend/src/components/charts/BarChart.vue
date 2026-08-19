<template>
  <div class="bar-chart-wrapper" ref="chartWrapper">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div class="chart-container" ref="chartContainer" :style="{ height: height }"></div>
  </div>
</template>

<script>
import echarts from '@/utils/echarts'

export default {
  name: 'BarChart',

  props: {
    title: {
      type: String,
      default: ''
    },
    xAxisData: {
      type: Array,
      default: () => []
    },
    seriesData: {
      type: Array,
      default: () => []
    },
    color: {
      type: Array,
      default: () => ['#3d7cf0', '#20a96b', '#f3a83b', '#e35d6a']
    },
    height: {
      type: String,
      default: '400px'
    },
    horizontal: {
      type: Boolean,
      default: false
    },
    showLegend: {
      type: Boolean,
      default: true
    }
  },

  data() {
    return {
      chart: null
    }
  },

  watch: {
    xAxisData: {
      handler() {
        this.updateChart()
      },
      deep: true
    },
    seriesData: {
      handler() {
        this.updateChart()
      },
      deep: true
    }
  },

  mounted() {
    this.initChart()
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },

  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chartContainer)
      this.updateChart()
    },

    updateChart() {
      if (!this.chart) return

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          show: this.showLegend,
          data: this.seriesData.map(item => item.name),
          top: 0,
          textStyle: {
            fontSize: 13,
            color: '#6c7d89'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: this.showLegend ? '15%' : '8%',
          containLabel: true
        },
        xAxis: this.horizontal ? {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: {
            lineStyle: { color: '#eef3f7', type: 'dashed' }
          },
          axisLabel: { color: '#6c7d89', fontSize: 11 }
        } : {
          type: 'category',
          data: this.xAxisData,
          axisLine: { lineStyle: { color: '#dce6eb' } },
          axisTick: { show: false },
          axisLabel: { color: '#6c7d89', fontSize: 11, rotate: this.xAxisData.length > 8 ? 30 : 0 }
        },
        yAxis: this.horizontal ? {
          type: 'category',
          data: this.xAxisData,
          axisLine: { lineStyle: { color: '#dce6eb' } },
          axisTick: { show: false },
          axisLabel: { color: '#6c7d89', fontSize: 11 }
        } : {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: {
            lineStyle: { color: '#eef3f7', type: 'dashed' }
          },
          axisLabel: { color: '#6c7d89', fontSize: 11 }
        },
        series: this.seriesData.map((item, index) => ({
          name: item.name,
          type: 'bar',
          data: item.data,
          barWidth: this.horizontal ? undefined : '30%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(
              this.horizontal ? 1 : 0,
              this.horizontal ? 0 : 1,
              this.horizontal ? 0 : 0,
              this.horizontal ? 1 : 1,
              [
                { offset: 0, color: this.color[index] || this.color[0] },
                { offset: 1, color: `${this.color[index] || this.color[0]}80` }
              ]
            ),
            borderRadius: [4, 4, 0, 0]
          }
        }))
      }

      this.chart.setOption(option, true)
    },

    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    }
  }
}
</script>

<style scoped>
.bar-chart-wrapper {
  width: 100%;
  background-color: #ffffff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
  margin-bottom: 16px;
}

.chart-container {
  width: 100%;
}
</style>
