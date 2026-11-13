package org.delcom.ifs25045springbootcashflow.domain.repository;

import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import java.util.Optional;

public interface IUserRepository extends IRepository<User, Integer> {
    /** Pencarian username tidak peka huruf besar/kecil. */
    Optional<User> findByUsername(String username);
}
