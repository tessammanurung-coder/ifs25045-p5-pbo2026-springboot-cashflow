package org.delcom.ifs25045springbootcashflow.domain.repository;

import java.util.List;
import java.util.Optional;

/** Port repository generik. save() = insert bila id null, selain itu update. */
public interface IRepository<T, ID> {
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean deleteById(ID id);
}
