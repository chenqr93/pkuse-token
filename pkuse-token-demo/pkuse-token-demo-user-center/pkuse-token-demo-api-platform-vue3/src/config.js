// ==================== 各客户端差异配置（复制模板时只需修改本文件） ====================
export default {
  // 客户端名称与模式说明
  clientName: '接口快速开发平台（magic-api）',
  modeLabel: 'SSO 模式三接入 · 开放接口 · 接口级鉴权',
  modeDesc: '面向接口快速开发的开放平台 demo：magic-api 在线编排接口 + Pkuse-Token 统一鉴权。登录层以 SSO 模式三接入用户中心（独立 Redis db7、HTTP 校验 ticket），接口权限码从授权中心分发（sso-client5），第三方调用方经 /open/token 换取平台令牌调用。',

  // 本应用后端地址（前端独立部署，跨域访问后端）
  apiBase: 'http://127.0.0.1:9005',

  // 前端开发服务器端口
  port: 5178,

  // 登录方式：
  //   navigate —— 前端跳转本应用后端 /sso/login，由后端（SDK）完成与认证中心的交互（模式一使用）
  //   ticket   —— 前端取认证中心授权地址，回跳后用 ticket 调后端换取本应用 token（模式二/三/NoSdk 使用）
  loginMode: 'ticket',

  // 认证中心控制台地址（页面上提供返回入口）
  consoleUrl: 'http://localhost:5173'
}
