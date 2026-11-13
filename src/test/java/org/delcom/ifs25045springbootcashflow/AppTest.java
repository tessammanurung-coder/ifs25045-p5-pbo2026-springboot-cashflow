package org.delcom.ifs25045springbootcashflow;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25045springbootcashflow.domain.entity.Role;
import org.delcom.ifs25045springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25045springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: seluruh konteks Spring (H2 lewat profil "test") naik dan wiring Clean Architecture utuh. */
@SpringBootTest
@ActiveProfiles("test")
class AppTest {
    @Autowired
    private AuthUseCase auth;
    @Autowired
    private FinanceUseCase finance;
    @Autowired
    private FileStorage storage;

    @Test
    void contextLoadsAndRootAdminIsSeeded() {
        assertEquals(Role.ADMIN, auth.login("root", "root-secret-1").getRole());
        assertTrue(storage.getRoot().toString().endsWith("test-uploads"));
    }

    @Test
    void endToEndAgainstH2() {
        var user = auth.register("apptest", "App Test", "secret123");
        finance.addIncome(user.getId(), "Gaji", "1000000", null);
        finance.addExpense(user.getId(), "Makan", "25000", null);
        assertEquals(975_000, finance.getBalance(user.getId()));
    }
}
