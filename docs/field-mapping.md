# 字段映射文档

## 文档概述
本文档详细说明鑫速录系统中OCR识别结果到标准数据库字段的映射规则，包括四层映射架构、三大报表字段映射表、不同会计准则差异处理等内容。

## 四层映射架构

### 架构设计理念
鑫速录采用**四层映射架构**，确保从OCR原始识别到最终入库的准确性和可追溯性：

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Layer 1:   │ -> │  Layer 2:   │ -> │  Layer 3:   │ -> │  Layer 4:   │
│ OCR原始值   │    │ 别名标准化  │    │ 标准编码    │    │ 数据库存储  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
     "货币资金"      "货币资金"       CASH_AND_         DECIMAL(18,2)
     "货币资⾦"                       CASH_EQUIVALENTS  (万元)
     "现金及等价物"
```

### 各层详细说明

#### Layer 1：OCR原始层（Raw OCR）
- **来源**：OCR引擎直接输出
- **特点**：
  - 可能包含识别错误（如"资⾦"代替"资金"）
  - 不同字体/排版导致同一字段多种写法
  - 包含原始坐标信息（bounding_box）
  - 保留置信度评分
- **示例**：
  ```
  原始字段名: "货币资⾦"
  识别值: "12,345.67"
  置信度: 0.95
  坐标: [120, 45, 280, 65]
  ```

#### Layer 2：别名标准化层（Alias Normalization）
- **处理内容**：
  - Unicode字符规范化（全角/半角转换）
  - 异体字/繁简转换
  - 同义词合并
  - 缩写展开
- **规则示例**：

| 原始文本 | 标准化后 | 转换类型 |
|---------|---------|---------|
| 货币资⾦ | 货币资金 | Unicode修复 |
| 应收帐款 | 应收账款 | 错别字纠正 |
| 存货 | 存货 | 无变化 |
| 流动资产合计 | 流动资产合计 | 无变化 |
| 营业总收入(元) | 营业总收入 | 单位剥离 |
| 一年内到期的非流动资产 | 一年内到期的非流动资产 | 无变化 |

#### Layer 3：标准编码层（Standard Code）
- **编码规范**：采用大写下划线命名（UPPER_SNAKE_CASE）
- **分类体系**：按财务报表类型分组
- **示例**：
  ```
  资产负债表:
  - CASH_AND_CASH_EQUIVALENTS (货币资金)
  - ACCOUNTS_RECEIVABLE (应收账款)
  - INVENTORY (存货)
  - TOTAL_CURRENT_ASSETS (流动资产合计)

  利润表:
  - OPERATING_REVENUE (营业收入)
  - OPERATING_COST (营业成本)
  - NET_PROFIT (净利润)

  现金流量表:
  - NET_CASH_FROM_OPERATING_ACTIVITIES (经营活动现金流净额)
  ```

#### Layer 4：数据库存储层（Database Storage）
- **数据类型**：统一使用 DECIMAL(18,2) 存储金额（单位：万元）
- **精度处理**：保留两位小数，四舍五入
- **索引策略**：按企业ID+期间建立复合索引
- **审计字段**：created_time, updated_time, deleted（逻辑删除）

---

## 三大报表字段映射表

### 1. 资产负债表字段映射（Balance Sheet）

#### 流动资产类（Current Assets）

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 1 | 货币资金 / 货币资⾦ / 现金及现金等价物 | 货币资金 | CASH_AND_CASH_EQUIVALENTS | ending_balance | CURRENT_ASSET | 元→万元 |
| 2 | 应收票据 | 应收票据 | NOTES_RECEIVABLE | ending_balance | CURRENT_ASSET | 元→万元 |
| 3 | 应收账款 / 应收帐款 | 应收账款 | ACCOUNTS_RECEIVABLE | ending_balance | CURRENT_ASSET | 元→万元 |
| 4 | 预付款项 | 预付款项 | PREPAYMENTS | ending_balance | CURRENT_ASSET | 元→万元 |
| 5 | 其他应收款 | 其他应收款 | OTHER_RECEIVABLES | ending_balance | CURRENT_ASSET | 元→万元 |
| 6 | 存货 | 存货 | INVENTORY | ending_balance | CURRENT_ASSET | 元→万元 |
| 7 | 合同资产 | 合同资产 | CONTRACT_ASSETS | ending_balance | CURRENT_ASSET | 元→万元 |
| 8 | 持有待售资产 | 持有待售资产 | ASSETS_HELD_FOR_SALE | ending_balance | CURRENT_ASSET | 元→万元 |
| 9 | 一年内到期的非流动资产 | 一年内到期的非流动资产 | NON_CURRENT_ASSETS_DUE_WITHIN_ONE_YEAR | ending_balance | CURRENT_ASSET | 元→万元 |
| 10 | 其他流动资产 | 其他流动资产 | OTHER_CURRENT_ASSETS | ending_balance | CURRENT_ASSET | 元→万元 |
| 11 | 流动资产合计 | 流动资产合计 | TOTAL_CURRENT_ASSETS | ending_balance | CURRENT_ASSET | 元→万元 |

#### 非流动资产类（Non-current Assets）

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 12 | 债权投资 | 债权投资 | DEBT_INVESTMENTS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 13 | 其他债权投资 | 其他债权投资 | OTHER_DEBT_INVESTMENTS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 14 | 长期应收款 | 长期应收款 | LONG_TERM_RECEIVABLES | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 15 | 长期股权投资 | 长期股权投资 | LONG_TERM_EQUITY_INVESTMENTS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 16 | 其他权益工具投资 | 其他权益工具投资 | OTHER_EQUITY_INVESTMENTS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 17 | 其他非流动金融资产 | 其他非流动金融资产 | OTHER_NON_CURRENT_FINANCIAL_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 18 | 投资性房地产 | 投资性房地产 | INVESTMENT_PROPERTY | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 19 | 固定资产 | 固定资产 | FIXED_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 20 | 在建工程 | 在建工程 | CONSTRUCTION_IN_PROGRESS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 21 | 生产性生物资产 | 生产性生物资产 | PRODUCTIVE_BIOLOGICAL_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 22 | 油气资产 | 油气资产 | OIL_AND_GAS_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 23 | 使用权资产 | 使用权资产 | RIGHT_OF_USE_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 24 | 无形资产 | 无形资产 | INTANGIBLE_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 25 | 开发支出 | 开发支出 | DEVELOPMENT_EXPENDITURE | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 26 | 商誉 | 商誉 | GOODWILL | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 27 | 长期待摊费用 | 长期待摊费用 | LONG_TERM_DEFERRED_EXPENSES | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 28 | 递延所得税资产 | 递延所得税资产 | DEFERRED_TAX_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 29 | 其他非流动资产 | 其他非流动资产 | OTHER_NON_CURRENT_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 30 | 非流动资产合计 | 非流动资产合计 | TOTAL_NON_CURRENT_ASSETS | ending_balance | NON_CURRENT_ASSET | 元→万元 |
| 31 | 资产总计 | 资产总计 | TOTAL_ASSETS | ending_balance | EQUITY | 元→万元 |

#### 流动负债类（Current Liabilities）

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 32 | 短期借款 | 短期借款 | SHORT_TERM_BORROWINGS | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 33 | 应付票据 | 应付票据 | NOTES_PAYABLE | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 34 | 应付账款 | 应付账款 | ACCOUNTS_PAYABLE | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 35 | 预收款项 | 预收款项 | ADVANCES_FROM_CUSTOMERS | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 36 | 合同负债 | 合同负债 | CONTRACT_LIABILITIES | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 37 | 应付职工薪酬 | 应付职工薪酬 | EMPLOYEE_BENEFITS_PAYABLE | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 38 | 应交税费 | 应交税费 | TAXES_PAYABLE | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 39 | 其他应付款 | 其他应付款 | OTHER_PAYABLES | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 40 | 一年内到期的非流动负债 | 一年内到期的非流动负债 | NON_CURRENT_LIABILITIES_DUE_WITHIN_ONE_YEAR | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 41 | 其他流动负债 | 其他流动负债 | OTHER_CURRENT_LIABILITIES | ending_balance | CURRENT_LIABILITY | 元→万元 |
| 42 | 流动负债合计 | 流动负债合计 | TOTAL_CURRENT_LIABILITIES | ending_balance | CURRENT_LIABILITY | 元→万元 |

#### 非流动负债与所有者权益类

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 43 | 长期借款 | 长期借款 | LONG_TERM_BORROWINGS | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 44 | 应付债券 | 应付债券 | BONDS_PAYABLE | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 45 | 租赁负债 | 租赁负债 | LEASE_LIABILITIES | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 46 | 预计负债 | 预计负债 | PROVISIONS | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 47 | 递延收益 | 递延收益 | DEFERRED_REVENUE | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 48 | 递延所得税负债 | 递延所得税负债 | DEFERRED_TAX_LIABILITIES | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 49 | 非流动负债合计 | 非流动负债合计 | TOTAL_NON_CURRENT_LIABILITIES | ending_balance | NON_CURRENT_LIABILITY | 元→万元 |
| 50 | 负债合计 | 负债合计 | TOTAL_LIABILITIES | ending_balance | EQUITY | 元→万元 |
| 51 | 实收资本（或股本） | 实收资本 | PAID_IN_CAPITAL | ending_balance | EQUITY | 元→万元 |
| 52 | 其他权益工具 | 其他权益工具 | OTHER_EQUITY_INSTRUMENTS | ending_balance | EQUITY | 元→万元 |
| 53 | 资本公积 | 资本公积 | CAPITAL_RESERVE | ending_balance | EQUITY | 元→万元 |
| 54 | 减：库存股 | 库藏股 | TREASURY_STOCK | ending_balance | EQUITY | 元→万元 |
| 55 | 其他综合收益 | 其他综合收益 | OTHER_COMPREHENSIVE_INCOME | ending_balance | EQUITY | 元→万元 |
| 56 | 专项储备 | 专项储备 | SPECIAL_RESERVE | ending_balance | EQUITY | 元→万元 |
| 57 | 盈余公积 | 盈余公积 | SURPLUS_RESERVE | ending_balance | EQUITY | 元→万元 |
| 58 | 未分配利润 | 未分配利润 | UNDISTRIBUTED_PROFITS | ending_balance | EQUITY | 元→万元 |
| 59 | 所有者权益合计 | 所有者权益合计 | TOTAL_EQUITY | ending_balance | EQUITY | 元→万元 |
| 60 | 负债和所有者权益总计 | 负债和所有者权益总计 | TOTAL_LIABILITIES_AND_EQUITY | ending_balance | EQUITY | 元→万元 |

### 2. 利润表字段映射（Income Statement）

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 1 | 营业总收⼊ / 主营业务收入 | 营业总收入 | TOTAL_OPERATING_INCOME | current_period_amount | REVENUE | 元→万元 |
| 2 | 营业成本 / 主营业务成本 | 营业总成本 | TOTAL_OPERATING_COST | current_period_amount | COST | 元→万元 |
| 3 | 税金及附加 | 税金及附加 | TAXES_AND_SURCHARGES | current_period_amount | EXPENSE | 元→万元 |
| 4 | 销售费用 | 销售费用 | SELLING_EXPENSES | current_period_amount | EXPENSE | 元→万元 |
| 5 | 管理费用 | 管理费用 | ADMINISTRATIVE_EXPENSES | current_period_amount | EXPENSE | 元→万元 |
| 6 | 研发费用 | 研发费用 | R&D_EXPENSES | current_period_amount | EXPENSE | 元→万元 |
| 7 | 财务费用 | 财务费用 | FINANCING_EXPENSES | current_period_amount | EXPENSE | 元→万元 |
| 8 | 加：其他收益 | 其他收益 | OTHER_INCOME | current_period_amount | REVENUE | 元→万元 |
| 9 | 投资收益（损失以"-"号填列） | 投资收益 | INVESTMENT_INCOME | current_period_amount | PROFIT | 元→万元 |
| 10 | 净敞口套期收益 | 净敞口套期收益 | NET_EXPOSURE_HEDGING_GAINS | current_period_amount | PROFIT | 元→万元 |
| 11 | 公允价值变动收益 | 公允价值变动收益 | FAIR_VALUE_CHANGES | current_period_amount | PROFIT | 元→万元 |
| 12 | 信用减值损失 | 信用减值损失 | CREDIT_IMPAIRMENT_LOSSES | current_period_amount | EXPENSE | 元→万元 |
| 13 | 资产减值损失 | 资产减值损失 | ASSET_IMPAIRMENT_LOSSES | current_period_amount | EXPENSE | 元→万元 |
| 14 | 资产处置收益 | 资产处置收益 | ASSET_DISPOSAL_INCOME | current_period_amount | PROFIT | 元→万元 |
| 15 | 营业利润（亏损以"-"号填列） | 营业利润 | OPERATING_PROFIT | current_period_amount | PROFIT | 元→万元 |
| 16 | 加：营业外收入 | 营业外收入 | NON_OPERATING_INCOME | current_period_amount | PROFIT | 元→万元 |
| 17 | 减：营业外支出 | 营业外支出 | NON_OPERATING_EXPENSES | current_period_amount | EXPENSE | 元→万元 |
| 18 | 利润总额（亏损总额以"-"号填列） | 利润总额 | TOTAL_PROFIT | current_period_amount | PROFIT | 元→万元 |
| 19 | 减：所得税费用 | 所得税费用 | INCOME_TAX_EXPENSE | current_period_amount | EXPENSE | 元→万元 |
| 20 | 净利润（净亏损以"-"号填列） | 净利润 | NET_PROFIT | current_period_amount | PROFIT | 元→万元 |
| 21 | 归属于母公司所有者的净利润 | 归母净利润 | NET_PROFIT_ATTRIBUTABLE_TO_PARENT | current_period_amount | PROFIT | 元→万元 |
| 22 | 少数股东损益 | 少数股东损益 | MINORITY_INTEREST | current_period_amount | PROFIT | 元→万元 |

### 3. 现金流量表字段映射（Cash Flow Statement）

#### 经营活动产生的现金流量

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 1 | 销售商品、提供劳务收到的现金 | 销售商品收到现金 | CASH_RECEIVED_FROM_SALES | current_period_amount | OPERATING_INFLOW | 元→万元 |
| 2 | 收到的税费返还 | 收到的税费返还 | TAX_REFUNDS_RECEIVED | current_period_amount | OPERATING_INFLOW | 元→万元 |
| 3 | 收到其他与经营活动有关的现金 | 收到其他经营活动现金 | OTHER_OPERATING_CASH_RECEIVED | current_period_amount | OPERATING_INFLOW | 元→万元 |
| 4 | 经营活动现金流入小计 | 经营活动现金流入小计 | CASH_INFLOWS_FROM_OPERATING_ACTIVITIES | current_period_amount | OPERATING_INFLOW | 元→万元 |
| 5 | 购买商品、接受劳务支付的现金 | 购买商品支付现金 | CASH_PAID_FOR_GOODS_AND_SERVICES | current_period_amount | OPERATING_OUTFLOW | 元→万元 |
| 6 | 支付给职工以及为职工支付的现金 | 支付给职工的现金 | CASH_PAID_TO_EMPLOYEES | current_period_amount | OPERATING_OUTFLOW | 元→万元 |
| 7 | 支付的各项税费 | 支付的各项税费 | TAXES_PAID | current_period_amount | OPERATING_OUTFLOW | 元→万元 |
| 8 | 支付其他与经营活动有关的现金 | 支付其他经营活动现金 | OTHER_OPERATING_CASH_PAID | current_period_amount | OPERATING_OUTFLOW | 元→万元 |
| 9 | 经营活动现金流出小计 | 经营活动现金流出小计 | CASH_OUTFLOWS_FROM_OPERATING_ACTIVITIES | current_period_amount | OPERATING_OUTFLOW | 元→万元 |
| 10 | 经营活动产生的现金流量净额 | 经营活动现金流净额 | NET_CASH_FROM_OPERATING_ACTIVITIES | current_period_amount | OPERATING_OUTFLOW | 元→万元 |

#### 投资活动产生的现金流量

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 11 | 收回投资收到的现金 | 收回投资收到现金 | CASH_RECEIVED_FROM_INVESTMENTS | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 12 | 取得投资收益收到的现金 | 取得投资收益收到现金 | DIVIDENDS_RECEIVED | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 13 | 处置固定资产、无形资产和其他长期资产收回的现金净额 | 处置长期资产收回现金 | CASH_FROM_DISPOSAL_OF_LONG_TERM_ASSETS | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 14 | 处置子公司及其他营业单位收到的现金净额 | 处置子公司收到现金 | CASH_FROM_DISPOSAL_OF_SUBSIDIARIES | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 15 | 收到其他与投资活动有关的现金 | 收到其他投资活动现金 | OTHER_INVESTING_CASH_RECEIVED | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 16 | 投资活动现金流入小计 | 投资活动现金流入小计 | CASH_INFLOWS_FROM_INVESTING_ACTIVITIES | current_period_amount | INVESTING_INFLOW | 元→万元 |
| 17 | 购建固定资产、无形资产和其他长期资产支付的现金 | 购建长期资产支付现金 | CASH_PAID_FOR_ACQUISITION_OF_LONG_TERM_ASSETS | current_period_amount | INVESTING_OUTFLOW | 元→万元 |
| 18 | 投资支付的现金 | 投资支付现金 | CASH_PAID_FOR_INVESTMENTS | current_period_amount | INVESTING_OUTFLOW | 元→万元 |
| 19 | 取得子公司及其他营业单位支付的现金净额 | 取得子公司支付现金 | CASH_PAID_FOR_ACQUISITION_OF_SUBSIDIARIES | current_period_amount | INVESTING_OUTFLOW | 元→万元 |
| 20 | 支付其他与投资活动有关的现金 | 支付其他投资活动现金 | OTHER_INVESTING_CASH_PAID | current_period_amount | INVESTING_OUTFLOW | 元→万元 |
| 21 | 投资活动现金流出小计 | 投资活动现金流出小计 | CASH_OUTFLOWS_FROM_INVESTING_ACTIVITIES | current_period_amount | INVESTING_OUTFLOW | 元→万元 |
| 22 | 投资活动产生的现金流量净额 | 投资活动现金流净额 | NET_CASH_FROM_INVESTING_ACTIVITIES | current_period_amount | INVESTING_OUTFLOW | 元→万元 |

#### 筹资活动产生的现金流量

| 序号 | OCR原始名称 | 标准名称 | 标准编码 | 数据库字段 | 所属类别 | 单位换算 |
|-----|------------|---------|---------|-----------|---------|---------|
| 23 | 吸收投资收到的现金 | 吸收投资收到现金 | CASH_RECEIVED_FROM_FINANCING | current_period_amount | FINANCING_INFLOW | 元→万元 |
| 24 | 取得借款收到的现金 | 取得借款收到现金 | CASH_FROM_BORROWINGS | current_period_amount | FINANCING_INFLOW | 元→万元 |
| 25 | 收到其他与筹资活动有关的现金 | 收到其他筹资活动现金 | OTHER_FINANCING_CASH_RECEIVED | current_period_amount | FINANCING_INFLOW | 元→万元 |
| 26 | 筹资活动现金流入小计 | 筹资活动现金流入小计 | CASH_INFLOWS_FROM_FINANCING_ACTIVITIES | current_period_amount | FINANCING_INFLOW | 元→万元 |
| 27 | 偿还债务支付的现金 | 偿还债务支付现金 | CASH_PAID_FOR_DEBT_REPAYMENT | current_period_amount | FINANCING_OUTFLOW | 元→万元 |
| 28 | 分配股利、利润或偿付利息支付的现金 | 分配股利支付现金 | CASH_PAID_FOR_DIVIDENDS | current_period_amount | FINANCING_OUTFLOW | 元→万元 |
| 29 | 支付其他与筹资活动有关的现金 | 支付其他筹资活动现金 | OTHER_FINANCING_CASH_PAID | current_period_amount | FINANCING_OUTFLOW | 元→万元 |
| 30 | 筹资活动现金流出小计 | 筹资活动现金流出小计 | CASH_OUTFLOWS_FROM_FINANCING_ACTIVITIES | current_period_amount | FINANCING_OUTFLOW | 元→万元 |
| 31 | 筹资活动产生的现金流量净额 | 筹资活动现金流净额 | NET_CASH_FROM_FINANCING_ACTIVITIES | current_period_amount | FINANCING_OUTFLOW | 元→万元 |
| 32 | 汇率变动对现金的影响 | 汇率变动影响 | EFFECT_OF_EXCHANGE_RATE_CHANGES | current_period_amount | OTHER | 元→万元 |
| 33 | 现金及现金等价物净增加额 | 现金净增加额 | NET_INCREASE_IN_CASH_AND_CASH_EQUIVALENTS | current_period_amount | OTHER | 元→万元 |
| 34 | 期初现金及现金等价物余额 | 期初现金余额 | BEGINNING_CASH_AND_CASH_EQUIVALENTS | current_period_amount | OTHER | 元→万元 |
| 35 | 期末现金及现金等价物余额 | 期末现金余额 | ENDING_CASH_AND_CASH_EQUIVALENTS | current_period_amount | OTHER | 元→万元 |

---

## 不同会计准则差异处理

### 中国企业会计准则（CAS）vs 国际财务报告准则（IFRS）

| 差异点 | CAS（中国企业会计准则） | IFRS（国际财务报告准则） | 系统处理方式 |
|-------|----------------------|----------------------|-------------|
| 报表格式 | 账户式（左右结构） | 报告式（上下结构） | 统一转换为账户式存储 |
| 存货计量 | 成本与可变现净值孰低 | 公允价值可选 | 使用CAS默认规则 |
| 固定资产折旧 | 直线法为主 | 允许加速折旧 | 记录折旧方法，不影响映射 |
| 无形资产摊销 | 有使用寿命的摊销 | 可重估 | 按CAS规则处理 |
| 研发费用 | 费用化为主 | 可资本化 | 区分研究阶段和开发阶段 |
| 政府补助 | 资产相关/收益相关 | 全部计入其他综合收益 | 统一归入"其他收益" |
| 借款费用 | 资本化条件严格 | 相对宽松 | 按CAS规则判断 |

### 小企业会计准则差异

| 项目 | 一般企业 | 小企业 | 映射调整 |
|------|---------|--------|---------|
| 资产减值准备 | 单独列示 | 不计提 | 映射时设为0或NULL |
| 公允价值变动 | 单独项目 | 不适用 | 映射时跳过 |
| 所得税费用 | 单独项目 | 不单独核算 | 映射时根据利润倒推 |
| 每股收益 | 必须披露 | 不要求 | 可选计算 |

---

## 单位换算规则

### 默认单位体系
- **系统内部存储单位**：万元（10,000元）
- **OCR识别常见单位**：元、千元、万元、亿元

### 换算算法

```java
// 伪代码：单位自动检测与换算
function convertToWanYuan(rawValue, detectedUnit) {
    switch(detectedUnit) {
        case '元':
            return rawValue / 10000;
        case '千元':
            return rawValue / 10;
        case '万元':
            return rawValue;
        case '亿元':
            return rawValue * 10000;
        default:
            // 尝试自动检测
            if (rawValue > 1000000) return rawValue / 10000; // 可能是元
            if (rawValue < 0.01) return rawValue * 10000;   // 可能是亿元
            return rawValue; // 默认为万元
    }
}
```

### 单位检测启发式规则

1. **数值范围推断**：
   - 大型企业：总资产 > 100亿 → 可能是万元
   - 中型企业：总资产 1-100亿 → 可能是万元或千元
   - 小微企业：总资产 < 1亿 → 可能是元

2. **表头关键词匹配**：
   - 包含"单位：元" → 除以10000
   - 包含"单位：万元" → 不变
   - 包含"单位：千元" → 除以10

3. **历史数据对比**：
   - 与该企业上期数据对比
   - 波动超过±50%触发警告

4. **人工确认机制**：
   - 低置信度字段强制人工复核
   - 单位异常标记为待确认状态

---

## 特殊值处理规则

### 1. 负数表示法

| 表示形式 | 示例 | 处理逻辑 | 正则表达式 |
|---------|------|---------|-----------|
| 标准负号 | `-1234.56` | 直接取负值 | `^-?\d+\.?\d*$` |
| 括号表示法 | `(1234.56)` | 提取数值并取负 | `^\((\d+\.?\d*)\)$` |
| 中文"负" | `负1234.56` | 提取数值并取负 | `^负(\d+\.?\d*)$` |
| 红色字体 | （视觉特征） | 依赖OCR标注 | N/A |

**处理代码示例**：
```javascript
function parseNegativeNumber(text) {
    // 规则1：括号表示法
    if (text.startsWith('(') && text.endsWith(')')) {
        return -parseFloat(text.slice(1, -1));
    }

    // 规则2：中文"负"前缀
    if (text.startsWith('负')) {
        return -parseFloat(text.substring(1));
    }

    // 规则3：标准负号
    const num = parseFloat(text);
    if (!isNaN(num)) {
        return num;
    }

    throw new Error(`无法解析数字: ${text}`);
}
```

### 2. 空值处理

| 场景 | 原始值 | 处理方式 | 数据库存储 |
|------|--------|---------|-----------|
| 单元格空白 | `""` 或 `" "` | 判定为空值 | NULL |
| 显示横杠 | `"-"` 或 `"——"` | 判定为零或无数据 | NULL 或 0.00 |
| 显示"无" | `"无"` | 判定为零 | 0.00 |
| 显示"N/A" | `"N/A"` | 判定为不适用 | NULL |
| 显示"***" | `"***"` | 判定为敏感数据 | NULL + 标记 |

**空值判定优先级**：
1. 明确的零值标识（"无"、"0"）→ 存储 0.00
2. 占位符（"-","——","*") → 存储 NULL
3. 真正空值 → 存储 NULL
4. 无法确定 → 标记为 LOW_CONFIDENCE，需人工确认

### 3. 数值格式清理

**需要处理的字符**：
- 千分位逗号：`1,234,567.89` → `1234567.89`
- 空格：`12 345.67` → `12345.67`
- 中文数字单位：`壹万贰仟` → 12000（特殊情况）
- 全角数字：`１２３４` → `1234`

**清理函数**：
```javascript
function cleanNumericString(raw) {
    return raw
        .replace(/,/g, '')           // 移除千分位逗号
        .replace(/\s/g, '')          // 移除空格
        .replace(/[Ａ-Ｚａ-ｚ０-９]/g, function(c) {
            // 全角转半角
            return String.fromCharCode(c.charCodeAt(0) - 0xFEE0);
        });
}
```

### 4. 精度处理规则

- **存储精度**：DECIMAL(18,2)，保留两位小数
- **显示精度**：
  - 金额类：保留2位小数
  - 百分比类：保留2位小数
  - 比率类：保留4位小数
  - 指标得分：整数
- **舍入规则**：四舍五入（银行家舍入法可选）
- **平衡校验容忍度**：配置在 `application.yml`：
  ```yaml
  financial-validation:
    balance-sheet-tolerance: 0.01  # 资产=负债+权益 差额<0.01万元视为平衡
  ```

---

## 映射规则管理

### 规则配置位置
- **数据库表**：`field_mapping_rule`
- **管理界面**：管理后台 → 规则配置页面
- **配置文件**：可通过 `data.sql` 预置规则

### 规则属性说明

| 属性名 | 类型 | 说明 | 示例 |
|-------|------|------|------|
| rule_name | VARCHAR(100) | 规则名称 | "货币资金映射" |
| source_field_name | VARCHAR(100) | OCR源字段名 | "货币资金" |
| target_field_code | VARCHAR(50) | 目标标准编码 | "CASH_AND_CASH_EQUIVALENTS" |
| report_type | VARCHAR(30) | 适用报表类型 | "BALANCE_SHEET" |
| mapping_type | VARCHAR(20) | 映射方式 | "DIRECT/CALCULATED/AGGREGATE" |
| transformation_rule | VARCHAR(500) | 转换规则 | "元→万元, /10000" |
| priority | INT | 优先级 | 100 |
| is_active | TINYINT | 是否启用 | 1 |
| match_confidence_threshold | DECIMAL(3,2) | 匹配阈值 | 0.70 |

### 映射冲突解决策略

当多个规则匹配同一OCR字段时：

1. **优先级排序**：选择 priority 最高的规则
2. **精确匹配优先**：DIRECT 类型优于 CALCULATED
3. **最新规则优先**：created_time 最新的优先
4. **人工介入**：置信度低于阈值时标记待审核

---

## 映射质量保障

### 自动化校验机制

1. **勾稽关系校验**：
   - 资产 = 负债 + 所有者权益
   - 营业收入 - 成本 = 毛利
   - 现金流期末 = 期初 + 净增加额

2. **合理性检查**：
   - 总资产 > 0
   - 流动比率通常在 0.5-5 之间
   - 资产负债率 < 200%（极端情况除外）

3. **历史对比**：
   - 环比波动 > ±30% 标记异常
   - 同比波动 > ±50% 标记高风险

4. **行业基准**：
   - 与同行业平均水平对比
   - 偏离超过2个标准差标记异常

### 人工复核流程

1. **低置信度字段高亮**：confidence_score < 0.7 的字段红色标记
2. **异常值提示**：超出合理范围的字段黄色标记
3. **批量修正支持**：允许批量修改同类错误
4. **修改留痕**：记录所有人工修正操作到 audit_log 表

---

## 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|---------|
| v1.0 | 2024年 | 开发团队 | 初始版本，覆盖三大报表60+个核心字段映射 |

---

## 相关文档
- [API接口文档](./api.md) - 映射规则的CRUD接口
- [数据库设计文档](./database-design.md) - field_mapping_rule表结构
- [财务公式文档](./financial-formulas.md) - 基于映射后的指标计算公式
