import { request } from '@/utils/request'

// 一局计分相关接口：提交、房主代提交、强制不输不赢，以及历史局维护。
export const submitRound = (roundId, data) => request({ url: `/api/rounds/${roundId}/submit`, method: 'POST', data })
export const ownerSubmit = (roundId, data) => request({ url: `/api/rounds/${roundId}/owner-submit`, method: 'POST', data })
export const forceNeutral = (roundId, data) => request({ url: `/api/rounds/${roundId}/force-neutral`, method: 'POST', data })
// 历史详情用于展示本局结果和输分明细，也给房主修改历史局使用。
export const getRoundDetail = (roundId) => request({ url: `/api/rounds/${roundId}` })
export const updateRound = (roundId, data) => request({ url: `/api/rounds/${roundId}`, method: 'PUT', data })
export const deleteRound = (roundId) => request({ url: `/api/rounds/${roundId}`, method: 'DELETE' })
