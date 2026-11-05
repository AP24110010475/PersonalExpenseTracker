package com.daybook;

import com.daybook.database.DatabaseConnection;
import com.daybook.database.DatabaseException;
import com.daybook.ui.MainFrame;

import javax.swing.*;

/**
 * Application Entry Point for DAYBOOK – Personal Expense Tracker.
 * Initializes SQLite database and launches Swing UI on the Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  DAYBOOK – Personal Finance Manager (Java Swing)");
        System.out.println("==================================================");

        // 1. Initialize SQLite Database Schema
        try {
            DatabaseConnection.initializeDatabase();
            System.out.println("[Database] SQLite initialized successfully at: " + DatabaseConnection.getDatabaseLocation());
        } catch (DatabaseException e) {
            System.err.println("[Database Error] Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database Initialization Failed:\n" + e.getMessage(),
                    "Fatal Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Set cross-platform look and feel with clean font rendering
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fallback to default
        }

        // 3. Register shutdown hook to gracefully close SQLite connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Lifecycle] Application shutting down. Closing database connection...");
            DatabaseConnection.closeConnection();
        }));

        // 4. Launch Main UI Frame on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                System.out.println("[UI] Main window displayed successfully.");
            } catch (Exception e) {
                System.err.println("[UI Error] Error launching main window: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
