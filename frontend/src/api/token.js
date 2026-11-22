// token 持久化：仅保存本地不透明 token，用于 H5 刷新后恢复登录态
const TOKEN_KEY = 'checkin:token'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}