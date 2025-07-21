package com.spadeboot.api.controller;

import com.spadeboot.api.dto.request.user.*;
import com.spadeboot.api.dto.response.UserDto;
import com.spadeboot.domain.user.User;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PlayerRepository playerRepository;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterDto registerDto) {
        User user = userService.register(registerDto);
        UserDto userDto = convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDto loginDto) {
        String token = userService.authenticate(loginDto);

        // Get user details for the response
        User user = userService.getCurrentUser();
        UserDto userDto = convertToDto(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        User user = userService.getCurrentUser();
        UserDto userDto = convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserDto userDto = convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    // Updated endpoints for user management

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(@Valid @RequestBody UpdateUserDto updateUserDto) {
        User currentUser = userService.getCurrentUser();
        User updatedUser = userService.updateUser(currentUser.getId(), updateUserDto);
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me/password")
    public ResponseEntity<UserDto> updateCurrentUserPassword(@Valid @RequestBody UpdatePasswordDto updatePasswordDto) {
        User currentUser = userService.getCurrentUser();
        User updatedUser = userService.updatePassword(currentUser.getId(), updatePasswordDto);
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateCurrentUserAvatar(@RequestParam("avatar") MultipartFile avatarFile) throws IOException {
        User currentUser = userService.getCurrentUser();
        User updatedUser = userService.updateAvatar(currentUser.getId(), avatarFile.getBytes());
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id, @Valid @RequestBody RoleChangeDto roleChangeDto) {
        User updatedUser = userService.updateUserRole(id, roleChangeDto.getIsAdmin());
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/balance")
    public ResponseEntity<UserDto> updateUserBalance(@PathVariable Long id, @Valid @RequestBody UpdateBalanceDto updateBalanceDto) {
        User updatedUser = userService.updateUserBalance(id, updateBalanceDto.getAmount());
        UserDto userDto = convertToDto(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setBalance(user.getBalance());

        // Convert avatar bytes to base64 instead of sending raw bytes
        if (user.getAvatar() != null) {
            dto.setAvatarFromBytes(user.getAvatar());
        } else {
            dto.setAvatarBase64(null);
        }

        dto.setIsAdmin(user.isAdmin());
        return dto;
    }
}