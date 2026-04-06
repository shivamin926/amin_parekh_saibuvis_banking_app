# Phase 6 – Integration and Delivery

Combines the Phase 3 Java Front End (`BankingConsoleApp.java`) with the Phase 5 Python Back End into a full day-to-day banking simulation.

---

## Prerequisites

| Tool | Required version | Check command |
|------|-----------------|---------------|
| Java (JDK) | 11 or later | `java -version` |
| Python | 3.6 or later | `python --version` |
| Git Bash | any | open **Git Bash** from Start menu |

> **Windows users:** all commands below must be run inside **Git Bash**, not PowerShell or CMD.  
> Git Bash is installed with Git for Windows and is available from the Start menu.

---

## Directory Structure

```
phase6/
├── daily.sh                     # Daily script (one simulated day)
├── weekly.sh                    # Weekly script (seven simulated days)
├── translate.py                 # ATF → Back End format translator
├── normalize_be.py              # Fixes Back End output for next-day input
├── accounts/
│   └── initial_be_accounts.txt  # Starting bank accounts (day 1 input)
└── sessions/
    ├── day1/  session1.in  session2.in  session3.in
    ├── day2/  session1.in  session2.in
    ├── day3/  session1.in  session2.in
    ├── day4/  session1.in  session2.in
    ├── day5/  session1.in  session2.in
    ├── day6/  session1.in  session2.in
    └── day7/  session1.in  session2.in  session3.in
```

---

## One-Time Setup: Compile the Front End

Before running any scripts, compile the Java Front End **once**:

```bash
cd ../phase3
javac BankingConsoleApp.java
cd ../phase6
```

You should see `BankingConsoleApp.class` appear in the `phase3/` folder. You only need to do this once.

---

## Running the Daily Script

The daily script simulates **one day** of banking:

1. Runs the Front End for each session file in `sessions/dayN/`
2. Merges all session ATF files into one
3. Translates the merged ATF to Back End format
4. Runs the Back End to produce updated accounts
5. Normalizes the output for the next day

**Usage:**
```bash
bash daily.sh <accounts_file> <day_number> <output_dir>
```

**Example – run day 1:**
```bash
bash daily.sh accounts/initial_be_accounts.txt 1 output/day1
```

**Example – run day 2 using day 1's output:**
```bash
bash daily.sh output/day1/new_be_accounts.txt 2 output/day2
```

**Output files written to `output/dayN/`:**

| File | Description |
|------|-------------|
| `session1.atf`, `session2.atf`, ... | Raw ATF output from each Front End session |
| `merged.atf` | All session ATF files concatenated |
| `merged_translated.txt` | Merged ATF converted to Back End command format |
| `new_be_accounts_raw.txt` | Raw Back End output (write.py format) |
| `new_be_accounts.txt` | Normalized accounts ready for the next day's input |

---

## Running the Weekly Script

The weekly script simulates **seven consecutive days** automatically. It starts from `accounts/initial_be_accounts.txt` and chains each day's output into the next.

```bash
bash weekly.sh
```

Output is written to:
```
output/
├── day1/
├── day2/
├── day3/
├── day4/
├── day5/
├── day6/
└── day7/
```

The final updated accounts file after all seven days is:
```
output/day7/new_be_accounts.txt
```

---

## 7-Day Simulation Overview

Starting accounts (hardcoded in the Java Front End and in `accounts/initial_be_accounts.txt`):

| Account | Name | Balance | Status | Plan |
|---------|------|---------|--------|------|
| 00001 | Candice | $1000.00 | Active | SP |
| 00002 | Jake | $500.00 | Active | NP |
| 00003 | Bob | $250.00 | Disabled | SP |

| Day | Sessions | Transactions |
|-----|----------|-------------|
| 1 | Candice, Jake, Admin | Candice withdraws $50; Jake deposits $100; Admin creates Diana ($200) |
| 2 | Candice, Jake | Candice transfers $100 → Jake; Jake pays bill EC $50 |
| 3 | Candice, Jake | Candice deposits $200; Jake withdraws $100 |
| 4 | Admin, Candice | Admin changes Candice's plan (SP↔NP); Candice withdraws $50 |
| 5 | Candice, Jake | Candice pays bill EC $100; Jake deposits $50 |
| 6 | Jake, Admin | Jake transfers $100 → Candice; Admin changes Jake's plan (NP↔SP) |
| 7 | Candice, Jake, Admin | Candice withdraws $100; Jake deposits $50; Admin deletes Bob |

---

## How It Works

### Front End (Java)
`BankingConsoleApp.java` in `phase3/` reads commands from **stdin** and writes a Bank Account Transaction File (ATF) to `daily_transaction_file.txt` in the `phase3/` directory. Each session `.in` file is piped into the Front End as its input.

### ATF Format (Front End output)
```
BEGIN_SESSION
LOGIN standard Candice
WDR 00001 50.00
LOGOUT
END_SESSION
```

### translate.py
Converts ATF commands to the format the Python Back End reads:
```
login standard Candice
withdrawal 00001 50.00
logout
```

### Back End (Python)
`phase5/main.py` reads the current accounts file and the translated transaction file, then writes an updated accounts file.

### normalize_be.py
The Back End's output format differs slightly from its input format. `normalize_be.py` fixes this so each day's output can be fed directly into the next day as input.

---

## Troubleshooting

**`bash: command not found`**  
Open **Git Bash** from the Start menu instead of using PowerShell or CMD.

**`Error: BankingConsoleApp.class not found` / `java: command not found`**  
Make sure Java (JDK) is installed and `java`/`javac` are on your PATH. Compile the Front End first (see One-Time Setup above).

**`python: command not found`**  
Make sure Python 3 is installed and on your PATH. On some systems you may need to use `python3` instead of `python` — if so, edit the `python` calls in `daily.sh` to `python3`.

**`ERROR: accounts file not found`**  
Run the scripts from inside the `phase6/` directory, or use the full path to the accounts file.

**Front End produces no transactions (empty ATF)**  
The Java Front End has hardcoded accounts (Candice, Jake, Bob). Session files must use those exact names and account numbers. Check that your session `.in` file uses `Candice`/`00001` or `Jake`/`00002`.
