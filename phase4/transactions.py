"""
transactions.py
Banking System Back End – Transaction Processing
Reads a merged daily transaction file and applies each transaction to the
current accounts loaded from the old bank accounts file.
Input:  old_bank_accounts.txt, daily_transaction_file.txt
Output: new_bank_accounts.txt
Run:    python main.py <old_accounts_file> <transaction_file> <new_accounts_file>
"""

# Transaction code constants
CODE_LOGIN      = "login"
CODE_LOGOUT     = "logout"
CODE_WITHDRAWAL = "withdrawal"
CODE_TRANSFER   = "transfer"
CODE_PAYBILL    = "paybill"
CODE_DEPOSIT    = "deposit"
CODE_CREATE     = "create"
CODE_DELETE     = "delete"
CODE_DISABLE    = "disable"
CODE_CHANGEPLAN = "changeplan"

# Session limits for standard users
CAP_WITHDRAWAL = 500.00
CAP_TRANSFER   = 1000.00
CAP_PAYBILL    = 2000.00

# Valid paybill companies
VALID_COMPANIES = {"EC", "CQ", "FI"}


class Session:
    """Holds the current login session state including role and spending caps."""

    def __init__(self):
        """Initialise a logged-out session with zeroed caps."""
        self.logged_in      = False
        self.role           = None   # "admin" or "standard"
        self.username       = None
        self.cap_withdrawal = 0.0
        self.cap_transfer   = 0.0
        self.cap_paybill    = 0.0

    def start(self, role, username=None):
        """Begin a session with the given role and optional username."""
        self.logged_in      = True
        self.role           = role
        self.username       = username
        self.cap_withdrawal = 0.0
        self.cap_transfer   = 0.0
        self.cap_paybill    = 0.0

    def end(self):
        """Reset session to logged-out state."""
        self.__init__()

    def is_admin(self):
        """Return True if current session is admin mode."""
        return self.role == "admin"


class PendingDeposit:
    """Represents a deposit queued during a session, applied on logout."""

    def __init__(self, acct_num, amount):
        """Store account number and amount for deferred application."""
        self.acct_num = acct_num
        self.amount   = amount


def find_account(accounts, acct_num):
    """Return the account dict matching acct_num, or None if not found."""
    for acc in accounts:
        if acc["account_number"] == acct_num.lstrip("0") or acc["account_number"] == acct_num:
            return acc
    return None


def validate_account_active(acc, cmd, error_log):
    """
    Check that account exists and is not disabled.
    Appends an error to error_log and returns False if invalid.
    """
    if acc is None:
        error_log.append(f"ERROR: {cmd}: Invalid account number.")
        return False
    if acc["status"] == "D":
        error_log.append(f"ERROR: {cmd}: Account is disabled.")
        return False
    return True


def apply_login(tokens, session, accounts, error_log):
    """
    Start a new session in standard or admin mode.
    Validates that no session is already active and that the username
    exists for standard login.
    """
    if session.logged_in:
        error_log.append("ERROR: login: Already logged in.")
        return
    if len(tokens) < 2:
        error_log.append("ERROR: login: Missing mode.")
        return
    mode = tokens[1].lower()
    if mode == "admin":
        session.start("admin")
    elif mode == "standard":
        if len(tokens) < 3:
            error_log.append("ERROR: login: Missing username for standard login.")
            return
        name = " ".join(tokens[2:]).strip()
        exists = any(a["name"] == name for a in accounts)
        if not exists:
            error_log.append(f"ERROR: login: Username '{name}' does not exist.")
            return
        session.start("standard", name)
    else:
        error_log.append(f"ERROR: login: Unknown mode '{mode}'.")


def apply_logout(session, pending_deposits, accounts, error_log):
    """
    End the session and apply all pending deposits to account balances.
    """
    if not session.logged_in:
        error_log.append("ERROR: logout: No active session.")
        return
    for pd in pending_deposits:
        acc = find_account(accounts, pd.acct_num)
        if acc is not None:
            acc["balance"] = round(acc["balance"] + pd.amount, 2)
            acc["total_transactions"] += 1
    pending_deposits.clear()
    session.end()


def apply_withdrawal(tokens, session, accounts, error_log):
    """
    Debit an account by the given amount.
    Enforces standard session cap ($500), sufficient funds, and account validity.
    """
    if not session.logged_in:
        error_log.append("ERROR: withdrawal: Must be logged in.")
        return
    try:
        if session.is_admin():
            name, acct_num, amt_str = tokens[1], tokens[2], tokens[3]
        else:
            name, acct_num, amt_str = session.username, tokens[1], tokens[2]
        amt = float(amt_str)
    except (IndexError, ValueError):
        error_log.append("ERROR: withdrawal: Invalid arguments.")
        return
    if amt <= 0:
        error_log.append("ERROR: withdrawal: Amount must be > 0.")
        return
    acc = find_account(accounts, acct_num)
    if not validate_account_active(acc, "withdrawal", error_log):
        return
    if acc["name"] != name:
        error_log.append("ERROR: withdrawal: Name/account mismatch.")
        return
    if not session.is_admin() and session.cap_withdrawal + amt > CAP_WITHDRAWAL + 1e-9:
        error_log.append("ERROR: withdrawal: Standard session cap of $500 exceeded.")
        return
    if acc["balance"] - amt < -1e-9:
        error_log.append("ERROR: withdrawal: Insufficient funds.")
        return
    acc["balance"] = round(acc["balance"] - amt, 2)
    acc["total_transactions"] += 1
    if not session.is_admin():
        session.cap_withdrawal += amt


def apply_transfer(tokens, session, accounts, error_log):
    """
    Move funds from one account to another.
    Enforces standard session cap ($1000), sufficient funds, and both
    account validities.
    """
    if not session.logged_in:
        error_log.append("ERROR: transfer: Must be logged in.")
        return
    try:
        if session.is_admin():
            name, from_num, to_num, amt_str = tokens[1], tokens[2], tokens[3], tokens[4]
        else:
            name, from_num, to_num, amt_str = session.username, tokens[1], tokens[2], tokens[3]
        amt = float(amt_str)
    except (IndexError, ValueError):
        error_log.append("ERROR: transfer: Invalid arguments.")
        return
    if amt <= 0:
        error_log.append("ERROR: transfer: Amount must be > 0.")
        return
    src = find_account(accounts, from_num)
    dst = find_account(accounts, to_num)
    if not validate_account_active(src, "transfer", error_log):
        return
    if not validate_account_active(dst, "transfer", error_log):
        return
    if src["name"] != name:
        error_log.append("ERROR: transfer: Name/source-account mismatch.")
        return
    if not session.is_admin() and session.cap_transfer + amt > CAP_TRANSFER + 1e-9:
        error_log.append("ERROR: transfer: Standard session cap of $1000 exceeded.")
        return
    if src["balance"] - amt < -1e-9:
        error_log.append("ERROR: transfer: Insufficient funds.")
        return
    src["balance"] = round(src["balance"] - amt, 2)
    dst["balance"] = round(dst["balance"] + amt, 2)
    src["total_transactions"] += 1
    dst["total_transactions"] += 1
    if not session.is_admin():
        session.cap_transfer += amt


def apply_paybill(tokens, session, accounts, error_log):
    """
    Debit an account to pay a registered company (EC, CQ, or FI).
    Enforces standard session cap ($2000) and valid company codes.
    """
    if not session.logged_in:
        error_log.append("ERROR: paybill: Must be logged in.")
        return
    try:
        acct_num, company, amt_str = tokens[1], tokens[2].upper(), tokens[3]
        amt = float(amt_str)
    except (IndexError, ValueError):
        error_log.append("ERROR: paybill: Invalid arguments.")
        return
    if company not in VALID_COMPANIES:
        error_log.append(f"ERROR: paybill: Invalid company '{company}'. Must be EC, CQ, or FI.")
        return
    if amt <= 0:
        error_log.append("ERROR: paybill: Amount must be > 0.")
        return
    acc = find_account(accounts, acct_num)
    if not validate_account_active(acc, "paybill", error_log):
        return
    if not session.is_admin() and acc["name"] != session.username:
        error_log.append("ERROR: paybill: Standard users can only pay from their own account.")
        return
    if not session.is_admin() and session.cap_paybill + amt > CAP_PAYBILL + 1e-9:
        error_log.append("ERROR: paybill: Standard session cap of $2000 exceeded.")
        return
    if acc["balance"] - amt < -1e-9:
        error_log.append("ERROR: paybill: Insufficient funds.")
        return
    acc["balance"] = round(acc["balance"] - amt, 2)
    acc["total_transactions"] += 1
    if not session.is_admin():
        session.cap_paybill += amt


def apply_deposit(tokens, session, accounts, pending_deposits, error_log):
    """
    Queue a deposit for an account; funds are applied on logout, not immediately.
    """
    if not session.logged_in:
        error_log.append("ERROR: deposit: Must be logged in.")
        return
    try:
        if session.is_admin():
            name, acct_num, amt_str = tokens[1], tokens[2], tokens[3]
        else:
            name, acct_num, amt_str = session.username, tokens[1], tokens[2]
        amt = float(amt_str)
    except (IndexError, ValueError):
        error_log.append("ERROR: deposit: Invalid arguments.")
        return
    if amt <= 0:
        error_log.append("ERROR: deposit: Amount must be > 0.")
        return
    acc = find_account(accounts, acct_num)
    if not validate_account_active(acc, "deposit", error_log):
        return
    if acc["name"] != name:
        error_log.append("ERROR: deposit: Name/account mismatch.")
        return
    pending_deposits.append(PendingDeposit(acct_num, amt))


def apply_create(tokens, session, accounts, error_log):
    """
    Create a new account (admin only). New account is not usable in same session.
    """
    if not session.logged_in or not session.is_admin():
        error_log.append("ERROR: create: Admin access required.")
        return
    try:
        name, acct_num, bal_str, plan = tokens[1], tokens[2], tokens[3], tokens[4].upper()
    except IndexError:
        error_log.append("ERROR: create: Invalid arguments.")
        return
    try:
        bal = float(bal_str)
    except ValueError:
        error_log.append("ERROR: create: Invalid balance.")
        return
    if len(name) > 20:
        error_log.append("ERROR: create: Name exceeds 20 characters.")
        return
    if bal <= 0 or bal > 99999.99:
        error_log.append("ERROR: create: Balance must be > 0 and <= $99999.99.")
        return
    if plan not in ("SP", "NP"):
        error_log.append("ERROR: create: Invalid plan. Must be SP or NP.")
        return
    if find_account(accounts, acct_num) is not None:
        error_log.append("ERROR: create: Account number already exists.")
        return
    accounts.append({
        "account_number":     acct_num.lstrip("0") or "0",
        "name":               name.strip(),
        "status":             "A",
        "balance":            round(bal, 2),
        "total_transactions": 0,
        "plan":               plan,
    })


def apply_delete(tokens, session, accounts, error_log):
    """
    Remove an existing account permanently (admin only).
    """
    if not session.logged_in or not session.is_admin():
        error_log.append("ERROR: delete: Admin access required.")
        return
    try:
        name, acct_num = tokens[1], tokens[2]
    except IndexError:
        error_log.append("ERROR: delete: Invalid arguments.")
        return
    acc = find_account(accounts, acct_num)
    if acc is None:
        error_log.append("ERROR: delete: Account does not exist.")
        return
    if acc["name"] != name:
        error_log.append("ERROR: delete: Name/account mismatch.")
        return
    accounts.remove(acc)


def apply_disable(tokens, session, accounts, error_log):
    """
    Mark an existing account as disabled (admin only), blocking future transactions.
    """
    if not session.logged_in or not session.is_admin():
        error_log.append("ERROR: disable: Admin access required.")
        return
    try:
        name, acct_num = tokens[1], tokens[2]
    except IndexError:
        error_log.append("ERROR: disable: Invalid arguments.")
        return
    acc = find_account(accounts, acct_num)
    if acc is None:
        error_log.append("ERROR: disable: Account does not exist.")
        return
    if acc["name"] != name:
        error_log.append("ERROR: disable: Name/account mismatch.")
        return
    acc["status"] = "D"


def apply_changeplan(tokens, session, accounts, error_log):
    """
    Toggle an account's plan between SP and NP (admin only).
    """
    if not session.logged_in or not session.is_admin():
        error_log.append("ERROR: changeplan: Admin access required.")
        return
    try:
        name, acct_num = tokens[1], tokens[2]
    except IndexError:
        error_log.append("ERROR: changeplan: Invalid arguments.")
        return
    acc = find_account(accounts, acct_num)
    if acc is None:
        error_log.append("ERROR: changeplan: Account does not exist.")
        return
    if acc["name"] != name:
        error_log.append("ERROR: changeplan: Name/account mismatch.")
        return
    acc["plan"] = "NP" if acc["plan"] == "SP" else "SP"


def process_transactions(accounts, transaction_lines):
    """
    Apply every transaction in transaction_lines to accounts.
    Returns a list of error strings for any rejected transactions.
    """
    session          = Session()
    pending_deposits = []
    error_log        = []

    for raw in transaction_lines:
        line = raw.strip()
        if not line:
            continue
        tokens = line.split()
        cmd    = tokens[0].lower()

        if cmd == CODE_LOGIN:
            apply_login(tokens, session, accounts, error_log)
        elif cmd == CODE_LOGOUT:
            apply_logout(session, pending_deposits, accounts, error_log)
        elif cmd == CODE_WITHDRAWAL:
            apply_withdrawal(tokens, session, accounts, error_log)
        elif cmd == CODE_TRANSFER:
            apply_transfer(tokens, session, accounts, error_log)
        elif cmd == CODE_PAYBILL:
            apply_paybill(tokens, session, accounts, error_log)
        elif cmd == CODE_DEPOSIT:
            apply_deposit(tokens, session, accounts, pending_deposits, error_log)
        elif cmd == CODE_CREATE:
            apply_create(tokens, session, accounts, error_log)
        elif cmd == CODE_DELETE:
            apply_delete(tokens, session, accounts, error_log)
        elif cmd == CODE_DISABLE:
            apply_disable(tokens, session, accounts, error_log)
        elif cmd == CODE_CHANGEPLAN:
            apply_changeplan(tokens, session, accounts, error_log)
        else:
            error_log.append(f"ERROR: Unknown transaction '{cmd}'.")

    # Force logout if session was left open at end of file
    if session.logged_in:
        apply_logout(session, pending_deposits, accounts, error_log)

    return error_log
