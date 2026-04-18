package com.example.droponairdemo.controller;

import com.example.droponairdemo.service.KeyDirectoryService;
import com.example.droponairdemo.service.KeyDirectoryService.DeviceKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public key directory.
 *
 * Users publish their X25519 public key so peers can fetch it to derive
 * a shared secret for end-to-end encryption.
 *
 * DropOnAir NEVER stores or validates user keys. The customer backend
 * (this service) is the source of truth for public keys.
 *
 * SDK configuration:
 *   keyDirectoryEndpoint: '/api/droponair/keys'   ← this controller's base path
 *
 * SDK will call:
 *   PUT /api/droponair/keys/me             , publish own public key
 *   GET /api/droponair/keys/{userId}       , fetch peer's public key
 */
@RestController
@RequestMapping("/api/droponair/keys")
public class KeyDirectoryController {

    private static final Logger log = LoggerFactory.getLogger(KeyDirectoryController.class);

    private final KeyDirectoryService keyDirectoryService;

    public KeyDirectoryController(KeyDirectoryService keyDirectoryService) {
        this.keyDirectoryService = keyDirectoryService;
    }

    record PublishKeyRequest(
        @NotBlank @Size(min = 40, max = 64) String publicKey,
        String deviceId
    ) {}

    /**
     * Publishes the authenticated user's X25519 public key.
     * Called by the SDK on initialization (and on key rotation).
     *
     * The SDK also sends the X-Device-Id header, we use that to support
     * multi-device scenarios (stored but demo only keeps latest).
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, String>> publishKey(
        @Valid @RequestBody PublishKeyRequest request,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader,
        Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        String deviceId = (request.deviceId() != null && !request.deviceId().isBlank())
            ? request.deviceId()
            : (deviceIdHeader != null ? deviceIdHeader : "default");

        keyDirectoryService.putKey(userId, deviceId, request.publicKey());
        log.debug("stage=key_published userId={} deviceId={}", userId, deviceId);

        return ResponseEntity.ok(Map.of("status", "ok", "userId", userId));
    }

    /**
     * Returns the public key(s) for a given user.
     * The SDK fetches this when preparing to send a message to a peer.
     *
     * Response format (matches sdk-js expectations):
     *   { "publicKey": "<base64>", "deviceKeys": [{ "deviceId": "...", "publicKey": "..." }] }
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getKey(
        @PathVariable String userId,
        Authentication authentication
    ) {
        DeviceKey key = keyDirectoryService.getKey(userId);
        if (key == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, String>> deviceKeys = keyDirectoryService.getDeviceKeys(userId)
            .stream()
            .map(dk -> Map.of("deviceId", dk.deviceId(), "publicKey", dk.publicKey()))
            .toList();

        return ResponseEntity.ok(Map.of(
            "publicKey", key.publicKey(),
            "deviceKeys", deviceKeys
        ));
    }
}
