package com.x.user.repository;

import com.x.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /**
     * Permission codes for a user (one SQL round-trip).
     */
    @Query(value = """
            SELECT DISTINCT p.permission_code
            FROM store_members sm
            INNER JOIN roles r ON sm.role_id = r.id
            INNER JOIN role_permissions rp ON rp.role_id = r.id
            INNER JOIN permissions p ON rp.permission_id = p.id
            WHERE sm.user_id = :userId
              AND p.permission_code IS NOT NULL
              AND p.permission_code <> ''
            """, nativeQuery = true)
    Set<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
