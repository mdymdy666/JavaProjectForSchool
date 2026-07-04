param(
  [string]$ProjectRoot = (Join-Path $PSScriptRoot '..')
)

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$RawArgs = ($args -join ' ')
$VerboseMode = $RawArgs -match '(^|\s)--verbose(\s|$)'
$ReportOnly = $RawArgs -match '(^|\s)--report-only(\s|$)'
$NoPause = $RawArgs -match '(^|\s)--no-pause(\s|$)'

Set-Location -LiteralPath $ProjectRoot

$ReportDir = Join-Path $ProjectRoot 'reports'
$TmpDir = Join-Path $ReportDir 'tmp'
$ReportFile = Join-Path $ReportDir 'dependency-check-report.md'
New-Item -ItemType Directory -Force -Path $ReportDir, $TmpDir | Out-Null

$Rows = New-Object System.Collections.Generic.List[object]
$Details = New-Object System.Collections.Generic.List[string]
$Aligned = New-Object System.Collections.Generic.List[string]
$Acceptable = New-Object System.Collections.Generic.List[string]
$NeedAdjust = New-Object System.Collections.Generic.List[string]
$Missing = New-Object System.Collections.Generic.List[string]
$ProjectNotConfigured = New-Object System.Collections.Generic.List[string]
$Failed = New-Object System.Collections.Generic.List[string]

function Add-Detail {
  param([string]$Text)
  $Details.Add($Text) | Out-Null
}

function Add-Row {
  param(
    [string]$Type,
    [string]$Name,
    [string]$Recommended,
    [string]$Detected,
    [string]$Source,
    [string]$Status,
    [string]$Advice
  )

  $Rows.Add([pscustomobject]@{
    Type = $Type
    Name = $Name
    Recommended = $Recommended
    Detected = $Detected
    Source = $Source
    Status = $Status
    Advice = $Advice
  }) | Out-Null

  switch -Regex ($Status) {
    '^已对齐$' { $Aligned.Add($Name) | Out-Null; break }
    '^可接受待确认$' { $Acceptable.Add($Name) | Out-Null; break }
    '^需调整$' { $NeedAdjust.Add($Name) | Out-Null; break }
    '^未检测到$' { $Missing.Add($Name) | Out-Null; break }
    '^项目未配置$' { $ProjectNotConfigured.Add($Name) | Out-Null; break }
    '^检测失败$' { $Failed.Add($Name) | Out-Null; break }
  }
}

function Invoke-Capture {
  param(
    [Parameter(Mandatory=$true)][string]$File,
    [string[]]$Arguments = @(),
    [int]$TimeoutSeconds = 30
  )

  if (-not (Test-Path -LiteralPath $File)) {
    return [pscustomobject]@{ ExitCode = 127; Output = "File not found: $File" }
  }

  $outFile = Join-Path $TmpDir ([guid]::NewGuid().ToString() + '.out.tmp')
  $errFile = Join-Path $TmpDir ([guid]::NewGuid().ToString() + '.err.tmp')
  try {
    $process = Start-Process -FilePath $File -ArgumentList $Arguments -NoNewWindow -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $errFile
    if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
      try { $process.Kill() } catch {}
      return [pscustomobject]@{ ExitCode = 124; Output = "Timed out after ${TimeoutSeconds}s" }
    }

    $stdout = if (Test-Path -LiteralPath $outFile) { Get-Content -Raw -LiteralPath $outFile -ErrorAction SilentlyContinue } else { '' }
    $stderr = if (Test-Path -LiteralPath $errFile) { Get-Content -Raw -LiteralPath $errFile -ErrorAction SilentlyContinue } else { '' }
    $output = (($stdout, $stderr) -join "`n").Trim()
    return [pscustomobject]@{ ExitCode = $process.ExitCode; Output = $output }
  } catch {
    return [pscustomobject]@{ ExitCode = 1; Output = $_.Exception.Message }
  } finally {
    Remove-Item -LiteralPath $outFile, $errFile -Force -ErrorAction SilentlyContinue
  }
}

function Resolve-FirstExisting {
  param([string[]]$Patterns)

  $found = New-Object System.Collections.Generic.List[string]
  foreach ($pattern in $Patterns) {
    if ([string]::IsNullOrWhiteSpace($pattern)) { continue }
    try {
      Get-Item -Path $pattern -ErrorAction SilentlyContinue | ForEach-Object {
        if (-not $_.PSIsContainer) {
          $full = $_.FullName
          if (-not $found.Contains($full)) { $found.Add($full) | Out-Null }
        }
      }
    } catch {}
  }
  return $found.ToArray()
}

function Get-PathCommand {
  param([string]$CommandName)
  $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($cmd) { return $cmd.Source }
  return $null
}

function New-ToolCandidateList {
  param(
    [string]$CommandName,
    [string[]]$EnvRoots = @(),
    [string[]]$ExtraPatterns = @()
  )

  $candidates = New-Object System.Collections.Generic.List[object]
  $pathHit = Get-PathCommand $CommandName
  if ($pathHit) {
    $candidates.Add([pscustomobject]@{ SourceType = 'PATH'; Path = $pathHit }) | Out-Null
  }

  foreach ($envName in $EnvRoots) {
    $envValue = [Environment]::GetEnvironmentVariable($envName, 'Process')
    if (-not $envValue) { $envValue = [Environment]::GetEnvironmentVariable($envName, 'User') }
    if (-not $envValue) { $envValue = [Environment]::GetEnvironmentVariable($envName, 'Machine') }
    if ($envValue) {
      foreach ($suffix in @("bin\$CommandName", $CommandName)) {
        $candidate = Join-Path $envValue $suffix
        if (Test-Path -LiteralPath $candidate) {
          $candidates.Add([pscustomobject]@{ SourceType = "ENV:$envName"; Path = (Resolve-Path -LiteralPath $candidate).Path }) | Out-Null
        }
      }
    }
  }

  foreach ($path in Resolve-FirstExisting $ExtraPatterns) {
    $candidates.Add([pscustomobject]@{ SourceType = 'FULL_PATH'; Path = $path }) | Out-Null
  }

  $seen = @{}
  $unique = New-Object System.Collections.Generic.List[object]
  foreach ($candidate in $candidates) {
    $key = $candidate.Path.ToLowerInvariant()
    if (-not $seen.ContainsKey($key)) {
      $seen[$key] = $true
      $unique.Add($candidate) | Out-Null
    }
  }
  return $unique.ToArray()
}

function Get-VersionFromOutput {
  param([string]$Output, [string]$Regex)
  if ($Output -match $Regex) { return $Matches[1] }
  return ''
}

function Get-NormalizedVersion {
  param([string]$Version)
  if ([string]::IsNullOrWhiteSpace($Version)) { return '' }
  return ($Version -replace '^v','').Trim()
}

function Compare-ToolVersion {
  param([string]$Name, [string]$Recommended, [string]$Detected)

  if ([string]::IsNullOrWhiteSpace($Detected)) {
    return [pscustomobject]@{ Status = '检测失败'; Advice = '命令可运行但无法解析版本，请人工查看输出。' }
  }

  $rec = Get-NormalizedVersion $Recommended
  $det = Get-NormalizedVersion $Detected
  if ($rec -and ($rec -eq $det)) {
    return [pscustomobject]@{ Status = '已对齐'; Advice = '版本与团队推荐版本一致。' }
  }

  switch ($Name) {
    'Git' {
      if ($det -match '^2\.(4[0-9]|[5-9][0-9])\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Git 2.4x+ 通常可用，记录差异后组内确认。' } }
    }
    { $_ -in @('Java','Javac') } {
      if ($det -match '^(21|24)(\.|$)') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Java 21 LTS 或 Java 24 可先保留，正式开发前全组确认。' } }
    }
    'Maven' {
      if ($det -match '^3\.9\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Maven 3.9.x 通常可用，建议记录差异。' } }
    }
    'Node.js' {
      if ($det -match '^22\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Node 22.x 通常可用，建议记录差异。' } }
      if ($det -match '^(20|21)\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Node 20/21 可能可用，建议正式前端创建前组内确认。' } }
    }
    'npm' {
      if ($det -match '^10\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='npm 10.x 通常可用，通常随 Node.js 管理。' } }
    }
    'pnpm' {
      if ($det -match '^11\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='pnpm 11.x 通常可用；建议固定到 11.7.0。' } }
    }
    { $_ -in @('MySQL Client','MySQL Server') } {
      if ($det -match '^8\.0\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='MySQL 8.0.x 通常可用；建议与团队确认是否必须 8.0.20。' } }
      if ($det -match '^(5\.|9\.)') { return [pscustomobject]@{ Status='需调整'; Advice='MySQL 主版本差异较大，建议并行安装或切换到 8.0.x。' } }
    }
    { $_ -in @('Redis Server','Redis CLI') } {
      if ($det -match '^[78]\.') { return [pscustomobject]@{ Status='可接受待确认'; Advice='Redis 7.x/8.x 通常可先保留，联调前组内确认。' } }
    }
    'Apifox' {
      return [pscustomobject]@{ Status='可接受待确认'; Advice='Apifox 小版本差异通常可接受，确保能打开项目接口集合。' }
    }
    'VS Code' {
      return [pscustomobject]@{ Status='可接受待确认'; Advice='VS Code 为推荐工具，稳定版通常可用；记录实际版本即可。' }
    }
    'IntelliJ IDEA' {
      return [pscustomobject]@{ Status='可接受待确认'; Advice='IntelliJ IDEA 为推荐工具，2025.x 或相近版本通常可用；记录实际版本即可。' }
    }
  }

  return [pscustomobject]@{ Status='需调整'; Advice='版本与推荐版本差异较大或不在兼容范围内，请组内确认后调整。' }
}

function Check-Tool {
  param(
    [string]$Type,
    [string]$Name,
    [string]$Recommended,
    [string]$CommandName,
    [string[]]$Arguments,
    [string]$VersionRegex,
    [string[]]$EnvRoots = @(),
    [string[]]$ExtraPatterns = @()
  )

  Write-Host "[CHECK] $Name"
  $candidates = @(New-ToolCandidateList -CommandName $CommandName -EnvRoots $EnvRoots -ExtraPatterns $ExtraPatterns)
  if ($candidates.Count -eq 0) {
    Add-Row $Type $Name $Recommended '' '' '未检测到' 'PATH、环境变量和常见目录均未发现。'
    Add-Detail "### $Name`n`n未检测到候选路径。`n"
    return $null
  }

  $candidateLines = $candidates | ForEach-Object { "- $($_.SourceType): $($_.Path)" }
  $candidateText = $candidateLines -join [Environment]::NewLine
  Add-Detail "### $Name`n`n候选路径：`n$candidateText`n"

  foreach ($candidate in $candidates) {
    $result = Invoke-Capture -File $candidate.Path -Arguments $Arguments -TimeoutSeconds 30
    if ($result.ExitCode -eq 0 -or $result.Output) {
      $version = Get-VersionFromOutput -Output $result.Output -Regex $VersionRegex
      $compare = Compare-ToolVersion -Name $Name -Recommended $Recommended -Detected $version
      if ($candidate.SourceType -ne 'PATH' -and $compare.Status -eq '已对齐') {
        $compare = [pscustomobject]@{ Status='可接受待确认'; Advice='版本匹配但未通过 PATH 命中，可运行；建议确认 PATH 是否需要调整。' }
      }
      Add-Row $Type $Name $Recommended $version "$($candidate.SourceType): $($candidate.Path)" $compare.Status $compare.Advice
      Add-Detail "使用命令：`"$($candidate.Path)`" $($Arguments -join ' ')`n`n输出：`n~~~text`n$($result.Output)`n~~~`n"
      return [pscustomobject]@{ Path = $candidate.Path; Version = $version; SourceType = $candidate.SourceType; Output = $result.Output; Status = $compare.Status }
    }
  }

  Add-Row $Type $Name $Recommended '' ($candidates[0].Path) '检测失败' '找到候选路径，但执行失败。'
  return $null
}

function Check-FileVersionTool {
  param(
    [string]$Type,
    [string]$Name,
    [string]$Recommended,
    [string]$CommandName,
    [string[]]$ExtraPatterns = @()
  )

  Write-Host "[CHECK] $Name"
  $candidates = @(New-ToolCandidateList -CommandName $CommandName -EnvRoots @() -ExtraPatterns $ExtraPatterns)
  if ($candidates.Count -eq 0) {
    Add-Row $Type $Name $Recommended '' '' '未检测到' 'PATH 和常见目录均未发现。'
    Add-Detail "### $Name`n`n未检测到候选路径。`n"
    return $null
  }

  $candidateLines = $candidates | ForEach-Object { "- $($_.SourceType): $($_.Path)" }
  $candidateText = $candidateLines -join [Environment]::NewLine
  Add-Detail "### $Name`n`n候选路径：`n$candidateText`n"

  $candidate = $candidates[0]
  $item = Get-Item -LiteralPath $candidate.Path -ErrorAction SilentlyContinue
  $version = ''
  if ($item -and $item.VersionInfo) {
    $version = if ($item.VersionInfo.ProductVersion) { $item.VersionInfo.ProductVersion } else { $item.VersionInfo.FileVersion }
    $version = ($version -replace '\.0$','').Trim()
  }
  if ($version) {
    $compare = Compare-ToolVersion -Name $Name -Recommended $Recommended -Detected $version
    Add-Row $Type $Name $Recommended $version "$($candidate.SourceType): $($candidate.Path)" $compare.Status $compare.Advice
  } else {
    Add-Row $Type $Name $Recommended '' "$($candidate.SourceType): $($candidate.Path)" '可接受待确认' '可执行文件存在，但未读取到版本信息；请人工启动确认。'
  }
  return [pscustomobject]@{ Path = $candidate.Path; Version = $version; SourceType = $candidate.SourceType }
}

function Find-ProjectFiles {
  param([string]$Filter)
  $exclude = '\\(target|build|node_modules|dist|dist-ssr|coverage|\.git)\\'
  return @(Get-ChildItem -LiteralPath $ProjectRoot -Recurse -Filter $Filter -File -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch $exclude })
}

function Check-MavenProjectDeps {
  $pomFiles = @(Find-ProjectFiles -Filter 'pom.xml')
  if ($pomFiles.Count -eq 0) {
    foreach ($name in @('Spring Boot','Spring Security','MySQL JDBC Driver','Spring Data Redis','JUnit / Spring Boot Test')) {
      Add-Row '配置依赖' $name '项目配置为准' '' '' '项目未配置' '当前仓库未发现 pom.xml，跳过 Maven 配置型依赖检测。'
    }
    Add-Detail "### Maven 项目配置依赖`n`n未发现 pom.xml，Spring Boot 等配置型依赖判定为项目未配置。`n"
    return
  }

  foreach ($pom in $pomFiles) {
    Add-Detail "### Maven 项目：$($pom.FullName)`n"
    $pomText = Get-Content -Raw -LiteralPath $pom.FullName -ErrorAction SilentlyContinue
    $mvn = Get-PathCommand 'mvn'
    if (-not $mvn) {
      foreach ($name in @('Spring Boot','Spring Security','MySQL JDBC Driver','Spring Data Redis','JUnit / Spring Boot Test')) {
        Add-Row '配置依赖' $name '项目配置为准' '' $pom.FullName '需调整' '发现 pom.xml，但 mvn 命令不可用，无法解析依赖。'
      }
      continue
    }

    $tree = Invoke-Capture -File $mvn -Arguments @('-f', $pom.FullName, '-q', 'dependency:tree') -TimeoutSeconds 180
    $combined = "$pomText`n$($tree.Output)"
    $checks = @(
      @{ Name='Spring Boot'; Recommended='3.5.15 或项目配置'; Pattern='spring-boot|org\.springframework\.boot' },
      @{ Name='Spring Security'; Recommended='6.5.11 或项目配置'; Pattern='spring-boot-starter-security|spring-security' },
      @{ Name='MySQL JDBC Driver'; Recommended='9.7.0 或项目配置'; Pattern='mysql-connector-j|mysql:mysql-connector-java|com\.mysql' },
      @{ Name='Spring Data Redis'; Recommended='3.5.12 或项目配置'; Pattern='spring-boot-starter-data-redis|spring-data-redis' },
      @{ Name='JUnit / Spring Boot Test'; Recommended='5.12.2 或项目配置'; Pattern='spring-boot-starter-test|junit-jupiter|junit' }
    )
    foreach ($check in $checks) {
      if ($combined -match $check.Pattern) {
        $status = if ($tree.ExitCode -eq 0) { '可接受待确认' } else { '检测失败' }
        $advice = if ($tree.ExitCode -eq 0) { '已在 Maven 配置或依赖树中发现；具体版本以项目配置和依赖树为准。' } else { '配置中疑似存在，但 dependency:tree 执行失败，需检查网络或 Maven 仓库。' }
        Add-Row '配置依赖' $check.Name $check.Recommended '项目配置/依赖树' $pom.FullName $status $advice
      } else {
        Add-Row '配置依赖' $check.Name $check.Recommended '' $pom.FullName '项目未配置' 'pom.xml 中未发现该依赖。'
      }
    }
    Add-Detail "Maven dependency:tree exit code: $($tree.ExitCode)`n`n~~~text`n$($tree.Output)`n~~~`n"
  }
}

function Check-NodeProjectDeps {
  $packageFiles = @(Find-ProjectFiles -Filter 'package.json')
  if ($packageFiles.Count -eq 0) {
    foreach ($name in @('Vue3','Vite','Axios','Vitest / Frontend Test')) {
      Add-Row '配置依赖' $name '项目配置为准' '' '' '项目未配置' '当前仓库未发现 package.json，跳过 Node 配置型依赖检测。'
    }
    Add-Detail "### Node 项目配置依赖`n`n未发现 package.json，Vue3/Vite 等配置型依赖判定为项目未配置。`n"
    return
  }

  foreach ($pkg in $packageFiles) {
    Add-Detail "### Node 项目：$($pkg.FullName)`n"
    $jsonText = Get-Content -Raw -LiteralPath $pkg.FullName -ErrorAction SilentlyContinue
    $json = $null
    try { $json = $jsonText | ConvertFrom-Json } catch {}
    $deps = @{}
    if ($json) {
      foreach ($section in @('dependencies','devDependencies','peerDependencies')) {
        if ($json.$section) {
          $json.$section.PSObject.Properties | ForEach-Object { $deps[$_.Name] = [string]$_.Value }
        }
      }
    }
    $checks = @(
      @{ Name='Vue3'; Recommended='3.5.24 或项目配置'; Package='vue' },
      @{ Name='Vite'; Recommended='6.3.5 或项目配置'; Package='vite' },
      @{ Name='Axios'; Recommended='1.7.9 或项目配置'; Package='axios' },
      @{ Name='Vitest / Frontend Test'; Recommended='2.1.9 或项目配置'; Package='vitest' }
    )
    foreach ($check in $checks) {
      if ($deps.ContainsKey($check.Package)) {
        Add-Row '配置依赖' $check.Name $check.Recommended $deps[$check.Package] $pkg.FullName '可接受待确认' '已在 package.json 中发现；具体解析版本以 lockfile/install 结果为准。'
      } else {
        Add-Row '配置依赖' $check.Name $check.Recommended '' $pkg.FullName '项目未配置' 'package.json 中未发现该依赖。'
      }
    }
  }
}

function Check-MySqlRuntime {
  param($MysqlClient)
  $services = @(Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'mysql' -or $_.DisplayName -match 'mysql' })
  if ($services.Count -gt 0) {
    $lines = $services | ForEach-Object { "- $($_.Name) / $($_.DisplayName): $($_.State), StartMode=$($_.StartMode), Path=$($_.PathName)" }
    $serviceText = $lines -join [Environment]::NewLine
    Add-Detail "### MySQL Windows 服务`n`n$serviceText`n"
  } else {
    Add-Detail "### MySQL Windows 服务`n`n未发现 MySQL 相关服务。`n"
  }

  if ($MysqlClient -and $MysqlClient.Path) {
    $login = Invoke-Capture -File $MysqlClient.Path -Arguments @('-u','root','-e','SELECT VERSION();') -TimeoutSeconds 15
    $status = if ($login.ExitCode -eq 0) { '已对齐' } elseif ($login.Output -match 'Access denied|password') { '可接受待确认' } else { '检测失败' }
    $advice = if ($login.ExitCode -eq 0) { 'MySQL 可无密码执行 SELECT VERSION()。' } elseif ($login.Output -match 'Access denied|password') { 'MySQL 客户端和服务存在，但连接需要测试账号；不要猜密码。' } else { '连接测试失败，请检查服务、端口或账号。' }
    Add-Row '连接测试' 'MySQL SELECT VERSION()' '可连接' ($(if ($login.ExitCode -eq 0) { 'PASS' } else { 'BLOCKED/FAILED' })) $MysqlClient.Path $status $advice
    Add-Detail "MySQL 连接测试输出：`n~~~text`n$($login.Output)`n~~~`n"
  }
}

function Check-RedisRuntime {
  param($RedisCli)
  $services = @(Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'redis' -or $_.DisplayName -match 'redis' })
  $processes = @(Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -match 'redis' })
  if ($services.Count -gt 0) {
    $lines = $services | ForEach-Object { "- $($_.Name) / $($_.DisplayName): $($_.State), StartMode=$($_.StartMode), Path=$($_.PathName)" }
    $serviceText = $lines -join [Environment]::NewLine
    Add-Detail "### Redis Windows 服务`n`n$serviceText`n"
  } else {
    Add-Detail "### Redis Windows 服务`n`n未发现 Redis 相关服务。`n"
  }
  if ($processes.Count -gt 0) {
    $lines = $processes | ForEach-Object { "- $($_.ProcessName) pid=$($_.Id)" }
    $processText = $lines -join [Environment]::NewLine
    Add-Detail "### Redis 进程`n`n$processText`n"
  }

  if ($RedisCli -and $RedisCli.Path) {
    $ping = Invoke-Capture -File $RedisCli.Path -Arguments @('ping') -TimeoutSeconds 10
    $output = $ping.Output
    if ($output -match 'PONG') {
      Add-Row '连接测试' 'Redis PING' 'PONG' 'PONG' $RedisCli.Path '已对齐' 'Redis CLI 可 PING 成功。'
    } elseif ($output -match 'NOAUTH|Authentication') {
      Add-Row '连接测试' 'Redis PING' 'PONG' 'NOAUTH' $RedisCli.Path '可接受待确认' 'Redis 正在运行但需要认证；不要猜密码或修改配置。'
    } else {
      Add-Row '连接测试' 'Redis PING' 'PONG' 'FAILED' $RedisCli.Path '检测失败' 'Redis PING 未通过，请检查服务、端口或认证配置。'
    }
    Add-Detail "Redis PING 输出：`n~~~text`n$output`n~~~`n"
  }
}

function Write-Report {
  $gitBranch = (git branch --show-current 2>$null)
  $gitStatus = (git status --short 2>$null) -join "`n"
  if ([string]::IsNullOrWhiteSpace($gitStatus)) { $gitStatus = 'clean' }
  $os = Get-CimInstance Win32_OperatingSystem -ErrorAction SilentlyContinue
  $computer = [Environment]::MachineName
  $time = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'

  $md = New-Object System.Collections.Generic.List[string]
  $md.Add('# Dependency Check Report') | Out-Null
  $md.Add('') | Out-Null
  $md.Add("| Item | Value |") | Out-Null
  $md.Add("|---|---|") | Out-Null
  $md.Add("| Check time | $time |") | Out-Null
  $md.Add("| Project path | $ProjectRoot |") | Out-Null
  $md.Add("| Git branch | $gitBranch |") | Out-Null
  $md.Add("| Computer | $computer |") | Out-Null
  if ($os) { $md.Add("| OS | $($os.Caption) $($os.Version) |") | Out-Null }
  $md.Add('') | Out-Null
  $md.Add('## Git Status') | Out-Null
  $md.Add('') | Out-Null
  $md.Add('```text') | Out-Null
  $md.Add($gitStatus) | Out-Null
  $md.Add('```') | Out-Null
  $md.Add('') | Out-Null
  $md.Add('## Summary Table') | Out-Null
  $md.Add('') | Out-Null
  $md.Add('| 类型 | 名称 | 推荐版本 | 检测版本 | 命令来源/路径 | 状态 | 处理建议 |') | Out-Null
  $md.Add('|---|---|---|---|---|---|---|') | Out-Null
  foreach ($row in $Rows) {
    $values = @($row.Type,$row.Name,$row.Recommended,$row.Detected,$row.Source,$row.Status,$row.Advice) | ForEach-Object {
      ([string]$_).Replace('|','\|').Replace("`r",' ').Replace("`n",' ')
    }
    $md.Add("| $($values -join ' | ') |") | Out-Null
  }
  $md.Add('') | Out-Null
  $md.Add('## Grouped Result') | Out-Null
  $md.Add('') | Out-Null
  $groups = @(
    @('已对齐', $Aligned),
    @('可接受待确认', $Acceptable),
    @('需调整', $NeedAdjust),
    @('未检测到', $Missing),
    @('项目未配置', $ProjectNotConfigured),
    @('检测失败', $Failed)
  )
  foreach ($group in $groups) {
    $items = @($group[1])
    $text = if ($items.Count -gt 0) { ($items -join '、') } else { '无' }
    $md.Add("- $($group[0])：$text") | Out-Null
  }
  $md.Add('') | Out-Null
  $md.Add('## Details') | Out-Null
  $md.Add('') | Out-Null
  foreach ($detail in $Details) { $md.Add($detail) | Out-Null }

  Set-Content -LiteralPath $ReportFile -Encoding UTF8 -Value $md
}

Write-Host ""
Write-Host "Campus Second-Hand Trading Platform dependency check"
Write-Host "Project: $ProjectRoot"
Write-Host ""

$git = Check-Tool '必须' 'Git' '2.49.0.windows.1' 'git.exe' @('--version') 'git version ([0-9][^\s]+)' @() @(
  'D:\dev-tools\git*\cmd\git.exe',
  'D:\soft_wares\Git\cmd\git.exe',
  'C:\Program Files\Git\cmd\git.exe',
  'C:\Program Files (x86)\Git\cmd\git.exe'
)
$java = Check-Tool '必须' 'Java' '24.0.2' 'java.exe' @('-version') 'version "([^"]+)"' @('JAVA_HOME') @(
  'D:\dev-tools\java*\*\bin\java.exe',
  'D:\dev-tools\java\*\bin\java.exe',
  'D:\soft_wares\java*\bin\java.exe',
  'C:\Program Files\Java\*\bin\java.exe'
)
$javac = Check-Tool '必须' 'Javac' '24.0.2' 'javac.exe' @('-version') 'javac ([0-9][^\s]+)' @('JAVA_HOME') @(
  'D:\dev-tools\java*\*\bin\javac.exe',
  'D:\dev-tools\java\*\bin\javac.exe',
  'D:\soft_wares\java*\bin\javac.exe',
  'C:\Program Files\Java\*\bin\javac.exe'
)
$maven = Check-Tool '必须' 'Maven' '3.9.11' 'mvn.cmd' @('-version') 'Apache Maven ([0-9][^\s]+)' @('MAVEN_HOME') @(
  'D:\dev-tools\maven*\*\bin\mvn.cmd',
  'D:\soft_wares2\maven*\*\*\bin\mvn.cmd',
  'C:\Program Files\Apache\maven*\bin\mvn.cmd'
)
$node = Check-Tool '必须' 'Node.js' 'v22.16.0' 'node.exe' @('--version') 'v?([0-9][^\s]+)' @('NODE_HOME') @(
  'D:\dev-tools\nodejs*\node.exe',
  'D:\dev-tools\nodejs\*\node.exe',
  'D:\soft_wares\nodejs\node.exe',
  'C:\Program Files\nodejs\node.exe'
)
$npm = Check-Tool '必须' 'npm' '10.9.2' 'npm.cmd' @('--version') '([0-9][^\s]+)' @('NODE_HOME') @(
  'D:\dev-tools\nodejs*\npm.cmd',
  'D:\dev-tools\nodejs\*\npm.cmd',
  'D:\soft_wares\nodejs\npm.cmd',
  'C:\Program Files\nodejs\npm.cmd'
)
$pnpm = Check-Tool '必须' 'pnpm' '11.7.0' 'pnpm.cmd' @('--version') '([0-9][^\s]+)' @('NODE_HOME') @(
  'D:\dev-tools\npm-global\pnpm.cmd',
  'D:\soft_wares5\pnpm\*\pnpm.cmd',
  "$env:APPDATA\npm\pnpm.cmd"
)
$mysql = Check-Tool '必须' 'MySQL Client' '8.0.20' 'mysql.exe' @('--version') 'Ver ([0-9][^\s]+)' @('MYSQL_HOME') @(
  'D:\dev-tools\mysql\*\bin\mysql.exe',
  'D:\soft_wares\mysql_sqlite\bin\mysql.exe',
  'D:\soft_wares*\mysql*\bin\mysql.exe',
  'E:\01big_project\mysql-8.0.20\*\bin\mysql.exe',
  'C:\Program Files\MySQL\*\bin\mysql.exe'
)
$mysqld = Check-Tool '必须' 'MySQL Server' '8.0.20' 'mysqld.exe' @('--version') 'Ver ([0-9][^\s]+)' @('MYSQL_HOME') @(
  'D:\dev-tools\mysql\*\bin\mysqld.exe',
  'D:\soft_wares\mysql_sqlite\bin\mysqld.exe',
  'D:\soft_wares*\mysql*\bin\mysqld.exe',
  'E:\01big_project\mysql-8.0.20\*\bin\mysqld.exe',
  'C:\Program Files\MySQL\*\bin\mysqld.exe'
)
$redisServer = Check-Tool '必须' 'Redis Server' '8.0.3' 'redis-server.exe' @('--version') 'v=([0-9][^\s]+)' @('REDIS_HOME') @(
  'D:\dev-tools\redis\*\redis-server.exe',
  'D:\dev-tools\redis\redis-server.exe',
  'D:\soft_wares\redis*\**\redis-server.exe',
  'C:\Program Files\Redis\redis-server.exe'
)
$redisCli = Check-Tool '必须' 'Redis CLI' '8.0.3' 'redis-cli.exe' @('--version') 'redis-cli ([0-9][^\s]+)' @('REDIS_HOME') @(
  'D:\dev-tools\redis\*\redis-cli.exe',
  'D:\dev-tools\redis\redis-cli.exe',
  'D:\soft_wares\redis*\**\redis-cli.exe',
  'C:\Program Files\Redis\redis-cli.exe'
)
$vscode = Check-Tool '推荐' 'VS Code' '稳定版' 'code.cmd' @('--version') '([0-9]+\.[0-9]+\.[0-9]+)' @() @(
  'D:\soft_wares\vscode\Microsoft VS Code\bin\code.cmd',
  "$env:LOCALAPPDATA\Programs\Microsoft VS Code\bin\code.cmd",
  'C:\Program Files\Microsoft VS Code\bin\code.cmd'
)
$idea = Check-FileVersionTool '推荐' 'IntelliJ IDEA' '2025.x 或相近版本' 'idea64.exe' @(
  'D:\Program Files\JetBrains\IntelliJ IDEA*\bin\idea64.exe',
  'C:\Program Files\JetBrains\IntelliJ IDEA*\bin\idea64.exe'
)
$apifox = Check-FileVersionTool '推荐' 'Apifox' '2.7.44' 'Apifox.exe' @(
  'D:\dev-tools\apifox\*\Apifox.exe',
  'D:\soft_wares2\APIfox_\Apifox.exe',
  "$env:LOCALAPPDATA\Programs\Apifox\Apifox.exe",
  'C:\Program Files\Apifox\Apifox.exe'
)

Check-MySqlRuntime -MysqlClient $mysql
Check-RedisRuntime -RedisCli $redisCli
Check-MavenProjectDeps
Check-NodeProjectDeps

Write-Report

Write-Host ""
Write-Host "Dependency check completed."
Write-Host "Report: $ReportFile"
Write-Host ""
Write-Host "Summary:"
Write-Host "  Aligned: $($Aligned.Count)"
Write-Host "  Acceptable / pending confirmation: $($Acceptable.Count)"
Write-Host "  Need adjustment: $($NeedAdjust.Count)"
Write-Host "  Missing: $($Missing.Count)"
Write-Host "  Project not configured: $($ProjectNotConfigured.Count)"
Write-Host "  Failed: $($Failed.Count)"

if (-not $VerboseMode) {
  Remove-Item -LiteralPath $TmpDir -Recurse -Force -ErrorAction SilentlyContinue
}

exit 0

