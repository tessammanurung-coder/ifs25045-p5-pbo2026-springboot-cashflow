package org.delcom.ifs25045springbootcashflow.domain.exception;

/** Input atau state tidak memenuhi aturan validasi domain. */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
