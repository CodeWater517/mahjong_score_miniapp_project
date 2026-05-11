<template>
  <view class="page stack">
    <view>
      <view class="page-title">绑定手机号</view>
      <view class="muted">绑定后即可创建房间和参与计分。</view>
    </view>

    <BaseCard class="stack">
      <BaseButton text="微信手机号一键绑定" open-type="getPhoneNumber" @getphonenumber="handleWxPhone" />
      <input class="input" placeholder="开发环境可输入手机号" type="number" v-model="manualPhone" />
      <BaseButton text="使用输入手机号" type="plain" @click="handleManualPhone" />
    </BaseCard>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useUserStore } from '@/stores/user'

const manualPhone = ref('')
const userStore = useUserStore()

const finish = async (phoneCode) => {
  try {
    await userStore.bindPhone(phoneCode)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    console.error('bind phone failed', error)
  }
}

const handleWxPhone = async (event) => {
  const code = event.detail?.code
  if (!code) {
    uni.showToast({ title: '未获得手机号授权', icon: 'none' })
    return
  }
  await finish(code)
}

const handleManualPhone = async () => {
  if (!/^1\d{10}$/.test(manualPhone.value)) {
    uni.showToast({ title: '请输入 11 位手机号', icon: 'none' })
    return
  }
  await finish(manualPhone.value)
}
</script>
