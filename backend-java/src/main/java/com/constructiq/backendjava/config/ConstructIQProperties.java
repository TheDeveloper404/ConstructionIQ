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
}
