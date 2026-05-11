<template>
  <!-- 座位卡片：展示东南西北座位、当前玩家和座位分。 -->
  <BaseCard>
    <view class="between">
      <view>
        <view class="seat-title">{{ seat.displayName || seatLabel(seat.seatName) }}</view>
        <view class="muted">{{ seat.empty ? '空位' : (seat.roomNickname || seat.nickname || '玩家') }}</view>
      </view>
      <view class="score" :class="{ positive: seat.currentScore > 0, negative: seat.currentScore < 0 }">
        {{ scoreText(seat.currentScore) }}
      </view>
    </view>
    <!-- actions 插槽给父组件放“加入、转房主、踢出”等按钮。 -->
    <view v-if="showActions" class="actions">
      <slot name="actions" />
    </view>
  </BaseCard>
</template>

<script setup>
import BaseCard from './BaseCard.vue'
import { scoreText, seatLabel } from '@/utils/format'

// seat 是后端返回的 SeatResponse；showActions 控制是否显示底部操作区。
defineProps({
  seat: { type: Object, required: true },
  showActions: { type: Boolean, default: false },
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 座位名比普通文字更大，方便用户快速识别自己所在位置。 */
.seat-title {
  font-size: 32rpx;
  font-weight: 700;
}

/* 分数默认中性色，正负再分别套用 positive/negative。 */
.score {
  font-size: 34rpx;
  font-weight: 800;
  color: $text-secondary;
}

.positive {
  color: $success-color;
}

.negative {
  color: $danger-color;
}

/* 操作按钮横向排列，按钮内容由父组件通过 slot 提供。 */
.actions {
  margin-top: 18rpx;
  display: flex;
  gap: 12rpx;
}
</style>
