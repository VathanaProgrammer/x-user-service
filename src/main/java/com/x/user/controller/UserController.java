package com.x.user.controller;

import com.x.user.dto.UserAuthResponse;
import com.x.user.model.User;
import com.x.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Auth lookup for BFF.
     * Uses 2 DB queries total (user + permission codes) instead of N+1 lazy loads.
     */
    @GetMapping("/{username}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserAuthResponse> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    Set<String> permissions = userRepository.findPermissionCodesByUserId(user.getId());
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
