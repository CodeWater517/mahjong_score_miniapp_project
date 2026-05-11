<template>
  <!-- 加入房间页：先按 6 位房间号查询，再进入选座页。 -->
  <view class="page stack">
    <view class="page-title">加入房间</view>
    <BaseCard class="stack">
      <input class="input" type="number" maxlength="6" placeholder="输入 6 位房间号" v-model="roomCode" />
      <BaseButton text="查询房间" @click="search" />
    </BaseCard>
    <BaseCard v-if="room">
      <view class="between">
        <view>
          <view class="section-title">{{ room.roomName }}</view>
          <view class="muted">{{ room.status }} · {{ room.roomCode }}</view>
        </view>
        <BaseButton text="选座" @click="goSeat" />
      </view>
      <view class="muted">空位 {{ room.emptySeats?.length || 0 }} 个</view>
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useRoomStore } from '@/stores/room'

const roomStore = useRoomStore()
const roomCode = ref('')
// 查询成功后的房间信息，null 表示还没有查到房间。
const room = ref(null)

// 支持分享卡片带 roomCode 进来，进入页面后自动查询房间。
onLoad(async (query) => {
  if (query.roomCode) {
    roomCode.value = query.roomCode
    await search()
  }
})

// 查询前先做简单格式校验，避免无意义请求。
const search = async () => {
  if (!/^\d{6}$/.test(roomCode.value)) {
    uni.showToast({ title: '请输入 6 位房间号', icon: 'none' })
    return
  }
  room.value = await roomStore.findByCode(roomCode.value)
}

// 只有存在空位时才允许进入选座页。
const goSeat = () => {
  if (!room.value?.emptySeats?.length) {
    uni.showToast({ title: '暂无空位', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/room/select-seat?roomId=${room.value.roomId}` })
}
</script>
