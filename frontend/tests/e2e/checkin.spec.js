// @ts-check
const { test, expect } = require('@playwright/test')

// 前端 H5（Vite dev server）与后端 API 地址。
// 本用例不对任何请求做 route mock，全部走真实后端。
const WEB = 'http://127.0.0.1:5173'
const API = 'http://127.0.0.1:8080/api/v1'

// 随机用户名：满足 3-32 位小写字母/数字/下划线，避免跨多次运行的唯一键冲突
const genUsername = () => `u${Math.random().toString(36).slice(2, 10)}`
const PASSWORD = 'password123'

// uniapp H5 编译后的真实 DOM 选择器（与源码 <view>/<input>/<button> 对应）
const SEL = {
  pageLogin: 'uni-page[data-page="pages/auth/login"]',
  pageRegister: 'uni-page[data-page="pages/auth/register"]',
  pageHabits: 'uni-page[data-page="pages/habits/index"]',
  pageCreate: 'uni-page[data-page="pages/habits/create"]',
  pageDetail: 'uni-page[data-page="pages/habits/detail"]',
  textInput: 'input.uni-input-input[type="text"]',
  pwdInput: 'input.uni-input-input[type="password"]',
  primaryBtn: 'uni-button.primary-btn',
  switchLink: '.switch',
  habitCard: '.habit-card',
  habitName: '.habit-name',
  habitStatus: '.habit-status',
  habitDuration: '.habit-duration',
  streakBadge: '.streak-badge',
  streakNum: '.streak-num',
  checkBtn: 'uni-button.check-btn',
  createBtn: '.uni-fab__circle',
  detailBtn: '.detail-btn',
  editBtn: '.edit-btn',
  deleteBtn: '.delete-btn',
  durationChip: '.duration-chip',
  logout: '.logout',
  empty: '.empty',
  detailStreakNumber: '.streak-number',
  metaValue: '.meta-value',
  modalConfirm: '.uni-modal__btn_primary',
  calendar: '.calendar',
  checkedDay: '.uni-calendar-item__weeks-box-circle',
}

// 通过注册页完成注册，最终停留在登录页
async function registerViaUi(page, username, password) {
  await page.goto(WEB)
  await expect(page.locator(SEL.pageLogin)).toBeVisible()
  await page.click(SEL.switchLink)
  await expect(page.locator(SEL.pageRegister)).toBeVisible()
  await page.fill(SEL.textInput, username)
  await page.locator(SEL.pwdInput).nth(0).fill(password)
  await page.locator(SEL.pwdInput).nth(1).fill(password)
  await page.click(SEL.primaryBtn)
  // 注册成功后延迟跳回登录页
  await expect(page.locator(SEL.pageLogin)).toBeVisible({ timeout: 10_000 })
}

// 在登录页输入用户名密码并登录，最终进入列表页
async function loginViaUi(page, username, password) {
  await page.fill(SEL.textInput, username)
  await page.fill(SEL.pwdInput, password)
  await page.click(SEL.primaryBtn)
  await expect(page.locator(SEL.pageHabits)).toBeVisible({ timeout: 10_000 })
}

// 通过列表页「新建」入口创建打卡项，最终回到列表页
async function createHabitViaUi(page, name) {
  await page.click(SEL.createBtn)
  await expect(page.locator(SEL.pageCreate)).toBeVisible()
  await page.fill(SEL.textInput, name)
  await page.click(SEL.primaryBtn)
  await expect(page.locator(SEL.pageHabits)).toBeVisible({ timeout: 10_000 })
}

test('注册 → 登录 → 创建 → 打卡 → 刷新保持 → 退出', async ({ page }) => {
  const username = genUsername()

  await registerViaUi(page, username, PASSWORD)
  await loginViaUi(page, username, PASSWORD)

  // 初始为空列表
  await expect(page.locator(SEL.empty)).toBeVisible()

  await createHabitViaUi(page, '阅读 30 分钟')

  // 创建后展示 0 天状态
  const card = page.locator(SEL.habitCard).first()
  await expect(card.locator(SEL.habitName)).toHaveText('阅读 30 分钟')
  await expect(card.locator(SEL.habitStatus)).toHaveText('今日未打卡，连续 0 天')
  await expect(card.locator(SEL.streakNum)).toHaveText('0')
  await expect(card.locator(SEL.checkBtn)).toHaveText('打卡')

  // 打卡后重新拉取列表，展示连续 1 天
  await card.locator(SEL.checkBtn).click()
  await expect(card.locator(SEL.habitStatus)).toHaveText('今日已打卡，连续 1 天')
  await expect(card.locator(SEL.streakNum)).toHaveText('1')
  await expect(card.locator(SEL.checkBtn)).toHaveText('已打卡')

  // 刷新后登录态与状态均保持（token 持久化到 uni storage）
  await page.reload()
  await expect(page.locator(SEL.pageHabits)).toBeVisible()
  const reloadedCard = page.locator(SEL.habitCard).first()
  await expect(reloadedCard.locator(SEL.habitStatus)).toHaveText('今日已打卡，连续 1 天')

  // 进入详情页：连续天数大字 + 月历 + 今日高亮
  await reloadedCard.locator(SEL.detailBtn).click()
  await expect(page.locator(SEL.pageDetail)).toBeVisible({ timeout: 10_000 })
  await expect(page.locator(SEL.detailStreakNumber)).toHaveText('1')
  await expect(page.locator(SEL.calendar)).toBeVisible()
  await expect(page.locator(SEL.checkedDay)).toHaveCount(1)

  // 返回列表并退出
  await page.goBack()
  await expect(page.locator(SEL.pageHabits)).toBeVisible()
  await page.click(SEL.logout)
  await expect(page.locator(SEL.pageLogin)).toBeVisible({ timeout: 10_000 })
})

test('创建带时长 → 详情展示 → 编辑改名改时长 → 删除', async ({ page }) => {
  const username = genUsername()
  await registerViaUi(page, username, PASSWORD)
  await loginViaUi(page, username, PASSWORD)

  // 初始为空列表
  await expect(page.locator(SEL.empty)).toBeVisible()

  // 创建打卡项并选择 45 分钟档位
  await page.click(SEL.createBtn)
  await expect(page.locator(SEL.pageCreate)).toBeVisible()
  await page.fill(SEL.textInput, '晨跑')
  await page.locator(SEL.durationChip).filter({ hasText: '45 分钟' }).click()
  await page.click(SEL.primaryBtn)
  await expect(page.locator(SEL.pageHabits)).toBeVisible({ timeout: 10_000 })

  const card = page.locator(SEL.habitCard).first()
  await expect(card.locator(SEL.habitName)).toHaveText('晨跑')
  await expect(card.locator(SEL.habitDuration)).toHaveText('目标 45 分钟')

  // 详情页展示时长（meta-value 第 0 项为名称，第 1 项为时长）
  await card.locator(SEL.detailBtn).click()
  await expect(page.locator(SEL.pageDetail)).toBeVisible({ timeout: 10_000 })
  await expect(page.locator(SEL.metaValue).nth(1)).toHaveText('45 分钟')
  await page.goBack()
  await expect(page.locator(SEL.pageHabits)).toBeVisible()

  // 编辑：改名并改时长 90 分钟（等待预填完成后填充）
  const card2 = page.locator(SEL.habitCard).first()
  await card2.locator(SEL.editBtn).click()
  await expect(page.locator(SEL.pageCreate)).toBeVisible()
  await expect(page.locator(SEL.textInput)).toHaveValue('晨跑')
  await page.fill(SEL.textInput, '夜跑')
  await page.locator(SEL.durationChip).filter({ hasText: '90 分钟' }).click()
  await page.click(SEL.primaryBtn)
  await expect(page.locator(SEL.pageHabits)).toBeVisible({ timeout: 10_000 })

  const card3 = page.locator(SEL.habitCard).first()
  await expect(card3.locator(SEL.habitName)).toHaveText('夜跑')
  await expect(card3.locator(SEL.habitDuration)).toHaveText('目标 90 分钟')

  // 删除：二次确认后列表回到空态
  await card3.locator(SEL.deleteBtn).click()
  await page.locator(SEL.modalConfirm).click()
  await expect(page.locator(SEL.empty)).toBeVisible({ timeout: 10_000 })
})

test('错误密码登录被拒绝并停留在登录页', async ({ page }) => {
  const username = genUsername()
  await registerViaUi(page, username, PASSWORD)

  // 用错误密码登录
  await page.fill(SEL.textInput, username)
  await page.fill(SEL.pwdInput, 'wrong-password-123')
  await page.click(SEL.primaryBtn)

  // 仍停留在登录页，未进入列表页
  await expect(page.locator(SEL.pageLogin)).toBeVisible()
  await expect(page.locator(SEL.pageHabits)).toHaveCount(0)
  // 后端返回 INVALID_CREDENTIALS，统一 toast 提示
  await expect(page.getByText('用户名或密码错误')).toBeVisible()
})

test('打卡幂等：重复打卡返回同一 recordId（真实 API）', async ({ request }) => {
  const username = genUsername()

  const reg = await request.post(`${API}/auth/register`, {
    data: { username, password: PASSWORD },
  })
  expect(reg.status()).toBe(201)

  const login = await request.post(`${API}/auth/login`, {
    data: { username, password: PASSWORD },
  })
  expect(login.status()).toBe(200)
  const token = (await login.json()).data.token
  const auth = { Authorization: `Bearer ${token}` }

  const created = await request.post(`${API}/habits`, {
    headers: auth,
    data: { name: '喝水' },
  })
  expect(created.status()).toBe(201)
  const habitId = (await created.json()).data.id

  const first = await request.post(`${API}/habits/${habitId}/check-ins`, {
    headers: auth,
  })
  expect(first.status()).toBe(200)
  const firstBody = (await first.json()).data
  expect(firstBody.streak).toBe(1)
  expect(firstBody.todayChecked).toBe(true)

  const second = await request.post(`${API}/habits/${habitId}/check-ins`, {
    headers: auth,
  })
  expect(second.status()).toBe(200)
  const secondBody = (await second.json()).data

  // 幂等核心：重复打卡 recordId 不变，streak 不重复累计
  expect(secondBody.recordId).toBe(firstBody.recordId)
  expect(secondBody.streak).toBe(1)

  const list = await request.get(`${API}/habits`, { headers: auth })
  const listBody = (await list.json()).data
  expect(listBody.items).toHaveLength(1)
  expect(listBody.items[0].streak).toBe(1)
  expect(listBody.items[0].todayChecked).toBe(true)

  // 详情接口：返回 streak 与历史打卡日期集合
  const detail = await request.get(`${API}/habits/${habitId}`, { headers: auth })
  expect(detail.status()).toBe(200)
  const detailBody = (await detail.json()).data
  expect(detailBody.id).toBe(habitId)
  expect(detailBody.streak).toBe(1)
  expect(detailBody.todayChecked).toBe(true)
  expect(detailBody.checkedDates).toHaveLength(1)
})

test('用户隔离与退出后 token 失效（真实 API）', async ({ request }) => {
  const userA = genUsername()
  const userB = genUsername()

  // 用户 A 注册登录并创建打卡项
  await request.post(`${API}/auth/register`, {
    data: { username: userA, password: PASSWORD },
  })
  const loginA = await request.post(`${API}/auth/login`, {
    data: { username: userA, password: PASSWORD },
  })
  const tokenA = (await loginA.json()).data.token
  const authA = { Authorization: `Bearer ${tokenA}` }

  const created = await request.post(`${API}/habits`, {
    headers: authA,
    data: { name: 'A 的打卡项' },
  })
  const habitIdA = (await created.json()).data.id

  // 用户 B 注册登录
  await request.post(`${API}/auth/register`, {
    data: { username: userB, password: PASSWORD },
  })
  const loginB = await request.post(`${API}/auth/login`, {
    data: { username: userB, password: PASSWORD },
  })
  const tokenB = (await loginB.json()).data.token
  const authB = { Authorization: `Bearer ${tokenB}` }

  // B 看不到 A 的数据
  const listB = await request.get(`${API}/habits`, { headers: authB })
  expect((await listB.json()).data.items).toHaveLength(0)

  // B 尝试对 A 的打卡项打卡：404，不泄露资源存在性
  const cross = await request.post(`${API}/habits/${habitIdA}/check-ins`, {
    headers: authB,
  })
  expect(cross.status()).toBe(404)
  expect((await cross.json()).code).toBe('HABIT_NOT_FOUND')

  // B 尝试查看 A 的打卡项详情：同样 404
  const crossDetail = await request.get(`${API}/habits/${habitIdA}`, {
    headers: authB,
  })
  expect(crossDetail.status()).toBe(404)
  expect((await crossDetail.json()).code).toBe('HABIT_NOT_FOUND')

  // A 退出后，旧 token 立即失效
  await request.post(`${API}/auth/logout`, { headers: authA })
  const me = await request.get(`${API}/auth/me`, { headers: authA })
  expect(me.status()).toBe(401)
  expect((await me.json()).code).toBe('UNAUTHENTICATED')
})