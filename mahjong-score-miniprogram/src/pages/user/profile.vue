<template>
  <!-- 个人资料页：当前只支持修改昵称，手机号只展示脱敏结果。 -->
  <view class="page stack">
    <view class="page-title">个人资料</view>
    <BaseCard class="stack">
      <input class="input" placeholder="昵称" v-model="nickname" />
      <view class="muted">手机号 {{ userStore.user?.phone || '-' }}</view>
      <BaseButton text="保存昵称" @click="save" />
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const nickname = ref('')

// 页面进入时先确保有用户资料，再把当前昵称放进输入框。
onLoad(async () => {
  if (!userStore.user) await userStore.loadMe()
  nickname.value = userStore.user?.nickname || ''
})

// 保存昵称后返回上一页，用户资料 store 会重新从后端加载。
const save = async () => {
  await userStore.updateNickname(nickname.value)
  uni.navigateBack()
}
</script>
