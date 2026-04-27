package com.akash.pooler_backend.config;

import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.repository.PbUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


/**
 * Data seeder — runs on application startup in dev and staging profiles only.
 *
 * Seeds:
 *  - 1 Super Admin
 *  - 1 Admin
 *  - 1 Moderator
 *  - 2 Regular users
 *
 * Credentials are logged to console on startup (dev only).
 * NEVER runs in prod (profile guard).
 */
@Slf4j
@Component
@Order(1)
@Profile({"dev", "staging"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {


    private final PbUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("DataSeeder: DB already seeded, skipping.");
            return;
        }

        log.info("DataSeeder: Seeding initial data...");

        seed("Super",  "Admin",  "superadmin@pooler.com",  "Admin@123!",  Role.ROLE_SUPER_ADMIN);
        seed("System", "Admin",  "admin@pooler.com",        "Admin@123!",  Role.ROLE_ADMIN);
        seed("John",   "Mod",    "moderator@pooler.com",    "Mod@1234!",   Role.ROLE_MODERATOR);
        seed("Alice",  "User",   "alice@pooler.com",        "User@1234!",  Role.ROLE_USER);
        seed("Bob",    "User",   "bob@pooler.com",          "User@1234!",  Role.ROLE_USER);

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  DataSeeder — Seeded Credentials (DEV / STAGING only)    ║");
        log.info("║  superadmin@pooler.com  /  Admin@123!                    ║");
        log.info("║  admin@pooler.com       /  Admin@123!                    ║");
        log.info("║  moderator@pooler.com   /  Mod@1234!                     ║");
        log.info("║  alice@pooler.com       /  User@1234!                    ║");
        log.info("║  bob@pooler.com         /  User@1234!                    ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    private void seed(String first, String last, String email, String password, Role role) {
        PbUserEntity user = PbUserEntity.builder()
                .firstName(first)
                .lastName(last)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        userRepo.save(user);
        log.debug("Seeded user: {} [{}]", email, role);
    }
}
