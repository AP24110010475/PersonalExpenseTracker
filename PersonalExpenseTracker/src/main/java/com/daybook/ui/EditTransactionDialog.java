package com.daybook.ui;

import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;
import com.daybook.model.ValidationException;
import com.daybook.service.TransactionService;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog for editing an existing transaction.
 */
public class EditTransactionDialog extends JDialog {

    private final Transaction transaction;
    private final TransactionService transactionService;
    private boolean saved = false;

    private JTextField titleField;
    private JTextField amountField;
    private JRadioButton expenseRadio;
    private JRadioButton incomeRadio;
    private JComboBox<String> categoryCombo;
    private JTextField dateField;
    private JComboBox<String> paymentMethodCombo;
    private JLabel errorLabel;

    public EditTransactionDialog(Frame owner, Transaction transaction, TransactionService transactionService) {
        super(owner, "Edit Transaction #" + transaction.getId(), true);
        this.transaction = transaction;
        this.transactionService = transactionService;

        setSize(480, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(UITheme.COLOR_CONTENT_BG);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(UITheme.COLOR_CONTENT_BG);
        container.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Error Banner
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_TERRACOTTA);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        container.add(errorLabel, gbc);
        gbc.gridwidth = 1;

        // Type
        JLabel typeLbl = new JLabel("Type:");
        typeLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        typeLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        container.add(typeLbl, gbc);

        JPanel typeP = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        typeP.setBackground(UITheme.COLOR_CONTENT_BG);
        expenseRadio = new JRadioButton("Expense", transaction.isExpense());
        expenseRadio.setBackground(UITheme.COLOR_CONTENT_BG);
        expenseRadio.setForeground(UITheme.COLOR_TERRACOTTA);
        incomeRadio = new JRadioButton("Income", transaction.isIncome());
        incomeRadio.setBackground(UITheme.COLOR_CONTENT_BG);
        incomeRadio.setForeground(UITheme.COLOR_OLIVE);
        ButtonGroup group = new ButtonGroup();
        group.add(expenseRadio);
        group.add(incomeRadio);
        typeP.add(expenseRadio);
        typeP.add(incomeRadio);

        gbc.gridx = 1;
        container.add(typeP, gbc);

        // Description
        JLabel descLbl = new JLabel("Description:");
        descLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        descLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        container.add(descLbl, gbc);

        titleField = UITheme.createTextField();
        titleField.setText(transaction.getTitle());
        gbc.gridx = 1;
        container.add(titleField, gbc);

        // Amount
        JLabel amtLbl = new JLabel("Amount (₹):");
        amtLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        amtLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        container.add(amtLbl, gbc);

        amountField = UITheme.createTextField();
        amountField.setText(String.valueOf(transaction.getAmount()));
        gbc.gridx = 1;
        container.add(amountField, gbc);

        // Category
        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        catLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        container.add(catLbl, gbc);

        categoryCombo = UITheme.createComboBox(Transaction.CATEGORIES);
        categoryCombo.setSelectedItem(transaction.getCategory());
        gbc.gridx = 1;
        container.add(categoryCombo, gbc);

        // Date
        JLabel dateLbl = new JLabel("Date (YYYY-MM-DD):");
        dateLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        dateLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 5;
        container.add(dateLbl, gbc);

        dateField = UITheme.createTextField();
        dateField.setText(transaction.getTransactionDate());
        gbc.gridx = 1;
        container.add(dateField, gbc);

        // Payment Method
        JLabel methodLbl = new JLabel("Payment Method:");
        methodLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        methodLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 6;
        container.add(methodLbl, gbc);

        paymentMethodCombo = UITheme.createComboBox(Transaction.PAYMENT_METHODS);
        paymentMethodCombo.setSelectedItem(transaction.getPaymentMethod());
        gbc.gridx = 1;
        container.add(paymentMethodCombo, gbc);

        add(container, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(UITheme.COLOR_CONTENT_BG);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.COLOR_BORDER));

        JButton cancelBtn = UITheme.createButton("Cancel", false);
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = UITheme.createButton("Update Record", true);
        saveBtn.addActionListener(e -> performUpdate());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void performUpdate() {
        String title = titleField.getText().trim();
        String amountStr = amountField.getText().trim();
        String type = incomeRadio.isSelected() ? "Income" : "Expense";
        String cat = (String) categoryCombo.getSelectedItem();
        String date = dateField.getText().trim();
        String method = (String) paymentMethodCombo.getSelectedItem();

        if (title.isEmpty()) {
            errorLabel.setText("Description cannot be empty.");
            return;
        }

        double amt;
        try {
            amt = Double.parseDouble(amountStr);
            if (amt <= 0) {
                errorLabel.setText("Amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid amount format.");
            return;
        }

        transaction.setTitle(title);
        transaction.setAmount(amt);
        transaction.setType(type);
        transaction.setCategory(cat);
        transaction.setTransactionDate(date);
        transaction.setPaymentMethod(method);

        try {
            transactionService.updateTransaction(transaction);
            saved = true;
            dispose();
        } catch (ValidationException | DatabaseException ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
