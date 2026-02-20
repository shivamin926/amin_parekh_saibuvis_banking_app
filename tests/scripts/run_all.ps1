$ErrorActionPreference = "Stop"

$accounts = "tests/currentaccounts.txt"
$inputsDir = "tests/inputs"
$outDir = "tests/outputs"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Get-ChildItem "$inputsDir\*.in" | ForEach-Object {
    $testName = $_.BaseName
    Write-Host "running test $testName"

    Get-Content $_.FullName |
        python bank_atm.py $accounts "$outDir\$testName.atf" |
        Set-Content "$outDir\$testName.out"
}

Write-Host "DONE. Outputs are in $outDir"