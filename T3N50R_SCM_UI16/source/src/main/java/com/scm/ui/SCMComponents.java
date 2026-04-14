package com.scm.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Reusable luxury UI component library for SCM application.
 */
public final class SCMComponents {

    private SCMComponents() {}

    // ══════════════════════════════════════════════════════════════════════════
    // GOLD PRIMARY BUTTON
    // ══════════════════════════════════════════════════════════════════════════
    public static JButton goldButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(SCMColors.ACCENT_COPPER);
                } else if (getModel().isRollover()) {
                    g2.setColor(SCMColors.ACCENT_GOLD_LIGHT);
                } else {
                    g2.setColor(SCMColors.ACCENT_GOLD);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(SCMColors.TEXT_ON_GOLD);
                g2.setFont(SCMColors.FONT_SUBHEAD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GHOST / OUTLINE BUTTON
    // ══════════════════════════════════════════════════════════════════════════
    public static JButton outlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? SCMColors.BG_HOVER : SCMColors.BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(SCMColors.ACCENT_GOLD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(SCMColors.ACCENT_GOLD_LIGHT);
                g2.setFont(SCMColors.FONT_SUBHEAD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        return btn;
    }

    // Danger button (red outline)
    public static JButton dangerButton(String text) {
        JButton btn = outlineButton(text);
        btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? new Color(0x8B,0x00,0x00) :
                           getModel().isRollover() ? new Color(0xFF,0x3E,0x55,80) : SCMColors.BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(SCMColors.STATUS_ERROR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(SCMColors.STATUS_ERROR);
                g2.setFont(SCMColors.FONT_SUBHEAD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STYLED TEXT FIELD
    // ══════════════════════════════════════════════════════════════════════════
    public static JTextField styledField(String placeholder) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner() && placeholder != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(SCMColors.TEXT_MUTED);
                    g2.setFont(SCMColors.FONT_BODY);
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2, getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2);
                    g2.dispose();
                }
            }
        };
        field.setBackground(SCMColors.BG_INPUT);
        field.setForeground(SCMColors.TEXT_PRIMARY);
        field.setCaretColor(SCMColors.ACCENT_GOLD);
        field.setFont(SCMColors.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SCMColors.BORDER_FOCUS, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
        return field;
    }

    // Password field
    public static JPasswordField styledPasswordField() {
        return styledPasswordField("");
    }

    public static JPasswordField styledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setBackground(SCMColors.BG_INPUT);
        field.setForeground(SCMColors.TEXT_PRIMARY);
        field.setCaretColor(SCMColors.ACCENT_GOLD);
        field.setFont(SCMColors.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SCMColors.BORDER_FOCUS, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
        return field;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMBO BOX
    // ══════════════════════════════════════════════════════════════════════════
    public static <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setBackground(SCMColors.BG_INPUT);
        combo.setForeground(SCMColors.TEXT_PRIMARY);
        combo.setFont(SCMColors.FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? SCMColors.BG_SELECTED : SCMColors.BG_INPUT);
                setForeground(SCMColors.TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
        return combo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STYLED TABLE
    // ══════════════════════════════════════════════════════════════════════════
    public static JTable styledTable(String[] columns, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);
        return table;
    }

    /** Overload: create a styled JTable from an existing DefaultTableModel */
    public static JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        styleTable(table);
        return table;
    }

    public static void styleTable(JTable table) {
        table.setBackground(SCMColors.BG_CARD);
        table.setForeground(SCMColors.TEXT_PRIMARY);
        table.setFont(SCMColors.FONT_BODY);
        table.setRowHeight(38);
        table.setGridColor(SCMColors.BORDER_DEFAULT);
        table.setSelectionBackground(SCMColors.BG_SELECTED);
        table.setSelectionForeground(SCMColors.ACCENT_GOLD_LIGHT);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(SCMColors.BG_TABLE_HEADER);
        header.setForeground(SCMColors.ACCENT_GOLD);
        header.setFont(SCMColors.FONT_SUBHEAD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, SCMColors.ACCENT_GOLD));
        header.setReorderingAllowed(false);

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (sel) {
                    setBackground(SCMColors.BG_SELECTED);
                    setForeground(SCMColors.ACCENT_GOLD_LIGHT);
                } else {
                    setBackground(row % 2 == 0 ? SCMColors.BG_CARD : SCMColors.BG_TABLE_ROW_ALT);
                    setForeground(SCMColors.TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARD PANEL (KPI card etc.)
    // ══════════════════════════════════════════════════════════════════════════
    public static JPanel cardPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCMColors.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(SCMColors.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    // Gold-bordered card
    public static JPanel goldCardPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCMColors.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Gold top-border accent
                g2.setColor(SCMColors.ACCENT_GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(12, 0, getWidth()-12, 0);
                g2.setColor(SCMColors.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LABEL HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    public static JLabel heading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(SCMColors.FONT_HEADING);
        lbl.setForeground(SCMColors.TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel goldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(SCMColors.FONT_HEADING);
        lbl.setForeground(SCMColors.ACCENT_GOLD);
        return lbl;
    }

    public static JLabel subLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(SCMColors.FONT_SMALL);
        lbl.setForeground(SCMColors.TEXT_SECONDARY);
        return lbl;
    }

    public static JLabel statusBadge(String text, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(SCMColors.FONT_BADGE);
        lbl.setForeground(color);
        lbl.setOpaque(false);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return lbl;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCROLL PANE
    // ══════════════════════════════════════════════════════════════════════════
    public static JScrollPane styledScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(SCMColors.BG_PRIMARY);
        sp.getViewport().setBackground(SCMColors.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(SCMColors.BORDER_DEFAULT));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = SCMColors.ACCENT_GOLD;
                trackColor = SCMColors.BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });
        sp.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = SCMColors.ACCENT_GOLD; trackColor = SCMColors.BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            @Override protected JButton createIncreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEPARATOR
    // ══════════════════════════════════════════════════════════════════════════
    public static JSeparator goldSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(SCMColors.ACCENT_GOLD);
        sep.setBackground(SCMColors.BG_CARD);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION HEADER (with gold underline)
    // ══════════════════════════════════════════════════════════════════════════
    public static JPanel sectionHeader(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(SCMColors.FONT_TITLE);
        titleLbl.setForeground(SCMColors.TEXT_PRIMARY);
        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(SCMColors.FONT_SMALL);
        subLbl.setForeground(SCMColors.TEXT_MUTED);
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(titleLbl, BorderLayout.NORTH);
        text.add(subLbl, BorderLayout.CENTER);
        p.add(text, BorderLayout.WEST);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOAST NOTIFICATION
    // ══════════════════════════════════════════════════════════════════════════
    public static void showToast(JFrame parent, String message, Color color) {
        JWindow toast = new JWindow(parent);
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCMColors.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel lbl = new JLabel(message);
        lbl.setFont(SCMColors.FONT_BODY);
        lbl.setForeground(SCMColors.TEXT_PRIMARY);
        panel.add(lbl);
        toast.add(panel);
        toast.pack();
        // Position bottom-right of parent
        int x = parent.getX() + parent.getWidth()  - toast.getWidth()  - 20;
        int y = parent.getY() + parent.getHeight() - toast.getHeight() - 50;
        toast.setLocation(x, y);
        toast.setVisible(true);
        // Auto-dismiss after 3 seconds
        new Timer(3000, e -> toast.dispose()).start();
    }

    /**
     * Overload: show a toast anchored to any Component, with isError flag.
     * isError=true uses STATUS_ERROR border, false uses STATUS_SUCCESS.
     */
    public static void showToast(Component anchor, String message, boolean isError) {
        Color color = isError ? SCMColors.STATUS_ERROR : SCMColors.STATUS_SUCCESS;
        Window window = SwingUtilities.getWindowAncestor(anchor);
        JWindow toast = (window instanceof JFrame)
            ? new JWindow((JFrame) window)
            : new JWindow();
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCMColors.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel lbl = new JLabel(message);
        lbl.setFont(SCMColors.FONT_BODY);
        lbl.setForeground(SCMColors.TEXT_PRIMARY);
        panel.add(lbl);
        toast.add(panel);
        toast.pack();
        if (window != null) {
            int x = window.getX() + window.getWidth()  - toast.getWidth()  - 20;
            int y = window.getY() + window.getHeight() - toast.getHeight() - 50;
            toast.setLocation(x, y);
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            toast.setLocation(screen.width - toast.getWidth() - 20, screen.height - toast.getHeight() - 60);
        }
        toast.setVisible(true);
        new Timer(3000, e -> toast.dispose()).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEXT AREA (for stack traces etc.)
    // ══════════════════════════════════════════════════════════════════════════
    public static JTextArea styledTextArea() {
        JTextArea ta = new JTextArea();
        ta.setBackground(SCMColors.BG_INPUT);
        ta.setForeground(SCMColors.TEXT_PRIMARY);
        ta.setFont(SCMColors.FONT_MONO);
        ta.setCaretColor(SCMColors.ACCENT_GOLD);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return ta;
    }

    // Spinner
    public static JSpinner styledSpinner(int min, int max, int val) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        spinner.setBackground(SCMColors.BG_INPUT);
        spinner.setForeground(SCMColors.TEXT_PRIMARY);
        spinner.setFont(SCMColors.FONT_BODY);
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setBackground(SCMColors.BG_INPUT);
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setForeground(SCMColors.TEXT_PRIMARY);
        return spinner;
    }
}
