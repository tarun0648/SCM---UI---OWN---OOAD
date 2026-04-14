package com.scm.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Singleton database connection manager for SCM application.
 * Handles MySQL connection with auto-reconnect support.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // ── Connection settings (override via env or config file) ──────────────────
    private static String DB_URL  = System.getProperty("scm.db.url",
            "jdbc:mysql://localhost:3306/scm_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static String DB_USER = System.getProperty("scm.db.user", "root");
    private static String DB_PASS = System.getProperty("scm.db.pass", "root");

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        connect();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns a live connection, reconnecting if necessary. */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connect();
            }
        } catch (SQLException e) {
            LOGGER.warning("Connection check failed – reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            LOGGER.info("✅ Connected to SCM database.");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("MySQL JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.severe("Failed to connect to database: " + e.getMessage());
            // Allow app to start in demo mode
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing connection: " + e.getMessage());
        }
    }

    /** Allow runtime reconfiguration (e.g., from settings screen). */
    public static void configure(String url, String user, String pass) {
        DB_URL  = url;
        DB_USER = user;
        DB_PASS = pass;
        if (instance != null) {
            instance.close();
            instance = new DatabaseConnection();
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
