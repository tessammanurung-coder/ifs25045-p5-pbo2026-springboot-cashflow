package org.delcom.ifs25045springbootcashflow.adapter.presenter;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.domain.entity.*;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FinancePresenterTest {
    private final FinancePresenter presenter = new FinancePresenter();
    private static final LocalDateTime T = LocalDateTime.of(2026, 3, 9, 7, 5);

    @Test
    void formatsMoneyWithThousandsSeparatorAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1_000_000));
        assertEquals("Rp 25,000.00", presenter.formatMoney(25_000));
        assertEquals("Rp 0.00", presenter.formatMoney(0));
        assertEquals("Rp -4,975,000.00", presenter.formatMoney(-4_975_000));
        assertEquals("Rp 9,007,199,254,740,993.00", presenter.formatMoney(9_007_199_254_740_993L));
    }

    @Test
    void formatsRowAndBalanceLikeTheModule() {
        Transaction t = new Transaction(3, 1, "Gaji", 1_000_000, TransactionType.INCOME, null, T);
        assertEquals("[3] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.formatRow(t));
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4_975_000));
    }

    @Test
    void labelsAndMessages() {
        Transaction in = new Transaction(1, 1, "Gaji", 100, TransactionType.INCOME, null, T);
        Transaction out = new Transaction(2, 1, "Kopi", 50, TransactionType.EXPENSE, null, T);
        assertEquals("Pemasukan", presenter.typeLabel(TransactionType.INCOME));
        assertEquals("Pengeluaran", presenter.typeLabel(TransactionType.EXPENSE));
        assertEquals("Admin", presenter.roleLabel(Role.ADMIN));
        assertEquals("Pengguna", presenter.roleLabel(Role.USER));
        assertEquals("Pemasukan ditambahkan: Rp 100.00", presenter.addedMessage(in));
        assertEquals("Pengeluaran ditambahkan: Rp 50.00", presenter.addedMessage(out));
        assertEquals("Transaksi [2] diperbarui", presenter.updatedMessage(out));
        assertEquals("Transaksi [2] dihapus", presenter.deletedMessage(out));
        assertEquals("09/03/2026 07:05", presenter.formatDateTime(T));
        assertEquals("-", presenter.formatDateTime(null));
    }

    @Test
    void everySortOptionHasLabel() {
        for (SortOption option : SortOption.values()) {
            assertFalse(presenter.sortLabel(option).isBlank(), option.name());
        }
    }

    @Test
    void failureMessageFallsBack() {
        assertEquals("Jumlah salah", presenter.failureMessage(new ValidationException("Jumlah salah")));
        assertEquals("Terjadi kesalahan", presenter.failureMessage(new RuntimeException()));
        assertEquals("Terjadi kesalahan", presenter.failureMessage(new RuntimeException(" ")));
    }
}
