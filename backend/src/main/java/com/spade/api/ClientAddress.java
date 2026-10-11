package com.spade.api;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Where a request really came from. Behind the Cloudflare Tunnel (ADR 0007) the remote address is always
 * cloudflared's, and Cloudflare puts the caller in CF-Connecting-IP.
 */
final class ClientAddress {

    private ClientAddress() {
    }

    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("CF-Connecting-IP");
        return forwarded != null && !forwarded.isBlank() ? forwarded.strip() : request.getRemoteAddr();
    }
}
