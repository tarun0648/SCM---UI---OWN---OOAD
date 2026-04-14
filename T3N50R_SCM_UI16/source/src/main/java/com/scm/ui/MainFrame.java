package com.scm.ui;

import com.scm.auth.AuthService;
import com.scm.auth.LoginScreen;
import com.scm.dashboard.DashboardPanel;
import com.scm.inventory.InventoryPanel;
import com.scm.orders.OrdersPanel;
import com.scm.logistics.LogisticsPanel;
import com.scm.pricing.PricingPanel;
import com.scm.forecast.ForecastPanel;
import com.scm.notifications.NotificationsPanel;
import com.scm.settings.SettingsPanel;
import com.scm.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.logging.Logger;

/**
 * C-02 – Navigation & Layout Controller
 * Main application shell: sidebar, header, content area, breadcrumb.
 * Handles panel routing, RBAC-filtered menu, notification badge.
 * Exception: INVALID_PANEL (E:005), MENU_LOAD_FAILURE (E:006)
 */
public class MainFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());

    // ── Panels (lazy init) ────────────────────────────────────────────────────
    private JPanel contentArea;
    private JPanel currentPanel;
    private JLabel breadcrumbLabel;
    private JLabel notifBadgeLabel;
    private JLabel connectionStatusLabel;
    private JLabel userInfoLabel;

    // ── Sidebar buttons ───────────────────────────────────────────────────────
    private static final String[][] MENU_ITEMS = {
        {SCMColors.ICON_DASH,     "Dashboard",    "DASHBOARD",   "VIEWER"},
        {SCMColors.ICON_INV,      "Inventory",    "INVENTORY",   "WAREHOUSE_STAFF"},
        {SCMColors.ICON_ORDER,    "Orders",       "ORDERS",      "SALES_STAFF"},
        {SCMColors.ICON_LOGIS,    "Logistics",    "LOGISTICS",   "LOGISTICS_OFFICER"},
        {SCMColors.ICON_PRICE,    "Pricing",      "PRICING",     "ADMIN"},
        {SCMColors.ICON_FORECAST, "Forecasting",  "FORECASTING", "MANAGER"},
        {SCMColors.ICON_NOTIF,    "Notifications","NOTIFICATIONS","VIEWER"},
        {SCMColors.ICON_SETTINGS, "Settings",     "SETTINGS",    "ADMIN"},
    };

    private JButton activeMenuBtn = null;
    private Timer notifRefreshTimer;

    public MainFrame() {
        initUI();
        navigateTo("DASHBOARD");
        startNotifRefresh();
    }

    private void initUI() {
        setTitle("SCM — Supply Chain Management | " + AuthService.getCurrentDisplayName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleClose(); }
        });
        setSize(1400, 860);
        setMinimumSize(new Dimension(1200, 720));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(SCMColors.BG_PRIMARY);

        // ── Sidebar ───────────────────────────────────────────────────────────
        JPanel sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // ── Right content area ────────────────────────────────────────────────
        JPanel rightSide = new JPanel(new BorderLayout(0, 0));
        rightSide.setBackground(SCMColors.BG_PRIMARY);

        // Header bar
        JPanel header = buildHeader();
        rightSide.add(header, BorderLayout.NORTH);

        // Content area
        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(SCMColors.BG_PRIMARY);
        contentArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightSide.add(contentArea, BorderLayout.CENTER);

        root.add(rightSide, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SIDEBAR (C-02)
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(SCMColors.BG_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Right border
                g2.setColor(SCMColors.BORDER_DEFAULT);
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                // Gold top strip
                GradientPaint gp = new GradientPaint(0, 0, SCMColors.ACCENT_COPPER,
                    getWidth(), 0, SCMColors.ACCENT_GOLD);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 3);
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);

        // Logo area
        JPanel logoArea = new JPanel();
        logoArea.setOpaque(false);
        logoArea.setLayout(new BoxLayout(logoArea, BoxLayout.Y_AXIS));
        logoArea.setBorder(BorderFactory.createEmptyBorder(24, 16, 16, 16));
        logoArea.setMaximumSize(new Dimension(220, 90));

        JLabel logo = new JLabel("SCM");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(SCMColors.ACCENT_GOLD);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoSub = new JLabel("Supply Chain Mgmt");
        logoSub.setFont(SCMColors.FONT_SMALL);
        logoSub.setForeground(SCMColors.TEXT_MUTED);
        logoSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        logoArea.add(logo);
        logoArea.add(Box.createVerticalStrut(2));
        logoArea.add(logoSub);
        sidebar.add(logoArea);

        // Separator
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(buildSidebarSep());
        sidebar.add(Box.createVerticalStrut(8));

        // Role label
        JLabel roleLabel = new JLabel("  " + AuthService.getCurrentRole().replace("_"," "));
        roleLabel.setFont(SCMColors.FONT_BADGE);
        roleLabel.setForeground(SCMColors.ACCENT_AMBER);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 0));
        sidebar.add(roleLabel);

        // Menu items
        for (String[] item : MENU_ITEMS) {
            String icon       = item[0];
            String label      = item[1];
            String panelId    = item[2];
            String minRole    = item[3];

            if (!AuthService.hasAccess(minRole)) continue; // RBAC filter

            JButton btn = buildMenuButton(icon, label, panelId);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildSidebarSep());

        // Connection status
        connectionStatusLabel = new JLabel("  ● DB Connected");
        connectionStatusLabel.setFont(SCMColors.FONT_BADGE);
        connectionStatusLabel.setForeground(SCMColors.STATUS_SUCCESS);
        connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 0, 0));
        sidebar.add(connectionStatusLabel);
        updateConnectionStatus();

        // User info at bottom
        userInfoLabel = new JLabel("  " + SCMColors.ICON_USER + "  " + AuthService.getCurrentUsername());
        userInfoLabel.setFont(SCMColors.FONT_SMALL);
        userInfoLabel.setForeground(SCMColors.TEXT_SECONDARY);
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 16, 0, 0));
        sidebar.add(userInfoLabel);

        // Logout button
        JButton logoutBtn = buildMenuButton(SCMColors.ICON_LOGOUT, "Logout", "LOGOUT");
        logoutBtn.setForeground(SCMColors.STATUS_ERROR);
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(12));

        return sidebar;
    }

    private JButton buildMenuButton(String icon, String label, String panelId) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = this == activeMenuBtn;
                if (isActive) {
                    g2.setColor(SCMColors.BG_SELECTED);
                    g2.fillRoundRect(4, 0, getWidth()-8, getHeight(), 8, 8);
                    // Gold left accent
                    g2.setColor(SCMColors.ACCENT_GOLD);
                    g2.fillRoundRect(0, 6, 4, getHeight()-12, 4, 4);
                } else if (getModel().isRollover()) {
                    g2.setColor(SCMColors.BG_HOVER);
                    g2.fillRoundRect(4, 0, getWidth()-8, getHeight(), 8, 8);
                }
                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                g2.setColor(isActive ? SCMColors.ACCENT_GOLD_LIGHT : SCMColors.TEXT_SECONDARY);
                g2.drawString(icon, 18, getHeight()/2 + 5);
                // Label
                g2.setFont(isActive ? SCMColors.FONT_SUBHEAD : SCMColors.FONT_BODY);
                g2.setColor(isActive ? SCMColors.TEXT_PRIMARY : SCMColors.TEXT_SECONDARY);
                g2.drawString(label, 44, getHeight()/2 + 5);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 40));
        btn.setMaximumSize(new Dimension(220, 40));
        btn.setMinimumSize(new Dimension(220, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn.addActionListener(e -> {
            if ("LOGOUT".equals(panelId)) {
                handleLogout();
            } else {
                navigateTo(panelId);
            }
        });
        return btn;
    }

    private JPanel buildSidebarSep() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(SCMColors.BORDER_DEFAULT);
                g.fillRect(12, 0, getWidth()-24, 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(220, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(220, 1); }
        };
        sep.setOpaque(false);
        return sep;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HEADER BAR
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(SCMColors.BG_SECONDARY);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(SCMColors.BORDER_DEFAULT);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 54));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 20));

        // Left: breadcrumb
        breadcrumbLabel = new JLabel("Home > Dashboard");
        breadcrumbLabel.setFont(SCMColors.FONT_SMALL);
        breadcrumbLabel.setForeground(SCMColors.TEXT_SECONDARY);
        header.add(breadcrumbLabel, BorderLayout.WEST);

        // Right: notifications + user
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);

        // Notification badge
        notifBadgeLabel = new JLabel(SCMColors.ICON_NOTIF + " 3") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFF,0x3E,0x55,40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        notifBadgeLabel.setFont(SCMColors.FONT_BODY);
        notifBadgeLabel.setForeground(SCMColors.STATUS_ERROR);
        notifBadgeLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        notifBadgeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notifBadgeLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navigateTo("NOTIFICATIONS"); }
        });

        // User display
        JLabel userLabel = new JLabel(SCMColors.ICON_USER + "  " + AuthService.getCurrentDisplayName()
            + "  [" + AuthService.getCurrentRole().replace("_"," ") + "]");
        userLabel.setFont(SCMColors.FONT_SMALL);
        userLabel.setForeground(SCMColors.TEXT_SECONDARY);

        // Logout quick button
        JButton quickLogout = new JButton("Logout");
        quickLogout.setFont(SCMColors.FONT_SMALL);
        quickLogout.setForeground(SCMColors.STATUS_ERROR);
        quickLogout.setBackground(new Color(0,0,0,0));
        quickLogout.setOpaque(false);
        quickLogout.setBorderPainted(false);
        quickLogout.setContentAreaFilled(false);
        quickLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        quickLogout.addActionListener(e -> handleLogout());

        rightHeader.add(notifBadgeLabel);
        rightHeader.add(new JSeparator(SwingConstants.VERTICAL));
        rightHeader.add(userLabel);
        rightHeader.add(quickLogout);
        header.add(rightHeader, BorderLayout.EAST);

        return header;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION ROUTING (C-02)
    // ══════════════════════════════════════════════════════════════════════════
    public void navigateTo(String panelId) {
        // Session expiry check – E:002
        if (AuthService.isSessionExpired()) {
            showExceptionModal("E:002 — Session Expired",
                "Your session has expired due to inactivity.\nPlease log in again.",
                SCMColors.STATUS_WARNING);
            handleLogout();
            return;
        }

        // RBAC check – E:003
        if (!AuthService.canAccessPanel(panelId)) {
            showExceptionModal("E:003 — Access Denied",
                "You do not have permission to access the '" + panelId + "' module.\n" +
                "Your role: " + AuthService.getCurrentRole() + "\n" +
                "This violation has been logged.",
                SCMColors.STATUS_ERROR);
            return;
        }

        AuthService.refreshSession();
        AuthService.setLastVisitedPanel(panelId);

        // Refresh UI on EDT
        SwingUtilities.invokeLater(() -> {
            JPanel panel = null;
            String breadcrumb = "Home > ";
            try {
                panel = switch (panelId) {
                    case "DASHBOARD"    -> { breadcrumb += "Dashboard";     yield new DashboardPanel(this); }
                    case "INVENTORY"    -> { breadcrumb += "Inventory";     yield new InventoryPanel(this); }
                    case "ORDERS"       -> { breadcrumb += "Orders";        yield new OrdersPanel(this); }
                    case "LOGISTICS"    -> { breadcrumb += "Logistics";     yield new LogisticsPanel(); }
                    case "PRICING"      -> { breadcrumb += "Pricing";       yield new PricingPanel(); }
                    case "FORECASTING"  -> { breadcrumb += "Forecasting";   yield new ForecastPanel(); }
                    case "NOTIFICATIONS"-> { breadcrumb += "Notifications"; yield new NotificationsPanel(); }
                    case "SETTINGS"     -> { breadcrumb += "Settings";      yield new SettingsPanel(AuthService.getCurrentUser()); }
                    default -> {
                        // E:005 – Invalid panel
                        showExceptionModal("404 — Panel Not Found",
                            "The requested panel '" + panelId + "' does not correspond\nto any registered UI route.",
                            SCMColors.STATUS_WARNING);
                        yield null;
                    }
                };
            } catch (Exception ex) {
                LOG.severe("Panel load error for " + panelId + ": " + ex.getMessage());
                showExceptionModal("Panel Load Error",
                    "Failed to load panel: " + panelId + "\nError: " + ex.getMessage(),
                    SCMColors.STATUS_ERROR);
            }

            if (panel != null) {
                contentArea.removeAll();
                contentArea.add(panel, BorderLayout.CENTER);
                contentArea.revalidate();
                contentArea.repaint();
                breadcrumbLabel.setText(breadcrumb);
                currentPanel = panel;
                updateActiveMenu(panelId);
            }
        });
    }

    private void updateActiveMenu(String panelId) {
        // Rebuild sidebar buttons highlight by finding matching button
        // We track via component names
        for (Component c : ((JPanel)getContentPane().getComponent(0)).getComponents()) {
            // sidebar is index 0
        }
        // The simplest approach: repaint sidebar
        getContentPane().repaint();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS BADGE REFRESH
    // ══════════════════════════════════════════════════════════════════════════
    private void startNotifRefresh() {
        notifRefreshTimer = new Timer(60_000, e -> refreshNotifBadge());
        notifRefreshTimer.start();
        refreshNotifBadge();
    }

    private void refreshNotifBadge() {
        SwingWorker<Integer,Void> w = new SwingWorker<>() {
            @Override protected Integer doInBackground() {
                try {
                    Connection conn = DatabaseConnection.getInstance().getConnection();
                    if (conn == null) return 3;
                    String sql = "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=FALSE";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, AuthService.getCurrentUserId());
                        ResultSet rs = ps.executeQuery();
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                } catch (Exception e) {
                    return 0;
                }
            }
            @Override protected void done() {
                try {
                    int count = get();
                    if (count > 0) {
                        notifBadgeLabel.setText(SCMColors.ICON_NOTIF + " " + count);
                        notifBadgeLabel.setForeground(SCMColors.STATUS_ERROR);
                    } else {
                        notifBadgeLabel.setText(SCMColors.ICON_NOTIF);
                        notifBadgeLabel.setForeground(SCMColors.TEXT_SECONDARY);
                    }
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    public void showToast(String message, Color color) {
        SCMComponents.showToast(this, message, color);
    }

    public void showExceptionModal(String title, String message, Color borderColor) {
        JDialog dialog = new JDialog(this, title, true);
        JPanel panel = new JPanel(new BorderLayout(0, 16)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(SCMColors.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(SCMColors.FONT_HEADING);
        titleLbl.setForeground(borderColor);

        JTextArea msgArea = new JTextArea(message);
        msgArea.setFont(SCMColors.FONT_BODY);
        msgArea.setForeground(SCMColors.TEXT_PRIMARY);
        msgArea.setBackground(SCMColors.BG_CARD);
        msgArea.setEditable(false);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);

        JButton close = SCMComponents.goldButton("Dismiss");
        close.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(close);

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(msgArea, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setSize(440, 240);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateConnectionStatus() {
        boolean connected = DatabaseConnection.getInstance().isConnected();
        if (connected) {
            connectionStatusLabel.setText("  ● DB Connected");
            connectionStatusLabel.setForeground(SCMColors.STATUS_SUCCESS);
        } else {
            connectionStatusLabel.setText("  ● Demo Mode");
            connectionStatusLabel.setForeground(SCMColors.STATUS_WARNING);
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (notifRefreshTimer != null) notifRefreshTimer.stop();
            AuthService.logout();
            dispose();
            // Restart login screen
            SwingUtilities.invokeLater(() -> {
                LoginScreen login = new LoginScreen();
                login.setVisible(true);
            });
        }
    }

    private void handleClose() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Exit SCM Application?", "Confirm Exit",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (notifRefreshTimer != null) notifRefreshTimer.stop();
            AuthService.logout();
            DatabaseConnection.getInstance().close();
            System.exit(0);
        }
    }
}
