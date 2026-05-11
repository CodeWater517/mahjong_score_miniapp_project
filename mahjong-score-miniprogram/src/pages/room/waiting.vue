<template>
  <!-- 房间等待页：房主开始游戏、转让房主、踢人；普通玩家查看座位和退出。 -->
  <view class="page stack">
    <view class="between">
      <view>
        <view class="page-title">{{ snapshot?.roomName || '房间' }}</view>
        <view class="muted">房间号 {{ snapshot?.roomCode }} · {{ snapshot?.status }}</view>
      </view>
      <BaseButton text="分享" type="plain" open-type="share" />
    </view>

    <view class="stack">
      <SeatCard v-for="seat in seats" :key="seat.seatId" :seat="seat" :show-actions="isOwner && !seat.empty && seat.currentUserId !== userStore.user?.userId">
        <template #actions>
          <BaseButton text="转房主" type="plain" @click="transfer(seat.currentUserId)" />
          <BaseButton text="踢出" type="danger" ghost @click="kick(seat.currentUserId)" />
        </template>
      </SeatCard>
    </view>

    <BaseCard class="stack">
      <BaseButton v-if="isOwner && snapshot?.status === 'WAITING'" text="开始游戏" :disabled="!full" @click="start" />
      <BaseButton v-if="snapshot?.status === 'PLAYING'" text="进入计分" @click="goPlay" />
      <BaseButton v-if="isOwner && snapshot?.status === 'CLOSED'" text="重新打开" @click="reopen" />
      <BaseButton text="退出房间" type="plain" @click="quit" />
    </BaseCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import SeatCard from '@/components/SeatCard.vue'
import { useRoomStore } from '@/stores/room'
import { useUserStore } from '@/stores/user'

const roomStore = useRoomStore()
const userStore = useUserStore()
const roomId = ref('')
// 页面主要依赖房间快照；用 computed 让模板自动跟随 store 更新。
const snapshot = computed(() => roomStore.snapshot)
const seats = computed(() => roomStore.seats)
// 判断当前登录用户是否是房主。
const isOwner = computed(() => snapshot.value?.ownerUserId === userStore.user?.userId)
// 满员才允许开始游戏。
const full = computed(() => seats.value.length > 0 && seats.value.every((seat) => !seat.empty))

// 页面进入时保存 roomId、确保用户资料存在、再加载房间快照。
onLoad(async (query) => {
  roomId.value = query.roomId
  if (!userStore.user) await userStore.loadMe()
  await roomStore.loadSnapshot(roomId.value)
})

// 微信分享配置：分享出去后，别人会先进入加入房间页并自动带上房间号。
onShareAppMessage(() => ({
  title: snapshot.value?.roomName || '麻将计分房间',
  path: `/pages/room/join?roomCode=${snapshot.value?.roomCode || ''}`,
}))

// 房主开始游戏后，后端会创建第一局提交轮次，然后页面进入计分页。
const start = async () => {
  await roomStore.start(roomId.value)
  goPlay()
}
const goPlay = () => uni.redirectTo({ url: `/pages/room/play?roomId=${roomId.value}` })
// 关闭后重新打开房间会创建新的开启段和新的提交轮次。
const reopen = async () => {
  await roomStore.reopen(roomId.value)
  goPlay()
}
// 房主相关操作直接交给 room store，store 会在成功后刷新快照。
const transfer = (targetUserId) => roomStore.transfer(roomId.value, targetUserId)
const kick = (targetUserId) => roomStore.kick(roomId.value, targetUserId)
// 退出房间后回到首页。
const quit = async () => {
  await roomStore.quit(roomId.value)
  uni.switchTab({ url: '/pages/home/index' })
}
</script>
