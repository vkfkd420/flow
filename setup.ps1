<#
.SYNOPSIS
  새 Windows PC에서 flow 로컬 개발 환경을 한 번에 구성한다 (SETUP.md 1~7번 자동화).

.DESCRIPTION
  다시 실행해도 안전하다. 이미 된 단계는 확인만 하고 건너뛰며, 기존 MySQL 데이터 폴더와 backend/.env는 덮어쓰지 않는다.
  비밀번호와 액세스 키는 실행 중에 직접 입력한다 (화면에 표시되지 않음, 비밀번호는 backend/.env에만 저장).

  실행 (저장소 루트에서):
    powershell -ExecutionPolicy Bypass -File .\setup.ps1

  주의: 이 파일은 UTF-8 BOM으로 저장해야 한다. Windows PowerShell 5.1은 BOM이 없으면 한글을 CP949로 읽어 깨진다.
#>
param(
  # 내부용: 관리자 권한으로 MySQL 데이터 폴더 초기화와 서비스 등록만 수행한다
  [switch]$InitMySqlService,
  [string]$InitLog
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$Root = $PSScriptRoot
$MySqlBase = 'C:\Program Files\MySQL\MySQL Server 8.4'
$MySqlData = 'C:\ProgramData\MySQL\MySQL Server 8.4'
$MySqlService = 'MySQL84'
$Mysql = "$MySqlBase\bin\mysql.exe"
$EnvFile = Join-Path $Root 'backend\.env'
$Bucket = 'min420-flow-uploads'
$Region = 'ap-northeast-2'
$Utf8NoBom = New-Object Text.UTF8Encoding($false)

# ---------------------------------------------------------------------------
# 관리자 권한 하위 작업: MySQL 데이터 폴더 초기화 + Windows 서비스 등록
# ---------------------------------------------------------------------------
if ($InitMySqlService) {
  try {
    if (Test-Path "$MySqlData\Data") { throw "데이터 폴더가 이미 있어 덮어쓰지 않습니다: $MySqlData\Data" }
    New-Item -ItemType Directory -Force $MySqlData | Out-Null
    $base = $MySqlBase -replace '\\', '/'
    $data = ($MySqlData -replace '\\', '/') + '/Data'
    [IO.File]::WriteAllText("$MySqlData\my.ini", "[mysqld]`r`nbasedir=$base`r`ndatadir=$data`r`nport=3306`r`n", $Utf8NoBom)
    # root 비밀번호 없이 초기화 → 서비스 시작 직후 일반 권한 쪽에서 비밀번호를 설정한다
    & "$MySqlBase\bin\mysqld.exe" --defaults-file="$MySqlData\my.ini" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) { throw "mysqld --initialize 실패 (종료 코드 $LASTEXITCODE)" }
    & "$MySqlBase\bin\mysqld.exe" --install $MySqlService --defaults-file="$MySqlData\my.ini"
    if ($LASTEXITCODE -ne 0) { throw "서비스 등록 실패 (종료 코드 $LASTEXITCODE)" }
    Start-Service $MySqlService
    [IO.File]::WriteAllText($InitLog, 'OK', $Utf8NoBom)
    exit 0
  } catch {
    [IO.File]::WriteAllText($InitLog, "FAIL $($_.Exception.Message)", $Utf8NoBom)
    exit 1
  }
}

# ---------------------------------------------------------------------------
# 공통 함수
# ---------------------------------------------------------------------------
$Results = New-Object System.Collections.Generic.List[object]

function Step($name, [scriptblock]$body) {
  Write-Host ''
  Write-Host "== $name" -ForegroundColor Cyan
  $status = & $body
  if (-not $status) { $status = '완료' }
  Write-Host "   → $status" -ForegroundColor Green
  $Results.Add([pscustomobject]@{ Step = $name; Result = $status })
}

function Update-SessionPath {
  $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
}

function Read-Secret($prompt) {
  $secure = Read-Host $prompt -AsSecureString
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Read-NewSecret($prompt) {
  while ($true) {
    $first = Read-Secret $prompt
    if ($first.Length -eq 0) { Write-Host '   비어 있을 수 없습니다.' -ForegroundColor Yellow; continue }
    if ($first.Contains("'")) { Write-Host "   작은따옴표(')는 쓸 수 없습니다." -ForegroundColor Yellow; continue }
    $second = Read-Secret "$prompt (확인)"
    if ($first -ceq $second) { return $first }
    Write-Host '   두 입력이 다릅니다. 다시 입력하세요.' -ForegroundColor Yellow
  }
}

function Confirm-Yes($question, [bool]$defaultYes) {
  $hint = if ($defaultYes) { '[Y/n]' } else { '[y/N]' }
  $answer = Read-Host "   $question $hint"
  if ([string]::IsNullOrWhiteSpace($answer)) { return $defaultYes }
  return ($answer -match '^(y|yes)$')
}

function ConvertTo-SqlString($value) {
  return $value.Replace('\', '\\').Replace("'", "''")
}

# mysql 실행 (비밀번호는 명령줄이 아니라 이 프로세스의 환경 변수로만 전달)
function Invoke-Mysql($user, $password, $sql, $database) {
  $argList = @('-h', '127.0.0.1', '-P', '3306', '-u', $user, '--default-character-set=utf8mb4', '-N', '-B', '-e', $sql)
  if ($database) { $argList += $database }
  if ($password) { $env:MYSQL_PWD = $password } else { $argList = @('--skip-password') + $argList }
  try {
    $out = & $Mysql @argList
    if ($LASTEXITCODE -ne 0) { throw "mysql 실행 실패 (계정 $user)" }
    return $out
  } finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
  }
}

function Test-MysqlLogin($user, $password) {
  $ErrorActionPreference = 'Continue'
  $env:MYSQL_PWD = $password
  try {
    & $Mysql -h 127.0.0.1 -P 3306 -u $user -N -B -e 'SELECT 1' 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
  } finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
  }
}

function Install-IfMissing($id, [scriptblock]$isInstalled, [string[]]$extra = @()) {
  if (& $isInstalled) { return "$id 있음" }
  Write-Host "   설치 중: $id"
  $argList = @('install', '--id', $id, '-e', '--silent', '--accept-source-agreements', '--accept-package-agreements') + $extra
  & winget @argList | Out-Host
  if ($LASTEXITCODE -ne 0) { throw "winget 설치 실패: $id (종료 코드 $LASTEXITCODE)" }
  Update-SessionPath
  return "$id 설치"
}

function Get-EnvValue($key) {
  if (-not (Test-Path $EnvFile)) { return $null }
  foreach ($line in [IO.File]::ReadAllLines($EnvFile)) {
    if ($line.StartsWith("$key=")) { return $line.Substring($key.Length + 1).Replace('\\', '\') }
  }
  return $null
}

# ---------------------------------------------------------------------------
# 1. 도구 설치
# ---------------------------------------------------------------------------
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
  throw 'winget이 없습니다. Microsoft Store에서 "앱 설치 관리자"를 설치한 뒤 다시 실행하세요.'
}
Write-Host "flow 로컬 환경 구성 (저장소: $Root)" -ForegroundColor White

Step '도구 설치' {
  $done = @(
    Install-IfMissing 'Git.Git' { Get-Command git -ErrorAction SilentlyContinue }
    Install-IfMissing 'EclipseAdoptium.Temurin.17.JDK' { Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue }
    Install-IfMissing 'OpenJS.NodeJS.22' { Get-Command node -ErrorAction SilentlyContinue }
    Install-IfMissing 'Oracle.MySQL' { Test-Path "$MySqlBase\bin\mysqld.exe" } @('--version', '8.4.9')
    Install-IfMissing 'Amazon.AWSCLI' { (Get-Command aws -ErrorAction SilentlyContinue) -or (Test-Path 'C:\Program Files\Amazon\AWSCLIV2\aws.exe') }
  )
  $installed = @($done | Where-Object { $_ -like '* 설치' })
  if ($installed.Count -eq 0) { return '모두 이미 있음' }
  return ($installed -join ', ')
}

Step 'JAVA_HOME (JDK 17)' {
  $jdk = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' | Sort-Object Name -Descending | Select-Object -First 1).FullName
  $userValue = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
  $effective = if ($userValue) { $userValue } else { [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
  # java -version 대신 JDK의 release 파일로 버전을 확인
  $isJava17 = $effective -and (Test-Path "$effective\release") -and ((Get-Content "$effective\release" -Raw) -match 'JAVA_VERSION="17')
  if ($isJava17) {
    $env:JAVA_HOME = $effective
    return "이미 17 ($effective)"
  }
  Write-Host "   현재 JAVA_HOME: $(if ($effective) { $effective } else { '(없음)' })"
  if ($userValue -and -not (Confirm-Yes "사용자 JAVA_HOME을 $jdk 로 바꿀까요? (다른 프로젝트가 쓰고 있다면 영향이 있음)" $true)) {
    $env:JAVA_HOME = $jdk
    return "이번 실행에서만 $jdk 사용 (사용자 JAVA_HOME은 그대로)"
  }
  [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
  $env:JAVA_HOME = $jdk
  return "사용자 JAVA_HOME → $jdk"
}

# ---------------------------------------------------------------------------
# 2. MySQL 서버
# ---------------------------------------------------------------------------
$script:RootPassword = $null

Step 'MySQL 서버' {
  $service = Get-Service $MySqlService -ErrorAction SilentlyContinue
  if ($service) {
    if ($service.Status -ne 'Running') {
      try { Start-Service $MySqlService } catch { throw "MySQL 서비스($MySqlService)를 시작하지 못했습니다. 관리자 PowerShell에서 Start-Service $MySqlService 를 실행하세요." }
      return "서비스 있음, 시작함"
    }
    return "서비스 있음 ($MySqlService, 실행 중)"
  }
  if (Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue) {
    return '3306에서 실행 중인 MySQL 사용 (서비스 등록 건너뜀)'
  }

  Write-Host '   데이터 폴더 초기화와 서비스 등록에 관리자 권한이 필요합니다. 권한 확인 창에서 [예]를 누르세요.'
  $log = Join-Path $env:TEMP 'flow-mysql-init.log'
  Remove-Item $log -ErrorAction SilentlyContinue
  $proc = Start-Process powershell -Verb RunAs -Wait -PassThru -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"", '-InitMySqlService', '-InitLog', "`"$log`"")
  $result = if (Test-Path $log) { [IO.File]::ReadAllText($log) } else { '(결과 없음)' }
  if ($proc.ExitCode -ne 0 -or -not $result.StartsWith('OK')) { throw "MySQL 초기화 실패: $result" }

  $script:RootPassword = Read-NewSecret '   MySQL root 비밀번호를 정하세요'
  Invoke-Mysql 'root' $null "ALTER USER 'root'@'localhost' IDENTIFIED BY '$(ConvertTo-SqlString $script:RootPassword)'" | Out-Null
  return "초기화, 서비스 등록 ($MySqlService, 자동 시작), root 비밀번호 설정"
}

# ---------------------------------------------------------------------------
# 3. DB, 계정, 스키마
# ---------------------------------------------------------------------------
$script:FlowPassword = Get-EnvValue 'DB_PASSWORD'

Step 'DB 구성 (flow)' {
  $flowOk = $script:FlowPassword -and (Test-MysqlLogin 'flow' $script:FlowPassword)
  if (-not $flowOk) {
    if (-not $script:RootPassword) {
      $script:RootPassword = Read-Secret '   MySQL root 비밀번호'
      if (-not (Test-MysqlLogin 'root' $script:RootPassword)) { throw 'root 로그인 실패. 비밀번호를 확인하세요.' }
    }
    if ($script:FlowPassword) {
      Write-Host '   backend/.env의 DB_PASSWORD로 flow 계정에 로그인할 수 없어, flow 계정 비밀번호를 .env 값으로 맞춥니다.'
    } else {
      $script:FlowPassword = Read-NewSecret '   앱 계정(flow) 비밀번호를 정하세요'
    }
    $pw = ConvertTo-SqlString $script:FlowPassword
    Invoke-Mysql 'root' $script:RootPassword @"
CREATE DATABASE IF NOT EXISTS flow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'flow'@'localhost' IDENTIFIED BY '$pw';
ALTER USER 'flow'@'localhost' IDENTIFIED BY '$pw';
GRANT ALL PRIVILEGES ON flow.* TO 'flow'@'localhost';
"@ | Out-Null
  }

  $tables = Invoke-Mysql 'flow' $script:FlowPassword "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'flow' AND UPPER(TABLE_NAME) = 'FILE_EXTENSION_POLICY'"
  if ([int]$tables -gt 0) {
    if ($flowOk) { return '이미 구성됨 (계정, 테이블)' }
    return '계정 설정, 테이블은 이미 있음'
  }
  foreach ($file in @('schema.sql', 'seed.sql')) {
    $path = (Join-Path $Root "db\$file") -replace '\\', '/'
    Invoke-Mysql 'flow' $script:FlowPassword "source $path" 'flow' | Out-Null
  }
  return 'DB·계정 생성, schema.sql → seed.sql 적용'
}

# ---------------------------------------------------------------------------
# 4. backend/.env
# ---------------------------------------------------------------------------
Step 'backend/.env' {
  if (Test-Path $EnvFile) { return '이미 있음 (그대로 둠)' }
  # Spring이 .properties 형식으로 읽으므로 역슬래시는 이스케이프, 파일은 BOM 없는 UTF-8
  $pw = $script:FlowPassword.Replace('\', '\\')
  $content = @(
    '# setup.ps1이 생성 (Git에 올라가지 않음)'
    'DB_URL=jdbc:mysql://localhost:3306/flow'
    'DB_USERNAME=flow'
    "DB_PASSWORD=$pw"
    ''
    "AWS_REGION=$Region"
    "AWS_S3_BUCKET=$Bucket"
  ) -join "`n"
  [IO.File]::WriteAllText($EnvFile, $content + "`n", $Utf8NoBom)
  return '생성 (DB 비밀번호, S3 버킷)'
}

# ---------------------------------------------------------------------------
# 5. S3 자격 증명
# ---------------------------------------------------------------------------
Step 'S3 자격 증명 (aws configure)' {
  Update-SessionPath
  $ErrorActionPreference = 'Continue'
  $arn = & aws sts get-caller-identity --query Arn --output text 2>$null
  if ($LASTEXITCODE -eq 0) { return "이미 등록됨 ($arn)" }
  Write-Host '   IAM 콘솔 → 사용자 flow-app → 보안 자격 증명 → 액세스 키 만들기 (컴퓨터마다 새 키 권장)'
  if (-not (Confirm-Yes '지금 액세스 키를 입력할까요? (건너뛰면 파일 업로드만 503, 나머지는 동작)' $false)) {
    return '건너뜀 (나중에 aws configure)'
  }
  Write-Host "   Default region은 $Region, output은 json 으로 입력하세요."
  # 입력 안내가 보이도록 콘솔을 그대로 물려준다 (& 로 실행하면 출력이 결과값으로 잡힘)
  Start-Process aws -ArgumentList 'configure' -NoNewWindow -Wait
  $arn = & aws sts get-caller-identity --query Arn --output text 2>$null
  if ($LASTEXITCODE -ne 0) { return '입력했지만 확인 실패 (키를 다시 확인하세요)' }
  return "등록됨 ($arn)"
}

# ---------------------------------------------------------------------------
# 6. 의존성 설치와 테스트
# ---------------------------------------------------------------------------
Step '프론트 의존성 (npm ci)' {
  Push-Location (Join-Path $Root 'frontend')
  try {
    & npm ci --no-audit --no-fund | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "npm ci 실패 (종료 코드 $LASTEXITCODE)" }
  } finally { Pop-Location }
}

Step '백엔드 테스트 (mvnw clean test)' {
  Push-Location (Join-Path $Root 'backend')
  try {
    & .\mvnw.cmd -B clean test | Select-String -Pattern 'Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+$', 'BUILD (SUCCESS|FAILURE)', '\[ERROR\]' | ForEach-Object { Write-Host "   $_" }
    if ($LASTEXITCODE -ne 0) { throw "테스트 실패 (종료 코드 $LASTEXITCODE). 위 [ERROR]와 SETUP.md 9번 문제 해결을 참고하세요." }
  } finally { Pop-Location }
}

# ---------------------------------------------------------------------------
# 7. 결과
# ---------------------------------------------------------------------------
Write-Host ''
Write-Host '== 결과' -ForegroundColor Cyan
$Results | Format-Table -AutoSize | Out-String | Write-Host

Write-Host '실행 (각각 새 터미널):' -ForegroundColor White
Write-Host '  cd backend;  .\mvnw.cmd spring-boot:run'
Write-Host '  cd frontend; npm run serve        → http://localhost:3000'
Write-Host ''
Write-Host '직접 해야 하는 것:' -ForegroundColor White
if (-not (& git config --global user.name)) { Write-Host '  - git config --global user.name / user.email (커밋할 경우)' }
Write-Host '  - Claude Code 지침: 기존 PC의 ~/.claude/CLAUDE.md 복사 (SETUP.md 10번)'
Write-Host '  - AWS 배포까지 할 경우: SSH 키 옮기기, 보안 그룹에 IP 추가 (SETUP.md 8번)'
