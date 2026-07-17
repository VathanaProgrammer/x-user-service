package com.VyntraUserService.UserService.config;

import com.VyntraUserService.UserService.model.*;
import com.VyntraUserService.UserService.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("local-seed")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Value("${vyntra.seed.admin-password}")
    private String adminPassword;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return;
        }

        // 1. Create Permissions
        Permission viewProducts = createPermission("VIEW_PRODUCTS", "View Products", "PRODUCT");
        Permission createProduct = createPermission("PRODUCT_CREATE", "Create Product", "PRODUCT");
        Permission orderRefund = createPermission("ORDER_REFUND", "Refund Order", "ORDER");

        // 2. Create Roles
        Role ownerRole = createRole("OWNER", "Business Owner", 1L);
        Role managerRole = createRole("MANAGER", "Store Manager", 1L);
        Role cashierRole = createRole("CASHIER", "Cashier", 1L);

        // 3. Map Role to Permissions
        mapRoleToPermissions(ownerRole, Arrays.asList(viewProducts, createProduct, orderRefund));
        mapRoleToPermissions(managerRole, Arrays.asList(viewProducts, createProduct));
        mapRoleToPermissions(cashierRole, Arrays.asList(viewProducts));

        // 4. Create Default User (password always stored as BCrypt)
        User admin = User.builder()
                .username("admin")
                .password(encodePassword(adminPassword))
                .fullName("System Administrator")
                .status(1)
                .isLocked(false)
                .failedAttempt(0)
                .build();
        userRepository.save(admin);

        // 5. Assign User to Store with Role
        StoreMember adminMembership = StoreMember.builder()
                .user(admin)
                .storeId(101L)
                .role(ownerRole)
                .build();
        storeMemberRepository.save(adminMembership);
    }

    private Permission createPermission(String code, String name, String module) {
        Permission permission = Permission.builder()
                .permissionCode(code)
                .permissionName(name)
                .moduleName(module)
                .build();
        return permissionRepository.save(permission);
    }

    private Role createRole(String code, String name, Long businessId) {
        Role role = Role.builder()
                .roleCode(code)
                .roleName(name)
                .businessId(businessId)
                .isSystem(true)
                .build();
        return roleRepository.save(role);
    }

    private void mapRoleToPermissions(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build();
            rolePermissionRepository.save(rp);
        }
    }

    private String encodePassword(String rawOrAlreadyHashed) {
        if (rawOrAlreadyHashed != null
                && (rawOrAlreadyHashed.startsWith("$2a$")
                || rawOrAlreadyHashed.startsWith("$2b$")
                || rawOrAlreadyHashed.startsWith("$2y$"))) {
            return rawOrAlreadyHashed;
        }
        return passwordEncoder.encode(rawOrAlreadyHashed == null ? "" : rawOrAlreadyHashed);
    }
}
