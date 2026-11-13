package org.delcom.ifs25045springbootcashflow.adapter.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.domain.entity.*;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Menguji adapter JDBC terhadap H2 (mode PostgreSQL), termasuk foreign key dan unique constraint. */
class JdbcRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 4, 12, 30, 15);

    private DataSource ds;
    private JdbcUserRepository users;
    private JdbcTransactionRepository transactions;

    private static DataSource newDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        return ds;
    }

    @BeforeEach
    void setUp() {
        ds = newDataSource();
        new SchemaInitializer(ds).initialize();
        users = new JdbcUserRepository(ds);
        transactions = new JdbcTransactionRepository(ds);
    }

    private User saveUser(String username) {
        return users.save(new User(null, username, "Nama " + username, "hash", Role.USER, null, NOW));
    }

    @Test
    void schemaInitializationIsIdempotent() {
        new SchemaInitializer(ds).initialize();
        saveUser("a");
        new SchemaInitializer(ds).initialize();
        assertEquals(1, users.findAll().size(), "data tidak hilang saat inisialisasi ulang");
    }

    @Test
    void schemaInitializerWrapsSqlErrors() {
        DataSource broken = new JdbcDataSource();
        ((JdbcDataSource) broken).setURL("jdbc:h2:tcp://localhost:1/nonexistent");
        assertThrows(RepositoryException.class, () -> new SchemaInitializer(broken).initialize());
    }

    @Test
    void userRoundTrip() {
        User u = saveUser("Budi");
        assertNotNull(u.getId());
        User loaded = users.findById(u.getId()).orElseThrow();
        assertEquals("budi", loaded.getUsername(), "username disimpan huruf kecil");
        assertEquals(Role.USER, loaded.getRole());
        assertEquals(NOW, loaded.getCreatedAt());
        assertEquals(u.getId(), users.findByUsername(" budi ").orElseThrow().getId());
        loaded.setName("Baru");
        loaded.setPasswordHash("hash2");
        loaded.setPhotoPath("/uploads/a.png");
        users.save(loaded);
        User updated = users.findById(u.getId()).orElseThrow();
        assertEquals("Baru", updated.getName());
        assertEquals("hash2", updated.getPasswordHash());
        assertEquals("/uploads/a.png", updated.getPhotoPath());
    }

    @Test
    void userEdgeCases() {
        saveUser("budi");
        assertThrows(ValidationException.class, () -> saveUser("BUDI"));
        assertTrue(users.findById(null).isEmpty());
        assertTrue(users.findById(0).isEmpty());
        assertTrue(users.findById(999).isEmpty());
        assertTrue(users.findByUsername(null).isEmpty());
        assertTrue(users.findByUsername(" ").isEmpty());
        assertTrue(users.findByUsername("nobody").isEmpty());
        User ghost = new User(999, "ghost", "G", "h", Role.USER, null, NOW);
        assertThrows(EntityNotFoundException.class, () -> users.save(ghost));
        assertFalse(users.deleteById(null));
        assertFalse(users.deleteById(999));
    }

    @Test
    void userDeleteAndAdminRole() {
        User admin = users.save(new User(null, "root", "Admin", "h", Role.ADMIN, null, NOW));
        assertEquals(Role.ADMIN, users.findById(admin.getId()).orElseThrow().getRole());
        assertTrue(users.deleteById(admin.getId()));
        assertTrue(users.findAll().isEmpty());
    }

    @Test
    void transactionRoundTripPreservesAllFields() {
        User u = saveUser("budi");
        Transaction t = transactions.save(new Transaction(null, u.getId(), "Gaji", 5_000_000_000L,
                TransactionType.INCOME, "/uploads/p.png", NOW));
        assertNotNull(t.getId());
        Transaction loaded = transactions.findById(t.getId()).orElseThrow();
        assertEquals("Gaji", loaded.getDescription());
        assertEquals(5_000_000_000L, loaded.getAmount());
        assertEquals(TransactionType.INCOME, loaded.getType());
        assertEquals("/uploads/p.png", loaded.getProofPath());
        assertEquals(NOW, loaded.getCreatedAt());
        assertEquals(u.getId(), loaded.getUserId());
    }

    @Test
    void transactionUpdateDeleteAndFindByUser() {
        User a = saveUser("a");
        User b = saveUser("b");
        Transaction t1 = transactions.save(new Transaction(null, a.getId(), "satu", 1, TransactionType.INCOME, null, NOW));
        transactions.save(new Transaction(null, a.getId(), "dua", 2, TransactionType.EXPENSE, null, NOW));
        transactions.save(new Transaction(null, b.getId(), "tiga", 3, TransactionType.INCOME, null, NOW));
        assertEquals(3, transactions.findAll().size());
        assertEquals(2, transactions.findByUserId(a.getId()).size());
        assertEquals(1, transactions.findByUserId(b.getId()).size());
        assertTrue(transactions.findByUserId(null).isEmpty());
        assertTrue(transactions.findByUserId(0).isEmpty());
        assertTrue(transactions.findByUserId(999).isEmpty());
        t1.update("satu-b", 11, TransactionType.EXPENSE, "/uploads/x.png");
        transactions.save(t1);
        Transaction reloaded = transactions.findById(t1.getId()).orElseThrow();
        assertEquals("satu-b", reloaded.getDescription());
        assertEquals(TransactionType.EXPENSE, reloaded.getType());
        assertTrue(transactions.deleteById(t1.getId()));
        assertFalse(transactions.deleteById(t1.getId()));
        assertFalse(transactions.deleteById(null));
        assertTrue(transactions.findById(null).isEmpty());
        assertTrue(transactions.findById(-4).isEmpty());
        assertTrue(transactions.findById(999).isEmpty());
        Transaction ghost = new Transaction(999, a.getId(), "x", 1, TransactionType.INCOME, null, NOW);
        assertThrows(EntityNotFoundException.class, () -> transactions.save(ghost));
    }

    @Test
    void foreignKeyRejectsUnknownUser() {
        Transaction orphan = new Transaction(null, 12345, "yatim", 1, TransactionType.INCOME, null, NOW);
        assertThrows(RepositoryException.class, () -> transactions.save(orphan));
    }

    @Test
    void dataSurvivesNewRepositoryInstances() {
        User u = saveUser("budi");
        transactions.save(new Transaction(null, u.getId(), "kekal", 5, TransactionType.INCOME, null, NOW));
        JdbcTransactionRepository fresh = new JdbcTransactionRepository(ds);
        assertEquals("kekal", fresh.findByUserId(u.getId()).get(0).getDescription());
    }

    @Test
    void sqlFailuresAreWrapped() throws SQLException {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE transactions");
            s.execute("DROP TABLE users");
        }
        Transaction t = new Transaction(null, 1, "x", 1, TransactionType.INCOME, null, NOW);
        assertThrows(RepositoryException.class, () -> transactions.save(t));
        assertThrows(RepositoryException.class, () -> transactions.findAll());
        assertThrows(RepositoryException.class, () -> transactions.findById(1));
        assertThrows(RepositoryException.class, () -> transactions.findByUserId(1));
        assertThrows(RepositoryException.class, () -> transactions.deleteById(1));
        User u = new User(null, "x", "X", "h", Role.USER, null, NOW);
        assertThrows(RepositoryException.class, () -> users.save(u));
        assertThrows(RepositoryException.class, () -> users.findAll());
        assertThrows(RepositoryException.class, () -> users.findById(1));
        assertThrows(RepositoryException.class, () -> users.findByUsername("x"));
        assertThrows(RepositoryException.class, () -> users.deleteById(1));
    }
}
