<template>
  <!-- 通用卡片容器：用 slot 接收父组件传进来的任意内容。 -->
  <view class="base-card">
    <slot />
  </view>
</template>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 卡片统一负责白底、边框和内边距，页面只需要关心里面放什么内容。 */
.base-card {
  padding: 24rpx;
  border: 1rpx solid $border-color;
  border-radius: 8rpx;
  background: $card-color;
  box-sizing: border-box;
}
</style>
