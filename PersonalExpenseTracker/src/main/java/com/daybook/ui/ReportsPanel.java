package com.daybook.ui;

import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;
import com.daybook.service.TransactionService;
import com.daybook.ui.components.CategoryBreakdownPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Reports and Analytics Module.
 * Calculates detailed monthly breakdowns, savings rate, highest spending category,
 * and comparative spending analytics.
 */
public class ReportsPanel extends JPanel {

    private final TransactionService transactionService;
    private final MainFrame mainFrame;

    private JComboBox<String> monthSelectorCombo;

    // Metric display labels
    private JLabel monthIncomeLabel;
    private JLabel monthExpenseLabel;
    private JLabel monthSavingsLabel;
    private JLabel savingsRateLabel;
    private JLabel topCategoryLabel;

    private CategoryBreakdownPanel categoryBreakdownPanel;
    private DefaultTableModel breakdownTableModel;

    public ReportsPanel(TransactionService transactionService, MainFrame mainFrame) {
        this.transactionService = transactionService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setBackground(UITheme.COLOR_MAIN_BG);

        initUI();
    }

    private void initUI() {
        // Header with Month Selector
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.COLOR_MAIN_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setBackground(UITheme.COLOR_MAIN_BG);

        JLabel titleLabel = new JLabel("Reports & Analytics");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel subLabel = new JLabel("Examine monthly income, spending distribution, and savings rate.");
        subLabel.setFont(UITheme.FONT_SUBTITLE);
        subLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        titleBlock.add(titleLabel);
        titleBlock.add(subLabel);
        headerPanel.add(titleBlock, BorderLayout.WEST);

        // Month Picker
        JPanel monthPickerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        monthPickerPanel.setBackground(UITheme.COLOR_MAIN_BG);

        JLabel selectLbl = new JLabel("Select Period:");
        selectLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        selectLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        monthSelectorCombo = UITheme.createComboBox(generateMonthOptions());
        monthSelectorCombo.setPreferredSize(new Dimension(160, 30));
        monthSelectorCombo.addActionListener(e -> updateReport());

        monthPickerPanel.add(selectLbl);
        monthPickerPanel.add(monthSelectorCombo);
        headerPanel.add(monthPickerPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Main Scrollable Body
        JPanel bodyContainer = new JPanel();
        bodyContainer.setLayout(new BoxLayout(bodyContainer, BoxLayout.Y_AXIS));
        bodyContainer.setBackground(UITheme.COLOR_MAIN_BG);
        bodyContainer.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

        // Row of 4 KPI Metric Cards
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiRow.setBackground(UITheme.COLOR_MAIN_BG);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        monthIncomeLabel = new JLabel("₹0.00");
        monthExpenseLabel = new JLabel("₹0.00");
        monthSavingsLabel = new JLabel("₹0.00");
        savingsRateLabel = new JLabel("0.0%");
        topCategoryLabel = new JLabel("None");

        kpiRow.add(createReportCard("PERIOD INCOME", monthIncomeLabel, UITheme.COLOR_OLIVE));
        kpiRow.add(createReportCard("PERIOD EXPENSES", monthExpenseLabel, UITheme.COLOR_TERRACOTTA));
        kpiRow.add(createReportCard("NET SAVINGS", monthSavingsLabel, UITheme.COLOR_PRIMARY_TEXT));
        kpiRow.add(createReportCard("SAVINGS RATE", savingsRateLabel, UITheme.COLOR_OLIVE));

        bodyContainer.add(kpiRow);
        bodyContainer.add(Box.createVerticalStrut(18));

        // Top Category Highlight Banner
        JPanel highlightCard = UITheme.createCardPanel();
        highlightCard.setLayout(new BorderLayout(14, 0));
        highlightCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        highlightCard.setBackground(UITheme.COLOR_LIGHT_BEIGE);

        JLabel topTitle = new JLabel("HIGHEST SPENDING CATEGORY:");
        topTitle.setFont(UITheme.FONT_LABEL);
        topTitle.setForeground(UITheme.COLOR_MUTED_TEXT);

        topCategoryLabel.setFont(UITheme.FONT_BODY_MEDIUM);
        topCategoryLabel.setForeground(UITheme.COLOR_TERRACOTTA);

        highlightCard.add(topTitle, BorderLayout.WEST);
        highlightCard.add(topCategoryLabel, BorderLayout.CENTER);

        bodyContainer.add(highlightCard);
        bodyContainer.add(Box.createVerticalStrut(18));

        // Two Column Breakdown: Visual Progress on Left, Tabular Breakdown on Right
        JPanel twoColPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        twoColPanel.setBackground(UITheme.COLOR_MAIN_BG);
        twoColPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        // Left: Visual bars
        JPanel visualCard = UITheme.createCardPanel();
        visualCard.setLayout(new BorderLayout(0, 12));

        JLabel visTitle = new JLabel("Expense Proportion by Category");
        visTitle.setFont(UITheme.FONT_BODY_MEDIUM);
        visTitle.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        visualCard.add(visTitle, BorderLayout.NORTH);

        categoryBreakdownPanel = new CategoryBreakdownPanel();
        JScrollPane visualScroll = new JScrollPane(categoryBreakdownPanel);
        visualScroll.setBorder(null);
        visualScroll.setBackground(UITheme.COLOR_CONTENT_BG);
        visualScroll.getViewport().setBackground(UITheme.COLOR_CONTENT_BG);
        visualCard.add(visualScroll, BorderLayout.CENTER);
        twoColPanel.add(visualCard);

        // Right: Detailed Table
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 12));

        JLabel tabTitle = new JLabel("Category Summary Table");
        tabTitle.setFont(UITheme.FONT_BODY_MEDIUM);
        tabTitle.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        tableCard.add(tabTitle, BorderLayout.NORTH);

        String[] cols = {"Category", "Total Spent", "Share (%)"};
        breakdownTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(breakdownTableModel);
        table.setFont(UITheme.FONT_BODY);
        table.setRowHeight(28);
        table.setBackground(UITheme.COLOR_CONTENT_BG);
        table.setGridColor(UITheme.COLOR_BORDER);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(UITheme.FONT_BODY_MEDIUM);
        table.getTableHeader().setBackground(UITheme.COLOR_LIGHT_BEIGE);
        table.getTableHeader().setBorder(UITheme.createBorder());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(UITheme.createBorder());
        tableScroll.getViewport().setBackground(UITheme.COLOR_CONTENT_BG);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        twoColPanel.add(tableCard);

        bodyContainer.add(twoColPanel);

        JScrollPane mainScroll = new JScrollPane(bodyContainer);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        mainScroll.getViewport().setBackground(UITheme.COLOR_MAIN_BG);

        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createReportCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 6));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_LABEL);
        titleLbl.setForeground(UITheme.COLOR_MUTED_TEXT);

        valueLabel.setFont(UITheme.FONT_BIG_NUMBER);
        valueLabel.setForeground(accentColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private String[] generateMonthOptions() {
        LocalDate now = LocalDate.now();
        List<String> options = new ArrayList<>();
        options.add("All Time");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            options.add(ym.format(fmt));
        }
        return options.toArray(new String[0]);
    }

    public void refreshData() {
        updateReport();
    }

    private void updateReport() {
        String selectedMonth = (String) monthSelectorCombo.getSelectedItem();
        String yearMonthFilter = "All Time".equals(selectedMonth) ? null : selectedMonth;

        try {
            List<Transaction> all = transactionService.getAllTransactions();
            List<Transaction> filtered;

            if (yearMonthFilter == null) {
                filtered = all;
            } else {
                filtered = all.stream()
                        .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().startsWith(yearMonthFilter))
                        .toList();
            }

            double income = transactionService.calculateTotalIncome(filtered);
            double expense = transactionService.calculateTotalExpenses(filtered);
            double savings = income - expense;
            double savingsRate = income > 0 ? (savings / income) * 100.0 : 0.0;

            monthIncomeLabel.setText(UITheme.formatCurrency(income));
            monthExpenseLabel.setText(UITheme.formatCurrency(expense));
            monthSavingsLabel.setText(UITheme.formatCurrency(savings));
            monthSavingsLabel.setForeground(savings >= 0 ? UITheme.COLOR_OLIVE : UITheme.COLOR_TERRACOTTA);

            savingsRateLabel.setText(String.format("%.1f%%", savingsRate));
            savingsRateLabel.setForeground(savingsRate >= 0 ? UITheme.COLOR_OLIVE : UITheme.COLOR_TERRACOTTA);

            // Highest category
            Map<String, Double> catMap = transactionService.getCategoryExpenseBreakdown(filtered, yearMonthFilter);
            if (!catMap.isEmpty()) {
                Map.Entry<String, Double> top = catMap.entrySet().iterator().next();
                double pct = expense > 0 ? (top.getValue() / expense) * 100.0 : 0.0;
                topCategoryLabel.setText(String.format("%s — %s (%.1f%% of period expenses)",
                        top.getKey(), UITheme.formatCurrency(top.getValue()), pct));
            } else {
                topCategoryLabel.setText("No expense transactions recorded in this period.");
            }

            // Category breakdown visual
            categoryBreakdownPanel.updateData(catMap, expense);

            // Populate breakdown table
            breakdownTableModel.setRowCount(0);
            for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                double amount = entry.getValue();
                double pct = expense > 0 ? (amount / expense) * 100.0 : 0.0;
                breakdownTableModel.addRow(new Object[]{
                        entry.getKey(),
                        UITheme.formatCurrency(amount),
                        String.format("%.1f%%", pct)
                });
            }

        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Failed to load report data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
