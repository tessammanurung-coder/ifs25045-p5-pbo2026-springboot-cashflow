package org.delcom.ifs25045springbootcashflow.adapter.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.repository.IUserRepository;
import java.util.Optional;

/** Basis bersama implementasi IUserRepository. */
public abstract class UserRepository implements IUserRepository {

    protected static boolean isValidId(Integer id) {
        return id != null && id > 0;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }
}
