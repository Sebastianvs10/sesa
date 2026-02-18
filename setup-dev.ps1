# setup-dev.ps1 - Script de configuración para desarrollo (Windows)

Write-Host "🚀 SESA - Script de Configuración Desarrollo (Windows)" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""

function Print-Status {
    param([string]$message)
    Write-Host "✓ $message" -ForegroundColor Green
}

function Print-Error {
    param([string]$message)
    Write-Host "✗ $message" -ForegroundColor Red
}

function Print-Warning {
    param([string]$message)
    Write-Host "⚠ $message" -ForegroundColor Yellow
}

# Verificar pre-requisitos
Write-Host "📋 Verificando pre-requisitos..." -ForegroundColor Cyan
Write-Host ""

# Java
$javaCheck = java -version 2>&1
if ($LASTEXITCODE -ne 0) {
    Print-Error "Java no está instalado. Instala JDK 17+"
    exit 1
} else {
    Print-Status "Java instalado: $($javaCheck | Select-Object -First 1)"
}

# Maven wrapper
if (-not (Test-Path "sesa-backend\mvnw.cmd")) {
    Print-Error "Maven wrapper no encontrado en sesa-backend\mvnw.cmd"
    exit 1
}
Print-Status "Maven wrapper encontrado"

# Node.js
$nodeVersion = node -v 2>&1
if ($LASTEXITCODE -ne 0) {
    Print-Error "Node.js no está instalado. Instala Node 18+"
    exit 1
} else {
    Print-Status "Node.js instalado: $nodeVersion"
}

# PostgreSQL
$pgqlCheck = Get-Process -Name postgres -ErrorAction SilentlyContinue
if ($pgqlCheck) {
    Print-Status "PostgreSQL está corriendo"
} else {
    Print-Warning "PostgreSQL no está corriendo. Asegúrate de iniciarlo antes de ejecutar el backend"
}

Write-Host ""
Write-Host "🔧 Configurando Backend..." -ForegroundColor Cyan
Write-Host ""

# Backend
Push-Location sesa-backend

Print-Status "Limpiando compilaciones anteriores..."
& .\mvnw.cmd clean -q
if ($LASTEXITCODE -ne 0) {
    Print-Warning "Maven clean falló, continuando..."
}

Print-Status "Descargando dependencias de Maven..."
& .\mvnw.cmd dependency:resolve -q

Pop-Location

Write-Host ""
Write-Host "🔧 Configurando Frontend..." -ForegroundColor Cyan
Write-Host ""

# Frontend
Push-Location sesa-salud

Print-Status "Instalando dependencias npm..."
npm install --legacy-peer-deps
if ($LASTEXITCODE -ne 0) {
    Print-Error "npm install falló"
    Pop-Location
    exit 1
}

Pop-Location

Write-Host ""
Write-Host "🗄️  Base de Datos PostgreSQL" -ForegroundColor Cyan
Write-Host ""

# Test PostgreSQL connection
$testConnection = @"
`$null = [Net.ServicePointManager]::ServerCertificateValidationCallback = {`$true}
`$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if (`$psqlPath) {
    Write-Host 'PostgreSQL CLI encontrado'
} else {
    Write-Host 'PostgreSQL CLI no está en PATH'
}
"@

Invoke-Expression $testConnection

Print-Warning "Verifica que PostgreSQL esté corriendo en localhost:5432 antes de iniciar el backend"

Write-Host ""
Write-Host "✨ Configuración completada!" -ForegroundColor Green
Write-Host ""
Write-Host "📌 Próximos pasos:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Terminal 1 - Iniciar Backend:" -ForegroundColor White
Write-Host "   cd sesa-backend" -ForegroundColor Gray
Write-Host "   .\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=`"--spring.profiles.active=dev`"" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Terminal 2 - Iniciar Frontend:" -ForegroundColor White
Write-Host "   cd sesa-salud" -ForegroundColor Gray
Write-Host "   npm start" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Acceder a la aplicación:" -ForegroundColor White
Write-Host "   Frontend: http://localhost:4200" -ForegroundColor Gray
Write-Host "   Backend:  http://localhost:8080/api" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Credenciales de prueba:" -ForegroundColor White
Write-Host "   Email: admin@sesa.com" -ForegroundColor Gray
Write-Host "   Password: admin123" -ForegroundColor Gray
Write-Host ""
Print-Warning "Nota: Asegúrate de que PostgreSQL esté corriendo antes de iniciar el backend"
Write-Host ""
