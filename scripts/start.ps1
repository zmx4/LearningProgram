#requires -Version 5.1
<#
.SYNOPSIS
    LearningProgram 一键启动脚本（Windows PowerShell 版）。

.DESCRIPTION
    做这些事：
      1. 解析 learning-program-backend\.env，注入 MySQL / Redis 等环境变量
      2. 端口预检：后端 1231 / 前端 5173 已被监听时跳过对应组件并提示
      3. 前端依赖缺失时自动 pnpm install（本项目约定必须用 pnpm）
      4. 后台启动后端（mvnw spring-boot:run），前台启动前端，日志都打在当前控制台
      5. 前端退出或 Ctrl+C 时用 taskkill /T /F 结束后端整棵进程树（cmd 无法 exec，必须树杀）

    首次使用请先运行 scripts\install.ps1 生成 .env 等配置。

.PARAMETER Backend
    只启动后端。

.PARAMETER Frontend
    只启动前端。

.PARAMETER Ssr
    前端以 SSR 模式启动（node server.mjs），默认为 SPA 模式（pnpm dev）。

.PARAMETER Help
    显示用法说明。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\start.ps1
    启动后端 + 前端（默认，SPA 模式）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\start.ps1 -Ssr
    启动后端 + SSR 模式前端。
#>
[CmdletBinding()]
param(
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$Ssr,
    [switch]$Help
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------- 路径与常量

$ProjectRoot  = Split-Path -Parent $PSScriptRoot
$BackendDir   = Join-Path $ProjectRoot 'learning-program-backend'
$FrontendDir  = Join-Path $ProjectRoot 'learning-program-frontend'
$BackendEnv   = Join-Path $BackendDir '.env'
[int]$BackendPort  = 1231
[int]$FrontendPort = 5173

# ---------------------------------------------------------------- 输出工具（与 install.ps1 一致）

function Write-Step  { param([string]$Message) Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Ok    { param([string]$Message) Write-Host "  [OK] $Message" -ForegroundColor Green }
function Write-Info  { param([string]$Message) Write-Host "  --   $Message" -ForegroundColor DarkGray }
function Write-Warn2 { param([string]$Message) Write-Host "  [!!] $Message" -ForegroundColor Yellow }
function Write-Err2  { param([string]$Message) Write-Host "  [XX] $Message" -ForegroundColor Red }

function Show-Usage {
    Write-Host @'
LearningProgram 一键启动脚本（Windows PowerShell 版）

用法：
  powershell -ExecutionPolicy Bypass -File scripts\start.ps1            # 启动后端 + 前端（默认，SPA 模式）
  powershell -ExecutionPolicy Bypass -File scripts\start.ps1 -Backend   # 只启动后端
  powershell -ExecutionPolicy Bypass -File scripts\start.ps1 -Frontend  # 只启动前端
  powershell -ExecutionPolicy Bypass -File scripts\start.ps1 -Ssr       # 前端以 SSR 模式启动（node server.mjs）

说明：
  - 首次使用请先运行 scripts\install.ps1 生成 .env 等配置
  - JAVA_HOME 未设置时会尝试定位本机 JDK 21（默认 java 可能是编译不了的 25）
'@
}

if ($Help) { Show-Usage; exit 0 }

# ---------------------------------------------------------------- 小工具

function Test-PortInUse {
    param([int]$Port)
    $listeners = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    return [bool]($listeners | Where-Object { $_.Port -eq $Port })
}

function Import-BackendEnv {
    if (-not (Test-Path $BackendEnv)) {
        throw "未找到 $BackendEnv，请先运行 scripts\install.ps1 生成配置"
    }
    foreach ($raw in Get-Content -Path $BackendEnv) {
        $line = $raw.Trim()
        if (-not $line -or $line.StartsWith('#')) { continue }
        if ($line.StartsWith('export ')) { $line = $line.Substring(7).TrimStart() }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        if ($key) { Set-Item -Path "env:$key" -Value $value }
    }
}

# JAVA_HOME 未设置时尝试定位本机 JDK 21（与 bash 版 pin_java_home 对应）
function Set-JavaHomeIfNeeded {
    if ($env:JAVA_HOME) { return }
    $candidates = @()
    foreach ($base in @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', "$env:USERPROFILE\.jdks")) {
        $found = Get-ChildItem -Path $base -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($found) { $candidates += $found }
    }
    if ($candidates.Count -gt 0) {
        $env:JAVA_HOME = $candidates[0]
        Write-Info "JAVA_HOME 未设置，已固定为 $($env:JAVA_HOME)"
        return
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $versionLine = (& java -version 2>&1 | Select-Object -First 1) -join ''
        if ($versionLine -notmatch 'version "21') {
            Write-Warn2 'JAVA_HOME 未设置且未找到 JDK 21，当前默认 java 可能无法编译（项目要求 JDK 21）'
        }
    }
}

# ---------------------------------------------------------------- 主流程

Write-Host 'LearningProgram 一键启动' -ForegroundColor White
Write-Host "项目目录：$ProjectRoot"

# 参数组合：默认两者都启；-Backend 只启后端，-Frontend 只启前端
$startBackend = -not $Frontend
$startFrontend = -not $Backend
if (-not $startBackend -and -not $startFrontend) {
    Write-Err2 '-Backend 与 -Frontend 不能同时指定'
    exit 2
}

# 端口预检：已被监听的组件跳过启动，避免端口冲突循环
if ($startBackend -and (Test-PortInUse -Port $BackendPort)) {
    Write-Warn2 "后端端口 $BackendPort 已被占用，假定后端已在运行，跳过启动"
    $startBackend = $false
}
if ($startFrontend -and (Test-PortInUse -Port $FrontendPort)) {
    Write-Warn2 "前端端口 $FrontendPort 已被占用，假定前端已在运行，跳过启动"
    $startFrontend = $false
}
if (-not $startBackend -and -not $startFrontend) {
    Write-Err2 '后端与前端都无需启动（对应端口均被占用）'
    exit 1
}

# 预检
if ($startBackend) {
    if (-not (Test-Path $BackendDir)) { Write-Err2 "未找到后端目录：$BackendDir"; exit 1 }
    Import-BackendEnv
    Set-JavaHomeIfNeeded
}
if ($startFrontend) {
    if (-not (Test-Path $FrontendDir)) { Write-Err2 "未找到前端目录：$FrontendDir"; exit 1 }
    if (-not (Get-Command pnpm -ErrorAction SilentlyContinue)) {
        Write-Err2 '未找到 pnpm：本项目前端约定使用 pnpm，请先 npm install -g pnpm 或 corepack enable'
        exit 1
    }
    if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
        Write-Step '安装前端依赖（pnpm install）'
        Push-Location $FrontendDir
        try { & pnpm install; if ($LASTEXITCODE -ne 0) { throw "pnpm install 失败（状态码 $LASTEXITCODE）" } }
        finally { Pop-Location }
    }
}

$backendProc = $null
try {
    if ($startBackend) {
        Write-Step "启动后端（spring-boot:run，端口 $BackendPort）"
        $mvnw = Join-Path $BackendDir 'mvnw.cmd'
        if (Test-Path $mvnw) {
            # cmd /c ""mvnw.cmd" spring-boot:run"：外层多包一层引号，兼容带空格的项目路径
            $backendProc = Start-Process -FilePath "$env:ComSpec" `
                -ArgumentList '/c', "`"`"$mvnw`" spring-boot:run`"" `
                -WorkingDirectory $BackendDir -NoNewWindow -PassThru
        }
        else {
            $backendProc = Start-Process -FilePath "$env:ComSpec" `
                -ArgumentList '/c', 'mvn spring-boot:run' `
                -WorkingDirectory $BackendDir -NoNewWindow -PassThru
        }
        Write-Info "后端 PID：$($backendProc.Id)（首次编译启动需要一些时间）"
        Write-Info "接口地址：http://localhost:$BackendPort"
    }

    if ($startFrontend) {
        if ($Ssr) {
            if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
                throw '未找到 node：SSR 模式需要 Node.js 22.18+'
            }
            Write-Step "启动前端（SSR：node server.mjs，端口 $FrontendPort）"
        }
        else {
            Write-Step "启动前端（SPA：pnpm dev，端口 $FrontendPort）"
        }
        Write-Info "页面地址：http://localhost:$FrontendPort"
        if (-not $startBackend) {
            Write-Info '后端未由本脚本启动，接口代理目标见 vite.config.ts 与 .env.local'
        }

        Push-Location $FrontendDir
        try {
            if ($Ssr) { & node server.mjs } else { & pnpm dev }
            $frontendExit = $LASTEXITCODE
        }
        finally { Pop-Location }

        # Ctrl+C（130 / 143）不算异常
        if ($frontendExit -ne 0 -and $frontendExit -ne 130 -and $frontendExit -ne 143) {
            Write-Err2 "前端异常退出（状态码 $frontendExit）"
        }
        exit $frontendExit
    }

    if ($startBackend) {
        Write-Info '按 Ctrl+C 停止后端'
        Wait-Process -Id $backendProc.Id
        Write-Ok "后端已退出（状态码 $($backendProc.ExitCode)）"
        exit $backendProc.ExitCode
    }
}
finally {
    if ($backendProc -and -not $backendProc.HasExited) {
        Write-Host "`n==> 正在停止后端（PID $($backendProc.Id)）" -ForegroundColor Cyan
        # mvnw.cmd 没有 exec 机制，必须整棵进程树强杀，防止遗留 java 子进程
        & taskkill /PID $backendProc.Id /T /F 2>$null | Out-Null
    }
}
