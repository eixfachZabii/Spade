package com.spade.api.dto;

import com.spade.service.LoginResult;

/** The only response that ever carries a token. */
public record SessionResponse(String token, DeviceResponse device) {

    public static SessionResponse from(LoginResult login) {
        return new SessionResponse(login.token(), DeviceResponse.from(login.session(), login.session().id()));
    }
}
