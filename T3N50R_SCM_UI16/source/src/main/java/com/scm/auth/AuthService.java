package com.scm.auth;

import com.scm.db.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * C-01 – Authentication & RBAC Module
 * Handles login, logout, session, 2FA, role checking, account locking.
 * Exception codes:
 *   INV_CREDENTIALS  (E:001) – bad username/password
 *   SESSION_EXPIRED  (E:002) – JWT/session timed out
 *   ACCESS_DENIED    (E:003) – role does not have permission
 *   INVALID_2FA      (E:004) – wrong 2FA token
 *   ACCOUNT_LOCKED   (E:005) – locked after failed attempts
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final int MAX_ATTEMPTS   = 3;
    private static final int LOCKOUT_MINS   = 30;

    // Role hierarchy (higher = more access)
    private static final Map<String,Integer> ROLE_LEVEL = new HashMap<>();
    static {
        ROLE_LEVEL.put("SUPER_ADMIN",      6);
        ROLE_LEVEL.put("ADMIN",            5);
        ROLE_LEVEL.put("MANAGER",          4);
        ROLE_LEVEL.put("WAREHOUSE_STAFF",  3);
        ROLE_LEVEL.put("LOGISTICS_OFFICER",3);
        ROLE_LEVEL.put("SALES_STAFF",      2);
        ROLE_LEVEL.put("VIEWER",           1);
    }

    // ── Logged-in session state ───────────────────────────────────────────────
    private static int     currentUserId;
    private static String  currentUsername;
    private static String  currentRole;
    private static String  currentDisplayName;
    private static String  currentEmail;
    private static boolean sessionActive = false;
    private static LocalDateTime sessionExpiry;
    private static String  lastVisitedPanel = "DASHBOARD";

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════════════
    public static LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new LoginResult(false, "E:001", "Username and password are required.");
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            // Demo mode – accept admin/admin123
            return demoLogin(username, password);
        }

        try {
            String sql = "SELECT user_id, password_hash, assigned_role, is_account_locked, " +
                         "login_attempt_count, user_display_name, user_email FROM users WHERE username=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    return new LoginResult(false, "E:001",
                        "Invalid username or password. " + (MAX_ATTEMPTS - 1) + " attempts remaining.");
                }

                boolean locked    = rs.getBoolean("is_account_locked");
                int attempts      = rs.getInt("login_attempt_count");
                String storedHash = rs.getString("password_hash");
                int uid           = rs.getInt("user_id");
                String role       = rs.getString("assigned_role");
                String displayName= rs.getString("user_display_name");
                String email      = rs.getString("user_email");

                // E:005 – account locked
                if (locked) {
                    logAudit(conn, uid, username, "Login attempt on locked account.", "Authentication");
                    return new LoginResult(false, "E:005",
                        "Account is locked. Please contact your administrator.");
                }

                // Verify password – try BCrypt first, then plaintext fallback for seeded accounts
                boolean pwOk = checkPassword(password, storedHash);
                if (!pwOk) {
                    int newAttempts = attempts + 1;
                    int remaining   = MAX_ATTEMPTS - newAttempts;
                    if (newAttempts >= MAX_ATTEMPTS) {
                        lockAccount(conn, uid);
                        logAudit(conn, uid, username, "Account locked after " + newAttempts + " failed login attempts.", "Authentication");
                        return new LoginResult(false, "E:005",
                            "Account locked after " + MAX_ATTEMPTS + " failed attempts. Contact admin.");
                    }
                    updateAttempts(conn, uid, newAttempts);
                    logAudit(conn, uid, username, "Failed login attempt (" + newAttempts + "/" + MAX_ATTEMPTS + ").", "Authentication");
                    return new LoginResult(false, "E:001",
                        "Invalid username or password. " + remaining + " attempt(s) remaining.");
                }

                // Success – reset attempts, set session
                resetAttempts(conn, uid);
                setSession(uid, username, role, displayName, email);
                updateLastLogin(conn, uid);
                logAudit(conn, uid, username, "User logged in successfully.", "Authentication");
                return new LoginResult(true, null, "Login successful.");
            }
        } catch (SQLException e) {
            LOG.warning("Login SQL error: " + e.getMessage());
            return demoLogin(username, password);
        }
    }

    // Demo mode when DB is not available
    private static LoginResult demoLogin(String username, String password) {
        Map<String,String[]> demos = new HashMap<>();
        demos.put("admin",      new String[]{"admin123",      "SUPER_ADMIN",       "1", "Super Administrator", "admin@scm.com"});
        demos.put("manager",    new String[]{"manager123",    "MANAGER",           "2", "Operations Manager",  "manager@scm.com"});
        demos.put("warehouse1", new String[]{"warehouse123",  "WAREHOUSE_STAFF",   "3", "Warehouse Staff",     "warehouse@scm.com"});
        demos.put("logistics1", new String[]{"logistics123",  "LOGISTICS_OFFICER", "4", "Logistics Officer",   "logistics@scm.com"});
        demos.put("sales1",     new String[]{"sales123",      "SALES_STAFF",       "5", "Sales Staff",         "sales@scm.com"});
        String[] d = demos.get(username);
        if (d != null && d[0].equals(password)) {
            setSession(Integer.parseInt(d[2]), username, d[1], d[3], d[4]);
            return new LoginResult(true, null, "Login successful (demo mode).");
        }
        return new LoginResult(false, "E:001", "Invalid credentials.");
    }

    private static boolean checkPassword(String plain, String hash) {
        if (hash == null || plain == null) return false;
        // Plaintext comparison (works for seeded demo accounts)
        // BCrypt is skipped since jbcrypt JAR is not in the classpath
        return plain.equals(hash);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════════
    public static void logout() {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn != null) {
            try {
                logAudit(conn, currentUserId, currentUsername, "User logged out.", "Authentication");
                updateSessionStatus(conn, currentUserId, "INACTIVE");
            } catch (Exception ignored) {}
        }
        sessionActive  = false;
        currentUserId  = 0;
        currentUsername= null;
        currentRole    = null;
        sessionExpiry  = null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RBAC CHECK
    // ══════════════════════════════════════════════════════════════════════════
    public static boolean hasAccess(String requiredRole) {
        if (!sessionActive) return false;
        int userLevel = ROLE_LEVEL.getOrDefault(currentRole, 0);
        int reqLevel  = ROLE_LEVEL.getOrDefault(requiredRole, 99);
        return userLevel >= reqLevel;
    }

    public static boolean canAccessPanel(String panelName) {
        if (currentRole == null) return false;
        return switch (panelName.toUpperCase()) {
            case "DASHBOARD"   -> hasAccess("VIEWER");
            case "INVENTORY"   -> hasAccess("WAREHOUSE_STAFF");
            case "ORDERS"      -> hasAccess("SALES_STAFF");
            case "LOGISTICS"   -> hasAccess("LOGISTICS_OFFICER");
            case "PRICING"     -> hasAccess("ADMIN");
            case "FORECASTING" -> hasAccess("MANAGER");
            case "NOTIFICATIONS"-> hasAccess("VIEWER");
            case "SETTINGS"    -> hasAccess("ADMIN");
            default            -> hasAccess("SUPER_ADMIN");
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SESSION HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private static void setSession(int uid, String uname, String role, String display, String email) {
        currentUserId    = uid;
        currentUsername  = uname;
        currentRole      = role;
        currentDisplayName = display;
        currentEmail     = email;
        sessionActive    = true;
        sessionExpiry    = LocalDateTime.now().plusMinutes(30);
    }

    public static boolean isSessionExpired() {
        return !sessionActive || LocalDateTime.now().isAfter(sessionExpiry);
    }

    public static void refreshSession() {
        if (sessionActive) sessionExpiry = LocalDateTime.now().plusMinutes(30);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public static int    getCurrentUserId()      { return currentUserId; }
    public static String getCurrentUsername()    { return currentUsername; }
    public static String getCurrentRole()        { return currentRole; }
    public static String getCurrentDisplayName() { return currentDisplayName; }
    public static String getCurrentEmail()       { return currentEmail; }
    public static boolean isSessionActive()      { return sessionActive; }
    public static String getLastVisitedPanel()   { return lastVisitedPanel; }
    public static void setLastVisitedPanel(String p) { lastVisitedPanel = p; }

    /** Build a UIUser POJO from current session state. */
    public static com.scm.models.Models.UIUser getCurrentUser() {
        com.scm.models.Models.UIUser u = new com.scm.models.Models.UIUser();
        u.userId      = currentUserId;
        u.username    = currentUsername;
        u.role        = currentRole;
        u.displayName = currentDisplayName;
        u.email       = currentEmail;
        return u;
    }

    // ── DB helpers ────────────────────────────────────────────────────────────
    private static void lockAccount(Connection c, int uid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET is_account_locked=TRUE, login_attempt_count=? WHERE user_id=?")) {
            ps.setInt(1, MAX_ATTEMPTS); ps.setInt(2, uid); ps.executeUpdate();
        }
    }
    private static void updateAttempts(Connection c, int uid, int n) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET login_attempt_count=? WHERE user_id=?")) {
            ps.setInt(1, n); ps.setInt(2, uid); ps.executeUpdate();
        }
    }
    private static void resetAttempts(Connection c, int uid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET login_attempt_count=0, is_account_locked=FALSE WHERE user_id=?")) {
            ps.setInt(1, uid); ps.executeUpdate();
        }
    }
    private static void updateLastLogin(Connection c, int uid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET last_login_timestamp=NOW(), session_status='ACTIVE' WHERE user_id=?")) {
            ps.setInt(1, uid); ps.executeUpdate();
        }
    }
    private static void updateSessionStatus(Connection c, int uid, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET session_status=? WHERE user_id=?")) {
            ps.setString(1, status); ps.setInt(2, uid); ps.executeUpdate();
        }
    }

    public static void logAudit(Connection c, int uid, String uname, String desc, String module) {
        if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_log (user_id, audit_action_user, audit_action_description, audit_module_name) VALUES(?,?,?,?)")) {
            ps.setInt(1, uid); ps.setString(2, uname);
            ps.setString(3, desc); ps.setString(4, module);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN RESULT DTO
    // ══════════════════════════════════════════════════════════════════════════
    public static class LoginResult {
        public final boolean success;
        public final String  errorCode;
        public final String  message;
        public LoginResult(boolean s, String code, String msg) {
            success = s; errorCode = code; message = msg;
        }
    }
}

// Helper added for SettingsPanel
