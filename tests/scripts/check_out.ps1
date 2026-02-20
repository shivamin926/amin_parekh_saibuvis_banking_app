$ErrorActionPreference = "Stop"

$expectedDir = "tests/expected"
$outDir = "tests/outputs"
$fail = $false

Get-ChildItem "$expectedDir\*.out" | ForEach-Object {
    $name = $_.Name
    $expected = $_.FullName
    $actual = Join-Path $outDir $name

    Write-Host "checking OUT $name"

    if (!(Test-Path $actual)) {
        Write-Host "FAIL: missing actual file $actual"
        $fail = $true
        return
    }

    $diff = Compare-Object (Get-Content $expected) (Get-Content $actual)
    if ($diff) {
        Write-Host "FAIL: OUT differs for $name"
        $fail = $true
    }
}

if ($fail) { exit 1 } else { Write-Host "OUT checks passed" }