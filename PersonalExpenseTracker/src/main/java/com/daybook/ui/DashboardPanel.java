package com.daybook.ui;

import com.daybook.database.DatabaseException;
import com.daybook.model.MonthlyComparison;
import com.daybook.model.Transaction;
import com.daybook.service.TransactionService;
import com.daybook.ui.components.CategoryBreakdownPanel;
import com.daybook.ui.components.SpendingBarChart;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Dashboard & Overview Panel.
 * Displays key financial metrics, month-over-month spending trend,
 * monthly spending bar chart, category breakdown, and recent transactions.
 */
public class DashboardPanel extends JPanel {

    private final TransactionService transactionService;
    private final MainFrame mainFrame;

    // Stat metric value labels
    private JLabel totalBalanceLabel;
    private JLabel totalIncomeLabel;
    private JLabel totalExpenseLabel;
    private JLabel currentMonthExpenseLabel;
    private JLabel comparisonIndicatorLabel;

    // Recent Transactions Table
    private DefaultTableModel recentTableModel;
    private JTable recentTable;

    // Visual summaries
    private SpendingBarChart spendingBarChart;
    private CategoryBreakdownPanel categoryBreakdownPanel;

    // Empty state container
    private JPanel contentContainer;
    private JPanel emptyStatePanel;
    private JPanel metricsGridPanel;

    public DashboardPanel(TransactionService transactionService, MainFrame mainFrame) {
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

        JLabel titleLabel = new JLabel("Financial Overview");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel dateLabel = new JLabel("Today: " + LocalDate.now());
        dateLabel.setFont(UITheme.FONT_SUBTITLE);
        dateLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(dateLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Scrollable Main Content
        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBackground(UITheme.COLOR_MAIN_BG);
        contentContainer.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

        // Setup Metric Cards
        metricsGridPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGridPanel.setBackground(UITheme.COLOR_MAIN_BG);
        metricsGridPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        totalBalanceLabel = new JLabel("₹0.00");
        totalIncomeLabel = new JLabel("₹0.00");
        totalExpenseLabel = new JLabel("₹0.00");
        currentMonthExpenseLabel = new JLabel("₹0.00");
        comparisonIndicatorLabel = new JLabel("—");

        metricsGridPanel.add(createMetricCard("TOTAL BALANCE", totalBalanceLabel, UITheme.COLOR_PRIMARY_TEXT));
        metricsGridPanel.add(createMetricCard("TOTAL INCOME", totalIncomeLabel, UITheme.COLOR_OLIVE));
        metricsGridPanel.add(createMetricCard("TOTAL EXPENSES", totalExpenseLabel, UITheme.COLOR_TERRACOTTA));
        metricsGridPanel.add(createMonthExpenseCard("CURRENT MONTH", currentMonthExpenseLabel, comparisonIndicatorLabel));

        contentContainer.add(metricsGridPanel);
        contentContainer.add(Box.createVerticalStrut(20));

        // Two-column layout: Left = Chart & Recent, Right = Category Breakdown
        JPanel middleSplit = new JPanel(new GridBagLayout());
        middleSplit.setBackground(UITheme.COLOR_MAIN_BG);
        middleSplit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Box: Spending Trend Chart
        gbc.gridx = 0;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, 16);

        JPanel chartCard = UITheme.createCardPanel();
        chartCard.setLayout(new BorderLayout(0, 10));
        JLabel chartTitle = new JLabel("Monthly Spending Trend (Last 6 Months)");
        chartTitle.setFont(UITheme.FONT_BODY_MEDIUM);
        chartTitle.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        chartCard.add(chartTitle, BorderLayout.NORTH);

        spendingBarChart = new SpendingBarChart();
        chartCard.add(spendingBarChart, BorderLayout.CENTER);
        middleSplit.add(chartCard, gbc);

        // Right Box: Category Breakdown
        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel catCard = UITheme.createCardPanel();
        catCard.setLayout(new BorderLayout(0, 10));
        JLabel catTitle = new JLabel("Category Breakdown");
        catTitle.setFont(UITheme.FONT_BODY_MEDIUM);
        catTitle.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        catCard.add(catTitle, BorderLayout.NORTH);

        categoryBreakdownPanel = new CategoryBreakdownPanel();
        JScrollPane catScroll = new JScrollPane(categoryBreakdownPanel);
        catScroll.setBorder(null);
        catScroll.setBackground(UITheme.COLOR_CONTENT_BG);
        catScroll.getViewport().setBackground(UITheme.COLOR_CONTENT_BG);
        catCard.add(catScroll, BorderLayout.CENTER);
        middleSplit.add(catCard, gbc);

        contentContainer.add(middleSplit);
        contentContainer.add(Box.createVerticalStrut(20));

        // Bottom Box: Recent Transactions Table
        JPanel recentCard = UITheme.createCardPanel();
        recentCard.setLayout(new BorderLayout(0, 12));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setBackground(UITheme.COLOR_CONTENT_BG);
        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(UITheme.FONT_BODY_MEDIUM);
        recentTitle.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JButton viewAllBtn = new JButton("View All →");
        viewAllBtn.setFont(UITheme.FONT_LABEL);
        viewAllBtn.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        viewAllBtn.setContentAreaFilled(false);
        viewAllBtn.setBorderPainted(false);
        viewAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAllBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.PAGE_TRANSACTIONS));

        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(viewAllBtn, BorderLayout.EAST);
        recentCard.add(recentHeader, BorderLayout.NORTH);

        String[] cols = {"Date", "Description", "Category", "Type", "Method", "Amount"};
        recentTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentTable = new JTable(recentTableModel);
        recentTable.setFont(UITheme.FONT_BODY);
        recentTable.setRowHeight(28);
        recentTable.setBackground(UITheme.COLOR_CONTENT_BG);
        recentTable.setSelectionBackground(UITheme.COLOR_LIGHT_BEIGE);
        recentTable.setSelectionForeground(UITheme.COLOR_PRIMARY_TEXT);
        recentTable.setGridColor(UITheme.COLOR_BORDER);
        recentTable.setShowVerticalLines(false);
        recentTable.getTableHeader().setFont(UITheme.FONT_BODY_MEDIUM);
        recentTable.getTableHeader().setBackground(UITheme.COLOR_LIGHT_BEIGE);
        recentTable.getTableHeader().setForeground(UITheme.COLOR_PRIMARY_TEXT);
        recentTable.getTableHeader().setBorder(UITheme.createBorder());

        // Color renderer for Amount & Type
        DefaultTableCellRenderer amountRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.RIGHT);
                String type = (String) table.getValueAt(row, 3);
                if ("Income".equalsIgnoreCase(type)) {
                    setForeground(UITheme.COLOR_OLIVE);
                } else {
                    setForeground(UITheme.COLOR_TERRACOTTA);
                }
                return c;
            }
        };
        recentTable.getColumnModel().getColumn(5).setCellRenderer(amountRenderer);

        JScrollPane tableScroll = new JScrollPane(recentTable);
        tableScroll.setBorder(UITheme.createBorder());
        tableScroll.setPreferredSize(new Dimension(800, 160));
        recentCard.add(tableScroll, BorderLayout.CENTER);

        contentContainer.add(recentCard);

        // Empty state component
        emptyStatePanel = createEmptyStatePanel();

        JScrollPane mainScroll = new JScrollPane(contentContainer);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        mainScroll.setBackground(UITheme.COLOR_MAIN_BG);
        mainScroll.getViewport().setBackground(UITheme.COLOR_MAIN_BG);

        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
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

    private JPanel createMonthExpenseCard(String title, JLabel valueLabel, JLabel compLabel) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 4));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_LABEL);
        titleLbl.setForeground(UITheme.COLOR_MUTED_TEXT);

        valueLabel.setFont(UITheme.FONT_BIG_NUMBER);
        valueLabel.setForeground(UITheme.COLOR_TERRACOTTA);

        compLabel.setFont(UITheme.FONT_SMALL);
        compLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UITheme.COLOR_CONTENT_BG);
        bottom.add(valueLabel, BorderLayout.NORTH);
        bottom.add(compLabel, BorderLayout.SOUTH);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel createEmptyStatePanel() {
        JPanel panel = UITheme.createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                UITheme.createBorder(),
                BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));

        JLabel iconLabel = new JLabel("[ No Transactions Recorded ]");
        iconLabel.setFont(UITheme.FONT_HEADER);
        iconLabel.setForeground(UITheme.COLOR_MUTED_TEXT);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("Welcome to DAYBOOK. Start tracking your income and expenses.");
        msgLabel.setFont(UITheme.FONT_BODY);
        msgLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addBtn = UITheme.createButton("+ Record First Transaction", true);
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.PAGE_ADD_TRANSACTION));

        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(msgLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(addBtn);

        return panel;
    }

    public void refreshData() {
        try {
            List<Transaction> all = transactionService.getAllTransactions();

            if (all.isEmpty()) {
                // Show clean empty state message
                totalBalanceLabel.setText("₹0.00");
                totalIncomeLabel.setText("₹0.00");
                totalExpenseLabel.setText("₹0.00");
                currentMonthExpenseLabel.setText("₹0.00");
                comparisonIndicatorLabel.setText("No transactions yet");

                recentTableModel.setRowCount(0);
                spendingBarChart.setData(null);
                categoryBreakdownPanel.updateData(null, 0);
                return;
            }

            // Calculate metrics from database
            double totalIncome = transactionService.calculateTotalIncome(all);
            double totalExpenses = transactionService.calculateTotalExpenses(all);
            double balance = transactionService.calculateTotalBalance(all);

            totalBalanceLabel.setText(UITheme.formatCurrency(balance));
            totalIncomeLabel.setText(UITheme.formatCurrency(totalIncome));
            totalExpenseLabel.setText(UITheme.formatCurrency(totalExpenses));

            // Current Month vs Previous Month
            LocalDate now = LocalDate.now();
            String currentYearMonth = YearMonth.from(now).toString();
            double currentMonthExp = transactionService.calculateMonthExpenses(currentYearMonth, all);
            currentMonthExpenseLabel.setText(UITheme.formatCurrency(currentMonthExp));

            MonthlyComparison comp = transactionService.compareWithPreviousMonth(all, now);
            if (!comp.hasPreviousData()) {
                comparisonIndicatorLabel.setText("First month on record");
                comparisonIndicatorLabel.setForeground(UITheme.COLOR_MUTED_TEXT);
            } else if (comp.isSpendingMore()) {
                comparisonIndicatorLabel.setText(String.format("▲ +%.1f%% vs last month (%s)",
                        comp.getPercentageChange(), UITheme.formatCompactCurrency(comp.getDifference())));
                comparisonIndicatorLabel.setForeground(UITheme.COLOR_TERRACOTTA);
            } else if (comp.isSpendingLess()) {
                comparisonIndicatorLabel.setText(String.format("▼ %.1f%% vs last month (%s)",
                        comp.getPercentageChange(), UITheme.formatCompactCurrency(Math.abs(comp.getDifference()))));
                comparisonIndicatorLabel.setForeground(UITheme.COLOR_OLIVE);
            } else {
                comparisonIndicatorLabel.setText("Same as last month");
                comparisonIndicatorLabel.setForeground(UITheme.COLOR_MUTED_TEXT);
            }

            // Update Chart
            Map<String, Double[]> trends = transactionService.getMonthlyTrends(all, 6);
            spendingBarChart.setData(trends);

            // Update Category Breakdown
            Map<String, Double> catMap = transactionService.getCategoryExpenseBreakdown(all, null);
            categoryBreakdownPanel.updateData(catMap, totalExpenses);

            // Populate Recent Transactions (limit 6)
            recentTableModel.setRowCount(0);
            List<Transaction> recent = transactionService.getRecentTransactions(6);
            for (Transaction t : recent) {
                String prefix = t.isIncome() ? "+ " : "- ";
                recentTableModel.addRow(new Object[]{
                        t.getTransactionDate(),
                        t.getTitle(),
                        t.getCategory(),
                        t.getType(),
                        t.getPaymentMethod(),
                        prefix + UITheme.formatCurrency(t.getAmount())
                });
            }

        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Failed to load dashboard data: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
