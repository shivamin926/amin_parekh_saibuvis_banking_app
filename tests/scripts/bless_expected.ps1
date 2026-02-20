$ErrorActionPreference = "Stop"

$actualDir = "tests/outputs"
$expectedDir = "tests/expected"

Get-ChildItem "$actualDir\*.out" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $expectedDir $_.Name) -Force
}

Get-ChildItem "$actualDir\*.atf" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $expectedDir $_.Name) -Force
}

Write-Host "Blessed expected outputs from actual outputs."