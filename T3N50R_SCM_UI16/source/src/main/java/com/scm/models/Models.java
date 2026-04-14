package com.scm.models;

/**
 * All data model classes for SCM UI Subsystem #16.
 */
public class Models {

    public static class UIUser {
        public int userId;
        public String username;
        public String email;
        public String displayName;
        public String passwordHash;
        public String role;
        public boolean isLocked;
        public int loginAttempts;
        public String lastLogin;
        public String sessionToken;
        public String theme;
        public String language;
    }

    public static class Product {
        public int productId;
        public String sku;
        public String productName;
        public String category;
        public int stockQty;
        public int reorderPoint;
        public String status;
        public int warehouseId;
        public double price;
        public String barcode;
    }

    public static class Order {
        public int orderId;
        public String orderCode;
        public int customerId;
        public String customerName;
        public String status;
        public double totalAmount;
        public String discountCode;
        public String deliveryAddress;
        public String createdAt;
        public String updatedAt;
    }

    public static class OrderItem {
        public int itemId;
        public int orderId;
        public int productId;
        public String sku;
        public String productName;
        public int quantity;
        public double unitPrice;
        public double subtotal;
    }

    public static class Shipment {
        public int shipmentId;
        public String shipmentCode;
        public int orderId;
        public String carrier;
        public String origin;
        public String destination;
        public String status;
        public String eta;
        public double lat;
        public double lng;
        public String trackingNumber;
    }

    public static class DashboardKPI {
        public int totalOrders;
        public double totalRevenue;
        public int lowStockCount;
        public int shipmentsToday;
        public double revenueChange;
        public String period;
    }

    public static class UINotification {
        public int notificationId;
        public String type;
        public String message;
        public String module;
        public boolean isRead;
        public String timestamp;
        public String refId;
    }

    public static class AuditLog {
        public int logId;
        public String timestamp;
        public String actionUser;
        public String action;
        public String module;
        public String ipAddress;
    }

    public static class PriceTier {
        public int tierId;
        public String tierName;
        public double pricePerUnit;
        public int minOrderQty;
        public String currency = "INR";
    }

    public static class DiscountRule {
        public int ruleId;
        public String code;
        public String type;
        public double value;
        public String validFrom;
        public String validTo;
        public double minOrderValue;
        public boolean isActive = true;
    }

    public static class SalesRecord {
        public int recordId;
        public String monthYear;
        public int productId;
        public String category;
        public int unitsSold;
        public double revenue;
        public int warehouseId;
    }

    public static class Warehouse {
        public int warehouseId;
        public String warehouseName;
        public String location;
        public String city;
        public int capacity;
        public int currentStock;
        public String managerId;
    }

    public static class Customer {
        public int customerId;
        public String customerName;
        public String email;
        public String phone;
        public String address;
        public String tier;
        public double totalOrders;
    }
}
