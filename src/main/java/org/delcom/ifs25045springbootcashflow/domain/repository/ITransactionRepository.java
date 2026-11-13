package org.delcom.ifs25045springbootcashflow.domain.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import java.util.List;

public interface ITransactionRepository extends IRepository<Transaction, Integer> {
    List<Transaction> findByUserId(Integer userId);
}
