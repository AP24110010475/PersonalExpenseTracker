package com.daybook.ui;

import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;
import com.daybook.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction History Module.
 * Structured JTable with multi-parameter filtering, search, sorting, and CRUD actions.
 */
public class TransactionPanel extends JPanel {

    private final TransactionService transactionService;
    private final MainFrame mainFrame;

    private DefaultTableModel tableModel;
    private JTable transactionTable;
    private List<Transaction> currentTransactions = new ArrayList<>();

    // Filter controls
    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JComboBox<String> categoryFilterCombo;
    private JTextField startDateField;
    private JTextField endDateField;
    private JComboBox<String> sortOrderCombo;

    // Counter label
    private JLabel countLabel;

    public TransactionPanel(TransactionService transactionService, MainFrame mainFrame) {
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
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 14, 28));

        JLabel titleLabel = new JLabel("Transaction History");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        countLabel = new JLabel("0 records found");
        countLabel.setFont(UITheme.FONT_SUBTITLE);
        countLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(countLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content: Filters Bar + Table Card
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setBackground(UITheme.COLOR_MAIN_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

        // Filter Bar Card
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Search, Type, Category, Sort
        // Search
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_LABEL);
        searchLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        filterCard.add(searchLbl, gbc);

        searchField = UITheme.createTextField();
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.addActionListener(e -> applyFilters());
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        filterCard.add(searchField, gbc);

        // Type
        JLabel typeLbl = new JLabel("Type:");
        typeLbl.setFont(UITheme.FONT_LABEL);
        typeLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        filterCard.add(typeLbl, gbc);

        typeFilterCombo = UITheme.createComboBox(new String[]{"All", "Income", "Expense"});
        typeFilterCombo.addActionListener(e -> applyFilters());
        gbc.gridx = 3;
        gbc.weightx = 0.15;
        filterCard.add(typeFilterCombo, gbc);

        // Category
        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_LABEL);
        catLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        filterCard.add(catLbl, gbc);

        String[] catOptions = new String[Transaction.CATEGORIES.length + 1];
        catOptions[0] = "All";
        System.arraycopy(Transaction.CATEGORIES, 0, catOptions, 1, Transaction.CATEGORIES.length);
        categoryFilterCombo = UITheme.createComboBox(catOptions);
        categoryFilterCombo.addActionListener(e -> applyFilters());
        gbc.gridx = 5;
        gbc.weightx = 0.2;
        filterCard.add(categoryFilterCombo, gbc);

        // Sort
        JLabel sortLbl = new JLabel("Sort:");
        sortLbl.setFont(UITheme.FONT_LABEL);
        sortLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 6;
        gbc.weightx = 0.0;
        filterCard.add(sortLbl, gbc);

        sortOrderCombo = UITheme.createComboBox(new String[]{"Newest First", "Oldest First"});
        sortOrderCombo.addActionListener(e -> applyFilters());
        gbc.gridx = 7;
        gbc.weightx = 0.15;
        filterCard.add(sortOrderCombo, gbc);

        // Row 2: Date Range and Filter / Reset buttons
        JLabel fromLbl = new JLabel("From:");
        fromLbl.setFont(UITheme.FONT_LABEL);
        fromLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        filterCard.add(fromLbl, gbc);

        startDateField = UITheme.createTextField();
        startDateField.setToolTipText("YYYY-MM-DD");
        startDateField.setPreferredSize(new Dimension(100, 28));
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        filterCard.add(startDateField, gbc);

        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(UITheme.FONT_LABEL);
        toLbl.setForeground(UITheme.COLOR_MUTED_TEXT);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        filterCard.add(toLbl, gbc);

        endDateField = UITheme.createTextField();
        endDateField.setToolTipText("YYYY-MM-DD");
        endDateField.setPreferredSize(new Dimension(100, 28));
        gbc.gridx = 3;
        gbc.weightx = 0.2;
        filterCard.add(endDateField, gbc);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBox.setBackground(UITheme.COLOR_CONTENT_BG);

        JButton resetBtn = UITheme.createButton("Reset", false);
        resetBtn.addActionListener(e -> resetFilters());

        JButton applyBtn = UITheme.createButton("Apply Filter", true);
        applyBtn.addActionListener(e -> applyFilters());

        btnBox.add(resetBtn);
        btnBox.add(applyBtn);

        gbc.gridx = 4;
        gbc.gridwidth = 4;
        gbc.weightx = 0.6;
        filterCard.add(btnBox, gbc);
        gbc.gridwidth = 1;

        centerPanel.add(filterCard, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        String[] columnNames = {"ID", "Date", "Description", "Category", "Type", "Payment Method", "Amount"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setFont(UITheme.FONT_BODY);
        transactionTable.setRowHeight(30);
        transactionTable.setBackground(UITheme.COLOR_CONTENT_BG);
        transactionTable.setSelectionBackground(UITheme.COLOR_LIGHT_BEIGE);
        transactionTable.setSelectionForeground(UITheme.COLOR_PRIMARY_TEXT);
        transactionTable.setGridColor(UITheme.COLOR_BORDER);
        transactionTable.setShowVerticalLines(false);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        transactionTable.getTableHeader().setFont(UITheme.FONT_BODY_MEDIUM);
        transactionTable.getTableHeader().setBackground(UITheme.COLOR_LIGHT_BEIGE);
        transactionTable.getTableHeader().setForeground(UITheme.COLOR_PRIMARY_TEXT);
        transactionTable.getTableHeader().setBorder(UITheme.createBorder());

        // Column widths
        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        transactionTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        // Color & Alignment Renderer for Type and Amount
        DefaultTableCellRenderer rightAmountRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.RIGHT);
                String type = (String) table.getValueAt(row, 4);
                if ("Income".equalsIgnoreCase(type)) {
                    setForeground(UITheme.COLOR_OLIVE);
                } else {
                    setForeground(UITheme.COLOR_TERRACOTTA);
                }
                return c;
            }
        };
        transactionTable.getColumnModel().getColumn(6).setCellRenderer(rightAmountRenderer);

        DefaultTableCellRenderer typeBadgeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String type = String.valueOf(value);
                if ("Income".equalsIgnoreCase(type)) {
                    setForeground(UITheme.COLOR_OLIVE);
                } else {
                    setForeground(UITheme.COLOR_TERRACOTTA);
                }
                return c;
            }
        };
        transactionTable.getColumnModel().getColumn(4).setCellRenderer(typeBadgeRenderer);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(UITheme.createBorder());
        scrollPane.getViewport().setBackground(UITheme.COLOR_CONTENT_BG);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar: Edit, Delete, New
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionToolbar.setBackground(UITheme.COLOR_CONTENT_BG);

        JButton newBtn = UITheme.createButton("+ Add Transaction", false);
        newBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.PAGE_ADD_TRANSACTION));

        JButton editBtn = UITheme.createButton("Edit Selected", false);
        editBtn.addActionListener(e -> editSelectedTransaction());

        JButton deleteBtn = UITheme.createButton("Delete Selected", false);
        deleteBtn.setForeground(UITheme.COLOR_TERRACOTTA);
        deleteBtn.addActionListener(e -> deleteSelectedTransaction());

        actionToolbar.add(newBtn);
        actionToolbar.add(editBtn);
        actionToolbar.add(deleteBtn);
        tableCard.add(actionToolbar, BorderLayout.SOUTH);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void refreshData() {
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText().trim();
        String type = (String) typeFilterCombo.getSelectedItem();
        String cat = (String) categoryFilterCombo.getSelectedItem();
        String start = startDateField.getText().trim();
        String end = endDateField.getText().trim();

        try {
            List<Transaction> list = transactionService.filterTransactions(query, type, cat, start, end);

            // Apply in-memory sort order
            boolean oldestFirst = "Oldest First".equals(sortOrderCombo.getSelectedItem());
            if (oldestFirst) {
                list.sort((a, b) -> a.getTransactionDate().compareTo(b.getTransactionDate()));
            } else {
                list.sort((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()));
            }

            currentTransactions = list;
            populateTable(list);

            countLabel.setText(list.size() + " records found");
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Failed to load transactions: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetFilters() {
        searchField.setText("");
        typeFilterCombo.setSelectedIndex(0);
        categoryFilterCombo.setSelectedIndex(0);
        startDateField.setText("");
        endDateField.setText("");
        sortOrderCombo.setSelectedIndex(0);
        applyFilters();
    }

    private void populateTable(List<Transaction> list) {
        tableModel.setRowCount(0);
        for (Transaction t : list) {
            String prefix = t.isIncome() ? "+ " : "- ";
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getTransactionDate(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getType(),
                    t.getPaymentMethod(),
                    prefix + UITheme.formatCurrency(t.getAmount())
            });
        }
    }

    private void editSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a transaction to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        Transaction tx = currentTransactions.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);

        if (tx != null) {
            EditTransactionDialog dialog = new EditTransactionDialog(mainFrame, tx, transactionService);
            dialog.setVisible(true);

            if (dialog.isSaved()) {
                mainFrame.refreshAllPanels();
            }
        }
    }

    private void deleteSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a transaction to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 2);
        String amount = (String) tableModel.getValueAt(selectedRow, 6);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete transaction #" + id + ": \"" + title + "\" (" + amount + ")?\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                transactionService.deleteTransaction(id);
                mainFrame.refreshAllPanels();
            } catch (DatabaseException e) {
                JOptionPane.showMessageDialog(this, "Failed to delete: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
