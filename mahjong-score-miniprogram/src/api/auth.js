import { request } from '@/utils/request'

// 微信登录：前端传临时 code，后端换取 openid 并返回本系统 token。
export const wechatLogin = (data) => request({ url: '/api/auth/wechat-login', method: 'POST', data, auth: false })
// 绑定手机号：微信授权 code 或开发环境手机号交给后端保存。
export const bindPhone = (data) => request({ url: '/api/auth/bind-phone', method: 'POST', data })
