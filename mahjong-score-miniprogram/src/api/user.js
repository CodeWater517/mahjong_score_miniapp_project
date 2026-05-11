import { request } from '@/utils/request'

// 当前登录用户资料。
export const getMe = () => request({ url: '/api/user/me' })
// 修改个人资料，目前只支持昵称。
export const updateProfile = (data) => request({ url: '/api/user/profile', method: 'PUT', data })
