package com.daybook.ui;

import com.daybook.database.DatabaseConnection;
import com.daybook.database.DatabaseException;
import com.daybook.service.TransactionService;

import javax.swing.*;
import java.awt.*;

/**
 * Settings Module.
 * Displays application meta-information, database statistics, currency configuration,
 * and data maintenance actions (reset / demo data loading).
 */
public class SettingsPanel extends JPanel {

    private final TransactionService transactionService;
    private final MainFrame mainFrame;

    private JLabel recordCountLabel;
    private JLabel dbLocationLabel;

    public SettingsPanel(TransactionService transactionService, MainFrame mainFrame) {
        this.transactionService = transactionService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setBackground(UITheme.COLOR_MAIN_BG);

        initUI();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.COLOR_MAIN_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        JLabel titleLabel = new JLabel("System Settings & Data");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel subLabel = new JLabel("Application configuration and SQLite database diagnostics.");
        subLabel.setFont(UITheme.FONT_SUBTITLE);
        subLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(UITheme.COLOR_MAIN_BG);
        container.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

        // 1. Application Info Card
        JPanel appInfoCard = UITheme.createCardPanel();
        appInfoCard.setLayout(new GridLayout(4, 2, 10, 8));
        appInfoCard.setMaximumSize(new Dimension(800, 140));

        appInfoCard.add(createMetaLabel("Application:"));
        appInfoCard.add(createValueLabel("DAYBOOK – Personal Finance Manager"));

        appInfoCard.add(createMetaLabel("Version:"));
        appInfoCard.add(createValueLabel("1.0.0 (Academic Edition)"));

        appInfoCard.add(createMetaLabel("Platform & Runtime:"));
        appInfoCard.add(createValueLabel("Java SE 17 | Java Swing GUI | Maven"));

        appInfoCard.add(createMetaLabel("Active Currency:"));
        appInfoCard.add(createValueLabel("Indian Rupee (INR — ₹)"));

        container.add(wrapWithTitle("APPLICATION INFORMATION", appInfoCard));
        container.add(Box.createVerticalStrut(20));

        // 2. Database Information Card
        JPanel dbCard = UITheme.createCardPanel();
        dbCard.setLayout(new GridLayout(3, 2, 10, 8));
        dbCard.setMaximumSize(new Dimension(800, 110));

        dbCard.add(createMetaLabel("Database Engine:"));
        dbCard.add(createValueLabel("SQLite 3 (via Xerial JDBC Driver)"));

        dbCard.add(createMetaLabel("Database Location:"));
        dbLocationLabel = createValueLabel(DatabaseConnection.getDatabaseLocation());
        dbCard.add(dbLocationLabel);

        dbCard.add(createMetaLabel("Total Stored Records:"));
        recordCountLabel = createValueLabel("0 transactions");
        dbCard.add(recordCountLabel);

        container.add(wrapWithTitle("DATABASE STATUS", dbCard));
        container.add(Box.createVerticalStrut(20));

        // 3. Maintenance & Actions Card
        JPanel actionCard = UITheme.createCardPanel();
        actionCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 8));
        actionCard.setMaximumSize(new Dimension(800, 70));

        JButton loadDemoBtn = UITheme.createButton("Load Sample Academic Data", false);
        loadDemoBtn.setToolTipText("Populates representative college expense records for demonstration");
        loadDemoBtn.addActionListener(e -> loadDemoData());

        JButton clearBtn = UITheme.createButton("Clear All Transaction Data", false);
        clearBtn.setForeground(UITheme.COLOR_TERRACOTTA);
        clearBtn.setToolTipText("Permanently deletes all transactions from SQLite");
        clearBtn.addActionListener(e -> confirmAndClearAll());

        actionCard.add(loadDemoBtn);
        actionCard.add(clearBtn);

        container.add(wrapWithTitle("DATA MAINTENANCE", actionCard));

        add(container, BorderLayout.CENTER);
    }

    private JPanel wrapWithTitle(String title, JComponent component) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(UITheme.COLOR_MAIN_BG);
        wrapper.setMaximumSize(new Dimension(800, component.getMaximumSize().height + 30));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_LABEL);
        titleLbl.setForeground(UITheme.COLOR_MUTED_TEXT);

        wrapper.add(titleLbl, BorderLayout.NORTH);
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel createMetaLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY_MEDIUM);
        lbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        return lbl;
    }

    private JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        return lbl;
    }

    public void refreshData() {
        try {
            int count = transactionService.getTransactionCount();
            recordCountLabel.setText(count + " transactions");
            dbLocationLabel.setText(DatabaseConnection.getDatabaseLocation());
        } catch (DatabaseException e) {
            recordCountLabel.setText("Database error");
        }
    }

    private void loadDemoData() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "This will populate realistic college student income and expense records.\nExisting records will be refreshed. Proceed?",
                "Load Academic Demonstration Data",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                transactionService.loadAcademicDemoData();
                JOptionPane.showMessageDialog(this, "Sample academic transactions loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.refreshAllPanels();
                mainFrame.navigateTo(MainFrame.PAGE_OVERVIEW);
            } catch (DatabaseException e) {
                JOptionPane.showMessageDialog(this, "Failed to load sample data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void confirmAndClearAll() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "WARNING: Are you sure you want to permanently delete ALL transaction records from the database?\nThis action cannot be undone.",
                "Confirm Clear All Data",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                transactionService.clearAllTransactions();
                JOptionPane.showMessageDialog(this, "All records have been cleared from SQLite.", "Data Reset", JOptionPane.INFORMATION_MESSAGE);
                mainFrame.refreshAllPanels();
                mainFrame.navigateTo(MainFrame.PAGE_OVERVIEW);
            } catch (DatabaseException e) {
                JOptionPane.showMessageDialog(this, "Failed to clear database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
