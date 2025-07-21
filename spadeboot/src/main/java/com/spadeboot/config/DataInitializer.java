// src/main/java/com/pokerapp/config/DataInitializer.java
package com.spadeboot.config;

import com.spadeboot.domain.user.Friendship;
import com.spadeboot.domain.user.FriendshipStatus;
import com.spadeboot.domain.user.User;
import com.spadeboot.repository.FriendshipRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) {
        // Create list of users to add
        List<User> users = new ArrayList<>();

        // Add admin user if it doesn't exist
        if (userRepository.findByUsername("Hoerter").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("Hoerter");
            adminUser.setEmail("Hoerter@spade.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setRole("ROLE_ADMIN");
            adminUser.setBalance(50000); // Give admin a higher starting balance

            users.add(adminUser);
            System.out.println("Admin user created with username: Hoerter and password: admin123");
        }

        // Add regular players
        String[] playerNames = {"Sebastian", "Markus", "Matthi", "Luca", "Paul", "Viktor"};
        String defaultPassword = "password123";

        for (String name : playerNames) {
            if (userRepository.findByUsername(name).isEmpty()) {
                User player = new User();
                player.setUsername(name);
                player.setEmail(name.toLowerCase() + "@spade.com");
                player.setPassword(passwordEncoder.encode(defaultPassword));
                player.setRole("ROLE_USER");
                player.setBalance(2000); // Starting balance for regular players

                users.add(player);
                System.out.println("Player created with username: " + name + " and password: " + defaultPassword);
            }
        }

        // Save all users
        users = userRepository.saveAll(users);

        // Create Player objects for all users
        for (User user : users) {
            userService.createPlayer(user.getId());
        }

        // Create friendships between all players if they don't already exist
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                User user1 = users.get(i);
                User user2 = users.get(j);

                // Check if a friendship already exists between these users
                if (friendshipRepository.findFriendship(user1, user2).isEmpty()) {
                    Friendship friendship = new Friendship();
                    friendship.setRequester(user1);
                    friendship.setAddressee(user2);
                    friendship.setStatus(FriendshipStatus.ACCEPTED); // All are already friends
                    friendship.setCreatedAt(now);
                    friendship.setUpdatedAt(now);

                    friendshipRepository.save(friendship);
                    System.out.println("Created friendship between " + user1.getUsername() + " and " + user2.getUsername());
                }
            }
        }
    }
}