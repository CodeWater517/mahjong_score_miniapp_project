import { defineStore } from 'pinia'
import { RoomSocketClient } from '@/utils/socket'
import { useRoomStore } from './room'

// 全局只创建一个 WebSocket 客户端，避免多个页面重复连接同一个房间。
const client = new RoomSocketClient()

// WebSocket store：页面只调用 connectRoom/close，具体连接细节交给 RoomSocketClient。
export const useSocketStore = defineStore('socket', {
  state: () => ({
    // 记录当前连接的房间号，方便调试或后续显示连接状态。
    connectedRoomId: null,
  }),
  actions: {
    connectRoom(roomId) {
      this.connectedRoomId = roomId
      const roomStore = useRoomStore()
      // 收到服务端消息时，统一交给 room store 根据消息类型刷新数据。
      client.connect(roomId, (message) => roomStore.handleWsMessage(message))
    },
    close() {
      this.connectedRoomId = null
      client.close()
    },
  },
})
