/**
 * 设备指纹
 *
 * 采集浏览器环境特征（UA、语言、屏幕、时区、Canvas 渲染差异等）+ 浏览器内首次生成的随机盐，取 SHA-256 作为设备标识：
 *   1. 随登录请求上报（deviceId），服务端用于「设备锁」绑定与登录日志；
 *   2. 同一浏览器再次登录时指纹一致（换浏览器/设备则不同），从而触发设备锁校验；
 *   3. 随机盐保证「同型号环境的两个浏览器（如两个 Chrome Profile、无痕窗口）」指纹互不相同，
 *      使单点注销可以精确到单个浏览器会话，不会误杀同环境下的其它浏览器。
 *
 * 说明：这是演示用的轻量实现；生产环境可换用专业指纹库（如 fingerprintjs）或结合设备证书。
 */
const FINGERPRINT_KEY = 'user-center-fingerprint'
const FINGERPRINT_SALT_KEY = 'user-center-fingerprint-salt'

let cached = ''

/** 获取设备指纹（同一浏览器内缓存，避免每次重复计算） */
export async function getDeviceFingerprint() {
  if (cached) {
    return cached
  }
  const stored = localStorage.getItem(FINGERPRINT_KEY)
  if (stored) {
    cached = stored
    return cached
  }
  const raw = [
    navigator.userAgent,
    navigator.language,
    (navigator.languages || []).join(','),
    screen.width + 'x' + screen.height + 'x' + screen.colorDepth,
    window.devicePixelRatio,
    new Date().getTimezoneOffset(),
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    navigator.hardwareConcurrency || 0,
    await canvasFingerprint(),
    // 浏览器内随机盐：首次生成后持久保存。区分「环境特征完全相同的两个浏览器」，同时保持同一浏览器内指纹稳定
    browserSalt()
  ].join('|')

  cached = 'fp-' + (await sha256(raw))
  localStorage.setItem(FINGERPRINT_KEY, cached)
  return cached
}

/** 浏览器内随机盐：首次生成（crypto 随机数）后存入 localStorage，此后固定不变 */
function browserSalt() {
  let salt = localStorage.getItem(FINGERPRINT_SALT_KEY)
  if (!salt) {
    const bytes = new Uint8Array(16)
    if (window.crypto && crypto.getRandomValues) {
      crypto.getRandomValues(bytes)
    } else {
      // 退化实现：随机数 + 时间戳
      for (let i = 0; i < bytes.length; i++) {
        bytes[i] = Math.floor(Math.random() * 256)
      }
      bytes[0] ^= Date.now() & 0xff
    }
    salt = Array.from(bytes).map((b) => b.toString(16).padStart(2, '0')).join('')
    localStorage.setItem(FINGERPRINT_SALT_KEY, salt)
  }
  return salt
}

/**
 * Canvas 渲染差异（不同设备/显卡/字体环境结果不同）
 *
 * <p> 注意：直接对整个 canvas 的 base64 取哈希在不同时刻可能不稳定（字体加载、抗锯齿等微差）， </p>
 * <p> 这里改为「量化取样 + 滚动校验和」，既能区分设备，又保证同一浏览器多次计算结果一致。 </p>
 */
async function canvasFingerprint() {
  try {
    // 等字体就绪后再绘制，避免字体加载前后渲染差异导致指纹漂移
    if (document.fonts && document.fonts.ready) {
      await document.fonts.ready
    }
    const canvas = document.createElement('canvas')
    canvas.width = 220
    canvas.height = 40
    const ctx = canvas.getContext('2d')
    ctx.textBaseline = 'top'
    ctx.font = '14px Arial'
    ctx.fillStyle = '#f60'
    ctx.fillRect(10, 5, 60, 20)
    ctx.fillStyle = '#069'
    ctx.fillText('Pkuse-Token 用户中心', 2, 15)
    ctx.strokeStyle = 'rgba(102, 204, 0, 0.7)'
    ctx.arc(50, 20, 18, 0, Math.PI * 2)
    ctx.stroke()

    // 量化取样：只取每 4 个像素中通道的高 4 位，降低渲染微差带来的抖动
    const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data
    let hash = 0x811c9dc5
    for (let i = 0; i < data.length; i += 4) {
      const quantized = (data[i] >> 4) | (data[i + 1] >> 4 & 0x0f) << 4
      hash = (Math.imul(hash ^ quantized, 0x01000193) >>> 0)
    }
    return 'cv' + hash.toString(16)
  } catch (e) {
    return 'no-canvas'
  }
}

/** SHA-256（localhost 属安全上下文，crypto.subtle 可用；不可用时退化为简单哈希） */
async function sha256(text) {
  try {
    const data = new TextEncoder().encode(text)
    const digest = await crypto.subtle.digest('SHA-256', data)
    return Array.from(new Uint8Array(digest))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
  } catch (e) {
    // 退化实现：FNV-1a
    let hash = 0x811c9dc5
    for (let i = 0; i < text.length; i++) {
      hash ^= text.charCodeAt(i)
      hash = Math.imul(hash, 0x01000193) >>> 0
    }
    return hash.toString(16).padStart(8, '0').repeat(4)
  }
}
