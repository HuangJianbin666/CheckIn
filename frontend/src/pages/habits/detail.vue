<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>

    <view v-else-if="!detail" class="empty">打卡项不存在</view>

    <view v-else class="content">
      <!-- 顶部：连续天数 -->
      <view class="streak-card">
        <view class="streak-label">当前连续打卡</view>
        <view class="streak-main">
          <uni-icons type="fire-filled" size="44" color="#fffbeb" />
          <view class="streak-number">{{ detail.streak }}</view>
          <view class="streak-unit">天</view>
        </view>
        <view class="streak-sub">
          {{ detail.todayChecked ? '今日已打卡，继续保持' : '今日还未打卡' }}
        </view>
      </view>

      <!-- 打卡项信息 -->
      <view class="meta-card">
        <view class="meta-row">
          <text class="meta-label">打卡项</text>
          <text class="meta-value">{{ detail.name }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-label">目标时长</text>
          <text class="meta-value">{{ detail.durationMinutes != null ? detail.durationMinutes + ' 分钟' : '未设置' }}</text>
        </view>
      </view>

      <!-- 月历（uni-ui 日历，打卡日打点标记） -->
      <view class="calendar">
        <uni-calendar
          :insert="true"
          :selected="selectedDates"
          :lunar="false"
          :showMonth="false"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getHabitDetail } from '../../api/habit'

const habitId = ref(null)
const detail = ref(null)
const loading = ref(true)

// uni-calendar 打点格式：[{ date: 'yyyy-MM-dd', info: '' }]
// info 传空字符串时仅显示圆点标记，不额外显示文字。
const selectedDates = computed(() => {
  if (!detail.value) return []
  return detail.value.checkedDates.map((date) => ({ date, info: '' }))
})

onLoad((query) => {
  habitId.value = query.habitId
  loadDetail()
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getHabitDetail(habitId.value)
  } catch (e) {
    // 404 / 401 已由 request.js 处理
    detail.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx;
  box-sizing: border-box;
}

.empty {
  padding: 160rpx 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 28rpx;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.streak-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-deep));
  border-radius: var(--radius-xl);
  padding: 48rpx 40rpx;
  color: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(238, 138, 78, 0.24);
}

.streak-card::after {
  content: '';
  position: absolute;
  top: -40%;
  right: -20%;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.streak-label {
  font-size: 26rpx;
  opacity: 0.9;
}

.streak-main {
  display: flex;
  align-items: baseline;
  margin-top: 16rpx;
}

.streak-main :deep(.uni-icons) {
  margin-right: 16rpx;
}

.streak-number {
  font-size: 96rpx;
  font-weight: 700;
  line-height: 1;
}

.streak-unit {
  font-size: 32rpx;
  margin-left: 8rpx;
}

.streak-sub {
  margin-top: 20rpx;
  font-size: 26rpx;
  opacity: 0.9;
}

.calendar {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 24rpx 16rpx;
  box-shadow: var(--shadow-sm);
}

.meta-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 32rpx;
  box-shadow: var(--shadow-sm);
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
}

.meta-row + .meta-row {
  border-top: 1px solid var(--color-border);
}

.meta-label {
  font-size: 26rpx;
  color: var(--color-text-secondary);
}

.meta-value {
  font-size: 28rpx;
  color: var(--color-text-strong);
  font-weight: 500;
}

/* 打卡圆点统一为主色 */
.calendar :deep(.uni-calendar-item__weeks-box-circle) {
  background-color: var(--color-primary);
}
</style>