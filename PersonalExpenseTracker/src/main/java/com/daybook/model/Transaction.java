package com.daybook.model;

import java.util.Objects;

/**
 * Model class representing a financial transaction (Income or Expense).
 * Demonstrates encapsulation with private fields, validation, and standard accessors.
 */
public class Transaction {

    private int id;
    private String title;
    private double amount;
    private String type; // "Income" or "Expense"
    private String category;
    private String paymentMethod;
    private String transactionDate; // Format: YYYY-MM-DD
    private String createdAt;

    // Standard categories
    public static final String[] CATEGORIES = {
        "Food",
        "Transport",
        "Education",
        "Shopping",
        "Entertainment",
        "Healthcare",
        "Bills",
        "Salary",
        "Other"
    };

    // Standard payment methods
    public static final String[] PAYMENT_METHODS = {
        "Cash",
        "UPI",
        "Debit Card",
        "Credit Card",
        "Bank Transfer"
    };

    // Default constructor
    public Transaction() {
    }

    // Constructor for creating new transactions before database ID assignment
    public Transaction(String title, double amount, String type, String category,
                       String paymentMethod, String transactionDate) {
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.transactionDate = transactionDate;
    }

    // Full constructor for retrieving stored transactions with ID and timestamp
    public Transaction(int id, String title, double amount, String type, String category,
                       String paymentMethod, String transactionDate, String createdAt) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    // Getters and Setters (Encapsulation)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return "Income".equalsIgnoreCase(this.type);
    }

    public boolean isExpense() {
        return "Expense".equalsIgnoreCase(this.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", transactionDate='" + transactionDate + '\'' +
                '}';
    }
}
