package org.delcom.ifs25045springbootcashflow.usecase;

import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;

/** Aturan validasi input yang dipakai bersama oleh beberapa use case. */
final class InputRules {
    static final int MIN_PASSWORD_LENGTH = 6;
    static final int MAX_PASSWORD_LENGTH = 100;
    static final int MAX_NAME_LENGTH = 100;

    private InputRules() {}

    static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Nama tidak boleh kosong");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Nama maksimal " + MAX_NAME_LENGTH + " karakter");
        }
        return trimmed;
    }

    static String requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password tidak boleh kosong");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password harus " + MIN_PASSWORD_LENGTH + "-"
                    + MAX_PASSWORD_LENGTH + " karakter");
        }
        return password;
    }

    static Integer requireValidId(Integer id) {
        if (id == null || id <= 0) {
            throw new ValidationException("ID tidak valid");
        }
        return id;
    }
}
