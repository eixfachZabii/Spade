package com.spadeboot.config;

import com.spadeboot.repository.FriendshipRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserService userService;
    @InjectMocks private DataInitializer initializer;

    @Test
    void onlyRunsInTheDevProfile() {
        Profile profile = DataInitializer.class.getAnnotation(Profile.class);
        assertNotNull(profile, "DataInitializer must carry @Profile(\"dev\")");
        assertArrayEquals(new String[] {"dev"}, profile.value());
    }

    @Test
    void skipsSeedingWhenNoSeedPasswordIsConfigured() {
        ReflectionTestUtils.setField(initializer, "seedPassword", "");
        initializer.run();
        verifyNoInteractions(userRepository, friendshipRepository, passwordEncoder, userService);
    }

    @Test
    void seedsUsersWithTheConfiguredPasswordAndNoHardCodedOne() {
        ReflectionTestUtils.setField(initializer, "seedPassword", "from-env-123");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("from-env-123")).thenReturn("hashed");
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        initializer.run();

        verify(passwordEncoder, atLeastOnce()).encode("from-env-123");
        verify(passwordEncoder, never()).encode("admin123");
        verify(passwordEncoder, never()).encode("password123");
    }
}
