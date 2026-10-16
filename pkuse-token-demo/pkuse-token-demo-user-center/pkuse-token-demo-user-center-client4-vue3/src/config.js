// ==================== 各客户端差异配置（复制模板时只需修改本文件） ====================
export default {
  // 客户端名称与模式说明
  clientName: '客户端4（NoSdk）',
  modeLabel: 'NoSdk：不依赖客户端 SDK，手动实现协议',
  modeDesc: '本项目未引入任何 Pkuse-Token 依赖：授权地址自行拼装，ticket 校验/单点注销/账号资料均通过调用认证中心 /sso/pushS 接口完成，参数签名（timestamp + nonce + MD5 sign）与本地会话（内存 token）均手动实现。',

  // 本应用后端地址（前端独立部署，跨域访问后端）
  apiBase: 'http://127.0.0.1:9004',

  // 前端开发服务器端口
  port: 5177,

  // 登录方式：
  //   navigate —— 前端跳转本应用后端 /sso/login，由后端（SDK）完成与认证中心的交互（模式一使用）
  //   ticket   —— 前端取认证中心授权地址，回跳后用 ticket 调后端换取本应用 token（模式二/三/NoSdk 使用）
  loginMode: 'ticket',

  // 认证中心控制台地址（页面上提供返回入口）
  consoleUrl: 'http://localhost:8090'
}
