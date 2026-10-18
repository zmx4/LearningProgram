<#
.SYNOPSIS
    LearningProgram 一键安装脚本（Windows PowerShell 版）。

.DESCRIPTION
    做这些事：
      1. 检查运行环境（JDK 21+、Maven、Node 22.18+、包管理器、mysql 客户端）
      2. 收集 MySQL / Redis 连接信息：环境变量里已有的直接沿用，
         没指定的（含仍是 .env.example 占位符的）会逐个提示手动输入
      3. 生成 learning-program-backend\.env 与 learning-program-frontend\.env.local（原文件先备份）
      4. 建库 learning 并导入 scripts\learning.sql（整库 DDL）
      5. 打包后端（mvnw.cmd package）、安装依赖并构建前端（pnpm/npm）

.PARAMETER Yes
    非交互：缺少必填环境变量时直接报错退出。

.PARAMETER NoDb
    跳过建库与建表。

.PARAMETER NoBuild
    只做配置与数据库，不编译打包。

.PARAMETER NoFrontend
    跳过前端依赖安装与构建。

.PARAMETER ForceEnv
    覆盖已有 .env / .env.local（默认也会先备份再覆盖）。

.PARAMETER ResetDb
    删除并重建 learning 库后重新导入 scripts\learning.sql（会丢失全部数据，交互模式需输入 yes 确认）。

.PARAMETER ForceMigrate
    已废弃：迁移脚本已移除，建表统一由 scripts\learning.sql 完成，传入后被忽略。

.PARAMETER NoWriteEnv
    不写文件，只把 export 语句打印出来。

.PARAMETER Start
    安装完成后前台启动后端。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\install.ps1

.EXAMPLE
    $env:MYSQL_USERNAME="root"; $env:MYSQL_PASSWORD="secret"; .\scripts\install.ps1 -Yes

.NOTES
    说明：把 MYSQL_PASSWORD / REDIS_PASSWORD 设为空字符串表示「明确使用空密码」；
    完全不设置该变量才会触发交互提问。
#>
[CmdletBinding()]
param(
    [switch]$Yes,
    [switch]$NoDb,
    [switch]$NoBuild,
    [switch]$NoFrontend,
    [switch]$ForceEnv,
    [switch]$ResetDb,
    [switch]$ForceMigrate,
    [switch]$NoWriteEnv,
    [switch]$Start
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------- 路径与常量

$ProjectRoot = Split-Path -Parent $PSScriptRoot
if ($env:INSTALL_PROJECT_ROOT) { $ProjectRoot = $env:INSTALL_PROJECT_ROOT }
$BackendDir   = Join-Path $ProjectRoot 'learning-program-backend'
$FrontendDir  = Join-Path $ProjectRoot 'learning-program-frontend'
$SchemaFile   = Join-Path $PSScriptRoot 'learning.sql'
$BackendEnv   = Join-Path $BackendDir '.env'
$FrontendEnv  = Join-Path $FrontendDir '.env.local'

$DbName            = 'learning'
$BackendPort       = '1231'
$FrontendPort      = '5173'
$PlaceholderValues = @('your_mysql_user', 'your_mysql_password', 'your_redis_password', 'changeme', 'example.com')
$RequiredNames     = @('MYSQL_USERNAME', 'MYSQL_PASSWORD', 'MYSQL_HOST', 'MYSQL_PORT', 'REDIS_HOST', 'REDIS_PORT', 'REDIS_PASSWORD')

$script:Summary     = New-Object System.Collections.Generic.List[string]
$script:MysqlClient = $null
$script:FrontendPm  = $null
$script:MissingVars = New-Object System.Collections.Generic.List[string]

# ---------------------------------------------------------------- 输出工具

function Write-Step { param([string]$Message) Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Ok   { param([string]$Message) Write-Host "  [OK] $Message" -ForegroundColor Green }
function Write-Info { param([string]$Message) Write-Host "  --   $Message" -ForegroundColor DarkGray }
function Write-Warn2 { param([string]$Message) Write-Host "  [!!] $Message" -ForegroundColor Yellow }
function Write-Err2 { param([string]$Message) Write-Host "  [XX] $Message" -ForegroundColor Red }

function Stop-Install {
    param([string]$Message)
    Write-Err2 $Message
    exit 1
}

function Remember { param([string]$Key, [string]$Value) $script:Summary.Add("$Key = $Value") }

function Get-EnvValue { param([string]$Name) [Environment]::GetEnvironmentVariable($Name) }

function Get-Masked {
    param([string]$Value)
    if ([string]::IsNullOrEmpty($Value)) { return '(空)' }
    return "******（$($Value.Length) 位）"
}

function Test-Placeholder {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $true }
    return ($PlaceholderValues -contains $Value)
}

function Get-UsableValue {
    param([string]$Value)
    if (Test-Placeholder $Value) { return '' }
    return $Value
}

function Write-Utf8File {
    param([string]$Path, [string]$Content)
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

# .env 里的值统一加双引号并转义，避免密码中的空格/引号破坏 source
function ConvertTo-EnvValue {
    param([string]$Value)
    if ($null -eq $Value) { $Value = '' }
    $escaped = $Value.Replace('\', '\\').Replace('"', '\"')
    return '"' + $escaped + '"'
}

function Backup-File {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    $stamp = Get-Date -Format 'yyyyMMddHHmmss'
    $backup = "$Path.bak.$stamp"
    Copy-Item -LiteralPath $Path -Destination $backup -Force
    Write-Info "已备份原文件 -> $backup"
}

# ---------------------------------------------------------------- 原生命令封装

<#
    运行原生命令：把 stderr 当普通输出处理。PowerShell 5.1 在 Stop 模式下会把
    原生命令写到 stderr 的内容当成终止性错误（java -version、Maven 都会写 stderr），
    所以这里临时切到 Continue，并把输出收集起来。
    返回 @{ ExitCode = n; Output = '...' }
#>
function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$WorkingDirectory = '',
        [switch]$Quiet
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $lines = New-Object System.Collections.Generic.List[string]
    $pushed = $false
    if ($WorkingDirectory) { Push-Location -LiteralPath $WorkingDirectory; $pushed = $true }
    $code = 0
    try {
        $global:LASTEXITCODE = 0
        & $FilePath @Arguments 2>&1 | ForEach-Object {
            $line = if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.ToString() } else { "$_" }
            $lines.Add($line)
            if (-not $Quiet) { Write-Host $line }
        }
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
        if ($pushed) { Pop-Location }
    }
    return [pscustomobject]@{ ExitCode = $code; Output = ($lines -join "`n") }
}

# ---------------------------------------------------------------- 工具链检查

function Test-Toolchain {
    Write-Step '检查运行环境'

    if (Get-Command java -ErrorAction SilentlyContinue) {
        $result = Invoke-Native -FilePath 'java' -Arguments @('-version') -Quiet
        $match = [regex]::Match($result.Output, '"(\d+)')
        if ($match.Success) {
            $major = [int]$match.Groups[1].Value
            if ($major -ge 21) {
                Write-Ok "JDK $major"
            } else {
                Write-Warn2 "检测到 Java $major，项目需要 JDK 21+，请升级后再编译"
            }
        } else {
            Write-Ok 'JDK 已安装（版本号未能识别）'
        }
    } else {
        Write-Warn2 '未找到 java，后端无法编译运行（需要 JDK 21+）'
    }

    $wrapper = Join-Path $BackendDir 'mvnw.cmd'
    if (Test-Path -LiteralPath $wrapper) {
        Write-Ok 'Maven Wrapper：learning-program-backend\mvnw.cmd'
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        $mvn = Invoke-Native -FilePath 'mvn' -Arguments @('-v') -Quiet
        Write-Ok "Maven：$(($mvn.Output -split "`n")[0])"
    } else {
        Write-Warn2 '未找到 mvnw.cmd / mvn，后端无法打包（需要 Maven 3.9+）'
    }

    if (Get-Command node -ErrorAction SilentlyContinue) {
        $node = Invoke-Native -FilePath 'node' -Arguments @('--version') -Quiet
        $nodeVersion = $node.Output.Trim().TrimStart('v')
        $numeric = ($nodeVersion -replace '-.*$', '')
        if (($numeric -as [version]) -and ([version]$numeric -ge [version]'22.18.0')) {
            Write-Ok "Node.js v$nodeVersion"
        } else {
            Write-Warn2 "Node.js v$nodeVersion 可能低于项目要求（^22.18.0 || >=24.12.0）"
        }
    } else {
        Write-Warn2 '未找到 node，前端无法安装依赖与构建（需要 Node 22.18+）'
    }

    $hasPnpm = Get-Command pnpm -ErrorAction SilentlyContinue
    $hasNpm = Get-Command npm -ErrorAction SilentlyContinue
    if ((Test-Path -LiteralPath (Join-Path $FrontendDir 'pnpm-lock.yaml')) -and $hasPnpm) {
        $script:FrontendPm = 'pnpm'
    } elseif ($hasNpm) {
        $script:FrontendPm = 'npm'
    } elseif ($hasPnpm) {
        $script:FrontendPm = 'pnpm'
    }
    if ($script:FrontendPm) {
        $pmVersion = (Invoke-Native -FilePath $script:FrontendPm -Arguments @('--version') -Quiet).Output.Trim()
        Write-Ok "前端包管理器：$($script:FrontendPm) $pmVersion"
    } else {
        Write-Warn2 '未找到 npm / pnpm，前端无法安装依赖'
    }

    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysql) {
        $script:MysqlClient = $mysql.Source
        Write-Ok "mysql 客户端：$($mysql.Source)"
    } else {
        Write-Warn2 '未找到 mysql 客户端，将跳过建库与建表，稍后按提示手工执行 SQL'
    }

    if (-not (Get-Command redis-cli -ErrorAction SilentlyContinue)) {
        Write-Info '未找到 redis-cli（不影响安装，仅少一步连通性自检）'
    }
}

# ---------------------------------------------------------------- 已有配置

function Import-ExistingEnv {
    Write-Step '读取已有配置'
    if (-not (Test-Path -LiteralPath $BackendEnv)) {
        Write-Info '未发现可复用的 .env，所有连接信息将逐个询问'
        return
    }
    $loaded = 0
    foreach ($line in Get-Content -LiteralPath $BackendEnv -Encoding UTF8) {
        $match = [regex]::Match($line, '^\s*export\s+([A-Za-z_][A-Za-z0-9_]*)=(.*)$')
        if (-not $match.Success) { continue }
        $name = $match.Groups[1].Value
        $value = $match.Groups[2].Value.Trim()
        if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
            $value = $value.Replace('\"', '"').Replace('\\', '\')
        }
        # 环境变量里已有的值优先，.env 只做补全
        if (-not [string]::IsNullOrEmpty((Get-EnvValue $name))) { continue }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        $loaded++
    }
    if ($loaded -gt 0) {
        Write-Info "已从 learning-program-backend\.env 载入 $loaded 项配置（作为默认值）"
    } else {
        Write-Info '未发现可复用的 .env，所有连接信息将逐个询问'
    }
}

# ---------------------------------------------------------------- 环境变量收集

function Read-ConfigValue {
    param(
        [string]$Name,
        [string]$Label,
        [string]$Default = '',
        [switch]$Secret,
        [switch]$Required,
        [switch]$Interactive
    )

    $explicitlySet = ($null -ne [Environment]::GetEnvironmentVariable($Name))
    $current = Get-EnvValue $Name
    $value = Get-UsableValue $current

    # 1) 环境变量已指定（非占位符）-> 沿用
    if (-not [string]::IsNullOrEmpty($value)) {
        if ($Secret) {
            Write-Ok "$Label：沿用环境变量（$(Get-Masked $value)）"
            Remember $Name (Get-Masked $value)
        } else {
            Write-Ok "$Label：沿用环境变量（$value）"
            Remember $Name $value
        }
        return $value
    }

    # 2) 敏感变量被显式设为空 -> 就是空值（如本机 Redis 无密码）
    if ($Secret -and $explicitlySet -and [string]::IsNullOrEmpty($current)) {
        Write-Ok "$Label：环境变量已显式设为空（按无密码处理）"
        Remember $Name '(空)'
        return ''
    }

    # 3) 非交互：能用默认值就用；必填项没值才报错
    if (-not $Interactive) {
        if (-not [string]::IsNullOrEmpty($Default)) {
            [Environment]::SetEnvironmentVariable($Name, $Default, 'Process')
            Write-Ok "$Label：使用默认值（$Default）"
            Remember $Name $Default
            return $Default
        }
        if ($Required) {
            Write-Err2 "$Label 未指定：请先设置 `$env:$Name 或去掉 -Yes 交互安装"
            $script:MissingVars.Add($Name)
            return $null
        }
        [Environment]::SetEnvironmentVariable($Name, '', 'Process')
        Write-Ok "$Label：未指定，按空值处理"
        Remember $Name '(空)'
        return ''
    }

    # 4) 交互：提示手动输入
    $hint = $Name
    if (-not [string]::IsNullOrEmpty($Default)) { $hint = "$hint，回车使用默认值 $Default" }
    if ($Secret) { $hint = "$hint，输入不回显" }

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $answer = ''
        if ($Secret) {
            # 控制台被重定向时 Read-Host -AsSecureString 会直接报错，给出可操作的提示
            try {
                $secure = Read-Host -Prompt "  请输入 $Label" -AsSecureString
            } catch {
                Write-Err2 "无法读取隐藏输入（控制台被重定向）：请改用环境变量注入，例如 `$env:$Name='...'"
                Write-Err2 '或直接在终端窗口里运行本脚本：powershell -ExecutionPolicy Bypass -File scripts\install.ps1'
                exit 1
            }
            if ($secure.Length -gt 0) {
                $answer = (New-Object System.Net.NetworkCredential('', $secure)).Password
            }
        } else {
            $answer = Read-Host -Prompt "  请输入 $Label（$hint）"
        }

        if ([string]::IsNullOrEmpty($answer) -and -not [string]::IsNullOrEmpty($Default)) {
            $answer = $Default
        }

        if (-not [string]::IsNullOrEmpty($answer)) {
            [Environment]::SetEnvironmentVariable($Name, $answer, 'Process')
            if ($Secret) { Remember $Name (Get-Masked $answer) } else { Remember $Name $answer }
            return $answer
        }

        if ($Secret) {
            if (-not $Required) {
                Write-Ok "$Label：按空值处理"
                Remember $Name '(空)'
                return ''
            }
            $confirm = Read-Host -Prompt "  $Label 为空，确认使用空密码？(y/N)"
            if ($confirm -match '^(y|Y)$') {
                Remember $Name '(空)'
                return ''
            }
        } elseif (-not $Required) {
            Write-Ok "$Label：按空值处理"
            Remember $Name '(空)'
            return ''
        }
        Write-Warn2 "$Label 不能为空，请重新输入"
    }

    Stop-Install "$Label 连续 3 次为空，安装中止"
}

function Read-AllConfig {
    Write-Step '配置数据库与 Redis 连接（未指定时手动输入）'
    $interactive = (-not $Yes)

    Read-ConfigValue -Name 'MYSQL_USERNAME' -Label 'MySQL 用户名' -Required -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'MYSQL_PASSWORD' -Label 'MySQL 密码' -Secret -Required -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'MYSQL_HOST' -Label 'MySQL 地址' -Default '127.0.0.1' -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'MYSQL_PORT' -Label 'MySQL 端口' -Default '3306' -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'REDIS_HOST' -Label 'Redis 地址' -Default '127.0.0.1' -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'REDIS_PORT' -Label 'Redis 端口' -Default '6379' -Interactive:$interactive | Out-Null
    Read-ConfigValue -Name 'REDIS_PASSWORD' -Label 'Redis 密码（无密码直接回车）' -Secret -Interactive:$interactive | Out-Null

    foreach ($portName in @('MYSQL_PORT', 'REDIS_PORT')) {
        $portValue = Get-EnvValue $portName
        if ($portValue -notmatch '^\d+$') {
            Stop-Install "$portName 必须是数字，当前值：$portValue"
        }
    }

    if ($script:MissingVars.Count -gt 0) {
        Write-Err2 "缺少必填环境变量：$($script:MissingVars -join ' ')"
        Write-Err2 "示例：`$env:MYSQL_USERNAME='root'; `$env:MYSQL_PASSWORD='secret'; .\scripts\install.ps1 -Yes"
        exit 1
    }
}

# ---------------------------------------------------------------- 写配置文件

function Select-ApiTarget {
    $target = Get-UsableValue (Get-EnvValue 'VITE_DEV_API_TARGET')
    if (-not [string]::IsNullOrEmpty($target)) {
        Write-Ok "前端代理目标：沿用环境变量（$target）"
        Remember 'VITE_DEV_API_TARGET' $target
        return $target
    }
    $default = "http://127.0.0.1:$BackendPort"
    if ($Yes) {
        Write-Ok "前端代理目标：使用默认值（$default）"
        Remember 'VITE_DEV_API_TARGET' $default
        return $default
    }
    $answer = Read-Host -Prompt "  前端 /api 代理目标（VITE_DEV_API_TARGET，回车使用 $default）"
    if ([string]::IsNullOrEmpty($answer)) { $answer = $default }
    Write-Ok "前端代理目标：$answer"
    Remember 'VITE_DEV_API_TARGET' $answer
    return $answer
}

function Write-EnvFile {
    param([string]$ApiTarget)

    Write-Step '生成配置文件'
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

    if (Test-Path -LiteralPath $BackendEnv) {
        if ($ForceEnv) {
            Write-Warn2 'learning-program-backend\.env 已存在，按 -ForceEnv 覆盖'
        } else {
            Write-Warn2 'learning-program-backend\.env 已存在，将备份后按本次输入重写'
        }
        Backup-File $BackendEnv
    }

    $lines = @(
        "# 由 scripts/install.ps1 生成于 $stamp",
        '# 用法：当前 PowerShell 会话的 $env: 已生效；Git Bash / Linux 可 source 本文件',
        '# 该文件已被 .gitignore 忽略，请勿提交真实账号密码',
        '',
        '# MySQL',
        "export MYSQL_USERNAME=$(ConvertTo-EnvValue (Get-EnvValue 'MYSQL_USERNAME'))",
        "export MYSQL_PASSWORD=$(ConvertTo-EnvValue (Get-EnvValue 'MYSQL_PASSWORD'))",
        "export MYSQL_HOST=$(ConvertTo-EnvValue (Get-EnvValue 'MYSQL_HOST'))",
        "export MYSQL_PORT=$(ConvertTo-EnvValue (Get-EnvValue 'MYSQL_PORT'))",
        '',
        '# Redis（无密码时留空）',
        "export REDIS_HOST=$(ConvertTo-EnvValue (Get-EnvValue 'REDIS_HOST'))",
        "export REDIS_PORT=$(ConvertTo-EnvValue (Get-EnvValue 'REDIS_PORT'))",
        "export REDIS_PASSWORD=$(ConvertTo-EnvValue (Get-EnvValue 'REDIS_PASSWORD'))"
    )
    Write-Utf8File -Path $BackendEnv -Content (($lines -join "`r`n") + "`r`n")
    Write-Ok '已写入 learning-program-backend\.env'

    if (-not $NoFrontend) {
        if (Test-Path -LiteralPath $FrontendEnv) { Backup-File $FrontendEnv }
        $frontendLines = @(
            "# 由 scripts/install.ps1 生成于 $stamp",
            '# dev / preview 时 vite 把 /api 转发到后端；改完需重启 dev server',
            '',
            "VITE_DEV_API_TARGET=$ApiTarget",
            '',
            '# 前端直连后端地址；留空表示与前端同源（推荐由 nginx 反向代理）',
            'VITE_API_BASE_URL='
        )
        Write-Utf8File -Path $FrontendEnv -Content (($frontendLines -join "`r`n") + "`r`n")
        Write-Ok '已写入 learning-program-frontend\.env.local'
    }
}

# ---------------------------------------------------------------- 数据库

function Invoke-MysqlScalar {
    param([string]$Sql)
    if (-not $script:MysqlClient) { return $null }
    $env:MYSQL_PWD = Get-EnvValue 'MYSQL_PASSWORD'
    try {
        $result = Invoke-Native -FilePath $script:MysqlClient -Arguments @(
            '-h', (Get-EnvValue 'MYSQL_HOST'),
            '-P', (Get-EnvValue 'MYSQL_PORT'),
            '-u', (Get-EnvValue 'MYSQL_USERNAME'),
            '--default-character-set=utf8mb4', '--connect-timeout=8', '-N', '-B', '-e', $Sql
        ) -Quiet
        if ($result.ExitCode -ne 0) { return $null }
        return $result.Output.Trim()
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Invoke-MysqlFile {
    param([string]$Path)
    $env:MYSQL_PWD = Get-EnvValue 'MYSQL_PASSWORD'
    try {
        # 交给 cmd 做输入重定向，避免 PowerShell 管道重新编码 SQL 文件里的中文
        $commandLine = '"{0}" -h {1} -P {2} -u {3} --default-character-set=utf8mb4 --connect-timeout=8 {4} < "{5}"' -f `
            $script:MysqlClient, (Get-EnvValue 'MYSQL_HOST'), (Get-EnvValue 'MYSQL_PORT'), `
            (Get-EnvValue 'MYSQL_USERNAME'), $DbName, $Path
        $result = Invoke-Native -FilePath 'cmd' -Arguments @('/c', $commandLine) -Quiet
        return ($result.ExitCode -eq 0)
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Get-ExpectedTableCount {
    if (-not (Test-Path -LiteralPath $SchemaFile)) { return 0 }
    $lines = [System.IO.File]::ReadAllLines($SchemaFile, [System.Text.Encoding]::UTF8)
    return @($lines | Where-Object { $_ -match '^CREATE TABLE' }).Count
}

function Get-TableCount {
    $value = Invoke-MysqlScalar -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DbName';"
    if ($null -eq $value) { return $null }
    return $value.Trim()
}

function New-Database {
    return ($null -ne (Invoke-MysqlScalar -Sql "CREATE DATABASE IF NOT EXISTS $DbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"))
}

function Confirm-Reset {
    $expected = Get-ExpectedTableCount
    Write-Warn2 "即将删除数据库 $DbName（其中数据全部丢失），再按 learning.sql 重建 $expected 张表"
    if ($Yes) {
        Write-Warn2 '非交互模式：已通过 -ResetDb 显式指定，直接执行'
        return $true
    }
    $answer = Read-Host -Prompt '  确认请输入 yes（其他任意输入取消）'
    return ($answer -eq 'yes')
}

function Import-Schema {
    $expected = Get-ExpectedTableCount
    Write-Info "导入 scripts\learning.sql（$expected 张表）"

    if (-not (Invoke-MysqlFile -Path $SchemaFile)) {
        Write-Err2 '导入 learning.sql 失败'
        Write-Info "可手工执行：mysql -h $(Get-EnvValue 'MYSQL_HOST') -P $(Get-EnvValue 'MYSQL_PORT') -u $(Get-EnvValue 'MYSQL_USERNAME') -p $DbName < scripts\learning.sql"
        return $false
    }

    $actual = Get-TableCount
    if ("$actual" -eq "$expected") {
        Write-Ok "建表完成：$actual 张表"
        return $true
    }

    $actualText = '未知'
    if ($null -ne $actual) { $actualText = $actual }
    Write-Warn2 "建表结果与预期不一致：预期 $expected 张，实际 $actualText 张"
    Write-Info '请检查 scripts\learning.sql 是否完整，或加 -ResetDb 重新建库'
    return $false
}

function Initialize-Database {
    Write-Step '初始化数据库'

    if ($ForceMigrate) {
        Write-Warn2 '-ForceMigrate 已废弃：迁移脚本已移除，建表统一由 scripts\learning.sql 完成，本次忽略该参数'
    }

    if (-not (Test-Path -LiteralPath $SchemaFile)) {
        Write-Err2 "未找到建表脚本：$SchemaFile"
        Write-Info '该文件是表结构的唯一来源，请确认 scripts\learning.sql 存在'
        return
    }

    if (-not $script:MysqlClient) {
        Write-Warn2 '跳过建库与建表：本机没有 mysql 客户端'
        Write-Info '请手工执行：'
        Write-Info "  1) CREATE DATABASE IF NOT EXISTS $DbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        Write-Info "  2) mysql -u <用户名> -p $DbName < scripts\learning.sql"
        return
    }

    if ($null -eq (Invoke-MysqlScalar -Sql 'SELECT 1;')) {
        Write-Err2 "MySQL 连接失败：$(Get-EnvValue 'MYSQL_USERNAME')@$(Get-EnvValue 'MYSQL_HOST'):$(Get-EnvValue 'MYSQL_PORT')"
        Write-Info '请检查账号密码、服务是否启动、以及该账号是否有远程访问权限'
        return
    }
    Write-Ok "MySQL 连接成功（$(Get-EnvValue 'MYSQL_HOST'):$(Get-EnvValue 'MYSQL_PORT')）"

    if (New-Database) {
        Write-Ok "数据库 $DbName 已就绪"
    } else {
        Write-Err2 "创建数据库 $DbName 失败：请确认账号具备建库权限"
        return
    }

    $existing = Get-TableCount
    if ($null -eq $existing) {
        Write-Warn2 '无法读取表清单（权限不足或库不存在），跳过建表'
        Write-Info "请手工导入：mysql -u <用户名> -p $DbName < scripts\learning.sql"
        return
    }

    if ($existing -ne '0') {
        Write-Info "数据库 $DbName 已有 $existing 张表（learning.sql 定义 $(Get-ExpectedTableCount) 张）"
        if (-not $ResetDb) {
            Write-Info '为避免覆盖现有数据，跳过建表'
            Write-Info '需要按 scripts\learning.sql 重建时加 -ResetDb（会清空全部数据）'
            return
        }
        if (-not (Confirm-Reset)) {
            Write-Info '已取消，数据库保持原样'
            return
        }
        if ($null -eq (Invoke-MysqlScalar -Sql "DROP DATABASE IF EXISTS $DbName;")) {
            Write-Err2 "删除数据库 $DbName 失败：请确认账号具备删库权限"
            return
        }
        Write-Ok "已删除数据库 $DbName"
        if (-not (New-Database)) {
            Write-Err2 "重建数据库 $DbName 失败"
            return
        }
    } else {
        Write-Info '检测到空数据库，开始建表'
    }

    $null = Import-Schema
}

# ---------------------------------------------------------------- 构建

function Build-Backend {
    Write-Step '打包后端'
    $wrapper = Join-Path $BackendDir 'mvnw.cmd'
    $result = $null
    if (Test-Path -LiteralPath $wrapper) {
        $result = Invoke-Native -FilePath $wrapper -Arguments @('-q', '-DskipTests', 'package') -WorkingDirectory $BackendDir
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        $result = Invoke-Native -FilePath 'mvn' -Arguments @('-q', '-DskipTests', 'package') -WorkingDirectory $BackendDir
    } else {
        Write-Warn2 '跳过后端打包：未找到 mvnw.cmd / mvn'
        return
    }
    if ($result.ExitCode -eq 0) {
        Write-Ok '后端打包完成（产物在 learning-program-backend\target\*.jar）'
    } else {
        Write-Err2 '后端打包失败，请查看上面的 Maven 输出'
    }
}

function Build-Frontend {
    Write-Step '安装并构建前端'
    if (-not $script:FrontendPm) {
        Write-Warn2 '跳过前端：未找到 npm / pnpm'
        return
    }

    $install = Invoke-Native -FilePath $script:FrontendPm -Arguments @('install') -WorkingDirectory $FrontendDir
    if ($install.ExitCode -ne 0) {
        Write-Err2 '前端依赖安装失败'
        return
    }
    Write-Ok "$($script:FrontendPm) install 完成"

    $build = Invoke-Native -FilePath $script:FrontendPm -Arguments @('run', 'build') -WorkingDirectory $FrontendDir
    if ($build.ExitCode -ne 0) {
        Write-Err2 '前端构建失败（build 会先做类型检查）'
        return
    }
    Write-Ok '前端构建完成（产物在 learning-program-frontend\dist）'
}

# ---------------------------------------------------------------- 收尾

function Write-Summary {
    Write-Step '安装结果'
    foreach ($item in $script:Summary) { Write-Info $item }

    Write-Host "`n下一步：" -ForegroundColor White
    Write-Host '  1) 启动后端：在同一个 PowerShell 窗口加载 .env 后运行，或直接加 -Start 参数启动'
    Write-Host '     Get-Content learning-program-backend\.env | Where-Object { $_ -match "^export " } | ForEach-Object { $kv = ($_ -replace "^export ", "") -split "=", 2; [Environment]::SetEnvironmentVariable($kv[0], $kv[1].Trim([char]34), "Process") }'
    Write-Host '     cd learning-program-backend; .\mvnw.cmd spring-boot:run'
    Write-Host "     接口地址：http://localhost:$BackendPort"
    Write-Host "  2) 启动前端：cd learning-program-frontend; $($script:FrontendPm) run dev"
    Write-Host "     页面地址：http://localhost:$FrontendPort"
    Write-Host "  3) 管理员账号与更多说明见 docs\INSTALLATION.md`n"
}

function Start-Backend {
    Write-Step '启动后端（Ctrl+C 停止）'
    $wrapper = Join-Path $BackendDir 'mvnw.cmd'
    if (Test-Path -LiteralPath $wrapper) {
        Invoke-Native -FilePath $wrapper -Arguments @('spring-boot:run') -WorkingDirectory $BackendDir | Out-Null
    } else {
        Invoke-Native -FilePath 'mvn' -Arguments @('spring-boot:run') -WorkingDirectory $BackendDir | Out-Null
    }
}

# ---------------------------------------------------------------- 主流程

function Invoke-Main {
    Write-Host 'LearningProgram 安装脚本（PowerShell）' -ForegroundColor White
    Write-Host "项目目录：$ProjectRoot"

    if (-not (Test-Path -LiteralPath $BackendDir)) { Stop-Install "未找到后端目录：$BackendDir" }
    if (-not (Test-Path -LiteralPath $FrontendDir)) { Stop-Install "未找到前端目录：$FrontendDir" }

    if ($Yes) { Write-Info '非交互模式：只使用环境变量与默认值，缺少必填项会直接报错' }

    # 标准输入被重定向时读不到隐藏输入，提前给出可操作的提示，避免反复空输入
    $inputRedirected = $false
    try { $inputRedirected = [Console]::IsInputRedirected } catch { $inputRedirected = $false }
    if ($inputRedirected -and -not $Yes) {
        Stop-Install '检测到标准输入被重定向，无法交互输入；请先设置环境变量再加 -Yes 运行（示例见脚本头部注释）'
    }

    Test-Toolchain
    Import-ExistingEnv
    Read-AllConfig
    $apiTarget = Select-ApiTarget

    if ($NoWriteEnv) {
        Write-Step '配置输出（-NoWriteEnv：只打印，不写文件）'
        foreach ($name in $RequiredNames) {
            Write-Host ("export {0}={1}" -f $name, (Get-EnvValue $name))
        }
    } else {
        Write-EnvFile -ApiTarget $apiTarget
    }

    if ($NoDb) {
        Write-Step '初始化数据库'
        Write-Info '已按 -NoDb 跳过'
    } else {
        Initialize-Database
    }

    if ($NoBuild) {
        Write-Step '构建'
        Write-Info '已按 -NoBuild 跳过'
    } else {
        Build-Backend
        if (-not $NoFrontend) { Build-Frontend }
    }

    Write-Summary

    if ($Start) { Start-Backend }
}

Invoke-Main
