package com.VyntraUserService.UserService.config;

import com.VyntraUserService.UserService.model.*;
import com.VyntraUserService.UserService.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final StoreMemberRepository storeMemberRepository;

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

        // 4. Create Default User
        User admin = User.builder()
                .username("admin")
                .password("admin123") // Should be encoded in a real app
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
}
