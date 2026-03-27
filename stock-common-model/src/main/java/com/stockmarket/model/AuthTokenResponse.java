package com.stockmarket.model;

import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuthTokenResponse {

    private String token;
    private Map<String, String> cookies;
    private String crumb;
    private long expiresIn;
    private long timestamp;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public String getCrumb() {
        return crumb;
    }

    public void setCrumb(String crumb) {
        this.crumb = crumb;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
