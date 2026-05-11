<template>
  <!-- 通用按钮组件：项目里所有主要按钮都走这里，保证样式和点击事件一致。 -->
  <button
    class="base-button"
    :class="[`base-button--${type}`, { 'base-button--ghost': ghost }]"
    :disabled="disabled"
    :open-type="openType"
    @getphonenumber="$emit('getphonenumber', $event)"
    @tap="$emit('click')"
  >
    <text v-if="icon" class="icon">{{ icon }}</text>
    <text>{{ text }}</text>
  </button>
</template>

<script setup>
// text/type/icon/ghost/disabled/openType 都是父组件传进来的配置。
defineProps({
  // 按钮上显示的文字。
  text: { type: String, required: true },
  // 视觉类型：primary、plain、danger 等，对应下面的 CSS 类。
  type: { type: String, default: 'primary' },
  // 简单图标字符，放在文字左边。
  icon: { type: String, default: '' },
  // ghost 表示镂空按钮，常用于危险操作的二次确认风格。
  ghost: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  // 微信小程序的特殊按钮能力，例如 getPhoneNumber、share。
  openType: { type: String, default: '' },
})

// click 是普通点击；getphonenumber 是微信手机号授权按钮专用事件。
defineEmits(['click', 'getphonenumber'])
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 按钮基础样式，所有类型都共享尺寸、圆角、文字排版。 */
.base-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  padding: 0 24rpx;
  border-radius: 8rpx;
  border: 1rpx solid transparent;
  font-size: 30rpx;
  font-weight: 650;
  line-height: 1.2;
  box-sizing: border-box;
}

/* 小程序 button 默认有边框伪元素，这里清掉，避免和自定义边框叠在一起。 */
.base-button::after {
  border: 0;
}

/* 主按钮，用于页面里的主要动作。 */
.base-button--primary {
  background: $primary-color;
  color: #fff;
}

/* 危险按钮，用于关闭房间、删除历史局等不可轻易恢复的动作。 */
.base-button--danger {
  background: $danger-color;
  color: #fff;
}

/* plain 和 ghost 都走白底描边，视觉上比主按钮弱一级。 */
.base-button--plain,
.base-button--primary.base-button--ghost,
.base-button--danger.base-button--ghost {
  border-color: $border-color;
  background: #fff;
  color: $text-main;
}

/* 禁用态只改变透明度，disabled 属性本身会阻止点击。 */
.base-button[disabled] {
  opacity: .55;
}

/* 图标和文字之间留一点距离。 */
.icon {
  margin-right: 8rpx;
}
</style>
