package org.delcom.ifs25045springbootcashflow.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.domain.entity.*;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 10, 0);

    private static Transaction tx(Integer id, long amount, TransactionType type, int minute) {
        return new Transaction(id, 1, "item " + id, amount, type, null, T0.plusMinutes(minute));
    }

    @Test
    void transactionKeepsValidValues() {
        Transaction t = new Transaction(null, 7, "  Gaji  ", 5_000, TransactionType.INCOME, " ", T0);
        assertNull(t.getId());
        assertEquals(7, t.getUserId());
        assertEquals("Gaji", t.getDescription());
        assertEquals(5_000, t.getAmount());
        assertNull(t.getProofPath(), "proof kosong dinormalkan menjadi null");
        assertEquals(T0, t.getCreatedAt());
        t.setId(3);
        assertEquals(3, t.getId());
    }

    @Test
    void transactionRejectsInvalidValues() {
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, "", 1, TransactionType.INCOME, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, null, 1, TransactionType.INCOME, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, "x", 0, TransactionType.INCOME, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, "x", -5, TransactionType.EXPENSE, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, "x", 1, null, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, null, "x", 1, TransactionType.INCOME, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 0, "x", 1, TransactionType.INCOME, null, T0));
        assertThrows(ValidationException.class, () -> new Transaction(null, 1, "x", 1, TransactionType.INCOME, null, null));
    }

    @Test
    void updateChangesContentButNotOwner() {
        Transaction t = tx(1, 100, TransactionType.INCOME, 0);
        t.update("baru", 250, TransactionType.EXPENSE, "/uploads/a.png");
        assertEquals("baru", t.getDescription());
        assertEquals(250, t.getAmount());
        assertEquals(TransactionType.EXPENSE, t.getType());
        assertEquals("/uploads/a.png", t.getProofPath());
        assertEquals(1, t.getUserId());
        assertThrows(ValidationException.class, () -> t.update(" ", 1, TransactionType.INCOME, null));
    }

    @Test
    void signedAmountDependsOnType() {
        assertEquals(100, tx(1, 100, TransactionType.INCOME, 0).signedAmount());
        assertEquals(-100, tx(2, 100, TransactionType.EXPENSE, 0).signedAmount());
    }

    @Test
    void userExposesFieldsAndRole() {
        User admin = new User(null, "root", "Root", "h", Role.ADMIN, null, T0);
        admin.setId(1);
        admin.setName("Admin");
        admin.setPasswordHash("h2");
        admin.setPhotoPath("/uploads/x.png");
        assertEquals(1, admin.getId());
        assertEquals("root", admin.getUsername());
        assertEquals("Admin", admin.getName());
        assertEquals("h2", admin.getPasswordHash());
        assertEquals("/uploads/x.png", admin.getPhotoPath());
        assertEquals(T0, admin.getCreatedAt());
        assertTrue(admin.isAdmin());
        assertFalse(new User(2, "u", "U", "h", Role.USER, null, T0).isAdmin());
        assertEquals(2, TransactionType.values().length);
        assertEquals(Role.USER, Role.valueOf("USER"));
    }

    @Test
    void sortOptionsOrderTransactions() {
        Transaction a = tx(1, 300, TransactionType.INCOME, 0);
        Transaction b = tx(2, 100, TransactionType.EXPENSE, 5);
        Transaction c = tx(3, 200, TransactionType.INCOME, 10);
        Transaction d = tx(4, 50, TransactionType.EXPENSE, 10);
        assertTrue(SortOption.values().length >= 4);
        assertEquals(List.of(4, 3, 2, 1), ids(SortOption.NEWEST, a, b, c, d));
        assertEquals(List.of(1, 2, 3, 4), ids(SortOption.OLDEST, a, b, c, d));
        assertEquals(List.of(4, 2, 3, 1), ids(SortOption.AMOUNT_ASC, a, b, c, d));
        assertEquals(List.of(1, 3, 2, 4), ids(SortOption.AMOUNT_DESC, a, b, c, d));
        assertEquals(List.of(3, 1, 4, 2), ids(SortOption.INCOME_FIRST, a, b, c, d));
        assertEquals(List.of(4, 2, 3, 1), ids(SortOption.EXPENSE_FIRST, a, b, c, d));
    }

    @Test
    void newestAndOldestHandleSameTimeAndNullIds() {
        Transaction x = new Transaction(null, 1, "x", 1, TransactionType.INCOME, null, T0);
        Transaction y = new Transaction(5, 1, "y", 1, TransactionType.INCOME, null, T0);
        List<Transaction> list = new ArrayList<>(List.of(y, x));
        list.sort(SortOption.OLDEST.comparator());
        assertSame(x, list.get(0));
        list.sort(SortOption.NEWEST.comparator());
        assertSame(y, list.get(0));
    }

    private static List<Integer> ids(SortOption option, Transaction... items) {
        List<Transaction> list = new ArrayList<>(List.of(items));
        list.sort(option.comparator());
        return list.stream().map(Transaction::getId).toList();
    }

    @Test
    void passwordHasherProducesSha256Hex() {
        PasswordHasher hasher = new PasswordHasher();
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hasher.hash("abc"));
        assertTrue(hasher.matches("abc", hasher.hash("abc")));
        assertFalse(hasher.matches("abd", hasher.hash("abc")));
        assertFalse(hasher.matches(null, "x"));
        assertFalse(hasher.matches("abc", null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }

    @Test
    void passwordHasherFailsClearlyForUnknownAlgorithm() {
        PasswordHasher broken = new PasswordHasher("NOPE-256");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> broken.hash("abc"));
        assertTrue(e.getMessage().contains("NOPE-256"));
    }

    @Test
    void exceptionHierarchyIsStructured() {
        assertInstanceOf(DomainException.class, new ValidationException("v"));
        assertInstanceOf(DomainException.class, new AuthenticationException("a"));
        assertInstanceOf(DomainException.class, new ForbiddenException("f"));
        EntityNotFoundException nf = new EntityNotFoundException("Transaksi", 9);
        assertInstanceOf(DomainException.class, nf);
        assertEquals("Transaksi dengan ID 9 tidak ditemukan", nf.getMessage());
        Throwable cause = new RuntimeException("x");
        assertSame(cause, new RepositoryException("r", cause).getCause());
        assertSame(cause, new DomainException("d", cause).getCause());
    }
}
