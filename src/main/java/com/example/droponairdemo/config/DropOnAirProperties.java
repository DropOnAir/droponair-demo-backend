package com.example.droponairdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binding for the droponair.* section in application.yml.
 * Replace placeholder values with real credentials from https://panel.droponair.com
 */
@Component
@ConfigurationProperties(prefix = "droponair")
public class DropOnAirProperties {

    /** Your DropOnAir App ID (from the dashboard). */
    private String appId = "YOUR_APP_ID";

    /** Your public API key (safe to expose to clients). */
    private String publicApiKey = "YOUR_PUBLIC_API_KEY";

    /**
     * Your server secret key, NEVER expose to clients.
     * Used for HMAC-SHA256 signing of token exchange requests.
     */
    private String serverSecret = "YOUR_SERVER_SECRET";

    /** Base URL of the DropOnAir SDK backend. */
    private String sdkBaseUrl = "https://sdk.droponair.com";

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPublicApiKey() { return publicApiKey; }
    public void setPublicApiKey(String publicApiKey) { this.publicApiKey = publicApiKey; }
    public String getServerSecret() { return serverSecret; }
    public void setServerSecret(String serverSecret) { this.serverSecret = serverSecret; }
    public String getSdkBaseUrl() { return sdkBaseUrl; }
    public void setSdkBaseUrl(String sdkBaseUrl) { this.sdkBaseUrl = sdkBaseUrl; }
}
