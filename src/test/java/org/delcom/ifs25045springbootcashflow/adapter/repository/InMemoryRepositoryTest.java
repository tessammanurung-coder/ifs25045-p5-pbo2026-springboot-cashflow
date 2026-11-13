package org.delcom.ifs25045springbootcashflow.adapter.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.domain.entity.*;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 8, 0);

    private static Transaction tx(int userId, String d) {
        return new Transaction(null, userId, d, 10, TransactionType.INCOME, null, NOW);
    }

    private static User user(String username) {
        return new User(null, username, "N", "h", Role.USER, null, NOW);
    }

    @Test
    void transactionCrud() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        Transaction a = repo.save(tx(1, "a"));
        Transaction b = repo.save(tx(2, "b"));
        repo.save(tx(1, "c"));
        assertEquals(1, a.getId());
        assertEquals(2, b.getId());
        assertEquals(3, repo.findAll().size());
        assertEquals(2, repo.findByUserId(1).size());
        assertTrue(repo.findByUserId(null).isEmpty());
        assertTrue(repo.findByUserId(0).isEmpty());
        a.update("a2", 20, TransactionType.EXPENSE, null);
        repo.save(a);
        assertEquals("a2", repo.findById(1).orElseThrow().getDescription());
        assertTrue(repo.findById(null).isEmpty());
        assertTrue(repo.findById(-1).isEmpty());
        assertTrue(repo.findById(99).isEmpty());
        assertTrue(repo.deleteById(1));
        assertFalse(repo.deleteById(1));
        assertFalse(repo.deleteById(null));
        Transaction ghost = tx(1, "ghost");
        ghost.setId(77);
        assertThrows(EntityNotFoundException.class, () -> repo.save(ghost));
    }

    @Test
    void userCrudAndUniqueUsername() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        User a = repo.save(user("budi"));
        repo.save(user("sari"));
        assertThrows(ValidationException.class, () -> repo.save(user("BUDI")));
        assertEquals(a.getId(), repo.findByUsername(" Budi ").orElseThrow().getId());
        assertTrue(repo.findByUsername(null).isEmpty());
        assertTrue(repo.findByUsername(" ").isEmpty());
        assertTrue(repo.findByUsername("none").isEmpty());
        a.setName("Baru");
        repo.save(a);
        assertEquals("Baru", repo.findById(a.getId()).orElseThrow().getName());
        assertEquals(2, repo.findAll().size());
        assertTrue(repo.findById(null).isEmpty());
        assertTrue(repo.findById(0).isEmpty());
        assertTrue(repo.deleteById(a.getId()));
        assertFalse(repo.deleteById(a.getId()));
        assertFalse(repo.deleteById(-2));
        User ghost = user("hantu");
        ghost.setId(50);
        assertThrows(EntityNotFoundException.class, () -> repo.save(ghost));
    }
}
