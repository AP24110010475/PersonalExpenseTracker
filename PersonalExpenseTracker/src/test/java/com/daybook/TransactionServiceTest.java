package com.daybook;

import com.daybook.database.DatabaseConnection;
import com.daybook.database.DatabaseException;
import com.daybook.model.MonthlyComparison;
import com.daybook.model.Transaction;
import com.daybook.model.ValidationException;
import com.daybook.service.TransactionService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit and Integration Tests for DAYBOOK.
 * Verifies CRUD, validations, mathematical calculations, and SQLite persistence.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionServiceTest {

    private static TransactionService service;
    private static final String TEST_DB = "test_expense_tracker.db";

    @BeforeAll
    public static void setUpAll() throws DatabaseException {
        // Point to separate test SQLite database
        DatabaseConnection.setDatabasePath(TEST_DB);
        DatabaseConnection.initializeDatabase();
        service = new TransactionService();
    }

    @AfterAll
    public static void tearDownAll() {
        DatabaseConnection.closeConnection();
        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @BeforeEach
    public void setUp() throws DatabaseException {
        service.clearAllTransactions();
    }

    @Test
    @Order(1)
    public void testAddValidIncomeTransaction() throws Exception {
        Transaction tx = new Transaction("Monthly Stipend", 25000.0, "Income", "Salary", "Bank Transfer", "2026-11-01");
        int id = service.addTransaction(tx);
        assertTrue(id > 0, "Generated ID should be positive");

        List<Transaction> all = service.getAllTransactions();
        assertEquals(1, all.size());
        assertEquals("Monthly Stipend", all.get(0).getTitle());
        assertEquals(25000.0, all.get(0).getAmount(), 0.001);
        assertTrue(all.get(0).isIncome());
    }

    @Test
    @Order(2)
    public void testAddValidExpenseTransaction() throws Exception {
        Transaction tx = new Transaction("Books & Stationery", 1200.0, "Expense", "Education", "UPI", "2026-11-02");
        int id = service.addTransaction(tx);
        assertTrue(id > 0);

        List<Transaction> all = service.getAllTransactions();
        assertEquals(1, all.size());
        assertTrue(all.get(0).isExpense());
    }

    @Test
    @Order(3)
    public void testValidationAmountMustBePositive() {
        Transaction tx = new Transaction("Invalid Zero Amount", 0.0, "Expense", "Food", "Cash", "2026-11-02");
        assertThrows(ValidationException.class, () -> service.addTransaction(tx));

        Transaction txNegative = new Transaction("Invalid Negative Amount", -50.0, "Expense", "Food", "Cash", "2026-11-02");
        assertThrows(ValidationException.class, () -> service.addTransaction(txNegative));
    }

    @Test
    @Order(4)
    public void testValidationTitleCannotBeBlank() {
        Transaction tx = new Transaction("   ", 100.0, "Expense", "Food", "Cash", "2026-11-02");
        assertThrows(ValidationException.class, () -> service.addTransaction(tx));
    }

    @Test
    @Order(5)
    public void testValidationDatePattern() {
        Transaction tx = new Transaction("Wrong Date", 100.0, "Expense", "Food", "Cash", "05-11-2026");
        assertThrows(ValidationException.class, () -> service.addTransaction(tx));
    }

    @Test
    @Order(6)
    public void testBalanceCalculations() throws Exception {
        service.addTransaction(new Transaction("Salary", 50000.0, "Income", "Salary", "Bank Transfer", "2026-11-01"));
        service.addTransaction(new Transaction("Rent", 15000.0, "Expense", "Bills", "UPI", "2026-11-02"));
        service.addTransaction(new Transaction("Groceries", 5000.0, "Expense", "Food", "UPI", "2026-11-03"));

        List<Transaction> list = service.getAllTransactions();

        double income = service.calculateTotalIncome(list);
        double expense = service.calculateTotalExpenses(list);
        double balance = service.calculateTotalBalance(list);

        assertEquals(50000.0, income, 0.001);
        assertEquals(20000.0, expense, 0.001);
        assertEquals(30000.0, balance, 0.001);
    }

    @Test
    @Order(7)
    public void testUpdateTransaction() throws Exception {
        Transaction tx = new Transaction("Cafeteria Lunch", 200.0, "Expense", "Food", "Cash", "2026-11-03");
        int id = service.addTransaction(tx);

        tx.setId(id);
        tx.setTitle("Cafeteria Special Lunch");
        tx.setAmount(250.0);
        service.updateTransaction(tx);

        List<Transaction> list = service.getAllTransactions();
        assertEquals(1, list.size());
        assertEquals("Cafeteria Special Lunch", list.get(0).getTitle());
        assertEquals(250.0, list.get(0).getAmount(), 0.001);
    }

    @Test
    @Order(8)
    public void testDeleteTransaction() throws Exception {
        int id = service.addTransaction(new Transaction("Temporary", 50.0, "Expense", "Other", "Cash", "2026-11-03"));
        assertEquals(1, service.getTransactionCount());

        service.deleteTransaction(id);
        assertEquals(0, service.getTransactionCount());
    }

    @Test
    @Order(9)
    public void testFilterByQueryAndCategory() throws Exception {
        service.addTransaction(new Transaction("College Textbook", 800.0, "Expense", "Education", "UPI", "2026-11-01"));
        service.addTransaction(new Transaction("Bus Ticket", 50.0, "Expense", "Transport", "Cash", "2026-11-02"));
        service.addTransaction(new Transaction("Campus Canteen", 120.0, "Expense", "Food", "UPI", "2026-11-02"));

        List<Transaction> searchMatch = service.filterTransactions("Textbook", "All", "All", null, null);
        assertEquals(1, searchMatch.size());
        assertEquals("College Textbook", searchMatch.get(0).getTitle());

        List<Transaction> catMatch = service.filterTransactions(null, "Expense", "Food", null, null);
        assertEquals(1, catMatch.size());
        assertEquals("Campus Canteen", catMatch.get(0).getTitle());
    }

    @Test
    @Order(10)
    public void testMonthlyComparison() throws Exception {
        // October: 10,000 expense
        service.addTransaction(new Transaction("Oct Rent", 10000.0, "Expense", "Bills", "UPI", "2026-10-05"));
        // November: 12,000 expense (20% increase)
        service.addTransaction(new Transaction("Nov Rent", 12000.0, "Expense", "Bills", "UPI", "2026-11-05"));

        List<Transaction> list = service.getAllTransactions();
        MonthlyComparison comp = service.compareWithPreviousMonth(list, LocalDate.of(2026, 11, 10));

        assertTrue(comp.hasPreviousData());
        assertTrue(comp.isSpendingMore());
        assertEquals(2000.0, comp.getDifference(), 0.01);
        assertEquals(20.0, comp.getPercentageChange(), 0.01);
    }

    @Test
    @Order(11)
    public void testCategoryBreakdown() throws Exception {
        service.addTransaction(new Transaction("Lunch", 300.0, "Expense", "Food", "UPI", "2026-11-01"));
        service.addTransaction(new Transaction("Dinner", 400.0, "Expense", "Food", "UPI", "2026-11-02"));
        service.addTransaction(new Transaction("Metro", 200.0, "Expense", "Transport", "UPI", "2026-11-02"));

        List<Transaction> list = service.getAllTransactions();
        Map<String, Double> breakdown = service.getCategoryExpenseBreakdown(list, "2026-11");

        assertEquals(700.0, breakdown.get("Food"), 0.01);
        assertEquals(200.0, breakdown.get("Transport"), 0.01);

        Map.Entry<String, Double> top = service.getHighestSpendingCategory(list, "2026-11");
        assertNotNull(top);
        assertEquals("Food", top.getKey());
        assertEquals(700.0, top.getValue(), 0.01);
    }
}
