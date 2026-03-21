"""
main.py
Banking System Back End – Entry Point
Reads the old bank accounts file and merged daily transaction file,
processes all transactions, and writes the updated accounts to a new file.

Input files:
    argv[1] - old bank accounts file  (e.g. old_bank_accounts.txt)
    argv[2] - merged transaction file (e.g. daily_transaction_file.txt)
Output file:
    argv[3] - new bank accounts file  (e.g. new_bank_accounts.txt)

Usage:
    python main.py old_bank_accounts.txt daily_transaction_file.txt new_bank_accounts.txt
"""

import sys
from read         import read_old_bank_accounts
from write        import write_new_current_accounts
from transactions import process_transactions
from print_error  import log_constraint_error


def main():
    """
    Orchestrate the back end: read inputs, process transactions, write output.
    Exits with an error message if required arguments are missing.
    """
    if len(sys.argv) != 4:
        log_constraint_error(
            "Usage: python main.py <old_accounts> <transactions> <new_accounts>",
            "main.py",
            fatal=True
        )
        sys.exit(1)

    old_accounts_path = sys.argv[1]
    transactions_path = sys.argv[2]
    new_accounts_path = sys.argv[3]

    # Read and validate the current bank accounts file
    accounts = read_old_bank_accounts(old_accounts_path)

    # Read the merged daily transaction file
    try:
        with open(transactions_path, "r") as f:
            transaction_lines = f.readlines()
    except FileNotFoundError:
        log_constraint_error(
            f"Transaction file not found: {transactions_path}",
            "main.py",
            fatal=True
        )
        sys.exit(1)

    # Process all transactions and collect constraint errors
    errors = process_transactions(accounts, transaction_lines)

    # Print any constraint errors
    for err in errors:
        print(err)

    # Write the updated accounts to the new bank accounts file
    write_new_current_accounts(accounts, new_accounts_path)
    print(f"New accounts file written to: {new_accounts_path}")


if __name__ == "__main__":
    main()
