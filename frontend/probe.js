const { chromium } = require('@playwright/test')

;(async () => {
  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.goto('http://127.0.0.1:5173/', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  console.log('=== URL ===')
  console.log(page.url())
  console.log('=== HTML ===')
  console.log(await page.content())
  await browser.close()
})()