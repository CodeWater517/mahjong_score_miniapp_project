<template>
  <!-- 房间战绩预留页：当前版本还没有图表，先保留入口和说明。 -->
  <view class="page stack">
    <view class="page-title">房间战绩</view>
    <BaseCard>
      <view class="muted">房间维度图表预留页，当前版本可从历史局和房间排行查看完整明细。</view>
    </BaseCard>
  </view>
</template>

<script setup>
// 这里只用到通用卡片组件，后续加图表时可以继续扩展。
import BaseCard from '@/components/BaseCard.vue'
</script>
