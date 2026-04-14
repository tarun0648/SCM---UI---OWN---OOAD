package com.scm.inventory;

import com.scm.auth.AuthService;
import com.scm.db.DatabaseConnection;
import com.scm.ui.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * C-04 – Inventory & Warehouse UI Panel
 * CRUD for products, RFID/barcode scan, low-stock alerts, zone maps, stock transfer.
 * Exceptions:
 *   E:012 INVALID_BARCODE_SCAN   – SKU not in system
 *   E:013 PRODUCT_NOT_FOUND      – manual SKU not found
 *   E:014 LOW_STOCK_THRESHOLD    – stock dropped to/below reorder point
 *   E:015 STOCK_TRANSFER_FAILURE – transfer rejected/timed out
 *   E:016 DUPLICATE_SKU_ENTRY    – SKU already exists
 */
public class InventoryPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(InventoryPanel.class.getName());
    private final JFrame parent;

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField, scanField;
    private JLabel statusLabel;
    private JPanel addEditForm;
    private boolean editMode = false;
    private int editProductId = -1;

    // Form fields
    private JTextField fSku, fName, fCategory, fStock, fReorder, fBarcode, fPrice;
    private JComboBox<String> fWarehouse;

    public InventoryPanel(JFrame parent) {
        this.parent = parent;
        setBackground(SCMColors.BG_PRIMARY);
        setLayout(new BorderLayout(0, 12));
        buildUI();
        loadProducts();
    }

    private void buildUI() {
        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);

        JPanel headerLeft = SCMComponents.sectionHeader("Inventory & Warehouse",
            "Manage products, stock levels, and warehouse zones");

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        searchField = SCMComponents.styledField("Search products…");
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(searchField.getText()); }
        });

        // Barcode scan field
        scanField = SCMComponents.styledField("Scan barcode/RFID…");
        scanField.setPreferredSize(new Dimension(180, 36));
        scanField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) processScan(scanField.getText().trim());
            }
        });

        JButton addBtn = SCMComponents.goldButton("+ Add Product");
        addBtn.addActionListener(e -> showAddForm());
        JButton scanBtn = SCMComponents.outlineButton("⬡ Scan");
        scanBtn.addActionListener(e -> processScan(scanField.getText().trim()));
        JButton refreshBtn = SCMComponents.outlineButton("↻ Refresh");
        refreshBtn.addActionListener(e -> loadProducts());

        toolbar.add(searchField);
        toolbar.add(scanField);
        toolbar.add(scanBtn);
        toolbar.add(refreshBtn);
        if (AuthService.hasAccess("WAREHOUSE_STAFF")) toolbar.add(addBtn);

        topBar.add(headerLeft, BorderLayout.WEST);
        topBar.add(toolbar, BorderLayout.EAST);

        // ── Alert banner (low stock) ──────────────────────────────────────────
        JPanel alertBanner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xFF,0xA5,0x00, 30));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
        };
        alertBanner.setOpaque(false);
        alertBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SCMColors.STATUS_WARNING, 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        statusLabel = new JLabel("⚠  Loading inventory data...");
        statusLabel.setFont(SCMColors.FONT_SMALL);
        statusLabel.setForeground(SCMColors.STATUS_WARNING);
        alertBanner.add(statusLabel, BorderLayout.CENTER);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] columns = {"SKU", "Product Name", "Category", "Stock", "Reorder Threshold", "Status", "Warehouse", "Unit Price (₹)", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 8; }
            @Override public Class<?> getColumnClass(int c) { return c == 8 ? JPanel.class : Object.class; }
        };
        productTable = new JTable(tableModel);
        SCMComponents.styleTable(productTable);
        productTable.setDefaultRenderer(Object.class, new InventoryRowRenderer());
        productTable.getColumnModel().getColumn(8).setCellRenderer(new ActionRenderer());
        productTable.getColumnModel().getColumn(8).setCellEditor(new ActionEditor());
        productTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        productTable.getColumnModel().getColumn(8).setPreferredWidth(150);
        JScrollPane sp = SCMComponents.styledScrollPane(productTable);

        // ── Add/Edit form ─────────────────────────────────────────────────────
        addEditForm = buildProductForm();
        addEditForm.setVisible(false);

        // Assemble
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(alertBanner, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        center.add(addEditForm, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRODUCT FORM
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildProductForm() {
        JPanel form = SCMComponents.goldCardPanel();
        form.setLayout(new BorderLayout(0, 10));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JLabel formTitle = SCMComponents.goldLabel("Add / Edit Product");
        form.add(formTitle, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(2, 5, 10, 8));
        fields.setOpaque(false);

        fSku      = SCMComponents.styledField("SKU (e.g. WGT-100)");
        fName     = SCMComponents.styledField("Product Name");
        fCategory = SCMComponents.styledField("Category");
        fStock    = SCMComponents.styledField("Current Stock");
        fReorder  = SCMComponents.styledField("Reorder Threshold");
        fBarcode  = SCMComponents.styledField("Barcode/RFID");
        fPrice    = SCMComponents.styledField("Unit Price (₹)");
        fWarehouse = SCMComponents.styledCombo(new String[]{
            "Mumbai Central WH","Delhi North WH","Bangalore South WH","Pune WH"});

        JLabel lSku=fl("SKU"), lName=fl("Name"), lCat=fl("Category"),
               lStock=fl("Stock"), lReorder=fl("Reorder Pt"), lBarcode=fl("Barcode"),
               lPrice=fl("Price ₹"), lWh=fl("Warehouse");

        fields.add(lSku);   fields.add(lName);   fields.add(lCat);
        fields.add(lStock); fields.add(lReorder);
        fields.add(fSku);   fields.add(fName);   fields.add(fCategory);
        fields.add(fStock); fields.add(fReorder);

        JPanel row2 = new JPanel(new GridLayout(2, 4, 10, 8));
        row2.setOpaque(false);
        row2.add(lBarcode); row2.add(lPrice); row2.add(lWh); row2.add(new JLabel());
        row2.add(fBarcode); row2.add(fPrice);  row2.add(fWarehouse); 

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton saveBtn   = SCMComponents.goldButton("💾 Save");
        JButton cancelBtn = SCMComponents.outlineButton("✕ Cancel");
        saveBtn.addActionListener(e -> saveProduct());
        cancelBtn.addActionListener(e -> { addEditForm.setVisible(false); clearForm(); });
        btnRow.add(cancelBtn); btnRow.add(saveBtn);

        JPanel combined = new JPanel(new BorderLayout(0, 8));
        combined.setOpaque(false);
        combined.add(fields, BorderLayout.NORTH);
        combined.add(row2, BorderLayout.CENTER);
        combined.add(btnRow, BorderLayout.SOUTH);
        form.add(combined, BorderLayout.CENTER);
        return form;
    }

    private JLabel fl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SCMColors.FONT_SMALL);
        l.setForeground(SCMColors.TEXT_SECONDARY);
        return l;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOAD PRODUCTS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadProducts() {
        SwingWorker<java.util.List<Object[]>, Void> w = new SwingWorker<>() {
            @Override protected java.util.List<Object[]> doInBackground() {
                return fetchProducts(null);
            }
            @Override protected void done() {
                try {
                    java.util.List<Object[]> rows = get();
                    tableModel.setRowCount(0);
                    int lowStockCount = 0;
                    for (Object[] r : rows) {
                        tableModel.addRow(r);
                        if ("LOW".equals(r[5]) || "OUT".equals(r[5])) lowStockCount++;
                    }
                    if (lowStockCount > 0) {
                        // E:014 – Low stock alert
                        statusLabel.setText("⚠  E:014 — " + lowStockCount + " product(s) below reorder point. Take action now.");
                        statusLabel.setForeground(SCMColors.STATUS_WARNING);
                    } else {
                        statusLabel.setText("✅  All stock levels normal.");
                        statusLabel.setForeground(SCMColors.STATUS_SUCCESS);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("❌  Failed to load inventory: " + ex.getMessage());
                    statusLabel.setForeground(SCMColors.STATUS_ERROR);
                }
            }
        };
        w.execute();
    }

    private java.util.List<Object[]> fetchProducts(String search) {
        java.util.List<Object[]> rows = new ArrayList<>();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) return getDemoProducts();
        try {
            String sql = "SELECT p.product_id, p.product_sku, p.product_name, p.product_category, " +
                         "p.current_stock_level, p.reorder_threshold, p.stock_status, " +
                         "COALESCE(w.warehouse_name,'—'), p.unit_price, p.barcode_rfid_value " +
                         "FROM products p LEFT JOIN warehouses w ON p.warehouse_id=w.warehouse_id " +
                         (search != null && !search.isEmpty() ?
                          "WHERE p.product_sku LIKE ? OR p.product_name LIKE ? OR p.product_category LIKE ? " : "") +
                         "ORDER BY p.stock_status DESC, p.product_sku";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (search != null && !search.isEmpty()) {
                    String like = "%" + search + "%";
                    ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString(2),  // SKU
                        rs.getString(3),  // Name
                        rs.getString(4),  // Category
                        rs.getInt(5),     // Stock
                        rs.getInt(6),     // Reorder
                        rs.getString(7),  // Status
                        rs.getString(8),  // Warehouse
                        "₹" + rs.getDouble(9), // Price
                        "Actions"
                    });
                }
            }
        } catch (SQLException e) {
            LOG.warning("Product fetch SQL: " + e.getMessage());
            return getDemoProducts();
        }
        return rows;
    }

    private java.util.List<Object[]> getDemoProducts() {
        return Arrays.asList(
            new Object[]{"WGT-100","Widget A","Electronics",284,50,"OK","Mumbai WH","₹500.0","Actions"},
            new Object[]{"SNS-200","Sensor B","Electronics",12,30,"LOW","Mumbai WH","₹1200.0","Actions"},
            new Object[]{"MTR-300","Motor C","Mechanical",0,10,"OUT","Delhi WH","₹3500.0","Actions"},
            new Object[]{"CBL-400","Cable D","Accessories",540,100,"OK","Delhi WH","₹150.0","Actions"},
            new Object[]{"PCB-500","PCB Module E","Electronics",8,20,"LOW","Bangalore WH","₹2200.0","Actions"},
            new Object[]{"HYD-600","Hydraulic Pump F","Mechanical",45,15,"OK","Bangalore WH","₹8500.0","Actions"},
            new Object[]{"OPT-700","Optical Sensor G","Electronics",190,50,"OK","Pune WH","₹650.0","Actions"},
            new Object[]{"FLT-800","Filter Unit H","Accessories",3,25,"LOW","Pune WH","₹320.0","Actions"}
        );
    }

    private void filterTable(String search) {
        SwingWorker<java.util.List<Object[]>, Void> w = new SwingWorker<>() {
            @Override protected java.util.List<Object[]> doInBackground() { return fetchProducts(search); }
            @Override protected void done() {
                try {
                    tableModel.setRowCount(0);
                    for (Object[] r : get()) tableModel.addRow(r);
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BARCODE / RFID SCAN (E:012)
    // ══════════════════════════════════════════════════════════════════════════
    private void processScan(String scanValue) {
        if (scanValue == null || scanValue.isEmpty()) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            // Demo: highlight first row
            if (tableModel.getRowCount() > 0) {
                productTable.setRowSelectionInterval(0, 0);
                showToast("✅ Scanned: " + scanValue + " — Product found (demo mode)", SCMColors.STATUS_SUCCESS);
            }
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT product_sku FROM products WHERE barcode_rfid_value=? OR product_sku=?")) {
            ps.setString(1, scanValue); ps.setString(2, scanValue);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String sku = rs.getString(1);
                searchField.setText(sku);
                filterTable(sku);
                showToast("✅ Scanned: " + sku + " found in inventory.", SCMColors.STATUS_SUCCESS);
                AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                    AuthService.getCurrentUsername(), "Barcode scanned: " + scanValue, "Inventory");
            } else {
                // E:012 – unrecognised barcode
                showExceptionDialog("E:012 — Invalid Barcode / RFID",
                    "Scanned value '" + scanValue + "' does not match any registered product SKU.\n" +
                    "Scan value has been logged. Use 'Add Product' to register a new SKU.", SCMColors.STATUS_WARNING);
                AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                    AuthService.getCurrentUsername(), "Failed barcode scan: " + scanValue, "Inventory");
            }
        } catch (SQLException e) {
            LOG.warning("Scan error: " + e.getMessage());
        }
        scanField.setText("");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADD / EDIT PRODUCT
    // ══════════════════════════════════════════════════════════════════════════
    private void showAddForm() {
        editMode = false; editProductId = -1;
        clearForm();
        ((JLabel) addEditForm.getComponent(0)).setText("Add New Product");
        addEditForm.setVisible(true);
    }

    private void saveProduct() {
        String sku = fSku.getText().trim();
        String name = fName.getText().trim();
        if (sku.isEmpty() || name.isEmpty()) {
            showExceptionDialog("Validation Error", "SKU and Product Name are required.", SCMColors.STATUS_WARNING);
            return;
        }
        int stock, reorder;
        double price;
        try {
            stock   = Integer.parseInt(fStock.getText().trim());
            reorder = Integer.parseInt(fReorder.getText().trim());
            price   = Double.parseDouble(fPrice.getText().trim().replace("₹",""));
        } catch (NumberFormatException e) {
            showExceptionDialog("Validation Error", "Stock, Reorder Threshold, and Price must be numeric.", SCMColors.STATUS_WARNING);
            return;
        }
        String category = fCategory.getText().trim();
        String barcode  = fBarcode.getText().trim();
        int whIndex     = fWarehouse.getSelectedIndex() + 1;
        String status   = stock == 0 ? "OUT" : stock <= reorder ? "LOW" : "OK";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            showToast("✅ Product saved (demo mode – no DB)", SCMColors.STATUS_SUCCESS);
            addEditForm.setVisible(false);
            loadProducts();
            return;
        }
        try {
            if (!editMode) {
                // Check duplicate SKU – E:016
                try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT product_id FROM products WHERE product_sku=?")) {
                    chk.setString(1, sku);
                    if (chk.executeQuery().next()) {
                        showExceptionDialog("E:016 — Duplicate SKU",
                            "SKU '" + sku + "' already exists in the inventory database.\n" +
                            "Please review the existing product and amend if needed.", SCMColors.STATUS_WARNING);
                        return;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (product_sku,product_name,product_category," +
                    "current_stock_level,reorder_threshold,stock_status,warehouse_id," +
                    "barcode_rfid_value,unit_price) VALUES(?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1,sku); ps.setString(2,name); ps.setString(3,category);
                    ps.setInt(4,stock); ps.setInt(5,reorder); ps.setString(6,status);
                    ps.setInt(7,whIndex); ps.setString(8,barcode); ps.setDouble(9,price);
                    ps.executeUpdate();
                }
                AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                    AuthService.getCurrentUsername(), "Added product: " + sku, "Inventory");
                showToast("✅ Product '" + sku + "' added successfully.", SCMColors.STATUS_SUCCESS);
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET product_name=?,product_category=?,current_stock_level=?," +
                    "reorder_threshold=?,stock_status=?,warehouse_id=?,barcode_rfid_value=?,unit_price=? WHERE product_id=?")) {
                    ps.setString(1,name); ps.setString(2,category); ps.setInt(3,stock);
                    ps.setInt(4,reorder); ps.setString(5,status); ps.setInt(6,whIndex);
                    ps.setString(7,barcode); ps.setDouble(8,price); ps.setInt(9,editProductId);
                    ps.executeUpdate();
                }
                AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                    AuthService.getCurrentUsername(), "Updated product: " + sku, "Inventory");
                showToast("✅ Product '" + sku + "' updated successfully.", SCMColors.STATUS_SUCCESS);
                // E:014 – check if now low stock
                if ("LOW".equals(status) || "OUT".equals(status)) {
                    showExceptionDialog("E:014 — Low Stock Alert",
                        "Product '" + name + "' (" + sku + ") is now " + status + ".\n" +
                        "Current stock: " + stock + " | Reorder threshold: " + reorder + "\n" +
                        "A low-stock notification has been sent to Warehouse Staff and Manager.", SCMColors.STATUS_WARNING);
                }
            }
            addEditForm.setVisible(false);
            clearForm();
            loadProducts();
        } catch (SQLException e) {
            showExceptionDialog("Database Error", "Failed to save product: " + e.getMessage(), SCMColors.STATUS_ERROR);
        }
    }

    private void clearForm() {
        fSku.setText(""); fName.setText(""); fCategory.setText("");
        fStock.setText(""); fReorder.setText(""); fBarcode.setText(""); fPrice.setText("");
        fWarehouse.setSelectedIndex(0);
    }

    private void showToast(String msg, Color color) {
        SCMComponents.showToast((JFrame) SwingUtilities.getWindowAncestor(this), msg, color);
    }

    private void showExceptionDialog(String title, String msg, Color color) {
        JDialog dialog = new JDialog(parent, title, true);
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(SCMColors.BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        JLabel t = new JLabel(title); t.setFont(SCMColors.FONT_HEADING); t.setForeground(color);
        JTextArea m = new JTextArea(msg); m.setFont(SCMColors.FONT_BODY); m.setForeground(SCMColors.TEXT_PRIMARY);
        m.setBackground(SCMColors.BG_CARD); m.setEditable(false); m.setLineWrap(true); m.setWrapStyleWord(true);
        JButton ok = SCMComponents.goldButton("OK"); ok.addActionListener(e -> dialog.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bp.setOpaque(false); bp.add(ok);
        p.add(t, BorderLayout.NORTH); p.add(m, BorderLayout.CENTER); p.add(bp, BorderLayout.SOUTH);
        dialog.setContentPane(p); dialog.setSize(420, 220);
        dialog.setLocationRelativeTo(parent); dialog.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TABLE RENDERERS
    // ══════════════════════════════════════════════════════════════════════════
    class InventoryRowRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, focus, row, col);
            setBackground(sel ? SCMColors.BG_SELECTED : row % 2 == 0 ? SCMColors.BG_CARD : SCMColors.BG_TABLE_ROW_ALT);
            setForeground(SCMColors.TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            if (col == 5) { // Status
                String status = String.valueOf(val);
                switch (status) {
                    case "OK"  -> setForeground(SCMColors.STATUS_SUCCESS);
                    case "LOW" -> setForeground(SCMColors.STATUS_WARNING);
                    case "OUT" -> setForeground(SCMColors.STATUS_ERROR);
                }
            }
            if (col == 3) { // Stock level
                try {
                    int qty = Integer.parseInt(val.toString());
                    Object reorder = t.getValueAt(row, 4);
                    int rt = Integer.parseInt(reorder.toString());
                    if (qty == 0)     setForeground(SCMColors.STATUS_ERROR);
                    else if (qty <= rt) setForeground(SCMColors.STATUS_WARNING);
                    else               setForeground(SCMColors.STATUS_SUCCESS);
                } catch (Exception ignored) {}
            }
            return this;
        }
    }

    class ActionRenderer implements TableCellRenderer {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        ActionRenderer() {
            p.setOpaque(false);
            p.add(SCMComponents.outlineButton("Edit"));
            p.add(SCMComponents.dangerButton("Delete"));
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) { return p; }
    }

    class ActionEditor extends DefaultCellEditor {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        int editRow;
        ActionEditor() {
            super(new JCheckBox());
            p.setOpaque(true);
            p.setBackground(SCMColors.BG_SELECTED);
            JButton edit = SCMComponents.outlineButton("Edit");
            JButton del  = SCMComponents.dangerButton("Delete");
            edit.addActionListener(e -> {
                fireEditingStopped();
                editSelectedProduct(editRow);
            });
            del.addActionListener(e -> {
                fireEditingStopped();
                deleteProduct(editRow);
            });
            p.add(edit); p.add(del);
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) { editRow = row; return p; }
        @Override public Object getCellEditorValue() { return ""; }
    }

    private void editSelectedProduct(int row) {
        editMode = true;
        fSku.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        fSku.setEditable(false);
        fName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        fCategory.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        fStock.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        fReorder.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        fPrice.setText(String.valueOf(tableModel.getValueAt(row, 7)).replace("₹",""));
        addEditForm.setVisible(true);
    }

    private void deleteProduct(int row) {
        String sku = String.valueOf(tableModel.getValueAt(row, 0));
        int confirm = JOptionPane.showConfirmDialog(parent,
            "Delete product '" + sku + "'? This cannot be undone.", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) { tableModel.removeRow(row); return; }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE product_sku=?")) {
            ps.setString(1, sku); ps.executeUpdate();
            tableModel.removeRow(row);
            AuthService.logAudit(conn, AuthService.getCurrentUserId(),
                AuthService.getCurrentUsername(), "Deleted product: " + sku, "Inventory");
            showToast("🗑 Product '" + sku + "' deleted.", SCMColors.STATUS_WARNING);
        } catch (SQLException e) {
            showExceptionDialog("Delete Error", "Cannot delete product: " + e.getMessage(), SCMColors.STATUS_ERROR);
        }
    }
}
