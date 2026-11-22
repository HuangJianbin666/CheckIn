# 一键验收脚本
# 顺序执行：java -version -> Redis 连通性 -> 后端单测 -> 后端集成测试 -> 前端 npm ci + build:h5 + test:e2e
# 任一步失败立即以非零退出码终止。
# 运行前需确保 8080 / 5173 端口空闲（脚本会自行拉起 e2e 后端与 H5 dev server 并清理）。

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$Backend = Join-Path $Root 'backend'
$Frontend = Join-Path $Root 'frontend'
$LogDir = Join-Path $Root 'target'

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$script:BackendProc = $null
$script:FrontendProc = $null

function Write-Step([string]$msg) {
    Write-Host "`n[verify] $msg" -ForegroundColor Cyan
}

function Die([string]$msg) {
    Write-Host "`n[verify] 失败：$msg" -ForegroundColor Red
    exit 1
}

# 端口探测用纯 .NET BCL（IPGlobalProperties），不依赖 NetTCPIP 模块/WMI，
# 避免部分 Windows 环境 Get-NetTCPConnection 慢或卡住。
function Test-Port([int]$port) {
    $listeners = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    foreach ($l in $listeners) {
        if ($l.Port -eq $port) { return $true }
    }
    return $false
}

function Stop-Servers {
    # spring-boot:run / npm run 都会 fork 子进程，用 taskkill /T 整棵进程树回收，
    # 不再依赖按端口反查 PID。
    foreach ($proc in @($script:BackendProc, $script:FrontendProc)) {
        if ($proc) {
            taskkill.exe /F /T /PID $proc.Id 2>$null | Out-Null
        }
    }
}

function Wait-Port([int]$port, [int]$timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

# 前置：端口占用检查（不清理用户自有服务）
if (Test-Port 8080) {
    Die "端口 8080 已被占用，请先停止现有后端"
}
if (Test-Port 5173) {
    Die "端口 5173 已被占用，请先停止现有 H5 dev server"
}

try {
    # 1. JDK（用 cmd /c 合并 stderr，避免 PowerShell 5.1 的 NativeCommandError 干扰）
    Write-Step "检查 JDK 版本"
    $javaVersion = cmd /c "java -version 2>&1"
    Write-Host $javaVersion
    if ($LASTEXITCODE -ne 0) { Die "JDK 不可用" }

    # 2. Redis 连通性
    Write-Step "检查 Redis 容器连通性"
    docker exec redis redis-cli ping | Out-Host
    if ($LASTEXITCODE -ne 0) { Die "Redis 容器不可用（docker exec redis redis-cli ping 失败）" }

    # 3. 后端单元测试（*Test，纯 JVM，不依赖 MySQL/Redis）
    Write-Step "后端单元测试：mvnw test"
    Push-Location $Backend
    .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) { Pop-Location; Die "后端单元测试失败" }
    Pop-Location

    # 4. 后端集成测试（*IT，checkin_test + Redis DB 1）
    Write-Step "后端集成测试：mvnw verify -Pintegration"
    Push-Location $Backend
    .\mvnw.cmd verify -Pintegration
    if ($LASTEXITCODE -ne 0) { Pop-Location; Die "后端集成测试失败" }
    Pop-Location

    # 5. 前端依赖安装与生产构建
    Write-Step "前端依赖安装：npm ci"
    Push-Location $Frontend
    npm ci
    if ($LASTEXITCODE -ne 0) { Pop-Location; Die "npm ci 失败" }

    Write-Step "前端生产构建：build:h5"
    npm run build:h5
    if ($LASTEXITCODE -ne 0) { Pop-Location; Die "前端构建失败" }
    Pop-Location

    # 6. E2E：拉起 e2e 后端 + H5 dev server，跑完清理
    Write-Step "启动 e2e 后端（checkin_e2e + Redis DB 2）"
    $env:SPRING_PROFILES_ACTIVE = 'e2e'
    $script:BackendProc = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', 'mvnw.cmd spring-boot:run' `
        -WorkingDirectory $Backend `
        -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'backend-e2e.log') `
        -RedirectStandardError (Join-Path $LogDir 'backend-e2e.err.log')
    Remove-Item Env:\SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue

    if (-not (Wait-Port 8080 90)) { Die "e2e 后端未在 90s 内就绪（见 target/backend-e2e*.log）" }

    Write-Step "启动 H5 dev server"
    $script:FrontendProc = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', 'npm run dev:h5' `
        -WorkingDirectory $Frontend `
        -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'frontend-dev.log') `
        -RedirectStandardError (Join-Path $LogDir 'frontend-dev.err.log')

    if (-not (Wait-Port 5173 60)) { Die "H5 dev server 未在 60s 内就绪（见 target/frontend-dev*.log）" }

    Write-Step "运行 Playwright E2E"
    Push-Location $Frontend
    npm run test:e2e
    if ($LASTEXITCODE -ne 0) { Pop-Location; Die "E2E 测试失败" }
    Pop-Location

    Write-Host "`n[verify] 全部通过" -ForegroundColor Green
    exit 0
}
finally {
    Stop-Servers
}