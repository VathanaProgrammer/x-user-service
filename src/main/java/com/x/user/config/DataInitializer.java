package com.x.user.config;

import com.x.user.model.*;
import com.x.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Local-only seed for a default admin user, roles, and permissions.
 * Active only with Spring profile {@code local-seed} (used by local K3s).
 *
 * <p>Default login (override password with {@code VYNTRA_SEED_ADMIN_PASSWORD}):
 * <ul>
 *   <li>username: {@code admin}</li>
 *   <li>password: {@code Admin@123456}</li>
 * </ul>
 */
@Configuration
@Profile("local-seed")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final long DEFAULT_BUSINESS_ID = 1L;
    private static final long DEFAULT_STORE_ID = 101L;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Value("${vyntra.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${vyntra.seed.admin-password:Admin@123456}")
    private String adminPassword;

    @Value("${vyntra.seed.admin-full-name:System Administrator}")
    private String adminFullName;

    @Value("${vyntra.seed.admin-email:admin@vyntra.local}")
    private String adminEmail;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) {
        if (!StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException(
                    "vyntra.seed.admin-password / VYNTRA_SEED_ADMIN_PASSWORD must not be empty when local-seed is active");
        }

        // 1. Permissions used by BFF (@PreAuthorize → PERMISSION_ + code)
        Permission viewProducts = ensurePermission("VIEW_PRODUCTS", "View Products", "PRODUCT");
        Permission createProduct = ensurePermission("PRODUCT_CREATE", "Create Product", "PRODUCT");
        Permission manageOrders = ensurePermission("MANAGE_ORDERS", "Manage Orders", "ORDER");
        Permission manageStock = ensurePermission("MANAGE_STOCK", "Manage Stock", "STOCK");
        Permission orderRefund = ensurePermission("ORDER_REFUND", "Refund Order", "ORDER");

        // 2. Roles
        Role ownerRole = ensureRole("OWNER", "Business Owner", DEFAULT_BUSINESS_ID);
        Role managerRole = ensureRole("MANAGER", "Store Manager", DEFAULT_BUSINESS_ID);
        Role cashierRole = ensureRole("CASHIER", "Cashier", DEFAULT_BUSINESS_ID);

        // 3. Role → permission maps
        ensureRolePermissions(ownerRole, Arrays.asList(
                viewProducts, createProduct, manageOrders, manageStock, orderRefund));
        ensureRolePermissions(managerRole, Arrays.asList(
                viewProducts, createProduct, manageStock));
        ensureRolePermissions(cashierRole, Arrays.asList(viewProducts, manageOrders));

        // 4. Default admin user
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .username(adminUsername)
                    .password(encodePassword(adminPassword))
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .status(1)
                    .isLocked(false)
                    .failedAttempt(0)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default user '{}' (local-seed). Change password before any shared use.", adminUsername);
        } else {
            log.info("Default user '{}' already exists — seed skipped for user row.", adminUsername);
        }

        // 5. Store membership (OWNER on default store)
        final User seededAdmin = admin;
        boolean alreadyMember = storeMemberRepository.findByUser(seededAdmin).stream()
                .anyMatch(sm -> DEFAULT_STORE_ID == sm.getStoreId()
                        && sm.getRole() != null
                        && "OWNER".equals(sm.getRole().getRoleCode()));
        if (!alreadyMember) {
            storeMemberRepository.save(StoreMember.builder()
                    .user(seededAdmin)
                    .storeId(DEFAULT_STORE_ID)
                    .role(ownerRole)
                    .build());
            log.info("Assigned user '{}' to store {} with OWNER role.", adminUsername, DEFAULT_STORE_ID);
        }
    }

    private Permission ensurePermission(String code, String name, String module) {
        return permissionRepository.findByPermissionCode(code)
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permissionCode(code)
                        .permissionName(name)
                        .moduleName(module)
                        .description("Seeded for local development")
                        .build()));
    }

    private Role ensureRole(String code, String name, Long businessId) {
        return roleRepository.findByRoleCode(code)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleCode(code)
                        .roleName(name)
                        .businessId(businessId)
                        .isSystem(true)
                        .description("Seeded for local development")
                        .build()));
    }

    private void ensureRolePermissions(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            boolean linked = rolePermissionRepository.findByRole(role).stream()
                    .anyMatch(rp -> rp.getPermission() != null
                            && permission.getId().equals(rp.getPermission().getId()));
            if (!linked) {
                rolePermissionRepository.save(RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build());
            }
        }
    }

    private String encodePassword(String rawOrAlreadyHashed) {
        if (rawOrAlreadyHashed != null
                && (rawOrAlreadyHashed.startsWith("$2a$")
                || rawOrAlreadyHashed.startsWith("$2b$")
                || rawOrAlreadyHashed.startsWith("$2y$"))) {
            return rawOrAlreadyHashed;
        }
        return passwordEncoder.encode(rawOrAlreadyHashed);
    }
}
