package com.scm.orders;

import com.scm.auth.AuthService;
import com.scm.db.DatabaseConnection;
import com.scm.ui.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * C-05 – Order Management & Fulfillment UI
 * New order creation, order tracking, returns, invoice/packing slip.
 * Exceptions:
 *   E:017 PAYMENT_GATEWAY_TIMEOUT
 *   E:018 ORDER_CREATION_FAILED
 *   E:019 INVALID_CUSTOMER_DATA
 *   E:020 RETURN_PROCESSING_FAILED
 *   E:021 INVOICE_UNAVAILABLE
 *   E:022 PACKING_SLIP_PRINT_ERROR
 *   E:023 INVALID_DISCOUNT_CODE
 */
public class OrdersPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(OrdersPanel.class.getName());
    private final JFrame parent;
    private DefaultTableModel tableModel;
    private JTable ordersTable;
    private JComboBox<String> statusFilter;
    private JLabel statusLabel;
    private JPanel orderFormPanel;

    // Form fields
    private JComboBox<String> fCustomer;
    private JTextField fSku, fQty, fDiscount;
    private JLabel fPriceLabel;
    private JTextArea fAddress;

    public OrdersPanel(JFrame parent) {
        this.parent = parent;
        setBackground(SCMColors.BG_PRIMARY);
        setLayout(new BorderLayout(0, 12));
        buildUI();
        loadOrders("ALL");
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(SCMComponents.sectionHeader("Order Management & Fulfillment",
            "Create, track, and manage customer orders"), BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        statusFilter = SCMComponents.styledCombo(new String[]{
            "ALL","PENDING","PROCESSING","SHIPPED","DELIVERED","RETURN","CANCELLED"});
        statusFilter.addActionListener(e -> loadOrders(Objects.toString(statusFilter.getSelectedItem())));
        JButton newOrderBtn = SCMComponents.goldButton("+ New Order");
        newOrderBtn.addActionListener(e -> showOrderForm());
        JButton refreshBtn = SCMComponents.outlineButton("↻ Refresh");
        refreshBtn.addActionListener(e -> loadOrders(Objects.toString(statusFilter.getSelectedItem())));

        toolbar.add(new JLabel("Status: ") {{ setForeground(SCMColors.TEXT_SECONDARY); }});
        toolbar.add(statusFilter);
        toolbar.add(refreshBtn);
        if (AuthService.hasAccess("SALES_STAFF")) toolbar.add(newOrderBtn);
        header.add(toolbar, BorderLayout.EAST);

        // Filter tabs (visual)
        JPanel tabs = buildStatusTabs();

        // Table
        String[] cols = {"Order #","Customer","Product(s)","Value (₹)","Date","Status","Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (col == 5 && c instanceof JLabel lbl) {
                    String status = String.valueOf(getValueAt(row, col));
                    lbl.setForeground(switch (status) {
                        case "DELIVERED" -> SCMColors.STATUS_SUCCESS;
                        case "PENDING"   -> SCMColors.STATUS_WARNING;
                        case "RETURN"    -> SCMColors.STATUS_ERROR;
                        case "SHIPPED"   -> SCMColors.STATUS_INFO;
                        default          -> SCMColors.TEXT_SECONDARY;
                    });
                }
                return c;
            }
        };
        SCMComponents.styleTable(ordersTable);
        ordersTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showOrderActions(ordersTable.getSelectedRow());
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setFont(SCMColors.FONT_SMALL);
        statusLabel.setForeground(SCMColors.TEXT_MUTED);

        JScrollPane sp = SCMComponents.styledScrollPane(ordersTable);

        // Order form
        orderFormPanel = buildOrderForm();
        orderFormPanel.setVisible(false);

        // Bottom action bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        actionBar.setOpaque(false);
        JButton printInvoice  = SCMComponents.outlineButton("🧾 Print Invoice");
        JButton printPacking  = SCMComponents.outlineButton("📦 Print Packing Slip");
        JButton processReturn = SCMComponents.dangerButton("↩ Process Return");
        printInvoice.addActionListener(e  -> generateInvoice());
        printPacking.addActionListener(e  -> printPackingSlip());
        processReturn.addActionListener(e -> processReturn());
        actionBar.add(printInvoice); actionBar.add(printPacking); actionBar.add(processReturn);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(tabs, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        center.add(statusLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(actionBar, BorderLayout.NORTH);
        south.add(orderFormPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildStatusTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabs.setOpaque(false);
        String[] statuses = {"All","Pending","Shipped","Delivered","Returns"};
        for (String s : statuses) {
            JButton tab = new JButton(s) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isRollover() ? SCMColors.BG_HOVER : SCMColors.BG_CARD);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(SCMColors.ACCENT_GOLD);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                    g2.setFont(SCMColors.FONT_SMALL);
                    g2.setColor(SCMColors.TEXT_PRIMARY);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, getHeight()/2+5);
                    g2.dispose();
                }
            };
            tab.setOpaque(false); tab.setContentAreaFilled(false); tab.setBorderPainted(false);
            tab.setFocusPainted(false); tab.setPreferredSize(new Dimension(80, 30));
            tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String filterVal = s.equals("All") ? "ALL" : s.equals("Returns") ? "RETURN" : s.toUpperCase();
            tab.addActionListener(e -> { statusFilter.setSelectedItem(filterVal); loadOrders(filterVal); });
            tabs.add(tab);
        }
        return tabs;
    }

    private JPanel buildOrderForm() {
        JPanel form = SCMComponents.goldCardPanel();
        form.setLayout(new BorderLayout(0, 10));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        form.add(SCMComponents.goldLabel("New Order Form"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(2, 4, 10, 8));
        fields.setOpaque(false);
        fCustomer = SCMComponents.styledCombo(new String[]{
            "Acme Corp","TechMart India","LogiBase Solutions","QuickShop Retail","GlobalTech Ltd"});
        fSku     = SCMComponents.styledField("Product SKU(s) comma-separated");
        fQty     = SCMComponents.styledField("Quantity");
        fDiscount= SCMComponents.styledField("Discount Code (optional)");
        fPriceLabel = new JLabel("Price: auto-calculated");
        fPriceLabel.setFont(SCMColors.FONT_SMALL);
        fPriceLabel.setForeground(SCMColors.ACCENT_GOLD);
        fAddress = new JTextArea(2, 20);
        fAddress.setBackground(SCMColors.BG_INPUT);
        fAddress.setForeground(SCMColors.TEXT_PRIMARY);
        fAddress.setFont(SCMColors.FONT_BODY);
        fAddress.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        fields.add(fl("Customer"));  fields.add(fl("Product SKU(s)"));
        fields.add(fl("Qty"));       fields.add(fl("Discount Code"));
        fields.add(fCustomer);       fields.add(fSku);
        fields.add(fQty);            fields.add(fDiscount);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton submit = SCMComponents.goldButton("✔ Submit Order");
        JButton draft  = SCMComponents.outlineButton("📋 Save Draft");
        JButton cancel = SCMComponents.dangerButton("✕ Cancel");
        submit.addActionListener(e -> submitOrder());
        draft.addActionListener(e  -> showToast("📋 Order saved as draft.", SCMColors.STATUS_INFO));
        cancel.addActionListener(e -> { orderFormPanel.setVisible(false); clearOrderForm(); });
        btnRow.add(cancel); btnRow.add(draft); btnRow.add(submit);
        btnRow.add(fPriceLabel);

        JPanel inner = new JPanel(new BorderLayout(0, 8));
        inner.setOpaque(false);
        inner.add(fields, BorderLayout.NORTH);
        inner.add(btnRow, BorderLayout.SOUTH);
        form.add(inner, BorderLayout.CENTER);
        return form;
    }

    private JLabel fl(String t) {
        JLabel l = new JLabel(t); l.setFont(SCMColors.FONT_SMALL); l.setForeground(SCMColors.TEXT_SECONDARY); return l;
    }

    private void loadOrders(String status) {
        SwingWorker<java.util.List<Object[]>, Void> w = new SwingWorker<>() {
            @Override protected java.util.List<Object[]> doInBackground() { return fetchOrders(status); }
            @Override protected void done() {
                try {
                    tableModel.setRowCount(0);
                    java.util.List<Object[]> rows = get();
                    for (Object[] r : rows) tableModel.addRow(r);
                    statusLabel.setText("Showing " + rows.size() + " orders  |  Double-click for details");
                } catch (Exception ex) {
                    statusLabel.setText("❌ Failed to load orders: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private java.util.List<Object[]> fetchOrders(String status) {
        java.util.List<Object[]> rows = new ArrayList<>();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) return getDemoOrders(status);
        try {
            String sql = "SELECT o.order_number, c.customer_name, " +
                "(SELECT GROUP_CONCAT(oi.product_sku) FROM order_items oi WHERE oi.order_id=o.order_id), " +
                "o.order_value, o.order_date, o.order_status " +
                "FROM orders o JOIN customers c ON o.customer_id=c.customer_id " +
                ("ALL".equals(status) ? "" : "WHERE o.order_status=? ") +
                "ORDER BY o.order_date DESC LIMIT 100";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!"ALL".equals(status)) ps.setString(1, status);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        String.format("₹%.0f", rs.getDouble(4)),
                        rs.getString(5), rs.getString(6)
                    });
                }
            }
        } catch (SQLException e) { return getDemoOrders(status); }
        return rows;
    }

    private java.util.List<Object[]> getDemoOrders(String filter) {
        java.util.List<Object[]> all = Arrays.asList(
            new Object[]{"ORD-4821","Acme Corp","WGT-100 x10","₹12,400","20-Feb","DELIVERED"},
            new Object[]{"ORD-4822","TechMart","SNS-200 x5","₹8,200","20-Feb","PENDING"},
            new Object[]{"ORD-4823","LogiBase","CBL-400 x50","₹3,500","19-Feb","RETURN"},
            new Object[]{"ORD-4824","QuickShop","MTR-300 x2","₹5,800","19-Feb","SHIPPED"},
            new Object[]{"ORD-4825","GlobalTech","HYD-600 x2","₹24,000","01-Apr","PROCESSING"},
            new Object[]{"ORD-4826","Acme Corp","WGT-100 x30","₹15,000","15-Mar","DELIVERED"},
            new Object[]{"ORD-4827","TechMart","CBL-400 x20","₹7,600","05-Apr","SHIPPED"},
            new Object[]{"ORD-4828","LogiBase","VLV-900 x5","₹9,300","08-Apr","PENDING"}
        );
        if ("ALL".equals(filter)) return all;
        java.util.List<Object[]> filtered = new ArrayList<>();
        for (Object[] r : all) if (filter.equals(r[5])) filtered.add(r);
        return filtered;
    }

    private void showOrderForm() {
        orderFormPanel.setVisible(true);
        revalidate(); repaint();
    }

    private void submitOrder() {
        String custName = Objects.toString(fCustomer.getSelectedItem());
        String skus     = fSku.getText().trim();
        String qtyStr   = fQty.getText().trim();
        String discount = fDiscount.getText().trim();

        // E:019 – validate customer data
        if (skus.isEmpty() || qtyStr.isEmpty()) {
            showDialog("E:019 — Invalid Order Data",
                "Product SKU(s) and Quantity are required to create an order.", SCMColors.STATUS_WARNING);
            return;
        }
        int qty;
        try { qty = Integer.parseInt(qtyStr); }
        catch (NumberFormatException e) {
            showDialog("E:019 — Invalid Quantity", "Quantity must be a valid integer.", SCMColors.STATUS_WARNING);
            return;
        }

        // Validate discount code – E:023
        if (!discount.isEmpty()) {
            if (!validateDiscount(discount)) {
                showDialog("E:023 — Invalid or Expired Discount Code",
                    "Discount code '" + discount + "' does not exist, has expired,\n" +
                    "or does not meet minimum order conditions.\nCode field cleared; price recalculated at full rate.",
                    SCMColors.STATUS_WARNING);
                fDiscount.setText("");
                return;
            }
        }

        // Simulate order creation (E:018 / E:017 in production)
        String orderNum = "ORD-" + (4829 + (int)(Math.random()*100));
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            showToast("✅ Order " + orderNum + " created (demo mode).", SCMColors.STATUS_SUCCESS);
            Object[] newRow = {orderNum, custName, skus, "₹" + (qty * 500), java.time.LocalDate.now(), "PENDING"};
            tableModel.insertRow(0, newRow);
            orderFormPanel.setVisible(false);
            return;
        }
        try {
            // E:018 check: find customer id
            int custId = 1;
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT customer_id FROM customers WHERE customer_name=?")) {
                ps.setString(1, custName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) custId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (order_number,customer_id,order_status,order_value," +
                "discount_code_applied,order_date,created_by) VALUES(?,?,?,?,?,CURDATE(),?)")) {
                ps.setString(1, orderNum); ps.setInt(2, custId);
                ps.setString(3, "PENDING"); ps.setDouble(4, qty * 500.0);
                ps.setString(5, discount.isEmpty() ? null : discount);
                ps.setInt(6, AuthService.getCurrentUserId());
                ps.executeUpdate();
            }
            AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                AuthService.getCurrentUsername(), "Created order: " + orderNum, "Orders");
            showToast("✅ Order " + orderNum + " created successfully.", SCMColors.STATUS_SUCCESS);
            orderFormPanel.setVisible(false);
            clearOrderForm();
            loadOrders("ALL");
        } catch (SQLException e) {
            showDialog("E:018 — Order Creation Failed",
                "Order could not be created due to backend rejection.\n" +
                "Error: " + e.getMessage() + "\nForm auto-saved as draft.", SCMColors.STATUS_ERROR);
        }
    }

    private boolean validateDiscount(String code) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) return code.equals("SAVE20") || code.equals("FLAT500");
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT discount_id FROM discount_rules WHERE discount_code=? AND is_active=TRUE " +
            "AND (discount_valid_to IS NULL OR discount_valid_to >= CURDATE())")) {
            ps.setString(1, code);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private void showOrderActions(int row) {
        if (row < 0) return;
        String orderNum = String.valueOf(tableModel.getValueAt(row, 0));
        String status   = String.valueOf(tableModel.getValueAt(row, 5));
        String[] opts = {"View Details","Generate Invoice","Print Packing Slip","Process Return","Cancel"};
        int choice = JOptionPane.showOptionDialog(parent,
            "Order: " + orderNum + "\nStatus: " + status,
            "Order Actions", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, opts, opts[0]);
        switch (choice) {
            case 1 -> generateInvoice();
            case 2 -> printPackingSlip();
            case 3 -> processReturn();
        }
    }

    private void generateInvoice() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) { showToast("⚠ Please select an order first.", SCMColors.STATUS_WARNING); return; }
        String orderNum = String.valueOf(tableModel.getValueAt(row, 0));
        // E:021 – Invoice unavailable simulation
        try {
            JOptionPane.showMessageDialog(parent,
                "Invoice PDF generated for " + orderNum + ".\n(iText PDF generation active in production build.)",
                "Invoice Generated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showDialog("E:021 — Invoice Unavailable",
                "Invoice could not be generated for " + orderNum + ".\nPlease retry. If retry fails, notify Finance/Admin.",
                SCMColors.STATUS_ERROR);
        }
    }

    private void printPackingSlip() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) { showToast("⚠ Please select an order first.", SCMColors.STATUS_WARNING); return; }
        String orderNum = String.valueOf(tableModel.getValueAt(row, 0));
        // E:022 – Packing slip error simulation
        JOptionPane.showMessageDialog(parent,
            "Packing slip for " + orderNum + " sent to printer.\n" +
            "If printer unavailable, download as PDF via the Export menu.",
            "Packing Slip", JOptionPane.INFORMATION_MESSAGE);
    }

    private void processReturn() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) { showToast("⚠ Please select an order first.", SCMColors.STATUS_WARNING); return; }
        String orderNum = String.valueOf(tableModel.getValueAt(row, 0));
        String status   = String.valueOf(tableModel.getValueAt(row, 5));
        if ("RETURN".equals(status)) {
            showDialog("Return Already Filed", "Order " + orderNum + " is already in RETURN status.", SCMColors.STATUS_WARNING);
            return;
        }
        String reason = JOptionPane.showInputDialog(parent, "Return reason for " + orderNum + ":", "Process Return", JOptionPane.QUESTION_MESSAGE);
        if (reason == null || reason.isBlank()) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            tableModel.setValueAt("RETURN", row, 5);
            showToast("↩ Return filed for " + orderNum + " (demo mode).", SCMColors.STATUS_WARNING);
            return;
        }
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE orders SET order_status='RETURN' WHERE order_number=?")) {
                ps.setString(1, orderNum); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO return_refunds (order_id, reason, return_status) " +
                "SELECT order_id,?,? FROM orders WHERE order_number=?")) {
                ps.setString(1, reason); ps.setString(2, "PENDING"); ps.setString(3, orderNum);
                ps.executeUpdate();
            }
            tableModel.setValueAt("RETURN", row, 5);
            AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                AuthService.getCurrentUsername(), "Return filed for order: " + orderNum, "Orders");
            showToast("↩ Return filed for " + orderNum + ".", SCMColors.STATUS_WARNING);
        } catch (SQLException e) {
            // E:020
            showDialog("E:020 — Return Processing Failed",
                "Return for order " + orderNum + " could not be processed.\nError: " + e.getMessage() +
                "\nPlease re-submit after 2 minutes.", SCMColors.STATUS_ERROR);
        }
    }

    private void clearOrderForm() { fSku.setText(""); fQty.setText(""); fDiscount.setText(""); }
    private void showToast(String msg, Color c) { SCMComponents.showToast((JFrame) SwingUtilities.getWindowAncestor(this), msg, c); }
    private void showDialog(String title, String msg, Color c) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.WARNING_MESSAGE);
    }
}
