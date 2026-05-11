import { request } from '@/utils/request'

// 首页排行榜：后端同时返回历史总榜和本月榜。
export const getHomeRanking = () => request({ url: '/api/rankings/home' })
