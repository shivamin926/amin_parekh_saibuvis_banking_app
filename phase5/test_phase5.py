import unittest
import tempfile
import os
import io
from contextlib import redirect_stdout

from transactions import apply_create, Session
from read import read_old_bank_accounts


class TestApplyCreateStatementCoverage(unittest.TestCase):
    """
    Statement coverage tests for apply_create().
    """

    def make_admin_session(self):
        session = Session()
        session.start("admin")
        return session

    def test_sc1_admin_access_required_when_logged_out(self):
        session = Session()   # logged out
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345", "100.00", "SP"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Admin access required."])
        self.assertEqual(accounts, [])

    def test_sc2_invalid_arguments_when_missing_tokens(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Invalid arguments."])
        self.assertEqual(accounts, [])

    def test_sc3_invalid_balance_text(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345", "abc", "SP"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Invalid balance."])
        self.assertEqual(accounts, [])

    def test_sc4_name_too_long(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        long_name = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        apply_create(["create", long_name, "12345", "100.00", "SP"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Name exceeds 20 characters."])
        self.assertEqual(accounts, [])

    def test_sc5_balance_out_of_range_zero(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345", "0.00", "SP"], session, accounts, error_log)

        self.assertEqual(
            error_log,
            ["ERROR: create: Balance must be > 0 and <= $99999.99."]
        )
        self.assertEqual(accounts, [])

    def test_sc6_invalid_plan(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345", "100.00", "XX"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Invalid plan. Must be SP or NP."])
        self.assertEqual(accounts, [])

    def test_sc7_duplicate_account_number(self):
        session = self.make_admin_session()
        accounts = [
            {
                "account_number": "12345",
                "name": "Existing User",
                "status": "A",
                "balance": 500.00,
                "total_transactions": 0,
                "plan": "SP",
            }
        ]
        error_log = []

        apply_create(["create", "Alice", "12345", "100.00", "SP"], session, accounts, error_log)

        self.assertEqual(error_log, ["ERROR: create: Account number already exists."])
        self.assertEqual(len(accounts), 1)

    def test_sc8_valid_create_success(self):
        session = self.make_admin_session()
        accounts = []
        error_log = []

        apply_create(["create", "Alice", "12345", "100.00", "SP"], session, accounts, error_log)

        self.assertEqual(error_log, [])
        self.assertEqual(len(accounts), 1)
        self.assertEqual(
            accounts[0],
            {
                "account_number": "12345",
                "name": "Alice",
                "status": "A",
                "balance": 100.00,
                "total_transactions": 0,
                "plan": "SP",
            }
        )


class TestReadOldBankAccountsDecisionLoopCoverage(unittest.TestCase):
    """
    Decision + loop coverage tests for read_old_bank_accounts().
    """

    def run_reader(self, file_contents):
        """
        Create a temporary file, run read_old_bank_accounts(),
        capture printed output, and return (accounts, output).
        """
        with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".txt") as temp_file:
            temp_file.write(file_contents)
            temp_path = temp_file.name

        try:
            captured = io.StringIO()
            with redirect_stdout(captured):
                accounts = read_old_bank_accounts(temp_path)
            return accounts, captured.getvalue()
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    def make_account_line(self, acct, name, status, balance, transactions, plan):
        """
        Build a line that exactly matches read_old_bank_accounts() format.
        """
        acct = str(acct).zfill(5)
        name_field = str(name).ljust(19)[:19]
        return f"{acct}{name_field}  {status} {balance} {transactions} {plan}\n"

    def test_dl1_empty_file_returns_empty_list(self):
        accounts, output = self.run_reader("")

        self.assertEqual(accounts, [])
        self.assertEqual(output, "")

    def test_dl2_one_valid_line(self):
        valid_line = self.make_account_line("12345", "Alice", "A", "00100.00", "0000", "SP")

        accounts, output = self.run_reader(valid_line)

        self.assertEqual(output, "")
        self.assertEqual(len(accounts), 1)
        self.assertEqual(
            accounts[0],
            {
                "account_number": "12345",
                "name": "Alice",
                "status": "A",
                "balance": 100.00,
                "total_transactions": 0,
                "plan": "SP",
            }
        )

    def test_dl3_invalid_length(self):
        short_line = "123\n"

        accounts, output = self.run_reader(short_line)

        self.assertEqual(accounts, [])
        self.assertIn("Invalid length", output)

    def test_dl4_invalid_account_number(self):
        bad_account_line = self.make_account_line("12A45", "Alice", "A", "00100.00", "0000", "SP")

        accounts, output = self.run_reader(bad_account_line)

        self.assertEqual(accounts, [])
        self.assertIn("Account number must be 5 digits", output)

    def test_dl5_invalid_status(self):
        bad_status_line = self.make_account_line("12345", "Alice", "X", "00100.00", "0000", "SP")

        accounts, output = self.run_reader(bad_status_line)

        self.assertEqual(accounts, [])
        self.assertIn("Invalid status", output)

    def test_dl6_negative_balance_string(self):
        negative_balance_line = self.make_account_line("12345", "Alice", "A", "-0100.00", "0000", "SP")

        accounts, output = self.run_reader(negative_balance_line)

        self.assertEqual(accounts, [])
        self.assertIn("Negative balance detected", output)

    def test_dl7_invalid_balance_format(self):
        bad_balance_format_line = self.make_account_line("12345", "Alice", "A", "00100000", "0000", "SP")

        accounts, output = self.run_reader(bad_balance_format_line)

        self.assertEqual(accounts, [])
        self.assertIn("Invalid balance format", output)

    def test_dl8_invalid_transaction_count(self):
        bad_transaction_line = self.make_account_line("12345", "Alice", "A", "00100.00", "00A0", "SP")

        accounts, output = self.run_reader(bad_transaction_line)

        self.assertEqual(accounts, [])
        self.assertIn("Transaction count must be 4 digits", output)

    def test_dl9_invalid_plan_type(self):
        bad_plan_line = self.make_account_line("12345", "Alice", "A", "00100.00", "0000", "XP")

        accounts, output = self.run_reader(bad_plan_line)

        self.assertEqual(accounts, [])
        self.assertIn("Invalid plan type", output)

    def test_dl10_mixed_valid_and_invalid_lines(self):
        mixed_lines = (
            self.make_account_line("12345", "Alice", "A", "00100.00", "0000", "SP") +
            self.make_account_line("12A45", "Bob", "A", "00200.00", "0000", "SP") +
            self.make_account_line("54321", "Charlie", "D", "00300.00", "0005", "NP") +
            self.make_account_line("99999", "Diana", "X", "00400.00", "0001", "SP")
        )

        accounts, output = self.run_reader(mixed_lines)

        self.assertEqual(len(accounts), 2)
        self.assertEqual(accounts[0]["account_number"], "12345")
        self.assertEqual(accounts[1]["account_number"], "54321")
        self.assertIn("Account number must be 5 digits", output)
        self.assertIn("Invalid status", output)


if __name__ == "__main__":
    unittest.main()