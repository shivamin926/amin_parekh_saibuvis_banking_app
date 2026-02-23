# tests/scripts/_make_failure_table.ps1
# Generates a Phase 3 failure table by diffing tests/expected vs tests/outputs

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$expectedDir = Join-Path $root "expected"
$outputDir   = Join-Path $root "outputs"

if (!(Test-Path $expectedDir)) { throw "Missing folder: $expectedDir" }
if (!(Test-Path $outputDir))   { throw "Missing folder: $outputDir" }

function Get-FirstDiffLine {
    param(
        [string]$expectedPath,
        [string]$actualPath
    )

    $expLines = Get-Content -LiteralPath $expectedPath
    $actLines = Get-Content -LiteralPath $actualPath

    $max = [Math]::Max($expLines.Count, $actLines.Count)
    for ($i = 0; $i -lt $max; $i++) {
        $e = if ($i -lt $expLines.Count) { $expLines[$i] } else { "<EOF>" }
        $a = if ($i -lt $actLines.Count) { $actLines[$i] } else { "<EOF>" }
        if ($e -ne $a) {
            # line numbers are 1-based for humans
            return @{
                LineNum = $i + 1
                Expected = $e
                Actual = $a
            }
        }
    }
    return $null
}

$rows = @()

# We treat each expected file as a "test artifact" and match it in outputs
$expectedFiles = Get-ChildItem -LiteralPath $expectedDir -File

foreach ($ef in $expectedFiles) {
    $name = $ef.Name
    $actualPath = Join-Path $outputDir $name

    $artifactType =
        if ($ef.Extension -eq ".out") { "terminal (.out)" }
        elseif ($ef.Extension -eq ".atf") { "transaction file (.atf)" }
        else { "other" }

    $status = "PASS"
    $diffSummary = ""
    $expectedBrief = ""
    $actualBrief = ""

    if (!(Test-Path $actualPath)) {
        $status = "FAIL"
        $diffSummary = "Missing actual output file"
        $expectedBrief = "(file exists)"
        $actualBrief = "(missing)"
    }
    else {
        # Quick compare (raw). If different, find first differing line.
        $expRaw = Get-Content -LiteralPath $ef.FullName -Raw
        $actRaw = Get-Content -LiteralPath $actualPath -Raw

        if ($expRaw -ne $actRaw) {
            $status = "FAIL"
            $first = Get-FirstDiffLine -expectedPath $ef.FullName -actualPath $actualPath
            if ($first -ne $null) {
                $diffSummary = "First diff at line $($first.LineNum)"
                $expectedBrief = $first.Expected
                $actualBrief = $first.Actual
            } else {
                $diffSummary = "Files differ (unknown diff position)"
            }
        }
    }

    # Phase 3-friendly row fields.
    # You will fill these in after you fix things:
    $rows += [PSCustomObject]@{
        Artifact = $name
        Type = $artifactType
        Result = $status
        "Diff summary" = $diffSummary
        "Expected (first mismatch line)" = $expectedBrief
        "Actual (first mismatch line)" = $actualBrief
        "Likely requirement/feature" = ""   # fill manually (e.g., withdrawal limit, login, etc.)
        "Root cause" = ""                  # fill manually
        "Fix made" = ""                    # fill manually
        "Re-test result" = ""              # fill manually (PASS after fix)
    }
}

# Save CSV
$csvPath = Join-Path $root "failure_table.csv"
$rows | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding UTF8

# Save Markdown
$mdPath = Join-Path $root "failure_table.md"

$md = @()
$md += "# Phase 3 Failure Table"
$md += ""
$md += "Generated from diffs between tests/expected and tests/outputs."
$md += ""
$md += "| # | Artifact | Type | Result | Diff summary | Expected (first mismatch line) | Actual (first mismatch line) | Requirement/Feature | Root cause | Fix made | Re-test |"
$md += "|---:|---|---|---|---|---|---|---|---|---|---|"

$i = 1
foreach ($r in $rows) {
    # Escape pipes for markdown table
    $e = ($r."Expected (first mismatch line)" -replace "\|", "\|")
    $a = ($r."Actual (first mismatch line)" -replace "\|", "\|")
    $md += ("| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} |" -f `
        $i, $r.Artifact, $r.Type, $r.Result, $r."Diff summary", $e, $a, `
        $r."Likely requirement/feature", $r."Root cause", $r."Fix made", $r."Re-test result")
    $i++
}

Set-Content -LiteralPath $mdPath -Value ($md -join "`n") -Encoding UTF8

# Summary to console
$failCount = ($rows | Where-Object { $_.Result -eq "FAIL" }).Count
$passCount = ($rows | Where-Object { $_.Result -eq "PASS" }).Count

Write-Host "Created:"
Write-Host "  $csvPath"
Write-Host "  $mdPath"
Write-Host ""
Write-Host "Summary: PASS=$passCount  FAIL=$failCount"
if ($failCount -gt 0) {
    Write-Host "Open failure_table.md and fill in Root cause/Fix/Re-test for FAIL rows."
}