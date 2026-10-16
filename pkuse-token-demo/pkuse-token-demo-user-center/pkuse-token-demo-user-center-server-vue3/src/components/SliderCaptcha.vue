<template>
  <div class="slider-captcha">
    <div
      ref="boxRef"
      class="captcha-box"
      :style="{ width: displayWidth + 'px', height: displayHeight + 'px' }"
    >
      <img v-if="captcha" class="captcha-bg" :src="captcha.backgroundImage" :style="{ width: displayWidth + 'px', height: displayHeight + 'px' }" />
      <img
        v-if="captcha"
        class="captcha-piece"
        :src="captcha.templateImage"
        :style="pieceStyle"
      />
      <div v-if="loading" class="captcha-mask">加载中…</div>
    </div>

    <div class="slider-bar" :class="{ active: dragging, verified }">
      <div class="slider-fill" :style="{ width: (displayPieceX + pieceWidth / 2) + 'px' }"></div>
      <div
        class="slider-handle"
        :style="{ left: displayPieceX + 'px' }"
        @mousedown.prevent="startDrag"
        @touchstart.prevent="startDrag"
      >
        <span v-if="verified">✓</span>
        <span v-else-if="!dragging">»</span>
        <span v-else>…</span>
      </div>
      <div class="slider-text">{{ verified ? '验证通过，请点击登录' : dragging ? '松开完成验证' : '按住滑块拖动' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'

const emit = defineEmits(['success'])
const props = defineProps({
  // 背景图渲染的最大宽度（px）
  maxWidth: { type: Number, default: 320 }
})

const captcha = ref(null)
const loading = ref(false)
const dragging = ref(false)
/** 本次滑块是否已通过（通过后禁止再次拖动，直到换一张） */
const verified = ref(false)
const boxRef = ref(null)

// 后端原始坐标系下的尺寸（校验按该坐标系计算百分比）
const originWidth = ref(0)
const originHeight = ref(0)
const originPieceWidth = ref(0)
// 拼图块的原始宽高比（用于按比例渲染）
const pieceRatio = ref(1)

// 渲染尺寸（按比例缩放到 maxWidth）
const displayWidth = ref(props.maxWidth)
const displayHeight = ref(120)
const pieceWidth = ref(0)

// 拼图当前渲染位置（相对验证码框左上角）
const displayPieceX = ref(0)
const displayPieceY = ref(0)

// 拖动过程中的轨迹
let startX = 0
let startY = 0
let startPieceX = 0
let startPieceY = 0
let startTime = 0
let trackList = []

const pieceStyle = computed(() => ({
  left: displayPieceX.value + 'px',
  top: displayPieceY.value + 'px',
  width: pieceWidth.value + 'px'
}))

onMounted(reload)

/** 拉取一张新验证码 */
async function reload() {
  loading.value = true
  dragging.value = false
  verified.value = false
  try {
    const res = await request.get('/api/auth/captcha')
    if (res.code !== 200 || !res.data) {
      return
    }
    captcha.value = res.data
    originWidth.value = res.data.backgroundImageWidth
    originHeight.value = res.data.backgroundImageHeight
    originPieceWidth.value = res.data.templateImageWidth
    pieceRatio.value = res.data.templateImageHeight / res.data.templateImageWidth

    // 按最大宽度等比缩放渲染
    const scale = Math.min(1, props.maxWidth / originWidth.value)
    displayWidth.value = Math.round(originWidth.value * scale)
    displayHeight.value = Math.round(originHeight.value * scale)
    pieceWidth.value = Math.round(originPieceWidth.value * scale)

    displayPieceX.value = 0
    displayPieceY.value = 0
  } finally {
    loading.value = false
  }
}

function startDrag(e) {
  // 已通过 / 加载中 / 未成功获取验证码时不允许拖动
  if (!captcha.value || loading.value || verified.value) {
    return
  }
  const point = e.touches ? e.touches[0] : e
  dragging.value = true
  startX = point.clientX
  startY = point.clientY
  startPieceX = displayPieceX.value
  startPieceY = displayPieceY.value
  startTime = Date.now()
  trackList = [{ x: displayPieceX.value, y: displayPieceY.value, t: 0, type: 'move' }]

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
}

/** 拖动中：拼图块跟随指针移动（X、Y 均可微调，校验以 X 为准） */
function onDrag(e) {
  if (!dragging.value) {
    return
  }
  if (e.cancelable) {
    e.preventDefault()
  }
  const point = e.touches ? e.touches[0] : e
  const dx = point.clientX - startX
  const dy = point.clientY - startY
  const maxX = Math.max(0, displayWidth.value - pieceWidth.value)
  displayPieceX.value = clamp(startPieceX + dx, 0, maxX)
  displayPieceY.value = clamp(startPieceY + dy, 0, Math.max(0, displayHeight.value - pieceWidth.value * pieceRatio.value))
  trackList.push({ x: displayPieceX.value, y: displayPieceY.value, t: Date.now() - startTime, type: 'move' })
}

function endDrag() {
  if (!dragging.value) {
    return
  }
  dragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)

  const bx = displayPieceX.value
  const by = displayPieceY.value
  const scale = displayWidth.value / originWidth.value
  const stopTime = Date.now()

  // 组装 tianai 协议的校验数据（换算回后端原始坐标系）
  const track = {
    bgImageWidth: originWidth.value,
    bgImageHeight: originHeight.value,
    templateImageWidth: originPieceWidth.value,
    templateImageHeight: Math.round(originPieceWidth.value * pieceRatio.value),
    startTime,
    stopTime,
    left: Math.round(bx / scale),
    top: Math.round(by / scale),
    trackList: trackList.map((p) => ({ ...p, x: Math.round(p.x / scale), y: Math.round(p.y / scale) })),
    data: captcha.value.data
  }
  // 只把验证结果交给父组件（由用户手动点「登录」提交），本组件不自动提交
  verified.value = true
  emit('success', { captchaId: captcha.value.id, track })
}

function clamp(value, min, max) {
  return value < min ? min : value > max ? max : value
}

defineExpose({ reload })
</script>

<style scoped>
.slider-captcha {
  width: 100%;
}

.captcha-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.captcha-box {
  position: relative;
  overflow: hidden;
  border-radius: 6px;
  background-color: #f5f7fa;
  user-select: none;
}

.captcha-bg {
  display: block;
}

.captcha-piece {
  position: absolute;
  left: 0;
  top: 0;
  filter: drop-shadow(0 0 3px rgba(0, 0, 0, 0.45));
}

.captcha-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.7);
  color: #909399;
  font-size: 12px;
}

.slider-bar {
  position: relative;
  height: 40px;
  margin-top: 10px;
  border-radius: 6px;
  background-color: #f1f3f6;
  overflow: hidden;
}

.slider-bar.active {
  background-color: #e8f2ff;
}

.slider-bar.verified {
  background-color: #e1f3d8;
}

.slider-bar.verified .slider-handle {
  background-color: #67c23a;
}

.slider-bar.verified .slider-text {
  color: #529b2e;
  font-weight: 600;
}

.slider-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background-color: rgba(64, 158, 255, 0.18);
}

.slider-handle {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background-color: #409eff;
  color: #fff;
  font-size: 18px;
  cursor: grab;
  transition: left 0.05s linear;
}

.slider-handle:active {
  cursor: grabbing;
}

.slider-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
  pointer-events: none;
}
</style>
