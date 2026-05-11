<template>
  <view class="rank-list">
    <!-- 没有数据时给用户一个空状态提示。 -->
    <view v-if="!items.length" class="muted empty">暂无排行</view>
    <!-- 每一行显示名次、昵称和分数。 -->
    <view v-for="item in items" :key="item.userId" class="rank-row">
      <view class="rank-no">{{ item.rank }}</view>
      <view class="rank-name">{{ item.nickname || '玩家' }}</view>
      <view class="rank-score" :class="{ positive: item.score > 0, negative: item.score < 0 }">{{ scoreText(item.score) }}</view>
    </view>
  </view>
</template>

<script setup>
import { scoreText } from '@/utils/format'

// items 由父组件传入，格式对应后端 RankingDtos.RankItem。
defineProps({
  items: { type: Array, default: () => [] },
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 列表本身只负责纵向排列，具体行内容在 rank-row 里布局。 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

/* 三列：名次、昵称、分数。 */
.rank-row {
  display: grid;
  grid-template-columns: 56rpx 1fr 120rpx;
  align-items: center;
  min-height: 64rpx;
  font-size: 28rpx;
}

/* 名次用主色突出，方便快速扫榜。 */
.rank-no {
  color: $primary-color;
  font-weight: 800;
}

/* 昵称可能很长，超出时省略，避免把分数挤掉。 */
.rank-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 分数右对齐，正负分更容易上下比较。 */
.rank-score {
  text-align: right;
  font-weight: 800;
}

.positive {
  color: $success-color;
}

.negative {
  color: $danger-color;
}

.empty {
  padding: 16rpx 0;
}
</style>
