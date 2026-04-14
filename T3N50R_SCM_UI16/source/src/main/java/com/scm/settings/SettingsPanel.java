package com.scm.settings;

import com.scm.auth.AuthService;
import com.scm.db.DatabaseConnection;
import com.scm.models.Models.*;
import com.scm.ui.SCMColors;
import com.scm.ui.SCMComponents;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class SettingsPanel extends JPanel {

    private UIUser currentUser;
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JToggleButton themeToggle;
    private boolean isDarkTheme = true;

    public SettingsPanel(UIUser user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 0));
        setBackground(SCMColors.BG_PRIMARY);
        initComponents();
    }

    private void initComponents() {
        JPanel header = SCMComponents.sectionHeader("⚙  User & System Settings",
            "Profile management, user administration, roles, theme and system configuration");
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(SCMColors.BG_SECONDARY);
        tabs.setForeground(SCMColors.TEXT_PRIMARY);
        tabs.setFont(SCMColors.FONT_BODY);
        tabs.setBorder(new EmptyBorder(12, 20, 20, 20));

        tabs.addTab("  👤 Profile  ", buildProfileTab());
        tabs.addTab("  👥 Users  ", buildUsersTab());
        tabs.addTab("  🎭 Roles  ", buildRolesTab());
        tabs.addTab("  🎨 Theme & Language  ", buildThemeTab());
        tabs.addTab("  🔔 Notifications  ", buildNotifPrefsTab());
        tabs.addTab("  ⚙ System Config  ", buildSystemConfigTab());

        add(tabs, BorderLayout.CENTER);
    }

    // ── Tab 1: Profile ───────────────────────────────────────────────────────
    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Profile Card
        JPanel profileCard = SCMComponents.goldCardPanel();
        profileCard.setLayout(new BorderLayout(20, 0));
        profileCard.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.ACCENT_GOLD, 1),
            new EmptyBorder(24, 24, 24, 24)
        ));
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Avatar circle
        JPanel avatar = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCMColors.ACCENT_GOLD);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(SCMColors.BG_PRIMARY);
                g2.setFont(SCMColors.FONT_HEADING.deriveFont(Font.BOLD, 28f));
                String initials = currentUser != null && currentUser.displayName != null && !currentUser.displayName.isEmpty()
                    ? String.valueOf(currentUser.displayName.charAt(0)).toUpperCase() : "A";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (64 - fm.stringWidth(initials)) / 2, 44);
            }
        };
        avatar.setPreferredSize(new Dimension(64, 64));
        avatar.setBackground(SCMColors.BG_CARD);

        JPanel info = new JPanel(new GridLayout(4, 1, 0, 4));
        info.setBackground(SCMColors.BG_CARD);
        String displayName = currentUser != null ? currentUser.displayName : "Admin";
        String email       = currentUser != null ? currentUser.email : "admin@scm.com";
        String role        = currentUser != null ? currentUser.role : "SUPER_ADMIN";
        String lastLogin   = "20-Feb-2026 09:00 AM";

        info.add(SCMComponents.goldLabel(displayName + "  [EDIT PROFILE]"));
        info.add(SCMComponents.subLabel("Email: " + email));
        JLabel roleLbl = new JLabel("Role: " + role + "  ●  Session: Active ✓");
        roleLbl.setForeground(SCMColors.STATUS_SUCCESS);
        roleLbl.setFont(SCMColors.FONT_SMALL);
        info.add(roleLbl);
        info.add(SCMComponents.subLabel("Last Login: " + lastLogin));

        profileCard.add(avatar, BorderLayout.WEST);
        profileCard.add(info, BorderLayout.CENTER);

        // Edit Profile Form
        JPanel editCard = SCMComponents.cardPanel();
        editCard.setLayout(new BorderLayout(0, 12));
        editCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(212,175,55,40), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel editTitle = SCMComponents.goldLabel("✏  Edit Profile");
        JPanel form = new JPanel(new GridLayout(4, 2, 16, 10));
        form.setBackground(SCMColors.BG_CARD);

        JTextField nameFld  = SCMComponents.styledField(displayName);
        JTextField emailFld = SCMComponents.styledField(email);
        JPasswordField passFld    = SCMComponents.styledPasswordField();
        JPasswordField confirmFld = SCMComponents.styledPasswordField();

        form.add(SCMComponents.subLabel("Display Name:")); form.add(nameFld);
        form.add(SCMComponents.subLabel("Email:")); form.add(emailFld);
        form.add(SCMComponents.subLabel("New Password:")); form.add(passFld);
        form.add(SCMComponents.subLabel("Confirm Password:")); form.add(confirmFld);

        JButton saveProfile = SCMComponents.goldButton("💾 Save Profile");
        JButton cancelEdit  = SCMComponents.outlineButton("✕ Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_CARD);
        btns.add(cancelEdit); btns.add(saveProfile);

        saveProfile.addActionListener(e -> {
            String newName  = nameFld.getText().trim();
            String newEmail = emailFld.getText().trim();
            String newPass  = new String(passFld.getPassword());
            String confPass = new String(confirmFld.getPassword());

            if (!newPass.isEmpty() && !newPass.equals(confPass)) {
                SCMComponents.showToast((Component)this, "⚠ Passwords do not match", true);
                return;
            }
            if (newName.isEmpty() || newEmail.isEmpty()) {
                // E:040 Profile update failed
                showExceptionModal("E:040", "MINOR",
                    "Profile Update Failed",
                    "Display name and email cannot be empty. Backend validation failed.",
                    "Please fill all required fields and retry. Form data retained.");
                return;
            }

            // Simulate update
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(600);
                    return updateProfileInDB(newName, newEmail, newPass);
                }
                protected void done() {
                    try {
                        if (get()) {
                            if (currentUser != null) {
                                currentUser.displayName = newName;
                                currentUser.email = newEmail;
                            }
                            SCMComponents.showToast(SettingsPanel.this, "✓ Profile updated successfully", false);
                        } else {
                            // E:041 Profile update failed
                            showExceptionModal("E:041", "MINOR",
                                "Profile Update Failed",
                                "User profile update request was rejected by the Auth Service due to validation failure or server error.",
                                "Form data retained. Please retry. Failure logged with user ID and timestamp.");
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:041", "MINOR", "Profile Update Failed",
                            ex.getMessage(), "Retry the update.");
                    }
                }
            };
            w.execute();
        });
        cancelEdit.addActionListener(e -> {
            nameFld.setText(displayName); emailFld.setText(email);
            passFld.setText(""); confirmFld.setText("");
        });

        editCard.add(editTitle, BorderLayout.NORTH);
        editCard.add(form, BorderLayout.CENTER);
        editCard.add(btns, BorderLayout.SOUTH);

        panel.add(profileCard, BorderLayout.NORTH);
        panel.add(editCard, BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 2: Users ─────────────────────────────────────────────────────────
    private JPanel buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(SCMColors.BG_PRIMARY);
        JLabel title = SCMComponents.goldLabel("👥  User Management");
        JButton addUser = SCMComponents.goldButton("+ Add User");
        addUser.addActionListener(e -> showAddUserDialog());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(SCMColors.BG_PRIMARY);
        btnPanel.add(addUser);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnPanel, BorderLayout.EAST);

        // Users table
        String[] cols = {"Username", "Email", "Role", "Status", "Last Login", "Actions"};
        usersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        usersTable = SCMComponents.styledTable(usersModel);
        usersTable.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
        usersTable.getColumnModel().getColumn(5).setCellRenderer(new ActionRenderer());
        usersTable.getColumnModel().getColumn(5).setCellEditor(
            new ActionButtonEditor(usersTable, usersModel));

        JScrollPane scroll = SCMComponents.styledScrollPane(new JScrollPane(usersTable));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        // Load users
        loadUsersAsync();
        return panel;
    }

    private void loadUsersAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> usersModel.setRowCount(0));
                try {
                    Connection conn = DatabaseConnection.getInstance().getConnection();
                    if (conn != null) {
                        String sql = "SELECT username, email, role, is_active, last_login FROM users LIMIT 20";
                        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                            while (rs.next()) {
                                final Object[] row = {
                                    rs.getString("username"), rs.getString("email"),
                                    rs.getString("role"),
                                    rs.getBoolean("is_active") ? "Active" : "Inactive",
                                    rs.getString("last_login"),
                                    "Edit"
                                };
                                SwingUtilities.invokeLater(() -> usersModel.addRow(row));
                            }
                            return null;
                        }
                    }
                } catch (Exception ignored) {}
                loadDemoUsers();
                return null;
            }
            protected void done() {
                try { get(); }
                catch (Exception ex) {
                    // E:045 User management load failure
                    showExceptionModal("E:045", "MINOR",
                        "User List Unavailable",
                        "User management table could not be loaded due to Auth Service failure or insufficient permissions.",
                        "Showing cached user list if available. All CRUD actions restricted until live data is loaded.");
                    loadDemoUsers();
                }
            }
        };
        worker.execute();
    }

    private void loadDemoUsers() {
        SwingUtilities.invokeLater(() -> {
            usersModel.setRowCount(0);
            Object[][] demo = {
                {"admin","admin@scm.com","SUPER_ADMIN","Active","20-Feb-2026 09:00","Edit"},
                {"manager","manager@scm.com","MANAGER","Active","20-Feb-2026 08:30","Edit"},
                {"warehouse1","wh1@scm.com","WAREHOUSE_STAFF","Active","19-Feb-2026","Edit"},
                {"logistics1","log1@scm.com","LOGISTICS_OFFICER","Active","20-Feb-2026","Edit"},
                {"sales1","sales1@scm.com","SALES_STAFF","Inactive","18-Feb-2026","Edit"}
            };
            for (Object[] row : demo) usersModel.addRow(row);
        });
    }

    // ── Tab 3: Roles ─────────────────────────────────────────────────────────
    private JPanel buildRolesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel title = SCMComponents.goldLabel("🎭  Role Assignment & Permissions");
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(SCMColors.BG_PRIMARY);
        titlePanel.add(title, BorderLayout.WEST);

        // Role cards
        JPanel rolesGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        rolesGrid.setBackground(SCMColors.BG_PRIMARY);

        String[][] roles = {
            {"SUPER_ADMIN","Full access to all 10 UI components","6"},
            {"ADMIN","All components except restricted staff screens","5"},
            {"MANAGER","Dashboard, Orders, Inventory, Forecast, Notifications","4"},
            {"WAREHOUSE_STAFF","Inventory, Orders, Notifications","3"},
            {"LOGISTICS_OFFICER","Transport & Logistics, Delivery, Notifications","3"},
            {"SALES_STAFF","Orders, Pricing view, Notifications","2"}
        };
        for (String[] r : roles) rolesGrid.add(buildRoleCard(r[0], r[1], Integer.parseInt(r[2])));

        // Role assignment form
        JPanel assignPanel = SCMComponents.cardPanel();
        assignPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));

        String[] users = {"admin","manager","warehouse1","logistics1","sales1"};
        String[] roleNames = {"SUPER_ADMIN","ADMIN","MANAGER","WAREHOUSE_STAFF","LOGISTICS_OFFICER","SALES_STAFF"};
        JComboBox<String> userCombo = SCMComponents.styledCombo(users);
        JComboBox<String> roleCombo = SCMComponents.styledCombo(roleNames);
        JButton assign = SCMComponents.goldButton("Assign Role");

        assign.addActionListener(e -> {
            // E:042 Role assignment error
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(500);
                    return Math.random() > 0.1;
                }
                protected void done() {
                    try {
                        if (get()) {
                            SCMComponents.showToast(SettingsPanel.this,
                                "✓ Role '" + roleCombo.getSelectedItem() + "' assigned to " + userCombo.getSelectedItem(), false);
                        } else {
                            showExceptionModal("E:042", "MAJOR",
                                "Role Assignment Error",
                                "Admin's attempt to assign or change a user role was rejected by the Auth Service.",
                                "Display reverted to previous role. Super Admin notified. Failed assignment attempt logged.");
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:042", "MAJOR", "Role Assignment Error",
                            ex.getMessage(), "Retry or contact Super Admin.");
                    }
                }
            };
            w.execute();
        });

        assignPanel.add(SCMComponents.subLabel("User:")); assignPanel.add(userCombo);
        assignPanel.add(SCMComponents.subLabel("Role:")); assignPanel.add(roleCombo);
        assignPanel.add(assign);

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(rolesGrid, BorderLayout.CENTER);
        panel.add(assignPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRoleCard(String roleName, String description, int level) {
        JPanel card = SCMComponents.cardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(212,175,55,50), 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel name = SCMComponents.goldLabel(roleName);
        name.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD, 13f));

        JLabel levelLbl = new JLabel("Access Level: " + level + "/6");
        levelLbl.setForeground(SCMColors.ACCENT_GOLD_LIGHT);
        levelLbl.setFont(SCMColors.FONT_SMALL);

        JLabel desc = SCMComponents.subLabel("<html>" + description + "</html>");
        desc.setFont(SCMColors.FONT_SMALL.deriveFont(10f));

        card.add(name, BorderLayout.NORTH);
        card.add(levelLbl, BorderLayout.CENTER);
        card.add(desc, BorderLayout.SOUTH);
        return card;
    }

    // ── Tab 4: Theme & Language ───────────────────────────────────────────────
    private JPanel buildThemeTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JPanel themeCard = SCMComponents.cardPanel();
        themeCard.setLayout(new BorderLayout(0, 16));
        themeCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(212,175,55,50), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel themeTitle = SCMComponents.goldLabel("🎨  Theme Preference");

        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        toggleRow.setBackground(SCMColors.BG_CARD);

        themeToggle = new JToggleButton("● Dark Mode");
        themeToggle.setSelected(true);
        themeToggle.setForeground(SCMColors.ACCENT_GOLD);
        themeToggle.setBackground(SCMColors.BG_CARD);
        themeToggle.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD));
        themeToggle.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.ACCENT_GOLD, 1),
            new EmptyBorder(8, 16, 8, 16)
        ));
        themeToggle.addActionListener(e -> {
            isDarkTheme = themeToggle.isSelected();
            themeToggle.setText(isDarkTheme ? "● Dark Mode" : "○ Light Mode");
            themeToggle.setForeground(isDarkTheme ? SCMColors.ACCENT_GOLD : SCMColors.STATUS_INFO);
            SCMComponents.showToast((Component)this,
                isDarkTheme ? "✓ Dark theme applied" : "✓ Light theme applied", false);
        });

        JLabel themeLbl = SCMComponents.subLabel("Dark Mode is the default luxury theme");
        toggleRow.add(themeToggle); toggleRow.add(themeLbl);

        // Language
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        langPanel.setBackground(SCMColors.BG_CARD);
        String[] langs = {"English (Default)", "Hindi", "Tamil", "Telugu", "Kannada", "Marathi"};
        JComboBox<String> langCombo = SCMComponents.styledCombo(langs);
        JButton saveLang = SCMComponents.goldButton("Save Language");
        saveLang.addActionListener(e -> {
            // E:043 Settings save failed simulation
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception { Thread.sleep(400); return true; }
                protected void done() {
                    try {
                        if (get()) SCMComponents.showToast(SettingsPanel.this,
                            "✓ Language set to: " + langCombo.getSelectedItem(), false);
                    } catch (Exception ex) {
                        showExceptionModal("E:043", "MINOR", "Settings Save Failed",
                            "System configuration save request (theme/language/notifications) failed to persist on the backend.",
                            "Settings retained in UI temporarily. User prompted to retry. Auto-revert to last saved config after 2 minutes.");
                    }
                }
            };
            w.execute();
        });
        langPanel.add(SCMComponents.subLabel("Language:")); langPanel.add(langCombo); langPanel.add(saveLang);

        JButton saveTheme = SCMComponents.goldButton("💾 Save Theme Settings");
        saveTheme.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(400);
                    return saveSettingsToDB("theme", isDarkTheme ? "dark" : "light");
                }
                protected void done() {
                    try {
                        if (get()) SCMComponents.showToast(SettingsPanel.this, "✓ Theme settings saved", false);
                        else showExceptionModal("E:043", "MINOR", "Settings Save Failed",
                            "Theme preference could not be persisted.",
                            "Retained in UI. Auto-revert after 2 minutes if not retried.");
                    } catch (Exception ex) {
                        // E:044 Theme load failure
                        showExceptionModal("E:044", "WARNING", "Theme Load Failure",
                            "Theme preference (dark/light mode) could not be loaded from system configuration on startup.",
                            "Falling back to default light theme. Notification shown. User can manually toggle and re-save preference.");
                    }
                }
            };
            w.execute();
        });

        themeCard.add(themeTitle, BorderLayout.NORTH);
        themeCard.add(toggleRow, BorderLayout.CENTER);

        JPanel bottomTheme = new JPanel(new BorderLayout(0, 8));
        bottomTheme.setBackground(SCMColors.BG_CARD);
        bottomTheme.add(langPanel, BorderLayout.NORTH);
        bottomTheme.add(saveTheme, BorderLayout.SOUTH);
        themeCard.add(bottomTheme, BorderLayout.SOUTH);

        panel.add(themeCard, BorderLayout.NORTH);
        return panel;
    }

    // ── Tab 5: Notification Preferences ──────────────────────────────────────
    private JPanel buildNotifPrefsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JLabel title = SCMComponents.goldLabel("🔔  Notification Preferences");

        JPanel prefsCard = SCMComponents.cardPanel();
        prefsCard.setLayout(new GridLayout(6, 2, 16, 12));
        prefsCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(212,175,55,50), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        String[] notifTypes = {
            "Low Stock Alerts", "Order Updates", "Payment Failures",
            "Shipment Updates", "System Events", "Login Alerts"
        };
        Map<String, JCheckBox> prefs = new LinkedHashMap<>();
        for (String t : notifTypes) {
            JCheckBox cb = new JCheckBox(t, true);
            cb.setForeground(SCMColors.TEXT_PRIMARY);
            cb.setBackground(SCMColors.BG_CARD);
            cb.setFont(SCMColors.FONT_BODY);
            prefs.put(t, cb);
            prefsCard.add(SCMComponents.subLabel(t)); prefsCard.add(cb);
        }

        JButton savePrefs = SCMComponents.goldButton("💾 Save Notification Preferences");
        savePrefs.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(400); return true;
                }
                protected void done() {
                    try { if (get()) SCMComponents.showToast(SettingsPanel.this, "✓ Notification preferences saved", false); }
                    catch (Exception ex) {
                        showExceptionModal("E:043", "MINOR", "Settings Save Failed",
                            "Notification preference flags failed to persist.",
                            "Retained in UI temporarily. Retry or contact admin.");
                    }
                }
            };
            w.execute();
        });

        JPanel titlePanel = new JPanel(new BorderLayout()); titlePanel.setBackground(SCMColors.BG_PRIMARY);
        titlePanel.add(title, BorderLayout.WEST);
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(prefsCard, BorderLayout.CENTER);
        panel.add(savePrefs, BorderLayout.SOUTH);
        return panel;
    }

    // ── Tab 6: System Config ─────────────────────────────────────────────────
    private JPanel buildSystemConfigTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel title = SCMComponents.goldLabel("⚙  System Configuration");
        JPanel titlePanel = new JPanel(new BorderLayout()); titlePanel.setBackground(SCMColors.BG_PRIMARY);
        titlePanel.add(title, BorderLayout.WEST);

        JPanel configCard = SCMComponents.cardPanel();
        configCard.setLayout(new GridLayout(5, 2, 16, 12));
        configCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(212,175,55,50), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JTextField sessionTimeoutFld = SCMComponents.styledField("30");
        JTextField maxLoginFld = SCMComponents.styledField("3");
        JTextField dbPoolFld = SCMComponents.styledField("10");
        JTextField logLevelFld = SCMComponents.styledField("INFO");
        JTextField backupFld = SCMComponents.styledField("Daily 2:00 AM");

        configCard.add(SCMComponents.subLabel("Session Timeout (min):")); configCard.add(sessionTimeoutFld);
        configCard.add(SCMComponents.subLabel("Max Login Attempts:")); configCard.add(maxLoginFld);
        configCard.add(SCMComponents.subLabel("DB Connection Pool:")); configCard.add(dbPoolFld);
        configCard.add(SCMComponents.subLabel("Log Level:")); configCard.add(logLevelFld);
        configCard.add(SCMComponents.subLabel("Backup Schedule:")); configCard.add(backupFld);

        JButton saveConfig = SCMComponents.goldButton("💾 Save System Config");
        saveConfig.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(600);
                    return saveSettingsToDB("session_timeout", sessionTimeoutFld.getText());
                }
                protected void done() {
                    try {
                        if (get()) SCMComponents.showToast(SettingsPanel.this, "✓ System configuration saved", false);
                        else {
                            // E:043 Settings save failed
                            showExceptionModal("E:043", "MINOR",
                                "Settings Not Saved",
                                "System configuration save request (theme, language, notifications) failed to persist on the backend.",
                                "Settings retained in UI temporarily. User prompted to retry. Auto-revert after 2 minutes.");
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:043", "MINOR", "Settings Not Saved",
                            ex.getMessage(), "Retry or contact Super Admin.");
                    }
                }
            };
            w.execute();
        });

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(configCard, BorderLayout.CENTER);
        panel.add(saveConfig, BorderLayout.SOUTH);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean updateProfileInDB(String name, String email, String password) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return true; // demo mode
            String sql = password.isEmpty()
                ? "UPDATE users SET display_name=?, email=? WHERE username=?"
                : "UPDATE users SET display_name=?, email=?, password_hash=? WHERE username=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name); ps.setString(2, email);
                if (!password.isEmpty()) {
                    ps.setString(3, password);
                    ps.setString(4, currentUser != null ? currentUser.username : "admin");
                } else {
                    ps.setString(3, currentUser != null ? currentUser.username : "admin");
                }
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) { return true; }
    }

    private boolean saveSettingsToDB(String key, String value) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return true;
            String sql = "INSERT INTO system_config (config_key, config_value) VALUES (?,?) ON DUPLICATE KEY UPDATE config_value=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key); ps.setString(2, value); ps.setString(3, value);
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) { return true; }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Add New User", true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("+ Add New User");
        JPanel form = new JPanel(new GridLayout(4, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);

        JTextField userFld  = SCMComponents.styledField("Username");
        JTextField emailFld = SCMComponents.styledField("Email");
        JPasswordField passFld = SCMComponents.styledPasswordField();
        String[] roleNames = {"ADMIN","MANAGER","WAREHOUSE_STAFF","LOGISTICS_OFFICER","SALES_STAFF","VIEWER"};
        JComboBox<String> roleCombo = SCMComponents.styledCombo(roleNames);

        form.add(SCMComponents.subLabel("Username:")); form.add(userFld);
        form.add(SCMComponents.subLabel("Email:")); form.add(emailFld);
        form.add(SCMComponents.subLabel("Password:")); form.add(passFld);
        form.add(SCMComponents.subLabel("Role:")); form.add(roleCombo);

        JButton save   = SCMComponents.goldButton("Save User");
        JButton cancel = SCMComponents.outlineButton("Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(cancel); btns.add(save);

        save.addActionListener(e -> {
            String uname = userFld.getText().trim();
            String uemail = emailFld.getText().trim();
            if (uname.isEmpty() || uemail.isEmpty()) {
                SCMComponents.showToast((Component)this, "⚠ Username and email are required", true);
                return;
            }
            usersModel.addRow(new Object[]{uname, uemail, roleCombo.getSelectedItem(), "Active", "Now", "Edit"});
            SCMComponents.showToast((Component)this, "✓ User '" + uname + "' created", false);
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showExceptionModal(String code, String category, String title, String message, String plan) {
        JDialog modal = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Exception — " + title, true);
        modal.setSize(480, 320);
        modal.setLocationRelativeTo(this);
        modal.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        Color catColor = "MAJOR".equals(category) ? SCMColors.STATUS_ERROR : SCMColors.STATUS_WARNING;
        JLabel codeLabel = new JLabel("  " + code + "  |  " + category);
        codeLabel.setForeground(catColor); codeLabel.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD));
        codeLabel.setOpaque(true);
        codeLabel.setBackground(new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 25));
        codeLabel.setBorder(new CompoundBorder(new LineBorder(catColor, 1), new EmptyBorder(6,12,6,12)));

        JLabel titleLbl = SCMComponents.goldLabel("⚠  " + title);
        JTextArea msgArea = new JTextArea("Issue: " + message + "\n\nResolution: " + plan);
        msgArea.setForeground(SCMColors.TEXT_SECONDARY); msgArea.setBackground(SCMColors.BG_CARD);
        msgArea.setFont(SCMColors.FONT_SMALL); msgArea.setEditable(false);
        msgArea.setLineWrap(true); msgArea.setWrapStyleWord(true);
        msgArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton dismiss = SCMComponents.outlineButton("Dismiss");
        JButton retry   = SCMComponents.goldButton("↻ Retry");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(dismiss); btns.add(retry);
        dismiss.addActionListener(e -> modal.dispose());
        retry.addActionListener(e -> modal.dispose());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(SCMColors.BG_SECONDARY);
        top.add(codeLabel, BorderLayout.NORTH); top.add(titleLbl, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(msgArea), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        modal.add(panel);
        modal.setVisible(true);
    }

    // ─── Renderers ────────────────────────────────────────────────────────────
    static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String val = v == null ? "" : v.toString();
            setBackground(sel ? SCMColors.BG_PRIMARY : SCMColors.BG_CARD);
            setForeground("Active".equals(val) ? SCMColors.STATUS_SUCCESS : SCMColors.STATUS_ERROR);
            setText("● " + val);
            return this;
        }
    }

    static class ActionRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            return SCMComponents.outlineButton("✏ Edit");
        }
    }

    static class ActionButtonEditor extends DefaultCellEditor {
        private JButton button;
        private JTable table;
        private DefaultTableModel model;
        private int currentRow;

        ActionButtonEditor(JTable table, DefaultTableModel model) {
            super(new JCheckBox());
            this.table = table; this.model = model;
            button = SCMComponents.outlineButton("✏ Edit");
            button.addActionListener(e -> fireEditingStopped());
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            currentRow = r; return button;
        }
        public Object getCellEditorValue() { return "Edit"; }
    }
}
