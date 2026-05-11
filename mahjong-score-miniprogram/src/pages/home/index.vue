<template>
  <!-- 首页：展示用户入口、最近房间、历史榜和月榜。 -->
  <view class="page stack">
    <view class="between">
      <view>
        <view class="page-title">麻将计分助手</view>
        <view class="muted">{{ userStore.user?.nickname || '微信用户' }}</view>
      </view>
      <BaseButton text="资料" type="plain" icon="✎" @click="goProfile" />
    </view>

    <view class="grid-2">
      <BaseButton text="创建房间" icon="+" @click="goCreate" />
      <BaseButton text="加入房间" type="plain" icon="#" @click="goJoin" />
    </view>

    <BaseCard>
      <view class="section-title">最近房间</view>
      <view v-if="!stats.recentRooms?.length" class="muted">暂无房间记录</view>
      <view v-for="room in stats.recentRooms" :key="room.roomId" class="room-row" @tap="goRoom(room)">
        <view>
          <view class="room-name">{{ room.roomName }}</view>
          <view class="muted">{{ room.status }} · {{ room.rounds }} 局</view>
        </view>
        <view class="room-score">{{ scoreText(room.myScore) }}</view>
      </view>
    </BaseCard>

    <BaseCard>
      <view class="section-title">历史总榜 Top 10</view>
      <RankList :items="ranking.totalRank || []" />
    </BaseCard>

    <BaseCard>
      <view class="section-title">月度榜 Top 10</view>
      <RankList :items="ranking.monthlyRank || []" />
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import RankList from '@/components/RankList.vue'
import { getHomeRanking } from '@/api/ranking'
import { getMyStats } from '@/api/stats'
import { useUserStore } from '@/stores/user'
import { scoreText } from '@/utils/format'

const userStore = useUserStore()
// ranking 保存首页两组排行榜，stats 保存个人概览和最近房间。
const ranking = ref({})
const stats = ref({})

// onShow 每次页面显示都会执行，适合刷新排行榜和最近房间这种可能变化的数据。
onShow(async () => {
  if (!userStore.user) await userStore.loadMe()
  ranking.value = await getHomeRanking()
  stats.value = await getMyStats('ALL')
})

// 以下是首页几个主要入口的跳转。
const goCreate = () => uni.navigateTo({ url: '/pages/room/create' })
const goJoin = () => uni.navigateTo({ url: '/pages/room/join' })
const goProfile = () => uni.navigateTo({ url: '/pages/user/profile' })
const goRoom = (room) => {
  // 正在计分的房间进入 play，等待中或关闭的房间进入 waiting。
  const page = room.status === 'PLAYING' ? 'play' : 'waiting'
  uni.navigateTo({ url: `/pages/room/${page}?roomId=${room.roomId}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 最近房间行：左侧房间信息，右侧当前用户在该房间的分数。 */
.room-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  border-bottom: 1rpx solid $border-color;
}

/* 最后一行不显示分隔线，让卡片底部更干净。 */
.room-row:last-child {
  border-bottom: 0;
}

.room-name {
  font-size: 28rpx;
  font-weight: 700;
}

/* 房间分数用主色强调。 */
.room-score {
  color: $primary-color;
  font-size: 30rpx;
  font-weight: 800;
}
</style>
