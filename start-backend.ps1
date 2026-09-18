# Starts turapp: ensures XAMPP MySQL is running, launches the web frontend in its own
# window, then runs the Spring Boot backend in this window.

$xamppMysqlBat = "C:\xampp\mysql_start.bat"
$webDir = Join-Path $PSScriptRoot "turapp-web"

$mysqlRunning = Get-Process -Name "mysqld" -ErrorAction SilentlyContinue
if (-not $mysqlRunning) {
    if (Test-Path $xamppMysqlBat) {
        Write-Host "Starting MySQL via XAMPP..."
        Start-Process -FilePath $xamppMysqlBat -WindowStyle Hidden
        Start-Sleep -Seconds 3
    } else {
        Write-Warning "MySQL is not running and $xamppMysqlBat was not found. Start it manually (e.g. via XAMPP Control Panel)."
    }
} else {
    Write-Host "MySQL is already running."
}

Write-Host "Starting web frontend (turapp-web) in a new window..."
$webCommand = "Set-Location '$webDir'; if (-not (Test-Path 'node_modules')) { npm install }; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $webCommand

Write-Host "Starting Spring Boot backend..."
Set-Location $PSScriptRoot
& .\mvnw.cmd spring-boot:run
