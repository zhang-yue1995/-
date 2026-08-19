<template>
  <div class="enterprise-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">企业与报表列表</h2>
      <p class="page-subtitle">管理所有企业信息及财务报表数据</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button icon="el-icon-download" plain round @click="handleExport">
        导出Excel
      </el-button>
      <el-button type="primary" icon="el-icon-plus" round @click="handleCreate">
        新增企业
      </el-button>
    </div>

    <!-- 筛选工具栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索企业名称/信用代码"
        prefix-icon="el-icon-search"
        clearable
        size="medium"
        class="filter-input"
        @input="handleSearch"
      />
      <el-date-picker
        v-model="filters.period"
        type="month"
        value-format="yyyy-MM"
        placeholder="报表期间"
        clearable
        size="medium"
        class="filter-select"
        @change="handleSearch"
      />
      <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable size="medium" class="filter-select" @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="健康" value="HEALTHY" />
        <el-option label="正常" value="NORMAL" />
        <el-option label="关注" value="ATTENTION" />
        <el-option label="高风险" value="DANGEROUS" />
        <el-option label="严重风险" value="CRITICAL" />
      </el-select>
      <el-select v-model="filters.status" placeholder="数据状态" clearable size="medium" class="filter-select" @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="已入库" value="APPROVED" />
        <el-option label="待复核" value="PENDING_REVIEW" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
    </div>

    <!-- 数据表格 -->
    <DataTable
      :data="tableData"
      :loading="loading"
      :total="total"
      :showIndex="false"
      :showOperations="false"
      rowKey="treeKey"
      :expandRowKeys="expandedKeys"
      :operationsWidth="210"
      :rowClassName="getRowClassName"
      @pagination-change="handlePaginationChange"
      @view="handleView"
      @edit="handleEdit"
      @delete="handleDelete"
    >
      <el-table-column prop="name" label="企业名称" min-width="180" fixed show-overflow-tooltip>
        <template slot-scope="{ row }">
          <a v-if="row.rowType === 'report'" href="javascript:;" class="report-child-link" @click="handleView(row)">报表期 {{ row.latestPeriod }}</a>
          <strong v-else class="enterprise-parent-name">{{ row.name }}</strong>
        </template>
      </el-table-column>

      <el-table-column prop="creditCode" label="统一社会信用代码" width="200" show-overflow-tooltip />

      <el-table-column prop="latestPeriod" label="报表期" width="120" />

      <el-table-column prop="balanceSheetStatus" label="资产负债表" width="120">
        <template slot-scope="{ row }">
          <StatusTag v-if="row.rowType === 'report'" :type="getStatusType(row.balanceSheetStatus)" :text="row.balanceSheetStatus" size="small" />
          <span v-else>—</span>
        </template>
      </el-table-column>

      <el-table-column prop="incomeStatus" label="利润表" width="120">
        <template slot-scope="{ row }">
          <StatusTag v-if="row.rowType === 'report'" :type="getStatusType(row.incomeStatus)" :text="row.incomeStatus" size="small" />
          <span v-else>—</span>
        </template>
      </el-table-column>

      <el-table-column prop="cashFlowStatus" label="现金流量表" width="130">
        <template slot-scope="{ row }">
          <StatusTag v-if="row.rowType === 'report'" :type="getStatusType(row.cashFlowStatus)" :text="row.cashFlowStatus" size="small" />
          <span v-else>—</span>
        </template>
      </el-table-column>

      <el-table-column prop="healthScore" label="健康度" width="100">
        <template slot-scope="{ row }">
          <span v-if="row.healthScore !== null && row.healthScore !== undefined" :class="['health-score', getHealthClass(row.healthScore)]">
            {{ row.healthScore }}分
          </span>
          <span v-else>—</span>
        </template>
      </el-table-column>

      <el-table-column prop="keyRisks" label="关键风险" width="150">
        <template slot-scope="{ row }">
          <div class="risk-tags">
            <el-tag
              v-for="(risk, index) in row.keyRisks"
              :key="index"
              size="mini"
              :type="risk.type === 'high' ? 'danger' : risk.type === 'medium' ? 'warning' : 'success'"
              effect="plain"
              round
            >
              {{ risk.name }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="managerName" label="客户经理" width="100" />

      <el-table-column prop="updatedAt" label="更新时间" width="160" sortable="custom">
        <template slot-scope="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <template slot="operations" slot-scope="{ row }">
        <div class="tree-operation-actions">
          <template v-if="row.rowType === 'enterprise'">
            <el-button type="text" size="small" :disabled="!row.children || !row.children.length" @click.stop="handleView(row)">查看报表</el-button>
            <el-button type="text" size="small" @click.stop="handleEdit(row)">编辑企业</el-button>
            <el-button type="text" size="small" class="danger-btn" @click.stop="handleDelete(row)">删除企业</el-button>
          </template>
          <template v-else>
            <el-button type="text" size="small" @click.stop="handleView(row)">查看</el-button>
            <el-button type="text" size="small" @click.stop="handleEdit(row)">企业信息</el-button>
            <el-button type="text" size="small" class="danger-btn" @click.stop="handleDelete(row)">删除报表</el-button>
          </template>
        </div>
      </template>
    </DataTable>

    <!-- 底部统计栏 -->
    <div class="stats-footer">
      <div class="stats-info">
        共 <strong>{{ totalEnterprises }}</strong> 家企业 · <strong>{{ totalReports }}</strong> 期报表
      </div>
      <div class="stats-cards">
        <div class="stat-card">
          <span class="stat-value">{{ stats.monthlyNewReports }}</span>
          <span class="stat-label">本月新增</span>
        </div>
        <div class="stat-card warning">
          <span class="stat-value">{{ stats.pendingReview }}</span>
          <span class="stat-label">待复核</span>
        </div>
        <div class="stat-card danger">
          <span class="stat-value">{{ stats.highRiskEnterprises }}</span>
          <span class="stat-label">高风险</span>
        </div>
        <div class="stat-card success">
          <span class="stat-value">{{ stats.averageHealthScore }}</span>
          <span class="stat-label">平均健康度</span>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="680px"
      :close-on-click-modal="false"
      custom-class="enterprise-dialog"
      @closed="resetIntakeWizard"
    >
      <el-steps v-if="dialogType === 'create'" :active="dialogStep - 1" finish-status="success" simple class="intake-steps">
        <el-step title="归档信息" />
        <el-step title="上传识别" />
        <el-step title="字段复核" />
        <el-step title="勾稽校验" />
      </el-steps>

      <el-form v-if="dialogType === 'edit' || dialogStep === 1" ref="form" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model.trim="formData.creditCode" placeholder="请输入18位统一社会信用代码" maxlength="18" @blur="checkArchivedEnterprise" />
          <div v-if="duplicateEnterpriseNotice" class="duplicate-enterprise-notice">{{ duplicateEnterpriseNotice }}</div>
        </el-form-item>
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="formData.industry" placeholder="请选择行业" style="width: 100%">
            <el-option label="制造业" value="manufacturing" />
            <el-option label="信息技术" value="it" />
            <el-option label="金融业" value="finance" />
            <el-option label="房地产" value="realestate" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户经理" prop="managerName">
          <el-input v-model="formData.managerName" placeholder="请输入客户经理姓名" />
        </el-form-item>
        <template v-if="dialogType === 'create'">
          <el-form-item label="报表期间" prop="reportPeriod">
            <el-date-picker v-model="formData.reportPeriod" type="month" value-format="yyyy-MM" placeholder="可由OCR识别，也可手工选择" style="width: 100%" />
          </el-form-item>
          <el-form-item label="报表口径" prop="reportType">
            <el-select v-model="formData.reportType" style="width: 100%">
              <el-option label="月报" value="MONTHLY" />
              <el-option label="季报" value="QUARTERLY" />
              <el-option label="年报" value="ANNUAL" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <div v-else-if="dialogStep === 2" v-loading="wizardBusy" class="intake-upload-step">
        <div class="wizard-callout">
          <i class="el-icon-document-add" />
          <div><strong>上传报表原件</strong><p>支持本地图片、PDF、Excel；系统完成识别后才创建企业和报表档案。</p></div>
        </div>
        <input ref="intakeFileInput" class="native-file-input" type="file" multiple accept=".jpg,.jpeg,.png,.pdf,.xls,.xlsx" @change="handleNativeFileSelect">
        <div class="native-upload-zone" @click="openIntakeFilePicker" @dragover.prevent @drop.prevent="handleIntakeFileDrop">
          <i class="el-icon-upload" />
          <div>将文件拖到此处，或<em>点击选择</em></div>
          <p>图片可多选；PDF、Excel 一次选择一个。单个文件不超过 30MB。</p>
        </div>
        <div v-if="selectedIntakeFiles.length" class="selected-file-list">
          <div v-for="(file, index) in selectedIntakeFiles" :key="file.name + index" class="selected-file-item">
            <span><i class="el-icon-document" /> {{ file.name }}</span>
            <el-button type="text" icon="el-icon-close" @click="removeIntakeFile(index)" />
          </div>
        </div>
      </div>

      <div v-else-if="dialogStep === 3" v-loading="wizardBusy" class="intake-review-step">
        <div class="review-summary">
          已识别 <strong>{{ reviewFields.length }}</strong> 个字段；可直接修改金额，确认后执行三张报表的勾稽校验与健康度评分。
        </div>
        <el-table :data="reviewFields" border stripe max-height="440">
          <el-table-column prop="fieldType" label="报表" width="120" />
          <el-table-column prop="fieldName" label="科目" min-width="170" />
          <el-table-column label="本期/期末" min-width="130"><template slot-scope="{ row }"><el-input v-model="row.fieldValue" size="mini" /></template></el-table-column>
          <el-table-column label="上期/年初" min-width="130"><template slot-scope="{ row }"><el-input v-model="row.secondaryValue" size="mini" /></template></el-table-column>
          <el-table-column label="本月" min-width="120"><template slot-scope="{ row }"><el-input v-model="row.tertiaryValue" size="mini" /></template></el-table-column>
        </el-table>
      </div>

      <div v-else v-loading="wizardBusy" class="intake-validation-step">
        <div class="validation-score">
          <span class="score-value">{{ intakeHealth.totalScore == null ? '—' : intakeHealth.totalScore }}</span>
          <span>财务健康度</span>
        </div>
        <div class="validation-list">
          <div v-for="(item, index) in validationResults" :key="index" :class="['validation-row', validationPassed(item) ? 'passed' : 'warning']">
            <i :class="validationPassed(item) ? 'el-icon-circle-check' : 'el-icon-warning-outline'" />
            <div><strong>{{ item.checkName || item.name || item.ruleName || item.validationName || `校验项 ${index + 1}` }}</strong><p>{{ item.detail || item.message || item.description || validationResultText(item) }}</p></div>
          </div>
          <el-empty v-if="!validationResults.length" description="暂无可执行的勾稽校验项" :image-size="70" />
        </div>
      </div>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button v-if="dialogType === 'create' && dialogStep > 1 && dialogStep < 4" @click="dialogStep -= 1">上一步</el-button>
        <el-button v-if="dialogType === 'edit' || dialogStep === 1" type="primary" @click="handleSubmitForm">{{ dialogType === 'create' ? '下一步：上传报表' : '确 定' }}</el-button>
        <el-button v-else-if="dialogStep === 2" type="primary" :loading="wizardBusy" :disabled="!selectedIntakeFiles.length" @click="startIntakeRecognition">上传并识别</el-button>
        <el-button v-else-if="dialogStep === 3" type="primary" :loading="wizardBusy" @click="confirmIntakeReview">确认复核并校验</el-button>
        <el-button v-else type="primary" @click="finishIntake">完成并查看报表</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getEnterpriseList,
  getEnterpriseDetail,
  getEnterpriseByCreditCode,
  updateEnterprise,
  deleteEnterprise
} from '@/api/enterprise'
import { getReportList, getReportDetail, deleteReport, intakeReport, submitReview, getReportValidations } from '@/api/report'
import { uploadReportFile } from '@/api/file'
import { recognizeFile, getTaskResults, mergeOcrTasks } from '@/api/ocr'
import { getHealthScore } from '@/api/indicator'
import { getDashboardStats } from '@/api/dashboard'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatDate } from '@/utils/format'

export default {
  name: 'EnterpriseList',

  components: {
    DataTable,
    StatusTag
  },

  data() {
    return {
      loading: false,
      total: 0,
      totalEnterprises: 0,
      totalReports: 0,
      stats: {
        monthlyNewReports: 0,
        pendingReview: 0,
        highRiskEnterprises: 0,
        averageHealthScore: 0
      },

      // 筛选条件
      filters: {
        keyword: '',
        period: '',
        riskLevel: '',
        status: ''
      },

      // 分页参数
      pagination: {
        page: 1,
        pageSize: 10
      },

      // 表格数据
      tableData: [],
      expandedKeys: [],

      // 对话框相关
      dialogVisible: false,
      dialogType: 'create', // create | edit
      dialogStep: 1,
      wizardBusy: false,
      selectedIntakeFiles: [],
      duplicateEnterpriseNotice: '',
      archivedEnterpriseId: null,
      intakeFileId: null,
      intakeTaskId: null,
      intakeArchiveId: null,
      reviewFields: [],
      validationResults: [],
      intakeHealth: {},
      formData: {
        id: null,
        name: '',
        creditCode: '',
        industry: '',
        managerName: '',
        reportPeriod: '',
        reportType: 'MONTHLY',
        remark: ''
      },
      formRules: {
        name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        creditCode: [
          { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
          { len: 18, message: '统一社会信用代码为18位', trigger: 'blur' }
        ],
        industry: [{ required: true, message: '请选择行业', trigger: 'change' }]
      }
    }
  },

  computed: {
    dialogTitle() {
      return this.dialogType === 'create' ? '新增企业' : '编辑企业'
    }
  },

  watch: {
    '$route.query': {
      deep: true,
      handler() {
        this.applyRouteQuery()
        this.fetchEnterpriseList()
        this.fetchStats()
      }
    }
  },

  created() {
    this.applyRouteQuery()
    this.fetchEnterpriseList()
    this.fetchStats()
  },

  methods: {
    applyRouteQuery() {
      const query = this.$route.query || {}
      this.filters.status = query.status || ''
      this.pagination.page = 1
      this.pagination.pageSize = (query.source === 'monthly' ||
        query.focusReportId || query.focusEnterpriseId) ? 100 : 10
    },

    async fetchEnterpriseList() {
      this.loading = true
      try {
        const enterprisePage = await getEnterpriseList({
          ...this.filters,
          pageNum: this.pagination.page,
          pageSize: this.pagination.pageSize
        })
        const res = await getReportList({
          ...this.filters,
          pageNum: 1,
          pageSize: 10000
        })
        const grouped = (res.list || []).reduce((map, item) => {
          const key = String(item.enterpriseId)
          if (!map[key]) map[key] = []
          map[key].push(item)
          return map
        }, {})
        this.tableData = (enterprisePage.list || []).map(enterprise => {
          const children = (grouped[String(enterprise.id)] || [])
            .sort((a, b) => String(b.reportPeriod || '').localeCompare(String(a.reportPeriod || '')))
            .map(item => {
              const filingStatus = this.getFilingStatusLabel(item.filingStatus)
              return {
                ...item,
                treeKey: `report-${item.archiveId}`,
                rowType: 'report',
                id: item.archiveId,
                name: enterprise.name,
                creditCode: enterprise.creditCode || '—',
                latestPeriod: item.reportPeriod || '—',
                balanceSheetStatus: filingStatus,
                incomeStatus: filingStatus,
                cashFlowStatus: filingStatus,
                healthScore: item.healthScore == null ? null : Number(item.healthScore),
                keyRisks: this.getRiskTags(item.riskLevel),
                managerName: item.managerName || enterprise.managerName || '—',
                updatedAt: item.createdTime
              }
            })
          const parent = {
            ...enterprise,
            treeKey: `enterprise-${enterprise.id}`,
            rowType: 'enterprise',
            enterpriseId: enterprise.id,
            creditCode: enterprise.creditCode || '—',
            latestPeriod: children.length ? children[0].latestPeriod : '暂无在线报表',
            healthScore: enterprise.latestHealthScore == null ? null : Number(enterprise.latestHealthScore),
            keyRisks: this.getRiskTags(enterprise.latestRiskLevel),
            managerName: enterprise.managerName || '—',
            updatedAt: enterprise.createdTime
          }
          if (children.length) parent.children = children
          return parent
        })
        this.total = enterprisePage.total || 0
        this.updateFocusedExpansion()
        this.locateFocusedRow()
      } catch (error) {
        console.error('Failed to fetch report list:', error)
        this.$message.error('获取企业与报表列表失败')
      } finally {
        this.loading = false
      }
    },

    async fetchStats() {
      try {
        const stats = await getDashboardStats()
        this.stats = { ...this.stats, ...stats }
        this.totalEnterprises = stats.totalEnterprises || 0
        this.totalReports = stats.totalReports || 0
      } catch (error) {
        console.error('Failed to fetch summary stats:', error)
      }
    },

    getRiskTags(level) {
      const labels = {
        HEALTHY: { name: '健康', type: 'low' },
        NORMAL: { name: '正常', type: 'low' },
        ATTENTION: { name: '需关注', type: 'medium' },
        DANGEROUS: { name: '高风险', type: 'high' },
        CRITICAL: { name: '严重风险', type: 'high' }
      }
      return labels[level] ? [labels[level]] : []
    },

    getStatusType(status) {
      const map = {
        '已入库': 'success',
        '已复核': 'success',
        '待复核': 'warning',
        '待审核': 'warning',
        '待审批': 'warning',
        '审核中': 'warning',
        '审批中': 'warning',
        '处理中': 'warning',
        '草稿': 'warning',
        '异常': 'danger'
      }
      return map[status] || 'default'
    },

    getFilingStatusLabel(status) {
      const labels = {
        APPROVED: '已入库',
        REVIEWED: '已复核',
        PENDING_REVIEW: '待复核',
        DRAFT: '草稿',
        PROCESSING: '处理中',
        FAILED: '异常'
      }
      return labels[status] || '已入库'
    },

    getHealthClass(score) {
      if (score >= 80) return 'is-good'
      if (score >= 60) return 'is-normal'
      if (score >= 40) return 'is-warning'
      return 'is-danger'
    },

    getRowClassName({ row }) {
      const focusReportId = this.$route.query.focusReportId
      const focusEnterpriseId = this.$route.query.focusEnterpriseId
      const focused = focusReportId
        ? String(row.archiveId) === String(focusReportId)
        : String(row.enterpriseId) === String(focusEnterpriseId || '')
      const isMonthly = row.rowType === 'report' &&
        this.$route.query.source === 'monthly' && this.isCreatedThisMonth(row.updatedAt)
      return focused || isMonthly
        ? 'monthly-focus-row'
        : ''
    },

    isCreatedThisMonth(value) {
      if (!value) return false
      const month = String(value).slice(0, 7)
      const now = new Date()
      const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      return month === currentMonth
    },

    locateFocusedRow() {
      const focusReportId = this.$route.query.focusReportId
      const focusEnterpriseId = this.$route.query.focusEnterpriseId
      const monthlySource = this.$route.query.source === 'monthly'
      if (!focusReportId && !focusEnterpriseId && !monthlySource) return
      this.$nextTick(() => {
        const target = this.$el.querySelector('.monthly-focus-row')
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'center' })
          if (monthlySource) {
            const reportRows = this.tableData.reduce((all, row) => all.concat(row.children || []), [])
            const monthlyRows = reportRows.filter(row => this.isCreatedThisMonth(row.updatedAt))
            const focused = reportRows.find(row => (
              focusReportId
                ? String(row.archiveId) === String(focusReportId)
                : String(row.enterpriseId) === String(focusEnterpriseId)
            ))
            const period = focused ? focused.latestPeriod : this.$route.query.focusPeriod
            const prefix = monthlyRows.length
              ? `已标出本月新增的 ${monthlyRows.length} 份报表`
              : '已定位到新增报表'
            this.$message.success(`${prefix}：${focused ? focused.name : ''} ${period || ''}`.trim())
          }
        }
      })
    },

    updateFocusedExpansion() {
      const query = this.$route.query || {}
      if (query.source === 'monthly' && !query.focusReportId && !query.focusEnterpriseId) {
        this.expandedKeys = this.tableData
          .filter(item => (item.children || []).some(child => this.isCreatedThisMonth(child.updatedAt)))
          .map(item => item.treeKey)
        return
      }
      const parent = this.tableData.find(item => {
        if (query.focusEnterpriseId && String(item.enterpriseId) === String(query.focusEnterpriseId)) return true
        return query.focusReportId && (item.children || []).some(child => String(child.archiveId) === String(query.focusReportId))
      })
      if (parent && !this.expandedKeys.includes(parent.treeKey)) {
        this.expandedKeys = [...this.expandedKeys, parent.treeKey]
      }
    },

    formatDate(date) {
      return formatDate(date)
    },

    handleSearch() {
      this.pagination.page = 1
      this.fetchEnterpriseList()
    },

    handlePaginationChange({ page, pageSize }) {
      this.pagination.page = page
      this.pagination.pageSize = pageSize
      this.fetchEnterpriseList()
    },

    async handleExport() {
      try {
        const result = await getReportList({
          ...this.filters,
          pageNum: 1,
          pageSize: Math.max(this.total, 1)
        })
        const escapeCell = value => {
          const raw = value == null ? '' : String(value)
          const safe = /^[=+\-@]/.test(raw) ? `'${raw}` : raw
          return `"${safe.replace(/"/g, '""')}"`
        }
        const rows = [
          ['企业名称', '统一社会信用代码', '报表期', '填报状态', '健康分', '风险等级', '客户经理', '归档时间'],
          ...(result.list || []).map(item => [
            item.enterpriseName,
            item.enterpriseCreditCode,
            item.reportPeriod,
            this.getFilingStatusLabel(item.filingStatus),
            item.healthScore,
            item.riskLevel,
            item.managerName,
            item.createdTime
          ])
        ]
        const csv = '\uFEFF' + rows.map(row => row.map(escapeCell).join(',')).join('\r\n')
        const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
        const link = document.createElement('a')
        link.href = url
        link.download = `企业报表清单_${new Date().toISOString().slice(0, 10)}.csv`
        link.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (error) {
        this.$message.error('导出失败，请重试')
      }
    },

    handleCreate() {
      this.resetIntakeWizard()
      this.dialogType = 'create'
      this.formData = {
        id: null,
        name: '',
        creditCode: '',
        industry: '',
        managerName: '',
        reportPeriod: '',
        reportType: 'MONTHLY',
        remark: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.form?.clearValidate()
      })
    },

    handleView(row) {
      const target = row.rowType === 'enterprise' ? ((row.children || [])[0]) : row
      if (!target) {
        this.$message.info('该企业暂无在线报表')
        return
      }
      this.$router.push({
        path: `/enterprise/${target.enterpriseId}`,
        query: { reportId: target.archiveId }
      })
    },

    async handleEdit(row) {
      try {
        const enterpriseId = row.enterpriseId || row.id
        const enterprise = await getEnterpriseDetail(enterpriseId)
        this.dialogType = 'edit'
        this.formData = {
          ...enterprise,
          id: enterpriseId,
          remark: enterprise.remark || ''
        }
        this.dialogVisible = true
      } catch (error) {
        this.$message.error('获取企业信息失败')
      }
    },

    async handleDelete(row) {
      try {
        const isEnterprise = row.rowType === 'enterprise'
        if (isEnterprise && row.children && row.children.length) {
          this.$message.warning('该企业仍有在线报表，请先删除全部报表后再删除企业档案')
          return
        }
        const message = isEnterprise
          ? `确定要删除企业档案“${row.name}”吗？`
          : `确定要删除“${row.name}”${row.latestPeriod}期报表吗？`
        await this.$confirm(message, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        if (isEnterprise) {
          await deleteEnterprise(row.enterpriseId)
          this.$message.success('企业档案已删除')
        } else {
          await deleteReport(row.archiveId)
          this.$message.success('该期报表已删除')
        }
        if (this.tableData.length === 1 && this.pagination.page > 1) {
          this.pagination.page -= 1
        }
        this.fetchEnterpriseList()
        this.fetchStats()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error('删除失败，请重试')
        }
      }
    },

    async handleSubmitForm() {
      try {
        await this.$refs.form.validate()

        if (this.dialogType === 'create') {
          this.dialogStep = 2
          this.$message.info('归档信息已暂存，请上传报表原件；此时尚未写入数据库')
          return
        } else {
          await updateEnterprise(this.formData.id, this.formData)
          this.$message.success('更新成功')
        }

        this.dialogVisible = false
        this.fetchEnterpriseList()
      } catch (error) {
        if (error !== false) {
          console.error('Form submit error:', error)
          this.$message.error('操作失败，请重试')
        }
      }
    },

    resetIntakeWizard() {
      this.dialogStep = 1
      this.wizardBusy = false
      this.selectedIntakeFiles = []
      this.duplicateEnterpriseNotice = ''
      this.archivedEnterpriseId = null
      this.intakeFileId = null
      this.intakeTaskId = null
      this.intakeArchiveId = null
      this.reviewFields = []
      this.validationResults = []
      this.intakeHealth = {}
    },

    async checkArchivedEnterprise() {
      const code = String(this.formData.creditCode || '').trim().toUpperCase()
      this.formData.creditCode = code
      this.duplicateEnterpriseNotice = ''
      this.archivedEnterpriseId = null
      if (this.dialogType !== 'create' || code.length !== 18) return
      try {
        const enterprise = await getEnterpriseByCreditCode(code)
        if (enterprise) {
          this.archivedEnterpriseId = enterprise.id
          this.formData.name = enterprise.name
          this.duplicateEnterpriseNotice = '当前企业已归档，无需重复操作'
          this.$message.warning(this.duplicateEnterpriseNotice)
        }
      } catch (error) {
        console.warn('Credit code lookup failed:', error)
      }
    },

    openIntakeFilePicker() {
      this.$refs.intakeFileInput && this.$refs.intakeFileInput.click()
    },

    handleNativeFileSelect(event) {
      this.setIntakeFiles(Array.from(event.target.files || []))
      event.target.value = ''
    },

    handleIntakeFileDrop(event) {
      this.setIntakeFiles(Array.from(event.dataTransfer.files || []))
    },

    setIntakeFiles(files) {
      if (!files.length) return
      if (files.some(file => file.size > 30 * 1024 * 1024)) {
        this.$message.error('单个文件不能超过30MB')
        return
      }
      const nonImages = files.filter(file => !/^image\//.test(file.type) && !/\.(jpg|jpeg|png)$/i.test(file.name))
      if (nonImages.length && files.length > 1) {
        this.$message.warning('PDF、Excel请单文件上传；多选仅支持报表照片')
        return
      }
      this.selectedIntakeFiles = files
    },

    removeIntakeFile(index) {
      this.selectedIntakeFiles.splice(index, 1)
    },

    async startIntakeRecognition() {
      if (!this.selectedIntakeFiles.length) return
      this.wizardBusy = true
      try {
        const uploads = []
        const tasks = []
        for (const file of this.selectedIntakeFiles) {
          const uploaded = await uploadReportFile(file)
          uploads.push(uploaded)
          tasks.push(await recognizeFile(uploaded.id))
        }
        this.intakeFileId = uploads[0].id
        this.intakeTaskId = tasks.length > 1
          ? (await mergeOcrTasks(tasks.map(item => item.id))).id
          : tasks[0].id
        const result = await getTaskResults(this.intakeTaskId)
        this.reviewFields = result.fieldResults || []
        if (!this.formData.reportPeriod && result.reportPeriod) this.formData.reportPeriod = result.reportPeriod
        if (!this.formData.reportPeriod) throw new Error('未识别到报表期，请返回归档信息补充报表期间')
        this.intakeArchiveId = await intakeReport({
          enterprise: this.formData,
          report: {
            fileId: this.intakeFileId,
            ocrTaskId: this.intakeTaskId,
            reportPeriod: this.formData.reportPeriod,
            reportDate: null,
            reportType: this.formData.reportType,
            dataSource: 'OCR_AUTO',
            filingStatus: 'DRAFT',
            managerName: this.formData.managerName,
            remark: this.formData.remark
          }
        })
        this.dialogStep = 3
        this.$message.success('OCR识别完成，请复核字段')
      } catch (error) {
        console.error('Intake recognition failed:', error)
        this.$message.error(error.message || '上传或识别失败，请检查文件后重试')
      } finally {
        this.wizardBusy = false
      }
    },

    async confirmIntakeReview() {
      this.wizardBusy = true
      try {
        const reviews = this.reviewFields.map(field => ({
          fieldResultId: field.id,
          correctedValue: field.fieldValue == null ? '' : String(field.fieldValue),
          correctedSecondaryValue: field.secondaryValue == null ? '' : String(field.secondaryValue),
          correctedTertiaryValue: field.tertiaryValue == null ? '' : String(field.tertiaryValue),
          isConfirmedCorrect: true,
          confidence: Number(field.confidenceScore || 100)
        }))
        await submitReview(this.intakeArchiveId, reviews)
        const [validations, health] = await Promise.all([
          getReportValidations(this.intakeArchiveId),
          getHealthScore(this.intakeArchiveId)
        ])
        this.validationResults = validations || []
        this.intakeHealth = health || {}
        this.dialogStep = 4
      } catch (error) {
        this.$message.error(error.message || '复核或勾稽校验失败')
      } finally {
        this.wizardBusy = false
      }
    },

    validationPassed(item) {
      return item && (item.passed === true || item.status === 'PASSED' || item.result === 'PASS')
    },

    validationResultText(item) {
      return this.validationPassed(item) ? '校验通过' : '存在差异，请在报表详情中复查'
    },

    async finishIntake() {
      const archiveId = this.intakeArchiveId
      this.dialogVisible = false
      await Promise.all([this.fetchEnterpriseList(), this.fetchStats()])
      const detail = await getReportDetail(archiveId)
      this.$router.push({ path: `/enterprise/${detail.enterpriseId}`, query: { reportId: archiveId } })
    }
  }
}
</script>

<style scoped>
.enterprise-list-page {
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

.action-bar .el-button--primary:hover {
  background-color: #0c7d68;
  border-color: #0c7d68;
}

/* 筛选工具栏 */
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

/* 企业链接 */
.enterprise-link {
  color: #3d7cf0;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.enterprise-link:hover {
  color: #0e8f78;
  text-decoration: underline;
}

/* 健康度样式 */
.health-score {
  font-weight: 600;
  font-size: 13px;
}

.health-score.is-good {
  color: #20a96b;
}

.health-score.is-normal {
  color: #f3a83b;
}

.health-score.is-warning {
  color: #f3a83b;
}

.health-score.is-danger {
  color: #e35d6a;
}

/* 风险标签 */
.risk-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.enterprise-list-page >>> .monthly-focus-row > td.el-table__cell {
  background: rgba(14, 143, 120, 0.11) !important;
  box-shadow: inset 0 1px 0 rgba(14, 143, 120, 0.28), inset 0 -1px 0 rgba(14, 143, 120, 0.28);
}

.enterprise-list-page >>> .monthly-focus-row > td.el-table__cell:first-child {
  box-shadow: inset 3px 0 0 #0e8f78, inset 0 1px 0 rgba(14, 143, 120, 0.28), inset 0 -1px 0 rgba(14, 143, 120, 0.28);
}

/* 底部统计栏 */
.stats-footer {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #ffffff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.stats-info {
  font-size: 13px;
  color: #6c7d89;
}

.stats-info strong {
  color: #10212b;
  font-weight: 600;
}

.stats-cards {
  display: flex;
  gap: 24px;
}

.stat-card {
  text-align: center;
  padding: 8px 20px;
  border-radius: 8px;
  background-color: #f8fafb;
}

.stat-card.warning {
  background-color: rgba(243, 168, 59, 0.08);
}

.stat-card.danger {
  background-color: rgba(227, 93, 106, 0.08);
}

.stat-card.success {
  background-color: rgba(32, 169, 107, 0.08);
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #10212b;
  line-height: 1.2;
}

.stat-card.warning .stat-value {
  color: #f3a83b;
}

.stat-card.danger .stat-value {
  color: #e35d6a;
}

.stat-card.success .stat-value {
  color: #20a96b;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: #6c7d89;
  margin-top: 2px;
}

/* 对话框样式覆盖 */
>>> .enterprise-dialog .el-dialog__header {
  border-bottom: 1px solid #eef3f7;
  padding: 20px 24px 16px;
}

>>> .enterprise-dialog .el-dialog__title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
}

>>> .enterprise-dialog .el-dialog__body {
  padding: 24px;
}

>>> .enterprise-dialog .el-dialog__footer {
  border-top: 1px solid #eef3f7;
  padding: 16px 24px;
}

.intake-steps { margin: -8px 0 24px; }
.wizard-callout { display:flex; gap:14px; align-items:center; padding:16px; margin-bottom:20px; border-radius:10px; color:#31525f; background:#eff8f6; }
.wizard-callout > i { font-size:36px; color:#0e8f78; }
.wizard-callout p { margin:5px 0 0; color:#6c7d89; font-size:13px; }
.intake-upload-step { min-height:300px; }
.native-file-input { display: none; }
.native-upload-zone { width:100%; min-height:132px; border:2px dashed #9ccfc2; border-radius:12px; display:flex; flex-direction:column; align-items:center; justify-content:center; cursor:pointer; color:#58717e; background:#f8fcfb; transition:.2s; box-sizing:border-box; }
.native-upload-zone:hover { border-color:#0e8f78; background:#f0faf7; }
.native-upload-zone > i { font-size:34px; color:#0e8f78; margin-bottom:8px; }
.native-upload-zone em { color:#0e8f78; font-style:normal; }
.native-upload-zone p { margin:8px 0 0; font-size:12px; color:#8a9aa3; }
.selected-file-list { margin-top:12px; display:grid; gap:8px; }
.selected-file-item { display:flex; align-items:center; justify-content:space-between; padding:8px 12px; border-radius:8px; background:#f4f8fa; color:#405965; }
.duplicate-enterprise-notice { margin-top:8px; padding:9px 12px; border-radius:8px; color:#a36a00; background:#fff6dc; border:1px solid #f0cb69; font-weight:600; }
.enterprise-parent-name { color:#10212b; }
.report-child-link { color:#168f79; font-weight:600; }
.tree-operation-actions { display:flex; justify-content:space-evenly; align-items:center; gap:12px; width:100%; }
.tree-operation-actions >>> .el-button + .el-button { margin-left:0; }
.review-summary { padding:12px 14px; margin-bottom:12px; color:#526b77; background:#f5f8fa; border-left:3px solid #0e8f78; }
.intake-review-step >>> .el-input__inner { text-align:right; font-variant-numeric:tabular-nums; }
.validation-score { display:flex; flex-direction:column; align-items:center; padding:18px; color:#637782; }
.score-value { color:#0e8f78; font-size:44px; font-weight:700; line-height:1; }
.validation-list { max-height:380px; overflow:auto; }
.validation-row { display:flex; gap:12px; padding:12px 14px; margin-bottom:8px; border-radius:8px; background:#f7fafb; }
.validation-row > i { margin-top:2px; font-size:20px; }
.validation-row.passed > i { color:#20a96b; }
.validation-row.warning > i { color:#f3a83b; }
.validation-row p { margin:4px 0 0; color:#6c7d89; font-size:12px; }
</style>
