// 月历纯函数：只做日期布局计算，不依赖时钟、网络或 uniapp API。

// 补齐为两位，用于拼接 yyyy-MM-dd。
function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

// Date 对象 -> 'yyyy-MM-dd'（本地时区，避免 toISOString 的 UTC 偏移）
export function formatDate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 月份增减：返回新的 { year, month }（month 1-12）
export function shiftMonth(year, month, delta) {
  const base = new Date(year, month - 1 + delta, 1)
  return { year: base.getFullYear(), month: base.getMonth() + 1 }
}

// 构建月历网格：周数组 -> 天数组。
// 每个 Day = { date, day, inMonth, isToday }。
// 从周日（getDay()===0）开始，前置补齐上月尾巴、后置补齐下月头。
export function buildMonthGrid(year, month) {
  const today = formatDate(new Date())
  const first = new Date(year, month - 1, 1)
  const daysInMonth = new Date(year, month, 0).getDate()

  // 起始列：本月 1 号是周几（0=周日）
  const lead = first.getDay()

  const cells = []

  // 前置上月的尾部
  for (let i = lead - 1; i >= 0; i--) {
    const d = new Date(year, month - 1, -i)
    cells.push(makeDay(d, today, false))
  }

  // 本月
  for (let day = 1; day <= daysInMonth; day++) {
    const d = new Date(year, month - 1, day)
    cells.push(makeDay(d, today, true))
  }

  // 后置下月的头部，补齐到 7 的整数倍
  const tail = (7 - (cells.length % 7)) % 7
  for (let i = 1; i <= tail; i++) {
    const d = new Date(year, month, i)
    cells.push(makeDay(d, today, false))
  }

  const weeks = []
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7))
  }
  return weeks
}

function makeDay(d, today, inMonth) {
  return {
    date: formatDate(d),
    day: d.getDate(),
    inMonth,
    isToday: formatDate(d) === today,
  }
}