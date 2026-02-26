/* 
   Phase 2 Web Front End — Banking UI
   - Command words: login, logout, withdrawal, transfer, paybill,
     deposit, create, delete, disable, changeplan
   - Messages shown as toasts (no “stdout log card”)
   */
// small front-end logic for demo banking actions and UI helpers

const K_SESSION = "fe_session";
const K_ACCOUNTS = "fe_accounts";
const K_TX = "fe_daily_tx";
const K_PENDING = "fe_pending_deposits";

const DEFAULT_ACCOUNTS = [
  { name: "Alice", number: "11111", balance: 1200.00, status: "ENABLED", plan: "SP" },
  { name: "Bob", number: "22222", balance: 300.00,  status: "ENABLED", plan: "NP" },
  { name: "Charlie", number: "33333", balance: 0.00,  status: "ENABLED", plan: "NP" },
  { name: "DisabledUser", number: "44444", balance: 100.00, status: "DISABLED", plan: "SP" }
];

const CAP_WITHDRAWAL = 500.00;
const CAP_TRANSFER   = 1000.00;
const CAP_PAYBILL    = 2000.00;

// set default data in localStorage when app first runs
function initIfNeeded(){
  if (!localStorage.getItem(K_ACCOUNTS)) localStorage.setItem(K_ACCOUNTS, JSON.stringify(DEFAULT_ACCOUNTS));
  if (!localStorage.getItem(K_TX)) localStorage.setItem(K_TX, JSON.stringify([]));
  if (!localStorage.getItem(K_PENDING)) localStorage.setItem(K_PENDING, JSON.stringify([]));
  if (!localStorage.getItem(K_SESSION)) {
    localStorage.setItem(K_SESSION, JSON.stringify({
      loggedIn:false, role:"NONE", username:"",
      totals:{ withdrawal:0, transfer:0, paybill:0 }
    }));
  }
  ensureToastHost();
}

// get current session object from localStorage
function getSession(){ return JSON.parse(localStorage.getItem(K_SESSION)); }

// save session object to localStorage
function setSession(s){ localStorage.setItem(K_SESSION, JSON.stringify(s)); }

// read saved accounts from localStorage
function getAccounts(){ return JSON.parse(localStorage.getItem(K_ACCOUNTS)); }

// write accounts to localStorage
function setAccounts(a){ localStorage.setItem(K_ACCOUNTS, JSON.stringify(a)); }

// read daily transaction lines
function getDailyTx(){ return JSON.parse(localStorage.getItem(K_TX)); }

// write daily transaction lines
function setDailyTx(lines){ localStorage.setItem(K_TX, JSON.stringify(lines)); }

// read pending deposits
function getPending(){ return JSON.parse(localStorage.getItem(K_PENDING)); }

// write pending deposits
function setPending(items){ localStorage.setItem(K_PENDING, JSON.stringify(items)); }

// add a line to the daily transaction list
function recordTx(line){
  const tx = getDailyTx();
  tx.push(line);
  setDailyTx(tx);
}

/* UI helpers  */
// make sure there's a DOM element to show toast messages
function ensureToastHost(){
  let host = document.getElementById("toasts");
  if (!host){
    host = document.createElement("div");
    host.id = "toasts";
    host.className = "toasts";
    document.body.appendChild(host);
  }
}

// show a small message on screen for feedback
function toast(type, title, msg){
  ensureToastHost();
  const host = document.getElementById("toasts");

  const div = document.createElement("div");
  div.className = `toast ${type}`;
  div.innerHTML = `
    <div class="toastDot"></div>
    <div>
      <p class="toastTitle">${escapeHtml(title)}</p>
      <p class="toastMsg">${escapeHtml(msg)}</p>
    </div>
  `;
  host.appendChild(div);

  setTimeout(() => {
    div.style.opacity = "0";
    div.style.transform = "translateY(-6px)";
    div.style.transition = "all .2s ease";
    setTimeout(() => div.remove(), 180);
  }, 2600);
}

// escape text before inserting into HTML to avoid mistakes
function escapeHtml(str){
  return String(str)
    .replaceAll("&","&amp;")
    .replaceAll("<","&lt;")
    .replaceAll(">","&gt;")
    .replaceAll('"',"&quot;")
    .replaceAll("'","&#039;");
}

// update the small banner that shows who is signed in
function setSessionBanner(){
  const el = document.getElementById("sessionBanner");
  if (!el) return;
  const s = getSession();
  if (!s.loggedIn) el.textContent = "Logged out";
  else el.textContent = (s.role === "ADMIN") ? "Signed in: Admin" : `Signed in: ${s.username}`;
}

// navigate to another page
function redirect(path){ window.location.href = path; }

// Guards
// block actions if user is not signed in
function requireLoggedIn(){
  const s = getSession();
  if (!s.loggedIn){
    toast("bad", "Action blocked", "You must login first.");
    redirect("login.html");
    return false;
  }
  return true;
}
// block actions if user is not an admin
function requireAdmin(){
  const s = getSession();
  if (!requireLoggedIn()) return false;
  if (s.role !== "ADMIN"){
    toast("bad", "Not allowed", "Admin access required.");
    redirect("dashboard.html");
    return false;
  }
  return true;
}


// Validation
// check name looks like a real name
function validNameFormat(name){
  if (!name) return false;
  const s = name.trim();
  if (!s) return false;
  return /^[A-Za-z][A-Za-z '\-]*$/.test(s);
}
// convert input to a number or return null
function parseAmount(x){
  const v = Number(x);
  if (!Number.isFinite(v)) return null;
  return v;
}
// find an account object by account number
function findAccount(number){
  return getAccounts().find(a => a.number === String(number).trim());
}
// replace an account in the stored accounts list
function saveAccount(updated){
  const accounts = getAccounts();
  const i = accounts.findIndex(a => a.number === updated.number);
  if (i >= 0) accounts[i] = updated;
  setAccounts(accounts);
}

// Command: Login
// handle login for admin or standard users
function cmd_login(mode, nameMaybe){
  const s = getSession();
  if (s.loggedIn){
    toast("bad", "login rejected", "Already signed in — logout first.");
    return false;
  }

  if (mode === "admin"){
    s.loggedIn = true;
    s.role = "ADMIN";
    s.username = "";
    s.totals = { withdrawal:0, transfer:0, paybill:0 };
    setSession(s);
    recordTx("login|admin");
    toast("ok", "Successfully signed in", "Admin session started.");
    return true;
  }

  if (mode === "standard"){
    const name = (nameMaybe || "").trim();
    if (!validNameFormat(name)){
      toast("bad", "login rejected", "Invalid name format.");
      return false;
    }
    const exists = getAccounts().some(a => a.name === name);
    if (!exists){
      toast("bad", "login rejected", "Username doesn’t exist.");
      return false;
    }

    s.loggedIn = true;
    s.role = "STANDARD";
    s.username = name;
    s.totals = { withdrawal:0, transfer:0, paybill:0 };
    setSession(s);
    recordTx(`login|standard|${name}`);
    toast("ok", "Successfully signed in", `Welcome, ${name}.`);
    return true;
  }

  toast("bad", "login rejected", "Invalid mode. Use standard/admin.");
  return false;
}

// Command: Logout
// handle logout and apply pending deposits when logging out
function cmd_logout(){
  const s = getSession();
  if (!s.loggedIn){
    toast("bad", "logout rejected", "You’re already logged out.");
    return false;
  }

  // Apply pending deposits at logout
  const pending = getPending();
  for (const pd of pending){
    const a = findAccount(pd.number);
    if (a){
      a.balance = Number(a.balance) + Number(pd.amount);
      saveAccount(a);
      recordTx(`deposit_applied|${pd.number}|${Number(pd.amount).toFixed(2)}`);
    }
  }
  setPending([]);

  recordTx("logout");

  s.loggedIn = false;
  s.role = "NONE";
  s.username = "";
  s.totals = { withdrawal:0, transfer:0, paybill:0 };
  setSession(s);

  toast("ok", "Signed out", "Daily transactions are ready to download.");
  return true;
}

// create and download the daily transaction file
function downloadDailyTxFile(){
  const lines = getDailyTx();
  const blob = new Blob([lines.join("\n") + "\n"], { type:"text/plain" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "daily_transaction_file.txt";
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(a.href);
}

// Commands: withdrawal / transfer / paybill / deposit
// try to withdraw money from an account with checks
function cmd_withdrawal({ name, number, amount }){
  const s = getSession();
  const amt = parseAmount(amount);
  if (amt === null || amt <= 0) return fail("withdrawal", "Invalid amount.");

  const acct = findAccount(number);
  if (!acct) return fail("withdrawal", "Invalid account number.");
  if (acct.status !== "ENABLED") return fail("withdrawal", "Account is disabled.");

  const owner = (s.role === "ADMIN") ? (name || "").trim() : s.username;
  if (!owner) return fail("withdrawal", "Missing name (admin).");
  if (acct.name !== owner) return fail("withdrawal", "Name/account mismatch.");

  if (s.role === "STANDARD" && (s.totals.withdrawal + amt) > CAP_WITHDRAWAL + 1e-9)
    return fail("withdrawal", "Standard session cap exceeded (500).");

  if ((acct.balance - amt) < -1e-9) return fail("withdrawal", "Insufficient funds.");

  acct.balance = Number(acct.balance) - amt;
  saveAccount(acct);

  if (s.role === "STANDARD"){
    s.totals.withdrawal += amt;
    setSession(s);
  }

  recordTx(`withdrawal|${owner}|${acct.number}|${amt.toFixed(2)}`);
  toast("ok", "withdrawal accepted", `New balance: ${acct.balance.toFixed(2)}`);
  return true;
}

// move money from one account to another with checks
function cmd_transfer({ name, from, to, amount }){
  const s = getSession();
  const amt = parseAmount(amount);
  if (amt === null || amt <= 0) return fail("transfer", "Invalid amount.");

  const src = findAccount(from);
  const dst = findAccount(to);
  if (!src) return fail("transfer", "Invalid source account.");
  if (!dst) return fail("transfer", "Invalid destination account.");
  if (src.status !== "ENABLED" || dst.status !== "ENABLED")
    return fail("transfer", "One account is disabled.");

  const owner = (s.role === "ADMIN") ? (name || "").trim() : s.username;
  if (!owner) return fail("transfer", "Missing name (admin).");
  if (src.name !== owner) return fail("transfer", "Name/source-account mismatch.");

  if (s.role === "STANDARD" && (s.totals.transfer + amt) > CAP_TRANSFER + 1e-9)
    return fail("transfer", "Standard session cap exceeded (1000).");

  if ((src.balance - amt) < -1e-9) return fail("transfer", "Insufficient funds.");

  src.balance = Number(src.balance) - amt;
  dst.balance = Number(dst.balance) + amt;
  saveAccount(src);
  saveAccount(dst);

  if (s.role === "STANDARD"){
    s.totals.transfer += amt;
    setSession(s);
  }

  recordTx(`transfer|${owner}|${src.number}->${dst.number}|${amt.toFixed(2)}`);
  toast("ok", "transfer accepted", `Source balance: ${src.balance.toFixed(2)}`);
  return true;
}

// pay a bill to a company from an account
function cmd_paybill({ number, company, amount }){
  const s = getSession();
  const amt = parseAmount(amount);
  if (amt === null || amt <= 0) return fail("paybill", "Invalid amount.");

  const acct = findAccount(number);
  if (!acct) return fail("paybill", "Invalid account number.");
  if (acct.status !== "ENABLED") return fail("paybill", "Account is disabled.");

  const comp = (company || "").trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(comp)) return fail("paybill", "Invalid company code format.");

  if (s.role === "STANDARD" && acct.name !== s.username)
    return fail("paybill", "Standard users must pay from their own account.");

  if (s.role === "STANDARD" && (s.totals.paybill + amt) > CAP_PAYBILL + 1e-9)
    return fail("paybill", "Standard session cap exceeded (2000).");

  if ((acct.balance - amt) < -1e-9) return fail("paybill", "Insufficient funds.");

  acct.balance = Number(acct.balance) - amt;
  saveAccount(acct);

  if (s.role === "STANDARD"){
    s.totals.paybill += amt;
    setSession(s);
  }

  recordTx(`paybill|${acct.number}|${comp}|${amt.toFixed(2)}`);
  toast("ok", "paybill accepted", `New balance: ${acct.balance.toFixed(2)}`);
  return true;
}

// add a pending deposit (applied on logout)
function cmd_deposit({ name, number, amount }){
  const s = getSession();
  const amt = parseAmount(amount);
  if (amt === null || amt <= 0) return fail("deposit", "Invalid amount.");

  const acct = findAccount(number);
  if (!acct) return fail("deposit", "Invalid account number.");
  if (acct.status !== "ENABLED") return fail("deposit", "Account is disabled.");

  const owner = (s.role === "ADMIN") ? (name || "").trim() : s.username;
  if (!owner) return fail("deposit", "Missing name (admin).");
  if (acct.name !== owner) return fail("deposit", "Name/account mismatch.");

  const pending = getPending();
  pending.push({ number: acct.number, amount: amt });
  setPending(pending);

  recordTx(`deposit|PENDING|${owner}|${acct.number}|${amt.toFixed(2)}`);
  toast("ok", "deposit accepted", "Funds will be available after logout.");
  return true;
}

// Privileged: create/delete/disable/changeplan (minimal)
// create a new account (admin only)
function cmd_create({ name, number, balance, plan }){
  const amt = parseAmount(balance);
  if (!validNameFormat(name)) return fail("create", "Invalid name format.");
  if (!number || !String(number).trim()) return fail("create", "Invalid account number.");
  if (amt === null || amt < 0) return fail("create", "Invalid starting balance.");
  const pl = (plan || "").trim().toUpperCase();
  if (pl !== "SP" && pl !== "NP") return fail("create", "Invalid plan.");

  if (findAccount(number)) return fail("create", "Account number already exists.");

  const accounts = getAccounts();
  accounts.push({ name:name.trim(), number:String(number).trim(), balance:amt, status:"ENABLED", plan:pl });
  setAccounts(accounts);

  recordTx(`create|${name.trim()}|${String(number).trim()}|${amt.toFixed(2)}|${pl}`);
  toast("ok", "create accepted", "New account created.");
  return true;
}

// delete an account if name matches (admin only)
function cmd_delete({ name, number }){
  const acct = findAccount(number);
  if (!acct) return fail("delete", "Account does not exist.");
  if ((name || "").trim() !== acct.name) return fail("delete", "Name/account mismatch.");

  setAccounts(getAccounts().filter(a => a.number !== acct.number));
  recordTx(`delete|${acct.name}|${acct.number}`);
  toast("ok", "delete accepted", "Account removed.");
  return true;
}

// disable an account (admin only)
function cmd_disable({ name, number }){
  const acct = findAccount(number);
  if (!acct) return fail("disable", "Account does not exist.");
  if ((name || "").trim() !== acct.name) return fail("disable", "Name/account mismatch.");

  acct.status = "DISABLED";
  saveAccount(acct);
  recordTx(`disable|${acct.name}|${acct.number}`);
  toast("ok", "disable accepted", "Account disabled.");
  return true;
}

// toggle the account plan between SP and NP
function cmd_changeplan({ name, number }){
  const acct = findAccount(number);
  if (!acct) return fail("changeplan", "Account does not exist.");
  if ((name || "").trim() !== acct.name) return fail("changeplan", "Name/account mismatch.");

  acct.plan = (acct.plan === "SP") ? "NP" : "SP";
  saveAccount(acct);
  recordTx(`changeplan|${acct.name}|${acct.number}|${acct.plan}`);
  toast("ok", "changeplan accepted", `Plan is now ${acct.plan}.`);
  return true;
}

// helper to show a failure toast and return false
function fail(cmd, reason){
  toast("bad", `${cmd} rejected`, reason);
  return false;
}

// Common wiring for pages  
function wireCommon(){
  initIfNeeded();
  setSessionBanner();
  lockNavWhenLoggedOut();   // disable nav links if not logged in

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn){
    logoutBtn.addEventListener("click", () => {
      if (!requireLoggedIn()) return;
      cmd_logout();
      setSessionBanner();
      redirect("login.html");
    });
  }

  const dlBtn = document.getElementById("downloadTxBtn");
  if (dlBtn){
    dlBtn.addEventListener("click", () => downloadDailyTxFile());
  }
}


// ui polish: show/hide nav links based on login state and role, and set active page highlight

function setNavUI(){
  const s = getSession();

  // session banner is already handled by setSessionBanner()
  // toggle login/logout buttons if they exist
  const loginLink =
    document.querySelector('a[href="login.html"]') ||
    document.querySelector('[data-action="login"]');

  const logoutBtn = document.getElementById("logoutBtn");
  const downloadBtn = document.getElementById("downloadTxBtn");

  if (loginLink) loginLink.style.display = s.loggedIn ? "none" : "inline-flex";
  if (logoutBtn) logoutBtn.style.display = s.loggedIn ? "inline-flex" : "none";
  if (downloadBtn) downloadBtn.style.display = s.loggedIn ? "inline-flex" : "none";
}

function setActiveNav(){
  // auto-detect the page key from filename
  const file = (location.pathname.split("/").pop() || "").toLowerCase();

  let key = "dashboard";
  if (file.includes("transfer")) key = "transfer";
  else if (file.includes("paybill")) key = "paybill";
  else if (file.includes("deposit")) key = "deposit";
  else if (file.includes("withdrawal")) key = "dashboard"; // not in nav, keep overview active
  else if (file.includes("create") || file.includes("delete") || file.includes("disable") || file.includes("changeplan")) key = "dashboard";
  else if (file.includes("login")) key = "dashboard";

  document.querySelectorAll("[data-nav]").forEach(a => {
    a.classList.toggle("active", a.getAttribute("data-nav") === key);
  });
}

// Update wireCommon to apply navbar UI polish 
const _wireCommon = wireCommon;
wireCommon = function(){
  _wireCommon();
  setNavUI();
  setActiveNav();
};

function lockNavWhenLoggedOut(){
  const s = getSession();
  document.querySelectorAll(".navLinks a").forEach(a => {
    const href = (a.getAttribute("href") || "").toLowerCase();

    // allow dashboard always (overview)
    const isOverview = href.includes("dashboard.html");

    if (!s.loggedIn && !isOverview){
      a.style.pointerEvents = "none";
      a.style.opacity = "0.45";
      a.title = "Please login first";
    } else {
      a.style.pointerEvents = "auto";
      a.style.opacity = "1";
      a.title = "";
    }
  });
}
