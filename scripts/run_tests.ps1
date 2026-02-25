# scripts/run_tests.ps1
# Runs all TC*.in in tests\inputs, compares to tests\outputs\TC*.out,
# writes actual outputs to actual_outputs\, and creates a CSV summary.

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$inDir  = Join-Path $root "tests\inputs"
$outDir = Join-Path $root "tests\outputs"
$actDir = Join-Path $root "actual_outputs"
$diffDir = Join-Path $root "diffs"

New-Item -ItemType Directory -Force -Path $actDir  | Out-Null
New-Item -ItemType Directory -Force -Path $diffDir | Out-Null

# Compile (optional: comment out if you compile manually)
Write-Host "Compiling..."
javac .\BankingConsoleApp.java

$results = @()

$inputs = Get-ChildItem $inDir -Filter "TC*.in" | Sort-Object Name
if ($inputs.Count -eq 0) {
  Write-Host "No inputs found in $inDir (expected TC*.in)."
  exit 1
}

foreach ($f in $inputs) {
  $tc = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)   # e.g., TC01
  $expected = Join-Path $outDir  "$tc.out"
  $actual   = Join-Path $actDir  "$tc.out"
  $diffFile = Join-Path $diffDir "$tc.diff.txt"

  if (!(Test-Path $expected)) {
    $results += [pscustomobject]@{
      TestCase = $tc
      Status   = "MISSING_EXPECTED"
      Expected = $expected
      Actual   = $actual
      Note     = "No expected .out file found"
    }
    continue
  }

  # Run program: pipe .in into java, redirect stdout to actual file
  Get-Content $f.FullName | java BankingConsoleApp > $actual

  # Compare exact text
  $expText = Get-Content $expected -Raw
  $actText = Get-Content $actual   -Raw

  if ($expText -eq $actText) {
    $results += [pscustomobject]@{
      TestCase = $tc
      Status   = "PASS"
      Expected = $expected
      Actual   = $actual
      Note     = ""
    }
  } else {
    # Create a simple diff (first ~40 differing lines)
    $expLines = Get-Content $expected
    $actLines = Get-Content $actual

    $max = [Math]::Max($expLines.Count, $actLines.Count)
    $diffLines = New-Object System.Collections.Generic.List[string]

    for ($i=0; $i -lt $max; $i++) {
      $e = if ($i -lt $expLines.Count) { $expLines[$i] } else { "<no line>" }
      $a = if ($i -lt $actLines.Count) { $actLines[$i] } else { "<no line>" }

      if ($e -ne $a) {
        $diffLines.Add(("Line {0}:" -f ($i+1)))
        $diffLines.Add(("  EXPECTED: {0}" -f $e))
        $diffLines.Add(("  ACTUAL:   {0}" -f $a))
        $diffLines.Add("")
        if ($diffLines.Count -ge 40) { break }
      }
    }

    $diffLines | Set-Content $diffFile

    $results += [pscustomobject]@{
      TestCase = $tc
      Status   = "FAIL"
      Expected = $expected
      Actual   = $actual
      Note     = "See $diffFile"
    }
  }
}

# Write summary CSV
$csvPath = Join-Path $root "test_results.csv"
$results | Export-Csv -NoTypeInformation -Path $csvPath

# Print a nice table in the console
$results | Format-Table -AutoSize

Write-Host ""
Write-Host "Saved:"
Write-Host " - $csvPath"
Write-Host " - Actual outputs in: $actDir"
Write-Host " - Diffs in: $diffDir"