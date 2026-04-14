package com.scm.ui;

import com.scm.auth.LoginScreen;

import javax.swing.*;
import java.awt.*;

public class SCMApplication {

    public static void main(String[] args) {
        // Set system properties for DB (override with env or args)
        System.setProperty("scm.db.url",  System.getenv("SCM_DB_URL")  != null ? System.getenv("SCM_DB_URL")  : "jdbc:mysql://localhost:3306/scm_db");
        System.setProperty("scm.db.user", System.getenv("SCM_DB_USER") != null ? System.getenv("SCM_DB_USER") : "root");
        System.setProperty("scm.db.pass", System.getenv("SCM_DB_PASS") != null ? System.getenv("SCM_DB_PASS") : "");

        // Apply Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[SCM] LookAndFeel not applied: " + e.getMessage());
        }

        // Global UI defaults — dark theme
        UIManager.put("Panel.background",          SCMColors.BG_PRIMARY);
        UIManager.put("Frame.background",          SCMColors.BG_PRIMARY);
        UIManager.put("Button.background",         SCMColors.BG_CARD);
        UIManager.put("Button.foreground",         SCMColors.TEXT_PRIMARY);
        UIManager.put("Label.foreground",          SCMColors.TEXT_PRIMARY);
        UIManager.put("TextField.background",      SCMColors.BG_CARD);
        UIManager.put("TextField.foreground",      SCMColors.TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", SCMColors.ACCENT_GOLD);
        UIManager.put("TextArea.background",       SCMColors.BG_CARD);
        UIManager.put("TextArea.foreground",       SCMColors.TEXT_PRIMARY);
        UIManager.put("PasswordField.background",  SCMColors.BG_CARD);
        UIManager.put("PasswordField.foreground",  SCMColors.TEXT_PRIMARY);
        UIManager.put("ComboBox.background",       SCMColors.BG_CARD);
        UIManager.put("ComboBox.foreground",       SCMColors.TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", SCMColors.ACCENT_GOLD);
        UIManager.put("ComboBox.selectionForeground", SCMColors.BG_PRIMARY);
        UIManager.put("List.background",           SCMColors.BG_CARD);
        UIManager.put("List.foreground",           SCMColors.TEXT_PRIMARY);
        UIManager.put("List.selectionBackground",  SCMColors.ACCENT_GOLD);
        UIManager.put("List.selectionForeground",  SCMColors.BG_PRIMARY);
        UIManager.put("Table.background",          SCMColors.BG_CARD);
        UIManager.put("Table.foreground",          SCMColors.TEXT_PRIMARY);
        UIManager.put("Table.gridColor",           new Color(255, 255, 255, 15));
        UIManager.put("Table.selectionBackground", new Color(212, 175, 55, 60));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background",    SCMColors.BG_SECONDARY);
        UIManager.put("TableHeader.foreground",    SCMColors.ACCENT_GOLD);
        UIManager.put("ScrollPane.background",     SCMColors.BG_PRIMARY);
        UIManager.put("ScrollBar.background",      SCMColors.BG_SECONDARY);
        UIManager.put("ScrollBar.thumb",           SCMColors.ACCENT_GOLD);
        UIManager.put("ScrollBar.track",           SCMColors.BG_CARD);
        UIManager.put("SplitPane.background",      SCMColors.BG_PRIMARY);
        UIManager.put("TabbedPane.background",     SCMColors.BG_SECONDARY);
        UIManager.put("TabbedPane.foreground",     SCMColors.TEXT_SECONDARY);
        UIManager.put("TabbedPane.selected",       SCMColors.BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", SCMColors.ACCENT_GOLD);
        UIManager.put("TabbedPane.contentAreaColor", SCMColors.BG_SECONDARY);
        UIManager.put("TabbedPane.light",          SCMColors.BG_SECONDARY);
        UIManager.put("TabbedPane.highlight",      SCMColors.ACCENT_GOLD);
        UIManager.put("TabbedPane.shadow",         SCMColors.BG_CARD);
        UIManager.put("TabbedPane.darkShadow",     SCMColors.BG_PRIMARY);
        UIManager.put("CheckBox.background",       SCMColors.BG_CARD);
        UIManager.put("CheckBox.foreground",       SCMColors.TEXT_PRIMARY);
        UIManager.put("RadioButton.background",    SCMColors.BG_CARD);
        UIManager.put("RadioButton.foreground",    SCMColors.TEXT_PRIMARY);
        UIManager.put("Spinner.background",        SCMColors.BG_CARD);
        UIManager.put("Spinner.foreground",        SCMColors.TEXT_PRIMARY);
        UIManager.put("Dialog.background",         SCMColors.BG_SECONDARY);
        UIManager.put("OptionPane.background",     SCMColors.BG_SECONDARY);
        UIManager.put("OptionPane.messageForeground", SCMColors.TEXT_PRIMARY);
        UIManager.put("ToolTip.background",        SCMColors.BG_CARD);
        UIManager.put("ToolTip.foreground",        SCMColors.TEXT_PRIMARY);
        UIManager.put("PopupMenu.background",      SCMColors.BG_CARD);
        UIManager.put("MenuItem.background",       SCMColors.BG_CARD);
        UIManager.put("MenuItem.foreground",       SCMColors.TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", SCMColors.ACCENT_GOLD);
        UIManager.put("MenuItem.selectionForeground", SCMColors.BG_PRIMARY);
        UIManager.put("Separator.foreground",      new Color(255, 255, 255, 20));
        UIManager.put("Separator.background",      new Color(255, 255, 255, 10));
        UIManager.put("ToggleButton.background",   SCMColors.BG_CARD);
        UIManager.put("ToggleButton.foreground",   SCMColors.TEXT_PRIMARY);
        UIManager.put("ToggleButton.select",       SCMColors.ACCENT_GOLD);

        SwingUtilities.invokeLater(() -> {
            try {
                showSplashScreen();
            } catch (Exception e) {
                System.err.println("[SCM] Startup error: " + e.getMessage());
                showLoginScreen();
            }
        });
    }

    private static void showSplashScreen() throws InterruptedException {
        JWindow splash = new JWindow();
        splash.setSize(480, 300);
        splash.setLocationRelativeTo(null);

        JPanel splashPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, SCMColors.BG_PRIMARY,
                    0, getHeight(), SCMColors.BG_SECONDARY
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Gold border
                g2.setColor(SCMColors.ACCENT_GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth()-3, getHeight()-3);

                // Decorative circles
                g2.setColor(new Color(212, 175, 55, 15));
                g2.fillOval(-40, -40, 200, 200);
                g2.fillOval(getWidth()-100, getHeight()-100, 200, 200);

                g2.dispose();
            }
        };
        splashPanel.setOpaque(false);

        // Logo
        JLabel logo = new JLabel("SCM", JLabel.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 72));
        logo.setForeground(SCMColors.ACCENT_GOLD);

        JLabel tagline = new JLabel("Supply Chain Management Portal", JLabel.CENTER);
        tagline.setFont(SCMColors.FONT_BODY);
        tagline.setForeground(SCMColors.TEXT_SECONDARY);

        JLabel team = new JLabel("Team T3N50R  —  UI Subsystem #16", JLabel.CENTER);
        team.setFont(SCMColors.FONT_SMALL);
        team.setForeground(new Color(212, 175, 55, 120));

        JLabel loading = new JLabel("Loading...", JLabel.CENTER);
        loading.setFont(SCMColors.FONT_SMALL);
        loading.setForeground(SCMColors.STATUS_INFO);

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setForeground(SCMColors.ACCENT_GOLD);
        progress.setBackground(SCMColors.BG_CARD);
        progress.setBorder(new javax.swing.border.EmptyBorder(0, 40, 0, 40));
        progress.setPreferredSize(new Dimension(0, 4));

        JPanel textPanel = new JPanel(new GridLayout(4, 1, 0, 8));
        textPanel.setOpaque(false);
        textPanel.setBorder(new javax.swing.border.EmptyBorder(20, 20, 20, 20));
        textPanel.add(tagline);
        textPanel.add(team);
        textPanel.add(loading);

        splashPanel.add(logo, BorderLayout.CENTER);
        splashPanel.add(textPanel, BorderLayout.SOUTH);
        splashPanel.add(progress, BorderLayout.NORTH);

        splash.add(splashPanel);
        splash.setVisible(true);

        // Animate progress
        for (int i = 0; i <= 100; i++) {
            final int val = i;
            SwingUtilities.invokeLater(() -> {
                progress.setValue(val);
                if (val < 30) loading.setText("Connecting to database...");
                else if (val < 60) loading.setText("Loading UI components...");
                else if (val < 90) loading.setText("Initializing modules...");
                else loading.setText("Ready!");
            });
            Thread.sleep(18);
        }

        Thread.sleep(400);
        splash.setVisible(false);
        splash.dispose();
        showLoginScreen();
    }

    private static void showLoginScreen() {
        LoginScreen login = new LoginScreen();
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }

    // Internal class for splash painting (used above as anonymous class)
    private static final Color BG_PRIMARY = SCMColors.BG_PRIMARY;
}
