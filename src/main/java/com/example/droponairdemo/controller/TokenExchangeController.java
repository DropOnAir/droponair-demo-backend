package com.example.droponairdemo.controller;

import com.example.droponairdemo.service.DropOnAirTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Token exchange proxy.
 *
 * The client SDK calls this endpoint with its user JWT.
 * This controller forwards the request to DropOnAir's token exchange API
 * after signing it with the server secret (HMAC-SHA256).
 *
 * The server secret NEVER leaves the backend.
 *
 * SDK configuration:
 *   tokenExchangeEndpoint: '/api/droponair/token'   ← this endpoint
 *
 * Flow:
 *   Client → POST /api/droponair/token (Bearer: user JWT)
 *   Backend → POST sdk.droponair.com/api/token/exchange (HMAC-signed)
 *   DropOnAir → returns short-lived droponair JWT
 *   Backend → Client: { accessToken: "...", expiresIn: 3600 }
 */
@RestController
@RequestMapping("/api/droponair")
public class TokenExchangeController {

    private static final Logger log = LoggerFactory.getLogger(TokenExchangeController.class);

    private final DropOnAirTokenService tokenService;

    public TokenExchangeController(DropOnAirTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Exchanges the authenticated user's demo JWT for a DropOnAir SDK token.
     *
     * The user's JWT is passed as the "customerUserToken" in the signed request
     * to DropOnAir. DropOnAir uses it to identify the user without ever seeing
     * the token contents (it trusts the HMAC signature from your server).
     *
     * The Authorization header contains the user's own JWT (set by the SDK automatically).
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> exchangeToken(
        @RequestHeader("Authorization") String authHeader,
        Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        // Pass the raw user JWT as the customer token
        String userJwt = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        log.debug("stage=token_exchange_request userId={}", userId);

        String droponairToken = tokenService.exchangeToken(userJwt);

        // Return in the format the SDK expects: { accessToken, expiresIn }
        return ResponseEntity.ok(Map.of(
            "accessToken", droponairToken,
            "expiresIn", 3600
        ));
    }
}
