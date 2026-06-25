package com.example.droponairdemo.service;

import com.example.droponairdemo.config.DropOnAirProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Performs the server-side DropOnAir token exchange.
 *
 * This is the ONLY place where the server secret should be used.
 * The customer's user token (JWT) is signed using HMAC-SHA256 and
 * exchanged for a short-lived DropOnAir JWT that the client SDK uses
 * to authenticate its WebSocket connection.
 *
 * HMAC signature = Base64( HMAC-SHA256(serverSecret,
 *                             appId + timestamp + nonce + SHA256Base64(body)) )
 */
@Service
public class DropOnAirTokenService {

    private static final Logger log = LoggerFactory.getLogger(DropOnAirTokenService.class);

    private final DropOnAirProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DropOnAirTokenService(DropOnAirProperties props) {
        this.props = props;
    }

    /**
     * Exchanges a customer user token for a DropOnAir JWT.
     *
     * @param customerUserToken  The user's JWT from your auth system
     * @return DropOnAir access token
     * @throws RuntimeException if the exchange fails
     */
    public String exchangeToken(String customerUserToken) {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = UUID.randomUUID().toString().replace("-", "");

            // Build the JSON request body POSTed to the relay.
            String body = objectMapper.writeValueAsString(
                Map.of("customerUserToken", customerUserToken)
            );

            // IMPORTANT: the relay computes the signed body hash over the customerUserToken VALUE,
            // not the JSON envelope. The signature must hash the same input, or the relay
            // returns 401 "Invalid signature".
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] bodyDigest = sha256.digest(customerUserToken.getBytes(StandardCharsets.UTF_8));
            String bodySha256Base64 = Base64.getEncoder().encodeToString(bodyDigest);

            // Compute HMAC-SHA256 signature
            String signatureData = props.getAppId() + timestamp + nonce + bodySha256Base64;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                props.getServerSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            ));
            String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(signatureData.getBytes(StandardCharsets.UTF_8))
            );

            // POST to DropOnAir token exchange endpoint
            String url = props.getSdkBaseUrl() + "/api/token/exchange";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-DropOnAir-Key", props.getAppId())
                .header("X-DropOnAir-Timestamp", timestamp)
                .header("X-DropOnAir-Nonce", nonce)
                .header("X-DropOnAir-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            log.debug("stage=token_exchange_sending appId={} url={}", props.getAppId(), url);

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("stage=token_exchange_failed status={} body={}",
                    response.statusCode(), response.body());
                throw new RuntimeException(
                    "DropOnAir token exchange failed with status " + response.statusCode()
                );
            }

            JsonNode responseJson = objectMapper.readTree(response.body());

            // Support both "accessToken" and "token" field names
            JsonNode tokenNode = responseJson.has("accessToken")
                ? responseJson.get("accessToken")
                : responseJson.get("token");

            if (tokenNode == null || tokenNode.isNull()) {
                throw new RuntimeException("No token in DropOnAir response: " + response.body());
            }

            log.debug("stage=token_exchange_success appId={}", props.getAppId());
            return tokenNode.asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Token exchange error: " + e.getMessage(), e);
        }
    }
}
