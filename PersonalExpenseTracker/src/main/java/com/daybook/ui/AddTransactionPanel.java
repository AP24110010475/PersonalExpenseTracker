package com.daybook.ui;

import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;
import com.daybook.model.ValidationException;
import com.daybook.service.TransactionService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Add Transaction Module.
 * Structured form allowing users to record income or expenses with rigorous input validation.
 */
public class AddTransactionPanel extends JPanel {

    private final TransactionService transactionService;
    private final MainFrame mainFrame;

    // Form Components
    private JRadioButton expenseRadio;
    private JRadioButton incomeRadio;
    private JTextField titleField;
    private JTextField amountField;
    private JComboBox<String> categoryCombo;
    private JTextField dateField;
    private JComboBox<String> paymentMethodCombo;

    // Status Banner
    private JLabel notificationLabel;

    public AddTransactionPanel(TransactionService transactionService, MainFrame mainFrame) {
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

        JLabel titleLabel = new JLabel("Record New Transaction");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel subtitleLabel = new JLabel("Enter transaction details below to update your financial records.");
        subtitleLabel.setFont(UITheme.FONT_SUBTITLE);
        subtitleLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form Container
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 28, 0));
        centerWrapper.setBackground(UITheme.COLOR_MAIN_BG);

        JPanel formCard = UITheme.createCardPanel();
        formCard.setPreferredSize(new Dimension(680, 520));
        formCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Notification / Status Banner
        notificationLabel = new JLabel(" ");
        notificationLabel.setFont(UITheme.FONT_LABEL);
        notificationLabel.setOpaque(true);
        notificationLabel.setBackground(UITheme.COLOR_CONTENT_BG);
        notificationLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formCard.add(notificationLabel, gbc);
        gbc.gridwidth = 1;

        // 1. Transaction Type
        JLabel typeLabel = createFieldLabel("Transaction Type *");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formCard.add(typeLabel, gbc);

        JPanel typeRadioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        typeRadioPanel.setBackground(UITheme.COLOR_CONTENT_BG);

        expenseRadio = new JRadioButton("Expense", true);
        expenseRadio.setFont(UITheme.FONT_BODY_MEDIUM);
        expenseRadio.setBackground(UITheme.COLOR_CONTENT_BG);
        expenseRadio.setForeground(UITheme.COLOR_TERRACOTTA);
        expenseRadio.setFocusPainted(false);

        incomeRadio = new JRadioButton("Income", false);
        incomeRadio.setFont(UITheme.FONT_BODY_MEDIUM);
        incomeRadio.setBackground(UITheme.COLOR_CONTENT_BG);
        incomeRadio.setForeground(UITheme.COLOR_OLIVE);
        incomeRadio.setFocusPainted(false);

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(expenseRadio);
        typeGroup.add(incomeRadio);

        typeRadioPanel.add(expenseRadio);
        typeRadioPanel.add(incomeRadio);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(typeRadioPanel, gbc);

        // 2. Title / Description
        JLabel descLabel = createFieldLabel("Title / Description *");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        formCard.add(descLabel, gbc);

        titleField = UITheme.createTextField();
        titleField.setToolTipText("e.g., Grocery Shopping, Monthly Fellowship, Books");
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(titleField, gbc);

        // 3. Amount
        JLabel amountLabel = createFieldLabel("Amount (INR ₹) *");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        formCard.add(amountLabel, gbc);

        amountField = UITheme.createTextField();
        amountField.setToolTipText("Enter positive numerical amount, e.g. 450.00");
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(amountField, gbc);

        // 4. Category
        JLabel catLabel = createFieldLabel("Category *");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        formCard.add(catLabel, gbc);

        categoryCombo = UITheme.createComboBox(Transaction.CATEGORIES);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(categoryCombo, gbc);

        // Auto-switch category recommendation when Income is selected
        incomeRadio.addActionListener(e -> categoryCombo.setSelectedItem("Salary"));
        expenseRadio.addActionListener(e -> categoryCombo.setSelectedItem("Food"));

        // 5. Transaction Date
        JLabel dateLabel = createFieldLabel("Date (YYYY-MM-DD) *");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        formCard.add(dateLabel, gbc);

        dateField = UITheme.createTextField();
        dateField.setText(LocalDate.now().toString());
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(dateField, gbc);

        // 6. Payment Method
        JLabel methodLabel = createFieldLabel("Payment Method");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.3;
        formCard.add(methodLabel, gbc);

        paymentMethodCombo = UITheme.createComboBox(Transaction.PAYMENT_METHODS);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formCard.add(paymentMethodCombo, gbc);

        // Separator
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.COLOR_BORDER);
        formCard.add(sep, gbc);

        // Action Buttons: Save, Clear, Cancel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(UITheme.COLOR_CONTENT_BG);

        JButton cancelBtn = UITheme.createButton("Cancel", false);
        cancelBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.PAGE_OVERVIEW));

        JButton clearBtn = UITheme.createButton("Clear Form", false);
        clearBtn.addActionListener(e -> resetForm());

        JButton saveBtn = UITheme.createButton("Save Transaction", true);
        saveBtn.addActionListener(e -> saveTransaction());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(saveBtn);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        formCard.add(buttonPanel, gbc);

        centerWrapper.add(formCard);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_BODY_MEDIUM);
        label.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        return label;
    }

    private void saveTransaction() {
        hideNotification();

        String title = titleField.getText().trim();
        String amountText = amountField.getText().trim();
        String type = incomeRadio.isSelected() ? "Income" : "Expense";
        String category = (String) categoryCombo.getSelectedItem();
        String date = dateField.getText().trim();
        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();

        // 1. Validate fields
        if (title.isEmpty()) {
            showError("Description cannot be empty.");
            titleField.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showError("Amount must be greater than zero.");
                amountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Amount must be a valid numeric figure (e.g. 250.50).");
            amountField.requestFocus();
            return;
        }

        if (category == null || category.isEmpty()) {
            showError("Please select a transaction category.");
            return;
        }

        Transaction tx = new Transaction(title, amount, type, category, paymentMethod, date);

        try {
            transactionService.addTransaction(tx);
            showSuccess("Transaction saved successfully: " + title + " (" + UITheme.formatCurrency(amount) + ")");
            resetForm();
            // Refresh main dashboard data
            mainFrame.refreshAllPanels();
        } catch (ValidationException ve) {
            showError("Validation Error: " + ve.getMessage());
        } catch (DatabaseException de) {
            showError("Database Error: " + de.getMessage());
        }
    }

    public void resetForm() {
        expenseRadio.setSelected(true);
        titleField.setText("");
        amountField.setText("");
        categoryCombo.setSelectedIndex(0);
        dateField.setText(LocalDate.now().toString());
        paymentMethodCombo.setSelectedIndex(0);
        titleField.requestFocus();
    }

    private void showError(String msg) {
        notificationLabel.setText("⚠ " + msg);
        notificationLabel.setForeground(UITheme.COLOR_TERRACOTTA);
        notificationLabel.setBackground(new Color(0xF7, 0xEB, 0xE7));
        notificationLabel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(UITheme.COLOR_TERRACOTTA, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    private void showSuccess(String msg) {
        notificationLabel.setText("✓ " + msg);
        notificationLabel.setForeground(UITheme.COLOR_OLIVE);
        notificationLabel.setBackground(new Color(0xF0, 0xF3, 0xEB));
        notificationLabel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(UITheme.COLOR_OLIVE, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    private void hideNotification() {
        notificationLabel.setText(" ");
        notificationLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        notificationLabel.setBackground(UITheme.COLOR_CONTENT_BG);
    }
}
