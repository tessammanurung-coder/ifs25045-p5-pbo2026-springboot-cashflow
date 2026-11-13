package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25045springbootcashflow.domain.exception.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Implementasi runtime berbasis JDBC (PostgreSQL); seluruh query memakai PreparedStatement. */
public class JdbcTransactionRepository extends TransactionRepository {
    private static final String COLUMNS =
            "id, user_id, description, amount, type, proof_path, created_at";

    private final DataSource dataSource;

    public JdbcTransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Transaction save(Transaction transaction) {
        try (Connection connection = dataSource.getConnection()) {
            if (transaction.getId() == null) {
                insert(connection, transaction);
            } else {
                update(connection, transaction);
            }
            return transaction;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan transaksi", e);
        }
    }

    private void insert(Connection connection, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, description, amount, type, proof_path, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"})) {
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getDescription());
            ps.setLong(3, t.getAmount());
            ps.setString(4, t.getType().name());
            ps.setString(5, t.getProofPath());
            ps.setTimestamp(6, Timestamp.valueOf(t.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Kolom IDENTITY selalu mengembalikan key; bila tidak, getInt melempar SQLException.
                keys.next();
                t.setId(keys.getInt(1));
            }
        }
    }

    private void update(Connection connection, Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET description = ?, amount = ?, type = ?, proof_path = ?"
                + " WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getDescription());
            ps.setLong(2, t.getAmount());
            ps.setString(3, t.getType().name());
            ps.setString(4, t.getProofPath());
            ps.setInt(5, t.getId());
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("Transaksi", t.getId());
            }
        }
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        List<Transaction> rows = query("SELECT " + COLUMNS + " FROM transactions WHERE id = ?", id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Transaction> findByUserId(Integer userId) {
        if (!isValidId(userId)) {
            return List.of();
        }
        return query("SELECT " + COLUMNS + " FROM transactions WHERE user_id = ? ORDER BY id", userId);
    }

    @Override
    public List<Transaction> findAll() {
        List<Transaction> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT " + COLUMNS + " FROM transactions ORDER BY id")) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal membaca daftar transaksi", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        if (!isValidId(id)) {
            return false;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghapus transaksi", e);
        }
    }

    private List<Transaction> query(String sql, int parameter) {
        List<Transaction> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal membaca transaksi", e);
        }
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("description"),
                rs.getLong("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("proof_path"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
