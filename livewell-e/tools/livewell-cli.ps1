param(
  [string]$BaseUrl = "http://127.0.0.1:8000",
  [string]$UserId  = "cli-user-1",
  [switch]$Interactive
)

# Show which PowerShell we're on
Write-Host ("PowerShell version: {0}" -f $PSVersionTable.PSVersion) -ForegroundColor DarkGray

# -------- Helpers --------
function Assert-Ok {
  param($resp, [string]$msg)
  if (-not $resp) { throw $msg }
}

function Has-Property {
  param($obj, [string]$name)
  return $obj -and ($obj.PSObject.Properties.Name -contains $name)
}

function Show-Plan {
  param($plan)
  if (-not $plan) { return }
  # In PS 5.1 JSON arrays are usually Object[]
  $isArray = ($plan -is [System.Array]) -or (Has-Property $plan 'Count')
  if (-not $isArray -or $plan.Count -eq 0) { return }

  Write-Host "`nToday's plan:" -ForegroundColor Cyan
  $i = 1
  foreach ($p in $plan) {
    $title = if (Has-Property $p 'title') { $p.title } else { '' }
    $desc  = if (Has-Property $p 'description') { $p.description } else { '' }
    $mins  = if (Has-Property $p 'duration_minutes') { $p.duration_minutes } else { $null }
    if ($mins) {
      Write-Host ("  {0}. {1} ({2} min) - {3}" -f $i, $title, $mins, $desc)
    } else {
      Write-Host ("  {0}. {1} - {2}" -f $i, $title, $desc)
    }
    $i++
  }
}

function Show-Frailty {
  param($frailty)
  if (-not $frailty) { return }
  $score = if (Has-Property $frailty 'score') { $frailty.score } else { $null }
  $band  = if (Has-Property $frailty 'band')  { $frailty.band }  else { $null }
  if ($score -ne $null -and $band) {
    Write-Host ("`nFrailty: score {0} ({1})" -f $score, $band) -ForegroundColor Yellow
  }
}

function Show-ChatResponse {
  param($resp)
  Assert-Ok $resp "Empty /chat response"
  Write-Host "`nAssistant:" -ForegroundColor Green
  $reply = if (Has-Property $resp 'reply' -and $resp.reply) { [string]$resp.reply } else { "" }
  if ($reply -ne "") { Write-Host $reply } else { Write-Host "<no reply text>" -ForegroundColor DarkYellow }
  Show-Plan $resp.plan
  Show-Frailty $resp.frailty
  Write-Host ""
}

# -------- API Calls --------
function Test-LiveWellHealth {
  $r = Invoke-RestMethod -Uri "$BaseUrl/health" -Method GET -ErrorAction Stop
  Write-Host "[/health] $($r.status)" -ForegroundColor DarkGray
  return $r
}

function Invoke-Prisma7 {
  param([Parameter(Mandatory=$true)] [hashtable]$Answers)
  $json = $Answers | ConvertTo-Json
  $r = Invoke-RestMethod -Uri "$BaseUrl/assessments/prisma7" -Method POST -ContentType "application/json" -Body $json -ErrorAction Stop
  if (-not (Has-Property $r 'score')) { throw "PRISMA response missing 'score'" }
  if (-not (Has-Property $r 'band'))  { throw "PRISMA response missing 'band'" }
  Write-Host ("[/assessments/prisma7] score={0} band={1}" -f $r.score, $r.band) -ForegroundColor DarkGray
  return $r
}

function Invoke-LiveWellChat {
  param(
    [Parameter(Mandatory=$true)] [string]$Message,
    [hashtable]$Prisma7
  )
  $payload = @{
    user_id = $UserId
    message = $Message
  }
  if ($Prisma7) { $payload.prisma7 = $Prisma7 }
  $json = $payload | ConvertTo-Json -Depth 6
  $r = Invoke-RestMethod -Uri "$BaseUrl/chat" -Method POST -ContentType "application/json" -Body $json -ErrorAction Stop
  Show-ChatResponse $r
  return $r
}

# -------- PRISMA-7 Interactive Builder --------
function New-Prisma7Interactive {
  Write-Host "`nAnswer Y/N for each question:" -ForegroundColor Cyan
  function Ask([string]$q) {
    while ($true) {
      $a = Read-Host "$q (y/n)"
      if ($a -match '^(y|yes)$') { return $true }
      if ($a -match '^(n|no)$')  { return $false }
      Write-Host "Please type y or n." -ForegroundColor DarkYellow
    }
  }
  $answers = @{
    over_85                              = (Ask "Are you over 85?")
    male                                 = (Ask "Are you male?")
    health_problems_limit_activities     = (Ask "Do health problems limit your activities?")
    need_help_regularly                  = (Ask "Do you need help on a regular basis?")
    health_problems_stay_home            = (Ask "Do health problems mean you stay at home?")
    count_on_someone_close               = (Ask "Can you count on someone close to you?")
    use_stick_walker_wheelchair          = (Ask "Do you use a stick, walker, or wheelchair?")
  }
  # Quick score preview
  $score = ($answers.GetEnumerator() | Where-Object {$_.Value -eq $true}).Count
  $band = if ($score -ge 3) { "potential_frail" } else { "low" }
  Write-Host ("PRISMA-7 preview: score={0} band={1}" -f $score, $band) -ForegroundColor Yellow
  return $answers
}

# -------- Interactive Chat Loop --------
function Start-LiveWellChat {
  Write-Host "LiveWell-E CLI connected to $BaseUrl as $UserId" -ForegroundColor Cyan
  Write-Host "Commands: /prisma to set answers, /clear to forget, /show to echo current, /exit to quit`n" -ForegroundColor DarkGray
  $script:PRISMA = $null

  Test-LiveWellHealth | Out-Null

  while ($true) {
    $msg = Read-Host "You"
    if (-not $msg) { continue }
    switch -Regex ($msg) {
      '^/exit$'   { break }
      '^/prisma$' { $script:PRISMA = New-Prisma7Interactive; continue }
      '^/clear$'  { $script:PRISMA = $null; Write-Host "Cleared PRISMA answers." -ForegroundColor DarkGray; continue }
      '^/show$'   { if ($script:PRISMA) { $script:PRISMA | ConvertTo-Json -Depth 5 } else { Write-Host "(none)" -ForegroundColor DarkGray }; continue }
      default     { Invoke-LiveWellChat -Message $msg -Prisma7 $script:PRISMA | Out-Null }
    }
  }
  Write-Host "Bye!" -ForegroundColor DarkGray
}

# If -Interactive was passed, start chat mode
if ($Interactive) { Start-LiveWellChat }
