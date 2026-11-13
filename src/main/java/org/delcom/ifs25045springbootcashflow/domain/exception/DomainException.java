package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Akar hierarki exception domain; semua kesalahan bisnis turunan dari kelas ini. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
