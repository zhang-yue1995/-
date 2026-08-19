# 数据库设计文档

## 文档概述
本文档详细描述鑫速录系统的数据库设计，包括ER图说明、表结构定义、字段说明、索引设计和数据类型选择理由。

## 数据库选型

- **数据库**: H2 Database 2.1.214
- **模式**: 内存模式（支持持久化切换）
- **兼容性**: MySQL模式（`MODE=MySQL`）
- **字符集**: UTF-8
- **连接池**: HikariCP（Spring Boot默认）

### 选型理由
1. **开发便利性**: 无需安装，内嵌运行
2. **快速启动**: 内存模式启动速度极快
3. **控制台支持**: Web界面便于调试
4. **生产可替换**: SQL标准兼容，可平滑迁移至MySQL/PostgreSQL

---

## ER图描述

### 核心实体关系

```
┌─────────────┐       ┌───────────────────┐       ┌──────────────┐
│    user     │       │     enterprise    │       │  uploaded_   │
│   (用户)    │1    N │     (企业)        │1    N │    file      │
└──────┬──────┘       └───────┬───────────┘       └──────┬───────┘
       │                     │                          │
       │ created_by          │ archive_id               │ ocr_task_id
       │                     │                          │
       ▼                     ▼                          ▼
┌─────────────────────────────────┐   ┌──────────────┐
│     financial_report_archive    │   │  ocr_task    │
│         (报表档案)              │──▶│ (OCR任务)    │
└──────────┬──────────────────────┘   └──────┬───────┘
           │                                │
           │ file_id                        │ task_id
           │                                │
           ▼                                ▼
┌─────────────────────────┐   ┌──────────────────────┐
│   ocr_field_result      │   │ 三大报表主表          │
│   (OCR识别结果)         │   ├──────────────────────┤
└─────────────────────────┘   │ balance_sheet        │
                              │ income_statement      │
                              │ cash_flow_statement   │
                              └──────────┬───────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
                    ▼                    ▼                    ▼
          ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
          │balance_sheet_   │ │income_statement_│ │cash_flow_       │
          │item             │ │item             │ │statement_item   │
          │(资产负债表明细) │ │(利润表明细)     │ │(现金流量表明细) │
          └─────────────────┘ └─────────────────┘ └─────────────────┘

┌─────────────────┐   ┌──────────────────────┐   ┌──────────────────┐
│ financial_       │   │ financial_indicator_│   │ financial_health │
│ indicator        │◀──▶│ value               │   │ _score           │
│ (指标定义)       │   │ (指标值)            │   │ (健康评分)       │
└─────────────────┘   └──────────────────────┘   └──────────────────┘
                                                      │
┌──────────────────────┐   ┌──────────────────────┐   │
│ field_mapping_rule   │   │ financial_analysis_  │◀──┘
│ (字段映射规则)       │   │ report              │
└──────────────────────┘   │ (分析报告)          │
                            └──────────────────────┘

┌──────────────────────┐   ┌──────────────────────┐
│ historical_indicator │   │ audit_log            │
│ _value               │   │ (审计日志)           │
│ (历史指标值)         │   └──────────────────────┘
└──────────────────────┘
```

### 实体关系说明

#### 一对多关系（1:N）
- **user → enterprise**: 一个用户可以创建多家企业
- **enterprise → financial_report_archive**: 一家企业可以有多个报表档案
- **financial_report_archive → uploaded_file**: 一个档案可以关联多个文件
- **uploaded_file → ocr_task**: 一个文件对应一个OCR任务
- **ocr_task → ocr_field_result**: 一个任务产生多个识别结果
- **三大主表 → 明细表**: 一个主表包含多个明细项

#### 多对一关系（N:1）
- **financial_indicator_value → financial_indicator**: 多个值对应一个指标定义
- **financial_indicator_value → enterprise**: 多个指标值属于一家企业
- **historical_indicator_value → indicator**: 历史值关联指标定义

#### 独立实体
- **user**: 系统用户，独立存在
- **field_mapping_rule**: 映射规则，全局配置
- **audit_log**: 审计日志，记录所有操作
- **financial_indicator**: 指标定义，全局配置

---

## 表结构详细说明

### 1. 用户表 (`user`)

**功能**: 存储系统用户信息（管理员、客户经理等）

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PRIMARY KEY | 用户ID |
| username | VARCHAR(50) | NOT NULL | - | UNIQUE, INDEX | 用户名 |
| password | VARCHAR(255) | NOT NULL | - | - | 密码（BCrypt加密） |
| real_name | VARCHAR(100) | YES | NULL | - | 真实姓名 |
| email | VARCHAR(100) | YES | NULL | - | 邮箱 |
| phone | VARCHAR(20) | YES | NULL | - | 手机号 |
| role | VARCHAR(20) | NOT NULL | 'USER' | CHECK | 角色：ADMIN/USER |
| status | TINYINT | NOT NULL | 1 | INDEX | 状态：0-禁用/1-启用 |
| last_login_time | DATETIME | YES | NULL | - | 最后登录时间 |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除标记 |

**索引**:
- `idx_user_username`: username（唯一索引）
- `idx_user_status`: status（普通索引）

**设计要点**:
- 密码使用BCrypt哈希存储，不可逆
- 支持逻辑删除，保留历史数据
- role字段预留扩展空间

---

### 2. 企业信息表 (`enterprise`)

**功能**: 存储企业基本信息和风险评级

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PRIMARY KEY | 企业ID |
| enterprise_name | VARCHAR(200) | NOT NULL | - | INDEX | 企业名称 |
| enterprise_code | VARCHAR(50) | YES | NULL | UNIQUE | 统一社会信用代码 |
| industry | VARCHAR(100) | YES | NULL | - | 所属行业 |
| legal_person | VARCHAR(100) | YES | NULL | - | 法定代表人 |
| registered_capital | DECIMAL(18,2) | YES | NULL | - | 注册资本（万元） |
| establish_date | DATE | YES | NULL | - | 成立日期 |
| address | VARCHAR(500) | YES | NULL | - | 注册地址 |
| contact_person | VARCHAR(100) | YES | NULL | - | 联系人 |
| contact_phone | VARCHAR(20) | YES | NULL | - | 联系电话 |
| risk_level | VARCHAR(20) | YES | 'NORMAL' | INDEX | 风险等级 |
| health_score | INT | YES | NULL | - | 健康评分(0-100) |
| last_report_date | DATE | YES | NULL | - | 最近报表日期 |
| created_by | BIGINT | YES | NULL | FK→user(id) | 创建人ID |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除 |

**索引**:
- `idx_enterprise_name`: enterprise_name（模糊查询优化）
- `idx_enterprise_code`: enterprise_code（唯一索引）
- `idx_enterprise_risk_level`: risk_level（筛选优化）

**设计要点**:
- enterprise_code为业务唯一键（统一社会信用代码）
- risk_level使用枚举值，便于统计和筛选
- health_score冗余存储最新值，避免实时计算

---

### 3. 财务报表归档表 (`financial_report_archive`)

**功能**: 记录每次上报的报表档案元信息

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PK | 报表档案ID |
| enterprise_id | BIGINT | NOT NULL | - | FK→enterprise | 企业ID |
| report_type | VARCHAR(30) | NOT NULL | - | CHECK | 报表类型 |
| report_period | VARCHAR(20) | NOT NULL | - | INDEX | 报表期间 |
| report_year | INT | NOT NULL | - | - | 年份 |
| report_quarter | INT | YES | NULL | - | 季度(1-4) |
| report_month | INT | YES | NULL | - | 月份(1-12) |
| filing_status | VARCHAR(20) | NOT NULL | 'DRAFT' | INDEX | 归档状态 |
| validation_status | VARCHAR(20) | YES | 'PENDING' | - | 校验状态 |
| total_assets | DECIMAL(18,2) | YES | NULL | - | 总资产（万元） |
| total_liabilities | DECIMAL(18,2) | YES | NULL | - | 总负债（万元） |
| total_equity | DECIMAL(18,2) | YES | NULL | - | 所有者权益（万元） |
| revenue | DECIMAL(18,2) | YES | NULL | - | 营业收入（万元） |
| net_profit | DECIMAL(18,2) | YES | NULL | - | 净利润（万元） |
| operating_cash_flow | DECIMAL(18,2) | YES | NULL | - | 经营现金流（万元） |
| remarks | TEXT | YES | NULL | - | 备注 |
| uploaded_by | BIGINT | YES | NULL | FK→user | 上传人ID |
| reviewed_by | BIGINT | YES | NULL | FK→user | 审核人ID |
| reviewed_time | DATETIME | YES | NULL | - | 审核时间 |
| review_comment | TEXT | YES | NULL | - | 审核意见 |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除 |

**索引**:
- `idx_archive_enterprise_id`: enterprise_id（外键索引）
- `idx_archive_period`: (report_year, report_quarter, report_month)（复合索引，按期间查询）
- `idx_archive_status`: filing_status（状态筛选）

**设计要点**:
- 冗余存储关键财务摘要，加速列表展示
- filing_status实现工作流状态机：DRAFT→PENDING_REVIEW→REVIEWED/APPROVED/REJECTED
- support多种报表周期：年度/季度/月度

---

### 4. 上传文件表 (`uploaded_file`)

**功能**: 记录上传的原始文件信息

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PK | 文件ID |
| archive_id | BIGINT | YES | NULL | FK→archive | 关联档案ID |
| original_filename | VARCHAR(255) | NOT NULL | - | - | 原始文件名 |
| stored_filename | VARCHAR(255) | YES | NULL | - | 存储文件名 |
| file_path | VARCHAR(500) | NOT NULL | - | - | 文件路径 |
| file_size | BIGINT | NOT NULL | - | - | 文件大小(字节) |
| file_type | VARCHAR(50) | NOT NULL | - | CHECK | 类型:PDF/IMAGE/EXCEL |
| mime_type | VARCHAR(100) | YES | NULL | - | MIME类型 |
| md5_hash | VARCHAR(32) | YES | NULL | - | MD5校验值 |
| upload_status | VARCHAR(20) | NOT NULL | 'UPLOADED' | - | 上传状态 |
| ocr_task_id | BIGINT | YES | NULL | FK→ocr_task | OCR任务ID |
| uploaded_by | BIGINT | NOT NULL | - | FK→user | 上传人ID |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除 |

**索引**:
- `idx_file_archive_id`: archive_id（按档案查文件）
- `idx_file_upload_time`: created_time（按时间排序）

**设计要点**:
- md5_hash用于去重和完整性校验
- upload_status跟踪处理进度
- 支持一个档案关联多个文件

---

### 5. OCR任务表 (`ocr_task`)

**功能**: 记录OCR识别任务的执行状态和统计

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PK | 任务ID |
| file_id | BIGINT | NOT NULL | - | FK→file | 文件ID |
| task_status | VARCHAR(20) | NOT NULL | 'PENDING' | INDEX | 任务状态 |
| task_type | VARCHAR(30) | NOT NULL | - | CHECK | 识别类型 |
| provider | VARCHAR(30) | YES | 'mock' | - | OCR服务商 |
| total_fields | INT | YES | 0 | - | 总字段数 |
| recognized_fields | INT | YES | 0 | - | 已识别数 |
| high_confidence_count | INT | YES | 0 | - | 高置信度数(≥0.9) |
| medium_confidence_count | INT | YES | 0 | - | 中置信度数(0.7-0.9) |
| low_confidence_count | INT | YES | 0 | - | 低置信度数(<0.7) |
| average_confidence | DECIMAL(5,4) | YES | NULL | - | 平均置信度 |
| processing_time_ms | INT | YES | NULL | - | 处理耗时(ms) |
| error_message | TEXT | YES | NULL | - | 错误信息 |
| result_summary | TEXT | YES | NULL | - | 结果摘要(JSON) |
| started_at | DATETIME | YES | NULL | - | 开始时间 |
| completed_at | DATETIME | YES | NULL | - | 完成时间 |
| created_by | BIGINT | NOT NULL | - | FK→user | 创建人ID |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除 |

**索引**:
- `idx_ocr_task_file_id`: file_id
- `idx_ocr_task_status`: task_status（状态筛选）

**设计要点**:
- 详细记录识别质量统计，用于评估OCR效果
- 支持多厂商对比（mock/tencent/baidu）
- result_summary存储JSON格式摘要，避免频繁查询明细

---

### 6. OCR识别字段结果表 (`ocr_field_result`)

**功能**: 存储每个字段的OCR识别详情

| 字段名 | 数据类型 | 可空 | 默认值 | 约束 | 说明 |
|-------|---------|------|--------|------|------|
| id | BIGINT | NOT NULL | AUTO_INCREMENT | PK | 记录ID |
| ocr_task_id | BIGINT | NOT NULL | - | FK→ocr_task | OCR任务ID |
| field_name | VARCHAR(100) | NOT NULL | - | - | 字段名称 |
| field_code | VARCHAR(50) | YES | NULL | - | 字段编码 |
| field_value | VARCHAR(500) | YES | NULL | - | 识别原始值 |
| numeric_value | DECIMAL(18,2) | YES | NULL | - | 数值型值(万元) |
| confidence_score | DECIMAL(5,4) | NOT NULL | - | INDEX | 置信度(0-1) |
| confidence_level | VARCHAR(20) | YES | NULL | - | 置信度等级 |
| field_type | VARCHAR(30) | YES | NULL | - | 字段分类 |
| is_reviewed | TINYINT | YES | 0 | INDEX | 是否已复核 |
| reviewed_value | DECIMAL(18,2) | YES | NULL | - | 复核修正值 |
| reviewed_by | BIGINT | YES | NULL | FK→user | 复核人ID |
| reviewed_at | DATETIME | YES | NULL | - | 复核时间 |
| review_comment | VARCHAR(500) | YES | NULL | - | 复核说明 |
| bounding_box | VARCHAR(200) | YES | NULL | - | 识别区域坐标 |
| page_number | INT | YES | 1 | - | 页码 |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | - | 创建时间 |
| updated_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | ON UPDATE | 更新时间 |
| deleted | TINYINT | NOT NULL | 0 | - | 逻辑删除 |

**索引**:
- `idx_ocr_result_task_id`: ocr_task_id
- `idx_ocr_result_confidence`: confidence_score（筛选低置信度）
- `idx_ocr_result_reviewed`: is_reviewed（筛选待复核）

**设计要点**:
- 同时保存原始文本和数值化后的值
- bounding_box支持可视化定位
- review_comment记录修正原因，便于审计

---

### 7-12. 三大报表主表及明细表

#### 7. 资产负债表主表 (`balance_sheet`)

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 主键ID |
| archive_id | BIGINT | NOT NULL, FK | 关联档案 |
| enterprise_id | BIGINT | NOT NULL, FK | 企业ID |
| report_period | VARCHAR(20) | NOT NULL | 报表期间 |
| report_date | DATE | NOT NULL | 报表日期 |
| total_current_assets | DECIMAL(18,2) | YES | 流动资产合计 |
| total_non_current_assets | DECIMAL(18,2) | YES | 非流动资产合计 |
| total_assets | DECIMAL(18,2) | YES | 资产总计 |
| total_current_liabilities | DECIMAL(18,2) | YES | 流动负债合计 |
| total_non_current_liabilities | DECIMAL(18,2) | YES | 非流动负债合计 |
| total_liabilities | DECIMAL(18,2) | YES | 负债合计 |
| total_equity | DECIMAL(18,2) | YES | 所有者权益合计 |
| balance_check_result | VARCHAR(20) | YES | 平衡校验结果 |
| balance_difference | DECIMAL(18,6) | YES | 平衡差额(万元) |
| created_time / updated_time / deleted | - | - | 公共字段 |

#### 8. 资产负债表明细项表 (`balance_sheet_item`)

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 主键ID |
| balance_sheet_id | BIGINT | NOT NULL, FK | 关联主表 |
| item_code | VARCHAR(50) | NOT NULL | 项目编码 |
| item_name | VARCHAR(100) | NOT NULL | 项目名称 |
| item_category | VARCHAR(30) | NOT NULL | 项目分类 |
| ending_balance | DECIMAL(18,2) | YES | 期末余额(万元) |
| beginning_balance | DECIMAL(18,2) | YES | 期初余额(万元) |
| change_amount | DECIMAL(18,2) | YES | 变动金额(万元) |
| change_percentage | DECIMAL(10,4) | YES | 变动百分比(%) |
| sort_order | INT | DEFAULT 0 | 排序号 |
| parent_item_code | VARCHAR(50) | YES | 父级编码 |
| is_total_row | TINYINT | DEFAULT 0 | 是否合计行 |

#### 9. 利润表主表 (`income_statement`)

类似结构，关键字段：
- total_operating_income（营业总收入）
- total_operating_cost（营业总成本）
- operating_profit（营业利润）
- total_profit（利润总额）
- net_profit（净利润）
- crosscheck_result（勾稽校验结果）

#### 10. 利润表明细项表 (`income_statement_item`)

关键字段：
- current_period_amount（本期金额）
- previous_period_amount（上期金额）
- change_amount / change_percentage

#### 11. 现金流量表主表 (`cash_flow_statement`)

关键字段：
- net_cash_flow_operating/investing/financing（三大活动现金流净额）
- net_increase_cash（现金净增加额）
- beginning_cash_balance / ending_cash_balance（期初/期末余额）

#### 12. 现金流量表明细项表 (`cash_flow_statement_item`)

同利润表明细结构

---

### 13. 字段映射规则表 (`field_mapping_rule`)

**功能**: 配置OCR字段到标准字段的映射关系

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 规则ID |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| source_field_name | VARCHAR(100) | NOT NULL, INDEX | 源字段名 |
| target_field_code | VARCHAR(50) | NOT NULL, INDEX | 目标编码 |
| target_field_name | VARCHAR(100) | YES | 目标字段名 |
| report_type | VARCHAR(30) | NOT NULL, INDEX | 适用报表类型 |
| mapping_type | VARCHAR(20) | DEFAULT 'DIRECT' | 映射方式 |
| transformation_rule | VARCHAR(500) | YES | 转换规则 |
| priority | INT | DEFAULT 0 | 优先级 |
| is_active | TINYINT | DEFAULT 1 | 是否启用 |
| match_confidence_threshold | DECIMAL(3,2) | DEFAULT 0.70 | 匹配阈值 |
| description | VARCHAR(500) | YES | 规则描述 |
| created_by / created_time / updated_time / deleted | - | - | 公共字段 |

---

### 14. 财务指标定义表 (`financial_indicator`)

**功能**: 定义所有可计算的财务指标

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 指标ID |
| indicator_code | VARCHAR(50) | NOT NULL, UNIQUE | 指标编码 |
| indicator_name | VARCHAR(100) | NOT NULL | 指标名称 |
| indicator_category | VARCHAR(30) | NOT NULL, INDEX | 指标类别 |
| formula_description | VARCHAR(500) | YES | 公式描述 |
| unit | VARCHAR(20) | DEFAULT '%' | 单位 |
| optimal_range_min | DECIMAL(10,4) | YES | 最优范围下限 |
| optimal_range_max | DECIMAL(10,4) | YES | 最优范围上限 |
| warning_threshold_low | DECIMAL(10,4) | YES | 预警下限 |
| warning_threshold_high | DECIMAL(10,4) | YES | 预警上限 |
| weight | DECIMAL(5,4) | DEFAULT 0.00 | 权重 |
| is_key_indicator | TINYINT | DEFAULT 0 | 是否核心指标 |
| display_order | INT | DEFAULT 0 | 显示顺序 |
| description | TEXT | YES | 详细说明 |
| created_time / updated_time / deleted | - | - | 公共字段 |

**指标类别枚举**:
- PROFITABILITY（盈利能力）
- SOLVENCY（偿债能力）
- OPERATION（运营效率）
- GROWTH（成长能力）
- CASH_FLOW（现金流能力）

---

### 15. 财务指标值表 (`financial_indicator_value`)

**功能**: 存储每个报表的指标计算结果

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 记录ID |
| indicator_id | BIGINT | NOT NULL, FK | 指标定义ID |
| archive_id | BIGINT | NOT NULL, FK | 报表档案ID |
| enterprise_id | BIGINT | NOT NULL, FK | 企业ID |
| report_period | VARCHAR(20) | NOT NULL | 报表期间 |
| indicator_value | DECIMAL(18,4) | YES | 指标值 |
| is_calculable | TINYINT | DEFAULT 1 | 是否可计算 |
| calculation_error | VARCHAR(500) | YES | 计算错误信息 |
| risk_status | VARCHAR(20) | YES | 风险状态 |
| trend_direction | VARCHAR(10) | YES | 趋势方向 |
| trend_change_rate | DECIMAL(10,4) | YES | 趋势变化率(%) |
| comparison_with_industry | VARCHAR(20) | YES | 行业对比 |
| score_contribution | DECIMAL(5,4) | YES | 评分贡献分 |
| calculated_at | DATETIME | YES | 计算时间 |
| created_time / updated_time / deleted | - | - | 公共字段 |

**复合索引**:
- `idx_iv_enterprise_period`: (enterprise_id, report_period)（趋势分析关键索引）

---

### 16. 健康评分表 (`financial_health_score`)

**功能**: 存储企业的五维健康评分结果

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 评分记录ID |
| enterprise_id | BIGINT | NOT NULL, FK | 企业ID |
| archive_id | BIGINT | YES | FK | 报表档案ID |
| report_period | VARCHAR(20) | NOT NULL | 报表期间 |
| overall_score | INT | NOT NULL | 综合得分(0-100) |
| profitability_score | INT | YES | 盈利能力得分 |
| solvency_score | INT | YES | 偿债能力得分 |
| operation_score | INT | YES | 运营能力得分 |
| growth_score | INT | YES | 成长能力得分 |
| cash_flow_score | INT | YES | 现金流能力得分 |
| risk_level | VARCHAR(20) | NOT NULL | 风险等级 |
| rank_percentile | DECIMAL(5,2) | YES | 行业排名百分位 |
| main_risk_factors | TEXT | YES | 主要风险因素(JSON) |
| improvement_suggestions | TEXT | YES | 改进建议(JSON) |
| scoring_model_version | VARCHAR(20) | DEFAULT 'V1.0' | 评分模型版本 |
| created_time / updated_time / deleted | - | - | 公共字段 |

---

### 17. 分析报告表 (`financial_analysis_report`)

**功能**: 存储生成的财务分析报告

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 报告ID |
| archive_id | BIGINT | NOT NULL, FK | 报表档案ID |
| enterprise_id | BIGINT | NOT NULL, FK | 企业ID |
| report_title | VARCHAR(200) | NOT NULL | 报告标题 |
| report_type | VARCHAR(30) | DEFAULT 'COMPREHENSIVE' | 报告类型 |
| executive_summary | TEXT | YES | 执行摘要 |
| financial_overview | TEXT | YES | 财务概况(JSON) |
| profitability_analysis | TEXT | YES | 盈利分析(JSON) |
| solvency_analysis | TEXT | YES | 偿债分析(JSON) |
| operation_analysis | TEXT | YES | 运营分析(JSON) |
| growth_analysis | TEXT | YES | 成长分析(JSON) |
| cash_flow_analysis | TEXT | YES | 现金流分析(JSON) |
| risk_assessment | TEXT | YES | 风险评估(JSON) |
| conclusion_and_suggestion | TEXT | YES | 结论建议(JSON) |
| data_quality_assessment | TEXT | YES | 数据质量评估(JSON) |
| generation_method | VARCHAR(30) | DEFAULT 'RULE_ENGINE' | 生成方式 |
| ai_provider | VARCHAR(30) | YES | AI服务商 |
| word_count | INT | YES | 报告字数 |
| generated_by | BIGINT | YES, FK | 生成人ID |
| generated_at | DATETIME | YES | 生成时间 |
| export_format | VARCHAR(20) | YES | 导出格式 |
| export_file_path | VARCHAR(500) | YES | 导出文件路径 |
| created_time / updated_time / deleted | - | - | 公共字段 |

**设计要点**:
- 分析内容以JSON格式存储，灵活扩展
- 支持多种报告类型和生成方式
- 记录导出信息，便于追溯

---

### 18. 历史指标值表 (`historical_indicator_value`)

**功能**: 存储历史时期的指标值，用于趋势分析

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 历史记录ID |
| indicator_id | BIGINT | NOT NULL, FK | 指标定义ID |
| enterprise_id | BIGINT | NOT NULL, FK | 企业ID |
| report_period | VARCHAR(20) | NOT NULL | 报表期间 |
| report_date | DATE | NOT NULL | 报表日期 |
| indicator_value | DECIMAL(18,4) | YES | 指标值 |
| period_type | VARCHAR(20) | NOT NULL | 周期类型(QUARTERLY/ANNUAL) |
| year_over_year_change | DECIMAL(10,4) | YES | 同比变化率(%) |
| quarter_over_quarter_change | DECIMAL(10,4) | YES | 环比变化率(%) |
| moving_average_4q | DECIMAL(18,4) | YES | 4期移动平均 |
| trend_line | VARCHAR(20) | YES | 趋势线类型 |
| peak_value | DECIMAL(18,4) | YES | 峰值(近12期) |
| trough_value | DECIMAL(18,4) | YES | 谷值(近12期) |
| volatility_index | DECIMAL(10,4) | YES | 波动性指数(标准差) |
| created_time / updated_time / deleted | - | - | 公共字段 |

**索引**:
- `idx_historical_enterprise`: (enterprise_id, report_date)
- `idx_historical_period`: report_period

---

### 19. 审计日志表 (`audit_log`)

**功能**: 记录系统所有关键操作日志

| 字段名 | 数据类型 | 可空 | 说明 |
|-------|---------|------|------|
| id | BIGINT | PK | 日志ID |
| user_id | BIGINT | YES, FK | 操作用户ID |
| username | VARCHAR(50) | YES | 操作用户名 |
| module | VARCHAR(50) | NOT NULL, INDEX | 操作模块 |
| operation | VARCHAR(50) | NOT NULL, INDEX | 操作类型 |
| target_type | VARCHAR(50) | YES | 目标对象类型 |
| target_id | BIGINT | YES | 目标对象ID |
| operation_description | VARCHAR(500) | NOT NULL | 操作描述 |
| request_url | VARCHAR(500) | YES | 请求URL |
| request_method | VARCHAR(10) | YES | 请求方法(GET/POST...) |
| request_params | TEXT | YES | 请求参数(JSON) |
| response_status | INT | YES | 响应状态码 |
| ip_address | VARCHAR(50) | YES | 客户端IP |
| user_agent | VARCHAR(500) | YES | 用户代理 |
| execution_time_ms | INT | YES | 执行耗时(ms) |
| is_success | TINYINT | NOT NULL, DEFAULT 1 | 是否成功 |
| error_message | TEXT | YES | 错误信息 |
| created_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 操作时间 |

**索引**:
- `idx_audit_user`: user_id
- `idx_audit_module`: module
- `idx_audit_operation`: operation
- `idx_audit_time`: created_time（时间范围查询）
- `idx_audit_target`: (target_type, target_id)

---

## 数据类型选择理由

### 为什么使用 BigDecimal？

**场景**: 所有金额、比率、百分比数值

**原因**:

1. **精度保证**:
   - float/double存在浮点数精度丢失问题
   - 示例：`0.1 + 0.2 = 0.30000000000000004`
   - BigDecimal精确表示十进制小数

2. **金融合规要求**:
   - 会计准则要求金额精确到分
   - 银行系统不允许舍入误差累积
   - 审计需要完整的计算链路

3. **比较操作安全**:
   ```java
   // 错误做法
   if (a == b) { ... }  // 可能永远不相等

   // 正确做法
   if (a.compareTo(b) == 0) { ... }  // 精确比较
   ```

4. **可控的舍入模式**:
   ```java
   BigDecimal result = value.setScale(2, RoundingMode.HALF_UP);
   ```

**DECIMAL精度选择**:
- `DECIMAL(18,2)`：金额字段（最大999万亿，精确到分）
- `DECIMAL(10,4)`：比率/百分比（如流动比率0.8333）
- `DECIMAL(5,4)`：置信度（0.0000-1.0000）

### 其他类型选择

| 数据类型 | 使用场景 | 理由 |
|---------|---------|------|
| BIGINT | 主键、外键、数量 | 自增ID，范围足够大 |
| VARCHAR(N) | 名称、代码、描述 | 变长字符串，节省空间 |
| TEXT | 大文本（备注、JSON） | 不定长内容 |
| DATE/DATETIME | 时间戳 | 标准日期类型 |
| TINYINT | 状态标记、布尔值 | 0/1，节省空间 |
| ENUM替代(VARCHAR+CHECK) | 固定选项 | 兼容性好，易扩展 |

---

## 外键约束设计

### 外键关系清单

| 子表 | 外键字段 | 父表 | 引用字段 | 删除策略 |
|-----|---------|------|---------|---------|
| enterprise | created_by | user | id | SET NULL |
| financial_report_archive | enterprise_id | enterprise | id | CASCADE |
| financial_report_archive | uploaded_by | user | id | SET NULL |
| financial_report_archive | reviewed_by | user | id | SET NULL |
| uploaded_file | archive_id | financial_report_archive | id | CASCADE |
| uploaded_file | uploaded_by | user | id | SET NULL |
| ocr_task | file_id | uploaded_file | id | CASCADE |
| ocr_task | created_by | user | id | SET NULL |
| ocr_field_result | ocr_task_id | ocr_task | id | CASCADE |
| ocr_field_result | reviewed_by | user | id | SET NULL |
| balance_sheet | archive_id | financial_report_archive | id | CASCADE |
| balance_sheet | enterprise_id | enterprise | id | CASCADE |
| （其他三大表同理） | - | - | - | - |
| field_mapping_rule | created_by | user | id | SET NULL |
| financial_indicator_value | indicator_id | financial_indicator | id | CASCADE |
| financial_indicator_value | archive_id | financial_report_archive | id | CASCADE |
| financial_indicator_value | enterprise_id | enterprise | id | CASCADE |
| financial_health_score | enterprise_id | enterprise | id | RESTRICT |
| financial_health_score | archive_id | financial_report_archive | id | SET NULL |
| financial_analysis_report | archive_id | financial_report_archive | id | CASCADE |
| financial_analysis_report | enterprise_id | enterprise | id | CASCADE |
| financial_analysis_report | generated_by | user | id | SET NULL |
| historical_indicator_value | indicator_id | financial_indicator | id | CASCADE |
| historical_indicator_value | enterprise_id | enterprise | id | RESTRICT |
| audit_log | user_id | user | id | SET NULL |

### 删除策略说明

- **CASCADE**: 父记录删除时自动删除子记录（适用于强依赖关系）
- **SET NULL**: 父记录删除时子记录的外键置空（保留子记录）
- **RESTRICT**: 如果存在子记录则禁止删除父记录（保护重要数据）

---

## 索引设计原则

### 1. 主键索引
- 所有表的id字段自动创建PRIMARY KEY
- 使用BIGINT自增，避免UUID的性能开销

### 2. 外键索引
- 所有外键字段默认添加索引
- 加速JOIN查询性能

### 3. 业务查询索引
根据常见查询模式设计：

```sql
-- 按企业+期间查询报表（高频）
CREATE INDEX idx_archive_enterprise_period
ON financial_report_archive(enterprise_id, report_year, report_quarter);

-- 按置信度筛选低质量OCR结果
CREATE INDEX idx_ocr_low_confidence
ON ocr_field_result(confidence_score)
WHERE confidence_score < 0.7;

-- 按时间范围查询审计日志
CREATE INDEX idx_audit_time_range
ON audit_log(created_time);
```

### 4. 复合索引原则
- 将等值条件字段放在前面
- 范围查询字段放在后面
- 遵循最左前缀原则

### 5. 覆盖索引
对于只读取索引列的查询，尽量使用覆盖索引：

```sql
CREATE INDEX idx_covering_indicators
ON financial_indicator_value(indicator_id, archive_id, indicator_value);
```

---

## 数据库初始化脚本

### schema.sql
- 创建19张表
- 定义所有约束和索引
- 插入初始序列（如需要）

### data.sql
- 插入演示用户数据（admin, manager）
- 插入3家示例企业
- 插入示例报表数据（每家企业3个期间）
- 插入60+条字段映射规则
- 插入35个财务指标定义
- 插入OCR识别样例数据
- 插入预计算的指标值和健康评分

---

## 性能优化建议

### 查询优化
1. **避免SELECT ***：明确列出需要的字段
2. **合理使用JOIN**：控制在3张表以内
3. **分页查询**：必须使用LIMIT/OFFSET或JPA Pageable
4. **缓存热点数据**：企业列表、指标定义等相对稳定的数据

### 写入优化
1. **批量插入**：使用saveAll()而非循环save()
2. **事务控制**：合理设置事务边界
3. **异步处理**：OCR识别、报告生成等耗时操作异步化

### 存储优化
1. **定期归档**：超过1年的审计日志归档到历史表
2. **清理软删除数据**：定期物理删除deleted=1的记录
3. **监控表大小**：重点关注ocr_field_result和indicator_value表的增长

---

## 数据库迁移指南

从H2迁移到生产数据库（MySQL/PostgreSQL）：

### 1. 修改连接配置
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xinsulu?useSSL=false&serverTimezone=Asia/Shanghai
    username: xinsulu_user
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. 语法差异处理
- H2的AUTO_INCREMENT → MySQL的AUTO_INCREMENT（兼容）
- H2的COMMENT → MySQL的COMMENT（兼容）
- ENGINE=InnoDB → 仅MySQL需要，PostgreSQL忽略

### 3. 数据类型映射
| H2 | MySQL | PostgreSQL |
|----|------|-----------|
| BIGINT | BIGINT | BIGINT |
| DECIMAL(18,2) | DECIMAL(18,2) | NUMERIC(18,2) |
| VARCHAR(N) | VARCHAR(N) | VARCHAR(N) |
| TEXT | TEXT | TEXT |
| DATETIME | DATETIME | TIMESTAMP |
| TINYINT | TINYINT | SMALLINT |

### 4. 数据迁移步骤
1. 导出H2数据：`SCRIPT TO 'backup.sql'`
2. 在目标库执行schema.sql建表
3. 转换并导入data.sql数据
4. 校验数据完整性
5. 切换应用配置并重启

---

## 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|---------|
| v1.0 | 2024-12-18 | 开发团队 | 初始版本，19张表完整设计 |

---

## 相关文档
- [API接口文档](./api.md) - 基于本数据库设计的接口说明
- [字段映射文档](./field-mapping.md) - 字段映射规则详解
- [财务公式文档](./financial-formulas.md) - 指标计算公式与字段来源
