import { getToken, removeToken } from './token'

const BASE_URL = '/api/v1'

// 业务/网络错误对象：code 为后端稳定错误码，message 为用户可读提示
export class ApiError extends Error {
  constructor(code, message, statusCode) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.statusCode = statusCode
  }
}

function toast(message) {
  uni.showToast({ title: message, icon: 'none', duration: 2500 })
}

// token 失效：清本地登录态并回登录页。仅 UNAUTHENTICATED 触发，
// 登录失败（INVALID_CREDENTIALS）不在此列。
function handleUnauthenticated() {
  removeToken()
  uni.reLaunch({ url: '/pages/auth/login' })
}

// 统一请求出口：所有页面只能通过本函数访问后端，禁止直接调用 uni.request。
export function request({ url, method = 'GET', data, header = {} }) {
  const token = getToken()

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 10000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header,
      },
      success(res) {
        const { statusCode, data: body } = res
        const code = body && body.code
        const message = body && body.message

        // 2xx 且业务码为 OK 才视为成功，直接返回 data
        if (statusCode >= 200 && statusCode < 300 && code === 'OK') {
          resolve(body.data)
          return
        }

        // 后端失败响应统一兜底为可读提示
        const errCode = code || 'INTERNAL_ERROR'
        const errMessage = message || '请求失败，请稍后重试'

        if (errCode === 'UNAUTHENTICATED') {
          toast(errMessage)
          handleUnauthenticated()
          reject(new ApiError(errCode, errMessage, statusCode))
          return
        }

        toast(errMessage)
        reject(new ApiError(errCode, errMessage, statusCode))
      },
      fail() {
        const message = '网络异常，请确认后端服务已启动'
        toast(message)
        reject(new ApiError('NETWORK_ERROR', message, 0))
      },
    })
  })
}