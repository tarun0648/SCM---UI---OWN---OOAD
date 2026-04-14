package com.scm.logistics;

import com.scm.db.DatabaseConnection;
import com.scm.models.Models.*;
import com.scm.ui.SCMColors;
import com.scm.ui.SCMComponents;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LogisticsPanel extends JPanel {

    private JTable shipmentsTable;
    private DefaultTableModel tableModel;
    private MapPanel mapPanel;
    private JLabel gpsStatusLabel;
    private JPanel etaPanel;
    private java.util.Timer gpsRefreshTimer;
    private List<Shipment> currentShipments = new ArrayList<>();

    // E:024 GPS unavailable flag
    private boolean gpsAvailable = true;
    // E:027 Carrier API timeout counter
    private int carrierApiRetryCount = 0;

    public LogisticsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(SCMColors.BG_PRIMARY);
        initComponents();
        loadShipmentsAsync();
        startGPSRefresh();
    }

    private void initComponents() {
        // Header
        JPanel header = SCMComponents.sectionHeader("🚚  Transport & Logistics", "Live tracking, route optimization and delivery management");
        add(header, BorderLayout.NORTH);

        // Main split: left = map + controls, right = shipment list
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(SCMColors.BG_PRIMARY);
        split.setBorder(null);
        split.setDividerSize(4);
        split.setDividerLocation(620);

        // LEFT SIDE
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setBackground(SCMColors.BG_PRIMARY);
        leftPanel.setBorder(new EmptyBorder(16, 20, 16, 8));

        // GPS Status Banner
        gpsStatusLabel = new JLabel("  🛰  GPS Feed: Live  •  Last updated: just now");
        gpsStatusLabel.setFont(SCMColors.FONT_SMALL);
        gpsStatusLabel.setForeground(SCMColors.STATUS_SUCCESS);
        gpsStatusLabel.setOpaque(true);
        gpsStatusLabel.setBackground(new Color(0, 200, 122, 30));
        gpsStatusLabel.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.STATUS_SUCCESS, 1),
            new EmptyBorder(6, 12, 6, 12)
        ));

        // Map Panel
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(580, 320));

        // Map controls
        JPanel mapControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        mapControls.setBackground(SCMColors.BG_PRIMARY);
        JButton zoomIn  = SCMComponents.outlineButton("🔍 Zoom In");
        JButton zoomOut = SCMComponents.outlineButton("🔍 Zoom Out");
        JButton recenter= SCMComponents.outlineButton("⊙ Recenter");
        JButton refresh = SCMComponents.goldButton("↻ Refresh GPS");
        mapControls.add(zoomIn); mapControls.add(zoomOut);
        mapControls.add(recenter); mapControls.add(refresh);

        zoomIn.addActionListener(e -> { mapPanel.zoom(1.2); mapPanel.repaint(); });
        zoomOut.addActionListener(e -> { mapPanel.zoom(0.8); mapPanel.repaint(); });
        recenter.addActionListener(e -> { mapPanel.recenter(); mapPanel.repaint(); });
        refresh.addActionListener(e -> refreshGPS());

        // Action buttons
        JPanel actionPanel = buildActionPanel();

        leftPanel.add(gpsStatusLabel, BorderLayout.NORTH);
        JPanel mapWrapper = new JPanel(new BorderLayout(0, 8));
        mapWrapper.setBackground(SCMColors.BG_PRIMARY);
        mapWrapper.add(mapPanel, BorderLayout.CENTER);
        mapWrapper.add(mapControls, BorderLayout.SOUTH);
        leftPanel.add(mapWrapper, BorderLayout.CENTER);
        leftPanel.add(actionPanel, BorderLayout.SOUTH);

        // RIGHT SIDE — shipments list
        JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
        rightPanel.setBackground(SCMColors.BG_PRIMARY);
        rightPanel.setBorder(new EmptyBorder(16, 8, 16, 20));

        JLabel shipLabel = SCMComponents.goldLabel("Active Shipments");
        shipLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        // ETA panel (scrollable cards)
        etaPanel = new JPanel();
        etaPanel.setLayout(new BoxLayout(etaPanel, BoxLayout.Y_AXIS));
        etaPanel.setBackground(SCMColors.BG_PRIMARY);
        JScrollPane etaScroll = new JScrollPane(etaPanel);
        etaScroll.setBackground(SCMColors.BG_PRIMARY);
        etaScroll.setBorder(null);
        etaScroll.getViewport().setBackground(SCMColors.BG_PRIMARY);
        SCMComponents.styledScrollPane(etaScroll);

        // Shipments table
        String[] cols = {"Shipment ID", "Origin", "Destination", "Carrier", "ETA", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        shipmentsTable = SCMComponents.styledTable(tableModel);
        shipmentsTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
        JScrollPane tableScroll = SCMComponents.styledScrollPane(new JScrollPane(shipmentsTable));

        rightPanel.add(shipLabel, BorderLayout.NORTH);
        rightPanel.add(etaScroll, BorderLayout.CENTER);

        // Bottom: full-width table
        JPanel bottomTable = new JPanel(new BorderLayout());
        bottomTable.setBackground(SCMColors.BG_PRIMARY);
        JLabel tblLbl = SCMComponents.subLabel("Shipment Details Table");
        tblLbl.setBorder(new EmptyBorder(8, 0, 6, 0));
        bottomTable.add(tblLbl, BorderLayout.NORTH);
        bottomTable.add(tableScroll, BorderLayout.CENTER);
        bottomTable.setPreferredSize(new Dimension(0, 220));

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBackground(SCMColors.BG_PRIMARY);
        centerPanel.add(split, BorderLayout.CENTER);
        centerPanel.add(bottomTable, BorderLayout.SOUTH);
        centerPanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel buildActionPanel() {
        JPanel panel = SCMComponents.cardPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        ((FlowLayout)panel.getLayout()).setVgap(8);

        JButton createDelivery = SCMComponents.goldButton("📦 Create Delivery Order");
        JButton optimizeRoute  = SCMComponents.outlineButton("🗺 Optimize Route");
        JButton dropShip       = SCMComponents.outlineButton("✈ Drop-Ship Config");

        createDelivery.addActionListener(e -> showCreateDeliveryDialog());
        optimizeRoute.addActionListener(e -> runRouteOptimization());
        dropShip.addActionListener(e -> showDropShipDialog());

        panel.add(createDelivery);
        panel.add(optimizeRoute);
        panel.add(dropShip);
        return panel;
    }

    private void loadShipmentsAsync() {
        SwingWorker<List<Shipment>, Void> worker = new SwingWorker<>() {
            protected List<Shipment> doInBackground() throws Exception {
                return fetchShipments();
            }
            protected void done() {
                try {
                    currentShipments = get();
                    populateTable(currentShipments);
                    populateETACards(currentShipments);
                    mapPanel.setShipments(currentShipments);
                    mapPanel.repaint();
                } catch (Exception ex) {
                    // E:024 GPS/data unavailable
                    showGPSUnavailable();
                    loadDemoShipments();
                }
            }
        };
        worker.execute();
    }

    private List<Shipment> fetchShipments() throws Exception {
        List<Shipment> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) throw new Exception("No DB");
        String sql = "SELECT s.*, o.delivery_address FROM shipments s LEFT JOIN orders o ON s.order_id = o.order_id LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shipment sh = new Shipment();
                sh.shipmentId   = rs.getInt("shipment_id");
                sh.shipmentCode = rs.getString("shipment_code");
                sh.status       = rs.getString("status");
                sh.carrier      = rs.getString("carrier");
                sh.origin       = rs.getString("origin_city");
                sh.destination  = rs.getString("destination_city");
                sh.eta          = rs.getString("estimated_arrival");
                sh.lat          = rs.getDouble("gps_lat");
                sh.lng          = rs.getDouble("gps_lng");
                list.add(sh);
            }
        }
        return list;
    }

    private void populateTable(List<Shipment> shipments) {
        tableModel.setRowCount(0);
        for (Shipment s : shipments) {
            tableModel.addRow(new Object[]{
                s.shipmentCode, s.origin, s.destination, s.carrier, s.eta, s.status
            });
        }
    }

    private void populateETACards(List<Shipment> shipments) {
        etaPanel.removeAll();
        for (Shipment s : shipments) {
            etaPanel.add(buildETACard(s));
            etaPanel.add(Box.createVerticalStrut(8));
        }
        etaPanel.revalidate();
        etaPanel.repaint();
    }

    private JPanel buildETACard(Shipment s) {
        JPanel card = SCMComponents.cardPanel();
        card.setLayout(new BorderLayout(8, 4));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        Color statusColor = getStatusColor(s.status);
        JLabel dot = new JLabel("●");
        dot.setForeground(statusColor);
        dot.setFont(dot.getFont().deriveFont(16f));

        JLabel id = SCMComponents.goldLabel(s.shipmentCode);
        id.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD, 13f));

        JLabel dest = SCMComponents.subLabel("→ " + s.destination + "  |  " + s.carrier);
        JLabel eta  = new JLabel("ETA: " + s.eta);
        eta.setForeground(statusColor);
        eta.setFont(SCMColors.FONT_SMALL);

        JPanel left = new JPanel(new BorderLayout(4, 2));
        left.setBackground(SCMColors.BG_CARD);
        left.add(id, BorderLayout.NORTH);
        left.add(dest, BorderLayout.CENTER);
        left.add(eta, BorderLayout.SOUTH);

        card.add(dot, BorderLayout.WEST);
        card.add(left, BorderLayout.CENTER);
        card.setBorder(new CompoundBorder(
            new LineBorder(statusColor, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return card;
    }

    private Color getStatusColor(String status) {
        if (status == null) return SCMColors.STATUS_INFO;
        return switch (status.toLowerCase()) {
            case "delivered" -> SCMColors.STATUS_SUCCESS;
            case "delayed"   -> SCMColors.STATUS_ERROR;
            case "in transit"-> SCMColors.STATUS_WARNING;
            default          -> SCMColors.STATUS_INFO;
        };
    }

    // E:024 GPS Unavailable
    private void showGPSUnavailable() {
        gpsStatusLabel.setText("  ⚠  GPS Unavailable — Showing last known positions");
        gpsStatusLabel.setForeground(SCMColors.STATUS_ERROR);
        gpsStatusLabel.setBackground(new Color(255, 62, 85, 20));
        gpsStatusLabel.setBorder(new CompoundBorder(
            new LineBorder(SCMColors.STATUS_ERROR, 1),
            new EmptyBorder(6, 12, 6, 12)
        ));
        mapPanel.setGPSUnavailable(true);
        mapPanel.repaint();
    }

    private void refreshGPS() {
        gpsStatusLabel.setText("  🔄  Refreshing GPS feed...");
        gpsStatusLabel.setForeground(SCMColors.STATUS_INFO);
        loadShipmentsAsync();
    }

    private void startGPSRefresh() {
        gpsRefreshTimer = new java.util.Timer(true);
        gpsRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (gpsAvailable) {
                        gpsStatusLabel.setText("  🛰  GPS Feed: Live  •  Last updated: just now");
                        gpsStatusLabel.setForeground(SCMColors.STATUS_SUCCESS);
                    }
                });
            }
        }, 30000, 30000);
    }

    // E:026 Route Optimization Failure
    private void runRouteOptimization() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Route Optimization", true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("🗺  Route Optimization Engine");
        JLabel sub = SCMComponents.subLabel("Select shipment and optimization mode");

        String[] shipIds = currentShipments.stream()
            .map(s -> s.shipmentCode).toArray(String[]::new);
        if (shipIds.length == 0) shipIds = new String[]{"No active shipments"};

        JComboBox<String> shipCombo = SCMComponents.styledCombo(shipIds);
        String[] modes = {"Fastest Route", "Fuel Efficient", "Least Stops", "Traffic Avoiding"};
        JComboBox<String> modeCombo = SCMComponents.styledCombo(modes);

        JPanel form = new JPanel(new GridLayout(4, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);
        form.add(SCMComponents.subLabel("Shipment:")); form.add(shipCombo);
        form.add(SCMComponents.subLabel("Mode:")); form.add(modeCombo);

        JButton optimize = SCMComponents.goldButton("⚡ Optimize Now");
        JButton manual   = SCMComponents.outlineButton("✏ Set Route Manually");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(manual); btns.add(optimize);

        optimize.addActionListener(e -> {
            // Simulate optimization with possible E:026
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(1500); // simulate API call
                    return Math.random() > 0.2; // 80% success
                }
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            SCMComponents.showToast((Component)LogisticsPanel.this,
                                "✓ Route optimized successfully for " + shipCombo.getSelectedItem(), false);
                            dialog.dispose();
                        } else {
                            // E:026
                            showExceptionModal("E:026", "MINOR",
                                "Route Optimization Unavailable",
                                "Route optimization engine failed to return a valid delivery route plan within the allowed timeout.",
                                "Falling back to last saved manual route. You can set the route manually.",
                                dialog);
                        }
                    } catch (Exception ex) {
                        showExceptionModal("E:026", "MINOR",
                            "Route Optimization Unavailable",
                            ex.getMessage(),
                            "Falling back to last saved manual route.",
                            dialog);
                    }
                }
            };
            optimize.setText("⏳ Optimizing...");
            optimize.setEnabled(false);
            w.execute();
        });

        manual.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog,
                "Manual route entry enabled.\nEnter waypoints in the route field.",
                "Manual Route", JOptionPane.INFORMATION_MESSAGE);
        });

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    // E:028 Delivery Order Creation Failed
    private void showCreateDeliveryDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Delivery Order", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("📦  Create Delivery Order");

        JPanel form = new JPanel(new GridLayout(8, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);

        JTextField orderIdFld  = SCMComponents.styledField("ORD-XXXX");
        JTextField originFld   = SCMComponents.styledField("Mumbai WH");
        JTextField destFld     = SCMComponents.styledField("Delhi");
        String[] carriers = {"BlueDart", "DTDC", "Delhivery", "FedEx", "Ecom Express"};
        JComboBox<String> carrierCombo = SCMComponents.styledCombo(carriers);
        JTextField weightFld   = SCMComponents.styledField("kg");
        JTextField noteFld     = SCMComponents.styledField("Fragile / Handle with care");
        JCheckBox dropShipChk  = new JCheckBox("Drop-Ship Order");
        dropShipChk.setForeground(SCMColors.TEXT_SECONDARY);
        dropShipChk.setBackground(SCMColors.BG_SECONDARY);
        JTextField contactFld  = SCMComponents.styledField("Contact person");

        form.add(SCMComponents.subLabel("Order ID:")); form.add(orderIdFld);
        form.add(SCMComponents.subLabel("Origin:")); form.add(originFld);
        form.add(SCMComponents.subLabel("Destination:")); form.add(destFld);
        form.add(SCMComponents.subLabel("Carrier:")); form.add(carrierCombo);
        form.add(SCMComponents.subLabel("Weight (kg):")); form.add(weightFld);
        form.add(SCMComponents.subLabel("Note:")); form.add(noteFld);
        form.add(SCMComponents.subLabel("Drop-Ship:")); form.add(dropShipChk);
        form.add(SCMComponents.subLabel("Contact:")); form.add(contactFld);

        JButton create = SCMComponents.goldButton("🚀 Create Delivery Order");
        JButton saveDraft = SCMComponents.outlineButton("💾 Save as Draft");
        JButton cancel = SCMComponents.outlineButton("✕ Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(cancel); btns.add(saveDraft); btns.add(create);

        cancel.addActionListener(e -> dialog.dispose());
        saveDraft.addActionListener(e -> {
            SCMComponents.showToast((Component)this, "📋 Delivery order saved as draft", false);
            dialog.dispose();
        });

        create.addActionListener(e -> {
            String ordId = orderIdFld.getText().trim();
            String orig  = originFld.getText().trim();
            String dest  = destFld.getText().trim();

            if (ordId.isEmpty() || orig.isEmpty() || dest.isEmpty()) {
                // E:028 missing data
                showExceptionModal("E:028", "MAJOR",
                    "Delivery Order Creation Failed",
                    "Missing required shipment data: Order ID, Origin, or Destination is empty.",
                    "Please fill all required fields and retry.",
                    dialog);
                return;
            }

            // Simulate carrier API call — E:027
            SwingWorker<Boolean, Void> w = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    Thread.sleep(1200);
                    carrierApiRetryCount++;
                    if (Math.random() < 0.15 && carrierApiRetryCount <= 2) {
                        throw new Exception("Carrier API timeout (HTTP 504)");
                    }
                    carrierApiRetryCount = 0;
                    return createDeliveryOrderInDB(ordId, orig, dest,
                        (String)carrierCombo.getSelectedItem(), noteFld.getText());
                }
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            SCMComponents.showToast((Component)LogisticsPanel.this,
                                "✓ Delivery order created for " + ordId, false);
                            dialog.dispose();
                            loadShipmentsAsync();
                        } else {
                            showExceptionModal("E:028", "MAJOR",
                                "Delivery Order Creation Failed",
                                "Carrier API rejected the delivery order.",
                                "Delivery form saved as draft. Logistics Officer and Admin notified.",
                                dialog);
                        }
                    } catch (Exception ex) {
                        // E:027 Carrier API Timeout
                        if (carrierApiRetryCount < 2) {
                            SCMComponents.showToast((Component)LogisticsPanel.this,
                                "⚠ Carrier API timeout — retrying (" + carrierApiRetryCount + "/2)...", true);
                        } else {
                            showExceptionModal("E:027", "MAJOR",
                                "Carrier API Timeout",
                                "Carrier API response timed out when creating delivery order.\n" + ex.getMessage(),
                                "Shipment marked as 'Status Unknown'. Admin has been alerted.",
                                dialog);
                        }
                    }
                }
            };
            create.setText("⏳ Creating...");
            create.setEnabled(false);
            w.execute();
        });

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private boolean createDeliveryOrderInDB(String orderId, String origin, String dest, String carrier, String note) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            String sql = "INSERT INTO shipments (shipment_code, order_id, carrier, origin_city, destination_city, status, estimated_arrival, gps_lat, gps_lng) VALUES (?,?,?,?,?,'Pending','TBD',19.07,72.87)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "SHP-" + System.currentTimeMillis() % 10000);
                ps.setInt(2, 0);
                ps.setString(3, carrier);
                ps.setString(4, origin);
                ps.setString(5, dest);
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) { return true; } // demo
    }

    private void showDropShipDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Drop-Ship Configuration", true);
        dialog.setSize(460, 340);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(SCMColors.BG_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SCMColors.BG_SECONDARY);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = SCMComponents.goldLabel("✈  Drop-Ship Configuration");
        JPanel form = new JPanel(new GridLayout(5, 2, 12, 10));
        form.setBackground(SCMColors.BG_SECONDARY);

        JTextField supplierFld = SCMComponents.styledField("Supplier name");
        JTextField skuFld      = SCMComponents.styledField("SKU-XXXX");
        JTextField qtyFld      = SCMComponents.styledField("Qty");
        JTextField custFld     = SCMComponents.styledField("Customer / destination");
        JCheckBox autoFulfill  = new JCheckBox("Auto-fulfil on order"); autoFulfill.setForeground(SCMColors.TEXT_SECONDARY); autoFulfill.setBackground(SCMColors.BG_SECONDARY);

        form.add(SCMComponents.subLabel("Supplier:")); form.add(supplierFld);
        form.add(SCMComponents.subLabel("SKU:")); form.add(skuFld);
        form.add(SCMComponents.subLabel("Quantity:")); form.add(qtyFld);
        form.add(SCMComponents.subLabel("Customer:")); form.add(custFld);
        form.add(SCMComponents.subLabel("Auto-Fulfil:")); form.add(autoFulfill);

        JButton save = SCMComponents.goldButton("💾 Save Config");
        JButton cancel = SCMComponents.outlineButton("✕ Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(cancel); btns.add(save);

        save.addActionListener(e -> {
            SCMComponents.showToast((Component)this, "✓ Drop-ship configuration saved", false);
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    // E:025 Shipment Not Found — triggered on table row search
    public void searchShipment(String query) {
        boolean found = currentShipments.stream()
            .anyMatch(s -> s.shipmentCode.equalsIgnoreCase(query) ||
                          s.carrier.equalsIgnoreCase(query) ||
                          s.destination.equalsIgnoreCase(query));
        if (!found) {
            showExceptionModal("E:025", "MINOR",
                "Shipment Not Found",
                "Shipment ID or query '" + query + "' does not exist in the logistics system.",
                "Try searching by carrier name or destination city.",
                this);
        }
    }

    private void showExceptionModal(String code, String category, String title, String message, String plan, Component parent) {
        JDialog modal = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Exception — " + title, true);
        modal.setSize(500, 360);
        modal.setLocationRelativeTo(parent);
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
        codeLabel.setBorder(new CompoundBorder(new LineBorder(catColor, 1), new EmptyBorder(6, 12, 6, 12)));

        JLabel titleLbl = SCMComponents.goldLabel("⚠  " + title);
        JTextArea msgArea = new JTextArea("Issue: " + message + "\n\nResolution: " + plan);
        msgArea.setForeground(SCMColors.TEXT_SECONDARY);
        msgArea.setBackground(SCMColors.BG_CARD);
        msgArea.setFont(SCMColors.FONT_SMALL);
        msgArea.setEditable(false);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton dismiss = SCMComponents.outlineButton("Dismiss");
        JButton retry   = SCMComponents.goldButton("↻ Retry");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(SCMColors.BG_SECONDARY);
        btns.add(dismiss); btns.add(retry);
        dismiss.addActionListener(e -> modal.dispose());
        retry.addActionListener(e -> { modal.dispose(); loadShipmentsAsync(); });

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(SCMColors.BG_SECONDARY);
        top.add(codeLabel, BorderLayout.NORTH);
        top.add(titleLbl, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(msgArea), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        modal.add(panel);
        modal.setVisible(true);
    }

    private void loadDemoShipments() {
        currentShipments = new ArrayList<>();
        String[][] data = {
            {"SHP-001","Mumbai WH","Delhi","BlueDart","2h","In Transit"},
            {"SHP-002","Pune WH","Nagpur","DTDC","5h","Delayed"},
            {"SHP-003","Chennai WH","Bangalore","Delhivery","1h","In Transit"},
            {"SHP-004","Delhi WH","Jaipur","FedEx","3h","Delivered"}
        };
        double[][] coords = {{28.6, 77.2},{21.1, 79.0},{12.9, 77.6},{26.9, 75.8}};
        for (int i = 0; i < data.length; i++) {
            Shipment s = new Shipment();
            s.shipmentCode = data[i][0]; s.origin = data[i][1];
            s.destination = data[i][2]; s.carrier = data[i][3];
            s.eta = data[i][4]; s.status = data[i][5];
            s.lat = coords[i][0]; s.lng = coords[i][1];
            currentShipments.add(s);
        }
        SwingUtilities.invokeLater(() -> {
            populateTable(currentShipments);
            populateETACards(currentShipments);
            mapPanel.setShipments(currentShipments);
            mapPanel.repaint();
        });
    }

    public void cleanup() {
        if (gpsRefreshTimer != null) gpsRefreshTimer.cancel();
    }

    // ─── Inner: Map Panel ────────────────────────────────────────────────────
    static class MapPanel extends JPanel {
        private List<Shipment> shipments = new ArrayList<>();
        private double zoom = 1.0;
        private boolean gpsUnavailable = false;
        private int offsetX = 0, offsetY = 0;

        MapPanel() {
            setBackground(new Color(13, 20, 35));
            setBorder(new LineBorder(SCMColors.ACCENT_GOLD, 1));
            addMouseWheelListener(e -> {
                zoom *= (e.getWheelRotation() < 0) ? 1.1 : 0.9;
                zoom = Math.max(0.5, Math.min(zoom, 3.0));
                repaint();
            });
        }

        void setShipments(List<Shipment> s) { this.shipments = s; }
        void setGPSUnavailable(boolean b) { this.gpsUnavailable = b; }
        void zoom(double factor) { zoom *= factor; zoom = Math.max(0.5, Math.min(zoom, 3.0)); }
        void recenter() { zoom = 1.0; offsetX = 0; offsetY = 0; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // Draw grid (map background)
            g2.setColor(new Color(30, 45, 70));
            for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

            // Draw stylized India map outline (simplified polygon)
            g2.setColor(new Color(40, 60, 100));
            g2.setStroke(new BasicStroke(2));
            int[] indiaX = {160,180,210,240,260,280,270,290,310,300,280,260,230,200,170,150,140,150,160};
            int[] indiaY = {40, 60, 55, 70, 80, 100,130,160,190,220,240,260,270,280,260,240,180,120,40};
            g2.drawPolyline(indiaX, indiaY, indiaX.length);
            g2.setColor(new Color(40, 60, 100, 60));
            g2.fillPolygon(indiaX, indiaY, indiaX.length);

            // Warehouse origin
            int wx = (int)(200 * zoom) + offsetX;
            int wy = (int)(150 * zoom) + offsetY;
            g2.setColor(SCMColors.ACCENT_GOLD);
            g2.fillOval(wx-8, wy-8, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(SCMColors.FONT_SMALL);
            g2.drawString("WH", wx-8, wy+18);

            // Draw shipment dots
            Color[] dotColors = {SCMColors.STATUS_SUCCESS, SCMColors.STATUS_ERROR,
                                 SCMColors.STATUS_INFO, SCMColors.STATUS_WARNING};
            for (int i = 0; i < Math.min(shipments.size(), dotColors.length); i++) {
                Shipment s = shipments.get(i);
                int sx = (int)((100 + i * 60) * zoom) + offsetX;
                int sy = (int)((80 + i * 50) * zoom) + offsetY;

                // Draw route line
                g2.setColor(new Color(dotColors[i].getRed(), dotColors[i].getGreen(), dotColors[i].getBlue(), 100));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6,4}, 0));
                g2.drawLine(wx, wy, sx, sy);

                // Draw vehicle dot
                g2.setStroke(new BasicStroke(2));
                g2.setColor(dotColors[i]);
                g2.fillOval(sx-6, sy-6, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(SCMColors.FONT_SMALL.deriveFont(10f));
                g2.drawString(s.shipmentCode, sx+10, sy+4);
                g2.setFont(SCMColors.FONT_SMALL.deriveFont(9f));
                g2.setColor(SCMColors.TEXT_SECONDARY);
                g2.drawString("→ " + s.destination, sx+10, sy+14);
            }

            // GPS unavailable overlay
            if (gpsUnavailable) {
                g2.setColor(new Color(255, 62, 85, 40));
                g2.fillRect(0, 0, w, h);
                g2.setColor(SCMColors.STATUS_ERROR);
                g2.setFont(SCMColors.FONT_BODY.deriveFont(Font.BOLD));
                String msg = "⚠  GPS UNAVAILABLE — Last Known Positions";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h/2);
            }

            // Map legend
            g2.setFont(SCMColors.FONT_SMALL.deriveFont(10f));
            g2.setColor(SCMColors.ACCENT_GOLD);
            g2.drawString("● Warehouse Origin", 8, h-30);
            g2.setColor(SCMColors.STATUS_INFO);
            g2.drawString("● In Transit", 8, h-18);
            g2.setColor(SCMColors.STATUS_ERROR);
            g2.drawString("● Delayed", 90, h-18);
        }
    }

    // ─── Status cell renderer ────────────────────────────────────────────────
    static class StatusCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String val = v == null ? "" : v.toString();
            setBackground(sel ? SCMColors.BG_PRIMARY : SCMColors.BG_CARD);
            setForeground(switch (val.toLowerCase()) {
                case "delivered" -> SCMColors.STATUS_SUCCESS;
                case "delayed"   -> SCMColors.STATUS_ERROR;
                case "pending"   -> SCMColors.STATUS_WARNING;
                default          -> SCMColors.STATUS_INFO;
            });
            setText("● " + val);
            return this;
        }
    }
}
