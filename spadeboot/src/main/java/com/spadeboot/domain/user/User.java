package com.spadeboot.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private String email;

    private Integer balance = 1000; // Default starting balance

    private Integer absInvestment = balance;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] avatar;

    @Column(name = "role")
    private String role = "ROLE_USER"; // Default role

    // Method to check if user has admin role
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }
}