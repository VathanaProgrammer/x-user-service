package com.VyntraUserService.UserService.controller;

import com.VyntraUserService.UserService.dto.UserAuthResponse;
import com.VyntraUserService.UserService.model.User;
import com.VyntraUserService.UserService.repository.StoreMemberRepository;
import com.VyntraUserService.UserService.repository.UserRepository;
import com.VyntraUserService.UserService.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserAuthResponse> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    Set<String> permissions = storeMemberRepository.findByUser(user).stream()
                            .flatMap(sm -> rolePermissionRepository.findByRole(sm.getRole()).stream())
                            .map(rp -> rp.getPermission().getPermissionCode())
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
