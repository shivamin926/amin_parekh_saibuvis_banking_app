# run_all.ps1
# Runs all TC*.in tests, captures .out + .atf, diffs against expected,
# and prints a summary + writes test_results.csv and diff files.

$ErrorActionPreference = "Stop"

# ---- Paths (relative to this script) ----
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$testsDir  = Resolve-Path (Join-Path $scriptDir "..")
$inputsDir = Join-Path $testsDir "inputs"
$expectedDir = Join-Path $testsDir "outputs"          # expected TCxx.out + TCxx.atf live here in your setup
$actualDir = Join-Path $testsDir "actual_outputs"
$diffsDir  = Join-Path $testsDir "diffs"

# ---- Program details ----
$projectDir = Join-Path $testsDir ".."  # parent is project root
$javaFile = Join-Path $projectDir "BankingConsoleApp.java"  
$className = "BankingConsoleApp"
$dailyTxFile = "daily_transaction_file.txt"                 # file your app writes

# ---- Ensure folders exist ----
New-Item -ItemType Directory -Force -Path $actualDir | Out-Null
New-Item -ItemType Directory -Force -Path $diffsDir  | Out-Null

# ---- Compile (in project root, creates .class there) ----
Write-Host "Compiling $javaFile ..."
javac (Resolve-Path $javaFile)

# ---- Results table ----
$results = @()

# ---- Run all tests ----
$tests = Get-ChildItem -Path $inputsDir -Filter "TC*.in" | Sort-Object Name
if ($tests.Count -eq 0) {
    Write-Host "No TC*.in files found in $inputsDir"
    exit 1
}

foreach ($t in $tests) {
    $testName = [System.IO.Path]::GetFileNameWithoutExtension($t.Name)  # TC01
    Write-Host "Running $testName ..."

    # Clean any previous tx file so we don't accidentally copy old one
    if (Test-Path $dailyTxFile) { Remove-Item $dailyTxFile -Force }

    $actualOut = Join-Path $actualDir ($testName + ".out")
    $actualAtf = Join-Path $actualDir ($testName + ".atf")

    # Run program, capture stdout to .out
    # Use -cp to point to project root where .class file was created
    Get-Content $t.FullName | java -cp (Resolve-Path $projectDir) $className | Set-Content -Encoding utf8 $actualOut

    # Capture ATF (copy daily tx file if produced, else blank)
    if (Test-Path $dailyTxFile) {
        Copy-Item $dailyTxFile $actualAtf -Force
    } else {
        "" | Set-Content -Encoding utf8 $actualAtf
    }

    # Expected files
    $expectedOut = Join-Path $expectedDir ($testName + ".out")
    $expectedAtf = Join-Path $expectedDir ($testName + ".atf")

    $outStatus = "PASS"
    $atfStatus = "PASS"

    # ---- Diff OUT ----
    if (!(Test-Path $expectedOut)) {
        $outStatus = "MISSING_EXPECTED"
    } else {
        $outDiff = Compare-Object (Get-Content $expectedOut) (Get-Content $actualOut) -SyncWindow 0
        if ($outDiff) {
            $outStatus = "FAIL"
            $diffPath = Join-Path $diffsDir ($testName + ".out.diff.txt")
            $outDiff | Out-String | Set-Content -Encoding utf8 $diffPath
        }
    }

    # ---- Diff ATF ----
    if (!(Test-Path $expectedAtf)) {
        $atfStatus = "MISSING_EXPECTED"
    } else {
        $atfDiff = Compare-Object (Get-Content $expectedAtf) (Get-Content $actualAtf) -SyncWindow 0
        if ($atfDiff) {
            $atfStatus = "FAIL"
            $diffPath = Join-Path $diffsDir ($testName + ".atf.diff.txt")
            $atfDiff | Out-String | Set-Content -Encoding utf8 $diffPath
        }
    }

    # ---- Combined label for easier scanning ----
    $combined =
        if ($outStatus -eq "FAIL" -and $atfStatus -eq "FAIL") { "BOTH_FAIL" }
        elseif ($outStatus -eq "FAIL") { "OUT_FAIL" }
        elseif ($atfStatus -eq "FAIL") { "ATF_FAIL" }
        elseif ($outStatus -eq "MISSING_EXPECTED" -or $atfStatus -eq "MISSING_EXPECTED") { "MISSING_EXPECTED" }
        else { "PASS" }

    $results += [PSCustomObject]@{
        Test = $testName
        OUT  = $outStatus
        ATF  = $atfStatus
        Result = $combined
    }
}

# ---- Write CSV ----
$csvPath = Join-Path $testsDir "test_results.csv"
$results | Export-Csv -NoTypeInformation -Path $csvPath

# ---- Print summary ----
Write-Host ""
Write-Host "==== SUMMARY ===="
$results | Format-Table -AutoSize

$pass = ($results | Where-Object {$_.Result -eq "PASS"}).Count
$fail = ($results | Where-Object {$_.Result -ne "PASS"}).Count
Write-Host ""
Write-Host "PASS: $pass    NON-PASS: $fail"
Write-Host "CSV:  $csvPath"
Write-Host "Diffs: $diffsDir"