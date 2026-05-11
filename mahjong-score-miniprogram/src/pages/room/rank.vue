<template>
  <!-- 房间排行页：展示当前房间内玩家按总净分排序的列表。 -->
  <view class="page stack">
    <view class="page-title">房间排行</view>
    <BaseCard>
      <RankList :items="rankList" />
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import BaseCard from '@/components/BaseCard.vue'
import RankList from '@/components/RankList.vue'
import { useRoomStore } from '@/stores/room'

const roomStore = useRoomStore()
const roomId = ref('')
const rankList = ref([])

// 单独的加载函数，方便 onShow 和手动刷新复用。
const load = async () => { rankList.value = await roomStore.loadRank(roomId.value) }
// onLoad 只负责读取路由参数；onShow 再加载数据，返回本页时也会刷新。
onLoad((query) => { roomId.value = query.roomId })
onShow(load)
</script>
