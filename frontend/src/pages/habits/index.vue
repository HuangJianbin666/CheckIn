<template>
  <view class="page">
    <view class="header">
      <view class="header-title">我的打卡</view>
      <view class="logout" @click="handleLogout">
        <uni-icons type="closeempty" size="16" color="#6b7280" class="logout-icon" />
        <text>退出</text>
      </view>
    </view>

    <view v-if="loading" class="empty">加载中…</view>

    <view v-else-if="items.length === 0" class="empty">
      <uni-icons type="circle-filled" size="56" color="#dbeafe" class="empty-icon" />
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
            <uni-icons type="eye" size="16" color="#2563eb" />
            <text>详情</text>
          </view>
          <view class="action-btn edit-btn" @click.stop="goEdit(item)">
            <uni-icons type="compose" size="16" color="#2563eb" />
            <text>编辑</text>
          </view>
          <view class="action-btn delete-btn" @click.stop="handleDelete(item)">
            <uni-icons type="trash" size="16" color="#ef4444" />
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
  buttonColor: '#2563eb',
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
  color: #1f2937;
}

.logout {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: #6b7280;
}

.logout-icon {
  display: block;
}

.empty {
  padding: 120rpx 0;
  text-align: center;
  color: #9ca3af;
  font-size: 28rpx;
}

.empty-icon {
  display: block;
  margin: 0 auto 24rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #9ca3af;
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
  background: #ffffff;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.habit-card:active {
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.08);
  transform: translateY(-2rpx);
}

.habit-info {
  flex: 1;
  min-width: 0;
  margin-right: 24rpx;
}

.habit-name {
  font-size: 32rpx;
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.habit-status {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
}

.habit-duration {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #9ca3af;
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
  color: #f97316;
}

.streak-badge.zero {
  color: #9ca3af;
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
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
  border-radius: 16rpx;
  background: #2563eb;
  color: #ffffff;
}

.check-btn.done {
  background: #e5e7eb;
  color: #6b7280;
}

.check-btn[disabled] {
  opacity: 0.7;
}

.habit-actions {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1px solid #f3f4f6;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 0 24rpx;
  height: 56rpx;
  border-radius: 28rpx;
  background: #eff6ff;
  font-size: 24rpx;
  color: #2563eb;
}

.delete-btn {
  background: #fef2f2;
  color: #ef4444;
}
</style>