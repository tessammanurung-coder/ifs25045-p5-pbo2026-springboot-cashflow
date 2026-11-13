package org.delcom.ifs25045springbootcashflow.config;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25045springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25045springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25045springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25045springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25045springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25045springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25045springbootcashflow.usecase.UserUseCase;
import java.nio.file.Path;
import java.time.Clock;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Composition Root: satu-satunya tempat yang merangkai port ke adapter dan use case. */
@Configuration
public class AppConfig implements WebMvcConfigurer {
    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    /** Dibuat sebelum repository agar tabel sudah ada saat repository dipakai. */
    @Bean
    public SchemaInitializer schemaInitializer(DataSource dataSource) {
        SchemaInitializer initializer = new SchemaInitializer(dataSource);
        initializer.initialize();
        return initializer;
    }

    @Bean
    public IUserRepository userRepository(DataSource dataSource, SchemaInitializer schema) {
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource, SchemaInitializer schema) {
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository users, PasswordHasher hasher, Clock clock) {
        return new AuthUseCase(users, hasher, clock);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactions, IUserRepository users,
                                         Clock clock) {
        return new FinanceUseCase(transactions, users, clock);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository users, PasswordHasher hasher) {
        return new UserUseCase(users, hasher);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage() {
        return new FileStorage(Path.of(uploadDir));
    }

    /** Seed admin dari ROOT_USERNAME / ROOT_PASSWORD; dilewati bila password tidak diisi. */
    @Bean
    public ApplicationRunner rootAdminSeeder(AuthUseCase auth,
                                             @Value("${app.root.username:root}") String username,
                                             @Value("${app.root.password:}") String password) {
        return args -> {
            if (password == null || password.isBlank()) {
                LOG.warn("ROOT_PASSWORD kosong: admin tidak di-seed.");
                return;
            }
            auth.ensureRootAdmin(username, password);
            LOG.info("Admin '{}' siap digunakan.", username);
        };
    }

    /** Melayani gambar bukti dan foto profil melalui /uploads/**. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(FileStorage.URL_PREFIX + "**").addResourceLocations(location);
    }
}
