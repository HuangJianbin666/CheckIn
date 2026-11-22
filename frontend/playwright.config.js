// @ts-check
const { defineConfig } = require('@playwright/test')

// E2E 前置：后端以 e2e profile 启动（checkin_e2e + Redis DB 2），
// H5 dev server 以 npm run dev:h5 启动。本配置不通过 webServer 拉起服务，
// 生命周期由 scripts/verify.ps1 或手工统一管理。
module.exports = defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  // 用例涉及登录态与用户隔离，串行执行避免状态串扰
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    headless: true,
    viewport: { width: 390, height: 844 },
    trace: 'retain-on-failure',
  },
})