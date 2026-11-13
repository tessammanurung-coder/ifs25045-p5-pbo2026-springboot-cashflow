package org.delcom.ifs25045springbootcashflow;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25045springbootcashflow.config.AppConfig;
import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.usecase.AuthUseCase;
import java.time.Clock;
import org.junit.jupiter.api.Test;

/** Menguji cabang seed admin yang tidak tercakup AppTest (password root kosong). */
class AppConfigTest {
    private final AppConfig config = new AppConfig();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final AuthUseCase auth = new AuthUseCase(users, new PasswordHasher(), Clock.systemDefaultZone());

    @Test
    void seederSkipsWhenPasswordIsBlankOrNull() throws Exception {
        config.rootAdminSeeder(auth, "root", "").run(null);
        config.rootAdminSeeder(auth, "root", null).run(null);
        config.rootAdminSeeder(auth, "root", "  ").run(null);
        assertTrue(users.findAll().isEmpty());
    }

    @Test
    void seederCreatesAdminWhenPasswordProvided() throws Exception {
        config.rootAdminSeeder(auth, "root", "root-secret").run(null);
        assertTrue(users.findByUsername("root").orElseThrow().isAdmin());
    }
}
