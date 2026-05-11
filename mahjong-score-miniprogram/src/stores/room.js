import { defineStore } from 'pinia'
import {
  closeRoom,
  createRoom,
  findRoomByCode,
  getRoomRank,
  getSnapshot,
  joinRoom,
  kickUser,
  listRoomRounds,
  quitRoom,
  reopenRoom,
  startRoom,
  transferOwner,
  undoLastRound,
} from '@/api/room'
import { deleteRound, forceNeutral, getRoundDetail, ownerSubmit, submitRound, updateRound } from '@/api/round'

// 房间 store：把房间快照、排行榜、历史局和 WebSocket 消息处理集中在一起。
export const useRoomStore = defineStore('room', {
  state: () => ({
    // snapshot 是后端返回的房间完整快照，包括座位、当前轮、房主、状态等。
    snapshot: null,
    // 当前房间排行榜。
    rankList: [],
    // 历史局列表。
    history: [],
    // WebSocket 推送的上一局结算结果，用来弹出结算弹窗。
    lastSummary: null,
    // 给页面显示的轻量提示消息。
    notice: '',
  }),
  getters: {
    // getter 是从 state 派生出来的常用字段，页面读取时更简洁。
    roomId: (state) => state.snapshot?.roomId,
    currentRound: (state) => state.snapshot?.currentRound,
    seats: (state) => state.snapshot?.seats || [],
  },
  actions: {
    // 下面这些 action 大多是 API 的薄封装，页面通过 store 调用，避免直接散落请求代码。
    async create(payload) {
      return createRoom(payload)
    },
    async findByCode(roomCode) {
      return findRoomByCode(roomCode)
    },
    async join(roomId, payload) {
      return joinRoom(roomId, payload)
    },
    async loadSnapshot(roomId) {
      // 快照是房间页面最核心的数据源，加载后同步刷新排行榜。
      this.snapshot = await getSnapshot(roomId)
      this.rankList = this.snapshot.rankList || []
      return this.snapshot
    },
    async loadRank(roomId) {
      this.rankList = await getRoomRank(roomId)
      return this.rankList
    },
    async start(roomId) {
      await startRoom(roomId)
      return this.loadSnapshot(roomId)
    },
    async close(roomId) {
      // 主动关闭房间时带上原因，后端会写入房间开启段和操作日志。
      await closeRoom(roomId, { reason: 'OWNER_CLOSE' })
      return this.loadSnapshot(roomId)
    },
    async reopen(roomId) {
      await reopenRoom(roomId)
      return this.loadSnapshot(roomId)
    },
    async quit(roomId) {
      await quitRoom(roomId)
    },
    async transfer(roomId, targetUserId) {
      await transferOwner(roomId, { targetUserId })
      return this.loadSnapshot(roomId)
    },
    async kick(roomId, targetUserId) {
      await kickUser(roomId, { targetUserId })
      return this.loadSnapshot(roomId)
    },
    async submit(roundId, payments) {
      return submitRound(roundId, { payments })
    },
    async ownerSubmit(roundId, targetUserId, payments) {
      return ownerSubmit(roundId, { targetUserId, payments })
    },
    async forceNeutral(roundId, targetUserId) {
      return forceNeutral(roundId, { targetUserId })
    },
    async loadHistory(roomId) {
      this.history = await listRoomRounds(roomId)
      return this.history
    },
    async detail(roundId) {
      return getRoundDetail(roundId)
    },
    async updateHistory(roundId, payments) {
      return updateRound(roundId, { payments })
    },
    async deleteHistory(roundId) {
      return deleteRound(roundId)
    },
    async undoLast(roomId) {
      await undoLastRound(roomId)
      return this.loadSnapshot(roomId)
    },
    async handleWsMessage(message) {
      // 服务端推送的消息没有类型或房间号时，说明格式不完整，直接忽略。
      if (!message?.type || !message.roomId) return
      if (message.type === 'ROUND_SETTLED') {
        // 一局结算完成后保存结算摘要，play 页面会弹窗展示。
        this.lastSummary = message.payload?.settledRound
      }
      if (message.type === 'ROUND_SUBMIT_MODIFIED') {
        // 已提交内容被修改时，给当前房间用户一个提示。
        this.notice = message.payload?.message || '本轮提交已更新'
      }
      // 这些消息都意味着房间状态可能变化，重新拉一份后端快照最稳妥。
      if (['ROOM_UPDATED', 'SEAT_UPDATED', 'OWNER_CHANGED', 'GAME_STARTED', 'ROUND_SETTLED', 'ROUND_RECALCULATED', 'ROOM_CLOSED', 'ROOM_REOPENED'].includes(message.type)) {
        await this.loadSnapshot(message.roomId)
      }
    },
    clearSummary() {
      // 用户关闭结算弹窗后，清空摘要，避免下次进入页面重复弹出。
      this.lastSummary = null
    },
  },
})
