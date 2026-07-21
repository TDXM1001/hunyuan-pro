param(
    [string]$MySqlClient = 'F:\phpstudy\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysql.exe',
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 3306,
    [string]$DbName = 'hunyuan_a1_it',
    [string]$DbUsername = 'root',
    [string]$DbPassword = 'root',
    [string]$RedisHost = '127.0.0.1',
    [int]$RedisPort = 6379,
    [int]$RedisDatabase = 15
)

$ErrorActionPreference = 'Stop'

if ($DbName -notmatch '_it$') {
    throw "Integration database name must end with '_it': $DbName"
}

if (-not (Test-Path -LiteralPath $MySqlClient -PathType Leaf)) {
    throw "phpStudy MySQL client not found: $MySqlClient"
}

$managedEnvironment = @(
    'HUNYUAN_IT_ENABLED',
    'HUNYUAN_IT_DB_URL',
    'HUNYUAN_IT_DB_USERNAME',
    'HUNYUAN_IT_DB_PASSWORD',
    'HUNYUAN_IT_REDIS_HOST',
    'HUNYUAN_IT_REDIS_PORT',
    'HUNYUAN_IT_REDIS_DATABASE',
    'MYSQL_PWD'
)
$previousEnvironment = @{}
foreach ($name in $managedEnvironment) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $env:MYSQL_PWD = $DbPassword
    $createDatabaseSql = "CREATE DATABASE IF NOT EXISTS ``$DbName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    & $MySqlClient "--host=$DbHost" "--port=$DbPort" "--user=$DbUsername" --execute=$createDatabaseSql
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create or verify integration database '$DbName'"
    }

    $env:HUNYUAN_IT_ENABLED = 'true'
    $env:HUNYUAN_IT_DB_URL = "jdbc:mysql://${DbHost}:$DbPort/${DbName}?characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    $env:HUNYUAN_IT_DB_USERNAME = $DbUsername
    $env:HUNYUAN_IT_DB_PASSWORD = $DbPassword
    $env:HUNYUAN_IT_REDIS_HOST = $RedisHost
    $env:HUNYUAN_IT_REDIS_PORT = [string]$RedisPort
    $env:HUNYUAN_IT_REDIS_DATABASE = [string]$RedisDatabase

    $backendRoot = Split-Path -Parent $PSScriptRoot
    Push-Location $backendRoot
    try {
        & mvn '-pl' 'hunyuan-admin' '-am' '-Dtest=FlywayMigrationTest,InitialAdminBootstrapIntegrationTest,RedisIsolationTest' '-Dsurefire.failIfNoSpecifiedTests=false' 'clean' 'test'
        if ($LASTEXITCODE -ne 0) {
            throw 'Isolated integration tests failed'
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    foreach ($name in $managedEnvironment) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
}
