#!/usr/bin/env bash
# =============================================================================
# daily.sh  -  Phase 6 Daily Banking Script
#
# Simulates one day of banking system operation:
#   1. Runs the Java Front End for each session file in sessions/day<N>/,
#      saving each session's transaction output as a separate ATF file.
#   2. Concatenates all ATF files into a single merged daily transaction file.
#   3. Translates the merged ATF into the Python Back End's command format.
#   4. Runs the Python Back End to produce an updated accounts file.
#   5. Normalizes the Back End output so it can be read back on the next day.
#
# Usage:
#   bash daily.sh <current_be_accounts> <day_number> <output_dir>
#
# Arguments:
#   current_be_accounts  Path to today's bank accounts file (BE format)
#   day_number           Integer 1-7 identifying which day's sessions to use
#   output_dir           Directory where today's output files will be written
#
# Outputs (written to output_dir):
#   session1.atf ... sessionN.atf   Individual ATF files from each FE session
#   merged.atf                      All ATF files concatenated
#   merged_translated.txt           merged.atf converted to BE command format
#   new_be_accounts_raw.txt         Raw BE output (write.py format)
#   new_be_accounts.txt             Normalized BE output (read.py format, for next day)
# =============================================================================

set -euo pipefail

# --- Arguments ---------------------------------------------------------------
if [ "$#" -ne 3 ]; then
    echo "Usage: bash daily.sh <current_be_accounts> <day_number> <output_dir>" >&2
    exit 1
fi

CURRENT_BE_ACCOUNTS="$1"
DAY="$2"
OUTPUT_DIR="$3"

# --- Paths -------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PHASE3_DIR="$(cd "$SCRIPT_DIR/../phase3" && pwd)"
PHASE5_DIR="$(cd "$SCRIPT_DIR/../phase5" && pwd)"
SESSION_DIR="$SCRIPT_DIR/sessions/day$DAY"

# Resolve accounts file to absolute path
CURRENT_BE_ACCOUNTS="$(cd "$(dirname "$CURRENT_BE_ACCOUNTS")" && pwd)/$(basename "$CURRENT_BE_ACCOUNTS")"

# --- Validate inputs ---------------------------------------------------------
if [ ! -f "$CURRENT_BE_ACCOUNTS" ]; then
    echo "ERROR: accounts file not found: $CURRENT_BE_ACCOUNTS" >&2
    exit 1
fi

if [ ! -d "$SESSION_DIR" ]; then
    echo "ERROR: session directory not found: $SESSION_DIR" >&2
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

# --- Compile the Front End if needed -----------------------------------------
if [ ! -f "$PHASE3_DIR/BankingConsoleApp.class" ]; then
    echo "Compiling BankingConsoleApp.java..."
    (cd "$PHASE3_DIR" && javac BankingConsoleApp.java)
fi

# --- Step 1: Run the Front End for each session file -------------------------
echo "=== Day $DAY: Running Front End sessions ==="
SESSION_NUM=0
for SESSION_FILE in "$SESSION_DIR"/*.in; do
    SESSION_NUM=$((SESSION_NUM + 1))
    SESSION_NAME="$(basename "$SESSION_FILE" .in)"
    echo "  Running session $SESSION_NUM ($SESSION_NAME)..."

    # The FE reads from stdin and writes daily_transaction_file.txt in its cwd
    (cd "$PHASE3_DIR" && java BankingConsoleApp < "$SESSION_FILE" > /dev/null 2>&1)

    # Move the generated ATF to the output directory
    mv "$PHASE3_DIR/daily_transaction_file.txt" "$OUTPUT_DIR/session${SESSION_NUM}.atf"
done

if [ "$SESSION_NUM" -eq 0 ]; then
    echo "ERROR: No session files found in $SESSION_DIR" >&2
    exit 1
fi
echo "  $SESSION_NUM session(s) completed."

# --- Step 2: Concatenate ATF files -------------------------------------------
echo "=== Day $DAY: Merging $SESSION_NUM ATF file(s) ==="
cat "$OUTPUT_DIR"/session*.atf > "$OUTPUT_DIR/merged.atf"

# --- Step 3: Translate ATF to Back End format --------------------------------
echo "=== Day $DAY: Translating to Back End format ==="
python "$SCRIPT_DIR/translate.py" "$OUTPUT_DIR/merged.atf" > "$OUTPUT_DIR/merged_translated.txt"

# --- Step 4: Run the Python Back End -----------------------------------------
echo "=== Day $DAY: Running Back End ==="
python "$PHASE5_DIR/main.py" \
    "$CURRENT_BE_ACCOUNTS" \
    "$OUTPUT_DIR/merged_translated.txt" \
    "$OUTPUT_DIR/new_be_accounts_raw.txt"

# --- Step 5: Normalize Back End output for next day -------------------------
echo "=== Day $DAY: Normalizing accounts for next day ==="
python "$SCRIPT_DIR/normalize_be.py" "$OUTPUT_DIR/new_be_accounts_raw.txt" \
    > "$OUTPUT_DIR/new_be_accounts.txt"

echo "=== Day $DAY complete. Updated accounts: $OUTPUT_DIR/new_be_accounts.txt ==="
