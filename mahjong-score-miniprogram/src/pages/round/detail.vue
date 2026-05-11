<template>
  <!-- 历史详情页：展示本局结果，也允许房主修改输分明细并触发后端重算。 -->
  <view class="page stack">
    <view class="page-title">第 {{ detail?.roundNo || '-' }} 局</view>

    <BaseCard>
      <view class="section-title">本局结果</view>
      <view v-for="item in detail?.summary || []" :key="item.userId" class="between result-row">
        <text>{{ item.nickname }}</text>
        <text :class="{ positive: item.netScore > 0, negative: item.netScore < 0 }">{{ scoreText(item.netScore) }}</text>
      </view>
    </BaseCard>

    <BaseCard class="stack">
      <view class="section-title">输分明细</view>
      <view v-for="(row, index) in editPayments" :key="index" class="history-row">
        <picker :range="players" range-key="label" :value="playerIndex(row.fromUserId)" @change="changeFrom(index, $event)">
          <view class="picker">{{ playerName(row.fromUserId) }}</view>
        </picker>
        <picker :range="players" range-key="label" :value="playerIndex(row.toUserId)" @change="changeTo(index, $event)">
          <view class="picker">{{ playerName(row.toUserId) }}</view>
        </picker>
        <input class="score-input" type="number" v-model="row.score" />
        <button class="remove" @tap="editPayments.splice(index, 1)">×</button>
      </view>
      <BaseButton text="添加明细" type="plain" @click="addPayment" />
      <BaseButton text="保存修改并重算" @click="save" />
      <BaseButton text="删除本局" type="danger" @click="remove" />
    </BaseCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import { useRoomStore } from '@/stores/room'
import { scoreText } from '@/utils/format'

const roomStore = useRoomStore()
const roundId = ref('')
// detail 是后端返回的本局完整详情。
const detail = ref(null)
// editPayments 是可编辑的输分明细副本，保存前不会影响后端。
const editPayments = ref([])
// picker 需要数组格式，这里把 summary 中的玩家转换成可选项。
const players = computed(() => (detail.value?.summary || []).map((item) => ({ userId: item.userId, label: item.nickname })))

// 进入页面后按 roundId 拉详情。
onLoad(async (query) => {
  roundId.value = query.roundId
  await load()
})

// 加载详情，并把 payments 转成表单可编辑的数据。
const load = async () => {
  detail.value = await roomStore.detail(roundId.value)
  editPayments.value = (detail.value.payments || []).map((item) => ({
    fromUserId: item.fromUserId,
    toUserId: item.toUserId,
    score: item.score,
    remark: item.remark || '',
  }))
}

// picker 使用下标，所以这里提供用户 ID 和下标/名称之间的转换。
const playerIndex = (userId) => Math.max(0, players.value.findIndex((item) => item.userId === userId))
const playerName = (userId) => players.value.find((item) => item.userId === userId)?.label || '玩家'
// 修改某一行的输家。
const changeFrom = (index, event) => { editPayments.value[index].fromUserId = players.value[Number(event.detail.value)]?.userId }
// 修改某一行的赢家。
const changeTo = (index, event) => { editPayments.value[index].toUserId = players.value[Number(event.detail.value)]?.userId }
// 新增一条明细时，默认使用前两个玩家，分数给 1，用户再自行修改。
const addPayment = () => editPayments.value.push({ fromUserId: players.value[0]?.userId, toUserId: players.value[1]?.userId, score: 1, remark: '' })
// 保存会覆盖该局输分明细，并让后端重新计算房间分数和统计。
const save = async () => {
  await roomStore.updateHistory(roundId.value, editPayments.value.map((item) => ({ ...item, score: Number(item.score) })))
  uni.navigateBack()
}
// 删除是逻辑删除历史局，后端同样会重算后续统计。
const remove = async () => {
  await roomStore.deleteHistory(roundId.value)
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 本局结果每行显示玩家和净分。 */
.result-row {
  min-height: 56rpx;
  font-size: 28rpx;
}

/* 明细编辑区：输家、赢家、分数、删除按钮四列。 */
.history-row {
  display: grid;
  grid-template-columns: 1fr 1fr 112rpx 56rpx;
  gap: 10rpx;
  align-items: center;
}

/* picker 和输入框保持一致的外观。 */
.picker,
.score-input {
  min-height: 72rpx;
  padding: 0 12rpx;
  border: 1rpx solid $border-color;
  border-radius: 8rpx;
  box-sizing: border-box;
  font-size: 24rpx;
  display: flex;
  align-items: center;
}

/* 删除按钮是圆形，适配表格最后一列。 */
.remove {
  width: 56rpx;
  height: 56rpx;
  padding: 0;
  border-radius: 50%;
  background: #f3f4f6;
  color: $danger-color;
  line-height: 56rpx;
}

.remove::after {
  border: 0;
}

.positive {
  color: $success-color;
}

.negative {
  color: $danger-color;
}
</style>
