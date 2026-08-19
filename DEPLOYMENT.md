# 部署与运维

## 1. Docker 单机部署

准备一台安装了 Docker Engine 与 Docker Compose 的服务器，开放管理后台端口（默认 `8080`）。

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up -d --build
docker compose ps
```

必须修改：

- `ADMIN_PASSWORD`、`MANAGER_PASSWORD`：使用独立高强度密码。
- `PUBLIC_ORIGIN`：生产管理后台的完整 HTTPS 源，例如 `https://finance.example.com`，不要带路径。
- `DEMO_DATA_ENABLED=false`：空库生产环境不得写入验收样例。
- `AI_PROVIDER=http`、`OCR_HTTP_ENDPOINT`、`OCR_HTTP_TOKEN`：接入真实 OCR。

反向代理或负载均衡应终止 HTTPS，再转发至宿主机 `WEB_PORT`。微信小程序正式环境只允许访问已在微信公众平台登记的 HTTPS request/upload/download 合法域名。

服务检查：

```powershell
Invoke-RestMethod https://finance.example.com/api/health
docker compose logs --tail 200 backend
docker compose logs --tail 100 frontend
```

更新：

```powershell
docker compose build --pull
docker compose up -d
```

## 2. 真实 OCR HTTP 协议

后端向 `OCR_HTTP_ENDPOINT` 发送 `multipart/form-data`：

- `file`：扫描 PDF、PNG、JPG/JPEG 原文件（XLS/XLSX 由后端本地解析，不发送至 OCR 服务）；
- `documentType`：固定为 `FINANCIAL_STATEMENT`；
- `Authorization`：配置 `OCR_HTTP_TOKEN` 时发送 `Bearer <token>`。

服务需返回下列结构之一：

```json
{
  "fields": [
    {
      "fieldName": "货币资金",
      "fieldCode": "cash",
      "fieldValue": "1034959.57",
      "confidenceScore": 98.25,
      "fieldType": "BALANCE_SHEET",
      "pageNumber": 1,
      "boundingBox": "[100,120,420,154]"
    }
  ]
}
```

或：

```json
{
  "data": {
    "fields": []
  }
}
```

约束：

- `fieldType` 只能为 `BALANCE_SHEET`、`INCOME_STATEMENT`、`CASH_FLOW_STATEMENT`。
- `confidenceScore` 可用 `0–1` 或 `0–100`，后端统一转为百分制。
- 金额字段 `fieldValue` 使用纯数字字符串，负数带 `-`，不要附带千分位或单位。
- `fieldName` 应使用中国企业会计报表常用项目名。未识别字段不要伪造，应省略并交给人工复核。

当 `AI_PROVIDER=mock` 时，系统明确标记任务提供方为 `MOCK_OCR` 并生成测试字段。该模式不得处理或出具真实客户报表。

## 3. 小程序发布

1. 微信开发者工具导入 `wxapp`，替换 `project.config.json` 中的 `appid`。
2. 开发环境在“详情 → 本地设置”中勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。开发者工具和真机调试统一使用 `wxapp/config.js` 的 `developmentBaseUrl`，该值应填写电脑当前局域网 IPv4，且手机和电脑必须处于同一局域网。
3. 普通小程序发布时，在 `wxapp/config.js` 的 `productionBaseUrl` 填写 `https://finance.example.com/api`。
4. 第三方平台发布也可通过 `extConfig.apiBaseUrl` 注入地址，此时 `productionBaseUrl` 可留空。
5. 上传合法域名、request 合法域名与 downloadFile 合法域名均需包含 API 域名。
6. 上传前清除开发工具缓存，分别验收 PDF、XLS/XLSX、相册图片和拍照上传。

## 4. 非 Docker 启动

后端需要 JDK 8 和 Maven 3：

```powershell
Set-Location backend
mvn clean package
$env:ADMIN_PASSWORD='replace-me'
$env:MANAGER_PASSWORD='replace-me'
$env:DB_URL='jdbc:h2:file:C:/xinsulu-data/xinsulu;MODE=MySQL;AUTO_SERVER=TRUE'
$env:UPLOAD_DIR='C:/xinsulu-data/uploads'
$env:DEMO_DATA_ENABLED='false'
$env:AI_PROVIDER='http'
$env:OCR_HTTP_ENDPOINT='https://ocr.example.com/v1/financial-statements'
java -jar target/xinsulu-backend-1.0.0.jar
```

管理后台：

```powershell
Set-Location frontend
npm ci
npm run build
```

将 `frontend/dist` 发布至 Nginx，并把 `/api/` 反向代理到后端 `8080` 端口。项目自带的 `frontend/nginx.conf` 可直接复用。

## 5. 数据备份与恢复

Docker 数据保存在命名卷 `xinsulu-data`，其中包括 H2 数据库文件和 `uploads` 原件。备份必须在后端停止后执行，避免数据库文件不一致：

```powershell
docker compose stop backend
docker run --rm -v xinsulu_xinsulu-data:/data -v ${PWD}:/backup alpine `
  tar czf /backup/xinsulu-data-backup.tgz -C /data .
docker compose start backend
```

恢复前先停止服务，并恢复到一个空卷。恢复属于覆盖操作，应在确认备份文件和目标环境后进行。

单机 H2 适合当前交付规模；若需要多实例、高并发或数据库高可用，应迁移到 MySQL/PostgreSQL，并为实体字段、索引和事务隔离做专项验收。

## 6. 上线验收清单

- `/api/health` 返回 `code=200` 和 `status=UP`。
- 未带 Token 访问 `/api/dashboard/stats` 返回 HTTP 401。
- 管理员和客户经理均可登录，默认密码已失效。
- 上传的原文件可下载，30 MB 限制符合业务要求。
- OCR 任务 `provider=HTTP_OCR`，没有 `MOCK_OCR` 生产记录。
- 人工复核后生成三张报表，并通过资产平衡、利润勾稽、现金流勾稽和现金一致性校验。
- 原始样例的核心金额、35 项指标和健康总分与基准一致。
- 管理后台路由刷新不返回 404。
- 小程序真机可完成 request、uploadFile、downloadFile。
- 已完成数据卷和上传原件备份演练。
