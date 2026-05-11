const DEFAULT_BASE_URL = 'http://localhost:8080'

// 后端接口地址：优先读取 .env 里的 VITE_API_BASE_URL，没有配置时走本地 8080。
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL

// 对 uni.request 做一层统一封装，页面里只关心“请求什么接口”，不用重复写 token、错误提示等通用逻辑。
export function request({ url, method = 'GET', data, auth = true }) {
  // 登录成功后 token 会存到本地缓存；后续需要登录的接口会自动带上它。
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        // auth 为 true 且本地有 token 时，才拼上 Authorization 请求头。
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: ({ data: body }) => {
        // 后端统一返回 { code, message, data }；code 为 0 表示业务成功。
        if (body && body.code === 0) {
          resolve(body.data)
          return
        }
        const message = body?.message || '请求失败'
        if (body?.code === 40100) {
          // token 过期或未登录时，清掉旧 token 并回到启动页重新登录。
          uni.removeStorageSync('token')
          uni.redirectTo({ url: '/pages/splash/index' })
        }
        uni.showToast({ title: message, icon: 'none' })
        reject(new Error(message))
      },
      fail: (err) => {
        // fail 代表网络层失败，例如服务没启动、域名不可达、超时等。
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      },
    })
  })
}
