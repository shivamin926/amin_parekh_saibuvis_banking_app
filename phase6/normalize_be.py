"""
normalize_be.py
Phase 6 - Back End output normalizer

The Python Back End's write.py produces a 40-character fixed-width format:
    {acct5} {name20} {status1} {balance8} {plan2}

The Back End's read.py expects a 44-character format:
    {acct5}{name19}  {status1} {balance8} {trans4} {plan2}

These differ in two ways:
  1. write.py adds a space between name and status (pushing status to pos 27),
     but read.py expects status at position 26.
  2. write.py omits the 4-digit transaction count field that read.py expects.

This script converts write.py output into the format read.py can parse, so the
Back End output from one day can be fed back as input on the next day.

Usage:
    python normalize_be.py <be_output_file>
    Output is written to stdout.
"""

import sys


def normalize(input_path):
    with open(input_path, "r") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if not line:
                continue

            # write.py format:
            #   positions 0-4:   account number (5 digits)
            #   position  5:     space
            #   positions 6-25:  name (20 chars, left-justified)
            #   position  26:    space (separator)
            #   position  27:    status (A or D)
            #   position  28:    space
            #   positions 29-36: balance (XXXXX.XX)
            #   position  37:    space
            #   positions 38-39: plan (SP or NP)
            if len(line) < 40:
                # Skip malformed or empty lines
                continue

            acc_num = line[0:5]
            name    = line[6:26].strip()
            status  = line[27]
            balance = line[29:37]
            plan    = line[38:40]

            # read.py format:
            #   positions 0-4:   account number (5 digits)
            #   positions 5-23:  name (19 chars, left-justified, no leading space)
            #   positions 24-25: "  " (two spaces)
            #   position  26:    status
            #   position  27:    space
            #   positions 28-35: balance (XXXXX.XX)
            #   position  36:    space
            #   positions 37-40: transaction count (always 0000 here)
            #   position  41:    space
            #   positions 42-43: plan
            name_field = name.ljust(19)[:19]
            out = f"{acc_num}{name_field}  {status} {balance} 0000 {plan}"
            print(out)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python normalize_be.py <be_output_file>", file=sys.stderr)
        sys.exit(1)
    normalize(sys.argv[1])
