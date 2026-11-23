<script>
export default {
  onLaunch: function () {},
  onShow: function () {},
  onHide: function () {},
}
</script>

<style>
/* 设计 token：唯一事实源，各页面 scoped 样式天然继承 */
:root {
  /* 主色：柔和蓝 */
  --color-primary: #5B8DEF;
  --color-primary-dark: #4A79E0;
  --color-primary-soft: #EAF1FE;
  --color-primary-disabled: #B9D0F7;
  /* 强调：柔和橙 */
  --color-accent: #F5A065;
  --color-accent-deep: #EE8A4E;
  --color-accent-soft: #FEF1E7;
  /* 文字 */
  --color-text-strong: #1F2937;
  --color-text-body: #4B5563;
  --color-text-secondary: #7C8494;
  --color-text-muted: #A6ADBB;
  /* 背景与边框 */
  --color-bg-page: #F6F8FB;
  --color-bg-card: #FFFFFF;
  --color-bg-input: #F5F7FA;
  --color-bg-disabled: #EEF1F6;
  --color-border: #E9EDF3;
  /* 圆角 */
  --radius-sm: 12rpx;
  --radius-md: 16rpx;
  --radius-lg: 20rpx;
  --radius-xl: 24rpx;
  --radius-pill: 999rpx;
  /* 阴影 */
  --shadow-sm: 0 4rpx 16rpx rgba(31, 45, 61, 0.04);
  --shadow-md: 0 12rpx 32rpx rgba(31, 45, 61, 0.06);
  --shadow-lg: 0 16rpx 48rpx rgba(31, 45, 61, 0.10);
  /* 过渡时长 */
  --dur-fast: 150ms;
  --dur-base: 240ms;
}

/* 全局公共样式 */
page {
  background-color: var(--color-bg-page);
  color: var(--color-text-body);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Helvetica, 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 隐藏浏览器原生密码框的“显示密码/清除”眼睛，统一使用 uni-easyinput 自带图标，避免双眼睛 */
input[type='password']::-ms-reveal,
input[type='password']::-ms-clear {
  display: none;
}
</style>