<template>
  <!-- 个人战绩页：按全部/本月/本周查看自己的汇总数据和最近房间。 -->
  <view class="page stack">
    <view class="between">
      <view class="page-title">个人战绩</view>
      <picker :range="ranges" range-key="label" :value="rangeIndex" @change="changeRange">
        <view class="tag">{{ ranges[rangeIndex].label }}</view>
      </picker>
    </view>

    <BaseCard>
      <view class="stats-grid">
        <view v-for="item in statItems" :key="item.label" class="stat-item">
          <view class="stat-value">{{ item.value }}</view>
          <view class="muted">{{ item.label }}</view>
        </view>
      </view>
    </BaseCard>

    <BaseCard>
      <view class="section-title">最近房间</view>
      <view v-if="!stats.recentRooms?.length" class="muted">暂无房间记录</view>
      <view v-for="room in stats.recentRooms" :key="room.roomId" class="between room-row">
        <view>
          <view class="room-title">{{ room.roomName }}</view>
          <view class="muted">第 {{ room.myRank }} 名 · {{ room.rounds }} 局</view>
        </view>
        <view>{{ scoreText(room.myScore) }}</view>
      </view>
    </BaseCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import BaseCard from '@/components/BaseCard.vue'
import { getMyStats } from '@/api/stats'
import { scoreText } from '@/utils/format'

const ranges = [
  // label 是页面显示，value 是传给后端的查询范围。
  { label: '全部', value: 'ALL' },
  { label: '本月', value: 'MONTH' },
  { label: '本周', value: 'WEEK' },
]
const rangeIndex = ref(0)
const stats = ref({})

// 把后端返回的原始字段整理成卡片网格需要的 label/value 数组。
const statItems = computed(() => [
  { label: '房间数', value: stats.value.totalRooms ?? 0 },
  { label: '总局数', value: stats.value.totalRounds ?? 0 },
  { label: '总净分', value: scoreText(stats.value.totalScore ?? 0) },
  { label: '均分', value: stats.value.avgRoundScore ?? 0 },
  { label: '最高单局', value: scoreText(stats.value.highestRoundScore ?? 0) },
  { label: '最低单局', value: scoreText(stats.value.lowestRoundScore ?? 0) },
  { label: '单局胜率', value: `${Math.round((stats.value.roundWinRate || 0) * 100)}%` },
  { label: '房间胜率', value: `${Math.round((stats.value.roomWinRate || 0) * 100)}%` },
  { label: '盈亏比', value: stats.value.profitLossRatio || '--' },
])

// 根据当前选择的范围加载个人战绩。
const load = async () => { stats.value = await getMyStats(ranges[rangeIndex.value].value) }
// picker 变化时更新下标并重新拉数据。
const changeRange = async (event) => {
  rangeIndex.value = Number(event.detail.value)
  await load()
}

// 每次显示页面都刷新，保证刚打完一局后数据能更新。
onShow(load)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 统计项三列排列，小屏也能保持整齐。 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20rpx;
}

/* 单个统计项的高度固定，避免不同文字造成卡片抖动。 */
.stat-item {
  min-height: 104rpx;
}

/* 统计值用主色和大字突出。 */
.stat-value {
  font-size: 34rpx;
  font-weight: 850;
  color: $primary-color;
}

/* 最近房间列表行。 */
.room-row {
  min-height: 82rpx;
  border-bottom: 1rpx solid $border-color;
}

.room-row:last-child {
  border-bottom: 0;
}

.room-title {
  font-size: 28rpx;
  font-weight: 700;
}
</style>
