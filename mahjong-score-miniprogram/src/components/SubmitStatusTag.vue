<template>
  <!-- 一局提交状态的小标签，例如待提交、已提交、房主代交。 -->
  <text class="status" :class="statusClass">{{ label }}</text>
</template>

<script setup>
import { computed } from 'vue'

// status 来自后端 RoomConstants 的提交状态字段。
const props = defineProps({
  status: { type: String, default: 'PENDING' },
})

// 把后端状态码翻译成页面上给用户看的中文。
const label = computed(() => {
  const map = {
    PENDING: '待提交',
    SUBMITTED: '已提交',
    OWNER_SUBMITTED: '房主代交',
    FORCED_SUBMITTED: '不输不赢',
  }
  return map[props.status] || props.status
})

// 当前只有“待提交”和“已完成类”两种视觉状态。
const statusClass = computed(() => (props.status === 'PENDING' ? 'pending' : 'done'))
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 标签基础样式：小尺寸、圆角、内联展示。 */
.status {
  display: inline-flex;
  align-items: center;
  min-height: 36rpx;
  padding: 0 12rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
}

/* 待提交用提醒色，表示还需要用户或房主处理。 */
.pending {
  background: #fff7ed;
  color: $warning-color;
}

/* 非待提交都视为完成状态，用绿色反馈。 */
.done {
  background: #ecfdf5;
  color: $success-color;
}
</style>
