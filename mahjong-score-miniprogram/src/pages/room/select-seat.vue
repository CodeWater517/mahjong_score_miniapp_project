<template>
  <!-- 选座页：展示当前房间所有空位，用户点某个座位后加入。 -->
  <view class="page stack">
    <view class="page-title">选择座位</view>
    <input class="input" placeholder="本房间昵称，可选" v-model="roomNickname" />
    <SeatCard v-for="seat in emptySeats" :key="seat.seatId" :seat="seat" :show-actions="true">
      <template #actions>
        <BaseButton text="加入此座" @click="join(seat)" />
      </template>
    </SeatCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import SeatCard from '@/components/SeatCard.vue'
import { useRoomStore } from '@/stores/room'

const roomStore = useRoomStore()
const roomId = ref('')
const roomNickname = ref('')
// emptySeats 从房间快照里的 seats 派生，只保留空座位。
const emptySeats = computed(() => roomStore.seats.filter((seat) => seat.empty))

// 进入页面时根据 roomId 拉房间快照，这样才能知道哪些座位为空。
onLoad(async (query) => {
  roomId.value = query.roomId
  await roomStore.loadSnapshot(roomId.value)
})

// 加入座位后重新拉一次快照，根据房间状态决定进入等待页还是计分页。
const join = async (seat) => {
  await roomStore.join(roomId.value, { seatId: seat.seatId, roomNickname: roomNickname.value })
  const snapshot = await roomStore.loadSnapshot(roomId.value)
  uni.redirectTo({ url: `/pages/room/${snapshot.status === 'PLAYING' ? 'play' : 'waiting'}?roomId=${roomId.value}` })
}
</script>
