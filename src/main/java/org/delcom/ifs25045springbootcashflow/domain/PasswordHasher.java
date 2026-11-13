package org.delcom.ifs25045springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hash password dengan SHA-256 (heksadesimal 64 karakter), sesuai ketentuan modul. */
public class PasswordHasher {
    private static final String ALGORITHM = "SHA-256";

    private final String algorithm;

    public PasswordHasher() {
        this(ALGORITHM);
    }

    /** Hanya untuk pengujian jalur kegagalan algoritma. */
    PasswordHasher(String algorithm) {
        this.algorithm = algorithm;
    }

    public String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password tidak boleh null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return HexFormat.of().formatHex(digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " tidak tersedia pada JVM ini", e);
        }
    }

    public boolean matches(String rawPassword, String expectedHash) {
        if (rawPassword == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(rawPassword).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
