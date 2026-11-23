<template>
  <view class="page">
    <view class="card">
      <view class="brand">注册账号</view>
      <view class="subtitle">创建你的第一个打卡项</view>

      <view class="field">
        <uni-easyinput
          v-model="username"
          prefixIcon="person"
          placeholder="用户名（3-32 位字母/数字/下划线）"
          primaryColor="#5B8DEF"
          :clearable="false"
        />
      </view>

      <view class="field">
        <uni-easyinput
          v-model="password"
          type="password"
          prefixIcon="locked"
          placeholder="密码（8-72 位）"
          primaryColor="#5B8DEF"
          :clearable="false"
        />
      </view>

      <view class="field">
        <uni-easyinput
          v-model="confirmPassword"
          type="password"
          prefixIcon="locked-filled"
          placeholder="确认密码"
          primaryColor="#5B8DEF"
          :clearable="false"
        />
      </view>

      <button
        class="primary-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleRegister"
      >
        注册
      </button>

      <view class="switch" @click="goLogin">已有账号？去登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { register } from '../../api/auth'

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

function validate() {
  if (!/^[A-Za-z0-9_]{3,32}$/.test(username.value.trim())) {
    uni.showToast({ title: '用户名需为 3-32 位字母、数字或下划线', icon: 'none' })
    return false
  }
  if (password.value.length < 8 || password.value.length > 72) {
    uni.showToast({ title: '密码长度需为 8-72 位', icon: 'none' })
    return false
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  return true
}

async function handleRegister() {
  if (loading.value) return
  if (!validate()) return

  loading.value = true
  try {
    await register(username.value.trim(), password.value)
    uni.showToast({ title: '注册成功，请登录', icon: 'success' })
    // 延迟跳转，确保 toast 可见
    setTimeout(() => uni.reLaunch({ url: '/pages/auth/login' }), 800)
  } catch (e) {
    // 注册失败（如 USERNAME_EXISTS 409）由 request.js 统一 toast 提示
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}

.card {
  width: 100%;
  max-width: 640rpx;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 60rpx 48rpx;
  box-shadow: var(--shadow-md);
}

.brand {
  font-size: 48rpx;
  font-weight: 600;
  color: var(--color-text-strong);
  text-align: center;
}

.subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--color-text-muted);
  text-align: center;
}

.field {
  margin-top: 40rpx;
}

.field :deep(.uni-easyinput__content) {
  height: 92rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-input);
  padding: 0 24rpx;
}

.field :deep(.uni-easyinput__content-input) {
  font-size: 30rpx;
  color: var(--color-text-strong);
}

.field :deep(.uni-easyinput__placeholder-class) {
  color: var(--color-text-muted);
  font-size: 28rpx;
}

.primary-btn {
  margin-top: 48rpx;
  height: 92rpx;
  line-height: 92rpx;
  background: var(--color-primary);
  color: #ffffff;
  border-radius: var(--radius-md);
  font-size: 32rpx;
  transition: background-color var(--dur-fast) ease, transform var(--dur-fast) ease;
}

.primary-btn:active {
  background: var(--color-primary-dark);
  transform: scale(0.97);
}

.primary-btn[disabled] {
  background: var(--color-primary-disabled);
  color: #ffffff;
}

.switch {
  margin-top: 32rpx;
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--color-primary);
}
</style>