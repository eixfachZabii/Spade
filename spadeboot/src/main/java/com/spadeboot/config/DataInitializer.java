package com.spadeboot.config;

import com.spadeboot.domain.user.Friendship;
import com.spadeboot.domain.user.FriendshipStatus;
import com.spadeboot.domain.user.User;
import com.spadeboot.repository.FriendshipRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the poker-night regulars as mutual friends, for local development only.
 * Runs only in the {@code dev} profile, and only when SPADE_SEED_PASSWORD is set.
 * Every seed user shares that password, and it is never logged.
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_NAME = "Hoerter";
    private static final String[] PLAYER_NAMES = {"Sebastian", "Markus", "Matthi", "Luca", "Paul", "Viktor"};

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Value("${spade.seed.password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("SPADE_SEED_PASSWORD is not set; skipping the dev seed users");
            return;
        }

        List<User> users = new ArrayList<>();
        if (userRepository.findByUsername(ADMIN_NAME).isEmpty()) {
            users.add(newUser(ADMIN_NAME, "ROLE_ADMIN", 50000));
        }
        for (String name : PLAYER_NAMES) {
            if (userRepository.findByUsername(name).isEmpty()) {
                users.add(newUser(name, "ROLE_USER", 2000));
            }
        }

        users = userRepository.saveAll(users);
        for (User user : users) {
            userService.createPlayer(user.getId());
            log.info("Seed user created: {}", user.getUsername());
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                User a = users.get(i);
                User b = users.get(j);
                if (friendshipRepository.findFriendship(a, b).isEmpty()) {
                    Friendship friendship = new Friendship();
                    friendship.setRequester(a);
                    friendship.setAddressee(b);
                    friendship.setStatus(FriendshipStatus.ACCEPTED);
                    friendship.setCreatedAt(now);
                    friendship.setUpdatedAt(now);
                    friendshipRepository.save(friendship);
                }
            }
        }
    }

    private User newUser(String name, String role, int balance) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name.toLowerCase() + "@spade.com");
        user.setPassword(passwordEncoder.encode(seedPassword));
        user.setRole(role);
        user.setBalance(balance);
        return user;
    }
}
