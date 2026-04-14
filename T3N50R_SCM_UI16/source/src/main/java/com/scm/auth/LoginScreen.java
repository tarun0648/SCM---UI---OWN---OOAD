package com.scm.auth;

import com.scm.ui.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * WF-1 – Login Screen (C-01 Auth & RBAC)
 * Luxury dark UI with gold accents, animated logo, full exception handling.
 */
public class LoginScreen extends JFrame {

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JTextField     tfaField;
    private JLabel         errorLabel;
    private JButton        loginBtn;
    private JCheckBox      showTfaCheck;
    private JPanel         tfaPanel;
    private Runnable       onLoginSuccess;

    public LoginScreen() {
        this((Runnable) null);
    }

    public LoginScreen(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        initUI();
    }

    private void initUI() {
        setTitle("SCM – Supply Chain Management Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(false);
        setResizable(false);

        // Main background panel
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background gradient
                GradientPaint gp = new GradientPaint(0, 0, SCMColors.BG_PRIMARY,
                    getWidth(), getHeight(), new Color(0x0A, 0x14, 0x2A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Decorative grid lines
                g2.setColor(new Color(0xD4, 0xAF, 0x37, 12));
                g2.setStroke(new BasicStroke(0.5f));
                for (int i = 0; i < getWidth(); i += 40) g2.drawLine(i, 0, i, getHeight());
                for (int i = 0; i < getHeight(); i += 40) g2.drawLine(0, i, getWidth(), i);
                // Gold circle decoration
                g2.setColor(new Color(0xD4, 0xAF, 0x37, 18));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(getWidth()/2 - 220, getHeight()/2 - 280, 440, 440);
                g2.drawOval(getWidth()/2 - 180, getHeight()/2 - 240, 360, 360);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ── Card ─────────────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x13, 0x1A, 0x2B, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                // Top gold border
                GradientPaint gp = new GradientPaint(0, 0, SCMColors.ACCENT_COPPER,
                    getWidth(), 0, SCMColors.ACCENT_GOLD_LIGHT);
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(20, 1, getWidth()-20, 1);
                // Subtle outer border
                g2.setColor(SCMColors.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 560));
        card.setBorder(BorderFactory.createEmptyBorder(40, 44, 40, 44));

        // ── Logo area ─────────────────────────────────────────────────────────
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));

        JLabel scmLogo = new JLabel("SCM") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font f = new Font("Segoe UI", Font.BOLD, 42);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics(f);
                int x = (getWidth() - fm.stringWidth("SCM")) / 2;
                int y = fm.getAscent() + 2;
                // Gold gradient text
                GradientPaint gp = new GradientPaint(x, y - fm.getAscent(), SCMColors.ACCENT_COPPER,
                    x + fm.stringWidth("SCM"), y, SCMColors.ACCENT_GOLD_LIGHT);
                g2.setPaint(gp);
                g2.drawString("SCM", x, y);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(200, 56); }
        };
        scmLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel("Supply Chain Management Portal", SwingConstants.CENTER);
        subTitle.setFont(SCMColors.FONT_SMALL);
        subTitle.setForeground(SCMColors.TEXT_MUTED);
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Gold divider
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0,0,0,0),
                    getWidth(), 0, SCMColors.ACCENT_GOLD);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(200, 1); }
        };
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoPanel.add(Box.createVerticalStrut(4));
        logoPanel.add(scmLogo);
        logoPanel.add(Box.createVerticalStrut(4));
        logoPanel.add(subTitle);
        logoPanel.add(Box.createVerticalStrut(18));
        logoPanel.add(divider);
        logoPanel.add(Box.createVerticalStrut(18));

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // Username
        JLabel uLabel = SCMComponents.subLabel("USERNAME / EMAIL");
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField = SCMComponents.styledField("Enter username");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setText("admin");

        // Password
        JLabel pLabel = SCMComponents.subLabel("PASSWORD");
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = SCMComponents.styledPasswordField("Enter password");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 2FA toggle
        showTfaCheck = new JCheckBox("Enable 2FA Token");
        showTfaCheck.setFont(SCMColors.FONT_SMALL);
        showTfaCheck.setForeground(SCMColors.TEXT_SECONDARY);
        showTfaCheck.setBackground(new Color(0,0,0,0));
        showTfaCheck.setOpaque(false);
        showTfaCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 2FA panel
        tfaPanel = new JPanel();
        tfaPanel.setOpaque(false);
        tfaPanel.setLayout(new BoxLayout(tfaPanel, BoxLayout.Y_AXIS));
        tfaPanel.setVisible(false);
        JLabel tfaLabel = SCMComponents.subLabel("2FA TOKEN (6-DIGIT)");
        tfaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfaField = SCMComponents.styledField("Enter 6-digit token");
        tfaField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tfaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfaPanel.add(Box.createVerticalStrut(10));
        tfaPanel.add(tfaLabel);
        tfaPanel.add(Box.createVerticalStrut(6));
        tfaPanel.add(tfaField);

        showTfaCheck.addActionListener(e -> {
            tfaPanel.setVisible(showTfaCheck.isSelected());
            pack();
        });

        // Error label
        errorLabel = new JLabel(" ", SwingConstants.CENTER);
        errorLabel.setFont(SCMColors.FONT_SMALL);
        errorLabel.setForeground(SCMColors.STATUS_ERROR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login button
        loginBtn = SCMComponents.goldButton("  LOGIN  ");
        loginBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> doLogin());

        // Forgot password link
        JLabel forgotLbl = new JLabel("Forgot Password? Contact Admin", SwingConstants.CENTER);
        forgotLbl.setFont(SCMColors.FONT_SMALL);
        forgotLbl.setForeground(SCMColors.ACCENT_GOLD);
        forgotLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Default credentials hint
        JLabel hint = new JLabel("Demo: admin / admin123  |  manager / manager123", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(SCMColors.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Build form
        form.add(uLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(14));
        form.add(pLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(12));
        form.add(showTfaCheck);
        form.add(tfaPanel);
        form.add(Box.createVerticalStrut(14));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(10));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(14));
        form.add(forgotLbl);
        form.add(Box.createVerticalStrut(10));
        form.add(hint);

        // Assemble card
        card.add(logoPanel, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel version = new JLabel("v1.0 | T3N50R | UI Subsystem #16");
        version.setFont(SCMColors.FONT_SMALL);
        version.setForeground(SCMColors.TEXT_MUTED);
        footer.add(version);
        card.add(footer, BorderLayout.SOUTH);

        // Enter key triggers login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        tfaField.addKeyListener(enterKey);

        root.add(card);

        // Window
        setContentPane(root);
        getContentPane().setBackground(SCMColors.BG_PRIMARY);
        setSize(520, 620);
        setLocationRelativeTo(null);
        usernameField.requestFocusInWindow();
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        errorLabel.setText(" ");

        if (username.isEmpty()) {
            showError("E:001 – Username is required.");
            return;
        }
        if (password.isEmpty()) {
            showError("E:001 – Password is required.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Authenticating...");

        SwingWorker<AuthService.LoginResult, Void> worker = new SwingWorker<>() {
            @Override protected AuthService.LoginResult doInBackground() {
                return AuthService.login(username, password);
            }
            @Override protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    loginBtn.setEnabled(true);
                    loginBtn.setText("  LOGIN  ");
                    if (result.success) {
                        dispose();
                        onLoginSuccess.run();
                    } else {
                        showError(result.errorCode + " – " + result.message);
                        passwordField.setText("");
                        if ("E:005".equals(result.errorCode)) {
                            loginBtn.setEnabled(false);
                            loginBtn.setText("ACCOUNT LOCKED");
                        }
                    }
                } catch (Exception ex) {
                    showError("Unexpected error: " + ex.getMessage());
                    loginBtn.setEnabled(true);
                    loginBtn.setText("  LOGIN  ");
                }
            }
        };
        worker.execute();
    }

    private void showError(String msg) {
        errorLabel.setText("<html><center>" + msg + "</center></html>");
    }
}
