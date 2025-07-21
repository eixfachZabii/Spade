// src/main/java/com/pokerapp/api/dto/request/FriendRequestDto.java
package com.spadeboot.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDto {
    @NotBlank(message = "Username is required")
    private String username;
}