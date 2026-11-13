package org.delcom.ifs25045springbootcashflow.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.*;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserUseCaseTest {
    private UserUseCase useCase;
    private User admin;
    private User budi;
    private final PasswordHasher hasher = new PasswordHasher();

    @BeforeEach
    void setUp() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthUseCase auth = new AuthUseCase(repo, hasher, Clock.systemDefaultZone());
        useCase = new UserUseCase(repo, hasher);
        admin = auth.ensureRootAdmin("root", "root-secret");
        budi = auth.register("budi", "Budi", "secret1");
    }

    @Test
    void listUsersIsAdminOnly() {
        List<User> all = useCase.listUsers(admin);
        assertEquals(List.of("root", "budi"), all.stream().map(User::getUsername).toList());
        assertThrows(ForbiddenException.class, () -> useCase.listUsers(budi));
        assertThrows(ForbiddenException.class, () -> useCase.listUsers(null));
    }

    @Test
    void getUserValidatesId() {
        assertEquals("budi", useCase.getUser(budi.getId()).getUsername());
        assertThrows(ValidationException.class, () -> useCase.getUser(null));
        assertThrows(ValidationException.class, () -> useCase.getUser(-1));
        assertThrows(EntityNotFoundException.class, () -> useCase.getUser(999));
    }

    @Test
    void updateProfileChangesName() {
        assertEquals("Budi S", useCase.updateProfile(budi.getId(), " Budi S ").getName());
        assertThrows(ValidationException.class, () -> useCase.updateProfile(budi.getId(), " "));
    }

    @Test
    void updatePasswordRequiresCurrentPassword() {
        useCase.updatePassword(budi.getId(), "secret1", "newsecret");
        assertTrue(hasher.matches("newsecret", useCase.getUser(budi.getId()).getPasswordHash()));
        assertThrows(ValidationException.class, () -> useCase.updatePassword(budi.getId(), "salah", "another1"));
        assertThrows(ValidationException.class, () -> useCase.updatePassword(budi.getId(), "newsecret", "123"));
    }

    @Test
    void updatePhotoStoresPath() {
        assertEquals("/uploads/a.png", useCase.updatePhoto(budi.getId(), "/uploads/a.png").getPhotoPath());
        assertThrows(ValidationException.class, () -> useCase.updatePhoto(budi.getId(), " "));
        assertThrows(ValidationException.class, () -> useCase.updatePhoto(budi.getId(), null));
    }
}
