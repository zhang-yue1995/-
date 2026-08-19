<template>
  <div class="rule-config-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">指标与规则配置</h2>
      <p class="page-subtitle">管理财务分析指标定义、阈值设置与健康度权重配置</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button icon="el-icon-download" plain round @click="handleDownloadTemplate">
        导入模板
      </el-button>
      <el-button type="primary" icon="el-icon-plus" round @click="handleCreateRule">
        新增规则
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索指标名称/公式"
        prefix-icon="el-icon-search"
        clearable
        size="medium"
        class="filter-input"
        @input="handleSearch"
      />
      <el-select v-model="filters.industry" placeholder="行业类别" clearable size="medium" class="filter-select" @change="handleSearch">
        <el-option label="全部行业" value="" />
        <el-option label="制造业" value="manufacturing" />
        <el-option label="信息技术" value="it" />
        <el-option label="金融业" value="finance" />
        <el-option label="房地产" value="realestate" />
        <el-option label="其他" value="other" />
      </el-select>
      <el-select v-model="filters.scale" placeholder="企业规模" clearable size="medium" class="filter-select" @change="handleSearch">
        <el-option label="全部规模" value="" />
        <el-option label="大型企业" value="large" />
        <el-option label="中型企业" value="medium" />
        <el-option label="小型企业" value="small" />
      </el-select>
      <el-select v-model="filters.status" placeholder="启用状态" clearable size="medium" class="filter-select" @change="handleSearch">
        <el-option label="全部状态" value="" />
        <el-option label="已启用" value="enabled" />
        <el-option label="已禁用" value="disabled" />
      </el-select>
    </div>

    <!-- 规则配置表格 -->
    <DataTable
      :data="ruleList"
      :loading="loading"
      :total="total"
      :showIndex="true"
      :showOperations="true"
      @pagination-change="handlePaginationChange"
      @edit="handleEditRule"
      @delete="handleDeleteRule"
    >
      <el-table-column prop="indicatorName" label="指标名称" min-width="140" show-overflow-tooltip fixed>
        <template slot-scope="{ row }">
          <span class="indicator-name">{{ row.indicatorName }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="formula" label="计算公式" min-width="180" show-overflow-tooltip>
        <template slot-scope="{ row }">
          <code class="formula-code">{{ row.formula }}</code>
        </template>
      </el-table-column>

      <el-table-column prop="normalThreshold" label="正常阈值" width="110" align="center">
        <template slot-scope="{ row }">
          <span class="threshold-tag threshold-normal">{{ row.normalThreshold }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="attentionThreshold" label="关注阈值" width="120" align="center">
        <template slot-scope="{ row }">
          <span class="threshold-tag threshold-warning">{{ row.attentionThreshold }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="highRiskThreshold" label="高风险阈值" width="120" align="center">
        <template slot-scope="{ row }">
          <span class="threshold-tag threshold-danger">{{ row.highRiskThreshold }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="weight" label="权重" width="80" align="center">
        <template slot-scope="{ row }">
          <strong>{{ row.weight }}</strong>
        </template>
      </el-table-column>

      <el-table-column prop="applicableIndustry" label="适用行业" width="100" show-overflow-tooltip />

      <el-table-column prop="status" label="状态" width="90" align="center">
        <template slot-scope="{ row }">
          <el-switch
            v-model="row.isEnabled"
            active-color="#0e8f78"
            inactive-color="#dce6eb"
            @change="handleToggleStatus(row)"
          />
        </template>
      </el-table-column>
    </DataTable>

    <!-- 底部2列区域 -->
    <div class="bottom-grid">
      <!-- 左侧：健康度权重配置 -->
      <div class="weight-config-card">
        <div class="card-header">
          <h3 class="card-title">五维权重配置</h3>
          <el-button type="primary" size="small" round @click="handleSaveWeights">保存权重</el-button>
        </div>

        <div class="weight-items">
          <div v-for="(item, index) in weightConfig" :key="index" class="weight-item">
            <div class="weight-info">
              <span class="weight-label">{{ item.label }}</span>
              <el-input-number v-model="item.value" :min="0" :max="100" :step="1" size="mini" class="weight-input" />
            </div>
            <div class="weight-bar-wrapper">
              <div class="weight-bar" :style="{ width: `${item.value}%`, backgroundColor: item.color }"></div>
              <div class="weight-bar-bg"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：规则治理原则 -->
      <div class="governance-card">
        <div class="card-header">
          <h3 class="card-title">配置说明与治理原则</h3>
        </div>

        <div class="governance-content">
          <ul class="principle-list">
            <li class="principle-item">
              <i class="el-icon-check principle-icon"></i>
              <span>阈值设置应参考<strong>行业标准</strong>和<strong>监管要求</strong>，确保科学性和合规性。</span>
            </li>
            <li class="principle-item">
              <i class="el-icon-check principle-icon"></i>
              <span>权重调整需经<strong>风控部门审批</strong>，并记录变更原因和时间戳。</span>
            </li>
            <li class="principle-item">
              <i class="el-icon-check principle-icon"></i>
              <span>规则变更需保留<strong>版本记录</strong>，支持回溯和审计追踪。</span>
            </li>
            <li class="principle-item">
              <i class="el-icon-check principle-icon"></i>
              <span>定期回顾规则的<strong>适用性和有效性</strong>，至少每季度评估一次。</span>
            </li>
            <li class="principle-item">
              <i class="el-icon-check principle-icon"></i>
              <span>特殊行业可申请<strong>定制化规则集</strong>，但需经过严格评审流程。</span>
            </li>
          </ul>

          <div class="version-info">
            <div class="version-item">
              <label>当前版本：</label>
              <span>v1.0.0</span>
            </div>
            <div class="version-item">
              <label>最后更新：</label>
              <span>{{ lastUpdated || '-' }}</span>
            </div>
            <div class="version-item">
              <label>更新人：</label>
              <span>系统管理员</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑规则对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="680px"
      :close-on-click-modal="false"
      custom-class="rule-dialog"
    >
      <el-form ref="ruleForm" :model="formData" :rules="formRules" label-width="130px">
        <el-form-item label="指标名称" prop="indicatorName">
          <el-input v-model="formData.indicatorName" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="计算公式" prop="formula">
          <el-input v-model="formData.formula" type="textarea" :rows="2" placeholder="请输入计算公式" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="正常阈值" prop="normalThreshold">
              <el-input v-model="formData.normalThreshold" placeholder="如: ≤60%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关注阈值" prop="attentionThreshold">
              <el-input v-model="formData.attentionThreshold" placeholder="如: 60-80%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高风险阈值" prop="highRiskThreshold">
              <el-input v-model="formData.highRiskThreshold" placeholder="如: >80%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="权重" prop="weight">
              <el-input-number v-model="formData.weight" :min="1" :max="50" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用行业" prop="applicableIndustry">
              <el-select v-model="formData.applicableIndustry" placeholder="请选择" style="width: 100%">
                <el-option label="全部行业" value="all" />
                <el-option label="制造业" value="manufacturing" />
                <el-option label="信息技术" value="it" />
                <el-option label="金融业" value="finance" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注说明" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入补充说明" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmitRule">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import DataTable from '@/components/DataTable.vue'
import {
  getIndicatorRules,
  createIndicatorRule,
  updateIndicatorRule,
  deleteIndicatorRule,
  getHealthWeights,
  updateHealthWeights
} from '@/api/rule'

export default {
  name: 'RuleConfig',

  components: {
    DataTable
  },

  data() {
    return {
      loading: false,
      total: 0,
      lastUpdated: '',

      // 筛选条件
      filters: {
        keyword: '',
        industry: '',
        scale: '',
        status: ''
      },

      // 分页参数
      pagination: {
        page: 1,
        pageSize: 10
      },

      // 规则列表数据
      ruleList: [],

      // 对话框相关
      dialogVisible: false,
      dialogType: 'create',
      formData: {
        id: null,
        indicatorName: '',
        formula: '',
        normalThreshold: '',
        attentionThreshold: '',
        highRiskThreshold: '',
        weight: 10,
        applicableIndustry: '',
        remark: ''
      },
      formRules: {
        indicatorName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
        formula: [{ required: true, message: '请输入计算公式', trigger: 'blur' }],
        normalThreshold: [{ required: true, message: '请输入正常阈值', trigger: 'blur' }]
      },

      // 健康度权重配置
      weightConfig: [
        { label: '偿债能力', value: 30, color: '#e35d6a' },
        { label: '盈利能力', value: 25, color: '#f3a83b' },
        { label: '现金流质量', value: 20, color: '#3d7cf0' },
        { label: '运营效率', value: 15, color: '#9dd99e' },
        { label: '成长性', value: 10, color: '#6c7d89' }
      ]
    }
  },

  computed: {
    dialogTitle() {
      return this.dialogType === 'create' ? '新增规则' : '编辑规则'
    }
  },

  created() {
    this.fetchRuleList()
  },

  methods: {
    async fetchRuleList() {
      this.loading = true
      try {
        const [rules, weights] = await Promise.all([
          getIndicatorRules({
            page: this.pagination.page,
            size: this.pagination.pageSize,
            keyword: this.filters.keyword,
            industry: this.filters.industry,
            status: this.filters.status
          }),
          getHealthWeights()
        ])
        this.ruleList = rules.content || []
        this.total = rules.totalElements || 0
        this.weightConfig = (weights || []).map(item => ({
          id: item.id,
          dimensionCode: item.dimensionCode,
          label: item.label,
          value: item.weight,
          color: item.color
        }))
        const dates = this.ruleList.map(item => item.updatedTime).filter(Boolean).sort()
        this.lastUpdated = dates.length ? dates[dates.length - 1] : ''
      } catch (error) {
        console.error('Failed to fetch rule list:', error)
        this.$message.error('获取规则列表失败')
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.pagination.page = 1
      this.fetchRuleList()
    },

    handlePaginationChange({ page, pageSize }) {
      this.pagination.page = page
      this.pagination.pageSize = pageSize
      this.fetchRuleList()
    },

    handleDownloadTemplate() {
      const header = ['指标名称', '计算公式', '正常阈值', '关注阈值', '高风险阈值', '权重', '适用行业']
      const rows = this.ruleList.map(item => [
        item.indicatorName, item.formula, item.normalThreshold, item.attentionThreshold,
        item.highRiskThreshold, item.weight, item.applicableIndustry
      ])
      const csv = '\uFEFF' + [header, ...rows]
        .map(row => row.map(value => `"${String(value == null ? '' : value).replace(/"/g, '""')}"`).join(','))
        .join('\r\n')
      const link = document.createElement('a')
      link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
      link.download = '指标规则模板.csv'
      link.click()
      URL.revokeObjectURL(link.href)
    },

    handleCreateRule() {
      this.dialogType = 'create'
      this.formData = {
        id: null,
        indicatorName: '',
        formula: '',
        normalThreshold: '',
        attentionThreshold: '',
        highRiskThreshold: '',
        weight: 10,
        applicableIndustry: '',
        remark: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.ruleForm?.clearValidate()
      })
    },

    handleEditRule(row) {
      this.dialogType = 'edit'
      this.formData = { ...row }
      this.dialogVisible = true
    },

    handleDeleteRule(row) {
      this.$confirm(`确定要删除规则"${row.indicatorName}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteIndicatorRule(row.id)
        this.$message.success('删除成功')
        this.fetchRuleList()
      }).catch(() => {})
    },

    async handleToggleStatus(row) {
      await updateIndicatorRule(row.id, { isEnabled: row.isEnabled })
      const statusText = row.isEnabled ? '启用' : '禁用'
      this.$message.success(`规则"${row.indicatorName}"已${statusText}`)
    },

    handleSubmitRule() {
      this.$refs.ruleForm.validate((valid) => {
        if (valid) {
          const submit = this.dialogType === 'create'
            ? createIndicatorRule(this.formData)
            : updateIndicatorRule(this.formData.id, this.formData)
          submit.then(() => {
            this.$message.success(this.dialogType === 'create' ? '规则创建成功' : '规则更新成功')
            this.dialogVisible = false
            this.fetchRuleList()
          }).catch(() => {})
        }
      })
    },

    async handleSaveWeights() {
      const total = this.weightConfig.reduce((sum, item) => sum + Number(item.value || 0), 0)
      if (total !== 100) {
        this.$message.error(`当前权重合计为${total}%，必须等于100%`)
        return
      }
      await updateHealthWeights(this.weightConfig.map(item => ({
        id: item.id,
        dimensionCode: item.dimensionCode,
        label: item.label,
        weight: item.value,
        color: item.color
      })))
      this.$message.success('权重配置保存成功')
    }
  }
}
</script>

<style scoped>
.rule-config-page {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #10212b;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 13px;
  color: #6c7d89;
  margin: 0;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-bar .el-button--primary {
  background-color: #0e8f78;
  border-color: #0e8f78;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px 24px;
  background-color: #ffffff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-input {
  flex: 1;
  min-width: 240px;
  max-width: 360px;
}

.filter-select {
  width: 160px;
}

/* 表格样式 */
.indicator-name {
  font-weight: 600;
  color: #10212b;
}

.formula-code {
  font-family: monospace;
  background-color: #f8fafb;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #3d7cf0;
}

.threshold-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.threshold-normal {
  background-color: rgba(32, 169, 107, 0.08);
  color: #20a96b;
}

.threshold-warning {
  background-color: rgba(243, 168, 59, 0.08);
  color: #f3a83b;
}

.threshold-danger {
  background-color: rgba(227, 93, 106, 0.08);
  color: #e35d6a;
}

/* 底部网格 */
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 24px;
}

@media screen and (max-width: 1366px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

/* 权重配置卡片 */
.weight-config-card,
.governance-card {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef3f7;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
  margin: 0;
}

/* 权重项 */
.weight-items {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.weight-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.weight-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.weight-label {
  font-size: 13px;
  color: #3d5563;
  font-weight: 500;
}

.weight-value {
  font-size: 14px;
  font-weight: 700;
  color: #10212b;
}

.weight-bar-wrapper {
  position: relative;
  height: 12px;
  background-color: #eef3f7;
  border-radius: 999px;
  overflow: hidden;
}

.weight-bar {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
  position: relative;
  z-index: 1;
}

.weight-bar-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

/* 治理原则卡片 */
.governance-content {
  line-height: 1.8;
}

.principle-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px 0;
}

.principle-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #3d5563;
  line-height: 1.7;
}

.principle-icon {
  color: #20a96b;
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}

.principle-item strong {
  color: #10212b;
  font-weight: 600;
}

.version-info {
  padding-top: 20px;
  border-top: 1px solid #eef3f7;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.version-item {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.version-item label {
  color: #6c7d89;
  min-width: 80px;
  font-weight: 500;
}

.version-item span {
  color: #10212b;
  font-family: monospace;
}

/* 对话框样式 */
>>> .rule-dialog .el-dialog__header {
  border-bottom: 1px solid #eef3f7;
  padding: 20px 24px 16px;
}

>>> .rule-dialog .el-dialog__title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
}

>>> .rule-dialog .el-dialog__body {
  padding: 24px;
}

>>> .rule-dialog .el-dialog__footer {
  border-top: 1px solid #eef3f7;
  padding: 16px 24px;
}
</style>
