[CmdletBinding()]
param(
  [int]$BackendPort = 8080,
  [int]$FrontendPort = 5173,
  [ValidateSet('127.0.0.1', '0.0.0.0')]
  [string]$FrontendHost = '127.0.0.1',
  [switch]$KillPortProcess,
  [switch]$AutoPort,
  [switch]$Restart,
  [switch]$StopOnly,
  [switch]$CheckOnly,
  [switch]$SkipInstall,
  [switch]$InstallFrontendDeps,
  [switch]$RunTests,
  [switch]$BuildFrontend,
  [switch]$OpenBrowser,
  [int]$StartupTimeoutSec = 90
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RootDir = $PSScriptRoot
$BackendDir = Join-Path $RootDir 'backend'
$FrontendDir = Join-Path $RootDir 'frontend'
$RunDir = Join-Path $RootDir 'tmp\run'

function Write-Step {
  param([string]$Message)
  Write-Host ''
  Write-Host "==> $Message"
}

function Write-Info {
  param([string]$Message)
  Write-Host "    $Message"
}

function Resolve-Tool {
  param([string]$Name)

  $command = Get-Command $Name -ErrorAction Stop
  $source = $command.Source
  if (-not $source) {
    $source = $command.Path
  }
  if ($source -and $source.EndsWith('.ps1')) {
    $cmdPath = [System.IO.Path]::ChangeExtension($source, '.cmd')
    if (Test-Path -LiteralPath $cmdPath) {
      return $cmdPath
    }
  }
  return $source
}

function Invoke-Native {
  param(
    [string]$File,
    [string[]]$ToolArgs,
    [string]$WorkingDirectory
  )

  Push-Location $WorkingDirectory
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    & $File @ToolArgs 2>&1 | ForEach-Object {
      Write-Host $_
    }
    $exitCode = $LASTEXITCODE
    return $exitCode
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
    Pop-Location
  }
}

function Invoke-StepCommand {
  param(
    [string]$Label,
    [string]$File,
    [string[]]$ToolArgs,
    [string]$WorkingDirectory
  )

  Write-Step $Label
  $exitCode = Invoke-Native -File $File -ToolArgs $ToolArgs -WorkingDirectory $WorkingDirectory
  if ($exitCode -ne 0) {
    throw "$Label failed with exit code $exitCode."
  }
}

function Show-Version {
  param(
    [string]$Name,
    [string]$File,
    [string[]]$ToolArgs
  )

  Write-Info $Name
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    $output = & $File @ToolArgs 2>&1
    $exitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }
  foreach ($line in $output) {
    Write-Info ("  " + $line.ToString())
  }
  if ($exitCode -ne 0) {
    throw "$Name check failed with exit code $exitCode."
  }
}

function Get-PortListeners {
  param([int]$Port)

  $connections = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
  foreach ($connection in $connections) {
    $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
    $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
    $processName = ''
    $commandLine = ''
    if ($process) {
      $processName = $process.ProcessName
    }
    if ($cim) {
      $commandLine = $cim.CommandLine
    }
    [PSCustomObject]@{
      Port = $Port
      Pid = $connection.OwningProcess
      ProcessName = $processName
      CommandLine = $commandLine
    }
  }
}

function Format-Listeners {
  param([object[]]$Listeners)

  if ($Listeners.Count -eq 0) {
    return 'none'
  }
  $parts = @()
  foreach ($listener in $Listeners) {
    $parts += "pid=$($listener.Pid), name=$($listener.ProcessName), cmd=$($listener.CommandLine)"
  }
  return ($parts -join '; ')
}

function Stop-PortListeners {
  param(
    [int]$Port,
    [string]$Name
  )

  $listeners = @(Get-PortListeners -Port $Port)
  foreach ($pidValue in @($listeners | Select-Object -ExpandProperty Pid -Unique)) {
    Write-Info "Stopping $Name port $Port process pid=$pidValue."
    Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
  }
  Start-Sleep -Seconds 2
}

function Stop-ProcessFromPidFile {
  param(
    [string]$Name,
    [string]$PidFile
  )

  if (-not (Test-Path -LiteralPath $PidFile)) {
    return
  }

  $raw = (Get-Content -LiteralPath $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
  Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
  if (-not $raw) {
    return
  }

  $pidValue = 0
  if (-not [int]::TryParse($raw, [ref]$pidValue)) {
    return
  }

  $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
  if ($process) {
    Write-Info "Stopping managed $Name process pid=$pidValue."
    Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
  }
}

function Stop-ManagedProcesses {
  Write-Step 'Stopping managed processes'
  Stop-ProcessFromPidFile -Name 'frontend command' -PidFile (Join-Path $RunDir 'frontend.command.pid')
  Stop-ProcessFromPidFile -Name 'frontend listener' -PidFile (Join-Path $RunDir 'frontend.pid')
  Stop-ProcessFromPidFile -Name 'backend command' -PidFile (Join-Path $RunDir 'backend.command.pid')
  Stop-ProcessFromPidFile -Name 'backend listener' -PidFile (Join-Path $RunDir 'backend.pid')
  Start-Sleep -Seconds 2
}

function Get-HttpResult {
  param([string]$Url)

  try {
    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
    return [PSCustomObject]@{
      Ok = $true
      StatusCode = $response.StatusCode
      Content = $response.Content
    }
  } catch {
    return [PSCustomObject]@{
      Ok = $false
      StatusCode = 0
      Content = $_.Exception.Message
    }
  }
}

function Test-BackendReady {
  param([int]$Port)

  $result = Get-HttpResult -Url "http://localhost:$Port/api/health"
  return ($result.Ok -and $result.StatusCode -eq 200 -and $result.Content -match 'campus-trade-backend')
}

function Test-FrontendReady {
  param([int]$Port)

  $page = Get-HttpResult -Url "http://localhost:$Port/"
  if (-not ($page.Ok -and $page.StatusCode -eq 200)) {
    return $false
  }

  $proxy = Get-HttpResult -Url "http://localhost:$Port/api/health"
  return ($proxy.Ok -and $proxy.StatusCode -eq 200 -and $proxy.Content -match 'campus-trade-backend')
}

function Wait-ForCondition {
  param(
    [string]$Name,
    [scriptblock]$Condition,
    [int]$TimeoutSec
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  do {
    if (& $Condition) {
      return
    }
    Start-Sleep -Seconds 2
  } while ((Get-Date) -lt $deadline)

  throw "$Name was not ready within $TimeoutSec seconds."
}

function Find-FreePort {
  param([int]$StartPort)

  for ($port = $StartPort; $port -lt ($StartPort + 100); $port++) {
    $listeners = @(Get-PortListeners -Port $port)
    if ($listeners.Count -eq 0) {
      return $port
    }
  }
  throw "No free port found starting at $StartPort."
}

function Resolve-ServicePort {
  param(
    [string]$Name,
    [int]$Port,
    [scriptblock]$ReadyProbe
  )

  $listeners = @(Get-PortListeners -Port $Port)
  if ($listeners.Count -eq 0) {
    return [PSCustomObject]@{ Port = $Port; Reuse = $false }
  }

  if ((-not $Restart) -and (& $ReadyProbe $Port)) {
    Write-Info "$Name already responds on port $Port; reusing it."
    return [PSCustomObject]@{ Port = $Port; Reuse = $true }
  }

  if ($KillPortProcess) {
    Write-Info "$Name port $Port is occupied: $(Format-Listeners -Listeners $listeners)"
    Stop-PortListeners -Port $Port -Name $Name
    return [PSCustomObject]@{ Port = $Port; Reuse = $false }
  }

  if ($AutoPort) {
    $nextPort = Find-FreePort -StartPort ($Port + 1)
    Write-Info "$Name port $Port is occupied; using $nextPort because -AutoPort was provided."
    return [PSCustomObject]@{ Port = $nextPort; Reuse = $false }
  }

  throw "$Name port $Port is occupied. Details: $(Format-Listeners -Listeners $listeners). Use -KillPortProcess, -AutoPort, or -Restart."
}

function Convert-ToNodePath {
  param([string]$Path)
  return ((Resolve-Path -LiteralPath $Path).ProviderPath -replace '\\', '/')
}

function New-RuntimeViteConfig {
  param(
    [int]$BackendPortValue,
    [int]$FrontendPortValue,
    [string]$HostValue
  )

  $configPath = Join-Path $RunDir 'vite.runtime.config.mjs'
  $frontendPath = Convert-ToNodePath -Path $FrontendDir
  $packageJsonPath = Convert-ToNodePath -Path (Join-Path $FrontendDir 'package.json')

  $config = @"
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const require = createRequire('$packageJsonPath');
const { defineConfig } = await import(pathToFileURL(require.resolve('vite')).href);
const vue = (await import(pathToFileURL(require.resolve('@vitejs/plugin-vue')).href)).default;

export default defineConfig({
  root: '$frontendPath',
  plugins: [vue()],
  server: {
    host: '$HostValue',
    port: $FrontendPortValue,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:$BackendPortValue',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:$BackendPortValue',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom'
  }
});
"@

  Set-Content -LiteralPath $configPath -Value $config -Encoding UTF8
  return $configPath
}

function Write-PidFile {
  param(
    [string]$Path,
    [int]$PidValue
  )
  Set-Content -LiteralPath $Path -Value $PidValue -Encoding ASCII
}

function Quote-CmdArg {
  param([string]$Value)
  return '"' + ($Value -replace '"', '""') + '"'
}

function Start-BackgroundCommand {
  param(
    [string]$Name,
    [string]$Command,
    [string[]]$ToolArgs,
    [string]$WorkingDirectory,
    [string]$StdOut,
    [string]$StdErr
  )

  $quotedParts = @((Quote-CmdArg -Value $Command))
  foreach ($arg in $ToolArgs) {
    $quotedParts += (Quote-CmdArg -Value $arg)
  }
  $commandOnly = ($quotedParts -join ' ')
  $cmdLine = ($commandOnly + ' >> ' + (Quote-CmdArg -Value $StdOut) + ' 2>> ' + (Quote-CmdArg -Value $StdErr))
  $scriptPath = Join-Path $RunDir ("start-$($Name.ToLowerInvariant()).cmd")
  $scriptContent = @(
    '@echo off',
    ('cd /d ' + (Quote-CmdArg -Value $WorkingDirectory)),
    ('echo [%date% %time%] Starting ' + $Name + ' > ' + (Quote-CmdArg -Value $StdOut)),
    ('echo Command: ' + $commandOnly + ' >> ' + (Quote-CmdArg -Value $StdOut)),
    $cmdLine
  )
  Set-Content -LiteralPath $scriptPath -Value $scriptContent -Encoding ASCII
  $process = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', (Quote-CmdArg -Value $scriptPath)) -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -PassThru
  Write-Info "$Name command pid: $($process.Id)"
  Write-Info "$Name command file: $scriptPath"
  Write-Info "$Name stdout: $StdOut"
  Write-Info "$Name stderr: $StdErr"
  return $process
}

New-Item -ItemType Directory -Force -Path $RunDir | Out-Null

Write-Step 'Project startup configuration'
Write-Info "Root: $RootDir"
Write-Info "Backend port: $BackendPort"
Write-Info "Frontend port: $FrontendPort"
Write-Info "Frontend host: $FrontendHost"
Write-Info "Logs: $RunDir"

if ($Restart) {
  Stop-ManagedProcesses
}

if ($StopOnly) {
  Stop-ManagedProcesses
  if ($KillPortProcess) {
    Stop-PortListeners -Port $FrontendPort -Name 'frontend'
    Stop-PortListeners -Port $BackendPort -Name 'backend'
  }
  Write-Info 'StopOnly completed.'
  exit 0
}

Write-Step 'Checking required tools'
$Git = Resolve-Tool 'git'
$Java = Resolve-Tool 'java'
$Maven = Resolve-Tool 'mvn'
$Node = Resolve-Tool 'node'
$Pnpm = Resolve-Tool 'pnpm'

Show-Version -Name 'Java' -File $Java -ToolArgs @('-version')
Show-Version -Name 'Maven' -File $Maven -ToolArgs @('-version')
Show-Version -Name 'Node.js' -File $Node -ToolArgs @('-v')
Show-Version -Name 'pnpm' -File $Pnpm -ToolArgs @('-v')

Write-Step 'Checking Git status'
Push-Location $RootDir
try {
  $gitStatus = & $Git status --short
} finally {
  Pop-Location
}
if ($gitStatus) {
  Write-Info 'Git status is not clean:'
  foreach ($line in $gitStatus) {
    Write-Info "  $line"
  }
} else {
  Write-Info 'Git status is clean.'
}

Write-Step 'Checking ports'
$backendProbe = { param($port) Test-BackendReady -Port $port }
$frontendProbe = { param($port) Test-FrontendReady -Port $port }
$backendDecision = Resolve-ServicePort -Name 'backend' -Port $BackendPort -ReadyProbe $backendProbe
$BackendPort = [int]$backendDecision.Port
$frontendDecision = Resolve-ServicePort -Name 'frontend' -Port $FrontendPort -ReadyProbe $frontendProbe
$FrontendPort = [int]$frontendDecision.Port

Write-Info "Backend final port: $BackendPort"
Write-Info "Frontend final port: $FrontendPort"

if ($CheckOnly) {
  Write-Info 'CheckOnly completed. No service was started.'
  exit 0
}

if ($RunTests) {
  Invoke-StepCommand -Label 'Running backend tests' -File $Maven -ToolArgs @('-f', (Join-Path $BackendDir 'pom.xml'), 'test') -WorkingDirectory $RootDir
}

if ((-not $SkipInstall) -and ($InstallFrontendDeps -or (-not (Test-Path -LiteralPath (Join-Path $FrontendDir 'node_modules'))))) {
  Invoke-StepCommand -Label 'Installing frontend dependencies' -File $Pnpm -ToolArgs @('install') -WorkingDirectory $FrontendDir
} else {
  Write-Step 'Installing frontend dependencies'
  Write-Info 'Skipped. Use -InstallFrontendDeps to force pnpm install.'
}

if ($RunTests) {
  Invoke-StepCommand -Label 'Running frontend tests' -File $Pnpm -ToolArgs @('test') -WorkingDirectory $FrontendDir
}

if ($BuildFrontend) {
  Invoke-StepCommand -Label 'Building frontend' -File $Pnpm -ToolArgs @('build') -WorkingDirectory $FrontendDir
}

if (-not $backendDecision.Reuse) {
  Write-Step 'Starting backend'
  $backendOut = Join-Path $RunDir "backend-$BackendPort.out.log"
  $backendErr = Join-Path $RunDir "backend-$BackendPort.err.log"
  Remove-Item -LiteralPath $backendOut, $backendErr -Force -ErrorAction SilentlyContinue

  $backendArgs = @(
    '-f',
    (Join-Path $BackendDir 'pom.xml'),
    'spring-boot:run',
    "-Dspring-boot.run.arguments=--server.port=$BackendPort"
  )
  $backendProcess = Start-BackgroundCommand -Name 'Backend' -Command $Maven -ToolArgs $backendArgs -WorkingDirectory $RootDir -StdOut $backendOut -StdErr $backendErr
  Write-PidFile -Path (Join-Path $RunDir 'backend.command.pid') -PidValue $backendProcess.Id

  Wait-ForCondition -Name 'backend' -TimeoutSec $StartupTimeoutSec -Condition { Test-BackendReady -Port $BackendPort }
  $backendListener = @(Get-PortListeners -Port $BackendPort | Select-Object -First 1)
  if ($backendListener.Count -gt 0) {
    Write-PidFile -Path (Join-Path $RunDir 'backend.pid') -PidValue $backendListener[0].Pid
    Write-Info "Backend listener pid: $($backendListener[0].Pid)"
  }
} else {
  Write-Step 'Starting backend'
  Write-Info 'Skipped. Existing backend was reused.'
}

$viteConfigPath = New-RuntimeViteConfig -BackendPortValue $BackendPort -FrontendPortValue $FrontendPort -HostValue $FrontendHost

if (-not $frontendDecision.Reuse) {
  Write-Step 'Starting frontend'
  $frontendOut = Join-Path $RunDir "frontend-$FrontendPort.out.log"
  $frontendErr = Join-Path $RunDir "frontend-$FrontendPort.err.log"
  Remove-Item -LiteralPath $frontendOut, $frontendErr -Force -ErrorAction SilentlyContinue

  $frontendArgs = @('exec', 'vite', '--config', $viteConfigPath)
  $frontendProcess = Start-BackgroundCommand -Name 'Frontend' -Command $Pnpm -ToolArgs $frontendArgs -WorkingDirectory $FrontendDir -StdOut $frontendOut -StdErr $frontendErr
  Write-PidFile -Path (Join-Path $RunDir 'frontend.command.pid') -PidValue $frontendProcess.Id

  Wait-ForCondition -Name 'frontend' -TimeoutSec $StartupTimeoutSec -Condition { Test-FrontendReady -Port $FrontendPort }
  $frontendListener = @(Get-PortListeners -Port $FrontendPort | Select-Object -First 1)
  if ($frontendListener.Count -gt 0) {
    Write-PidFile -Path (Join-Path $RunDir 'frontend.pid') -PidValue $frontendListener[0].Pid
    Write-Info "Frontend listener pid: $($frontendListener[0].Pid)"
  }
} else {
  Write-Step 'Starting frontend'
  Write-Info 'Skipped. Existing frontend was reused.'
}

$backendHealth = "http://localhost:$BackendPort/api/health"
$frontendUrl = "http://localhost:$FrontendPort/"
$proxyHealth = "http://localhost:$FrontendPort/api/health"

Write-Step 'Final verification'
$backendResult = Get-HttpResult -Url $backendHealth
$frontendResult = Get-HttpResult -Url $frontendUrl
$proxyResult = Get-HttpResult -Url $proxyHealth

Write-Info "Backend health: HTTP $($backendResult.StatusCode) $backendHealth"
Write-Info "Frontend page: HTTP $($frontendResult.StatusCode) $frontendUrl"
Write-Info "Frontend proxy health: HTTP $($proxyResult.StatusCode) $proxyHealth"

if (-not ($backendResult.Ok -and $frontendResult.Ok -and $proxyResult.Ok)) {
  throw 'Final verification failed. Check logs in the run directory.'
}

Write-Host ''
Write-Host 'Startup completed.'
Write-Host "Frontend: $frontendUrl"
Write-Host "Backend health: $backendHealth"
Write-Host "Frontend proxy health: $proxyHealth"
Write-Host "Run logs: $RunDir"

if ($OpenBrowser) {
  Start-Process $frontendUrl | Out-Null
}
