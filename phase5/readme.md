
## Test Cases

### Create Command Test Cases

**Statement coverage for `apply_create()`:**

This set of test cases covers all main paths in `apply_create()`:
- Reject if not logged in or not admin
- Reject if arguments are missing
- Reject if balance is not numeric
- Reject if name is too long
- Reject if balance is out of range
- Reject if plan is invalid
- Reject if account already exists
- Otherwise, create the account successfully

| Test ID | Input / Situation                            | What path it covers        | Expected Result                                        |
| ------- | -------------------------------------------- | -------------------------- | ------------------------------------------------------ |
| SC1     | session is logged out                        | admin access check         | `ERROR: create: Admin access required.`                |
| SC2     | admin session, but missing args              | invalid argument path      | `ERROR: create: Invalid arguments.`                    |
| SC3     | admin session, balance = "abc"             | invalid balance conversion | `ERROR: create: Invalid balance.`                      |
| SC4     | admin session, name longer than 20 chars     | name length check          | `ERROR: create: Name exceeds 20 characters.`           |
| SC5     | admin session, balance = 0.00                | lower bound balance check  | `ERROR: create: Balance must be > 0 and <= $99999.99.` |
| SC6     | admin session, plan = XX                     | invalid plan check         | `ERROR: create: Invalid plan. Must be SP or NP.`       |
| SC7     | admin session, account number already exists | duplicate account check    | `ERROR: create: Account number already exists.`        |
| SC8     | admin session, valid inputs                  | success path               | account is appended to `accounts`                      |



### Account File Parsing Test Cases

**Decision and loop coverage for `read_old_bank_accounts()`:**

This set of test cases covers all main decisions and loop paths in `read_old_bank_accounts()`, including:
- Line length check
- Account number digits check
- Status check
- Negative balance string check
- Balance format check
- Transaction count check
- Plan type check
- Exception path
- Valid append path

| Test ID | File content / Situation    | What it covers                                | Expected Result                |
| ------- | --------------------------- | --------------------------------------------- | ------------------------------ |
| DL1     | empty file                  | loop with zero useful iterations              | returns `[]`                   |
| DL2     | one valid line              | valid path inside loop                        | one account added              |
| DL3     | short line                  | invalid length decision                       | prints invalid length error    |
| DL4     | non-digit account number    | invalid account-number decision               | prints account number error    |
| DL5     | invalid status              | invalid status decision                       | prints status error            |
| DL6     | negative balance string     | negative balance decision                     | prints negative balance error  |
| DL7     | malformed balance format    | invalid balance format decision               | prints balance format error    |
| DL8     | non-digit transaction count | invalid transaction count decision            | prints transaction count error |
| DL9     | invalid plan type           | invalid plan decision                         | prints invalid plan error      |
| DL10    | mixed valid + invalid lines | multiple loop iterations, true/false outcomes | only valid accounts returned   |

## How to Run the Unit Tests

To run all unit tests for Phase 5, use the following command in the phase5 directory:

```
python -m unittest test_phase5.py -v
```

## Test Failure Analysis

| Failure # | Test name                                 | What went wrong                                                              | Why it happened                                                                                                                                                           | How it was fixed                                                                                                                                      |
| --------- | ----------------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1         | `test_dl2_one_valid_line`                 | Expected 1 valid account, but got an error: `Invalid status '0'`             | The “valid” test line was not aligned to the fixed-width positions that `read_old_bank_accounts()` expects, so the parser read `0` as the status character instead of `A` | Added a helper method `make_account_line(...)` to build correctly aligned fixed-width test lines, then rebuilt the valid test input using that helper |
| 2         | `test_dl6_negative_balance_string`        | Expected `"Negative balance detected"` but got `Invalid status '-'`          | The negative balance line was shifted, so `-` landed in the status position                                                                                               | Rebuilt the test line with `make_account_line(...)` so `A` is in the status position and `-0100.00` is in the balance field                           |
| 3         | `test_dl7_invalid_balance_format`         | Expected `"Invalid balance format"` but got `Invalid status '0'`             | The malformed balance test line was misaligned, so the parser failed earlier at status validation                                                                         | Rebuilt the test line with `make_account_line(...)` so the invalid balance value is tested in the correct balance field                               |
| 4         | `test_dl8_invalid_transaction_count`      | Expected `"Transaction count must be 4 digits"` but got `Invalid status '0'` | The transaction-count test line was shifted, so the parser never reached the transaction-count validation                                                                 | Rebuilt the test line with `make_account_line(...)` so the invalid transaction count is placed in the correct `37:41` field                           |
| 5         | `test_dl9_invalid_plan_type`              | Expected `"Invalid plan type"` but got `Invalid status '0'`                  | The invalid plan test line was not formatted correctly, so the parser failed at the status check first                                                                    | Rebuilt the test line with `make_account_line(...)` so the plan appears in the correct final field                                                    |
| 6         | `test_dl10_mixed_valid_and_invalid_lines` | Expected 2 valid accounts, but got 0                                         | All the mixed test lines were hand-written with incorrect spacing, so even the “valid” rows were being parsed incorrectly                                                 | Rebuilt every line in the mixed test using `make_account_line(...)` so valid and invalid rows are both tested properly                                |
