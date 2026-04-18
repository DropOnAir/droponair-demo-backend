package com.example.droponairdemo.controller;

import com.example.droponairdemo.service.DemoJwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock authentication controller.
 *
 * In a real application you already have your own auth system.
 * This controller exists only to simulate user login for the demos.
 *
 * POST /api/auth/login , accepts a userId + displayName, returns a demo JWT
 * GET  /api/auth/me    , returns the current user's info (from JWT)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DemoJwtService jwtService;

    public AuthController(DemoJwtService jwtService) {
        this.jwtService = jwtService;
    }

    record LoginRequest(
        @NotBlank @Size(min = 1, max = 64) String userId,
        @Size(max = 64) String displayName
    ) {}

    record LoginResponse(String jwt, String userId, String displayName) {}

    /**
     * Mock login, accepts any userId without password verification.
     * In production, replace with real credential verification.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String displayName = (request.displayName() != null && !request.displayName().isBlank())
            ? request.displayName()
            : request.userId();

        String jwt = jwtService.issueToken(request.userId(), displayName);
        return ResponseEntity.ok(new LoginResponse(jwt, request.userId(), displayName));
    }

    /**
     * Returns the current authenticated user's info.
     * The JWT auth filter has already validated the token and set Authentication.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String displayName = jwtService.extractDisplayName(
            // Re-read JWT from security context, in prod, store in a UserDetails object
            // For simplicity here, derive display name from userId
            jwtService.issueToken(userId, userId)  // dummy call just to getDisplayName below
        );
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "displayName", userId  // Simplified for demo
        ));
    }
}
