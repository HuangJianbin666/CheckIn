<template>
  <view class="page">
    <view class="header">
      <view class="header-title">我的打卡</view>
      <view class="logout" @click="handleLogout">
        <uni-icons type="closeempty" size="16" color="#7C8494" class="logout-icon" />
        <text>退出</text>
      </view>
    </view>

    <view v-if="loading" class="empty">加载中…</view>

    <view v-else-if="items.length === 0" class="empty">
      <uni-icons type="circle-filled" size="56" color="#EAF1FE" class="empty-icon" />
      <view class="empty-text">还没有打卡项，点击下方按钮创建第一个</view>
    </view>

    <view v-else class="list">
      <view
        v-for="item in items"
        :key="item.id"
        class="habit-card"
        @click="goDetail(item)"
      >
        <view class="habit-info">
          <view class="habit-name">{{ item.name }}</view>
          <view class="habit-status">{{ statusText(item) }}</view>
          <view v-if="item.durationMinutes != null" class="habit-duration">
            目标 {{ item.durationMinutes }} 分钟
          </view>
        </view>
        <view class="habit-right">
          <view class="streak-badge" :class="{ zero: item.streak === 0 }">
            <view class="streak-num">{{ item.streak }}</view>
            <view class="streak-txt">连续天</view>
          </view>
          <button
            class="check-btn"
            :class="{ done: item.todayChecked }"
            :loading="checkingId === item.id"
            :disabled="checkingId === item.id || item.todayChecked"
            @click.stop="handleCheckIn(item)"
          >
            {{ item.todayChecked ? '已打卡' : '打卡' }}
          </button>
        </view>

        <view class="habit-actions">
          <view class="action-btn detail-btn" @click.stop="goDetail(item)">
            <uni-icons type="eye" size="16" color="#5B8DEF" />
            <text>详情</text>
          </view>
          <view class="action-btn edit-btn" @click.stop="goEdit(item)">
            <uni-icons type="compose" size="16" color="#5B8DEF" />
            <text>编辑</text>
          </view>
          <view class="action-btn delete-btn" @click.stop="handleDelete(item)">
            <uni-icons type="trash" size="16" color="#EE8A4E" />
            <text>删除</text>
          </view>
        </view>
      </view>
    </view>

    <uni-fab
      :pattern="fabPattern"
      horizontal="right"
      vertical="bottom"
      :pop-menu="false"
      @fabClick="goCreate"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listHabits, checkIn, deleteHabit } from '../../api/habit'
import { logout } from '../../api/auth'
import { getToken, removeToken } from '../../api/token'

const items = ref([])
const loading = ref(true)
const checkingId = ref(null)

// uni-fab 悬浮按钮配色：主色圆底 + 白色加号
const fabPattern = {
  buttonColor: '#5B8DEF',
  iconColor: '#ffffff',
  icon: 'plusempty',
}

// 纯展示函数：只消费后端返回的 streak / todayChecked，不自行计算连续天数
function statusText(item) {
  if (item.todayChecked) {
    return `今日已打卡，连续 ${item.streak} 天`
  }
  if (item.streak > 0) {
    return `今日未打卡，截至昨天连续 ${item.streak} 天`
  }
  return '今日未打卡，连续 0 天'
}

async function loadList() {
  loading.value = true
  try {
    const res = await listHabits()
    items.value = res.items
  } catch (e) {
    // 401 已由 request.js 跳转登录；其余错误已 toast
  } finally {
    loading.value = false
  }
}

async function handleCheckIn(item) {
  if (checkingId.value !== null) return
  checkingId.value = item.id
  try {
    await checkIn(item.id)
    // 打卡后重新拉列表，以后端最新状态为准
    await loadList()
  } catch (e) {
    // 错误已由 request.js 提示
  } finally {
    checkingId.value = null
  }
}

function confirmDelete(item) {
  return new Promise((resolve) => {
    uni.showModal({
      title: '删除打卡项',
      content: `确定删除「${item.name}」吗？历史打卡记录将一并删除。`,
      confirmText: '删除',
      confirmColor: '#ef4444',
      success: (res) => resolve(res.confirm),
      fail: () => resolve(false),
    })
  })
}

async function handleDelete(item) {
  const confirmed = await confirmDelete(item)
  if (!confirmed) return
  try {
    await deleteHabit(item.id)
    await loadList()
  } catch (e) {
    // 错误已由 request.js 提示
  }
}

async function handleLogout() {
  try {
    await logout()
  } catch (e) {
    // 忽略：即使服务端会话删除失败，仍清本地 token 并回登录页
  } finally {
    removeToken()
    uni.reLaunch({ url: '/pages/auth/login' })
  }
}

function goCreate() {
  uni.navigateTo({ url: '/pages/habits/create' })
}

function goEdit(item) {
  uni.navigateTo({ url: `/pages/habits/create?habitId=${item.id}` })
}

function goDetail(item) {
  uni.navigateTo({ url: `/pages/habits/detail?habitId=${item.id}` })
}

onShow(() => {
  // 刷新恢复登录态：无 token 直接回登录页
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  loadList()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 160rpx;
  box-sizing: border-box;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 8rpx 28rpx;
}

.header-title {
  font-size: 40rpx;
  font-weight: 600;
  color: var(--color-text-strong);
}

.logout {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 0 16rpx 16rpx;
  font-size: 28rpx;
  color: var(--color-text-secondary);
  transition: opacity var(--dur-fast) ease;
}

.logout:active {
  opacity: 0.7;
}

.logout-icon {
  display: block;
}

.empty {
  padding: 120rpx 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 28rpx;
}

.empty-icon {
  display: block;
  margin: 0 auto 24rpx;
}

.empty-text {
  font-size: 28rpx;
  color: var(--color-text-muted);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.habit-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 32rpx;
  box-shadow: var(--shadow-sm);
  transition: transform var(--dur-fast) ease, box-shadow var(--dur-fast) ease;
}

.habit-card:active {
  box-shadow: var(--shadow-md);
  transform: translateY(-2rpx) scale(0.99);
}

.habit-info {
  flex: 1;
  min-width: 0;
  margin-right: 24rpx;
}

.habit-name {
  font-size: 32rpx;
  font-weight: 500;
  color: var(--color-text-strong);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.habit-status {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--color-text-secondary);
}

.habit-duration {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--color-text-muted);
}

.habit-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.streak-badge {
  display: flex;
  align-items: baseline;
  color: var(--color-accent);
}

.streak-badge.zero {
  color: var(--color-text-muted);
}

.streak-num {
  font-size: 56rpx;
  font-weight: 700;
  line-height: 1;
}

.streak-txt {
  font-size: 22rpx;
  margin-left: 6rpx;
}

.check-btn {
  margin: 0;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #ffffff;
  transition: background-color var(--dur-fast) ease, transform var(--dur-fast) ease;
}

.check-btn:active {
  background: var(--color-primary-dark);
  transform: scale(0.97);
}

.check-btn.done {
  background: var(--color-bg-disabled);
  color: var(--color-text-secondary);
}

.check-btn[disabled] {
  opacity: 1;
}

.habit-actions {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1px solid var(--color-border);
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 0 24rpx;
  height: 72rpx;
  border-radius: var(--radius-pill);
  background: var(--color-primary-soft);
  font-size: 24rpx;
  color: var(--color-primary);
  transition: transform var(--dur-fast) ease, opacity var(--dur-fast) ease;
}

.action-btn:active {
  transform: scale(0.97);
  opacity: 0.85;
}

.delete-btn {
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
}
</style>