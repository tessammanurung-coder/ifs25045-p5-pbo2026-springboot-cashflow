package org.delcom.ifs25045springbootcashflow.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.adapter.repository.InMemoryTransactionRepository;
import org.delcom.ifs25045springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.entity.*;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinanceUseCaseTest {
    private FinanceUseCase finance;
    private User admin;
    private User budi;
    private User sari;

    @BeforeEach
    void setUp() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T03:00:00Z"), ZoneOffset.UTC);
        AuthUseCase auth = new AuthUseCase(users, new PasswordHasher(), clock);
        finance = new FinanceUseCase(new InMemoryTransactionRepository(), users, clock);
        admin = auth.ensureRootAdmin("root", "root-secret");
        budi = auth.register("budi", "Budi", "secret1");
        sari = auth.register("sari", "Sari", "secret1");
    }

    @Test
    void addIncomeAndExpenseUpdateBalance() {
        Transaction in = finance.addIncome(budi.getId(), "Gaji", "5000000", "/uploads/a.png");
        finance.addExpense(budi.getId(), "Makan", "25000", null);
        assertNotNull(in.getId());
        assertEquals(TransactionType.INCOME, in.getType());
        assertEquals("/uploads/a.png", in.getProofPath());
        assertEquals(5_000_000, finance.getTotalIncome(budi.getId()));
        assertEquals(25_000, finance.getTotalExpense(budi.getId()));
        assertEquals(4_975_000, finance.getBalance(budi.getId()));
        assertEquals(2, finance.countTransactions(budi.getId()));
    }

    @Test
    void summaryCombinesCountsAndTotals() {
        finance.addIncome(budi.getId(), "Gaji", "5000000", null);
        finance.addExpense(budi.getId(), "Makan", "25000", null);
        FinanceUseCase.Summary s = finance.getSummary(budi.getId());
        assertEquals(new FinanceUseCase.Summary(2, 5_000_000, 25_000, 4_975_000), s);
        assertEquals(s, finance.summarize(finance.getTransactionsForAdmin(admin, budi.getId(), null, null)));
        assertEquals(new FinanceUseCase.Summary(0, 0, 0, 0), finance.getSummary(sari.getId()));
        assertThrows(EntityNotFoundException.class, () -> finance.getSummary(999));
    }

    @Test
    void numericOverloadsAlsoWork() {
        finance.addIncome(budi.getId(), "Bonus", 1_000L, null);
        finance.addExpense(budi.getId(), "Parkir", 400L, null);
        assertEquals(600, finance.getBalance(budi.getId()));
    }

    @Test
    void balanceIsZeroWithoutTransactions() {
        assertEquals(0, finance.getBalance(budi.getId()));
        assertEquals(0, finance.getTotalIncome(budi.getId()));
        assertEquals(0, finance.getTotalExpense(budi.getId()));
        assertTrue(finance.getTransactions(budi.getId()).isEmpty());
    }

    @Test
    void addValidatesDescriptionAndAmount() {
        Integer id = budi.getId();
        assertThrows(ValidationException.class, () -> finance.addIncome(id, "", "100", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(id, "  ", "100", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "0", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "-5", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "abc", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "1.5", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "", null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", null, null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "1".repeat(16), null));
        assertThrows(ValidationException.class, () -> finance.addExpense(id, "x", "000", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(id, "x", -1L, null));
        assertEquals(0, finance.countTransactions(id));
    }

    @Test
    void addRequiresExistingUser() {
        assertThrows(EntityNotFoundException.class, () -> finance.addIncome(999, "x", "1", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(null, "x", "1", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(0, "x", "1", null));
    }

    @Test
    void searchIsCaseInsensitiveAndScopedToUser() {
        finance.addExpense(budi.getId(), "Makan Siang", "30000", null);
        finance.addExpense(budi.getId(), "Bensin", "50000", null);
        finance.addExpense(sari.getId(), "Makan malam", "70000", null);
        List<Transaction> found = finance.search(budi.getId(), "  makan ", SortOption.NEWEST);
        assertEquals(1, found.size());
        assertEquals("Makan Siang", found.get(0).getDescription());
        assertEquals(2, finance.search(budi.getId(), null, null).size());
        assertEquals(2, finance.search(budi.getId(), "  ", SortOption.OLDEST).size());
        assertTrue(finance.search(budi.getId(), "zzz", SortOption.NEWEST).isEmpty());
    }

    @Test
    void sortingUsesSortOption() {
        finance.addExpense(budi.getId(), "kecil", "10", null);
        finance.addIncome(budi.getId(), "besar", "900", null);
        finance.addExpense(budi.getId(), "sedang", "500", null);
        assertEquals(List.of("besar", "sedang", "kecil"),
                names(finance.search(budi.getId(), "", SortOption.AMOUNT_DESC)));
        assertEquals(List.of("kecil", "sedang", "besar"),
                names(finance.search(budi.getId(), "", SortOption.AMOUNT_ASC)));
        assertEquals("besar", finance.search(budi.getId(), "", SortOption.INCOME_FIRST).get(0).getDescription());
        assertEquals(TransactionType.EXPENSE,
                finance.search(budi.getId(), "", SortOption.EXPENSE_FIRST).get(0).getType());
    }

    private static List<String> names(List<Transaction> list) {
        return list.stream().map(Transaction::getDescription).toList();
    }

    @Test
    void deleteRemovesOwnTransactionAndReturnsIt() {
        Transaction t = finance.addIncome(budi.getId(), "Gaji", "100", "/uploads/p.png");
        Transaction removed = finance.delete(budi.getId(), t.getId());
        assertEquals("/uploads/p.png", removed.getProofPath());
        assertEquals(0, finance.countTransactions(budi.getId()));
        assertThrows(EntityNotFoundException.class, () -> finance.delete(budi.getId(), t.getId()));
    }

    @Test
    void ownershipIsEnforced() {
        Transaction t = finance.addIncome(budi.getId(), "Gaji", "100", null);
        assertThrows(ForbiddenException.class, () -> finance.delete(sari.getId(), t.getId()));
        assertThrows(ForbiddenException.class, () -> finance.getTransaction(sari.getId(), t.getId()));
        assertThrows(ForbiddenException.class, () -> finance.delete(admin.getId(), t.getId()));
        assertThrows(ForbiddenException.class,
                () -> finance.update(sari.getId(), t.getId(), "x", "5", TransactionType.INCOME, null));
        assertEquals(1, finance.countTransactions(budi.getId()));
    }

    @Test
    void getTransactionReturnsOwnedTransactionOnly() {
        Transaction t = finance.addExpense(budi.getId(), "Kopi", "10", null);
        assertEquals("Kopi", finance.getTransaction(budi.getId(), t.getId()).getDescription());
        assertThrows(EntityNotFoundException.class, () -> finance.getTransaction(budi.getId(), 4242));
    }

    @Test
    void invalidIdsAreRejected() {
        assertThrows(ValidationException.class, () -> finance.delete(budi.getId(), null));
        assertThrows(ValidationException.class, () -> finance.delete(budi.getId(), 0));
        assertThrows(EntityNotFoundException.class, () -> finance.delete(budi.getId(), 12345));
        assertEquals(7, finance.parseId(" 7 "));
        assertThrows(ValidationException.class, () -> finance.parseId("abc"));
        assertThrows(ValidationException.class, () -> finance.parseId(null));
        assertThrows(ValidationException.class, () -> finance.parseId("0"));
        assertThrows(ValidationException.class, () -> finance.parseId("-3"));
    }

    @Test
    void updateChangesFieldsAndKeepsOwner() {
        Transaction t = finance.addIncome(budi.getId(), "Gaji", "100", null);
        Transaction u = finance.update(budi.getId(), t.getId(), "Gaji Bulanan", "250",
                TransactionType.EXPENSE, "/uploads/b.png");
        assertEquals("Gaji Bulanan", u.getDescription());
        assertEquals(250, u.getAmount());
        assertEquals(TransactionType.EXPENSE, u.getType());
        assertEquals(-250, finance.getBalance(budi.getId()));
        assertThrows(ValidationException.class,
                () -> finance.update(budi.getId(), t.getId(), "", "250", TransactionType.INCOME, null));
    }

    @Test
    void adminSeesOtherUsersTransactionsReadOnly() {
        finance.addIncome(budi.getId(), "Gaji", "100", null);
        finance.addExpense(budi.getId(), "Kopi", "10", null);
        assertEquals(2, finance.getTransactionsForAdmin(admin, budi.getId(), null, null).size());
        assertEquals(1, finance.getTransactionsForAdmin(admin, budi.getId(), "kopi", SortOption.NEWEST).size());
        assertThrows(ForbiddenException.class, () -> finance.getTransactionsForAdmin(sari, budi.getId(), null, null));
        assertThrows(ForbiddenException.class, () -> finance.getTransactionsForAdmin(null, budi.getId(), null, null));
        assertThrows(EntityNotFoundException.class, () -> finance.getTransactionsForAdmin(admin, 999, null, null));
        assertTrue(finance.getTransactions(admin.getId()).isEmpty(), "admin punya cashflow sendiri");
    }
}
