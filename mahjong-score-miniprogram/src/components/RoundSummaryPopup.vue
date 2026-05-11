<template>
  <!-- summary 有值才显示弹窗；没有结算结果时整块不渲染。 -->
  <view v-if="summary" class="mask">
    <view class="popup">
      <view class="between">
        <view class="title">第 {{ summary.roundNo }} 局结果</view>
        <button class="close" @tap="$emit('close')">×</button>
      </view>
      <view class="summary-list">
        <view v-for="item in summary.summary" :key="item.userId" class="between summary-row">
          <text>{{ item.nickname || '玩家' }}</text>
          <text :class="{ positive: item.netScore > 0, negative: item.netScore < 0 }">{{ scoreText(item.netScore) }}</text>
        </view>
      </view>
      <BaseButton text="知道了" @click="$emit('close')" />
    </view>
  </view>
</template>

<script setup>
import BaseButton from './BaseButton.vue'
import { scoreText } from '@/utils/format'

// summary 来自 WebSocket 的 ROUND_SETTLED 消息，包含 roundNo 和 summary 列表。
defineProps({
  summary: { type: Object, default: null },
})

// 关闭动作交给父组件处理，父组件会清掉 roomStore.lastSummary。
defineEmits(['close'])
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 半透明遮罩盖住整个屏幕，把用户注意力聚焦到结算弹窗。 */
.mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, .42);
}

/* 底部弹窗，符合小程序里常见的结果确认交互。 */
.popup {
  width: 100%;
  padding: 28rpx;
  border-radius: 16rpx 16rpx 0 0;
  background: #fff;
  box-sizing: border-box;
}

.title {
  font-size: 34rpx;
  font-weight: 800;
}

/* 关闭按钮使用系统 button，但清掉默认边框。 */
.close {
  width: 56rpx;
  height: 56rpx;
  padding: 0;
  background: #f3f4f6;
  border-radius: 50%;
  line-height: 56rpx;
}

.close::after {
  border: 0;
}

.summary-list {
  margin: 24rpx 0;
}

.summary-row {
  min-height: 56rpx;
  font-size: 28rpx;
}

/* 结算列表中，正分和负分分别用成功色/危险色。 */
.positive {
  color: $success-color;
}

.negative {
  color: $danger-color;
}
</style>
