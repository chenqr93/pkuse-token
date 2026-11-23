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

    <div class="slider-bar" :class="{ active: dragging, verified: verified }">
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

<script>
import request from '../utils/request'

export default {
  name: 'SliderCaptcha',
  props: {
    // 背景图渲染的最大宽度（px）
    maxWidth: { type: Number, default: 320 }
  },
  data() {
    return {
      captcha: null,
      loading: false,
      dragging: false,
      /** 本次滑块是否已通过（通过后禁止再次拖动，直到换一张） */
      verified: false,
      // 后端原始坐标系下的尺寸（校验按该坐标系计算百分比）
      originWidth: 0,
      originHeight: 0,
      originPieceWidth: 0,
      // 拼图块的原始宽高比（用于按比例渲染）
      pieceRatio: 1,
      // 渲染尺寸（按比例缩放到 maxWidth）
      displayWidth: this.maxWidth,
      displayHeight: 120,
      pieceWidth: 0,
      // 拼图当前渲染位置（相对验证码框左上角）
      displayPieceX: 0,
      displayPieceY: 0
    }
  },
  computed: {
    pieceStyle() {
      return {
        left: this.displayPieceX + 'px',
        top: this.displayPieceY + 'px',
        width: this.pieceWidth + 'px'
      }
    }
  },
  created() {
    // 拖动过程中的轨迹（无需响应式）
    this.startX = 0
    this.startY = 0
    this.startPieceX = 0
    this.startPieceY = 0
    this.startTime = 0
    this.trackList = []
  },
  mounted() {
    this.reload()
  },
  beforeDestroy() {
    this.removeDragListeners()
  },
  methods: {
    /** 拉取一张新验证码 */
    async reload() {
      this.loading = true
      this.dragging = false
      this.verified = false
      try {
        const res = await request.get('/api/auth/captcha')
        if (res.code !== 200 || !res.data) {
          return
        }
        this.captcha = res.data
        this.originWidth = res.data.backgroundImageWidth
        this.originHeight = res.data.backgroundImageHeight
        this.originPieceWidth = res.data.templateImageWidth
        this.pieceRatio = res.data.templateImageHeight / res.data.templateImageWidth

        // 按最大宽度等比缩放渲染
        const scale = Math.min(1, this.maxWidth / this.originWidth)
        this.displayWidth = Math.round(this.originWidth * scale)
        this.displayHeight = Math.round(this.originHeight * scale)
        this.pieceWidth = Math.round(this.originPieceWidth * scale)

        this.displayPieceX = 0
        this.displayPieceY = 0
      } finally {
        this.loading = false
      }
    },

    startDrag(e) {
      // 已通过 / 加载中 / 未成功获取验证码时不允许拖动
      if (!this.captcha || this.loading || this.verified) {
        return
      }
      const point = e.touches ? e.touches[0] : e
      this.dragging = true
      this.startX = point.clientX
      this.startY = point.clientY
      this.startPieceX = this.displayPieceX
      this.startPieceY = this.displayPieceY
      this.startTime = Date.now()
      this.trackList = [{ x: this.displayPieceX, y: this.displayPieceY, t: 0, type: 'move' }]

      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.endDrag)
      document.addEventListener('touchmove', this.onDrag, { passive: false })
      document.addEventListener('touchend', this.endDrag)
    },

    /** 拖动中：拼图块跟随指针移动（X、Y 均可微调，校验以 X 为准） */
    onDrag(e) {
      if (!this.dragging) {
        return
      }
      if (e.cancelable) {
        e.preventDefault()
      }
      const point = e.touches ? e.touches[0] : e
      const dx = point.clientX - this.startX
      const dy = point.clientY - this.startY
      const maxX = Math.max(0, this.displayWidth - this.pieceWidth)
      this.displayPieceX = this.clamp(this.startPieceX + dx, 0, maxX)
      this.displayPieceY = this.clamp(
        this.startPieceY + dy,
        0,
        Math.max(0, this.displayHeight - this.pieceWidth * this.pieceRatio)
      )
      this.trackList.push({
        x: this.displayPieceX,
        y: this.displayPieceY,
        t: Date.now() - this.startTime,
        type: 'move'
      })
    },

    endDrag() {
      if (!this.dragging) {
        return
      }
      this.dragging = false
      this.removeDragListeners()

      const bx = this.displayPieceX
      const by = this.displayPieceY
      const scale = this.displayWidth / this.originWidth
      const stopTime = Date.now()

      // 组装 tianai 协议的校验数据（换算回后端原始坐标系）
      const track = {
        bgImageWidth: this.originWidth,
        bgImageHeight: this.originHeight,
        templateImageWidth: this.originPieceWidth,
        templateImageHeight: Math.round(this.originPieceWidth * this.pieceRatio),
        startTime: this.startTime,
        stopTime: stopTime,
        left: Math.round(bx / scale),
        top: Math.round(by / scale),
        trackList: this.trackList.map((p) => ({
          ...p,
          x: Math.round(p.x / scale),
          y: Math.round(p.y / scale)
        })),
        data: this.captcha.data
      }
      // 只把验证结果交给父组件（由用户手动点「登录」提交），本组件不自动提交
      this.verified = true
      this.$emit('success', { captchaId: this.captcha.id, track: track })
    },

    removeDragListeners() {
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.endDrag)
      document.removeEventListener('touchmove', this.onDrag)
      document.removeEventListener('touchend', this.endDrag)
    },

    clamp(value, min, max) {
      return value < min ? min : value > max ? max : value
    }
  }
}
</script>

<style lang="less" scoped>
@import '../styles/tokens.less';

.slider-captcha {
  width: 100%;
}

.captcha-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: @pd-color-text-muted;
}

.captcha-box {
  position: relative;
  overflow: hidden;
  border-radius: 6px;
  background-color: @pd-color-bg;
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
  background-color: fade(@pd-color-white, 70%);
  color: @pd-color-text-muted;
  font-size: 12px;
}

.slider-bar {
  position: relative;
  height: 40px;
  margin-top: 10px;
  border-radius: 6px;
  background-color: @pd-color-border;
  overflow: hidden;
}

.slider-bar.active {
  background-color: @pd-tag-info-bg;
}

.slider-bar.verified {
  background-color: @pd-tag-success-bg;
}

.slider-bar.verified .slider-handle {
  background-color: @pd-color-success;
}

.slider-bar.verified .slider-text {
  color: @pd-tag-success;
  font-weight: 600;
}

.slider-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background-color: fade(@pd-color-selected, 18%);
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
  background-color: @pd-color-selected;
  color: @pd-color-white;
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
  color: @pd-color-text-muted;
  font-size: 13px;
  pointer-events: none;
}
</style>
