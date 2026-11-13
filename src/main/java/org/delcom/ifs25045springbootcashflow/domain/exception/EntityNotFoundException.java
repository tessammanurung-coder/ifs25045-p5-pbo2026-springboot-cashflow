package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Entity dengan ID tertentu tidak ditemukan. */
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entityName, Object id) {
        super(entityName + " dengan ID " + id + " tidak ditemukan");
    }
}
