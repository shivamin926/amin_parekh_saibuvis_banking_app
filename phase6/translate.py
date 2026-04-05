"""
translate.py
Phase 6 - ATF to Back End format translator

The Java Front End outputs a Bank Account Transaction File (ATF) using its own
command names and argument order. The Python Back End expects a different format.
This script translates the merged ATF file into the format that the Back End reads.

ATF command formats (FE output):
  Standard session:
    WDR <acct> <amount>
    XFR <from_acct> <to_acct> <amount>
    DEP <acct> <amount>
    BILL <acct> <company> <amount>
  Admin session (name is always included by the FE):
    WDR <acct> <amount> <name>
    XFR <from_acct> <to_acct> <amount> <name>
    DEP <acct> <amount> <name>
    BILL <acct> <company> <amount>
    NEW <name> <acct> <balance>
    DEL <name> <acct>
    DIS <name> <acct>
    CPL <name> <acct> <new_plan>
  Both:
    LOGIN admin
    LOGIN standard <name>
    LOGOUT
    BEGIN_SESSION / END_SESSION  (stripped)

Back End command formats (BE input):
  Standard session (BE uses session.username for name):
    withdrawal <acct> <amount>
    transfer <from_acct> <to_acct> <amount>
    deposit <acct> <amount>
    paybill <acct> <company> <amount>
  Admin session:
    withdrawal <name> <acct> <amount>
    transfer <name> <from_acct> <to_acct> <amount>
    deposit <name> <acct> <amount>
    paybill <acct> <company> <amount>
    create <name> <acct> <balance> SP
    delete <name> <acct>
    disable <name> <acct>
    changeplan <name> <acct>
  Both:
    login admin
    login standard <name>
    logout

Usage:
    python translate.py <atf_file>
    Output is written to stdout.
"""

import sys


def translate(atf_path):
    mode = None      # "admin" or "standard"
    username = None  # set for standard sessions

    with open(atf_path, "r") as f:
        for raw in f:
            line = raw.strip()
            if not line:
                continue

            # Skip session delimiters
            if line in ("BEGIN_SESSION", "END_SESSION"):
                continue

            parts = line.split()
            cmd = parts[0]

            if cmd == "LOGIN":
                if len(parts) >= 2 and parts[1].lower() == "admin":
                    mode = "admin"
                    username = None
                    print("login admin")
                elif len(parts) >= 3 and parts[1].lower() == "standard":
                    mode = "standard"
                    username = " ".join(parts[2:])
                    print(f"login standard {username}")
                continue

            if cmd == "LOGOUT":
                mode = None
                username = None
                print("logout")
                continue

            # Transaction lines
            if mode == "standard":
                if cmd == "WDR" and len(parts) == 3:
                    # WDR <acct> <amount>
                    print(f"withdrawal {parts[1]} {parts[2]}")
                elif cmd == "XFR" and len(parts) == 4:
                    # XFR <from> <to> <amount>
                    print(f"transfer {parts[1]} {parts[2]} {parts[3]}")
                elif cmd == "DEP" and len(parts) == 3:
                    # DEP <acct> <amount>
                    print(f"deposit {parts[1]} {parts[2]}")
                elif cmd == "BILL" and len(parts) == 4:
                    # BILL <acct> <company> <amount>
                    print(f"paybill {parts[1]} {parts[2]} {parts[3]}")

            elif mode == "admin":
                if cmd == "WDR" and len(parts) == 4:
                    # WDR <acct> <amount> <name>
                    print(f"withdrawal {parts[3]} {parts[1]} {parts[2]}")
                elif cmd == "XFR" and len(parts) == 5:
                    # XFR <from> <to> <amount> <name>
                    print(f"transfer {parts[4]} {parts[1]} {parts[2]} {parts[3]}")
                elif cmd == "DEP" and len(parts) == 4:
                    # DEP <acct> <amount> <name>
                    print(f"deposit {parts[3]} {parts[1]} {parts[2]}")
                elif cmd == "BILL" and len(parts) == 4:
                    # BILL <acct> <company> <amount> (no name field)
                    print(f"paybill {parts[1]} {parts[2]} {parts[3]}")
                elif cmd == "NEW" and len(parts) == 4:
                    # NEW <name> <acct> <balance>  → default plan SP
                    print(f"create {parts[1]} {parts[2]} {parts[3]} SP")
                elif cmd == "DEL" and len(parts) == 3:
                    # DEL <name> <acct>
                    print(f"delete {parts[1]} {parts[2]}")
                elif cmd == "DIS" and len(parts) == 3:
                    # DIS <name> <acct>
                    print(f"disable {parts[1]} {parts[2]}")
                elif cmd == "CPL" and len(parts) == 4:
                    # CPL <name> <acct> <new_plan>  → changeplan just needs name+acct
                    print(f"changeplan {parts[1]} {parts[2]}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python translate.py <atf_file>", file=sys.stderr)
        sys.exit(1)
    translate(sys.argv[1])
