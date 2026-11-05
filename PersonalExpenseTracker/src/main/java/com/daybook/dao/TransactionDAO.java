package com.daybook.dao;

import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;

import java.util.List;

/**
 * Interface defining the Data Access Object (DAO) contract for Transaction entities.
 * Demonstrates abstraction, separating business logic from raw database operations.
 */
public interface TransactionDAO {

    /**
     * Inserts a new transaction into the database.
     * @param transaction the transaction to insert
     * @return generated ID of the inserted record
     * @throws DatabaseException if an SQL error occurs
     */
    int insert(Transaction transaction) throws DatabaseException;

    /**
     * Retrieves a single transaction by primary key ID.
     * @param id transaction ID
     * @return Transaction object or null if not found
     * @throws DatabaseException if an SQL error occurs
     */
    Transaction getById(int id) throws DatabaseException;

    /**
     * Retrieves all transactions ordered by transaction date descending.
     * @return List of all transactions
     * @throws DatabaseException if an SQL error occurs
     */
    List<Transaction> getAll() throws DatabaseException;

    /**
     * Retrieves the most recent N transactions.
     * @param limit number of transactions to return
     * @return List of recent transactions
     * @throws DatabaseException if an SQL error occurs
     */
    List<Transaction> getRecent(int limit) throws DatabaseException;

    /**
     * Searches and filters transactions based on multiple criteria.
     * @param query text match on title
     * @param type "Income", "Expense", or "All"
     * @param category category name or "All"
     * @param startDate start date string (inclusive) or null
     * @param endDate end date string (inclusive) or null
     * @return filtered list of transactions
     * @throws DatabaseException if an SQL error occurs
     */
    List<Transaction> getFiltered(String query, String type, String category, String startDate, String endDate) throws DatabaseException;

    /**
     * Updates an existing transaction by ID.
     * @param transaction modified transaction object
     * @throws DatabaseException if an SQL error occurs
     */
    void update(Transaction transaction) throws DatabaseException;

    /**
     * Deletes a transaction by ID.
     * @param id transaction ID to delete
     * @throws DatabaseException if an SQL error occurs
     */
    void delete(int id) throws DatabaseException;

    /**
     * Clears all transactions from the table.
     * @throws DatabaseException if an SQL error occurs
     */
    void clearAll() throws DatabaseException;

    /**
     * Returns the total count of transactions in the database.
     * @return count of rows
     * @throws DatabaseException if an SQL error occurs
     */
    int getCount() throws DatabaseException;
}
