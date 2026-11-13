package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repository berbasis memori untuk unit test; data hilang saat proses berhenti. */
public class InMemoryTransactionRepository extends TransactionRepository {
    private final Map<Integer, Transaction> store = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(sequence.incrementAndGet());
        } else if (!store.containsKey(transaction.getId())) {
            throw new EntityNotFoundException("Transaksi", transaction.getId());
        }
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        return isValidId(id) ? Optional.ofNullable(store.get(id)) : Optional.empty();
    }

    @Override
    public List<Transaction> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Transaction::getId)).toList();
    }

    @Override
    public boolean deleteById(Integer id) {
        return isValidId(id) && store.remove(id) != null;
    }
}
