package com.daybook.service;

import com.daybook.dao.TransactionDAO;
import com.daybook.dao.TransactionDAOImpl;
import com.daybook.database.DatabaseException;
import com.daybook.model.MonthlyComparison;
import com.daybook.model.Transaction;
import com.daybook.model.ValidationException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Layer coordinating business logic, data validation, and analytical calculations.
 * Serves as the intermediary between the Swing UI and DAO persistence layer.
 */
public class TransactionService {

    private final TransactionDAO transactionDAO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TransactionService() {
        this.transactionDAO = new TransactionDAOImpl();
    }

    // Constructor injection for testing
    public TransactionService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    /**
     * Validates and saves a new transaction.
     */
    public int addTransaction(Transaction transaction) throws ValidationException, DatabaseException {
        validateTransaction(transaction);
        return transactionDAO.insert(transaction);
    }

    /**
     * Validates and updates an existing transaction.
     */
    public void updateTransaction(Transaction transaction) throws ValidationException, DatabaseException {
        if (transaction.getId() <= 0) {
            throw new ValidationException("Invalid transaction ID for update.");
        }
        validateTransaction(transaction);
        transactionDAO.update(transaction);
    }

    /**
     * Deletes a transaction by ID.
     */
    public void deleteTransaction(int id) throws DatabaseException {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction ID must be positive.");
        }
        transactionDAO.delete(id);
    }

    /**
     * Fetches all transactions.
     */
    public List<Transaction> getAllTransactions() throws DatabaseException {
        return transactionDAO.getAll();
    }

    /**
     * Fetches recent transactions up to the specified limit.
     */
    public List<Transaction> getRecentTransactions(int limit) throws DatabaseException {
        return transactionDAO.getRecent(limit);
    }

    /**
     * Filters transactions using criteria.
     */
    public List<Transaction> filterTransactions(String query, String type, String category,
                                                String startDate, String endDate) throws DatabaseException {
        return transactionDAO.getFiltered(query, type, category, startDate, endDate);
    }

    /**
     * Clears all transactions.
     */
    public void clearAllTransactions() throws DatabaseException {
        transactionDAO.clearAll();
    }

    /**
     * Gets total count of transactions.
     */
    public int getTransactionCount() throws DatabaseException {
        return transactionDAO.getCount();
    }

    // -------------------------------------------------------------
    // Validation Logic
    // -------------------------------------------------------------
    public void validateTransaction(Transaction tx) throws ValidationException {
        if (tx == null) {
            throw new ValidationException("Transaction object cannot be null.");
        }

        if (tx.getTitle() == null || tx.getTitle().trim().isEmpty()) {
            throw new ValidationException("Description / Title cannot be empty.");
        }

        if (tx.getTitle().trim().length() > 100) {
            throw new ValidationException("Description cannot exceed 100 characters.");
        }

        if (tx.getAmount() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }

        if (Double.isNaN(tx.getAmount()) || Double.isInfinite(tx.getAmount())) {
            throw new ValidationException("Amount must be a valid numeric value.");
        }

        if (tx.getType() == null || (!tx.getType().equalsIgnoreCase("Income") && !tx.getType().equalsIgnoreCase("Expense"))) {
            throw new ValidationException("Transaction type must be 'Income' or 'Expense'.");
        }

        if (tx.getCategory() == null || tx.getCategory().trim().isEmpty()) {
            throw new ValidationException("Please select a valid category.");
        }

        if (tx.getTransactionDate() == null || tx.getTransactionDate().trim().isEmpty()) {
            throw new ValidationException("Transaction date is required.");
        }

        try {
            LocalDate.parse(tx.getTransactionDate().trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must follow the format YYYY-MM-DD (e.g. 2026-11-05).");
        }
    }

    // -------------------------------------------------------------
    // Financial Metrics & Aggregations
    // -------------------------------------------------------------

    public double calculateTotalIncome(List<Transaction> transactions) {
        return transactions.stream()
                .filter(Transaction::isIncome)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double calculateTotalExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double calculateTotalBalance(List<Transaction> transactions) {
        return calculateTotalIncome(transactions) - calculateTotalExpenses(transactions);
    }

    /**
     * Computes total expenses for a target year-month (e.g., "2026-11").
     */
    public double calculateMonthExpenses(String yearMonth, List<Transaction> transactions) {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().startsWith(yearMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Computes total income for a target year-month.
     */
    public double calculateMonthIncome(String yearMonth, List<Transaction> transactions) {
        return transactions.stream()
                .filter(Transaction::isIncome)
                .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().startsWith(yearMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Compares the current month's expenses with the previous month's expenses.
     */
    public MonthlyComparison compareWithPreviousMonth(List<Transaction> transactions, LocalDate referenceDate) {
        YearMonth currentYearMonth = YearMonth.from(referenceDate);
        YearMonth previousYearMonth = currentYearMonth.minusMonths(1);

        String currentKey = currentYearMonth.toString(); // e.g. "2026-11"
        String previousKey = previousYearMonth.toString(); // e.g. "2026-10"

        double currentExp = calculateMonthExpenses(currentKey, transactions);
        double previousExp = calculateMonthExpenses(previousKey, transactions);

        // Check if there are any transactions at all in the previous month
        boolean hasPrevious = transactions.stream()
                .anyMatch(t -> t.getTransactionDate() != null && t.getTransactionDate().startsWith(previousKey));

        return new MonthlyComparison(currentExp, previousExp, hasPrevious);
    }

    /**
     * Calculates category breakdown for expenses in a specific month or all transactions.
     * Returns a sorted map of Category -> Total Amount descending.
     */
    public Map<String, Double> getCategoryExpenseBreakdown(List<Transaction> transactions, String yearMonthOrNull) {
        Map<String, Double> breakdown = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.isExpense()) {
                if (yearMonthOrNull == null || (t.getTransactionDate() != null && t.getTransactionDate().startsWith(yearMonthOrNull))) {
                    String cat = t.getCategory();
                    breakdown.put(cat, breakdown.getOrDefault(cat, 0.0) + t.getAmount());
                }
            }
        }

        // Sort descending by value
        return breakdown.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Identifies the category with the highest spending for a given month or overall.
     * Returns array [CategoryName, FormattedAmount] or null if no expense records.
     */
    public Map.Entry<String, Double> getHighestSpendingCategory(List<Transaction> transactions, String yearMonthOrNull) {
        Map<String, Double> breakdown = getCategoryExpenseBreakdown(transactions, yearMonthOrNull);
        return breakdown.entrySet().stream().findFirst().orElse(null);
    }

    /**
     * Monthly trend aggregation (last N months).
     * Returns LinkedHashMap with YearMonth string ("YYYY-MM") -> Double expense sum.
     */
    public Map<String, Double[]> getMonthlyTrends(List<Transaction> transactions, int lastMonthsCount) {
        LocalDate now = LocalDate.now();
        Map<String, Double[]> result = new LinkedHashMap<>();

        for (int i = lastMonthsCount - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            String key = ym.toString();
            double inc = calculateMonthIncome(key, transactions);
            double exp = calculateMonthExpenses(key, transactions);
            result.put(key, new Double[]{inc, exp});
        }

        return result;
    }

    /**
     * Loads realistic academic demonstration data.
     * Helpful for project evaluations and faculty demonstrations.
     */
    public void loadAcademicDemoData() throws DatabaseException {
        // Clear first
        clearAllTransactions();

        LocalDate now = LocalDate.now();
        String currentMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        LocalDate prevMonthDate = now.minusMonths(1);
        String prevMonth = String.format("%04d-%02d", prevMonthDate.getYear(), prevMonthDate.getMonthValue());

        List<Transaction> sampleData = List.of(
            // Previous month records
            new Transaction("College Semester Tuition", 35000.0, "Expense", "Education", "Bank Transfer", prevMonth + "-04"),
            new Transaction("Monthly Fellowship / Stipend", 45000.0, "Income", "Salary", "Bank Transfer", prevMonth + "-01"),
            new Transaction("Hostel & Mess Advance", 12000.0, "Expense", "Bills", "UPI", prevMonth + "-06"),
            new Transaction("Textbooks & Stationery", 2800.0, "Expense", "Education", "Debit Card", prevMonth + "-10"),
            new Transaction("Local Metro & Bus Pass", 1500.0, "Expense", "Transport", "UPI", prevMonth + "-12"),
            new Transaction("Grocery & Provisions", 4200.0, "Expense", "Food", "Cash", prevMonth + "-18"),
            new Transaction("Freelance Web Project", 18500.0, "Income", "Salary", "UPI", prevMonth + "-22"),
            new Transaction("Weekend Movie & Cafe", 1450.0, "Expense", "Entertainment", "UPI", prevMonth + "-25"),

            // Current month records
            new Transaction("Monthly Fellowship / Stipend", 45000.0, "Income", "Salary", "Bank Transfer", currentMonth + "-01"),
            new Transaction("Campus Canteen & Meals", 3450.0, "Expense", "Food", "UPI", currentMonth + "-02"),
            new Transaction("Monthly High-Speed Internet", 999.0, "Expense", "Bills", "UPI", currentMonth + "-03"),
            new Transaction("Online Research Certification", 3200.0, "Expense", "Education", "Credit Card", currentMonth + "-04"),
            new Transaction("Campus Cab Fare", 480.0, "Expense", "Transport", "UPI", currentMonth + "-05"),
            new Transaction("Pharmacy & Health Essentials", 850.0, "Expense", "Healthcare", "Debit Card", currentMonth + "-05")
        );

        for (Transaction tx : sampleData) {
            transactionDAO.insert(tx);
        }
    }
}
