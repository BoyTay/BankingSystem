$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$targetRoot = Join-Path $projectRoot 'target'
$inputDir = Join-Path $targetRoot 'desktop-input'
$dependencyDir = Join-Path $targetRoot 'desktop-dependency'
$outputDir = Join-Path $targetRoot 'desktop'
$appDir = Join-Path $outputDir 'VietBank'

$jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackageCommand) {
    throw 'Can JDK 17+ co jpackage trong PATH de dong goi ung dung Windows.'
}
$env:JAVA_HOME = Split-Path (Split-Path $jpackageCommand.Source -Parent) -Parent

Push-Location $projectRoot
try {
    foreach ($directory in @($inputDir, $dependencyDir, $appDir)) {
        $fullPath = [IO.Path]::GetFullPath($directory)
        $targetPrefix = [IO.Path]::GetFullPath($targetRoot).TrimEnd('\') + '\'
        if (-not $fullPath.StartsWith($targetPrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Duong dan build khong hop le: $fullPath"
        }
        if (Test-Path -LiteralPath $fullPath) {
            Remove-Item -LiteralPath $fullPath -Recurse -Force
        }
    }

    & .\mvnw.cmd -q -DskipTests package dependency:copy-dependencies `
        -DincludeScope=runtime "-DoutputDirectory=$dependencyDir"
    if ($LASTEXITCODE -ne 0) { throw 'Maven build that bai.' }

    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $targetRoot 'banking-system-1.0-SNAPSHOT.jar') -Destination $inputDir
    Copy-Item -Path (Join-Path $dependencyDir '*.jar') -Destination $inputDir

    & $jpackageCommand.Source --type app-image --name VietBank --app-version 1.0.0 `
        --input $inputDir --dest $outputDir `
        --main-jar banking-system-1.0-SNAPSHOT.jar `
        --main-class com.banking.ui.GuiLauncher
    if ($LASTEXITCODE -ne 0) { throw 'jpackage that bai.' }

    Write-Host "Da dong goi: $appDir\VietBank.exe"
    Write-Host 'Chep ca thu muc VietBank sang may Windows khac de chay, khong can cai JDK.'
}
finally {
    Pop-Location
}
