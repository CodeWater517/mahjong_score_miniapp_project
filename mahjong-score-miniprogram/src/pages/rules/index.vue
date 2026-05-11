<template>
  <!-- 规则说明页：用普通文字解释本小程序的计分边界和约定。 -->
  <view class="page stack">
    <view class="page-title">规则说明</view>
    <BaseCard class="stack">
      <view class="section-title">基础术语</view>
      <view class="muted">本小程序记录每一局玩家主动填写的输分流水，并自动计算净分。</view>
      <view class="section-title">计分说明</view>
      <view class="muted">每名玩家只填写“我输给谁多少分”。所有参与者提交后，系统按收支差额结算本局。</view>
      <view class="section-title">座位继承</view>
      <view class="muted">座位分会保留在东南西北座位上，新玩家加入空座位后继承该座位当前分数。</view>
      <view class="section-title">版本边界</view>
      <view class="muted">当前版本暂不支持自动番型识别和番型计算。</view>
    </BaseCard>
  </view>
</template>

<script setup>
// 当前页面是纯展示页，只需要卡片组件，不需要额外状态。
import BaseCard from '@/components/BaseCard.vue'
</script>
