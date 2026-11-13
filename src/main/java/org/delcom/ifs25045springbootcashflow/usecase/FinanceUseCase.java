package org.delcom.ifs25045springbootcashflow.usecase;

import org.delcom.ifs25045springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ForbiddenException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25045springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25045springbootcashflow.domain.repository.IUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Seluruh aturan bisnis cashflow; setiap operasi dibatasi pada pemilik transaksi. */
public class FinanceUseCase {
    private static final int MAX_AMOUNT_DIGITS = 15;

    private final ITransactionRepository transactions;
    private final IUserRepository users;
    private final Clock clock;

    public FinanceUseCase(ITransactionRepository transactions, IUserRepository users, Clock clock) {
        this.transactions = transactions;
        this.users = users;
        this.clock = clock;
    }

    // ---- Tambah ----

    public Transaction addIncome(Integer userId, String description, String amount, String proofPath) {
        return add(userId, description, parseAmount(amount), TransactionType.INCOME, proofPath);
    }

    public Transaction addExpense(Integer userId, String description, String amount, String proofPath) {
        return add(userId, description, parseAmount(amount), TransactionType.EXPENSE, proofPath);
    }

    public Transaction addIncome(Integer userId, String description, long amount, String proofPath) {
        return add(userId, description, amount, TransactionType.INCOME, proofPath);
    }

    public Transaction addExpense(Integer userId, String description, long amount, String proofPath) {
        return add(userId, description, amount, TransactionType.EXPENSE, proofPath);
    }

    private Transaction add(Integer userId, String description, long amount,
                            TransactionType type, String proofPath) {
        requireUser(userId);
        Transaction transaction = new Transaction(null, userId, description, amount, type,
                proofPath, LocalDateTime.now(clock));
        return transactions.save(transaction);
    }

    // ---- Baca / cari / urut ----

    public Transaction getTransaction(Integer userId, Integer id) {
        return requireOwned(userId, id);
    }

    /** Semua transaksi milik pengguna, terbaru lebih dulu. */
    public List<Transaction> getTransactions(Integer userId) {
        return search(userId, null, SortOption.NEWEST);
    }

    public List<Transaction> search(Integer userId, String keyword, SortOption sort) {
        requireUser(userId);
        return filterAndSort(transactions.findByUserId(userId), keyword, sort);
    }

    /** Peninjauan read-only transaksi pengguna lain; khusus ADMIN. */
    public List<Transaction> getTransactionsForAdmin(User actor, Integer targetUserId,
                                                     String keyword, SortOption sort) {
        if (actor == null || !actor.isAdmin()) {
            throw new ForbiddenException("Hanya admin yang dapat melihat transaksi pengguna lain");
        }
        requireUser(targetUserId);
        return filterAndSort(transactions.findByUserId(targetUserId), keyword, sort);
    }

    private List<Transaction> filterAndSort(List<Transaction> source, String keyword, SortOption sort) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        SortOption option = sort == null ? SortOption.NEWEST : sort;
        return source.stream()
                .filter(t -> needle.isEmpty()
                        || t.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(option.comparator())
                .toList();
    }

    // ---- Ubah / hapus ----

    public Transaction update(Integer userId, Integer id, String description, String amount,
                              TransactionType type, String proofPath) {
        Transaction transaction = requireOwned(userId, id);
        transaction.update(description, parseAmount(amount), type, proofPath);
        return transactions.save(transaction);
    }

    /** Mengembalikan transaksi yang dihapus agar pemanggil dapat membersihkan file buktinya. */
    public Transaction delete(Integer userId, Integer id) {
        Transaction transaction = requireOwned(userId, id);
        transactions.deleteById(id);
        return transaction;
    }

    // ---- Ringkasan ----

    /** Ringkasan untuk kartu saldo: jumlah transaksi, total pemasukan, total pengeluaran, dan saldo. */
    public record Summary(int count, long income, long expense, long balance) {}

    public Summary getSummary(Integer userId) {
        requireUser(userId);
        return summarize(transactions.findByUserId(userId));
    }

    /** Menghitung ringkasan dari daftar yang sudah diambil (mis. milik pengguna lain untuk admin). */
    public Summary summarize(List<Transaction> items) {
        long income = items.stream().filter(t -> t.getType() == TransactionType.INCOME)
                .mapToLong(Transaction::getAmount).sum();
        long expense = items.stream().filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToLong(Transaction::getAmount).sum();
        return new Summary(items.size(), income, expense, income - expense);
    }

    public long getTotalIncome(Integer userId) {
        return total(userId, TransactionType.INCOME);
    }

    public long getTotalExpense(Integer userId) {
        return total(userId, TransactionType.EXPENSE);
    }

    public long getBalance(Integer userId) {
        requireUser(userId);
        return transactions.findByUserId(userId).stream().mapToLong(Transaction::signedAmount).sum();
    }

    public int countTransactions(Integer userId) {
        requireUser(userId);
        return transactions.findByUserId(userId).size();
    }

    private long total(Integer userId, TransactionType type) {
        requireUser(userId);
        return transactions.findByUserId(userId).stream()
                .filter(t -> t.getType() == type)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    // ---- Validasi ----

    /** Jumlah harus bilangan bulat positif (digit saja). */
    public long parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Jumlah tidak boleh kosong");
        }
        String text = raw.trim();
        if (!text.matches("\\d+")) {
            throw new ValidationException("Jumlah harus berupa angka bulat positif");
        }
        if (text.length() > MAX_AMOUNT_DIGITS) {
            throw new ValidationException("Jumlah terlalu besar");
        }
        long value = Long.parseLong(text);
        if (value <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
        return value;
    }

    public Integer parseId(String raw) {
        if (raw == null || !raw.trim().matches("\\d{1,9}")) {
            throw new ValidationException("ID tidak valid");
        }
        return InputRules.requireValidId(Integer.valueOf(raw.trim()));
    }

    private void requireUser(Integer userId) {
        InputRules.requireValidId(userId);
        if (users.findById(userId).isEmpty()) {
            throw new EntityNotFoundException("Pengguna", userId);
        }
    }

    private Transaction requireOwned(Integer userId, Integer id) {
        requireUser(userId);
        InputRules.requireValidId(id);
        Transaction transaction = transactions.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaksi", id));
        if (!transaction.getUserId().equals(userId)) {
            throw new ForbiddenException("Transaksi ini bukan milik Anda");
        }
        return transaction;
    }
}
