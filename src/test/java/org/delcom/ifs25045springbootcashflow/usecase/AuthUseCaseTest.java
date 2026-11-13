package org.delcom.ifs25045springbootcashflow.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthUseCaseTest {
    private InMemoryUserRepository users;
    private PasswordHasher hasher;
    private AuthUseCase auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        hasher = new PasswordHasher();
        auth = new AuthUseCase(users, hasher, Clock.systemDefaultZone());
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        User u = auth.register("  Budi.01 ", " Budi ", "rahasia1");
        assertNotNull(u.getId());
        assertEquals("budi.01", u.getUsername());
        assertEquals("Budi", u.getName());
        assertEquals(Role.USER, u.getRole());
        assertEquals(hasher.hash("rahasia1"), u.getPasswordHash());
        assertNotEquals("rahasia1", u.getPasswordHash());
    }

    @Test
    void registerValidatesInput() {
        assertThrows(ValidationException.class, () -> auth.register(null, "A", "secret1"));
        assertThrows(ValidationException.class, () -> auth.register(" ", "A", "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("a", "A", "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("bad name", "A", "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("budi", "", "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("budi", null, "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("budi", "x".repeat(101), "secret1"));
        assertThrows(ValidationException.class, () -> auth.register("budi", "Budi", null));
        assertThrows(ValidationException.class, () -> auth.register("budi", "Budi", ""));
        assertThrows(ValidationException.class, () -> auth.register("budi", "Budi", "12345"));
        assertThrows(ValidationException.class, () -> auth.register("budi", "Budi", "x".repeat(101)));
    }

    @Test
    void registerRejectsDuplicateUsernameCaseInsensitive() {
        auth.register("budi", "Budi", "secret1");
        assertThrows(ValidationException.class, () -> auth.register("BUDI", "Lain", "secret2"));
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User created = auth.register("budi", "Budi", "secret1");
        assertEquals(created.getId(), auth.login("Budi", "secret1").getId());
    }

    @Test
    void loginFailsWithWrongCredentials() {
        auth.register("budi", "Budi", "secret1");
        assertThrows(AuthenticationException.class, () -> auth.login("budi", "salah-salah"));
        assertThrows(AuthenticationException.class, () -> auth.login("tidakada", "secret1"));
        assertThrows(ValidationException.class, () -> auth.login("", "secret1"));
        assertThrows(ValidationException.class, () -> auth.login(null, "secret1"));
        assertThrows(ValidationException.class, () -> auth.login("budi", ""));
        assertThrows(ValidationException.class, () -> auth.login("budi", null));
    }

    @Test
    void findUserHandlesInvalidIds() {
        User u = auth.register("budi", "Budi", "secret1");
        assertTrue(auth.findUser(u.getId()).isPresent());
        assertTrue(auth.findUser(null).isEmpty());
        assertTrue(auth.findUser(0).isEmpty());
        assertTrue(auth.findUser(999).isEmpty());
    }

    @Test
    void ensureRootAdminIsIdempotent() {
        User root = auth.ensureRootAdmin("root", "root-secret");
        assertEquals(Role.ADMIN, root.getRole());
        assertEquals(root.getId(), auth.ensureRootAdmin("root", "root-secret").getId());
        assertEquals(1, users.findAll().size());
        assertEquals(root.getId(), auth.login("root", "root-secret").getId());
    }

    @Test
    void ensureRootAdminRejectsExistingNonAdminAndWeakPassword() {
        auth.register("boss", "Boss", "secret1");
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin("boss", "secret1"));
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin("root", ""));
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin("", "secret1"));
    }
}
