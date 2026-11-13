package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Kredensial login tidak valid. */
public class AuthenticationException extends DomainException {
    public AuthenticationException(String message) {
        super(message);
    }
}
