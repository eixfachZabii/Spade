package com.spadeboot.service;

import com.spadeboot.api.dto.request.user.LoginDto;
import com.spadeboot.api.dto.request.user.RegisterDto;
import com.spadeboot.api.dto.request.user.UpdatePasswordDto;
import com.spadeboot.api.dto.request.user.UpdateUserDto;
import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.User;
import com.spadeboot.exception.NotFoundException;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.security.JwtUtils;
import com.spadeboot.security.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Transactional
    public User register(RegisterDto registerDto) {
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create the base User entity
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setBalance(1000);  // Default starting balance
        user.setRole("ROLE_USER"); // Default role

        user = userRepository.save(user);

        createPlayer(user.getId());

        return user;
    }

    @Transactional
    public String authenticate(LoginDto loginDto) {
        // Perform authentication using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        // Set the authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        return jwtUtils.generateJwtToken(authentication);
    }

    @Transactional
    public void createPlayer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for user ID: " + userId));

        //Only create Player if absent
        if(playerRepository.existsByUserId(userId)) {
            playerRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Error fetching Player with user ID: " + userId));
            return;
        }

        Player player = new Player();
        player.setUser(user);

        playerRepository.save(player);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
    }



    @Transactional
    public User updateUser(Long userId, UpdateUserDto updateUserDto) {
        User user = getUserById(userId);

        // Check if username is being changed and if it's already taken
        if (!user.getUsername().equals(updateUserDto.getUsername()) &&
                userRepository.existsByUsername(updateUserDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(updateUserDto.getEmail()) &&
                userRepository.existsByEmail(updateUserDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setUsername(updateUserDto.getUsername());
        user.setEmail(updateUserDto.getEmail());

        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(Long userId, UpdatePasswordDto updatePasswordDto) {
        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(updatePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(updatePasswordDto.getNewPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public User updateAvatar(Long userId, byte[] avatar) {
        User user = getUserById(userId);
        user.setAvatar(avatar);
        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserRole(Long userId, boolean isAdmin) {
        User user = getUserById(userId);

        if (isAdmin) {
            user.setRole("ROLE_ADMIN");
        } else {
            user.setRole("ROLE_USER");
        }

        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserBalance(Long userId, Integer amount) {
        User user = getUserById(userId);
        user.setBalance(user.getBalance() + amount);
        user.setAbsInvestment(user.getAbsInvestment() + amount);
        return userRepository.save(user);
    }
}