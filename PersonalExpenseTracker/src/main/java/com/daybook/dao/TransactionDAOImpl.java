package com.daybook.dao;

import com.daybook.database.DatabaseConnection;
import com.daybook.database.DatabaseException;
import com.daybook.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of TransactionDAO using SQLite.
 * Uses PreparedStatements for all queries to ensure security against SQL injection.
 */
public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public int insert(Transaction transaction) throws DatabaseException {
        String sql = """
            INSERT INTO transactions (title, amount, type, category, payment_method, transaction_date)
            VALUES (?, ?, ?, ?, ?, ?);
            """;

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, transaction.getTitle());
            pstmt.setDouble(2, transaction.getAmount());
            pstmt.setString(3, transaction.getType());
            pstmt.setString(4, transaction.getCategory());
            pstmt.setString(5, transaction.getPaymentMethod());
            pstmt.setString(6, transaction.getTransactionDate());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    transaction.setId(id);
                    return id;
                } else {
                    throw new DatabaseException("Creating transaction failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public Transaction getById(int id) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE id = ?;";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTransaction(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching transaction ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getAll() throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC;";
        List<Transaction> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getRecent(int limit) throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT ?;";
        List<Transaction> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching recent transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getFiltered(String query, String type, String category,
                                         String startDate, String endDate) throws DatabaseException {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND LOWER(title) LIKE ? ");
            params.add("%" + query.trim().toLowerCase() + "%");
        }

        if (type != null && !type.equalsIgnoreCase("All") && !type.trim().isEmpty()) {
            sql.append("AND type = ? ");
            params.add(type.trim());
        }

        if (category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty()) {
            sql.append("AND category = ? ");
            params.add(category.trim());
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append("AND transaction_date >= ? ");
            params.add(startDate.trim());
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append("AND transaction_date <= ? ");
            params.add(endDate.trim());
        }

        sql.append("ORDER BY transaction_date DESC, id DESC;");

        List<Transaction> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error filtering transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Transaction transaction) throws DatabaseException {
        String sql = """
            UPDATE transactions
            SET title = ?, amount = ?, type = ?, category = ?, payment_method = ?, transaction_date = ?
            WHERE id = ?;
            """;

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transaction.getTitle());
            pstmt.setDouble(2, transaction.getAmount());
            pstmt.setString(3, transaction.getType());
            pstmt.setString(4, transaction.getCategory());
            pstmt.setString(5, transaction.getPaymentMethod());
            pstmt.setString(6, transaction.getTransactionDate());
            pstmt.setInt(7, transaction.getId());

            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Update failed, transaction with ID " + transaction.getId() + " not found.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM transactions WHERE id = ?;";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting transaction ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void clearAll() throws DatabaseException {
        String sql = "DELETE FROM transactions;";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            // Reset autoincrement sequence
            try (Statement resetStmt = conn.createStatement()) {
                resetStmt.execute("DELETE FROM sqlite_sequence WHERE name='transactions';");
            } catch (SQLException ignored) {
                // Ignore if sqlite_sequence not yet populated
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error clearing transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public int getCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM transactions;";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting transactions: " + e.getMessage(), e);
        }
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getDouble("amount"),
            rs.getString("type"),
            rs.getString("category"),
            rs.getString("payment_method"),
            rs.getString("transaction_date"),
            rs.getString("created_at")
        );
    }
}
