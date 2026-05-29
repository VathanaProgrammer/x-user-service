package com.VyntraUserService.UserService.repository;

import com.VyntraUserService.UserService.model.RolePermission;
import com.VyntraUserService.UserService.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(Role role);
}
