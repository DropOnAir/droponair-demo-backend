package com.example.droponairdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties for the demo's own JWT signing (NOT the DropOnAir server secret).
 * The demo backend issues simple HS256 tokens so the client has a "user JWT"
 * to pass to the token exchange endpoint.
 */
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private String jwtSecret = "demo-super-secret-key-change-in-prod-min-256bits";
    private int jwtExpiryHours = 24;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public int getJwtExpiryHours() { return jwtExpiryHours; }
    public void setJwtExpiryHours(int jwtExpiryHours) { this.jwtExpiryHours = jwtExpiryHours; }
}
