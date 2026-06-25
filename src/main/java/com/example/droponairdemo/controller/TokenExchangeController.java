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
     * Exchanges the authenticated user for a DropOnAir SDK token.
     *
     * The SDK sends the user's own JWT in the Authorization header; your security
     * layer validates it and resolves the stable user id. That id is sent as the
     * "customerUserToken" in the HMAC-signed request to DropOnAir, and DropOnAir
     * uses it verbatim as the user's identity (it trusts the signature from your
     * server, never the token contents). Use a stable id here (not the raw JWT),
     * since this is the value peers address each other by.
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> exchangeToken(
        Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();

        log.debug("stage=token_exchange_request userId={}", userId);

        // Pass the STABLE user id as the customerUserToken. DropOnAir uses this
        // value verbatim as the user's identity (the relay user_id that peers
        // address each other by). Sending the raw JWT instead would make every
        // user_id an opaque token string and break peer addressing (alice <-> bob).
        String droponairToken = tokenService.exchangeToken(userId);

        // Return in the format the SDK expects: { accessToken, expiresIn }
        return ResponseEntity.ok(Map.of(
            "accessToken", droponairToken,
            "expiresIn", 3600
        ));
    }
}
