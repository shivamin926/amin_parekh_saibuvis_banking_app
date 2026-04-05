#!/usr/bin/env bash
# =============================================================================
# weekly.sh  -  Phase 6 Weekly Banking Script
#
# Simulates seven consecutive days of banking system operation by calling
# daily.sh once for each day. The updated accounts file produced at the end
# of each day is passed as the starting accounts file for the next day.
#
# Usage:
#   bash weekly.sh
#
# Outputs (written to output/dayN/ for each day N = 1..7):
#   See daily.sh for per-day output details.
#   The final updated accounts are in output/day7/new_be_accounts.txt.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Starting accounts file (in the format read.py expects)
CURRENT_BE="$SCRIPT_DIR/accounts/initial_be_accounts.txt"

echo "======================================================================"
echo "  WEEKLY BANKING SIMULATION"
echo "======================================================================"

for DAY in 1 2 3 4 5 6 7; do
    OUTPUT_DIR="$SCRIPT_DIR/output/day$DAY"
    echo ""
    echo "----------------------------------------------------------------------"
    echo "  DAY $DAY"
    echo "----------------------------------------------------------------------"

    bash "$SCRIPT_DIR/daily.sh" "$CURRENT_BE" "$DAY" "$OUTPUT_DIR"

    # The normalized output becomes the next day's input
    CURRENT_BE="$OUTPUT_DIR/new_be_accounts.txt"
done

echo ""
echo "======================================================================"
echo "  WEEKLY SIMULATION COMPLETE"
echo "  Final accounts: $CURRENT_BE"
echo "======================================================================"
