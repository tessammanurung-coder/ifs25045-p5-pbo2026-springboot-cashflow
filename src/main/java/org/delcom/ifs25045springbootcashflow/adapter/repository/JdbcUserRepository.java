package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25045springbootcashflow.domain.exception.RepositoryException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;

/** Implementasi runtime berbasis JDBC (PostgreSQL); seluruh query memakai PreparedStatement. */
public class JdbcUserRepository extends UserRepository {
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String COLUMNS = "id, username, name, password_hash, role, photo_path, created_at";

    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User save(User user) {
        try (Connection connection = dataSource.getConnection()) {
            if (user.getId() == null) {
                insert(connection, user);
            } else {
                update(connection, user);
            }
            return user;
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ValidationException("Username sudah digunakan");
            }
            throw new RepositoryException("Gagal menyimpan pengguna", e);
        }
    }

    private void insert(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users (username, name, password_hash, role, photo_path, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"})) {
            // Username disimpan huruf kecil agar UNIQUE database tidak peka huruf besar/kecil.
            ps.setString(1, user.getUsername().toLowerCase(Locale.ROOT));
            ps.setString(2, user.getName());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getPhotoPath());
            ps.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Kolom IDENTITY selalu mengembalikan key; bila tidak, getInt melempar SQLException.
                keys.next();
                user.setId(keys.getInt(1));
            }
        }
    }

    private void update(Connection connection, User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, password_hash = ?, photo_path = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getPhotoPath());
            ps.setInt(4, user.getId());
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("Pengguna", user.getId());
            }
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE LOWER(username) = LOWER(?)", username.trim());
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT " + COLUMNS + " FROM users ORDER BY id")) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal membaca daftar pengguna", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        if (!isValidId(id)) {
            return false;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghapus pengguna", e);
        }
    }

    private Optional<User> queryOne(String sql, Object parameter) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal membaca pengguna", e);
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("photo_path"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
