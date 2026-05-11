<template>
  <!-- 本轮提交页：玩家填写“我输给谁多少分”，房主代交时也是复用这个页面。 -->
  <view class="page stack">
    <view>
      <view class="page-title">{{ targetUserId ? '房主代提交' : '本轮提交' }}</view>
      <view class="muted">填写“输给谁多少分”，空列表表示不输不赢。</view>
    </view>

    <BaseCard class="stack">
      <ScoreInputRow
        v-for="(row, index) in payments"
        :key="index"
        v-model="payments[index]"
        :targets="targets"
        @remove="payments.splice(index, 1)"
      />
      <BaseButton text="添加输分记录" type="plain" @click="addRow" />
      <BaseButton text="提交本轮" @click="submit" />
    </BaseCard>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import ScoreInputRow from '@/components/ScoreInputRow.vue'
import { useRoomStore } from '@/stores/room'
import { useUserStore } from '@/stores/user'

const roomStore = useRoomStore()
const userStore = useUserStore()
const roomId = ref('')
const roundId = ref('')
// targetUserId 有值表示房主正在给指定玩家代提交；为空表示当前登录用户自己提交。
const targetUserId = ref('')
const payments = ref([])

// 本次提交者的用户 ID：房主代交用 targetUserId，普通提交用当前登录用户。
const submitterId = computed(() => Number(targetUserId.value || userStore.user?.userId))
// 当前轮参与者来自房间快照。
const participants = computed(() => roomStore.currentRound?.participants || [])
// 可选择的收分目标不能包含提交者自己。
const targets = computed(() => participants.value
  .filter((item) => item.userId !== submitterId.value)
  .map((item) => ({ userId: item.userId, label: item.nickname })))

// 进入页面时加载房间快照，并把已有提交内容回填到表单中，方便修改。
onLoad(async (query) => {
  roomId.value = query.roomId
  roundId.value = query.roundId
  targetUserId.value = query.targetUserId || ''
  if (!userStore.user) await userStore.loadMe()
  await roomStore.loadSnapshot(roomId.value)
  const submitter = participants.value.find((item) => item.userId === submitterId.value)
  payments.value = (submitter?.payments || []).map((item) => ({
    toUserId: item.toUserId,
    score: item.score,
    remark: item.remark,
  }))
})

// 新增一条输分记录，默认选择第一个可收分玩家。
const addRow = () => {
  payments.value.push({ toUserId: targets.value[0]?.userId, score: undefined, remark: '' })
}

// 提交前过滤掉未选玩家或分数无效的行，然后根据是否代交调用不同接口。
const submit = async () => {
  const data = payments.value
    .filter((item) => item.toUserId && Number(item.score) > 0)
    .map((item) => ({ toUserId: item.toUserId, score: Number(item.score), remark: item.remark || '' }))
  if (targetUserId.value) {
    await roomStore.ownerSubmit(roundId.value, Number(targetUserId.value), data)
  } else {
    await roomStore.submit(roundId.value, data)
  }
  uni.navigateBack()
}
</script>
