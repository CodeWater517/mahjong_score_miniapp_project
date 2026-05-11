<template>
  <!-- 创建房间页：选择人数和初始分，提交后房主自动进入东位。 -->
  <view class="page stack">
    <view class="page-title">创建房间</view>
    <BaseCard class="stack">
      <view class="section-title">房间人数</view>
      <view class="grid-2">
        <BaseButton v-for="count in [2, 3, 4]" :key="count" :text="`${count} 人房`" :type="playerCount === count ? 'primary' : 'plain'" @click="playerCount = count" />
      </view>
      <view class="section-title">初始分</view>
      <input class="input" type="number" v-model="initialScore" />
      <BaseButton text="创建并进入东位" @click="submit" />
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useRoomStore } from '@/stores/room'

const roomStore = useRoomStore()
// 默认 4 人房，初始分为 0。ref 表示页面里的响应式数据。
const playerCount = ref(4)
const initialScore = ref(0)

// 调用后端创建房间，成功后跳到等待页。
const submit = async () => {
  const room = await roomStore.create({ playerCount: playerCount.value, initialScore: Number(initialScore.value) || 0 })
  uni.redirectTo({ url: `/pages/room/waiting?roomId=${room.roomId}` })
}
</script>
