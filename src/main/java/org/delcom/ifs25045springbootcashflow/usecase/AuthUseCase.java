package org.delcom.ifs25045springbootcashflow.usecase;

import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.AuthenticationException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25045springbootcashflow.domain.repository.IUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/** Registrasi, login, dan seed admin. Logout bersifat stateless: sesi dikelola AuthSession. */
public class AuthUseCase {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,30}$");

    private final IUserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public AuthUseCase(IUserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    public User register(String username, String name, String password) {
        String normalized = normalizeUsername(username);
        String cleanName = InputRules.requireName(name);
        InputRules.requirePassword(password);
        return createUser(normalized, cleanName, password, Role.USER);
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Username dan password wajib diisi");
        }
        User user = users.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("Username atau password salah"));
        if (!hasher.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Username atau password salah");
        }
        return user;
    }

    /** Dipakai AuthSession untuk memulihkan pengguna dari ID di sesi/cookie. */
    public Optional<User> findUser(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return users.findById(id);
    }

    /** Membuat admin bila belum ada. Idempotent: pemanggilan berulang mengembalikan admin yang sama. */
    public User ensureRootAdmin(String username, String password) {
        String normalized = normalizeUsername(username);
        InputRules.requirePassword(password);
        Optional<User> existing = users.findByUsername(normalized);
        if (existing.isPresent()) {
            if (!existing.get().isAdmin()) {
                throw new ValidationException("Username root '" + normalized + "' sudah dipakai oleh pengguna biasa");
            }
            return existing.get();
        }
        return createUser(normalized, "Administrator", password, Role.ADMIN);
    }

    private User createUser(String username, String name, String password, Role role) {
        if (users.findByUsername(username).isPresent()) {
            throw new ValidationException("Username sudah digunakan");
        }
        User user = new User(null, username, name, hasher.hash(password), role, null,
                LocalDateTime.now(clock));
        return users.save(user);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username tidak boleh kosong");
        }
        String normalized = username.trim().toLowerCase();
        if (!USERNAME.matcher(normalized).matches()) {
            throw new ValidationException(
                    "Username 3-30 karakter: huruf, angka, titik, atau garis bawah");
        }
        return normalized;
    }
}
