package com.scm.notifications;

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
import java.util.List;

public class NotificationsPanel extends JPanel {

    private JPanel notifContainer;
    private JTable auditTable;
    private DefaultTableModel auditModel;
    private JTextField auditSearchField;
    private JComboBox<String> typeFilter;
    private JLabel notifUnavailableBadge;
    private List<UINotification> notifications = new ArrayList<>();
    private List<AuditLog> auditLogs = new ArrayList<>();
    private java.util.Timer autoRetryTimer;
    private int retryCount = 0;

    public NotificationsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(SCMColors.BG_PRIMARY);
        initComponents();
        loadDataAsync();
        startAutoRetry();
    }

    private void initComponents() {
        JPanel header = SCMComponents.sectionHeader("🔔  Notifications & Exception Handling",
            "Real-time alerts, error logs, audit trail and exception reports");
        add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(SCMColors.BG_PRIMARY);
        split.setBorder(null);
        split.setDividerSize(4);
        split.setDividerLocation(420);

        // LEFT — notifications feed
        JPanel leftPanel = buildNotificationsPanel();
        // RIGHT — audit log
        JPanel rightPanel = buildAuditLogPanel();

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildNotificationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 20, 16, 8));

        // Title + unavailable badge
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setBackground(SCMColors.BG_PRIMARY);
        JLabel title = SCMComponents.goldLabel("📬  Live Notifications");

        notifUnavailableBadge = new JLabel("  ⚠ Notifications Unavailable  ");
        notifUnavailableBadge.setForeground(SCMColors.STATUS_ERROR);
        notifUnavailableBadge.setFont(SCMColors.FONT_SMALL);
        notifUnavailableBadge.setOpaque(true);
        notifUnavailableBadge.setBackground(new Color(255, 62, 85, 20));
        notifUnavailableBadge.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.STATUS_ERROR, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        notifUnavailableBadge.setVisible(false);

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(notifUnavailableBadge, BorderLayout.EAST);

        // Type filter
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setBackground(SCMColors.BG_PRIMARY);
        String[] types = {"All Types", "INFO", "WARNING", "ERROR", "SUCCESS"};
        typeFilter = SCMComponents.styledCombo(types);
        typeFilter.addActionListener(e -> filterNotifications());
        JButton markAllRead = SCMComponents.outlineButton("✓ Mark All Read");
        markAllRead.addActionListener(e -> markAllRead());
        filterRow.add(SCMComponents.subLabel("Filter:")); filterRow.add(typeFilter);
        filterRow.add(markAllRead);

        // Notifications container
        notifContainer = new JPanel();
        notifContainer.setLayout(new BoxLayout(notifContainer, BoxLayout.Y_AXIS));
        notifContainer.setBackground(SCMColors.BG_PRIMARY);

        JScrollPane scroll = new JScrollPane(notifContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(SCMColors.BG_PRIMARY);
        SCMComponents.styledScrollPane(scroll);

        // Exception Modal Demo button
        JButton demoException = SCMComponents.outlineButton("🔴 Test Exception Modal");
        demoException.addActionListener(e -> showExceptionModal(
            "E:017", "MAJOR", "Payment Gateway Timeout",
            "Connection to the payment/order gateway timed out (HTTP 504) during order transaction processing.",
            "Retry up to 3 times. On all failures, mark order as PENDING and notify Admin via Notification UI.",
            "Orders", "ORD-4823"));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setBackground(SCMColors.BG_PRIMARY);
        btnRow.add(demoException);

        panel.add(titleRow, BorderLayout.NORTH);
        JPanel topControls = new JPanel(new BorderLayout(0, 8));
        topControls.setBackground(SCMColors.BG_PRIMARY);
        topControls.add(filterRow, BorderLayout.NORTH);
        topControls.add(btnRow, BorderLayout.SOUTH);
        topControls.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBackground(SCMColors.BG_PRIMARY);
        centerPanel.add(topControls, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAuditLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 8, 16, 20));

        // Title + export
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(SCMColors.BG_PRIMARY);
        JLabel title = SCMComponents.goldLabel("📋  Audit Log");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(SCMColors.BG_PRIMARY);
        JButton exportReport = SCMComponents.outlineButton("📄 Export Report");
        JButton refreshLog   = SCMComponents.outlineButton("↻ Refresh");
        btnPanel.add(refreshLog); btnPanel.add(exportReport);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(btnPanel, BorderLayout.EAST);

        // Search bar
        auditSearchField = SCMComponents.styledField("Search audit log...");
        auditSearchField.addActionListener(e -> filterAuditLog(auditSearchField.getText()));

        // Audit table
        String[] cols = {"Timestamp", "User", "Action", "Module"};
        auditModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = SCMComponents.styledTable(auditModel);
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(200);

        JScrollPane scroll = SCMComponents.styledScrollPane(new JScrollPane(auditTable));

        // E:039 Audit log load failure handler
        refreshLog.addActionListener(e -> {
            SwingWorker<Void, Void> w = new SwingWorker<>() {
                protected Void doInBackground() throws Exception {
                    loadAuditLog();
                    return null;
                }
                protected void done() {
                    try { get(); }
                    catch (Exception ex) {
                        showAuditLogUnavailable();
                    }
                }
            };
            w.execute();
        });

        // E:040 Exception report export
        exportReport.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(800);
                    return Math.random() > 0.1;
                }
                protected void done() {
                    try {
                        if (get()) {
                            SCMComponents.showToast((Component)NotificationsPanel.this,
                                "✓ Exception audit report exported", false);
                        } else {
                            showExceptionModal("E:040", "MINOR",
                                "Exception Report Export Failed",
                                "Exception audit report PDF export failed due to missing log data or report generation service error.",
                                "Retry export once automatically. CSV download available as alternative.",
                                "Notifications", "AUDIT");
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:040", "MINOR", "Exception Report Export Failed",
                            ex.getMessage(), "Try CSV download as alternative.", "Notifications", "AUDIT");
                    }
                }
            };
            w.execute();
        });

        panel.add(titleRow, BorderLayout.NORTH);
        panel.add(auditSearchField, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBackground(SCMColors.BG_PRIMARY);
        centerPanel.add(auditSearchField, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private void loadDataAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                try {
                    loadNotifications();
                } catch (Exception e) {
                    // E:038 Notification fetch failure
                    SwingUtilities.invokeLater(() -> {
                        notifUnavailableBadge.setVisible(true);
                        loadDemoNotifications();
                    });
                }
                try {
                    loadAuditLog();
                } catch (Exception e) {
                    showAuditLogUnavailable();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadNotifications() throws Exception {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) throw new Exception("No DB");
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC LIMIT 30";
        notifications.clear();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UINotification n = new UINotification();
                n.notificationId = rs.getInt("notification_id");
                n.type = rs.getString("notification_type");
                n.message = rs.getString("message");
                n.isRead = rs.getBoolean("is_read");
                n.module = rs.getString("module_name");
                n.timestamp = rs.getString("created_at");
                notifications.add(n);
            }
        }
        SwingUtilities.invokeLater(() -> renderNotifications(notifications));
    }

    private void loadAuditLog() throws Exception {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) throw new Exception("No DB");
        String sql = "SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 50";
        auditLogs.clear();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AuditLog log = new AuditLog();
                log.timestamp  = rs.getString("timestamp");
                log.actionUser = rs.getString("action_user");
                log.action     = rs.getString("action_description");
                log.module     = rs.getString("module_name");
                auditLogs.add(log);
            }
        }
        SwingUtilities.invokeLater(() -> {
            auditModel.setRowCount(0);
            for (AuditLog log : auditLogs)
                auditModel.addRow(new Object[]{log.timestamp, log.actionUser, log.action, log.module});
        });
    }

    private void renderNotifications(List<UINotification> list) {
        notifContainer.removeAll();
        String filter = typeFilter.getSelectedItem().toString();
        for (UINotification n : list) {
            if (!filter.equals("All Types") && !n.type.equals(filter)) continue;
            notifContainer.add(buildNotifCard(n));
            notifContainer.add(Box.createVerticalStrut(6));
        }
        notifContainer.revalidate();
        notifContainer.repaint();
    }

    private JPanel buildNotifCard(UINotification n) {
        Color typeColor = switch (n.type) {
            case "ERROR"   -> SCMColors.STATUS_ERROR;
            case "WARNING" -> SCMColors.STATUS_WARNING;
            case "SUCCESS" -> SCMColors.STATUS_SUCCESS;
            default        -> SCMColors.STATUS_INFO;
        };

        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBackground(n.isRead ? SCMColors.BG_CARD : new Color(212,175,55,10));
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, 3, 0, 0, typeColor),
            new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        String icon = switch (n.type) {
            case "ERROR"   -> "🔴";
            case "WARNING" -> "⚠";
            case "SUCCESS" -> "✓";
            default        -> "ℹ";
        };

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setForeground(typeColor);
        iconLbl.setFont(iconLbl.getFont().deriveFont(16f));

        JLabel msgLbl = new JLabel("<html><b>" + n.module + "</b> — " + n.message + "</html>");
        msgLbl.setForeground(n.isRead ? SCMColors.TEXT_SECONDARY : SCMColors.TEXT_PRIMARY);
        msgLbl.setFont(SCMColors.FONT_SMALL);

        JLabel timeLbl = SCMComponents.subLabel(n.timestamp != null ? n.timestamp : "Just now");
        JLabel unreadDot = new JLabel(n.isRead ? "" : "● Unread");
        unreadDot.setForeground(typeColor);
        unreadDot.setFont(SCMColors.FONT_SMALL.deriveFont(10f));

        JPanel right = new JPanel(new BorderLayout(0, 2));
        right.setBackground(card.getBackground());
        right.add(msgLbl, BorderLayout.CENTER);
        right.add(timeLbl, BorderLayout.SOUTH);

        // Show exception modal on ERROR click
        if ("ERROR".equals(n.type)) {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showExceptionModal("E:XXX", "MAJOR",
                        n.message, n.message,
                        "Review the error details and take corrective action.",
                        n.module, "");
                    n.isRead = true;
                    card.setBackground(SCMColors.BG_CARD);
                }
            });
        }

        card.add(iconLbl, BorderLayout.WEST);
        card.add(right, BorderLayout.CENTER);
        return card;
    }

    private void filterNotifications() {
        renderNotifications(notifications);
    }

    private void markAllRead() {
        notifications.forEach(n -> n.isRead = true);
        renderNotifications(notifications);
        SCMComponents.showToast((Component)this, "✓ All notifications marked as read", false);
    }

    private void filterAuditLog(String query) {
        auditModel.setRowCount(0);
        for (AuditLog log : auditLogs) {
            if (log.action.toLowerCase().contains(query.toLowerCase()) ||
                log.actionUser.toLowerCase().contains(query.toLowerCase()) ||
                log.module.toLowerCase().contains(query.toLowerCase())) {
                auditModel.addRow(new Object[]{log.timestamp, log.actionUser, log.action, log.module});
            }
        }
    }

    private void showAuditLogUnavailable() {
        SwingUtilities.invokeLater(() -> {
            auditModel.setRowCount(0);
            // E:039 Audit log unavailable — load demo + show placeholder
            loadDemoAuditLog();
            SCMComponents.showToast((Component)this, "⚠ Audit log unavailable — showing cached data", true);
        });
    }

    private void loadDemoNotifications() {
        notifications.clear();
        Object[][] data = {
            {"SUCCESS","Orders","Order ORD-4821 dispatched successfully","2 min ago",false},
            {"WARNING","Inventory","Stock for SKU MTR-300 is critically low (0 units)","5 min ago",false},
            {"ERROR","Orders","Payment gateway timeout — Order ORD-4823 failed","8 min ago",false},
            {"INFO","Logistics","Shipment SHP-001 in transit to Delhi. ETA: 2h","15 min ago",true},
            {"WARNING","Inventory","Stock for SKU SNS-200 is below reorder point","22 min ago",true},
            {"SUCCESS","Orders","Order ORD-4820 delivered to Acme Corp","1h ago",true},
            {"ERROR","Logistics","Carrier API timeout for SHP-002 — Status Unknown","1h 30min ago",false},
            {"INFO","System","System backup completed successfully","2h ago",true}
        };
        for (Object[] d : data) {
            UINotification n = new UINotification();
            n.type = (String)d[0]; n.module = (String)d[1];
            n.message = (String)d[2]; n.timestamp = (String)d[3];
            n.isRead = (Boolean)d[4];
            notifications.add(n);
        }
        SwingUtilities.invokeLater(() -> renderNotifications(notifications));
    }

    private void loadDemoAuditLog() {
        auditLogs.clear();
        Object[][] data = {
            {"10:42 AM","admin@scm","Deleted Order ORD-4800","Orders"},
            {"09:15 AM","manager@scm","Updated Stock MTR-300","Inventory"},
            {"09:00 AM","admin@scm","User login — SUPER_ADMIN","Auth"},
            {"08:45 AM","warehouse1@scm","Added Product WGT-105","Inventory"},
            {"08:30 AM","logistics1@scm","Created Delivery Order SHP-006","Logistics"},
            {"08:00 AM","admin@scm","Changed role: sales1 → MANAGER","Settings"},
            {"Yesterday 5:30 PM","manager@scm","Exported Forecast Report","Forecasting"},
            {"Yesterday 3:15 PM","admin@scm","Updated Discount Rule SAVE20","Pricing"}
        };
        for (Object[] d : data) {
            AuditLog log = new AuditLog();
            log.timestamp = (String)d[0]; log.actionUser = (String)d[1];
            log.action = (String)d[2]; log.module = (String)d[3];
            auditLogs.add(log);
        }
        SwingUtilities.invokeLater(() -> {
            auditModel.setRowCount(0);
            for (AuditLog log : auditLogs)
                auditModel.addRow(new Object[]{log.timestamp, log.actionUser, log.action, log.module});
        });
    }

    private void startAutoRetry() {
        autoRetryTimer = new java.util.Timer(true);
        autoRetryTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (retryCount < 3) {
                    retryCount++;
                    try {
                        loadNotifications();
                        notifUnavailableBadge.setVisible(false);
                        retryCount = 0;
                    } catch (Exception ignored) {}
                }
            }
        }, 60000, 60000);
    }

    public void showExceptionModal(String code, String category, String title,
                                    String message, String plan, String module, String refId) {
        JDialog modal = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
            "Exception — " + title, true);
        modal.setSize(560, 420);
        modal.setLocationRelativeTo(this);
        modal.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        Color catColor = "MAJOR".equals(category) ? SCMColors.STATUS_ERROR : SCMColors.STATUS_WARNING;

        JPanel topBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topBadge.setBackground(SCMColors.BG_SECONDARY);

        JLabel codeLabel = new JLabel("  " + code + "  ");
        codeLabel.setForeground(catColor);
        codeLabel.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD));
        codeLabel.setOpaque(true);
        codeLabel.setBackground(new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 25));
        codeLabel.setBorder(new CompoundBorder(new LineBorder(catColor, 1), new EmptyBorder(4,8,4,8)));

        JLabel catLabel = new JLabel(category);
        catLabel.setForeground(catColor);
        catLabel.setFont(SCMColors.FONT_SMALL);

        JLabel modLabel = SCMComponents.subLabel("Module: " + module);
        if (!refId.isEmpty()) {
            JLabel refLabel = SCMComponents.subLabel("  Ref: " + refId);
            topBadge.add(refLabel);
        }

        topBadge.add(codeLabel); topBadge.add(catLabel); topBadge.add(modLabel);

        JLabel titleLbl = SCMComponents.goldLabel("⚠  " + title);
        titleLbl.setFont(SCMColors.FONT_HEADING);

        // Stack trace area
        JTextArea detailArea = new JTextArea(
            "Error Message:\n" + message + "\n\n" +
            "Module: " + module + (refId.isEmpty() ? "" : "\nReference: " + refId) + "\n\n" +
            "Resolution Plan:\n" + plan + "\n\n" +
            "Timestamp: " + new java.util.Date() + "\n" +
            "Stack trace: [collapsible — available in full log]"
        );
        detailArea.setForeground(SCMColors.TEXT_SECONDARY);
        detailArea.setBackground(SCMColors.BG_CARD);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(new LineBorder(new Color(255,255,255,20), 1));
        detailScroll.setPreferredSize(new Dimension(0, 160));

        // Buttons
        JButton dismiss  = SCMComponents.outlineButton("✕ Dismiss");
        JButton retry    = SCMComponents.outlineButton("↻ Retry");
        JButton report   = SCMComponents.goldButton("📄 Export Report");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(dismiss); btns.add(retry); btns.add(report);

        dismiss.addActionListener(e -> modal.dispose());
        retry.addActionListener(e -> { modal.dispose(); loadDataAsync(); });
        report.addActionListener(e -> {
            SCMComponents.showToast((Component)this, "📄 Exception report exported", false);
            modal.dispose();
        });

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(SCMColors.BG_SECONDARY);
        top.add(topBadge, BorderLayout.NORTH);
        top.add(titleLbl, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(detailScroll, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        modal.add(panel);
        modal.setVisible(true);
    }

    public void cleanup() {
        if (autoRetryTimer != null) autoRetryTimer.cancel();
    }

    // Public method to add notification from other panels
    public static void addNotification(String type, String module, String message) {
        // This would be hooked into the MainFrame's notifications panel
        // For demo purposes, notifications are loaded from DB on refresh
    }
}
