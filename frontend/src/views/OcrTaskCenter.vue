<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>OCR任务中心</h2>
        <p>查看识别进度、提供方、置信度与字段结果</p>
      </div>
      <el-button type="primary" icon="el-icon-refresh" @click="fetchTasks">刷新</el-button>
    </div>

    <div class="filter-card">
      <el-select v-model="status" clearable placeholder="任务状态" @change="handleSearch">
        <el-option label="全部状态" value="" />
        <el-option label="等待处理" value="PENDING" />
        <el-option label="识别中" value="PROCESSING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="失败" value="FAILED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
    </div>

    <div class="table-card">
      <DataTable
        :data="filteredTasks"
        :loading="loading"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        :show-index="true"
        empty-text="暂无OCR任务"
        @pagination-change="handlePagination"
      >
        <el-table-column prop="id" label="任务编号" width="110">
          <template slot-scope="{ row }">OCR-{{ String(row.id).padStart(6, '0') }}</template>
        </el-table-column>
        <el-table-column prop="provider" label="识别提供方" width="130" />
        <el-table-column prop="taskStatus" label="状态" width="110">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.taskStatus)" size="small">
              {{ statusText(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recognizedFields" label="识别字段" width="100">
          <template slot-scope="{ row }">{{ row.recognizedFields || 0 }}/{{ row.totalFields || 0 }}</template>
        </el-table-column>
        <el-table-column prop="averageConfidence" label="平均置信度" width="120">
          <template slot-scope="{ row }">
            {{ row.averageConfidence == null ? '—' : `${Number(row.averageConfidence).toFixed(2)}%` }}
          </template>
        </el-table-column>
        <el-table-column prop="lowConfidenceCount" label="低置信度" width="100" />
        <el-table-column prop="processingTimeMs" label="耗时" width="100">
          <template slot-scope="{ row }">
            {{ row.processingTimeMs == null ? '—' : `${row.processingTimeMs} ms` }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="165" />
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              :disabled="row.taskStatus !== 'COMPLETED'"
              @click="showResults(row)"
            >查看结果</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </div>

    <el-dialog title="OCR字段结果" :visible.sync="resultVisible" width="900px">
      <el-table :data="fieldResults" border stripe max-height="520">
        <el-table-column prop="fieldType" label="报表类型" width="170" />
        <el-table-column prop="fieldName" label="字段名称" min-width="160" />
        <el-table-column prop="fieldValue" label="识别值" min-width="140" />
        <el-table-column prop="confidenceScore" label="置信度" width="100">
          <template slot-scope="{ row }">{{ Number(row.confidenceScore || 0).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column prop="confidenceLevel" label="等级" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import DataTable from '@/components/DataTable.vue'
import { getOcrTaskList, getTaskResults } from '@/api/ocr'

export default {
  name: 'OcrTaskCenter',
  components: { DataTable },
  data() {
    return {
      loading: false,
      tasks: [],
      total: 0,
      page: 1,
      pageSize: 10,
      status: '',
      resultVisible: false,
      fieldResults: []
    }
  },
  computed: {
    filteredTasks() {
      return this.status
        ? this.tasks.filter(item => item.taskStatus === this.status)
        : this.tasks
    }
  },
  created() {
    this.fetchTasks()
  },
  methods: {
    async fetchTasks() {
      this.loading = true
      try {
        const result = await getOcrTaskList({ page: this.page, size: this.pageSize })
        this.tasks = result.content || []
        this.total = result.totalElements || 0
      } catch (error) {
        this.$message.error('OCR任务加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.page = 1
      this.fetchTasks()
    },
    handlePagination({ page, pageSize }) {
      this.page = page
      this.pageSize = pageSize
      this.fetchTasks()
    },
    statusText(status) {
      return {
        PENDING: '等待处理',
        PROCESSING: '识别中',
        COMPLETED: '已完成',
        FAILED: '失败',
        CANCELLED: '已取消'
      }[status] || status
    },
    statusType(status) {
      return {
        PENDING: 'warning',
        PROCESSING: 'warning',
        COMPLETED: 'success',
        FAILED: 'danger',
        CANCELLED: 'info'
      }[status] || 'info'
    },
    async showResults(row) {
      const result = await getTaskResults(row.id)
      this.fieldResults = result.fieldResults || []
      this.resultVisible = true
    }
  }
}
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:18px; }
.page-header h2 { margin:0 0 6px; color:#10212b; font-size:24px; }
.page-header p { margin:0; color:#6c7d89; }
.filter-card, .table-card { background:#fff; border-radius:12px; padding:18px; box-shadow:0 4px 16px rgba(27,61,78,.05); }
.filter-card { margin-bottom:16px; }
.filter-card .el-select { width:200px; }
</style>
