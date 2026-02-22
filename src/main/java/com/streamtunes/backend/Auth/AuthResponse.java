package com.streamtunes.backend.Auth;

public class AuthResponse {
    private String username;
    private String displayName;
    private String token;

    public AuthResponse() {}

    public AuthResponse(String username, String displayName, String token) {
        this.username = username;
        this.displayName = displayName;
        this.token = token;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
