<script>
export default {
  onLaunch() {
    // 小程序全局启动钩子：当前真正的登录跳转逻辑在 splash 页面里。
    // 这里先保留入口，后面要做版本检查、全局埋点、启动日志时可以放在这里。
  },
}
</script>

<style lang="scss">
@import './styles/variables.scss';

/* page 是小程序每个页面最外层的系统标签；这里设置全局背景、字体和文字颜色。 */
page {
  min-height: 100%;
  background: $background-color;
  color: $text-main;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
</style>
