<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>

    <view v-else-if="!detail" class="empty">打卡项不存在</view>

    <view v-else class="content">
      <!-- 顶部：连续天数 -->
      <view class="streak-card">
        <view class="streak-label">当前连续打卡</view>
        <view class="streak-main">
          <view class="flame" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="44" height="44">
              <path
                fill="currentColor"
                d="M12 2c.6 2.8-1 4.6-2.5 6.3C8.1 10 6.5 11.6 6.5 14a5.5 5.5 0 0 0 11 0c0-2.1-.9-3.6-2-5-.6-.7-1.2-1.5-1.5-2.4-1.2 1.3-2.3 2.6-2.6 4.3-.2 1.1-.8 1.6-1.4 1.1-.3-.3-.4-.7-.2-1.1.9-1.7 1.6-4.3 2.2-6.9Z"
              />
            </svg>
          </view>
          <view class="streak-number">{{ detail.streak }}</view>
          <view class="streak-unit">天</view>
        </view>
        <view class="streak-sub">
          {{ detail.todayChecked ? '今日已打卡，继续保持' : '今日还未打卡' }}
        </view>
      </view>

      <!-- 月历 -->
      <view class="calendar">
        <view class="calendar-head">
          <view class="nav" @click="prevMonth">‹</view>
          <view class="title">{{ year }}年{{ month }}月</view>
          <view class="nav" @click="nextMonth">›</view>
        </view>

        <view class="week-row">
          <view v-for="w in weekLabels" :key="w" class="week-cell">{{ w }}</view>
        </view>

        <view v-for="(week, wi) in weeks" :key="wi" class="week-row">
          <view
            v-for="cell in week"
            :key="cell.date"
            class="day-cell"
            :class="{
              dim: !cell.inMonth,
              checked: isChecked(cell.date),
              today: cell.isToday,
            }"
          >
            <view class="day-num">{{ cell.day }}</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getHabitDetail } from '../../api/habit'
import { buildMonthGrid, shiftMonth } from '../../utils/calendar'

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']

const habitId = ref(null)
const detail = ref(null)
const loading = ref(true)

const now = new Date()
const view = ref({ year: now.getFullYear(), month: now.getMonth() + 1 })

const weeks = computed(() => buildMonthGrid(view.value.year, view.value.month))
const checkedSet = computed(() => {
  if (!detail.value) return new Set()
  return new Set(detail.value.checkedDates)
})

function isChecked(date) {
  return checkedSet.value.has(date)
}

function prevMonth() {
  view.value = shiftMonth(view.value.year, view.value.month, -1)
}

function nextMonth() {
  view.value = shiftMonth(view.value.year, view.value.month, 1)
}

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
  color: #9ca3af;
  font-size: 28rpx;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.streak-card {
  background: linear-gradient(135deg, #f97316, #ef4444);
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  color: #ffffff;
  box-shadow: 0 8rpx 32rpx rgba(239, 68, 68, 0.25);
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

.flame {
  color: #fffbeb;
  margin-right: 16rpx;
  display: flex;
  align-items: center;
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
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.calendar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12rpx 24rpx;
}

.nav {
  width: 64rpx;
  height: 64rpx;
  line-height: 60rpx;
  text-align: center;
  font-size: 44rpx;
  color: #2563eb;
}

.title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.week-row {
  display: flex;
}

.week-cell {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
  padding: 8rpx 0;
}

.day-cell {
  flex: 1;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2rpx;
  border-radius: 12rpx;
}

.day-num {
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 28rpx;
  color: #374151;
  border-radius: 50%;
}

.day-cell.dim .day-num {
  color: #d1d5db;
}

.day-cell.checked .day-num {
  background: #2563eb;
  color: #ffffff;
}

.day-cell.today .day-num {
  box-shadow: inset 0 0 0 2rpx #2563eb;
}

.day-cell.today.checked .day-num {
  box-shadow: none;
  outline: 4rpx solid #bfdbfe;
}
</style>