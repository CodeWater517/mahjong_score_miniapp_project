<template>
  <!-- 计分页：展示当前排行、本轮提交状态，以及房主的代交/关闭等操作。 -->
  <view class="page stack">
    <view class="between">
      <view>
        <view class="page-title">{{ snapshot?.roomName || '房间计分' }}</view>
        <view class="muted">房间号 {{ snapshot?.roomCode }} · 第 {{ currentRound?.roundNo || '-' }} 局</view>
      </view>
      <text class="tag">{{ snapshot?.status }}</text>
    </view>

    <BaseCard>
      <view class="section-title">当前排行</view>
      <RankList :items="snapshot?.rankList || []" />
    </BaseCard>

    <BaseCard>
      <view class="section-title">本轮提交</view>
      <view v-for="participant in currentRound?.participants || []" :key="participant.userId" class="submit-row">
        <view>
          <view class="name">{{ participant.nickname }}</view>
          <view class="muted">{{ participant.payments?.length ? participant.payments.map((p) => `输给${p.toNickname}${p.score}`).join('，') : '不输不赢' }}</view>
        </view>
        <SubmitStatusTag :status="participant.submitStatus" />
      </view>
    </BaseCard>

    <BaseCard class="stack">
      <BaseButton text="编辑我的提交" @click="goSubmit()" />
      <view class="grid-2">
        <BaseButton text="历史局" type="plain" @click="goHistory" />
        <BaseButton text="房间排行" type="plain" @click="goRank" />
      </view>
    </BaseCard>

    <BaseCard v-if="isOwner" class="stack">
      <view class="section-title">房主操作</view>
      <view v-for="participant in pendingParticipants" :key="participant.userId" class="owner-row">
        <text>{{ participant.nickname }}</text>
        <view class="row actions">
          <BaseButton text="代交" type="plain" @click="goSubmit(participant.userId)" />
          <BaseButton text="不输不赢" type="plain" @click="force(participant.userId)" />
        </view>
      </view>
      <view class="grid-2">
        <BaseButton text="撤销上一局" type="plain" @click="undoLast" />
        <BaseButton text="关闭房间" type="danger" @click="close" />
      </view>
    </BaseCard>

    <RoundSummaryPopup :summary="roomStore.lastSummary" @close="roomStore.clearSummary()" />
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import RankList from '@/components/RankList.vue'
import RoundSummaryPopup from '@/components/RoundSummaryPopup.vue'
import SubmitStatusTag from '@/components/SubmitStatusTag.vue'
import { useRoomStore } from '@/stores/room'
import { useSocketStore } from '@/stores/socket'
import { useUserStore } from '@/stores/user'

const roomStore = useRoomStore()
const socketStore = useSocketStore()
const userStore = useUserStore()
const roomId = ref('')
// 轮询兜底：WebSocket 断线时也能定时刷新房间快照。
let pollingTimer = null

const snapshot = computed(() => roomStore.snapshot)
const currentRound = computed(() => roomStore.currentRound)
const isOwner = computed(() => snapshot.value?.ownerUserId === userStore.user?.userId)
// 房主只需要处理“还没提交，且不是自己”的玩家。
const pendingParticipants = computed(() => (currentRound.value?.participants || [])
  .filter((item) => item.submitStatus === 'PENDING' && item.userId !== userStore.user?.userId))

// 统一的快照刷新函数，WebSocket 消息和轮询都会用到。
const load = async () => roomStore.loadSnapshot(roomId.value)

// 页面加载：拿 roomId、确保用户资料、加载快照、连接 WebSocket，并启动定时轮询。
onLoad(async (query) => {
  roomId.value = query.roomId
  if (!userStore.user) await userStore.loadMe()
  await load()
  socketStore.connectRoom(roomId.value)
  pollingTimer = setInterval(load, 15000)
})

// 小程序页面卸载时关闭轮询和 WebSocket，避免离开页面后继续占用资源。
onUnload(() => {
  clearInterval(pollingTimer)
  socketStore.close()
})

// Vue 组件销毁时也清一次定时器，和 onUnload 互相兜底。
onBeforeUnmount(() => {
  clearInterval(pollingTimer)
})

// 跳到提交页；targetUserId 有值代表房主给别人代提交。
const goSubmit = (targetUserId) => {
  const target = targetUserId ? `&targetUserId=${targetUserId}` : ''
  uni.navigateTo({ url: `/pages/round/submit?roomId=${roomId.value}&roundId=${currentRound.value.roundId}${target}` })
}
const goHistory = () => uni.navigateTo({ url: `/pages/round/history?roomId=${roomId.value}` })
const goRank = () => uni.navigateTo({ url: `/pages/room/rank?roomId=${roomId.value}` })
// 房主把某个未提交玩家标记为“不输不赢”。
const force = async (targetUserId) => {
  await roomStore.forceNeutral(currentRound.value.roundId, targetUserId)
  await load()
}
// 撤销上一局会触发后端重算。
const undoLast = async () => roomStore.undoLast(roomId.value)
// 关闭房间后回等待页，这样用户能看到 CLOSED 状态和重新打开按钮。
const close = async () => {
  await roomStore.close(roomId.value)
  uni.redirectTo({ url: `/pages/room/waiting?roomId=${roomId.value}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 提交状态行和房主待处理行共用同一种左右排版。 */
.submit-row,
.owner-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 78rpx;
  border-bottom: 1rpx solid $border-color;
}

/* 每组列表最后一行不画底边。 */
.submit-row:last-child,
.owner-row:last-child {
  border-bottom: 0;
}

.name {
  font-size: 28rpx;
  font-weight: 700;
}

.actions {
  gap: 10rpx;
}
</style>
