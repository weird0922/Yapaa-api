param([switch]$SoloPreparar)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
foreach ($command in @('java.exe', 'sqlcmd.exe')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Instala Java 21 y SQL Server con sqlcmd. Falta $command."
    }
}
function Leer-Propiedad($archivo, $nombre) {
    if (Test-Path -LiteralPath $archivo) {
        $linea = Get-Content -LiteralPath $archivo | Where-Object { $_.StartsWith("$nombre=") } | Select-Object -First 1
        if ($linea) { return $linea.Substring($nombre.Length + 1) }
    }
}
$sqlClave = Leer-Propiedad 'config/app-local.properties' 'spring.datasource.password'
if (-not $sqlClave) {
    $clave = Read-Host 'Contraseña del usuario sa de tu SQL Server local' -AsSecureString
    $sqlClave = [pscredential]::new('sa', $clave).GetNetworkCredential().Password
}
$env:SQLCMDPASSWORD = $sqlClave
$base = 'YAPAA_DEMO_GIT'
function Ejecutar-Sql([string]$catalogo, [string[]]$argumentos) {
    & sqlcmd.exe -S tcp:localhost,1433 -U sa -C -l 5 -b -f 65001 -d $catalogo @argumentos
    if ($LASTEXITCODE -ne 0) { throw "SQL Server devolvió un error en $catalogo." }
}
try {
    $estado = (Ejecutar-Sql 'master' @('-h','-1','-W','-Q',"SET NOCOUNT ON; SELECT IIF(DB_ID(N'$base') IS NULL, 0, 1);") | Out-String).Trim()
    if ($estado -eq '0') {
        $original = Get-Content 'sql/yapaa-scriptsql.sql' -Raw -Encoding UTF8
        $inicio = $original.IndexOf('CREATE DATABASE YAPAA_BD;')
        if ($inicio -lt 0) { throw 'No se encontró el esquema inicial.' }
        $contenido = $original.Substring($inicio).Replace('YAPAA_BD', $base)
        if ($contenido -match '(?i)\bDROP\s+DATABASE\b') { throw 'El esquema no puede eliminar bases.' }
        $temp = [IO.Path]::GetTempFileName()
        try {
            [IO.File]::WriteAllText($temp, $contenido, [Text.UTF8Encoding]::new($false))
            Ejecutar-Sql 'master' @('-i',$temp) | Out-Null
        } finally {
            Remove-Item -LiteralPath $temp -ErrorAction SilentlyContinue
        }
    } elseif ($estado -ne '1') { throw 'No se pudo verificar la base de demostración.' }
    foreach ($script in @('20261123-verificacion-correo.sql','20261123-direcciones-delivery.sql','20261125-stock-al-pagar.sql')) {
        Ejecutar-Sql $base @('-i',"sql/$script") | Out-Null
    }
} finally {
    Remove-Item Env:SQLCMDPASSWORD -ErrorAction SilentlyContinue
}
Write-Host "Base de demostración preparada: $base."
if ($SoloPreparar) { return }
$remitente = Leer-Propiedad 'config/correo-local.properties' 'spring.mail.username'
$correoClave = Leer-Propiedad 'config/correo-local.properties' 'spring.mail.password'
if (-not $correoClave) {
    $carpeta = Join-Path $env:LOCALAPPDATA 'YAPAA-Demo'
    $archivo = Join-Path $carpeta 'correo.xml'
    if (Test-Path -LiteralPath $archivo) {
        $credencial = Import-Clixml -LiteralPath $archivo
    } else {
        $remitente = Read-Host 'Correo Gmail remitente (Enter para usar yapaaservice@gmail.com)'
        if (-not $remitente) { $remitente = 'yapaaservice@gmail.com' }
        $clave = Read-Host 'Contraseña de aplicación de Gmail' -AsSecureString
        if ($clave.Length -eq 0) { throw 'El OTP por correo requiere una contraseña de aplicación.' }
        $credencial = [pscredential]::new($remitente, $clave)
        New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
        $credencial | Export-Clixml -LiteralPath $archivo
    }
    $remitente = $credencial.UserName
    $correoClave = $credencial.GetNetworkCredential().Password
}
if (-not $remitente) { throw 'Falta el correo remitente de la configuración local.' }
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:1433;databaseName=$base;encrypt=true;trustServerCertificate=true"
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = $sqlClave
$env:YAPAA_JWT_SECRETO = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$env:YAPAA_ADMIN_CORREO = 'admin@admin.com'
$env:YAPAA_ADMIN_CLAVE = 'YapaaDemo2026!'
$env:YAPAA_CORREO_MODO = 'smtp'
$env:SPRING_MAIL_USERNAME = $remitente
$env:SPRING_MAIL_PASSWORD = $correoClave -replace '\s', ''
Write-Host 'Administrador de demostración: admin@admin.com / YapaaDemo2026!'
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
