import { defineStore } from 'pinia'
import { bindPhone, wechatLogin } from '@/api/auth'
import { getMe, updateProfile } from '@/api/user'

// 用户 store：集中保存登录 token、用户资料和手机号绑定状态。
export const useUserStore = defineStore('user', {
  state: () => ({
    // 从本地缓存恢复登录态，避免每次打开小程序都丢失 token。
    token: uni.getStorageSync('token') || '',
    // user 里放昵称、头像、手机号等资料。
    user: uni.getStorageSync('user') || null,
    hasBindPhone: false,
  }),
  actions: {
    // 小程序启动时调用：先拿微信临时 code，再交给后端换 token 和用户身份。
    async bootstrap() {
      let loginCode = ''
      try {
        const loginResult = await new Promise((resolve, reject) => {
          uni.login({ provider: 'weixin', success: resolve, fail: reject })
        })
        loginCode = loginResult?.code || ''
      } catch (error) {
        console.warn('uni.login failed, using local dev code.', error)
      }
      const data = await wechatLogin({ code: loginCode || `local-dev-${Date.now()}` })
      this.token = data.token
      this.hasBindPhone = data.hasBindPhone
      uni.setStorageSync('token', data.token)
      if (data.hasBindPhone) {
        // 绑定过手机号的用户直接拉完整资料。
        await this.loadMe()
      } else {
        // 未绑定手机号时，先保存后端返回的基础资料，下一步会跳到绑定页。
        this.user = {
          userId: data.userId,
          nickname: data.nickname,
          avatarUrl: data.avatarUrl,
        }
      }
      return data
    },
    // 从后端读取当前登录用户资料，并同步到本地缓存。
    async loadMe() {
      this.user = await getMe()
      this.hasBindPhone = Boolean(this.user?.phone)
      uni.setStorageSync('user', this.user)
      return this.user
    },
    // 绑定手机号后重新读取用户资料，确保页面展示的是最新状态。
    async bindPhone(phoneCode) {
      await bindPhone({ phoneCode })
      await this.loadMe()
    },
    // 修改昵称后也重新读取一遍资料，保持缓存和后端一致。
    async updateNickname(nickname) {
      await updateProfile({ nickname })
      await this.loadMe()
    },
  },
})
