package org.delcom.ifs25045springbootcashflow.adapter.presenter;

import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Format teks/uang untuk tampilan. Entity tidak tahu cara memformat dirinya sendiri. */
public class FinancePresenter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Contoh: 1000000 menjadi "Rp 1,000,000.00". */
    public String formatMoney(long amount) {
        return String.format(Locale.US, "Rp %,.2f", BigDecimal.valueOf(amount));
    }

    public String typeLabel(TransactionType type) {
        return type == TransactionType.INCOME ? "Pemasukan" : "Pengeluaran";
    }

    public String roleLabel(Role role) {
        return role == Role.ADMIN ? "Admin" : "Pengguna";
    }

    public String sortLabel(SortOption option) {
        return switch (option) {
            case NEWEST -> "Terbaru";
            case OLDEST -> "Terlama";
            case AMOUNT_ASC -> "Jumlah terkecil";
            case AMOUNT_DESC -> "Jumlah terbesar";
            case INCOME_FIRST -> "Pemasukan dulu";
            case EXPENSE_FIRST -> "Pengeluaran dulu";
        };
    }

    /** Format baris: [id] keterangan | Rp 1,000,000.00 | [Pemasukan]. */
    public String formatRow(Transaction t) {
        return "[" + t.getId() + "] " + t.getDescription() + " | " + formatMoney(t.getAmount())
                + " | [" + typeLabel(t.getType()) + "]";
    }

    public String formatBalance(long balance) {
        return "Saldo: " + formatMoney(balance);
    }

    public String formatDateTime(LocalDateTime time) {
        return time == null ? "-" : TIME.format(time);
    }

    public String addedMessage(Transaction t) {
        return typeLabel(t.getType()) + " ditambahkan: " + formatMoney(t.getAmount());
    }

    public String updatedMessage(Transaction t) {
        return "Transaksi [" + t.getId() + "] diperbarui";
    }

    public String deletedMessage(Transaction t) {
        return "Transaksi [" + t.getId() + "] dihapus";
    }

    public String failureMessage(Exception e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "Terjadi kesalahan" : message;
    }
}
