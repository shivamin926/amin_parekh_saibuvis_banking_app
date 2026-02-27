# SQA Project Phase 3 

**Course:** Software Quality Assurance (SQA)

**Professor:** Dr. Cristiano Politowski

### Group Members
- Aakanksha Parekh, 100871641
- Vibhavan Saibuvis, 100872481
- Shiv Amin, 100867326

## Introduction / Overview
This project is a small banking application with two parts:
- a console program (`BankingConsoleApp.java`) and
- a simple web UI in the `source_code/` folder.

The `tests/` folder contains test inputs, expected outputs, actual outputs and diffs used to check the program.

## Project structure (In Summary)
- `BankingConsoleApp.java` - console application entry point.
- `source_code/` - HTML, CSS and JS for the web interface.
- `tests/` - test files and folders (see below).

## Tests (Overview)
- `tests/expected/` - expected output files (`TC01.atf`, `TC02.atf`, ...).
- `tests/actual_outputs/` - the outputs produced when you run the app for each test.
- `tests/diffs/` - diffs showing where expected and actual differ.
- `tests/currentaccounts.txt` and `tests/daily_transaction_file.txt` - example inputs used by the console app.

## How to run 

1) Run the console app (Windows/ cross-platform):

```bash
javac BankingConsoleApp.java
java BankingConsoleApp
```

Use the Test Cases from the Test Folder to put in the Inputs.

2) Run the web UI:
- Open `source_code/login.html` in your browser, or serve the folder:

```bash
npx http-server source_code
```

3) Compare outputs (Windows example):

```powershell
# compare expected vs actual for TC01
fc tests\expected\TC01.atf tests\actual_outputs\TC01.atf
```

Or use `diff` on Unix-like systems.

4) To run all the Test Cases, use the following command:
```bash
powershell -ExecutionPolicy Bypass -File tests/scripts/run_all.ps1
```

## Quick tips
- If a test fails, open the matching file in `tests/diffs/` to see differences.
- To update expected outputs, verify the new output is correct and replace the file in `tests/expected/`.


