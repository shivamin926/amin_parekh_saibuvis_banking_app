# make_failure_table.ps1
# Builds a Phase-3 style failure table (CSV) from test_results.csv + diff files.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$testsDir  = Resolve-Path (Join-Path $scriptDir "..")

$resultsCsv = Join-Path $testsDir "test_results.csv"
$diffsDir   = Join-Path $testsDir "diffs"
$outPath    = Join-Path $testsDir "failure_table.csv"

if (!(Test-Path $resultsCsv)) {
    Write-Host "ERROR: Cannot find $resultsCsv"
    Write-Host "Run run_all.ps1 first to generate test_results.csv."
    exit 1
}

$rows = Import-Csv $resultsCsv

# Helper: return first N non-empty lines of a diff file (for quick context)
function Get-DiffPreview($path, $maxLines = 12) {
    if (!(Test-Path $path)) { return "" }
    $lines = Get-Content $path | Where-Object { $_ -ne "" }
    if ($lines.Count -le $maxLines) { return ($lines -join " | ") }
    return (($lines[0..($maxLines-1)]) -join " | ") + " | ..."
}

$failureTable = @()

foreach ($r in $rows) {
    if ($r.Result -eq "PASS") { continue }

    $testName = $r.Test
    $outDiffPath = Join-Path $diffsDir ($testName + ".out.diff.txt")
    $atfDiffPath = Join-Path $diffsDir ($testName + ".atf.diff.txt")

    $outFailed = Test-Path $outDiffPath
    $atfFailed = Test-Path $atfDiffPath

    # Phase 3 columns
    $whatTesting =
        if ($testName -match '^TC(\d+)$') {
            "Acceptance test $testName (see inputs/$testName.in)"
        } else {
            "Acceptance test (see inputs/$testName.in)"
        }

    $natureOfFailure =
        if ($outFailed -and $atfFailed) { "Terminal output (.out) mismatch AND transaction file (.atf) mismatch" }
        elseif ($outFailed)              { "Terminal output (.out) mismatch" }
        elseif ($atfFailed)              { "Transaction file (.atf) mismatch" }
        else                             { "Missing expected file(s) or mismatch not captured" }

    $outPreview = Get-DiffPreview $outDiffPath 10
    $atfPreview = Get-DiffPreview $atfDiffPath 10

    $failureTable += [PSCustomObject]@{
        TestName        = $testName
        WhatItTests     = $whatTesting
        WrongOutput     = $natureOfFailure
        OutDiffPreview  = $outPreview
        AtfDiffPreview  = $atfPreview
        CodeError       = ""   # you fill in after debugging
        FixApplied      = ""   # you fill in after debugging
        StatusAfterFix  = ""   # optional: PASS/FAIL after rerun
        Notes           = ""   # optional
    }
}

$failureTable | Export-Csv -NoTypeInformation -Path $outPath

Write-Host "Wrote failure table to: $outPath"
Write-Host "Rows: $($failureTable.Count)"