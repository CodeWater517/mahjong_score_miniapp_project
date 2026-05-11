<template>
  <!-- 单条输分记录：选择赢家、填写分数、填写备注、删除本行。 -->
  <view class="score-row">
    <picker :range="targets" range-key="label" :value="targetIndex" @change="changeTarget">
      <view class="picker">{{ selectedLabel }}</view>
    </picker>
    <input class="score-input" type="number" placeholder="分数" :value="modelValue.score" @input="changeScore" />
    <input class="remark-input" placeholder="备注" :value="modelValue.remark" @input="changeRemark" />
    <button class="remove" @tap="$emit('remove')">×</button>
  </view>
</template>

<script setup>
import { computed } from 'vue'

// modelValue 是 v-model 传进来的当前行数据；targets 是可选择的收分玩家列表。
const props = defineProps({
  modelValue: { type: Object, required: true },
  targets: { type: Array, default: () => [] },
})
// update:modelValue 用来配合 v-model；remove 通知父组件删除这一行。
const emit = defineEmits(['update:modelValue', 'remove'])

// picker 只能使用数组下标，所以根据当前 toUserId 找到选中的下标。
const targetIndex = computed(() => Math.max(0, props.targets.findIndex((item) => item.userId === props.modelValue.toUserId)))
const selectedLabel = computed(() => props.targets[targetIndex.value]?.label || '选择玩家')

// patch 只修改当前行的一部分字段，其它字段保持原样。
const patch = (value) => emit('update:modelValue', { ...props.modelValue, ...value })
// picker 的 event.detail.value 是字符串下标，需要转成数字再取 userId。
const changeTarget = (event) => patch({ toUserId: props.targets[Number(event.detail.value)]?.userId })
// 空输入不保存为 0，而是 undefined，这样提交前可以过滤掉无效行。
const changeScore = (event) => patch({ score: Number(event.detail.value) || undefined })
const changeRemark = (event) => patch({ remark: event.detail.value })
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

/* 四列分别是赢家选择、分数、备注、删除按钮。 */
.score-row {
  display: grid;
  grid-template-columns: 176rpx 128rpx 1fr 56rpx;
  gap: 10rpx;
  align-items: center;
}

/* 选择框和输入框保持同样高度，让表单看起来整齐。 */
.picker,
.score-input,
.remark-input {
  min-height: 72rpx;
  padding: 0 14rpx;
  border: 1rpx solid $border-color;
  border-radius: 8rpx;
  background: #fff;
  box-sizing: border-box;
  font-size: 26rpx;
  display: flex;
  align-items: center;
}

/* 删除按钮是圆形小按钮，避免占用过多横向空间。 */
.remove {
  width: 56rpx;
  height: 56rpx;
  padding: 0;
  border-radius: 50%;
  background: #f3f4f6;
  color: $danger-color;
  font-size: 34rpx;
  line-height: 56rpx;
}

.remove::after {
  border: 0;
}
</style>
