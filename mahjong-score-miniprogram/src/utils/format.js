// 把后端保存的座位编码转成用户能看懂的中文座位名。
export const seatLabel = (seatName) => {
  const map = { EAST: '东', SOUTH: '南', WEST: '西', NORTH: '北' }
  return map[seatName] || seatName || '-'
}

// 分数展示规则：正数前面加 +，负数和 0 直接显示。
export const scoreText = (score = 0) => {
  if (score > 0) return `+${score}`
  return String(score)
}

// 后端时间通常是 2026-04-29T20:00:00，页面只显示到分钟。
export const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
