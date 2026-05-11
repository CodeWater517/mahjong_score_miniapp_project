import { request } from '@/utils/request'

// 个人战绩接口。range 可传 ALL、MONTH、WEEK，分别代表全部、本月、本周。
export const getMyStats = (range = 'ALL') => request({ url: `/api/stats/me?range=${range}` })
