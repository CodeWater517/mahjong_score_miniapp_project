import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// Vite 是前端构建工具；uni 插件会把 Vue/uni-app 代码编译成微信小程序可运行的代码。
export default defineConfig({
  // plugins 数组里放构建插件，这里只需要 uni-app 官方插件。
  plugins: [uni()],
})
