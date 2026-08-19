# 鑫速录

企业财务报表自动化填报与智能分析系统，包含：

- `wxapp`：原生微信小程序，完成上传、OCR、建档、复核、三张报表、校验、健康度、分析和趋势查询。
- `frontend`：Vue 2 管理后台，还原原型中的工作台、企业档案、报表详情、分析、趋势和规则配置。
- `backend`：Spring Boot API，提供鉴权、文件存储、OCR 适配、报表持久化、指标计算、健康评分和分析报告。

## 一键部署

需要 Docker Engine 与 Docker Compose：

```powershell
Copy-Item .env.example .env
# 编辑 .env，至少修改两个登录密码；生产环境还需配置真实 OCR
docker compose up -d --build
```

启动后访问 `http://localhost:8080`。健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

首次验收可保留 `.env` 中的 `DEMO_DATA_ENABLED=true` 和 `AI_PROVIDER=mock`。带文字层的标准三表 PDF 会优先在本地真实解析，并统一补齐为资产负债表 65 项、利润表 37 项、现金流量表 38 项；扫描件或图片需要切换到 `AI_PROVIDER=http` 并配置 OCR 服务。

完整生产配置、OCR 协议、小程序发布和备份恢复说明见 [DEPLOYMENT.md](./DEPLOYMENT.md)，本次验证明细见 [DELIVERY_REPORT.md](./DELIVERY_REPORT.md)。

## 本地开发

后端：

```powershell
Set-Location backend
mvn spring-boot:run
```

管理后台：

```powershell
Set-Location frontend
npm ci
npm run serve
```

访问 `http://localhost:8081`。开发环境默认账号为 `admin` / `admin123` 与 `manager` / `manager123`；密码可通过 `ADMIN_PASSWORD`、`MANAGER_PASSWORD` 环境变量覆盖，生产环境必须修改。

小程序用微信开发者工具导入 `wxapp`。开发者工具和真机开发版统一访问 `wxapp/config.js` 中的 `developmentBaseUrl`，该地址应填写电脑当前局域网 IPv4，手机与电脑必须处于同一局域网。体验版和正式版必须按 `DEPLOYMENT.md` 配置 HTTPS API 域名。

## 验证

```powershell
Set-Location backend
mvn test

Set-Location ..\frontend
npm run build
npm audit --omit=dev
```

后端集成测试覆盖：

- 报表原件核心值、三张报表结构和 35 项指标；
- 五维健康度精确评分（总分 22，风险等级 `DANGEROUS`）；
- 指标与分析结果幂等；
- API 鉴权、工作台、企业搜索；
- 上传 → 文件下载 → OCR → 建档 → 复核 → 三张报表持久化全链路。
- 真实三页 PDF 的企业名称、报表期、关键金额及 65/37/38 个完整字段回归验证。

## 关键配置

| 环境变量 | 说明 | 生产要求 |
|---|---|---|
| `ADMIN_PASSWORD` | 管理员密码 | 必须修改 |
| `MANAGER_PASSWORD` | 客户经理密码 | 必须修改 |
| `PUBLIC_ORIGIN` | 管理后台公开源 | 使用真实 HTTPS 域名 |
| `DEMO_DATA_ENABLED` | 是否写入样例报表 | 生产设为 `false` |
| `AI_PROVIDER` | `mock` 或 `http` | 仅处理文字层 PDF 可用 `mock`；扫描件设为 `http` |
| `OCR_HTTP_ENDPOINT` | 标准化 OCR 服务地址 | 处理扫描件或图片时必填 |
| `OCR_HTTP_TOKEN` | OCR Bearer Token | 按服务配置 |
| `UPLOAD_DIR` | 上传文件目录 | 容器默认 `/data/uploads` |

## 数据单位与基准

系统按报表原件使用“元”。内置验收样例为“江苏曼斯特机电科技有限公司，2026-03”：

- 资产总计：`7,923,544.71`
- 负债合计：`7,995,201.81`
- 所有者权益：`-71,657.10`
- 营业收入：`773,769.57`
- 净利润：`-9,950.59`
- 经营活动现金流净额：`-391,006.53`
- 期末现金：`1,034,959.57`

健康度五维得分为 `29.50 / 31.75 / 9.00 / 9.00 / 22.67`，加权总分为 `22`。
