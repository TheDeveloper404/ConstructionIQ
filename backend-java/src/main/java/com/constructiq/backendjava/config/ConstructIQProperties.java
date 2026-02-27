package com.constructiq.backendjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "constructiq")
public class ConstructIQProperties {
    private boolean demoMode = true;
    private String demoOrgId = "demo-org-001";
    private String demoUserId = "demo-user-001";
    private String corsOrigins = "*";
    private String senderEmail = "onboarding@resend.dev";
    private String resendApiKey = "";
    private String authTokenSecret = "change-this-secret";
    private String authTokenPreviousSecrets = "";
    private long authTokenTtlMinutes = 480;
    private String adminEmail = "admin@constructiq.local";
    private String adminPassword = "admin123";
    private boolean rateLimitEnabled = true;
    private int rateLimitPerMinute = 120;
    private int authLoginRateLimitPerMinute = 20;

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getDemoOrgId() {
        return demoOrgId;
    }

    public void setDemoOrgId(String demoOrgId) {
        this.demoOrgId = demoOrgId;
    }

    public String getDemoUserId() {
        return demoUserId;
    }

    public void setDemoUserId(String demoUserId) {
        this.demoUserId = demoUserId;
    }

    public String getCorsOrigins() {
        return corsOrigins;
    }

    public void setCorsOrigins(String corsOrigins) {
        this.corsOrigins = corsOrigins;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getResendApiKey() {
        return resendApiKey;
    }

    public void setResendApiKey(String resendApiKey) {
        this.resendApiKey = resendApiKey;
    }

    public String getAuthTokenSecret() {
        return authTokenSecret;
    }

    public void setAuthTokenSecret(String authTokenSecret) {
        this.authTokenSecret = authTokenSecret;
    }

    public String getAuthTokenPreviousSecrets() {
        return authTokenPreviousSecrets;
    }

    public void setAuthTokenPreviousSecrets(String authTokenPreviousSecrets) {
        this.authTokenPreviousSecrets = authTokenPreviousSecrets;
    }

    public long getAuthTokenTtlMinutes() {
        return authTokenTtlMinutes;
    }

    public void setAuthTokenTtlMinutes(long authTokenTtlMinutes) {
        this.authTokenTtlMinutes = authTokenTtlMinutes;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getAuthLoginRateLimitPerMinute() {
        return authLoginRateLimitPerMinute;
    }

    public void setAuthLoginRateLimitPerMinute(int authLoginRateLimitPerMinute) {
        this.authLoginRateLimitPerMinute = authLoginRateLimitPerMinute;
    }
}
