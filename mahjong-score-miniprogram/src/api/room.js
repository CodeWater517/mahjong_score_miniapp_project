import { request } from '@/utils/request'

// 房间相关接口：每个函数对应后端 RoomController 或 RankingController 的一个路由。
export const createRoom = (data) => request({ url: '/api/rooms', method: 'POST', data })
// 房间快照包含房间状态、座位、当前轮和排行，是房间页最常用的数据。
export const getSnapshot = (roomId) => request({ url: `/api/rooms/${roomId}/snapshot` })
export const findRoomByCode = (roomCode) => request({ url: `/api/rooms/code/${roomCode}` })
export const joinRoom = (roomId, data) => request({ url: `/api/rooms/${roomId}/join`, method: 'POST', data })
export const startRoom = (roomId) => request({ url: `/api/rooms/${roomId}/start`, method: 'POST' })
export const closeRoom = (roomId, data = {}) => request({ url: `/api/rooms/${roomId}/close`, method: 'POST', data })
export const reopenRoom = (roomId) => request({ url: `/api/rooms/${roomId}/reopen`, method: 'POST' })
export const transferOwner = (roomId, data) => request({ url: `/api/rooms/${roomId}/transfer-owner`, method: 'POST', data })
export const kickUser = (roomId, data) => request({ url: `/api/rooms/${roomId}/kick`, method: 'POST', data })
export const quitRoom = (roomId) => request({ url: `/api/rooms/${roomId}/quit`, method: 'POST' })
// 撤销上一局会把最近一局历史局标记删除，并触发后端重算。
export const undoLastRound = (roomId) => request({ url: `/api/rooms/${roomId}/rounds/undo-last`, method: 'POST' })
export const listRoomRounds = (roomId) => request({ url: `/api/rooms/${roomId}/rounds` })
export const getRoomRank = (roomId) => request({ url: `/api/rooms/${roomId}/rank` })
