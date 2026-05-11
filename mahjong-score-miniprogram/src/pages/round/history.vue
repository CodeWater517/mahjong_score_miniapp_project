<template>
  <!-- 历史局页：列出已经结算的每一局，点击可进入详情和修改。 -->
  <view class="page stack">
    <view class="between">
      <view class="page-title">历史局</view>
      <BaseButton text="刷新" type="plain" @click="load" />
    </view>
    <BaseCard v-if="!history.length">
      <view class="muted">暂无历史局</view>
    </BaseCard>
    <BaseCard v-for="round in history" :key="round.roundId" @tap="goDetail(round.roundId)">
      <view class="between">
        <view>
          <view class="round-title">第 {{ round.roundNo }} 局</view>
          <view class="muted">{{ formatTime(round.settledAt) }}</view>
        </view>
        <view class="summary">
          <text v-for="item in round.summary" :key="item.userId" class="summary-item">{{ item.nickname }} {{ scoreText(item.netScore) }}</text>
        </view>
      </view>
    </BaseCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import { useRoomStore } from '@/stores/room'
import { formatTime, scoreText } from '@/utils/format'

const roomStore = useRoomStore()
const roomId = ref('')
// history 保存在 room store 中，当前页面只负责展示。
const history = computed(() => roomStore.history)

// 按当前 roomId 从后端加载历史局。
const load = () => roomStore.loadHistory(roomId.value)

// 读取路由参数。
onLoad((query) => {
  roomId.value = query.roomId
})

// 每次显示页面都刷新，避免详情页修改后列表还是旧数据。
onShow(load)

// 进入某一局详情。
const goDetail = (roundId) => uni.navigateTo({ url: `/pages/round/detail?roundId=${roundId}` })
</script>

<style lang="scss" scoped>
/* 历史局标题使用更粗的字重，和右侧摘要区分开。 */
.round-title {
  font-size: 30rpx;
  font-weight: 800;
}

/* 摘要靠右竖排，方便快速看每个玩家本局净分。 */
.summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}

.summary-item {
  font-size: 24rpx;
}
</style>
