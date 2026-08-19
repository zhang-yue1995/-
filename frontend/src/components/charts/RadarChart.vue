<template>
  <div class="radar-chart-wrapper">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div class="chart-container" ref="chartContainer" :style="{ height: height }"></div>
  </div>
</template>

<script>
import echarts from '@/utils/echarts'

export default {
  name: 'RadarChart',

  props: {
    title: {
      type: String,
      default: ''
    },
    indicators: {
      type: Array,
      default: () => []
    },
    seriesData: {
      type: Array,
      default: () => []
    },
    colors: {
      type: Array,
      default: () => ['rgba(53, 208, 176, 0.3)', 'rgba(14, 143, 120, 0.6)']
    },
    height: {
      type: String,
      default: '400px'
    },
    shape: {
      type: String,
      default: 'polygon' // polygon | circle
    }
  },

  data() {
    return {
      chart: null
    }
  },

  watch: {
    indicators: {
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
      if (!this.chart || !this.indicators.length) return

      const option = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          data: this.seriesData.map(item => item.name),
          bottom: 0,
          textStyle: {
            fontSize: 13,
            color: '#6c7d89'
          }
        },
        radar: {
          indicator: this.indicators.map(ind => ({
            name: ind.name,
            max: ind.max || 100
          })),
          shape: this.shape,
          splitNumber: 4,
          axisName: {
            color: '#6c7d89',
            fontSize: 12,
            padding: [3, 5]
          },
          splitLine: {
            lineStyle: {
              color: '#eef3f7'
            }
          },
          splitArea: {
            areaStyle: {
              color: ['rgba(238, 243, 247, 0)', 'rgba(238, 243, 247, 0.3)', 'rgba(238, 243, 247, 0.5)', 'rgba(238, 243, 247, 0.7)']
            }
          },
          axisLine: {
            lineStyle: {
              color: '#dce6eb'
            }
          }
        },
        series: [
          {
            type: 'radar',
            data: this.seriesData.map((item, index) => ({
              value: item.value,
              name: item.name,
              symbol: 'circle',
              symbolSize: 6,
              lineStyle: {
                width: 2,
                color: this.colors[index] || this.colors[0]
              },
              areaStyle: {
                color: this.colors[index] || this.colors[0]
              },
              itemStyle: {
                color: this.colors[index] || this.colors[0],
                borderColor: '#fff',
                borderWidth: 2
              }
            }))
          }
        ]
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
.radar-chart-wrapper {
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
