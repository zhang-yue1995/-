<template>
  <div class="data-table-wrapper">
    <el-table
      ref="table"
      v-loading="loading"
      :data="tableData"
      :border="border"
      :stripe="stripe"
      :size="size"
      :height="height"
      :max-height="maxHeight"
      :row-key="rowKey"
      :tree-props="treeProps"
      :expand-row-keys="expandRowKeys"
      :row-class-name="rowClassName"
      :default-sort="defaultSort"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
      @row-click="handleRowClick"
      style="width: 100%"
    >
      <!-- 选择列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="50"
        align="center"
        fixed
      />

      <!-- 序号列 -->
      <el-table-column
        v-if="showIndex"
        type="index"
        label="序号"
        width="60"
        align="center"
        :index="indexMethod"
      />

      <!-- 动态列 -->
      <slot />

      <!-- 操作列 -->
      <el-table-column
        v-if="$scopedSlots.operations || showOperations"
        label="操作"
        :width="operationsWidth"
        align="center"
        fixed="right"
      >
        <template slot-scope="scope">
          <slot name="operations" :row="scope.row" :$index="scope.$index" />
          <template v-if="!$scopedSlots.operations && showOperations">
            <div class="operation-actions">
              <el-button type="text" size="small" @click.stop="handleView(scope.row)">查看</el-button>
              <el-button type="text" size="small" @click.stop="handleEdit(scope.row)">编辑</el-button>
              <el-popconfirm
                title="确定删除吗？"
                @confirm="handleDelete(scope.row)"
                confirmButtonText="确定"
                cancelButtonText="取消"
              >
                <el-button slot="reference" type="text" size="small" class="danger-btn">删除</el-button>
              </el-popconfirm>
            </div>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination && total > 0" class="pagination-wrapper">
      <div class="pagination-info">
        共 {{ total }} 条数据
      </div>
      <el-pagination
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && tableData.length === 0" class="empty-state">
      <i class="el-icon-folder-opened empty-icon"></i>
      <p class="empty-text">{{ emptyText }}</p>
      <el-button v-if="showEmptyAction" type="primary" size="small" @click="$emit('empty-action')">
        {{ emptyActionText }}
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DataTable',

  props: {
    // 数据相关
    data: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    total: {
      type: Number,
      default: 0
    },

    // 表格配置
    border: {
      type: Boolean,
      default: true
    },
    stripe: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'medium'
    },
    height: {
      type: [String, Number],
      default: null
    },
    maxHeight: {
      type: [String, Number],
      default: null
    },
    rowKey: {
      type: [String, Function],
      default: 'id'
    },
    treeProps: {
      type: Object,
      default: () => ({ children: 'children', hasChildren: 'hasChildren' })
    },
    expandRowKeys: {
      type: Array,
      default: () => []
    },
    rowClassName: {
      type: [Function, String],
      default: ''
    },
    defaultSort: {
      type: Object,
      default: () => ({})
    },
    emptyText: {
      type: String,
      default: '暂无数据'
    },

    // 列配置
    showSelection: {
      type: Boolean,
      default: false
    },
    showIndex: {
      type: Boolean,
      default: false
    },
    showOperations: {
      type: Boolean,
      default: false
    },
    operationsWidth: {
      type: [String, Number],
      default: 200
    },

    // 分页配置
    showPagination: {
      type: Boolean,
      default: true
    },

    // 空状态配置
    showEmptyAction: {
      type: Boolean,
      default: false
    },
    emptyActionText: {
      type: String,
      default: '添加数据'
    }
  },

  data() {
    return {
      currentPage: 1,
      pageSize: 10,
      tableData: this.data
    }
  },

  watch: {
    data: {
      handler(newVal) {
        this.tableData = newVal
      },
      immediate: true,
      deep: true
    }
  },

  methods: {
    indexMethod(index) {
      return (this.currentPage - 1) * this.pageSize + index + 1
    },

    handleSelectionChange(selection) {
      this.$emit('selection-change', selection)
    },

    handleSortChange({ column, prop, order }) {
      this.$emit('sort-change', { prop, order })
    },

    handleRowClick(row, column, event) {
      this.$emit('row-click', row, event)
    },

    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.$emit('pagination-change', {
        page: this.currentPage,
        pageSize: this.pageSize
      })
    },

    handleCurrentChange(val) {
      this.currentPage = val
      this.$emit('pagination-change', {
        page: this.currentPage,
        pageSize: this.pageSize
      })
    },

    handleView(row) {
      this.$emit('view', row)
    },

    handleEdit(row) {
      this.$emit('edit', row)
    },

    handleDelete(row) {
      this.$emit('delete', row)
    },

    // 公开方法：刷新表格
    refresh() {
      this.$refs.table?.doLayout()
    },

    // 公开方法：清空选择
    clearSelection() {
      this.$refs.table?.clearSelection()
    }
  }
}
</script>

<style scoped>
.data-table-wrapper {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

/* 表格样式优化 */
.data-table-wrapper >>> .el-table {
  font-size: 13px;
}

.data-table-wrapper >>> .el-table th.el-table__cell {
  background-color: #f8fafb !important;
  color: #10212b;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 0;
}

.data-table-wrapper >>> .el-table td.el-table__cell {
  padding: 10px 0;
  color: #3d5563;
}

.data-table-wrapper >>> .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background-color: #f8fafb;
}

.data-table-wrapper >>> .el-table__body tr:hover > td.el-table__cell {
  background-color: rgba(14, 143, 120, 0.04);
}

.operation-actions {
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  gap: 14px;
  width: 100%;
}

.operation-actions >>> .el-button + .el-button {
  margin-left: 0;
}

/* 危险按钮颜色 */
.danger-btn {
  color: #e35d6a !important;
}

.danger-btn:hover {
  color: #f07a85 !important;
}

/* 分页样式 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.pagination-info {
  font-size: 13px;
  color: #6c7d89;
}

.pagination-wrapper >>> .el-pagination {
  text-align: right;
}

/* 空状态 */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #dce6eb;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #6c7d89;
  margin-bottom: 16px;
}
</style>
