import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './styles/common.scss'

// uni-app 的入口函数。小程序启动时，框架会调用 createApp 来创建 Vue 应用实例。
export function createApp() {
  // createSSRApp 是 uni-app 推荐的 Vue3 创建方式，兼容多端运行。
  const app = createSSRApp(App)
  // Pinia 是全局状态管理工具，用户信息、房间信息都放在 store 里共享。
  app.use(createPinia())
  return {
    // 必须把 app 返回给 uni-app，后续页面和组件才能正常挂载。
    app,
  }
}
