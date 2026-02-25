import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Banking Console Front End (Phase 2 style)
 *
 * Input: commands on stdin (one per line)
 * Output: responses on stdout
 * Output file: daily_transaction_file.txt written on logout
 *
 * Supported command words:
 * login, logout, withdrawal, transfer, paybill, deposit, create, delete, disable, changeplan
 *
 * Command formats (space-separated):
 *
 * login admin
 * login standard <name>
 *
 * logout
 *
 * withdrawal <acctNum> <amount>                    (STANDARD)
 * withdrawal <name> <acctNum> <amount>             (ADMIN)
 *
 * transfer <fromAcct> <toAcct> <amount>            (STANDARD)
 * transfer <name> <fromAcct> <toAcct> <amount>     (ADMIN)
 *
 * paybill <acctNum> <companyCode2Letters> <amount> (STANDARD/ADMIN)
 *
 * deposit <acctNum> <amount>                       (STANDARD)
 * deposit <name> <acctNum> <amount>                (ADMIN)
 *
 * create <name> <acctNum> <startBalance> <SP|NP>   (ADMIN)
 * delete <name> <acctNum>                          (ADMIN)
 * disable <name> <acctNum>                         (ADMIN)
 * changeplan <name> <acctNum>                      (ADMIN)
 *
 * Optional:
 * help
 * quit
 */
public class BankingConsoleApp {

    // ---------- Models ----------
    enum Role { NONE, STANDARD, ADMIN }
    enum Status { ENABLED, DISABLED }
    enum Plan { SP, NP }

    static class Account {
        String name;
        String number;
        double balance;
        Status status;
        Plan plan;

        Account(String name, String number, double balance, Status status, Plan plan) {
            this.name = name;
            this.number = number;
            this.balance = balance;
            this.status = status;
            this.plan = plan;
        }
    }

    static class Session {
        boolean loggedIn = false;
        Role role = Role.NONE;
        String username = "";
        double capWithdrawal = 0;
        double capTransfer = 0;
        double capPaybill = 0;

        void reset() {
            loggedIn = false;
            role = Role.NONE;
            username = "";
            capWithdrawal = 0;
            capTransfer = 0;
            capPaybill = 0;
        }
    }

    static class PendingDeposit {
        String acctNum;
        double amount;
        PendingDeposit(String acctNum, double amount) {
            this.acctNum = acctNum;
            this.amount = amount;
        }
    }

    // ---------- Constants ----------
    static final double CAP_WITHDRAWAL = 500.00;
    static final double CAP_TRANSFER   = 1000.00;
    static final double CAP_PAYBILL    = 2000.00;

    // ---------- State ----------
    static final Map<String, Account> accounts = new LinkedHashMap<>();
    static final List<String> dailyTx = new ArrayList<>();
    static final List<PendingDeposit> pendingDeposits = new ArrayList<>();
    static Session session = new Session();

    static boolean requireLoginForTxn() {
        if (!session.loggedIn) {
            System.out.println("ERROR: must login first");
            return false;
        }
        return true;
    }

    // static boolean requireActiveSessionForLogout() {
    //     if (!session.loggedIn) {
    //         System.out.println("ERROR: no active session");
    //         return false;
    //     }
    //     return true;
    // }

    public static void main(String[] args) throws Exception {
    seedSampleAccounts();
    session = new Session();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {

        while (true) {

            String cmd = readCmdLine(br);
            if (cmd == null) break;
            if (cmd.isEmpty()) continue;

            cmd = cmd.toLowerCase();

            if (cmd.equals("login")) {

                String mode = readCmdLine(br);
                if (mode == null) break;

                if (mode.equalsIgnoreCase("admin")) {
                    loginAdmin();
                    System.out.println("Login successful (admin)");
                }

                else if (mode.equalsIgnoreCase("standard")) {
                    String name = readCmdLine(br);
                    if (name == null) break;

                    loginStandard(name);
                    System.out.println("Login successful (standard)");
                }

                else {
                    System.out.println("Login rejected");
                }
            }

            else if (cmd.equals("logout")) {
                if (!session.loggedIn) {
                    System.out.println("ERROR: no active session");
                } else {
                    logout();
                    System.out.println("Logout successful");
                }
            }
            else if (cmd.equals("withdrawal")) {
                if (!requireLoginForTxn()) continue;
                // later: read args + do withdrawal
            }

            else if (cmd.equals("transfer")) {
                if (!requireLoginForTxn()) continue;
            }

            else if (cmd.equals("paybill")) {
                if (!requireLoginForTxn()) continue;
            }

            else if (cmd.equals("deposit")) {
                if (!requireLoginForTxn()) continue;
            }
        }
    }
}

    static String readCmdLine(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null) return null;
        System.out.println("READ_CMD " + line);
        return line.trim();
    }

    static boolean requireLogin() {
        if (!session.loggedIn) {
            System.out.println("ERROR: must login first");
            return false;
        }
        return true;
    }

    static void loginAdmin() {
        session.loggedIn = true;
        session.role = Role.ADMIN;
    }

    static void loginStandard(String name) {
        session.loggedIn = true;
        session.role = Role.STANDARD;
        session.username = name;
    }

    static void logout() {
        session.reset();
    }

    static void printHelp() {
        System.out.println("""
                Commands:
                  login admin
                  login standard <name>
                  logout

                  withdrawal <acctNum> <amount>                 (STANDARD)
                  withdrawal <name> <acctNum> <amount>          (ADMIN)

                  transfer <from> <to> <amount>                 (STANDARD)
                  transfer <name> <from> <to> <amount>          (ADMIN)

                  paybill <acctNum> <CC> <amount>               (STANDARD/ADMIN)

                  deposit <acctNum> <amount>                    (STANDARD)
                  deposit <name> <acctNum> <amount>             (ADMIN)

                  create <name> <acctNum> <bal> <SP|NP>         (ADMIN)
                  delete <name> <acctNum>                       (ADMIN)
                  disable <name> <acctNum>                      (ADMIN)
                  changeplan <name> <acctNum>                   (ADMIN)

                  help
                  quit
                """);
    }

    static void seedSampleAccounts() {
        put(new Account("Alice", "11111", 1200.00, Status.ENABLED, Plan.SP));
        put(new Account("Bob", "22222", 300.00, Status.ENABLED, Plan.NP));
        put(new Account("Charlie", "33333", 0.00, Status.ENABLED, Plan.NP));
        put(new Account("DisabledUser", "44444", 100.00, Status.DISABLED, Plan.SP));
    }

    static void put(Account a) { accounts.put(a.number, a); }

    // ---------- Parsing + Dispatch ----------
    static void handleLine(String line) {
        String[] t = line.split("\\s+");
        String cmd = t[0].toLowerCase(Locale.ROOT);

        try {
            switch (cmd) {
                case "login" -> cmdLogin(t);
                case "logout" -> cmdLogout(t);
                case "withdrawal" -> cmdWithdrawal(t);
                case "transfer" -> cmdTransfer(t);
                case "paybill" -> cmdPaybill(t);
                case "deposit" -> cmdDeposit(t);

                case "create" -> cmdCreate(t);
                case "delete" -> cmdDelete(t);
                case "disable" -> cmdDisable(t);
                case "changeplan" -> cmdChangeplan(t);

                default -> reject(cmd, "Unknown command.");
            }
        } catch (IllegalArgumentException ex) {
            reject(cmd, ex.getMessage());
        } catch (Exception ex) {
            reject(cmd, "Unexpected error: " + ex.getMessage());
        }
    }

    // ---------- Helpers ----------
    static void info(String msg) { System.out.println("[INFO] " + msg); }
    static void ok(String cmd, String msg) { System.out.println("[OK] " + cmd + " accepted — " + msg); }
    static void reject(String cmd, String reason) { System.out.println("[REJECT] " + cmd + " rejected — " + reason); }

    static void requireLoggedIn() {
        if (!session.loggedIn) throw new IllegalArgumentException("You must login first.");
    }
    static void requireAdmin() {
        requireLoggedIn();
        if (session.role != Role.ADMIN) throw new IllegalArgumentException("Admin access required.");
    }

    static boolean validName(String name) {
        if (name == null) return false;
        String s = name.trim();
        return !s.isEmpty() && s.matches("^[A-Za-z][A-Za-z '\\-]*$");
    }

    static double parseAmount(String s) {
        try {
            double v = Double.parseDouble(s);
            if (!Double.isFinite(v)) return -1;
            return v;
        } catch (Exception e) {
            return -1;
        }
    }

    static Account getAcct(String num) {
        if (num == null) return null;
        return accounts.get(num.trim());
    }

    static void record(String line) { dailyTx.add(line); }

    // ---------- Commands ----------
    static void cmdLogin(String[] t) {
        if (session.loggedIn) {
            reject("login", "Already signed in — logout first.");
            return;
        }
        if (t.length < 2) { reject("login", "Usage: login admin | login standard <name>"); return; }

        String mode = t[1].toLowerCase(Locale.ROOT);

        if (mode.equals("admin")) {
            session.loggedIn = true;
            session.role = Role.ADMIN;
            session.username = "";
            session.capWithdrawal = session.capTransfer = session.capPaybill = 0;
            record("login|admin");
            ok("login", "Admin session started.");
            return;
        }

        if (mode.equals("standard")) {
            if (t.length < 3) { reject("login", "Usage: login standard <name>"); return; }
            String name = joinFrom(t, 2).trim();
            if (!validName(name)) { reject("login", "Invalid name format."); return; }

            boolean exists = accounts.values().stream().anyMatch(a -> a.name.equals(name));
            if (!exists) { reject("login", "Username doesn’t exist."); return; }

            session.loggedIn = true;
            session.role = Role.STANDARD;
            session.username = name;
            session.capWithdrawal = session.capTransfer = session.capPaybill = 0;
            record("login|standard|" + name);
            ok("login", "Welcome, " + name + ".");
            return;
        }

        reject("login", "Invalid mode. Use standard/admin.");
    }

    static void cmdLogout(String[] t) throws IOException {
        if (!session.loggedIn) { reject("logout", "You’re already logged out."); return; }

        // apply pending deposits
        for (PendingDeposit pd : pendingDeposits) {
            Account a = getAcct(pd.acctNum);
            if (a != null) {
                a.balance += pd.amount;
                record("deposit_applied|" + a.number + "|" + money(pd.amount));
            }
        }
        pendingDeposits.clear();

        record("logout");
        writeDailyTxFile("daily_transaction_file.txt");

        session.reset();
        ok("logout", "Signed out. Wrote daily_transaction_file.txt");
    }

    static void writeDailyTxFile(String filename) throws IOException {
        Files.writeString(Paths.get(filename), String.join("\n", dailyTx) + "\n");
    }

    static void cmdWithdrawal(String[] t) {
        requireLoggedIn();

        boolean admin = (session.role == Role.ADMIN);
        String name;
        String acctNum;
        String amtStr;

        if (admin) {
            if (t.length < 4) { reject("withdrawal", "Usage (admin): withdrawal <name> <acctNum> <amount>"); return; }
            name = t[1];
            acctNum = t[2];
            amtStr = t[3];
        } else {
            if (t.length < 3) { reject("withdrawal", "Usage: withdrawal <acctNum> <amount>"); return; }
            name = session.username;
            acctNum = t[1];
            amtStr = t[2];
        }

        double amt = parseAmount(amtStr);
        if (amt <= 0) { reject("withdrawal", "Invalid amount."); return; }

        Account a = getAcct(acctNum);
        if (a == null) { reject("withdrawal", "Invalid account number."); return; }
        if (a.status != Status.ENABLED) { reject("withdrawal", "Account is disabled."); return; }

        if (!a.name.equals(name)) { reject("withdrawal", "Name/account mismatch."); return; }

        if (session.role == Role.STANDARD && (session.capWithdrawal + amt) > CAP_WITHDRAWAL + 1e-9) {
            reject("withdrawal", "Standard session cap exceeded (500).");
            return;
        }

        if ((a.balance - amt) < -1e-9) { reject("withdrawal", "Insufficient funds."); return; }

        a.balance -= amt;
        if (session.role == Role.STANDARD) session.capWithdrawal += amt;

        record("withdrawal|" + name + "|" + a.number + "|" + money(amt));
        ok("withdrawal", "New balance: " + money(a.balance));
    }

    static void cmdTransfer(String[] t) {
        requireLoggedIn();

        boolean admin = (session.role == Role.ADMIN);
        String name;
        String from;
        String to;
        String amtStr;

        if (admin) {
            if (t.length < 5) { reject("transfer", "Usage (admin): transfer <name> <from> <to> <amount>"); return; }
            name = t[1];
            from = t[2];
            to = t[3];
            amtStr = t[4];
        } else {
            if (t.length < 4) { reject("transfer", "Usage: transfer <from> <to> <amount>"); return; }
            name = session.username;
            from = t[1];
            to = t[2];
            amtStr = t[3];
        }

        double amt = parseAmount(amtStr);
        if (amt <= 0) { reject("transfer", "Invalid amount."); return; }

        Account src = getAcct(from);
        Account dst = getAcct(to);
        if (src == null) { reject("transfer", "Invalid source account."); return; }
        if (dst == null) { reject("transfer", "Invalid destination account."); return; }
        if (src.status != Status.ENABLED || dst.status != Status.ENABLED) { reject("transfer", "One account is disabled."); return; }
        if (!src.name.equals(name)) { reject("transfer", "Name/source-account mismatch."); return; }

        if (session.role == Role.STANDARD && (session.capTransfer + amt) > CAP_TRANSFER + 1e-9) {
            reject("transfer", "Standard session cap exceeded (1000).");
            return;
        }

        if ((src.balance - amt) < -1e-9) { reject("transfer", "Insufficient funds."); return; }

        src.balance -= amt;
        dst.balance += amt;
        if (session.role == Role.STANDARD) session.capTransfer += amt;

        record("transfer|" + name + "|" + src.number + "->" + dst.number + "|" + money(amt));
        ok("transfer", "Source balance: " + money(src.balance));
    }

    static void cmdPaybill(String[] t) {
        requireLoggedIn();

        if (t.length < 4) { reject("paybill", "Usage: paybill <acctNum> <CC> <amount>"); return; }
        String acctNum = t[1];
        String cc = t[2].trim().toUpperCase(Locale.ROOT);
        String amtStr = t[3];

        if (!cc.matches("^[A-Z]{2}$")) { reject("paybill", "Invalid company code format."); return; }

        double amt = parseAmount(amtStr);
        if (amt <= 0) { reject("paybill", "Invalid amount."); return; }

        Account a = getAcct(acctNum);
        if (a == null) { reject("paybill", "Invalid account number."); return; }
        if (a.status != Status.ENABLED) { reject("paybill", "Account is disabled."); return; }

        if (session.role == Role.STANDARD && !a.name.equals(session.username)) {
            reject("paybill", "Standard users must pay from their own account.");
            return;
        }

        if (session.role == Role.STANDARD && (session.capPaybill + amt) > CAP_PAYBILL + 1e-9) {
            reject("paybill", "Standard session cap exceeded (2000).");
            return;
        }

        if ((a.balance - amt) < -1e-9) { reject("paybill", "Insufficient funds."); return; }

        a.balance -= amt;
        if (session.role == Role.STANDARD) session.capPaybill += amt;

        record("paybill|" + a.number + "|" + cc + "|" + money(amt));
        ok("paybill", "New balance: " + money(a.balance));
    }

    static void cmdDeposit(String[] t) {
        requireLoggedIn();

        boolean admin = (session.role == Role.ADMIN);
        String name;
        String acctNum;
        String amtStr;

        if (admin) {
            if (t.length < 4) { reject("deposit", "Usage (admin): deposit <name> <acctNum> <amount>"); return; }
            name = t[1];
            acctNum = t[2];
            amtStr = t[3];
        } else {
            if (t.length < 3) { reject("deposit", "Usage: deposit <acctNum> <amount>"); return; }
            name = session.username;
            acctNum = t[1];
            amtStr = t[2];
        }

        double amt = parseAmount(amtStr);
        if (amt <= 0) { reject("deposit", "Invalid amount."); return; }

        Account a = getAcct(acctNum);
        if (a == null) { reject("deposit", "Invalid account number."); return; }
        if (a.status != Status.ENABLED) { reject("deposit", "Account is disabled."); return; }
        if (!a.name.equals(name)) { reject("deposit", "Name/account mismatch."); return; }

        pendingDeposits.add(new PendingDeposit(a.number, amt));
        record("deposit|PENDING|" + name + "|" + a.number + "|" + money(amt));
        ok("deposit", "Accepted (pending). Funds available after logout.");
    }

    // ---------- Admin commands ----------
    static void cmdCreate(String[] t) {
        requireAdmin();
        if (t.length < 5) { reject("create", "Usage: create <name> <acctNum> <bal> <SP|NP>"); return; }

        String name = t[1];
        String acctNum = t[2];
        double bal = parseAmount(t[3]);
        String planStr = t[4].trim().toUpperCase(Locale.ROOT);

        if (!validName(name)) { reject("create", "Invalid name format."); return; }
        if (acctNum.trim().isEmpty()) { reject("create", "Invalid account number."); return; }
        if (bal < 0) { reject("create", "Invalid starting balance."); return; }

        Plan plan;
        try { plan = Plan.valueOf(planStr); }
        catch (Exception e) { reject("create", "Invalid plan."); return; }

        if (accounts.containsKey(acctNum)) { reject("create", "Account number already exists."); return; }

        put(new Account(name.trim(), acctNum.trim(), bal, Status.ENABLED, plan));
        record("create|" + name.trim() + "|" + acctNum.trim() + "|" + money(bal) + "|" + plan);
        ok("create", "New account created.");
    }

    static void cmdDelete(String[] t) {
        requireAdmin();
        if (t.length < 3) { reject("delete", "Usage: delete <name> <acctNum>"); return; }

        String name = t[1].trim();
        String acctNum = t[2].trim();

        Account a = getAcct(acctNum);
        if (a == null) { reject("delete", "Account does not exist."); return; }
        if (!a.name.equals(name)) { reject("delete", "Name/account mismatch."); return; }

        accounts.remove(acctNum);
        record("delete|" + a.name + "|" + a.number);
        ok("delete", "Account removed.");
    }

    static void cmdDisable(String[] t) {
        requireAdmin();
        if (t.length < 3) { reject("disable", "Usage: disable <name> <acctNum>"); return; }

        String name = t[1].trim();
        String acctNum = t[2].trim();

        Account a = getAcct(acctNum);
        if (a == null) { reject("disable", "Account does not exist."); return; }
        if (!a.name.equals(name)) { reject("disable", "Name/account mismatch."); return; }

        a.status = Status.DISABLED;
        record("disable|" + a.name + "|" + a.number);
        ok("disable", "Account disabled.");
    }

    static void cmdChangeplan(String[] t) {
        requireAdmin();
        if (t.length < 3) { reject("changeplan", "Usage: changeplan <name> <acctNum>"); return; }

        String name = t[1].trim();
        String acctNum = t[2].trim();

        Account a = getAcct(acctNum);
        if (a == null) { reject("changeplan", "Account does not exist."); return; }
        if (!a.name.equals(name)) { reject("changeplan", "Name/account mismatch."); return; }

        a.plan = (a.plan == Plan.SP) ? Plan.NP : Plan.SP;
        record("changeplan|" + a.name + "|" + a.number + "|" + a.plan);
        ok("changeplan", "Plan is now " + a.plan + ".");
    }

    // ---------- utils ----------
    static String money(double x) { return String.format(Locale.US, "%.2f", x); }

    static String joinFrom(String[] t, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < t.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(t[i]);
        }
        return sb.toString();
    }
}