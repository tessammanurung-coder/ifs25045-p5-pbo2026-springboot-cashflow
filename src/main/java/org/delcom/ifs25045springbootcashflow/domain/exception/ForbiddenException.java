package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Pengguna tidak berhak melakukan operasi pada data tersebut. */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(message);
    }
}
