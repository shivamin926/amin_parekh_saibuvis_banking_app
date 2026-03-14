# Phase 4 Banking App

## Prerequisites

Ensure you have Python installed on your system.

## Setup Instructions

1. Navigate to the **phase 4** folder:
   ```
   cd phase4
   ```

2. Verify the following files are present in the phase 4 directory:
   - `main.py`
   - `old_bank_accounts.txt`
   - `daily_transaction_file.txt`

## Running the Program

**You must be in the phase 4 folder to run the program.**

Once you are in the phase 4 directory, execute the following command:

```
python main.py old_bank_accounts.txt daily_transaction_file.txt new_bank_accounts.txt
```

### Command Breakdown:
- `python main.py` - Runs the main banking application script
- `old_bank_accounts.txt` - Input file containing existing bank accounts
- `daily_transaction_file.txt` - Input file containing daily transactions
- `new_bank_accounts.txt` - Output file where updated accounts will be written

## Output

After running the command, the program will generate:
- `new_bank_accounts.txt` - Updated bank account information after processing all transactions
