package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.repository.ITransactionRepository;
import java.util.List;

/** Basis bersama implementasi ITransactionRepository: ID tidak valid dan filter per pemilik. */
public abstract class TransactionRepository implements ITransactionRepository {

    protected static boolean isValidId(Integer id) {
        return id != null && id > 0;
    }

    @Override
    public List<Transaction> findByUserId(Integer userId) {
        if (!isValidId(userId)) {
            return List.of();
        }
        return findAll().stream().filter(t -> userId.equals(t.getUserId())).toList();
    }
}
