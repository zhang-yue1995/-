# API接口文档

## 文档概述
本文档详细描述鑫速录系统后端提供的所有RESTful API接口，包括请求方法、URL路径、参数格式、响应结构和错误码说明。

## 基本信息

- **基础URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token（JWT）
- **数据格式**: JSON（Content-Type: application/json）
- **字符编码**: UTF-8
- **版本**: v1.0.0

## 通用约定

### 统一响应格式

所有API接口返回统一的响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1702875366000
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|-----|------|------|
| code | Integer | 状态码（200成功，其他失败） |
| message | String | 响应消息 |
| data | Object | 业务数据（可为null） |
| timestamp | Long | 服务器时间戳（毫秒） |

### 分页响应格式

列表查询接口使用统一的分页结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

**分页参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|-------|------|
| page | Integer | 否 | 0 | 页码（从0开始） |
| size | Integer | 否 | 10 | 每页大小（最大100） |
| sort | String | 否 | createdAt,desc | 排序字段（格式：field,direction） |

### 错误码列表

| HTTP状态码 | 业务码 | 说明 | 示例场景 |
|-----------|-------|------|---------|
| 200 | 200 | 成功 | 操作成功 |
| 400 | 400 | 请求参数错误 | 缺少必填参数、参数格式错误 |
| 401 | 401 | 未认证 | Token无效或过期 |
| 403 | 403 | 无权限 | 权限不足 |
| 404 | 404 | 资源不存在 | ID对应的记录不存在 |
| 409 | 409 | 资源冲突 | 唯一键冲突（如用户名重复） |
| 500 | 500 | 服务器内部错误 | 系统异常 |

**错误响应示例**：

```json
{
  "code": 400,
  "message": "参数校验失败: 企业名称不能为空",
  "data": null,
  "timestamp": 1702875366000
}
```

---

## 1. 认证授权模块 (`/api/auth`)

### 1.1 用户登录

**POST** `/api/auth/login`

登录系统获取JWT Token。

**请求参数**:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "role": "ADMIN",
      "email": "admin@xinsulu.com"
    }
  },
  "timestamp": 1702875366000
}
```

**使用Token**: 后续请求需要在Header中携带：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 1.2 用户登出

**POST** `/api/auth/logout`

登出系统，使当前Token失效。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1702875366000
}
```

### 1.3 获取当前用户信息

**GET** `/api/auth/me`

获取当前登录用户的详细信息。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@xinsulu.com",
    "phone": "13800138000",
    "role": "ADMIN",
    "status": 1,
    "lastLoginTime": "2024-12-18 10:30:00",
    "createdTime": "2024-01-01 00:00:00"
  },
  "timestamp": 1702875366000
}
```

---

## 2. 企业管理模块 (`/api/enterprises`)

### 2.1 获取企业列表

**GET** `/api/enterprises`

分页查询企业列表。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| keyword | String | 否 | 搜索关键词（企业名称/信用代码） |
| industry | String | 否 | 行业筛选 |
| riskLevel | String | 否 | 风险等级筛选（LOW/NORMAL/ATTENTION/HIGH/CRITICAL） |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页大小（默认10） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "enterpriseName": "xxx有限公司",
        "enterpriseCode": "91330100MA2XXX",
        "industry": "制造业",
        "legalPerson": "张三",
        "registeredCapital": 5000.00,
        "riskLevel": "HIGH",
        "healthScore": 71,
        "lastReportDate": "2024-09-30",
        "reportCount": 3,
        "createdTime": "2024-03-15 10:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "size": 10,
    "number": 0
  },
  "timestamp": 1702875366000
}
```

### 2.2 获取企业详情

**GET** `/api/enterprises/{id}`

获取单个企业的完整信息。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | Long | 是 | 企业ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "enterpriseName": "xxx有限公司",
    "enterpriseCode": "91330100MA2XXX",
    "industry": "制造业",
    "legalPerson": "张三",
    "registeredCapital": 5000.00,
    "establishDate": "2018-06-20",
    "address": "浙江省杭州市西湖区xxx路xxx号",
    "contactPerson": "李四",
    "contactPhone": "13900139000",
    "riskLevel": "HIGH",
    "healthScore": 71,
    "lastReportDate": "2024-09-30",
    "reportCount": 3,
    "createdBy": {
      "id": 2,
      "username": "manager",
      "realName": "张经理"
    },
    "createdTime": "2024-03-15 10:00:00",
    "updatedTime": "2024-09-30 15:30:00"
  },
  "timestamp": 1702875366000
}
```

### 2.3 新增企业

**POST** `/api/enterprises`

创建新的企业档案。

**请求头**: 需要认证（ADMIN/MANAGER权限）

**请求参数**:

```json
{
  "enterpriseName": "新科技有限公司",
  "enterpriseCode": "91330100MA2YYY",
  "industry": "信息技术",
  "legalPerson": "王五",
  "registeredCapital": 1000.00,
  "establishDate": "2020-01-15",
  "address": "浙江省杭州市滨江区xxx路xxx号",
  "contactPerson": "赵六",
  "contactPhone": "13700137000"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "企业创建成功",
  "data": {
    "id": 4,
    "enterpriseName": "新科技有限公司",
    "enterpriseCode": "91330100MA2YYY",
    "createdTime": "2024-12-18 10:00:00"
  },
  "timestamp": 1702875366000
}
```

### 2.4 更新企业信息

**PUT** `/api/enterprises/{id}`

更新企业基本信息。

**请求头**: 需要认证

**请求参数**: 同新增（所有字段可选，只传需要更新的字段）

**响应示例**:

```json
{
  "code": 200,
  "message": "企业信息更新成功",
  "data": null,
  "timestamp": 1702875366000
}
```

### 2.5 删除企业

**DELETE** `/api/enterprises/{id}`

删除企业（逻辑删除）。

**请求头**: 需要认证（ADMIN权限）

**响应示例**:

```json
{
  "code": 200,
  "message": "企业已删除",
  "data": null,
  "timestamp": 1702875366000
}
```

### 2.6 获取企业的报表历史

**GET** `/api/enterprises/{id}/reports`

获取指定企业的所有报表档案。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | Long | 是 | 企业ID |

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| year | Integer | 否 | 年份筛选 |
| quarter | Integer | 否 | 季度筛选（1-4） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 101,
      "reportPeriod": "2024Q3",
      "reportYear": 2024,
      "reportQuarter": 3,
      "filingStatus": "APPROVED",
      "totalAssets": 50000.00,
      "revenue": 30000.00,
      "netProfit": 1500.00,
      "healthScore": 71,
      "createdTime": "2024-10-15 10:00:00"
    }
  ],
  "timestamp": 1702875366000
}
```

### 2.7 获取企业分析概览

**GET** `/api/enterprises/{id}/overview`

获取企业的综合分析概览（用于工作台展示）。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | Long | 是 | 企业ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enterpriseInfo": { /* 企业基本信息 */ },
    "latestHealthScore": 71,
    "riskTrend": "IMPROVING", // IMPROVING/STABLE/DECLINING
    "recentReports": [ /* 最近3期报表 */ ],
    "keyIndicators": [ /* 关键指标 */ ],
    "riskAlerts": [ /* 风险预警 */ ]
  },
  "timestamp": 1702875366000
}
```

---

## 3. 报表管理模块 (`/api/reports`)

### 3.1 获取报表列表

**GET** `/api/reports`

分页查询报表档案列表。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| enterpriseId | Long | 否 | 企业ID筛选 |
| reportPeriod | String | 否 | 报表期间（如2024Q3） |
| filingStatus | String | 否 | 归档状态筛选 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页大小 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 101,
        "enterpriseId": 1,
        "enterpriseName": "xxx有限公司",
        "reportPeriod": "2024Q3",
        "reportType": "BALANCE_SHEET",
        "filingStatus": "APPROVED",
        "validationStatus": "PASSED",
        "totalAssets": 50000.00,
        "totalLiabilities": 45000.00,
        "revenue": 30000.00,
        "netProfit": 1500.00,
        "uploadedBy": "张经理",
        "reviewedTime": "2024-10-16 14:00:00",
        "createdTime": "2024-10-15 10:00:00"
      }
    ],
    "totalElements": 9,
    "totalPages": 1
  },
  "timestamp": 1702875366000
}
```

### 3.2 获取报表详情

**GET** `/api/reports/{id}`

获取报表的完整详情（含三大表数据）。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| id | Long | 是 | 报表档案ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "archiveInfo": { /* 档案基本信息 */ },
    "balanceSheet": { /* 资产负债表 */ },
    "incomeStatement": { /* 利润表 */ },
    "cashFlowStatement": { /* 现金流量表 */ },
    "ocrResults": [ /* OCR识别结果 */ ],
    "validationResult": { /* 校验结果 */ },
    "indicators": [ /* 财务指标 */ ]
  },
  "timestamp": 1702875366000
}
```

### 3.3 新建报表建档

**POST** `/api/reports/archive`

创建新的报表建档记录。

**请求头**: 需要认证

**请求参数**:

```json
{
  "enterpriseId": 1,
  "reportType": "BALANCE_SHEET",
  "reportPeriod": "2024Q4",
  "reportYear": 2024,
  "reportQuarter": 4,
  "remarks": "第四季度报表"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "报表建档成功",
  "data": {
    "id": 110,
    "enterpriseId": 1,
    "reportPeriod": "2024Q4",
    "filingStatus": "DRAFT",
    "createdTime": "2024-12-18 10:00:00"
  },
  "timestamp": 1702875366000
}
```

### 3.4 提交报表审核

**PUT** `/api/reports/{id}/submit`

提交报表进入审核流程。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "报表已提交审核",
  "data": null,
  "timestamp": 1702875366000
}
```

### 3.5 审核报表

**PUT** `/api/reports/{id}/review`

审核通过或驳回报表。

**请求头**: 需要认证（ADMIN/MANAGER权限）

**请求参数**:

```json
{
  "action": "APPROVE", // APPROVE or REJECT
  "comment": "审核通过，数据质量良好"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "报表审核完成",
  "data": {
    "id": 101,
    "filingStatus": "APPROVED",
    "reviewedBy": "张经理",
    "reviewedTime": "2024-12-18 11:00:00"
  },
  "timestamp": 1702875366000
}
```

### 3.6 删除报表

**DELETE** `/api/reports/{id}`

删除报表档案（逻辑删除）。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "报表已删除",
  "data": null,
  "timestamp": 1702875366000
}
```

---

## 4. 文件上传模块 (`/api/files`)

### 4.1 上传文件

**POST** `/api/files/upload`

上传财务报表图片或PDF文件。

**请求头**:
- `Authorization`: Bearer token
- `Content-Type`: multipart/form-data

**请求参数** (FormData):

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| file | File | 是 | 上传的文件（支持jpg/png/pdf） |
| archiveId | Long | 否 | 关联的报表档案ID |
| fileType | String | 否 | 文件类型（PDF/IMAGE/EXCEL） |

**响应示例**:

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "fileId": 201,
    "originalFilename": "balance_sheet_q3.pdf",
    "storedFilename": "20241218_abc123.pdf",
    "filePath": "/uploads/2024/12/18/20241218_abc123.pdf",
    "fileSize": 2048576,
    "fileType": "PDF",
    "md5Hash": "d41d8cd98f00b204e9800998ecf8427e",
    "uploadStatus": "UPLOADED",
    "uploadTime": "2024-12-18 10:05:00"
  },
  "timestamp": 1702875366000
}
```

**文件限制**:
- 最大文件大小：20MB
- 支持格式：JPG, PNG, PDF
- 图片分辨率建议：≥150 DPI

### 4.2 获取文件信息

**GET** `/api/files/{fileId}`

获取上传文件的详细信息。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| fileId | Long | 是 | 文件ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 201,
    "originalFilename": "balance_sheet_q3.pdf",
    "fileSize": 2048576,
    "fileType": "PDF",
    "uploadStatus": "COMPLETED",
    "ocrTaskId": 301,
    "uploadedBy": "张经理",
    "uploadedAt": "2024-12-18 10:05:00"
  },
  "timestamp": 1702875366000
}
```

### 4.3 下载文件

**GET** `/api/files/{fileId}/download`

下载已上传的文件。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| fileId | Long | 是 | 文件ID |

**响应**: 文件流（application/octet-stream）

---

## 5. OCR识别模块 (`/api/ocr`)

### 5.1 发起OCR识别任务

**POST** `/api/ocr/recognize`

对上传的文件发起OCR识别。

**请求头**: 需要认证

**请求参数**:

```json
{
  "fileId": 201,
  "taskType": "ALL", // BALANCE_SHEET, INCOME_STATEMENT, CASH_FLOW, ALL
  "provider": "mock" // mock, tencent, baidu
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "OCR识别任务已提交",
  "data": {
    "taskId": 301,
    "fileId": 201,
    "taskStatus": "PENDING",
    "taskType": "ALL",
    "provider": "mock",
    "createdAt": "2024-12-18 10:06:00"
  },
  "timestamp": 1702875366000
}
```

### 5.2 查询OCR任务状态

**GET** `/api/ocr/tasks/{taskId}`

查询OCR任务的执行状态和进度。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| taskId | Long | 是 | 任务ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 301,
    "taskId": "TASK_20241218100600",
    "taskStatus": "COMPLETED",
    "taskType": "ALL",
    "provider": "mock",
    "progress": 100,
    "totalFields": 85,
    "recognizedFields": 82,
    "highConfidenceCount": 70,
    "mediumConfidenceCount": 10,
    "lowConfidenceCount": 2,
    "averageConfidence": 0.92,
    "processingTimeMs": 3500,
    "startedAt": "2024-12-18 10:06:01",
    "completedAt": "2024-12-18 10:06:04",
    "resultSummary": "识别完成，2个低置信度字段需复核"
  },
  "timestamp": 1702875366000
}
```

**任务状态枚举**:

| 状态 | 说明 |
|------|------|
| PENDING | 待处理 |
| PROCESSING | 处理中 |
| COMPLETED | 已完成 |
| FAILED | 失败 |

### 5.3 获取OCR识别结果

**GET** `/api/ocr/tasks/{taskId}/results`

获取OCR识别的详细字段结果。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| taskId | Long | 是 | 任务ID |

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| confidenceLevel | String | 否 | 置信度等级筛选（HIGH/MEDIUM/LOW） |
| reviewed | Boolean | 否 | 是否已复核 |
| page | Integer | 否 | 页码 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 401,
        "fieldName": "货币资金",
        "fieldCode": "CASH_AND_CASH_EQUIVALENTS",
        "fieldValue": "12,345.67",
        "numericValue": 12345.67,
        "confidenceScore": 0.98,
        "confidenceLevel": "HIGH",
        "fieldType": "ASSET",
        "isReviewed": true,
        "reviewedValue": 12345.67,
        "boundingBox": "[120, 45, 280, 65]",
        "pageNumber": 1
      }
    ],
    "totalElements": 82,
    "summary": {
      "highConfidence": 70,
      "mediumConfidence": 10,
      "lowConfidence": 2,
      "averageConfidence": 0.92
    }
  },
  "timestamp": 1702875366000
}
```

---

## 6. 字段复核模块 (`/api/report-fields`)

### 6.1 获取待复核字段列表

**GET** `/api/report-fields/pending-review`

获取指定报表中待复核的字段列表（低置信度或未复核）。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| archiveId | Long | 是 | 报表档案ID |
| confidenceThreshold | Double | 否 | 置信度阈值（默认0.7） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 401,
      "fieldName": "货币资金",
      "ocrValue": "12,345.67",
      "numericValue": 12345.67,
      "confidenceScore": 0.65,
      "confidenceLevel": "LOW",
      "suggestedValue": 12345.67,
      "reason": "OCR识别模糊"
    }
  ],
  "timestamp": 1702875366000
}
```

### 6.2 更新单个字段复核值

**PUT** `/api/report-fields/{fieldId}/review`

人工复核并修正单个字段值。

**请求头**: 需要认证

**请求参数**:

```json
{
  "reviewedValue": 13000.00,
  "reviewComment": "经核对原始报表，实际值为13000万元"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "字段复核完成",
  "data": {
    "id": 401,
    "isReviewed": true,
    "reviewedValue": 13000.00,
    "reviewedBy": "张经理",
    "reviewedAt": "2024-12-18 11:00:00"
  },
  "timestamp": 1702875366000
}
```

### 6.3 批量更新字段复核值

**PUT** `/api/report-fields/batch-review`

批量复核多个字段。

**请求头**: 需要认证

**请求参数**:

```json
{
  "reviews": [
    {
      "fieldId": 401,
      "reviewedValue": 13000.00,
      "reviewComment": "修正值"
    },
    {
      "fieldId": 402,
      "reviewedValue": 8500.00,
      "reviewComment": "确认无误"
    }
  ]
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "批量复核完成，共更新2个字段",
  "data": {
    "successCount": 2,
    "failCount": 0,
    "details": []
  },
  "timestamp": 1702875366000
}
```

---

## 7. 三大报表模块 (`/api/financial-statements`)

### 7.1 获取资产负债表

**GET** `/api/financial-statements/balance-sheet/{archiveId}`

获取指定报表档案的资产负债表数据。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| archiveId | Long | 是 | 报表档案ID |

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| includeItems | Boolean | 否 | 是否包含明细项（默认true） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 501,
    "archiveId": 101,
    "enterpriseId": 1,
    "reportPeriod": "2024Q3",
    "reportDate": "2024-09-30",
    "totalCurrentAssets": 25000.00,
    "totalNonCurrentAssets": 25000.00,
    "totalAssets": 50000.00,
    "totalCurrentLiabilities": 30000.00,
    "totalNonCurrentLiabilities": 15000.00,
    "totalLiabilities": 45000.00,
    "totalEquity": 5000.00,
    "balanceCheckResult": "PASSED",
    "balanceDifference": 0.00,
    "items": [
      {
        "itemCode": "CASH_AND_CASH_EQUIVALENTS",
        "itemName": "货币资金",
        "itemCategory": "CURRENT_ASSET",
        "endingBalance": 5000.00,
        "beginningBalance": 4800.00,
        "changeAmount": 200.00,
        "changePercentage": 4.17
      }
      /* ... 更多明细项 */
    ]
  },
  "timestamp": 1702875366000
}
```

### 7.2 获取利润表

**GET** `/api/financial-statements/income-statement/{archiveId}`

获取指定报表档案的利润表数据。

**路径参数**: 同上

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 601,
    "archiveId": 101,
    "reportPeriod": "2024Q3",
    "startDate": "2024-07-01",
    "endDate": "2024-09-30",
    "totalOperatingIncome": 30000.00,
    "totalOperatingCost": 25000.00,
    "operatingProfit": 3500.00,
    "totalProfit": 3200.00,
    "netProfit": 1500.00,
    "netProfitDeducted": 1400.00,
    "crosscheckResult": "PASSED",
    "items": [ /* 利润表明细项 */ ]
  },
  "timestamp": 1702875366000
}
```

### 7.3 获取现金流量表

**GET** `/api/financial-statements/cash-flow/{archiveId}`

获取指定报表档案的现金流量表数据。

**路径参数**: 同上

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 701,
    "archiveId": 101,
    "reportPeriod": "2024Q3",
    "netCashFlowOperating": 2000.00,
    "netCashFlowInvesting": -1500.00,
    "netCashFlowFinancing": 500.00,
    "netIncreaseCash": 1000.00,
    "beginningCashBalance": 4000.00,
    "endingCashBalance": 5000.00,
    "crosscheckResult": "PASSED",
    "items": [ /* 现金流量表明细项 */ ]
  },
  "timestamp": 1702875366000
}
```

---

## 8. 财务指标模块 (`/api/indicators`)

### 8.1 获取指标定义列表

**GET** `/api/indicators/definitions`

获取所有财务指标的定义信息。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| category | String | 否 | 指标类别筛选 |
| isKeyIndicator | Boolean | 否 | 是否为核心指标 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "indicatorCode": "CURRENT_RATIO",
      "indicatorName": "流动比率",
      "indicatorCategory": "SOLVENCY",
      "formulaDescription": "流动资产 / 流动负债",
      "unit": "倍",
      "optimalRangeMin": 1.5,
      "optimalRangeMax": 2.5,
      "weight": 0.08,
      "isKeyIndicator": true
    }
    /* ... 更多指标 */
  ],
  "timestamp": 1702875366000
}
```

### 8.2 计算并获取指标值

**GET** `/api/indicators/values/{archiveId}`

计算并获取指定报表的所有财务指标值。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| archiveId | Long | 是 | 报表档案ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "archiveId": 101,
    "calculatedAt": "2024-12-18 10:10:00",
    "indicators": [
      {
        "indicatorCode": "CURRENT_RATIO",
        "indicatorName": "流动比率",
        "indicatorCategory": "偿债能力",
        "value": 0.83,
        "unit": "倍",
        "riskStatus": "WARNING",
        "trendDirection": "DOWN",
        "trendChangeRate": -5.2,
        "scoreContribution": 6.5
      }
      /* ... 30+个指标 */
    ],
    "summary": {
      "totalIndicators": 35,
      "safeCount": 20,
      "warningCount": 10,
      "dangerCount": 5
    }
  },
  "timestamp": 1702875366000
}
```

### 8.3 获取健康评分

**GET** `/api/indicators/health-score/{archiveId}`

获取指定报表的五维健康评分。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| archiveId | Long | 是 | 报表档案ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "overallScore": 71,
    "dimensions": {
      "profitability": {
        "score": 75,
        "weight": 0.25,
        "label": "盈利能力"
      },
      "solvency": {
        "score": 68,
        "weight": 0.25,
        "label": "偿债能力"
      },
      "operation": {
        "score": 72,
        "weight": 0.20,
        "label": "运营效率"
      },
      "growth": {
        "score": 70,
        "weight": 0.15,
        "label": "成长能力"
      },
      "cashFlow": {
        "score": 69,
        "weight": 0.15,
        "label": "现金流质量"
      }
    },
    "riskLevel": "ATTENTION",
    "rankPercentile": 45.5,
    "mainRiskFactors": ["资产负债率偏高", "流动性紧张"],
    "improvementSuggestions": ["优化负债结构", "加强应收账款回收"],
    "scoringModelVersion": "V1.0"
  },
  "timestamp": 1702875366000
}
```

---

## 9. 分析报告模块 (`/api/analysis-reports`)

### 9.1 生成分析报告

**POST** `/api/analysis-reports/generate`

为指定报表生成财务分析报告。

**请求头**: 需要认证

**请求参数**:

```json
{
  "archiveId": 101,
  "reportType": "COMPREHENSIVE", // COMPREHENSIVE, RISK_ALERT, TREND_ANALYSIS
  "generationMethod": "RULE_ENGINE" // RULE_ENGINE, AI_MODEL
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "分析报告生成成功",
  "data": {
    "reportId": 801,
    "archiveId": 101,
    "reportTitle": "xxx有限公司2024年第三季度财务分析报告",
    "reportType": "COMPREHENSIVE",
    "generationMethod": "RULE_ENGINE",
    "generatedAt": "2024-12-18 10:15:00",
    "wordCount": 3500
  },
  "timestamp": 1702875366000
}
```

### 9.2 获取分析报告详情

**GET** `/api/analysis-reports/{reportId}`

获取完整的分析报告内容。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| reportId | Long | 是 | 报告ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 801,
    "reportTitle": "xxx有限公司2024年第三季度财务分析报告",
    "executiveSummary": "该公司整体财务状况需关注，盈利能力较弱，偿债压力较大...",
    "financialOverview": { /* 财务概况 */ },
    "profitabilityAnalysis": { /* 盈利能力分析 */ },
    "solvencyAnalysis": { /* 偿债能力分析 */ },
    "operationAnalysis": { /* 运营效率分析 */ },
    "growthAnalysis": { /* 成长能力分析 */ },
    "cashFlowAnalysis": { /* 现金流分析 */ },
    "riskAssessment": { /* 风险评估 */ },
    "conclusionAndSuggestion": { /* 结论与建议 */ },
    "generatedAt": "2024-12-18 10:15:00"
  },
  "timestamp": 1702875366000
}
```

### 9.3 导出分析报告

**GET** `/api/analysis-reports/{reportId}/export`

导出分析报告（PDF/Word/Excel格式）。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| reportId | Long | 是 | 报告ID |

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| format | String | 否 | 导出格式（PDF/WORD/EXCEL，默认PDF） |

**响应**: 文件流

---

## 10. 历史趋势模块 (`/api/trends`)

### 10.1 获取单指标趋势

**GET** `/api/trends/single-indicator`

获取单个财务指标的历史趋势数据。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| enterpriseId | Long | 是 | 企业ID |
| indicatorCode | String | 是 | 指标编码（如CURRENT_RATIO） |
| periods | Integer | 否 | 查询期数（默认6，最大12） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "indicatorCode": "CURRENT_RATIO",
    "indicatorName": "流动比率",
    "unit": "倍",
    "enterpriseId": 1,
    "enterpriseName": "xxx有限公司",
    "trendData": [
      {"period": "2024Q1", "value": 0.95, "yoyChange": -3.1},
      {"period": "2024Q2", "value": 0.88, "yoyChange": -7.4},
      {"period": "2024Q3", "value": 0.83, "yoyChange": -5.2},
      {"period": "2024Q4", "value": 0.85, "yoyChange": 2.4}
    ],
    "statistics": {
      "average": 0.88,
      "max": 0.95,
      "min": 0.83,
      "volatility": 0.06,
      "trendLine": "DECLINING"
    }
  },
  "timestamp": 1702875366000
}
```

### 10.2 获取多指标对比趋势

**GET** `/api/trends/multi-indicator`

获取多个指标的对比趋势图数据。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| enterpriseId | Long | 是 | 企业ID |
| indicatorCodes | String | 是 | 多个指标编码，逗号分隔 |
| periods | Integer | 否 | 查询期数 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enterpriseId": 1,
    "periods": ["2024Q1", "2024Q2", "2024Q3"],
    "series": [
      {
        "name": "流动比率",
        "code": "CURRENT_RATIO",
        "unit": "倍",
        "data": [0.95, 0.88, 0.83]
      },
      {
        "name": "速动比率",
        "code": "QUICK_RATIO",
        "unit": "倍",
        "data": [0.75, 0.68, 0.62]
      }
    ]
  },
  "timestamp": 1702875366000
}
```

### 10.3 获取趋势摘要

**GET** `/api/trends/summary/{enterpriseId}`

获取企业整体趋势摘要。

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| enterpriseId | Long | 是 | 企业ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enterpriseId": 1,
    "overallTrend": "DECLINING", // IMPROVING, STABLE, DECLINING, FLUCTUATING
    "keyFindings": [
      "盈利能力连续3期下滑",
      "偿债压力持续加大",
      "现金流状况略有改善"
    ],
    "topImprovingIndicators": [
      {"code": "OPERATING_CASH_FLOW_RATIO", "name": "现金流量比率", "change": "+5.2%"}
    ],
    "topDecliningIndicators": [
      {"code": "CURRENT_RATIO", "name": "流动比率", "change": "-12.6%"}
    ],
    "alerts": [
      {"level": "WARNING", "message": "流动比率低于警戒线1.0"}
    ]
  },
  "timestamp": 1702875366000
}
```

---

## 11. 映射规则模块 (`/api/mapping-rules`)

### 11.1 获取映射规则列表

**GET** `/api/mapping-rules`

分页查询字段映射规则。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| reportType | String | 否 | 报表类型筛选 |
| isActive | Boolean | 否 | 是否启用 |
| keyword | String | 否 | 关键词搜索 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页大小 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "ruleName": "货币资金映射",
        "sourceFieldName": "货币资金",
        "targetFieldCode": "CASH_AND_CASH_EQUIVALENTS",
        "targetFieldName": "货币资金",
        "reportType": "BALANCE_SHEET",
        "mappingType": "DIRECT",
        "transformationRule": "元→万元",
        "priority": 100,
        "isActive": true,
        "matchConfidenceThreshold": 0.70
      }
    ],
    "totalElements": 60,
    "totalPages": 6
  },
  "timestamp": 1702875366000
}
```

### 11.2 创建映射规则

**POST** `/api/mapping-rules`

创建新的字段映射规则。

**请求头**: 需要认证（ADMIN权限）

**请求参数**:

```json
{
  "ruleName": "新规则",
  "sourceFieldName": "测试字段",
  "targetFieldCode": "TEST_FIELD",
  "targetFieldName": "测试字段",
  "reportType": "BALANCE_SHEET",
  "mappingType": "DIRECT",
  "priority": 50
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "映射规则创建成功",
  "data": {
    "id": 61,
    "ruleName": "新规则",
    "createdTime": "2024-12-18 10:20:00"
  },
  "timestamp": 1702875366000
}
```

### 11.3 更新映射规则

**PUT** `/api/mapping-rules/{ruleId}`

更新映射规则配置。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "映射规则更新成功",
  "data": null,
  "timestamp": 1702875366000
}
```

### 11.4 删除映射规则

**DELETE** `/api/mapping-rules/{ruleId}`

删除映射规则（逻辑删除）。

**请求头**: 需要认证（ADMIN权限）

**响应示例**:

```json
{
  "code": 200,
  "message": "映射规则已删除",
  "data": null,
  "timestamp": 1702875366000
}
```

### 11.5 批量导入映射规则

**POST** `/api/mapping-rules/import`

批量导入映射规则（Excel/CSV格式）。

**请求头**:
- Authorization: Bearer token
- Content-Type: multipart/form-data

**请求参数** (FormData):

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| file | File | 是 | 规则文件（Excel/CSV） |

**响应示例**:

```json
{
  "code": 200,
  "message": "导入成功",
  "data": {
    "totalCount": 50,
    "successCount": 48,
    "failCount": 2,
    "errors": [
      {"row": 15, "reason": "目标编码重复"}
    ]
  },
  "timestamp": 1702875366000
}
```

---

## 12. 工作台统计模块 (`/api/dashboard`)

### 12.1 获取KPI统计数据

**GET** `/api/dashboard/kpi`

获取工作台KPI统计卡片数据。

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalEnterprises": 156,
    "newEnterprisesThisMonth": 12,
    "totalReports": 423,
    "reportsThisMonth": 38,
    "pendingReviewReports": 5,
    "averageHealthScore": 72.5,
    "highRiskEnterpriseCount": 23,
    "ocrProcessingToday": 15
  },
  "timestamp": 1702875366000
}
```

### 12.2 获取处理量趋势

**GET** `/api/dashboard/trend`

获取近期的报表处理量趋势数据。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| days | Integer | 否 | 天数（默认30，最大90） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["12-01", "12-02", ..., "12-18"],
    "uploadCount": [5, 8, 3, ...],
    "ocrCount": [4, 7, 3, ...],
    "reviewCount": [3, 6, 2, ...],
    "approveCount": [2, 5, 2, ...]
  },
  "timestamp": 1702875366000
}
```

### 12.3 获取风险分布数据

**GET** `/api/dashboard/risk-distribution`

获取企业风险等级分布情况。

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "distribution": [
      {"level": "LOW", "label": "健康", "count": 45, "percentage": 28.8},
      {"level": "NORMAL", "label": "基本健康", "count": 52, "percentage": 33.3},
      {"level": "ATTENTION", "label": "需关注", "count": 35, "percentage": 22.4},
      {"level": "HIGH", "label": "高风险", "count": 18, "percentage": 11.5},
      {"level": "CRITICAL", "label": "严重风险", "count": 6, "percentage": 3.8}
    ]
  },
  "timestamp": 1702875366000
}
```

### 12.4 获取待办任务列表

**GET** `/api/dashboard/todo-list`

获取当前用户的待办任务。

**请求头**: 需要认证

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "REVIEW",
      "title": "xxx有限公司2024Q3报表待复核",
      "enterpriseName": "xxx有限公司",
      "priority": "HIGH",
      "createdAt": "2024-12-17 16:00:00",
      "deadline": "2024-12-19 18:00:00"
    }
  ],
  "timestamp": 1702875366000
}
```

---

## 13. 审计日志模块 (`/api/audit-logs`)

### 13.1 查询审计日志

**GET** `/api/audit-logs`

分页查询操作审计日志。

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| userId | Long | 否 | 用户ID筛选 |
| module | String | 否 | 模块筛选（AUTH/ENTERPRISE/REPORT等） |
| operation | String | 否 | 操作类型筛选（CREATE/READ/UPDATE/DELETE等） |
| startTime | String | 否 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页大小 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 9001,
        "userId": 2,
        "username": "manager",
        "module": "REPORT",
        "operation": "REVIEW",
        "targetType": "FinancialReportArchive",
        "targetId": 101,
        "operationDescription": "复核报表 xxx有限公司2024Q3",
        "requestUrl": "/api/report-fields/batch-review",
        "requestMethod": "PUT",
        "ipAddress": "192.168.1.100",
        "executionTimeMs": 256,
        "isSuccess": true,
        "createdTime": "2024-12-18 11:00:00"
      }
    ],
    "totalElements": 1000,
    "totalPages": 100
  },
  "timestamp": 1702875366000
}
```

### 13.2 导出审计日志

**GET** `/api/audit-logs/export`

导出审计日志（Excel格式）。

**查询参数**: 同查询接口

**响应**: Excel文件流

---

## 接口调用示例（cURL）

### 登录获取Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 查询企业列表

```bash
curl http://localhost:8080/api/enterprises?page=0&size=10 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 上传文件并发起OCR

```bash
# 1. 上传文件
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@/path/to/balance_sheet.pdf"

# 2. 发起OCR识别
curl -X POST http://localhost:8080/api/ocr/recognize \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fileId":201,"taskType":"ALL"}'
```

---

## 接口权限矩阵

| 接口模块 | ADMIN | MANAGER | USER | 未登录 |
|---------|-------|---------|------|--------|
| /auth/* | ✓ | ✓ | ✓ | ✓ |
| /enterprises GET | ✓ | ✓ | ✗ | ✗ |
| /enterprises POST | ✓ | ✓ | ✗ | ✗ |
| /enterprises PUT/DELETE | ✓ | ✗ | ✗ | ✗ |
| /files/upload | ✓ | ✓ | ✗ | ✗ |
| /ocr/* | ✓ | ✓ | ✗ | ✗ |
| /report-fields/* | ✓ | ✓ | ✗ | ✗ |
| /financial-statements GET | ✓ | ✓ | ✓ | ✗ |
| /indicators GET | ✓ | ✓ | ✓ | ✗ |
| /analysis-reports/* | ✓ | ✓ | ✗ | ✗ |
| /trends GET | ✓ | ✓ | ✓ | ✗ |
| /mapping-rules CRUD | ✓ | ✗ | ✗ | ✗ |
| /dashboard/* | ✓ | ✓ | ✗ | ✗ |
| /audit-logs GET | ✓ | ✓ | ✗ | ✗ |

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.0 | 2024-12-18 | 初始版本，涵盖13个模块、40+个接口 |

---

## 相关文档
- [字段映射文档](./field-mapping.md) - OCR字段映射规则
- [数据库设计文档](./database-design.md) - 数据模型说明
- [财务公式文档](./financial-formulas.md) - 指标计算公式
