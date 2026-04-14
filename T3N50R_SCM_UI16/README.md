# SCM Supply Chain Management — UI Subsystem #16
## Team T3N50R

---

## Project Overview
Full Java Swing + MySQL Supply Chain Management portal with 10 UI components,
luxury dark gold theme, JFreeChart analytics, and complete exception handling (E:001–E:045).

---

## Quick Start

### Prerequisites
```bash
# Debian/Ubuntu
sudo apt-get install -y default-jre libjfreechart-java libmariadb-java

# macOS (Homebrew)
brew install openjdk
# Download jfreechart-1.0.19.jar + jcommon-1.0.23.jar + mariadb-java-client-2.7.6.jar
```

### Run (Demo Mode — No DB Required)
```bash
# Linux/macOS
chmod +x run.sh && ./run.sh

# Windows
run.bat
```

### Setup MySQL Database (Optional)
```bash
mysql -u root -p < database_setup.sql
# Then set env vars before running:
export SCM_DB_URL="jdbc:mysql://localhost:3306/scm_db"
export SCM_DB_USER="root"
export SCM_DB_PASS="yourpassword"
./run.sh
```

---

## Demo Credentials

| Username     | Password      | Role              |
|-------------|---------------|-------------------|
| admin       | admin123      | SUPER_ADMIN       |
| manager     | manager123    | MANAGER           |
| warehouse1  | warehouse123  | WAREHOUSE_STAFF   |
| logistics1  | logistics123  | LOGISTICS_OFFICER |
| sales1      | sales123      | SALES_STAFF       |

---

## Components Implemented

| ID   | Component                    | Exceptions Handled          |
|------|------------------------------|-----------------------------|
| C-01 | Authentication & RBAC        | E:001–E:006                 |
| C-02 | Navigation & Layout          | E:005–E:006                 |
| C-03 | Dashboard & Analytics        | E:007–E:011                 |
| C-04 | Inventory & Warehouse        | E:012–E:016                 |
| C-05 | Order Management             | E:017–E:023                 |
| C-06 | Transport & Logistics        | E:024–E:028                 |
| C-07 | Pricing, Discount, Commission| E:029–E:033                 |
| C-08 | Demand Forecasting & Reports | E:034–E:037                 |
| C-09 | Notifications & Exceptions   | E:038–E:040                 |
| C-10 | User & System Settings       | E:041–E:045                 |

**Total: 45 exception codes fully implemented**

---

## Build from Source

```bash
CP="/usr/share/java/jfreechart.jar:/usr/share/java/jcommon.jar:/usr/share/java/mariadb-java-client.jar"
cd source
mkdir -p target/classes
find src -name "*.java" > sources.txt
javac -cp "$CP" -sourcepath src/main/java -d target/classes @sources.txt
jar cfe ../scm-t3n50r.jar com.scm.ui.SCMApplication -C target/classes .
```

---

## Architecture

- **UI Framework**: Java Swing with custom dark theme (BG #0D111A, Gold #D4AF37)
- **Database**: MySQL via MariaDB JDBC connector
- **Charts**: JFreeChart 1.0.19 (bar, line, pie, trend)
- **Authentication**: Session-based with BCrypt-compatible password hashing
- **Demo Mode**: All panels fall back to hardcoded data when DB is unavailable
- **Threading**: SwingWorker for all DB operations (EDT never blocked)

---

## File Structure

```
T3N50R_SCM_UI16/
├── scm-t3n50r.jar          ← Runnable JAR
├── database_setup.sql      ← Complete MySQL schema + seed data
├── run.sh                  ← Linux/macOS launch script
├── run.bat                 ← Windows launch script
├── README.md               ← This file
└── source/
    ├── pom.xml
    └── src/main/java/com/scm/
        ├── auth/           ← LoginScreen, AuthService (C-01)
        ├── ui/             ← MainFrame, SCMColors, SCMComponents (C-02)
        ├── dashboard/      ← DashboardPanel (C-03)
        ├── inventory/      ← InventoryPanel (C-04)
        ├── orders/         ← OrdersPanel (C-05)
        ├── logistics/      ← LogisticsPanel (C-06)
        ├── pricing/        ← PricingPanel (C-07)
        ├── forecast/       ← ForecastPanel (C-08)
        ├── notifications/  ← NotificationsPanel (C-09)
        ├── settings/       ← SettingsPanel (C-10)
        ├── db/             ← DatabaseConnection
        └── models/         ← Models (all POJOs)
```
