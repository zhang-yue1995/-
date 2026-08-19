<template>
  <div class="pie-chart-wrapper">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div class="chart-container" ref="chartContainer" :style="{ height: height }"></div>
  </div>
</template>

<script>
import echarts from '@/utils/echarts'

export default {
  name: 'PieChart',

  props: {
    title: {
      type: String,
      default: ''
    },
    chartData: {
      type: Array,
      default: () => []
    },
    colors: {
      type: Array,
      default: () => ['#20a96b', '#9dd99e', '#f3a83b', '#e35d6a', '#3d7cf0']
    },
    height: {
      type: String,
      default: '400px'
    },
    donut: {
      type: Boolean,
      default: true
    },
    radius: {
      type: Array,
      default: () => ['45%', '70%']
    },
    showLegend: {
      type: Boolean,
      default: true
    },
    legendPosition: {
      type: String,
      default: 'right' // right / bottom
    }
  },

  data() {
    return {
      chart: null
    }
  },

  watch: {
    chartData: {
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

      const legendPosition = this.legendPosition === 'bottom'
        ? { bottom: '5%', left: 'center' }
        : { right: '8%', top: 'center', orient: 'vertical' }

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          show: this.showLegend,
          ...legendPosition,
          textStyle: {
            fontSize: 13,
            color: '#6c7d89'
          },
          itemWidth: 12,
          itemHeight: 12,
          itemGap: 15
        },
        series: [
          {
            type: 'pie',
            radius: this.donut ? this.radius : '65%',
            center: ['40%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderColor: '#ffffff',
              borderWidth: 2,
              borderRadius: 4
            },
            label: {
              show: !this.donut,
              position: 'outside',
              formatter: '{b}\n{d}%',
              fontSize: 12,
              color: '#6c7d89'
            },
            labelLine: {
              show: !this.donut,
              length: 15,
              length2: 10
            },
            emphasis: {
              label: {
                show: this.donut,
                fontSize: 14,
                fontWeight: 'bold'
              },
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.2)'
              }
            },
            data: this.chartData.map((item, index) => ({
              value: item.value,
              name: item.name,
              itemStyle: {
                color: this.colors[index % this.colors.length]
              }
            }))
          }
        ]
      }

      // 如果是环形图，在中心显示总计
      if (this.donut && this.chartData.length > 0) {
        const total = this.chartData.reduce((sum, item) => sum + item.value, 0)
        option.graphic = [{
          type: 'text',
          left: '32%',
          top: '46%',
          style: {
            text: `${total}`,
            textAlign: 'center',
            fill: '#10212b',
            fontSize: 24,
            fontWeight: 'bold'
          }
        }, {
          type: 'text',
          left: '32%',
          top: '56%',
          style: {
            text: '总计',
            textAlign: 'center',
            fill: '#6c7d89',
            fontSize: 12
          }
        }]
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
.pie-chart-wrapper {
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
