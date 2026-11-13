package org.delcom.ifs25045springbootcashflow.domain.entity;

import java.util.Comparator;

/** Pilihan pengurutan transaksi; tiap opsi membawa Comparator-nya sendiri. */
public enum SortOption {
    NEWEST(Comparator.comparing(Transaction::getCreatedAt).reversed()
            .thenComparing(Comparator.comparing(Transaction::getId,
                    Comparator.nullsFirst(Comparator.<Integer>naturalOrder())).reversed())),
    OLDEST(Comparator.comparing(Transaction::getCreatedAt)
            .thenComparing(Transaction::getId, Comparator.nullsFirst(Comparator.<Integer>naturalOrder()))),
    AMOUNT_ASC(Comparator.comparingLong(Transaction::getAmount)),
    AMOUNT_DESC(Comparator.comparingLong(Transaction::getAmount).reversed()),
    INCOME_FIRST(Comparator.comparing((Transaction t) -> t.getType() != TransactionType.INCOME)
            .thenComparing(Comparator.comparing(Transaction::getCreatedAt).reversed())),
    EXPENSE_FIRST(Comparator.comparing((Transaction t) -> t.getType() != TransactionType.EXPENSE)
            .thenComparing(Comparator.comparing(Transaction::getCreatedAt).reversed()));

    private final Comparator<Transaction> comparator;

    SortOption(Comparator<Transaction> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Transaction> comparator() {
        return comparator;
    }
}
