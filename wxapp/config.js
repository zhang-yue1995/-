module.exports = {
  // 开发者工具和真机开发版统一访问电脑的局域网地址，避免运行环境
  // 判断差异导致真机误用 127.0.0.1。网络变化后请同步更新此项。
  // developmentBaseUrl: 'http://192.168.43.5:8080/api',
  developmentBaseUrl: 'http://127.0.0.1:8080/api',

  // 正式发布前填写已在微信公众平台登记的 HTTPS API 地址。
  // 使用第三方平台 extConfig.apiBaseUrl 时可保持为空。
  productionBaseUrl: ''
};
