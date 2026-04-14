-- ============================================================
-- SCM Supply Chain Management - Full Database Setup Script
-- Team T3N50R | UI Subsystem #16
-- ============================================================

CREATE DATABASE IF NOT EXISTS scm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE scm_db;

-- ============================================================
-- USERS & RBAC
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    user_email VARCHAR(150) NOT NULL UNIQUE,
    user_display_name VARCHAR(150),
    password_hash VARCHAR(255) NOT NULL,
    two_factor_secret VARCHAR(100),
    assigned_role ENUM('SUPER_ADMIN','ADMIN','MANAGER','WAREHOUSE_STAFF','LOGISTICS_OFFICER','SALES_STAFF','VIEWER') NOT NULL DEFAULT 'VIEWER',
    is_account_locked BOOLEAN DEFAULT FALSE,
    login_attempt_count INT DEFAULT 0,
    last_login_timestamp DATETIME,
    session_status ENUM('ACTIVE','INACTIVE','LOCKED') DEFAULT 'INACTIVE',
    theme_preference ENUM('DARK','LIGHT') DEFAULT 'DARK',
    language_preference VARCHAR(20) DEFAULT 'English',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    jwt_token VARCHAR(500) NOT NULL,
    redirect_panel_url VARCHAR(200),
    expiry_time DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    pref_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    email_alerts BOOLEAN DEFAULT TRUE,
    stock_alerts BOOLEAN DEFAULT TRUE,
    order_alerts BOOLEAN DEFAULT TRUE,
    shipment_alerts BOOLEAN DEFAULT TRUE,
    system_alerts BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- PRODUCTS & INVENTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS warehouses (
    warehouse_id INT AUTO_INCREMENT PRIMARY KEY,
    warehouse_name VARCHAR(150) NOT NULL,
    warehouse_zone VARCHAR(100),
    location VARCHAR(200),
    capacity INT DEFAULT 10000,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_sku VARCHAR(100) NOT NULL UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    product_category VARCHAR(100),
    current_stock_level INT DEFAULT 0,
    reorder_threshold INT DEFAULT 10,
    stock_status ENUM('OK','LOW','OUT') DEFAULT 'OK',
    warehouse_id INT,
    barcode_rfid_value VARCHAR(200),
    unit_price DECIMAL(10,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS stock_transfers (
    transfer_id INT AUTO_INCREMENT PRIMARY KEY,
    product_sku VARCHAR(100) NOT NULL,
    from_warehouse_id INT,
    to_warehouse_id INT,
    quantity INT NOT NULL,
    transfer_status ENUM('PENDING','APPROVED','REJECTED','COMPLETED') DEFAULT 'PENDING',
    requested_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requested_by) REFERENCES users(user_id)
);

-- ============================================================
-- CUSTOMERS
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(200) NOT NULL,
    customer_email VARCHAR(150) UNIQUE,
    delivery_address TEXT,
    phone VARCHAR(30),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ORDERS
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    order_status ENUM('PENDING','PROCESSING','SHIPPED','DELIVERED','RETURN','CANCELLED') DEFAULT 'PENDING',
    order_value DECIMAL(12,2) DEFAULT 0.00,
    discount_code_applied VARCHAR(50),
    order_date DATE NOT NULL,
    created_by INT,
    invoice_generated BOOLEAN DEFAULT FALSE,
    packing_slip_generated BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS order_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_applied DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS return_refunds (
    return_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    reason TEXT,
    return_status ENUM('PENDING','APPROVED','REJECTED','PROCESSED') DEFAULT 'PENDING',
    refund_amount DECIMAL(12,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- ============================================================
-- PRICING, DISCOUNT & COMMISSION
-- ============================================================
CREATE TABLE IF NOT EXISTS price_tiers (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT,
    tier_level VARCHAR(50) NOT NULL,
    price_per_unit DECIMAL(10,2) NOT NULL,
    min_order_quantity INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS discount_rules (
    discount_id INT AUTO_INCREMENT PRIMARY KEY,
    discount_code VARCHAR(50) NOT NULL UNIQUE,
    discount_type ENUM('PERCENTAGE','FLAT') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    discount_valid_from DATE,
    discount_valid_to DATE,
    min_order_value_for_discount DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agents (
    agent_id VARCHAR(50) PRIMARY KEY,
    agent_name VARCHAR(200) NOT NULL,
    agent_email VARCHAR(150),
    commission_rate DECIMAL(5,2) DEFAULT 5.00,
    tier_level ENUM('LEVEL1','LEVEL2','LEVEL3') DEFAULT 'LEVEL1',
    payout_status ENUM('PENDING','PAID','ON_HOLD') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commission_ledger (
    ledger_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    order_id INT,
    commission_amount DECIMAL(10,2) NOT NULL,
    payout_status ENUM('PENDING','PAID','ON_HOLD') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- ============================================================
-- LOGISTICS & TRANSPORT
-- ============================================================
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id INT AUTO_INCREMENT PRIMARY KEY,
    shipment_number VARCHAR(50) NOT NULL UNIQUE,
    order_id INT,
    shipment_status ENUM('PREPARING','IN_TRANSIT','DELAYED','DELIVERED','CANCELLED') DEFAULT 'PREPARING',
    carrier_name VARCHAR(150),
    delivery_origin VARCHAR(200),
    delivery_destination VARCHAR(200),
    gps_latitude DOUBLE,
    gps_longitude DOUBLE,
    vehicle_id VARCHAR(50),
    estimated_arrival_time VARCHAR(50),
    carrier_api_response TEXT,
    route_plan TEXT,
    drop_ship_config TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- ============================================================
-- DEMAND FORECASTING
-- ============================================================
CREATE TABLE IF NOT EXISTS sales_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    product_sku VARCHAR(100) NOT NULL,
    sale_date DATE NOT NULL,
    quantity_sold INT DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0.00,
    product_category VARCHAR(100),
    warehouse_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forecast_results (
    forecast_id INT AUTO_INCREMENT PRIMARY KEY,
    product_sku VARCHAR(100) NOT NULL,
    forecast_month INT NOT NULL,
    forecast_year INT NOT NULL,
    predicted_demand DOUBLE NOT NULL,
    seasonal_factor DOUBLE DEFAULT 1.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reorder_suggestions (
    suggestion_id INT AUTO_INCREMENT PRIMARY KEY,
    product_sku VARCHAR(100) NOT NULL,
    suggested_quantity INT NOT NULL,
    reason TEXT,
    is_actioned BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- NOTIFICATIONS & AUDIT LOG
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    notification_type ENUM('INFO','WARNING','ERROR','SUCCESS') DEFAULT 'INFO',
    notification_message TEXT NOT NULL,
    exception_module_name VARCHAR(100),
    exception_error_code VARCHAR(50),
    exception_stack_trace TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    alert_config_rules TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    audit_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id INT,
    audit_action_user VARCHAR(150),
    audit_action_description TEXT NOT NULL,
    audit_module_name VARCHAR(100),
    ip_address VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- ============================================================
-- SYSTEM CONFIG
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- KPI SNAPSHOTS (for dashboard)
-- ============================================================
CREATE TABLE IF NOT EXISTS kpi_snapshots (
    snapshot_id INT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id INT,
    snapshot_date DATE NOT NULL,
    total_orders INT DEFAULT 0,
    total_revenue DOUBLE DEFAULT 0.0,
    low_stock_count INT DEFAULT 0,
    shipments_today INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Default Users (passwords: admin123, manager123, staff123, warehouse123, logistics123)
INSERT INTO users (username, user_email, user_display_name, password_hash, assigned_role, session_status) VALUES
('admin', 'admin@scm.com', 'Super Administrator', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh/S', 'SUPER_ADMIN', 'INACTIVE'),
('manager', 'manager@scm.com', 'Operations Manager', '$2a$10$8K1p/a0dR1LXMIgoEDFrwOfMQbLgtnOemgjNv2c1xU3Mv2gKVP20m', 'MANAGER', 'INACTIVE'),
('warehouse1', 'warehouse@scm.com', 'Warehouse Staff', '$2a$10$GRvDnVCFjHqzjF5JLGQ7/.q7Sxv5.CrFTH/JDNYqSu3OIuMK2Hri', 'WAREHOUSE_STAFF', 'INACTIVE'),
('logistics1', 'logistics@scm.com', 'Logistics Officer', '$2a$10$XvJ5d9B1N8Z2mKqL3rT0OuWgPzA4CaFsDe6HjI7RyVb8KmNxQpM3q', 'LOGISTICS_OFFICER', 'INACTIVE'),
('sales1', 'sales@scm.com', 'Sales Staff', '$2a$10$TqA9rB2C3dE4fF5gG6hH7iI8jJ9kK0lL1mM2nN3oO4pP5qQ6rR7sS', 'SALES_STAFF', 'INACTIVE');

-- Warehouses
INSERT INTO warehouses (warehouse_name, warehouse_zone, location, capacity) VALUES
('Mumbai Central WH', 'Zone-A', 'Mumbai, Maharashtra', 50000),
('Delhi North WH', 'Zone-B', 'Delhi, NCR', 40000),
('Bangalore South WH', 'Zone-C', 'Bangalore, Karnataka', 35000),
('Pune WH', 'Zone-D', 'Pune, Maharashtra', 25000);

-- Products
INSERT INTO products (product_sku, product_name, product_category, current_stock_level, reorder_threshold, stock_status, warehouse_id, unit_price) VALUES
('WGT-100', 'Widget A - Industrial Grade', 'Electronics', 284, 50, 'OK', 1, 500.00),
('SNS-200', 'Sensor B - High Precision', 'Electronics', 12, 30, 'LOW', 1, 1200.00),
('MTR-300', 'Motor C - DC Brushless', 'Mechanical', 0, 10, 'OUT', 2, 3500.00),
('CBL-400', 'Cable D - Shielded 5m', 'Accessories', 540, 100, 'OK', 2, 150.00),
('PCB-500', 'PCB Module E - Controller', 'Electronics', 8, 20, 'LOW', 3, 2200.00),
('HYD-600', 'Hydraulic Pump F', 'Mechanical', 45, 15, 'OK', 3, 8500.00),
('OPT-700', 'Optical Sensor G', 'Electronics', 190, 50, 'OK', 4, 650.00),
('FLT-800', 'Filter Unit H - Industrial', 'Accessories', 3, 25, 'LOW', 4, 320.00),
('VLV-900', 'Valve I - Pneumatic', 'Mechanical', 67, 20, 'OK', 1, 1800.00),
('CMP-1000', 'Compressor J - Mini', 'Mechanical', 0, 5, 'OUT', 2, 12000.00);

-- Customers
INSERT INTO customers (customer_name, customer_email, delivery_address, phone) VALUES
('Acme Corp', 'orders@acme.com', '14 Industrial Estate, Mumbai 400001', '+91-22-12345678'),
('TechMart India', 'purchase@techmart.in', '55 Tech Park, Bangalore 560001', '+91-80-87654321'),
('LogiBase Solutions', 'supply@logibase.com', '22 Logistics Hub, Pune 411001', '+91-20-11223344'),
('QuickShop Retail', 'store@quickshop.in', '8 Market Street, Delhi 110001', '+91-11-99887766'),
('GlobalTech Ltd', 'procurement@globaltech.com', '100 Tech Avenue, Hyderabad 500001', '+91-40-55667788');

-- Price Tiers
INSERT INTO price_tiers (product_id, tier_level, price_per_unit, min_order_quantity) VALUES
(1, 'TIER1-Retail', 500.00, 1),
(1, 'TIER2-Wholesale', 420.00, 50),
(1, 'TIER3-Distributor', 360.00, 200),
(2, 'TIER1-Retail', 1200.00, 1),
(2, 'TIER2-Wholesale', 980.00, 25),
(3, 'TIER1-Retail', 3500.00, 1),
(3, 'TIER2-Wholesale', 3000.00, 10);

-- Discount Rules
INSERT INTO discount_rules (discount_code, discount_type, discount_value, discount_valid_from, discount_valid_to, min_order_value_for_discount) VALUES
('SAVE20', 'PERCENTAGE', 20.00, '2026-01-01', '2026-12-31', 500.00),
('FLAT500', 'FLAT', 500.00, '2026-01-01', '2026-06-30', 5000.00),
('SUMMER10', 'PERCENTAGE', 10.00, '2026-04-01', '2026-07-31', 1000.00),
('BULK15', 'PERCENTAGE', 15.00, '2026-01-01', '2026-12-31', 10000.00);

-- Agents
INSERT INTO agents (agent_id, agent_name, agent_email, commission_rate, tier_level, payout_status) VALUES
('AGT-001', 'Rahul M.', 'rahul@agents.com', 8.00, 'LEVEL2', 'PAID'),
('AGT-002', 'Priya S.', 'priya@agents.com', 5.00, 'LEVEL1', 'PAID'),
('AGT-003', 'Vikram N.', 'vikram@agents.com', 10.00, 'LEVEL3', 'PENDING');

-- Orders
INSERT INTO orders (order_number, customer_id, order_status, order_value, discount_code_applied, order_date, created_by) VALUES
('ORD-4821', 1, 'DELIVERED', 12400.00, NULL, '2026-02-20', 1),
('ORD-4822', 2, 'PENDING', 8200.00, NULL, '2026-02-20', 1),
('ORD-4823', 3, 'RETURN', 3500.00, 'SAVE20', '2026-02-19', 1),
('ORD-4824', 4, 'SHIPPED', 5800.00, NULL, '2026-02-19', 1),
('ORD-4825', 5, 'PROCESSING', 24000.00, 'BULK15', '2026-04-01', 1),
('ORD-4826', 1, 'DELIVERED', 15000.00, NULL, '2026-03-15', 1),
('ORD-4827', 2, 'SHIPPED', 7600.00, 'SUMMER10', '2026-04-05', 1),
('ORD-4828', 3, 'PENDING', 9300.00, NULL, '2026-04-08', 1);

-- Order Items
INSERT INTO order_items (order_id, product_sku, quantity, unit_price) VALUES
(1, 'WGT-100', 10, 500.00),
(1, 'CBL-400', 20, 150.00),
(2, 'SNS-200', 5, 1200.00),
(2, 'CBL-400', 10, 150.00),
(3, 'MTR-300', 1, 3500.00),
(4, 'CBL-400', 20, 150.00),
(4, 'WGT-100', 5, 500.00),
(5, 'HYD-600', 2, 8500.00),
(5, 'VLV-900', 4, 1800.00);

-- Shipments
INSERT INTO shipments (shipment_number, order_id, shipment_status, carrier_name, delivery_origin, delivery_destination, gps_latitude, gps_longitude, vehicle_id, estimated_arrival_time) VALUES
('SHP-001', 1, 'DELIVERED', 'BlueDart', 'Mumbai WH', 'Delhi', 28.7041, 77.1025, 'VH-MH-4521', '2h'),
('SHP-002', 2, 'DELAYED', 'DTDC', 'Pune WH', 'Nagpur', 21.1458, 79.0882, 'VH-MH-3322', '5h'),
('SHP-003', 4, 'IN_TRANSIT', 'FedEx', 'Mumbai WH', 'Hyderabad', 17.3850, 78.4867, 'VH-MH-7711', '1h'),
('SHP-004', 5, 'PREPARING', 'Delhivery', 'Delhi WH', 'Bangalore', 12.9716, 77.5946, 'VH-DL-1234', '24h');

-- Sales Records (last 6 months)
INSERT INTO sales_records (product_sku, sale_date, quantity_sold, revenue, product_category, warehouse_id) VALUES
('WGT-100', '2025-10-01', 45, 22500.00, 'Electronics', 1),
('WGT-100', '2025-11-01', 52, 26000.00, 'Electronics', 1),
('WGT-100', '2025-12-01', 68, 34000.00, 'Electronics', 1),
('WGT-100', '2026-01-01', 60, 30000.00, 'Electronics', 1),
('WGT-100', '2026-02-01', 75, 37500.00, 'Electronics', 1),
('WGT-100', '2026-03-01', 82, 41000.00, 'Electronics', 1),
('SNS-200', '2025-10-01', 20, 24000.00, 'Electronics', 1),
('SNS-200', '2025-11-01', 18, 21600.00, 'Electronics', 1),
('SNS-200', '2025-12-01', 25, 30000.00, 'Electronics', 1),
('SNS-200', '2026-01-01', 22, 26400.00, 'Electronics', 1),
('SNS-200', '2026-02-01', 30, 36000.00, 'Electronics', 1),
('SNS-200', '2026-03-01', 28, 33600.00, 'Electronics', 1),
('MTR-300', '2025-10-01', 8, 28000.00, 'Mechanical', 2),
('MTR-300', '2025-11-01', 12, 42000.00, 'Mechanical', 2),
('MTR-300', '2025-12-01', 15, 52500.00, 'Mechanical', 2),
('MTR-300', '2026-01-01', 10, 35000.00, 'Mechanical', 2),
('MTR-300', '2026-02-01', 14, 49000.00, 'Mechanical', 2),
('MTR-300', '2026-03-01', 18, 63000.00, 'Mechanical', 2),
('CBL-400', '2025-10-01', 120, 18000.00, 'Accessories', 2),
('CBL-400', '2025-11-01', 145, 21750.00, 'Accessories', 2),
('CBL-400', '2025-12-01', 200, 30000.00, 'Accessories', 2),
('CBL-400', '2026-01-01', 180, 27000.00, 'Accessories', 2),
('CBL-400', '2026-02-01', 220, 33000.00, 'Accessories', 2),
('CBL-400', '2026-03-01', 195, 29250.00, 'Accessories', 2);

-- Forecast Results
INSERT INTO forecast_results (product_sku, forecast_month, forecast_year, predicted_demand, seasonal_factor) VALUES
('WGT-100', 4, 2026, 88.0, 1.1),
('WGT-100', 5, 2026, 92.0, 1.1),
('WGT-100', 6, 2026, 95.0, 1.2),
('SNS-200', 4, 2026, 32.0, 1.05),
('SNS-200', 5, 2026, 35.0, 1.05),
('MTR-300', 4, 2026, 20.0, 1.0),
('CBL-400', 4, 2026, 210.0, 1.0),
('CBL-400', 5, 2026, 230.0, 1.1);

-- Reorder Suggestions
INSERT INTO reorder_suggestions (product_sku, suggested_quantity, reason) VALUES
('SNS-200', 50, 'Stock below reorder threshold. Current: 12, Threshold: 30'),
('MTR-300', 30, 'Stock out! Immediate reorder required.'),
('PCB-500', 25, 'Stock critically low. Current: 8, Threshold: 20'),
('FLT-800', 40, 'Stock below reorder threshold. Current: 3, Threshold: 25'),
('CMP-1000', 10, 'Stock out! Immediate reorder required.');

-- Notifications
INSERT INTO notifications (user_id, notification_type, notification_message, exception_module_name, exception_error_code, is_read) VALUES
(1, 'WARNING', 'Stock for SKU MTR-300 is critically low (0 units). Immediate reorder required.', 'Inventory', 'INV-001', FALSE),
(1, 'ERROR', 'Payment gateway timeout — Order ORD-4823 failed. Error Code: 504', 'Orders', 'ORD-504', FALSE),
(1, 'SUCCESS', 'Order ORD-4821 dispatched successfully via BlueDart.', 'Orders', NULL, TRUE),
(1, 'WARNING', 'Stock for SKU FLT-800 is below reorder point (3 units).', 'Inventory', 'INV-002', FALSE),
(1, 'INFO', 'Shipment SHP-002 is delayed. ETA extended to 5h.', 'Logistics', NULL, TRUE),
(2, 'WARNING', 'Forecast data unavailable for PCB-500. Insufficient historical data.', 'Forecasting', 'FCST-001', FALSE),
(1, 'ERROR', 'Commission ledger calculation failed for AGT-003. Missing commission rate config.', 'Pricing', 'PRC-001', FALSE),
(3, 'INFO', 'New stock transfer request for MTR-300 submitted.', 'Inventory', NULL, FALSE);

-- Audit Log
INSERT INTO audit_log (audit_action_user, audit_action_description, audit_module_name) VALUES
('admin@scm.com', 'User admin logged in successfully.', 'Authentication'),
('admin@scm.com', 'Deleted Order ORD-4800 (test order).', 'Orders'),
('manager@scm.com', 'Updated Stock for MTR-300 to 0 (manual correction).', 'Inventory'),
('admin@scm.com', 'Created new discount rule SAVE20.', 'Pricing'),
('admin@scm.com', 'Assigned role MANAGER to user manager@scm.com.', 'Settings'),
('warehouse@scm.com', 'Added new product PCB-500 to inventory.', 'Inventory'),
('logistics@scm.com', 'Created delivery order SHP-004 via Delhivery.', 'Logistics'),
('admin@scm.com', 'Exported audit log report to PDF.', 'Notifications'),
('manager@scm.com', 'Generated demand forecast for WGT-100 (6 months).', 'Forecasting'),
('admin@scm.com', 'System configuration saved: theme=DARK, language=English.', 'Settings');

-- KPI Snapshots
INSERT INTO kpi_snapshots (warehouse_id, snapshot_date, total_orders, total_revenue, low_stock_count, shipments_today) VALUES
(NULL, '2026-04-09', 1284, 421000.00, 5, 87),
(1, '2026-04-09', 450, 180000.00, 2, 32),
(2, '2026-04-09', 380, 120000.00, 2, 28),
(3, '2026-04-09', 280, 80000.00, 1, 18),
(4, '2026-04-09', 174, 41000.00, 0, 9);

-- Commission Ledger
INSERT INTO commission_ledger (agent_id, order_id, commission_amount, payout_status) VALUES
('AGT-001', 1, 992.00, 'PAID'),
('AGT-001', 6, 1200.00, 'PAID'),
('AGT-002', 2, 410.00, 'PAID'),
('AGT-002', 3, 175.00, 'PAID'),
('AGT-003', 5, 2400.00, 'PENDING'),
('AGT-001', 7, 608.00, 'PENDING');

-- System Config
INSERT INTO system_config (config_key, config_value) VALUES
('app.name', 'SCM Supply Chain Management'),
('app.version', '1.0.0'),
('app.theme.default', 'DARK'),
('session.timeout.minutes', '30'),
('login.max.attempts', '3'),
('login.lockout.minutes', '30'),
('export.default.format', 'PDF'),
('dashboard.refresh.seconds', '60');

-- Return Records
INSERT INTO return_refunds (order_id, reason, return_status, refund_amount) VALUES
(3, 'Product damaged during transit. Customer reported on delivery.', 'APPROVED', 3500.00);
