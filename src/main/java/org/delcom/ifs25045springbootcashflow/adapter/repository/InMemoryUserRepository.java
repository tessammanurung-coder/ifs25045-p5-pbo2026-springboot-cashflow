package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repository berbasis memori untuk unit test; meniru constraint UNIQUE pada username. */
public class InMemoryUserRepository extends UserRepository {
    private final Map<Integer, User> store = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public User save(User user) {
        boolean duplicate = store.values().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername())
                        && !u.getId().equals(user.getId()));
        if (duplicate) {
            throw new ValidationException("Username sudah digunakan");
        }
        if (user.getId() == null) {
            user.setId(sequence.incrementAndGet());
        } else if (!store.containsKey(user.getId())) {
            throw new EntityNotFoundException("Pengguna", user.getId());
        }
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Integer id) {
        return isValidId(id) ? Optional.ofNullable(store.get(id)) : Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return store.values().stream().sorted(Comparator.comparing(User::getId)).toList();
    }

    @Override
    public boolean deleteById(Integer id) {
        return isValidId(id) && store.remove(id) != null;
    }
}
