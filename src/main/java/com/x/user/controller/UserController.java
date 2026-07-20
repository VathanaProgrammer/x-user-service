package com.x.user.controller;

import com.x.user.dto.UserAuthResponse;
import com.x.user.model.User;
import com.x.user.repository.RolePermissionRepository;
import com.x.user.repository.StoreMemberRepository;
import com.x.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Auth lookup for BFF. Must stay transactional so lazy role/permission graphs load safely
     * (open-in-view is false).
     */
    @GetMapping("/{username}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserAuthResponse> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    Set<String> permissions = storeMemberRepository.findByUser(user).stream()
                            .map(sm -> sm.getRole())
                            .filter(Objects::nonNull)
                            .flatMap(role -> rolePermissionRepository.findByRole(role).stream())
                            .map(rp -> rp.getPermission())
                            .filter(Objects::nonNull)
                            .map(permission -> permission.getPermissionCode())
                            .filter(code -> code != null && !code.isBlank())
                            .collect(Collectors.toSet());

                    return UserAuthResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .permissions(permissions)
                            .build();
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
