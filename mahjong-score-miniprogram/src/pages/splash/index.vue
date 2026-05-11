<template>
  <view class="page splash">
    <view class="brand">麻将计分助手</view>
    <view class="muted">{{ statusText }}</view>
    <BaseButton v-if="canRetry" text="重新进入" type="plain" @click="enterApp" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BaseButton from '@/components/BaseButton.vue'
import { useUserStore } from '@/stores/user'

const statusText = ref('正在进入...')
const canRetry = ref(false)

const enterApp = async () => {
  canRetry.value = false
  statusText.value = '正在进入...'
  try {
    const userStore = useUserStore()
    const result = await userStore.bootstrap()
    if (result.hasBindPhone) {
      uni.switchTab({ url: '/pages/home/index' })
    } else {
      uni.redirectTo({ url: '/pages/auth/bind-phone' })
    }
  } catch (error) {
    console.error('app bootstrap failed', error)
    statusText.value = '进入失败，请确认本地后端已启动'
    canRetry.value = true
    uni.showToast({ title: '进入失败，请检查后端服务', icon: 'none' })
  }
}

onLoad(enterApp)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.splash {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
}

.brand {
  color: $primary-color;
  font-size: 48rpx;
  font-weight: 850;
}
</style>
