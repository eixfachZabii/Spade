// src/main/java/com/pokerapp/config/SecurityConfig.java
package com.spadeboot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.spadeboot.security.JwtAuthenticationFilter;
import com.spadeboot.security.JwtAuthenticationEntryPoint;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // This enables @PreAuthorize annotations
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - allow both with and without trailing slash
                        .requestMatchers("/api/users/register", "/api/users/register/").permitAll()
                        .requestMatchers("/api/users/login", "/api/users/login/").permitAll()

                        // Hub endpoints that need public access
                        .requestMatchers("/api/spotify/login").permitAll()
                        .requestMatchers("/api/spotify/callback").permitAll()
                        .requestMatchers("/api/spotify/refresh_token").permitAll()
                        .requestMatchers("/api/spotify/lyrics").permitAll()
                        .requestMatchers("/api/spotify/debug/**").permitAll()
                        .requestMatchers("/api/cheatsheet/**").permitAll()

                        // WebSocket endpoints - IMPORTANT: Allow all WebSocket related paths
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/users/{id}/roles").hasRole("ADMIN")
                        .requestMatchers("/api/users/{id}/balance").hasRole("ADMIN")

                        // Protected endpoints for logged-in users
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/tables/**").authenticated()
                        .requestMatchers("/api/games/**").authenticated()
                        .requestMatchers("/api/players/**").authenticated()
                        .requestMatchers("/api/friends/**").authenticated()

                        // Default policy: require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Use specific origins instead of "*" for credentials support
        configuration.setAllowedOrigins(Arrays.asList(
                "https://localhost:3000",
                "http://localhost:3000",
                "https://127.0.0.1:3000",
                "http://127.0.0.1:3000",
                "https://localhost:3001",
                "http://localhost:3001",
                "https://127.0.0.1:3001",
                "http://127.0.0.1:3001"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Auth-Token", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("X-Auth-Token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}