import { API_BASE_URL } from './request'

// HTTP 地址和 WebSocket 地址只差协议：例如 http://localhost:8080 -> ws://localhost:8080。
const toSocketUrl = (baseUrl) => baseUrl.replace(/^http/, 'ws')

// 房间 WebSocket 客户端：负责连接、心跳、重连和把消息交给页面/store 处理。
export class RoomSocketClient {
  constructor() {
    // uni.connectSocket 返回的连接任务对象。
    this.task = null
    // 当前连接的房间，重连时会继续使用。
    this.roomId = null
    // 区分“用户主动关闭”和“网络异常断开”，避免退出页面后还自动重连。
    this.closedByUser = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    // 外部传入的消息回调，一般会转给 room store。
    this.onMessage = null
  }

  connect(roomId, onMessage) {
    // 先关闭旧连接，保证同一时间只连接一个房间。
    this.close()
    this.closedByUser = false
    this.roomId = roomId
    this.onMessage = onMessage
    const token = uni.getStorageSync('token')
    // token 和 roomId 放在查询参数里，后端握手阶段会校验并登记到对应房间。
    this.task = uni.connectSocket({
      url: `${toSocketUrl(API_BASE_URL)}/ws/room?token=${encodeURIComponent(token)}&roomId=${roomId}`,
      complete: () => {},
    })
    this.task.onOpen(() => {
      // 连接成功后启动心跳，避免长时间无消息导致连接被中间网络断开。
      this.startHeartbeat()
    })
    this.task.onMessage(({ data }) => {
      try {
        const message = JSON.parse(data)
        // 收到服务端消息后交给调用方处理，例如刷新房间快照。
        this.onMessage && this.onMessage(message)
      } catch (error) {
        console.warn('Invalid ws message', error)
      }
    })
    this.task.onClose(() => this.scheduleReconnect())
    this.task.onError(() => this.scheduleReconnect())
  }

  startHeartbeat() {
    clearInterval(this.heartbeatTimer)
    // 每 25 秒发一次 PING；后端收到后会回 PONG。
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'PING', roomId: this.roomId, payload: {} })
    }, 25000)
  }

  send(message) {
    // 没有连接对象时直接跳过，避免空指针错误。
    if (!this.task) return
    this.task.send({ data: JSON.stringify(message) })
  }

  scheduleReconnect() {
    clearInterval(this.heartbeatTimer)
    // 主动关闭或没有房间号时，不需要重连。
    if (this.closedByUser || !this.roomId) return
    clearTimeout(this.reconnectTimer)
    // 简单延迟重连，给网络和后端一点恢复时间。
    this.reconnectTimer = setTimeout(() => this.connect(this.roomId, this.onMessage), 2000)
  }

  close() {
    // 标记主动关闭，这样 onClose 不会触发自动重连。
    this.closedByUser = true
    clearTimeout(this.reconnectTimer)
    clearInterval(this.heartbeatTimer)
    if (this.task) {
      this.task.close({})
      this.task = null
    }
  }
}
