package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Kegagalan infrastruktur penyimpanan (mis. SQLException) dibungkus menjadi exception domain. */
public class RepositoryException extends DomainException {
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
