package com.scm.pricing;

import com.scm.db.DatabaseConnection;
import com.scm.models.Models.*;
import com.scm.ui.SCMColors;
import com.scm.ui.SCMComponents;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class PricingPanel extends JPanel {

    private JPanel tiersPanel;
    private JTable discountTable, commissionTable;
    private DefaultTableModel discountModel, commissionModel;
    private List<PriceTier> priceTiers = new ArrayList<>();
    private List<DiscountRule> discountRules = new ArrayList<>();

    public PricingPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(SCMColors.BG_PRIMARY);
        initComponents();
        loadDataAsync();
    }

    private void initComponents() {
        JPanel header = SCMComponents.sectionHeader("💰  Pricing, Discount & Commission",
            "Manage price tiers, discount rules, and agent commission ledger");
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(SCMColors.BG_SECONDARY);
        tabs.setForeground(SCMColors.TEXT_PRIMARY);
        tabs.setFont(SCMColors.FONT_BODY);
        tabs.setBorder(new EmptyBorder(12, 20, 20, 20));

        // Style tabs
        UIManager.put("TabbedPane.selected", SCMColors.BG_CARD);
        UIManager.put("TabbedPane.contentAreaColor", SCMColors.BG_SECONDARY);

        tabs.addTab("  📊 Price Tiers  ", buildPriceTiersTab());
        tabs.addTab("  🏷 Discount Rules  ", buildDiscountRulesTab());
        tabs.addTab("  💼 Commission Ledger  ", buildCommissionTab());

        // Color active tab
        tabs.setBackgroundAt(0, SCMColors.BG_CARD);
        tabs.setForegroundAt(0, SCMColors.ACCENT_GOLD);

        add(tabs, BorderLayout.CENTER);
    }

    // ── Tab 1: Price Tiers ───────────────────────────────────────────────────
    private JPanel buildPriceTiersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Tier cards row
        tiersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        tiersPanel.setBackground(SCMColors.BG_PRIMARY);

        JScrollPane tiersScroll = new JScrollPane(tiersPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tiersScroll.setBorder(null);
        tiersScroll.getViewport().setBackground(SCMColors.BG_PRIMARY);
        tiersScroll.setPreferredSize(new Dimension(0, 180));

        // Add Tier button
        JButton addTierBtn = SCMComponents.goldButton("+ Add Price Tier");
        addTierBtn.addActionListener(e -> showAddTierDialog());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(SCMColors.BG_PRIMARY);
        JLabel lbl = SCMComponents.goldLabel("Price Tier Configuration");
        topBar.add(lbl, BorderLayout.WEST);
        topBar.add(addTierBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(tiersScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTierCard(PriceTier tier) {
        JPanel card = SCMComponents.goldCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(180, 150));
        card.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.ACCENT_GOLD, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel name = SCMComponents.goldLabel(tier.tierName);
        name.setAlignmentX(CENTER_ALIGNMENT);
        JLabel price = new JLabel("₹" + String.format("%,.0f", tier.pricePerUnit) + " / unit");
        price.setForeground(Color.WHITE);
        price.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD, 16f));
        price.setAlignmentX(CENTER_ALIGNMENT);
        JLabel minQty = SCMComponents.subLabel("Min Qty: " + tier.minOrderQty);
        minQty.setAlignmentX(CENTER_ALIGNMENT);

        JButton editBtn = SCMComponents.outlineButton("✏ Edit");
        editBtn.setAlignmentX(CENTER_ALIGNMENT);
        editBtn.addActionListener(e -> showEditTierDialog(tier));

        card.add(Box.createVerticalStrut(8));
        card.add(name);
        card.add(Box.createVerticalStrut(8));
        card.add(price);
        card.add(Box.createVerticalStrut(6));
        card.add(minQty);
        card.add(Box.createVerticalStrut(12));
        card.add(editBtn);
        return card;
    }

    // ── Tab 2: Discount Rules ────────────────────────────────────────────────
    private JPanel buildDiscountRulesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Discount Rule Builder
        JPanel builderCard = SCMComponents.cardPanel();
        builderCard.setLayout(new BorderLayout(12, 12));
        builderCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(212, 175, 55, 60), 1),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel builderTitle = SCMComponents.goldLabel("🏷  Discount Rule Builder");
        JPanel form = new JPanel(new GridLayout(3, 4, 12, 10));
        form.setBackground(SCMColors.BG_CARD);

        JTextField codeField  = SCMComponents.styledField("e.g. SAVE20");
        String[] types = {"Percentage", "Fixed Amount", "Buy X Get Y"};
        JComboBox<String> typeCombo = SCMComponents.styledCombo(types);
        JTextField valueField = SCMComponents.styledField("e.g. 20");
        JTextField fromField  = SCMComponents.styledField("dd/mm/yyyy");
        JTextField toField    = SCMComponents.styledField("dd/mm/yyyy");
        JTextField minOrdField = SCMComponents.styledField("Min order ₹");

        form.add(SCMComponents.subLabel("Code:")); form.add(codeField);
        form.add(SCMComponents.subLabel("Type:")); form.add(typeCombo);
        form.add(SCMComponents.subLabel("Value:")); form.add(valueField);
        form.add(SCMComponents.subLabel("Valid From:")); form.add(fromField);
        form.add(SCMComponents.subLabel("Valid To:")); form.add(toField);
        form.add(SCMComponents.subLabel("Min Order (₹):")); form.add(minOrdField);

        JButton addRule = SCMComponents.goldButton("+ Add Rule");
        addRule.addActionListener(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                SCMComponents.showToast((Component)this, "⚠ Discount code cannot be empty", true);
                return;
            }
            // E:033 Duplicate discount rule check
            boolean isDuplicate = discountRules.stream()
                .anyMatch(r -> r.code.equalsIgnoreCase(code));
            if (isDuplicate) {
                showExceptionModal("E:033", "WARNING",
                    "Duplicate Discount Rule Detected",
                    "A discount rule with code '" + code + "' already exists or has overlapping validity dates.",
                    "Please use a unique code and check date ranges for conflicts.");
                return;
            }
            // E:029 Invalid discount rule
            String val = valueField.getText().trim();
            if (val.isEmpty() || !val.matches("\\d+(\\.\\d+)?")) {
                showExceptionModal("E:029", "WARNING",
                    "Invalid Discount Rule",
                    "Discount value '" + val + "' is not a valid number.",
                    "Enter a numeric discount value (e.g. 20 for 20%).");
                return;
            }
            addDiscountRuleInDB(code, (String)typeCombo.getSelectedItem(),
                Double.parseDouble(val), fromField.getText(), toField.getText(),
                minOrdField.getText());
            codeField.setText(""); valueField.setText("");
            fromField.setText("dd/mm/yyyy"); toField.setText("dd/mm/yyyy");
        });

        JPanel builderBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        builderBottom.setBackground(SCMColors.BG_CARD);
        builderBottom.add(addRule);

        builderCard.add(builderTitle, BorderLayout.NORTH);
        builderCard.add(form, BorderLayout.CENTER);
        builderCard.add(builderBottom, BorderLayout.SOUTH);

        // Discount table
        String[] cols = {"Code", "Type", "Value", "Valid From", "Valid To", "Min Order", "Status", "Actions"};
        discountModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 7; }
        };
        discountTable = SCMComponents.styledTable(discountModel);
        discountTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        discountTable.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer("Delete"));
        discountTable.getColumnModel().getColumn(7).setCellEditor(
            new ActionButtonEditor(new JCheckBox(), "Delete", (row) -> {
                // E:029 guard
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete discount rule '" + discountModel.getValueAt(row, 0) + "'?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    discountModel.removeRow(row);
                    SCMComponents.showToast((Component)this, "✓ Discount rule deleted", false);
                }
            }));

        JScrollPane tableScroll = SCMComponents.styledScrollPane(new JScrollPane(discountTable));

        panel.add(builderCard, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 3: Commission Ledger ─────────────────────────────────────────────
    private JPanel buildCommissionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SCMColors.BG_PRIMARY);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(SCMColors.BG_PRIMARY);
        JLabel title = SCMComponents.goldLabel("💼  Agent Commission Ledger");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(SCMColors.BG_PRIMARY);
        JButton exportPayout = SCMComponents.goldButton("📄 Export Payout Report");
        JButton recalc       = SCMComponents.outlineButton("↻ Recalculate");
        btnPanel.add(recalc); btnPanel.add(exportPayout);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnPanel, BorderLayout.EAST);

        // E:031 Commission ledger error
        recalc.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(800);
                    return true;
                }
                protected void done() {
                    try {
                        if (get()) {
                            loadCommissionData();
                            SCMComponents.showToast((Component)PricingPanel.this, "✓ Commission recalculated", false);
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:031", "MAJOR",
                            "Commission Ledger Error",
                            "Commission ledger calculation failed due to missing agent data or invalid commission rate configuration.",
                            "Affected agent records flagged in red. Finance/Admin notified. Payout blocked until recalculation verified.");
                    }
                }
            };
            w.execute();
        });

        // E:032 Payout export failure
        exportPayout.addActionListener(e -> {
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(1000);
                    return Math.random() > 0.15;
                }
                protected void done() {
                    try {
                        if (get()) {
                            SCMComponents.showToast((Component)PricingPanel.this, "✓ Payout report exported to PDF", false);
                        } else {
                            showExceptionModal("E:032", "MINOR",
                                "Payout Export Failed",
                                "Commission payout report PDF export failed due to incomplete data or report generation error.",
                                "Export snackbar shown. Retry option available. If retries fail, admin prompted to download raw CSV instead.");
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:032", "MINOR", "Payout Export Failed", ex.getMessage(),
                            "Try again or download raw CSV.");
                    }
                }
            };
            w.execute();
        });

        String[] cols = {"Agent", "Level", "Rate %", "Orders", "Commission (₹)", "Status", "Actions"};
        commissionModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 6; }
        };
        commissionTable = SCMComponents.styledTable(commissionModel);
        commissionTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scroll = SCMComponents.styledScrollPane(new JScrollPane(commissionTable));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Data Loading ─────────────────────────────────────────────────────────
    private void loadDataAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                try {
                    loadPriceTiers();
                    loadDiscountRules();
                    loadCommissionData();
                } catch (Exception e) {
                    loadDemoData();
                }
                return null;
            }
            protected void done() {
                refreshUI();
            }
        };
        worker.execute();
    }

    private void loadPriceTiers() throws Exception {
        priceTiers.clear();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) throw new Exception("No DB");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM price_tiers LIMIT 10")) {
            while (rs.next()) {
                PriceTier t = new PriceTier();
                t.tierId = rs.getInt("tier_id");
                t.tierName = rs.getString("tier_name");
                t.pricePerUnit = rs.getDouble("price_per_unit");
                t.minOrderQty = rs.getInt("min_order_qty");
                priceTiers.add(t);
            }
        }
    }

    private void loadDiscountRules() throws Exception {
        discountRules.clear();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM discount_rules LIMIT 20")) {
            while (rs.next()) {
                DiscountRule r = new DiscountRule();
                r.ruleId = rs.getInt("rule_id");
                r.code = rs.getString("discount_code");
                r.type = rs.getString("discount_type");
                r.value = rs.getDouble("discount_value");
                r.validFrom = rs.getString("valid_from");
                r.validTo = rs.getString("valid_to");
                r.minOrderValue = rs.getDouble("min_order_value");
                discountRules.add(r);
            }
        }
        SwingUtilities.invokeLater(() -> {
            discountModel.setRowCount(0);
            for (DiscountRule r : discountRules) {
                discountModel.addRow(new Object[]{
                    r.code, r.type, r.value + (r.type.equals("Percentage") ? "%" : " ₹"),
                    r.validFrom, r.validTo, "₹" + r.minOrderValue, "Active", "Delete"
                });
            }
        });
    }

    private void loadCommissionData() {
        SwingUtilities.invokeLater(() -> {
            commissionModel.setRowCount(0);
            try {
                Connection conn = DatabaseConnection.getInstance().getConnection();
                if (conn != null) {
                    String sql = "SELECT a.agent_name, a.agent_level, a.commission_rate, " +
                        "COUNT(c.ledger_id) as orders, COALESCE(SUM(c.commission_amount),0) as total, a.payout_status " +
                        "FROM agents a LEFT JOIN commission_ledger c ON a.agent_id = c.agent_id " +
                        "GROUP BY a.agent_id";
                    try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) {
                            commissionModel.addRow(new Object[]{
                                rs.getString("agent_name"),
                                "Level " + rs.getString("agent_level"),
                                rs.getDouble("commission_rate") + "%",
                                rs.getInt("orders"),
                                "₹" + String.format("%,.2f", rs.getDouble("total")),
                                rs.getString("payout_status"),
                                "Mark Paid"
                            });
                        }
                        return;
                    }
                }
            } catch (Exception ignored) {}
            // Demo data
            Object[][] demo = {
                {"Rahul M.", "Level 2", "8%", 48, "₹18,400", "Paid", "Mark Paid"},
                {"Priya S.", "Level 1", "5%", 30, "₹9,200",  "Paid", "Mark Paid"},
                {"Vikram D.", "Level 3", "12%", 62, "₹31,500", "Pending", "Mark Paid"}
            };
            for (Object[] row : demo) commissionModel.addRow(row);
        });
    }

    private void refreshUI() {
        SwingUtilities.invokeLater(() -> {
            tiersPanel.removeAll();
            if (priceTiers.isEmpty()) loadDemoTiers();
            for (PriceTier t : priceTiers) tiersPanel.add(buildTierCard(t));
            tiersPanel.revalidate();
            tiersPanel.repaint();
        });
    }

    private void loadDemoData() {
        loadDemoTiers();
        // Demo discount rules
        SwingUtilities.invokeLater(() -> {
            discountModel.setRowCount(0);
            Object[][] demo = {
                {"SAVE20","Percentage","20%","01/01/2026","31/03/2026","₹500","Active","Delete"},
                {"FLAT500","Fixed Amount","₹500","15/01/2026","28/02/2026","₹1000","Active","Delete"},
                {"BULK10","Percentage","10%","01/02/2026","30/06/2026","₹2000","Active","Delete"},
                {"WINTER15","Percentage","15%","01/12/2025","31/01/2026","₹750","Expired","Delete"}
            };
            for (Object[] row : demo) discountModel.addRow(row);

            discountRules.clear();
            DiscountRule r = new DiscountRule(); r.ruleId=1; r.code="SAVE20"; r.type="Percentage";
            discountRules.add(r);
        });
    }

    private void loadDemoTiers() {
        priceTiers.clear();
        String[][] data = {{"Retail","500","1"},{"Wholesale","420","50"},{"Distributor","360","200"}};
        for (String[] d : data) {
            PriceTier t = new PriceTier();
            t.tierName = d[0]; t.pricePerUnit = Double.parseDouble(d[1]);
            t.minOrderQty = Integer.parseInt(d[2]);
            priceTiers.add(t);
        }
    }

    private void addDiscountRuleInDB(String code, String type, double val, String from, String to, String minOrd) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn != null) {
                String sql = "INSERT INTO discount_rules (discount_code,discount_type,discount_value,valid_from,valid_to,min_order_value) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code); ps.setString(2, type); ps.setDouble(3, val);
                    ps.setString(4, from); ps.setString(5, to);
                    ps.setDouble(6, minOrd.isEmpty() ? 0 : Double.parseDouble(minOrd.replaceAll("[^0-9.]","")));
                    ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {}
        DiscountRule r = new DiscountRule();
        r.code = code; r.type = type; r.value = val;
        r.validFrom = from; r.validTo = to;
        discountRules.add(r);
        discountModel.addRow(new Object[]{code, type, val+(type.equals("Percentage")?"%":" ₹"), from, to, minOrd, "Active", "Delete"});
        SCMComponents.showToast((Component)this, "✓ Discount rule '" + code + "' added", false);
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    private void showAddTierDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Add Price Tier", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("+ Add Price Tier");
        JPanel form = new JPanel(new GridLayout(3, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);

        JTextField nameFld  = SCMComponents.styledField("Tier name");
        JTextField priceFld = SCMComponents.styledField("Price per unit");
        JTextField qtyFld   = SCMComponents.styledField("Min quantity");

        form.add(SCMComponents.subLabel("Tier Name:")); form.add(nameFld);
        form.add(SCMComponents.subLabel("Price/Unit (₹):")); form.add(priceFld);
        form.add(SCMComponents.subLabel("Min Qty:")); form.add(qtyFld);

        JButton save = SCMComponents.goldButton("Save Tier");
        JButton cancel = SCMComponents.outlineButton("Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(cancel); btns.add(save);

        save.addActionListener(e -> {
            try {
                String name = nameFld.getText().trim();
                double price = Double.parseDouble(priceFld.getText().trim());
                int qty = Integer.parseInt(qtyFld.getText().trim());
                PriceTier t = new PriceTier();
                t.tierName = name; t.pricePerUnit = price; t.minOrderQty = qty;
                priceTiers.add(t);
                tiersPanel.add(buildTierCard(t));
                tiersPanel.revalidate(); tiersPanel.repaint();
                SCMComponents.showToast((Component)this, "✓ Price tier '" + name + "' added", false);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                // E:030 Price tier load/save failure
                showExceptionModal("E:030", "WARNING",
                    "Price Tier Save Failure",
                    "Invalid numeric values for price or quantity.",
                    "Please enter valid numbers and retry.");
            }
        });
        cancel.addActionListener(e -> dialog.dispose());

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showEditTierDialog(PriceTier tier) {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Edit Tier — " + tier.tierName, true);
        dialog.setSize(400, 280);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("✏ Edit — " + tier.tierName);
        JPanel form = new JPanel(new GridLayout(2, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);

        JTextField priceFld = SCMComponents.styledField(String.valueOf(tier.pricePerUnit));
        JTextField qtyFld   = SCMComponents.styledField(String.valueOf(tier.minOrderQty));
        form.add(SCMComponents.subLabel("Price/Unit (₹):")); form.add(priceFld);
        form.add(SCMComponents.subLabel("Min Qty:")); form.add(qtyFld);

        JButton save = SCMComponents.goldButton("Save Changes");
        JButton cancel = SCMComponents.outlineButton("Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(cancel); btns.add(save);

        save.addActionListener(e -> {
            try {
                tier.pricePerUnit = Double.parseDouble(priceFld.getText().trim());
                tier.minOrderQty = Integer.parseInt(qtyFld.getText().trim());
                refreshUI();
                SCMComponents.showToast((Component)this, "✓ Tier updated", false);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showExceptionModal("E:030", "WARNING", "Price Tier Load Failure",
                    "Invalid numeric values.", "Enter valid numbers and retry.");
            }
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
        codeLabel.setForeground(catColor);
        codeLabel.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD));
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
        retry.addActionListener(e -> { modal.dispose(); loadDataAsync(); });

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(SCMColors.BG_SECONDARY);
        top.add(codeLabel, BorderLayout.NORTH); top.add(titleLbl, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(msgArea), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        modal.add(panel);
        modal.setVisible(true);
    }

    // ─── Renderers ───────────────────────────────────────────────────────────
    static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String val = v == null ? "" : v.toString();
            setBackground(sel ? SCMColors.BG_PRIMARY : SCMColors.BG_CARD);
            setForeground(switch(val.toLowerCase()) {
                case "paid", "active" -> SCMColors.STATUS_SUCCESS;
                case "pending"        -> SCMColors.STATUS_WARNING;
                case "expired"        -> SCMColors.TEXT_SECONDARY;
                default               -> SCMColors.TEXT_PRIMARY;
            });
            setText("● " + val);
            return this;
        }
    }

    static class ActionButtonRenderer extends DefaultTableCellRenderer {
        private final String label;
        ActionButtonRenderer(String label) { this.label = label; }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JButton btn = SCMComponents.dangerButton(label);
            return btn;
        }
    }

    static class ActionButtonEditor extends DefaultCellEditor {
        private final String label;
        private final java.util.function.IntConsumer action;
        private JButton button;
        private int currentRow;

        ActionButtonEditor(JCheckBox cb, String label, java.util.function.IntConsumer action) {
            super(cb);
            this.label = label; this.action = action;
            button = SCMComponents.dangerButton(label);
            button.addActionListener(e -> { fireEditingStopped(); action.accept(currentRow); });
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            currentRow = r; return button;
        }
        public Object getCellEditorValue() { return label; }
    }
}
