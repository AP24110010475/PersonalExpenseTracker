package com.daybook.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database manager managing the SQLite JDBC connection.
 * Automatically bootstraps the database schema if not present upon initial launch.
 */
public class DatabaseConnection {

    private static final String DEFAULT_DB_NAME = "expense_tracker.db";
    private static String databaseUrl = "jdbc:sqlite:" + DEFAULT_DB_NAME;
    private static Connection connection = null;

    // Private constructor prevents direct instantiation (Singleton pattern)
    private DatabaseConnection() {}

    /**
     * Configure a custom database path (useful for testing or custom locations).
     */
    public static synchronized void setDatabasePath(String path) {
        closeConnection();
        databaseUrl = "jdbc:sqlite:" + path;
    }

    /**
     * Retrieves an active connection to the SQLite database.
     * Reopens the connection if closed.
     *
     * @return active java.sql.Connection instance
     * @throws DatabaseException if connection fails
     */
    public static synchronized Connection getConnection() throws DatabaseException {
        try {
            if (connection == null || connection.isClosed()) {
                // Load SQLite JDBC Driver class
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(databaseUrl);
                
                // Enable foreign keys and WAL mode for better concurrency and safety
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                    stmt.execute("PRAGMA journal_mode = WAL;");
                }
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found in classpath.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish SQLite database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Bootstraps the required database tables if they do not exist.
     * Called on application startup.
     *
     * @throws DatabaseException if schema creation fails
     */
    public static synchronized void initializeDatabase() throws DatabaseException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                payment_method TEXT,
                transaction_date TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            );
            """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
            CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);
            CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
            """;

        try {
            Connection conn = getConnection();
            try (Statement statement = conn.createStatement()) {
                statement.execute(createTableSQL);
                statement.execute(createIndexSQL);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database tables: " + e.getMessage(), e);
        }
    }

    /**
     * Safely closes the active connection when the application exits.
     */
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Notice: Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Returns the resolved file path of the database.
     */
    public static String getDatabaseLocation() {
        if (databaseUrl.startsWith("jdbc:sqlite:")) {
            String path = databaseUrl.substring("jdbc:sqlite:".length());
            File file = new File(path);
            return file.getAbsolutePath();
        }
        return databaseUrl;
    }
}
