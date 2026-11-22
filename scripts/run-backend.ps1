# Arranca el backend en local cargando antes el .env de la raíz del repo.
# Spring no lee .env por sí solo; este script lo vuelca a variables de entorno del proceso.
#
# Uso (desde cualquier carpeta):
#   .\scripts\run-backend.ps1            # perfil dev
#   .\scripts\run-backend.ps1 -Profile prod
param(
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $key, $value = $line -split "=", 2
        if ($null -eq $value) { return }
        [Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim(), "Process")
    }
    Write-Host "Variables cargadas desde $envFile"
} else {
    Write-Warning "No existe $envFile; copia .env.example a .env. Se usarán los valores por defecto del perfil."
}

$env:SPRING_PROFILES_ACTIVE = $Profile

Push-Location (Join-Path $root "backend")
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
