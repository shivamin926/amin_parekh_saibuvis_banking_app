import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BankingConsoleApp {

    enum Status { ENABLED, DISABLED }
    enum Plan { SP, NP }

    static class Account {
        String name;           // holder name
        String number;         // account number
        double balance;        // current balance
        Status status;         // ENABLED or DISABLED
        Plan plan;             // SP (Standard Plan) or NP (Non-Profit)

        Account(String n, String num, double bal, Status s, Plan p) {
            name = n;
            number = num;
            balance = bal;
            status = s;
            plan = p;
        }
    }

    // limits per session
    static final double CAP_WITHDRAWAL = 500.0;
    static final double CAP_TRANSFER = 1000.0;
    static final double CAP_PAYBILL = 2000.0;

    // accounts map
    static final Map<String, Account> accounts = new LinkedHashMap<>();

    // pending deposits (applied on logout)
    static final Map<String, Double> pendingDepositsMap = new LinkedHashMap<>();

    // accounts created in current session cannot be used until logout
    static final Set<String> pendingCreatedAccounts = new LinkedHashSet<>();

    // session information (mirrors Python implementation)
    static boolean sessionActive = false;          // login happened
    static String sessionMode = null;              // "standard" or "admin"
    static String sessionUser = null;              // name for standard sessions

    // writer for ATF output file
    static BufferedWriter atfWriter = null;

    // helper to get next account number (5 digits) used by create
    static String nextAccountNumber() {
        int max = accounts.keySet().stream()
                .mapToInt(s -> {
                    try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
                })
                .max().orElse(0);
        return String.format("%05d", max + 1);
    }

    // log-read convenience: prints and returns raw line (may be blank)
    static String readRaw(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null) return null;
        System.out.println("READ_CMD " + line);
        return line;
    }

    // amount parser - parses and reprompts silently on invalid format (no READ_CMD logging)
    static double parseAmountSilentReprompt(BufferedReader br, String firstToken) throws IOException {
        String tok = firstToken;
        while (true) {
            try {
                return Double.parseDouble(tok);
            } catch (Exception e) {
                System.out.println("ERROR: invalid amount");
                tok = br.readLine();
                if (tok == null) return -1;
                tok = tok.trim();
                // NO READ_CMD logging for reprompt after invalid format
            }
        }
    }

    // amount parser - just parses, returns -1 if invalid (no reprompt, no error message)
    static double parseAmountSimple(String token) {
        try {
            return Double.parseDouble(token);
        } catch (Exception e) {
            return -1;  // indicates invalid format
        }
    }

    // amount validator with reprompt loop for invalid range (non-positive or too large)
    // Only called if parseAmountSimple returned a valid number
    static double validateAmountWithReprompt(BufferedReader br, double firstAmount) throws IOException {
        double amt = firstAmount;
        while (true) {
            if (amt <= 0) {
                System.out.println("ERROR: non-positive amount");
            } else if (amt > 99999.99) {
                System.out.println("ERROR: amount too large");
            } else {
                return amt;  // valid amount
            }
            // reprompt - read next line and parse
            String nxt = br.readLine();
            if (nxt == null) return -1;
            nxt = nxt.trim();
            System.out.println("READ_CMD " + nxt);
            amt = parseAmountSimple(nxt);
            if (amt < 0) {
                // Invalid format after reprompt - print error and return failure
                System.out.println("ERROR: invalid amount");
                return -1;
            }
        }
    }

    static boolean acctExists(String acct) { return acct != null && accounts.containsKey(acct); }

    static boolean acctActive(String acct) {
        Account a = accounts.get(acct);
        return a != null && a.status == Status.ENABLED;
    }

    static double effectiveBalance(String acct) {
        Account a = accounts.get(acct);
        return a != null ? a.balance : 0.0;
    }

    static void commitPendingOnLogout() {
        for (var e : pendingDepositsMap.entrySet()) {
            Account a = accounts.get(e.getKey());
            if (a != null) a.balance += e.getValue();
        }
        pendingDepositsMap.clear();
        pendingCreatedAccounts.clear();
    }

    static boolean rejectIfNotLoggedIn() {
        if (!sessionActive) {
            System.out.println("ERROR: must login first");
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        seedSampleAccounts();
        sessionActive = false;
        sessionMode = null;
        sessionUser = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            atfWriter = Files.newBufferedWriter(Paths.get("daily_transaction_file.txt"));
            atfWriter.write("BEGIN_SESSION\n");

            while (true) {
                String raw = br.readLine();
                if (raw == null) break;
                String cmd = raw.trim();
                System.out.println("READ_CMD " + raw);
                if (cmd.isEmpty()) continue;

                switch (cmd) {
                    case "login" -> {
                        if (sessionActive) {
                            System.out.println("ERROR: already logged in");
                            continue;
                        }
                        String modeRaw = readRaw(br);
                        if (modeRaw == null) break;
                        String mode = modeRaw.trim();
                        if (mode.equals("admin")) {
                            sessionActive = true;
                            sessionMode = "admin";
                            sessionUser = null;
                            atfWriter.write("LOGIN admin\n");
                            System.out.println("Login successful (admin)");
                            continue;
                        }
                        if (mode.equals("standard")) {
                            String nameRaw = readRaw(br);
                            if (nameRaw == null) break;
                            // Check for blank name - this is an error, do NOT reprompt
                            if (nameRaw.trim().isEmpty()) {
                                System.out.println("ERROR: invalid name");
                                continue;  // Back to main loop, treat next line as new command
                            }
                            String name = nameRaw.trim();
                            if (accounts.values().stream().noneMatch(a -> a.name.equals(name))) {
                                System.out.println("ERROR: unknown user");
                                continue;
                            }
                            sessionActive = true;
                            sessionMode = "standard";
                            sessionUser = name;
                            atfWriter.write("LOGIN standard " + name + "\n");
                            System.out.println("Login successful (standard)");
                            continue;
                        }
                        System.out.println("ERROR: invalid login mode");
                    }
                    case "logout" -> {
                        if (!sessionActive) {
                            System.out.println("ERROR: no active session");
                        } else {
                            atfWriter.write("LOGOUT\n");
                            commitPendingOnLogout();
                            sessionActive = false;
                            sessionMode = null;
                            sessionUser = null;
                            System.out.println("Logout successful");
                        }
                    }
                    case "withdrawal" -> {
                        if (rejectIfNotLoggedIn()) continue;
                        if (sessionMode.equals("standard")) {
                            String acctRaw = readRaw(br);
                            if (acctRaw == null) break;
                            String acct = acctRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSilentReprompt(br, amtToken);
                            if (amt < 0) break;
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (pendingCreatedAccounts.contains(acct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(acct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!acctActive(acct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            if (!accounts.get(acct).name.equals(sessionUser)) {
                                System.out.println("ERROR: account not owned by user");
                                continue;
                            }
                            if (amt > CAP_WITHDRAWAL) {
                                System.out.println("ERROR: standard withdrawal limit exceeded");
                                continue;
                            }
                            if (effectiveBalance(acct) - amt < 0) {
                                System.out.println("ERROR: insufficient funds");
                                continue;
                            }
                            accounts.get(acct).balance -= amt;
                            atfWriter.write(String.format("WDR %s %.2f\n", acct, amt));
                            System.out.println("Withdrawal successful");
                        } else {
                            // admin
                            String nameRaw = readRaw(br);
                            if (nameRaw == null) break;
                            String name = nameRaw.trim();
                            String acctRaw = readRaw(br);
                            if (acctRaw == null) break;
                            String acct = acctRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSilentReprompt(br, amtToken);
                            if (amt < 0) break;
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (pendingCreatedAccounts.contains(acct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(acct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!accounts.get(acct).name.equals(name)) {
                                System.out.println("ERROR: name/account mismatch");
                                continue;
                            }
                            if (!acctActive(acct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            if (effectiveBalance(acct) - amt < 0) {
                                System.out.println("ERROR: insufficient funds");
                                continue;
                            }
                            accounts.get(acct).balance -= amt;
                            atfWriter.write(String.format("WDR %s %.2f %s\n", acct, amt, name));
                            System.out.println("Withdrawal successful");
                        }
                    }
                    case "transfer" -> {
                        if (rejectIfNotLoggedIn()) continue;
                        if (sessionMode.equals("standard")) {
                            String fromRaw = readRaw(br);
                            if (fromRaw == null) break;
                            String fromAcct = fromRaw.trim();
                            String toRaw = readRaw(br);
                            if (toRaw == null) break;
                            String toAcct = toRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSimple(amtToken);
                            if (amt < 0) {
                                System.out.println("ERROR: invalid amount");
                                continue;
                            }
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (amt > CAP_TRANSFER) {
                                System.out.println("ERROR: standard transfer limit exceeded");
                                continue;
                            }
                            if (pendingCreatedAccounts.contains(fromAcct) || pendingCreatedAccounts.contains(toAcct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(fromAcct) || !acctExists(toAcct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!acctActive(fromAcct) || !acctActive(toAcct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            if (!accounts.get(fromAcct).name.equals(sessionUser)) {
                                System.out.println("ERROR: source account not owned by user");
                                continue;
                            }
                            if (effectiveBalance(fromAcct) - amt < 0) {
                                System.out.println("ERROR: insufficient funds");
                                continue;
                            }
                            accounts.get(fromAcct).balance -= amt;
                            accounts.get(toAcct).balance += amt;
                            atfWriter.write(String.format("XFR %s %s %.2f\n", fromAcct, toAcct, amt));
                            System.out.println("Transfer successful");
                        } else {
                            String nameRaw = readRaw(br);
                            if (nameRaw == null) break;
                            String name = nameRaw.trim();
                            String fromRaw = readRaw(br);
                            if (fromRaw == null) break;
                            String fromAcct = fromRaw.trim();
                            String toRaw = readRaw(br);
                            if (toRaw == null) break;
                            String toAcct = toRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSilentReprompt(br, amtToken);
                            if (amt < 0) break;
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (pendingCreatedAccounts.contains(fromAcct) || pendingCreatedAccounts.contains(toAcct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(fromAcct) || !acctExists(toAcct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!accounts.get(fromAcct).name.equals(name)) {
                                System.out.println("ERROR: name/account mismatch");
                                continue;
                            }
                            if (!acctActive(fromAcct) || !acctActive(toAcct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            if (effectiveBalance(fromAcct) - amt < 0) {
                                System.out.println("ERROR: insufficient funds");
                                continue;
                            }
                            accounts.get(fromAcct).balance -= amt;
                            accounts.get(toAcct).balance += amt;
                            atfWriter.write(String.format("XFR %s %s %.2f %s\n", fromAcct, toAcct, amt, name));
                            System.out.println("Transfer successful");
                        }
                    }
                    case "paybill" -> {
                        if (rejectIfNotLoggedIn()) continue;
                        String acctRaw = readRaw(br);
                        if (acctRaw == null) break;
                        String acct = acctRaw.trim();
                        String ccRaw = readRaw(br);
                        if (ccRaw == null) break;
                        String cc = ccRaw.trim();
                        String amtTokenRaw = readRaw(br);
                        if (amtTokenRaw == null) break;
                        String amtToken = amtTokenRaw.trim();
                        double amt = parseAmountSilentReprompt(br, amtToken);
                        if (amt < 0) break;
                        amt = validateAmountWithReprompt(br, amt);
                        if (amt < 0) break;
                        if (!cc.matches("^[A-Z]{2}$")) {
                            System.out.println("ERROR: invalid company code");
                            continue;
                        }
                        if (!acctExists(acct)) {
                            System.out.println("ERROR: invalid account");
                            continue;
                        }
                        if (!acctActive(acct)) {
                            System.out.println("ERROR: account disabled");
                            continue;
                        }
                        if (sessionMode.equals("standard") && !accounts.get(acct).name.equals(sessionUser)) {
                            System.out.println("ERROR: standard users must pay from their own account");
                            continue;
                        }
                        if (sessionMode.equals("standard") && (amt > CAP_PAYBILL)) {
                            System.out.println("ERROR: standard paybill limit exceeded");
                            continue;
                        }
                        if (effectiveBalance(acct) - amt < 0) {
                            System.out.println("ERROR: insufficient funds");
                            continue;
                        }
                        accounts.get(acct).balance -= amt;
                        atfWriter.write(String.format("BILL %s %s %.2f\n", acct, cc, amt));
                        System.out.println("Paybill successful");
                    }
                    case "deposit" -> {
                        if (rejectIfNotLoggedIn()) continue;
                        if (sessionMode.equals("standard")) {
                            String acctRaw = readRaw(br);
                            if (acctRaw == null) break;
                            String acct = acctRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSilentReprompt(br, amtToken);
                            if (amt < 0) break;
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (pendingCreatedAccounts.contains(acct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(acct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!acctActive(acct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            if (!accounts.get(acct).name.equals(sessionUser)) {
                                System.out.println("ERROR: account not owned by user");
                                continue;
                            }
                            pendingDepositsMap.merge(acct, amt, Double::sum);
                            atfWriter.write(String.format("DEP %s %.2f\n", acct, amt));
                            System.out.println("Deposit accepted (available next session)");
                        } else {
                            String nameRaw = readRaw(br);
                            if (nameRaw == null) break;
                            String name = nameRaw.trim();
                            String acctRaw = readRaw(br);
                            if (acctRaw == null) break;
                            String acct = acctRaw.trim();
                            String amtTokenRaw = readRaw(br);
                            if (amtTokenRaw == null) break;
                            String amtToken = amtTokenRaw.trim();
                            double amt = parseAmountSilentReprompt(br, amtToken);
                            if (amt < 0) break;
                            amt = validateAmountWithReprompt(br, amt);
                            if (amt < 0) break;
                            if (pendingCreatedAccounts.contains(acct)) {
                                System.out.println("ERROR: account not available in same session");
                                continue;
                            }
                            if (!acctExists(acct)) {
                                System.out.println("ERROR: invalid account");
                                continue;
                            }
                            if (!accounts.get(acct).name.equals(name)) {
                                System.out.println("ERROR: name/account mismatch");
                                continue;
                            }
                            if (!acctActive(acct)) {
                                System.out.println("ERROR: account disabled");
                                continue;
                            }
                            pendingDepositsMap.merge(acct, amt, Double::sum);
                            atfWriter.write(String.format("DEP %s %.2f %s\n", acct, amt, name));
                            System.out.println("Deposit accepted (available next session)");
                        }
                    }
                    case "create" -> {
                        if (!sessionActive || !"admin".equals(sessionMode)) {
                            System.out.println("ERROR: insufficient privilege");
                            continue;
                        }
                        String nameRaw = readRaw(br);
                        if (nameRaw == null) break;
                        if (nameRaw.trim().isEmpty()) {
                            System.out.println("ERROR: invalid name");
                            continue;
                        }
                        String name = nameRaw.trim();
                        String balRaw = readRaw(br);
                        if (balRaw == null) break;
                        double bal = parseAmountSilentReprompt(br, balRaw.trim());
                        if (bal < 0) break;
                        bal = validateAmountWithReprompt(br, bal);
                        if (bal < 0) break;
                        String acct = nextAccountNumber();
                        accounts.put(acct, new Account(name, acct, bal, Status.ENABLED, Plan.SP));
                        pendingCreatedAccounts.add(acct);
                        atfWriter.write(String.format("NEW %s %s %.2f\n", name, acct, bal));
                        System.out.println("Account created (available next session): " + acct);
                    }
                    case "delete" -> {
                        if (!sessionActive || !"admin".equals(sessionMode)) {
                            System.out.println("ERROR: insufficient privilege");
                            continue;
                        }
                        String nameRaw = readRaw(br);
                        if (nameRaw == null) break;
                        String name = nameRaw.trim();
                        String acctRaw = readRaw(br);
                        if (acctRaw == null) break;
                        String acct = acctRaw.trim();
                        if (!acctExists(acct)) {
                            System.out.println("ERROR: invalid account");
                            continue;
                        }
                        if (!accounts.get(acct).name.equals(name)) {
                            System.out.println("ERROR: name/account mismatch");
                            continue;
                        }
                        accounts.remove(acct);
                        atfWriter.write(String.format("DEL %s %s\n", name, acct));
                        System.out.println("Account deleted");
                    }
                    case "disable" -> {
                        if (!sessionActive || !"admin".equals(sessionMode)) {
                            System.out.println("ERROR: insufficient privilege");
                            continue;
                        }
                        String nameRaw = readRaw(br);
                        if (nameRaw == null) break;
                        String name = nameRaw.trim();
                        String acctRaw = readRaw(br);
                        if (acctRaw == null) break;
                        String acct = acctRaw.trim();
                        if (!acctExists(acct)) {
                            System.out.println("ERROR: invalid account");
                            continue;
                        }
                        Account a = accounts.get(acct);
                        if (!a.name.equals(name)) {
                            System.out.println("ERROR: name/account mismatch");
                            continue;
                        }
                        if (!acctActive(acct)) {
                            System.out.println("ERROR: already disabled");
                            continue;
                        }
                        a.status = Status.DISABLED;
                        atfWriter.write(String.format("DIS %s %s\n", name, acct));
                        System.out.println("Account disabled");
                    }
                    case "changeplan" -> {
                        if (!sessionActive || !"admin".equals(sessionMode)) {
                            System.out.println("ERROR: insufficient privilege");
                            continue;
                        }
                        String nameRaw = readRaw(br);
                        if (nameRaw == null) break;
                        String name = nameRaw.trim();
                        String acctRaw = readRaw(br);
                        if (acctRaw == null) break;
                        String acct = acctRaw.trim();
                        if (!acctExists(acct)) {
                            System.out.println("ERROR: invalid account");
                            continue;
                        }
                        Account a = accounts.get(acct);
                        if (!a.name.equals(name)) {
                            System.out.println("ERROR: name/account mismatch");
                            continue;
                        }
                        a.plan = (a.plan == Plan.SP) ? Plan.NP : Plan.SP;
                        atfWriter.write(String.format("CPL %s %s %s\n", name, acct, a.plan));
                        System.out.println("Plan is now " + a.plan + ".");
                    }
                    default -> {
                        // Unknown command - check if we need to be logged in
                        if (!sessionActive && !cmd.equals("login") && !cmd.equals("logout")) {
                            System.out.println("ERROR: must login first");
                        }
                        // otherwise silently ignore unknown commands
                    }
                }
            }
            atfWriter.write("END_SESSION\n");
            atfWriter.close();
        }
    }

    static void seedSampleAccounts() {
        // Match currentaccounts.txt: acct,name,balance,status,plan
        put(new Account("Candice", "00001", 1000.00, Status.ENABLED, Plan.SP));
        put(new Account("Jake",    "00002", 500.00, Status.ENABLED, Plan.NP));
        put(new Account("Bob",     "00003", 250.00, Status.DISABLED, Plan.SP));
    }

    static void put(Account a) { accounts.put(a.number, a); }
}
