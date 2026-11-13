package org.delcom.ifs25045springbootcashflow.domain.entity;

import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.time.LocalDateTime;

/** Satu catatan keuangan milik satu pengguna. Invarian dijaga di konstruktor dan {@link #update}. */
public class Transaction {
    private Integer id;
    private final Integer userId;
    private String description;
    private long amount;
    private TransactionType type;
    private String proofPath;
    private final LocalDateTime createdAt;

    public Transaction(Integer id, Integer userId, String description, long amount,
                       TransactionType type, String proofPath, LocalDateTime createdAt) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("ID pengguna tidak valid");
        }
        if (createdAt == null) {
            throw new ValidationException("Waktu transaksi wajib diisi");
        }
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        apply(description, amount, type, proofPath);
    }

    /** Mengubah isi transaksi; id, pemilik, dan waktu pembuatan tidak berubah. */
    public void update(String description, long amount, TransactionType type, String proofPath) {
        apply(description, amount, type, proofPath);
    }

    private void apply(String description, long amount, TransactionType type, String proofPath) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
        if (type == null) {
            throw new ValidationException("Tipe transaksi wajib diisi");
        }
        this.description = description.trim();
        this.amount = amount;
        this.type = type;
        this.proofPath = (proofPath == null || proofPath.isBlank()) ? null : proofPath;
    }

    /** Pemasukan bernilai positif, pengeluaran negatif; dipakai untuk menghitung saldo. */
    public long signedAmount() {
        return type == TransactionType.INCOME ? amount : -amount;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public String getDescription() { return description; }
    public long getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getProofPath() { return proofPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
