<template>
  <div v-loading="loading" class="trend-monitor-page">
    <div class="page-header">
      <h2 class="page-title">历史趋势监控</h2>
      <p class="page-subtitle">{{ selectedEnterpriseName }} · 多维度财务指标时序分析与预警</p>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">企业：</span>
        <el-select v-model="enterpriseId" filterable size="small" @change="fetchTrendData">
          <el-option
            v-for="enterprise in enterprises"
            :key="enterprise.id"
            :label="enterprise.name"
            :value="enterprise.id"
          />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">查询期数：</span>
        <el-select v-model="periods" size="small" @change="fetchTrendData">
          <el-option label="近5期" :value="5" />
          <el-option label="近10期" :value="10" />
          <el-option label="近20期" :value="20" />
        </el-select>
      </div>
    </div>

    <el-alert
      v-if="quarterLabels.length < 2"
      title="历史数据不足"
      description="当前仅有一期真实报表，无法判断变化方向；后续归档报表会自动进入趋势序列。"
      type="info"
      :closable="false"
      show-icon
      class="insufficient-alert"
    />

    <div class="charts-grid">
      <LineChart
        title="盈利能力趋势"
        :xAxisData="quarterLabels"
        :seriesData="profitabilitySeries"
        :color="['#3d7cf0', '#20a96b']"
        height="360px"
      />
      <LineChart
        title="杠杆与流动性趋势"
        :xAxisData="quarterLabels"
        :seriesData="leverageSeries"
        :color="['#e35d6a', '#3d7cf0']"
        height="360px"
      />
    </div>

    <div class="indicator-cards-grid">
      <div
        v-for="(indicator, index) in indicatorCards"
        :key="indicator.code"
        class="indicator-card"
        :class="[`card-${indicator.status}`]"
      >
        <div class="card-header">
          <span class="card-label">{{ indicator.label }}</span>
          <canvas :ref="`sparkline${index}`" width="80" height="30"></canvas>
        </div>
        <div class="card-value">{{ indicator.value }}{{ indicator.unit }}</div>
        <div class="card-trend" :class="{ 'is-negative': indicator.trend < 0, 'is-positive': indicator.trend > 0 }">
          <i v-if="indicator.trend !== null" :class="indicator.trend > 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
          <span>{{ indicator.trendText }}</span>
        </div>
        <div v-if="indicator.alert" class="card-alert">
          <i class="el-icon-warning-outline"></i>
          {{ indicator.alert }}
        </div>
      </div>
    </div>

    <div class="alert-table-section">
      <div class="section-header">
        <h3 class="section-title">趋势预警记录</h3>
        <el-badge :value="alertCount" :hidden="alertCount === 0" type="danger">
          <el-tag size="small" effect="dark">待处理</el-tag>
        </el-badge>
      </div>
      <DataTable
        :data="alertRecords"
        :loading="loading"
        :total="alertRecords.length"
        :showIndex="true"
        :showPagination="false"
        emptyText="当前指标未触发预警"
      >
        <el-table-column prop="alertTime" label="报告期" width="130" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="indicatorName" label="指标名称" width="160" />
        <el-table-column prop="currentValue" label="当前值" width="120" align="right">
          <template slot-scope="{ row }">
            <span class="text-danger">{{ row.currentValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="预警阈值" width="120" align="right" />
        <el-table-column prop="trend" label="变化趋势" width="100" align="center" />
        <el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
          <template slot-scope="{ row }">
            <StatusTag :type="row.riskLevel === '高' ? 'danger' : 'warning'" :text="row.riskLevel" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <StatusTag :type="row.status === '已处理' ? 'success' : 'warning'" :text="row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleViewAlert">查看报表</el-button>
            <el-button
              v-if="row.status === '待确认'"
              type="success"
              size="mini"
              plain
              @click="handleMarkProcessed(row)"
            >
              标记处理
            </el-button>
          </template>
        </el-table-column>
      </DataTable>
    </div>
  </div>
</template>

<script>
import { getMultiIndicatorTrend } from '@/api/trend'
import { getEnterpriseList } from '@/api/enterprise'
import LineChart from '@/components/charts/LineChart.vue'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'

const CARD_CONFIG = {
  netProfitMargin: { label: '销售净利率', unit: '%', low: 3 },
  roe: { label: '净资产收益率', unit: '%', low: 8 },
  debtToAssetRatio: { label: '资产负债率', unit: '%', high: 80 },
  currentRatio: { label: '流动比率', unit: 'x', low: 1.5 }
}

export default {
  name: 'TrendMonitor',

  components: {
    LineChart,
    DataTable,
    StatusTag
  },

  data() {
    return {
      loading: false,
      enterpriseId: null,
      enterprises: [],
      periods: 10,
      quarterLabels: [],
      trends: [],
      profitabilitySeries: [],
      leverageSeries: [],
      indicatorCards: [],
      alertRecords: []
    }
  },

  computed: {
    selectedEnterpriseName() {
      const enterprise = this.enterprises.find(item => item.id === this.enterpriseId)
      return enterprise ? enterprise.name : '请选择企业'
    },
    alertCount() {
      return this.alertRecords.filter(row => row.status === '待确认').length
    }
  },

  async created() {
    await this.loadEnterprises()
    await this.fetchTrendData()
  },

  methods: {
    async loadEnterprises() {
      try {
        const page = await getEnterpriseList({ pageNum: 1, pageSize: 100, activeReportsOnly: true })
        this.enterprises = page.list || []
        this.enterpriseId = Number(this.$route.query.enterpriseId) ||
          (this.enterprises[0] && this.enterprises[0].id)
      } catch (error) {
        console.error('Failed to load enterprises:', error)
        this.$message.error('企业列表加载失败')
      }
    },

    async fetchTrendData() {
      if (!this.enterpriseId) return
      this.loading = true
      try {
        this.trends = await getMultiIndicatorTrend(this.enterpriseId, { periods: this.periods }) || []
        const periodSet = new Set()
        this.trends.forEach(trend => {
          ;(trend.dataList || []).forEach(point => periodSet.add(point.reportPeriod))
        })
        this.quarterLabels = Array.from(periodSet)
        this.profitabilitySeries = [
          this.makeSeries('netProfitMargin', '销售净利率'),
          this.makeSeries('roe', '净资产收益率')
        ]
        this.leverageSeries = [
          this.makeSeries('debtToAssetRatio', '资产负债率'),
          this.makeSeries('currentRatio', '流动比率')
        ]
        this.indicatorCards = Object.keys(CARD_CONFIG).map(code => this.makeCard(code))
        this.alertRecords = this.buildAlerts()
        this.$nextTick(this.drawSparklines)
      } catch (error) {
        console.error('Failed to fetch trend data:', error)
        this.$message.error('趋势数据加载失败')
      } finally {
        this.loading = false
      }
    },

    findTrend(code) {
      return this.trends.find(item => item.indicatorCode === code) || { dataList: [] }
    },

    makeSeries(code, name) {
      const points = this.findTrend(code).dataList || []
      const valueMap = points.reduce((map, point) => {
        map[point.reportPeriod] = point.value
        return map
      }, {})
      return {
        name,
        data: this.quarterLabels.map(period => valueMap[period] === undefined ? null : valueMap[period])
      }
    },

    makeCard(code) {
      const config = CARD_CONFIG[code]
      const points = this.findTrend(code).dataList || []
      const latest = points[points.length - 1] || {}
      const value = latest.value === null || latest.value === undefined
        ? '—'
        : Number(latest.value).toFixed(2)
      const trend = latest.changeRate === null || latest.changeRate === undefined
        ? null
        : Number(latest.changeRate)
      let alert = null
      if (config.high !== undefined && Number(latest.value) >= config.high) {
        alert = `超过 ${config.high}${config.unit} 警戒线`
      }
      if (config.low !== undefined && Number(latest.value) < config.low) {
        alert = `低于 ${config.low}${config.unit} 警戒线`
      }
      return {
        code,
        label: config.label,
        unit: config.unit,
        value,
        trend,
        trendText: trend === null ? '历史数据不足' : `较上期 ${trend > 0 ? '+' : ''}${trend.toFixed(2)}%`,
        status: alert ? 'negative' : 'positive',
        alert,
        data: points.map(point => Number(point.value)).filter(Number.isFinite)
      }
    },

    buildAlerts() {
      return this.indicatorCards.filter(card => card.alert).map((card, index) => {
        const points = this.findTrend(card.code).dataList || []
        const latest = points[points.length - 1] || {}
        return {
          id: `${card.code}-${index}`,
          alertTime: latest.reportPeriod || '—',
          enterpriseName: this.selectedEnterpriseName,
          indicatorName: card.label,
          currentValue: `${card.value}${card.unit}`,
          threshold: card.alert.replace('超过 ', '').replace('低于 ', '').replace(' 警戒线', ''),
          trend: latest.trendDirection === 'UP' ? '上升' : latest.trendDirection === 'DOWN' ? '下降' : '数据不足',
          riskLevel: ['debtToAssetRatio', 'currentRatio'].includes(card.code) ? '高' : '中',
          status: '待确认'
        }
      })
    },

    drawSparklines() {
      this.indicatorCards.forEach((card, index) => {
        const ref = this.$refs[`sparkline${index}`]
        const canvas = Array.isArray(ref) ? ref[0] : ref
        if (!canvas) return
        const ctx = canvas.getContext('2d')
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        if (card.data.length < 2) return
        const min = Math.min(...card.data)
        const max = Math.max(...card.data)
        const range = max - min || 1
        ctx.beginPath()
        ctx.strokeStyle = card.status === 'positive' ? '#20a96b' : '#e35d6a'
        ctx.lineWidth = 1.5
        card.data.forEach((value, pointIndex) => {
          const x = (pointIndex / (card.data.length - 1)) * canvas.width
          const y = canvas.height - ((value - min) / range) * (canvas.height - 4) - 2
          pointIndex === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
        })
        ctx.stroke()
      })
    },

    handleViewAlert() {
      this.$router.push(`/enterprise/${this.enterpriseId}`)
    },

    async handleMarkProcessed(record) {
      try {
        await this.$confirm('确认已完成该风险的线下核查？', '标记处理', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        record.status = '已处理'
        this.$message.success('已标记为处理')
      } catch (error) {
        // 用户取消。
      }
    }
  }
}
</script>

<style scoped>
.trend-monitor-page {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 4px;
  color: #10212b;
  font-size: 20px;
}

.page-subtitle {
  margin: 0;
  color: #6c7d89;
  font-size: 13px;
}

.filter-bar {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 18px 24px;
  margin-bottom: 18px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-label {
  margin-right: 8px;
  color: #6c7d89;
  font-size: 13px;
}

.insufficient-alert {
  margin-bottom: 18px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.indicator-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.indicator-card {
  position: relative;
  overflow: hidden;
  padding: 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.indicator-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  content: '';
}

.card-positive::before {
  background: #20a96b;
}

.card-negative::before {
  background: #e35d6a;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-label {
  color: #6c7d89;
  font-size: 13px;
}

.card-value {
  margin-bottom: 8px;
  color: #10212b;
  font-size: 26px;
  font-weight: 700;
}

.card-negative .card-value,
.card-trend.is-negative,
.text-danger {
  color: #e35d6a;
}

.card-trend.is-positive {
  color: #20a96b;
}

.card-trend {
  min-height: 18px;
  margin-bottom: 8px;
  font-size: 12px;
}

.card-alert {
  padding: 6px 9px;
  color: #e35d6a;
  font-size: 11px;
  background: rgba(227, 93, 106, 0.07);
  border-radius: 6px;
}

.alert-table-section {
  padding: 24px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  color: #10212b;
  font-size: 16px;
}

@media (max-width: 1200px) {
  .indicator-cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .charts-grid,
  .indicator-cards-grid {
    grid-template-columns: 1fr;
  }
  .filter-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
