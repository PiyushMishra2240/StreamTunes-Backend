package com.streamtunes.backend.Auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.streamtunes.backend.Auth.AuthResponse;
import com.streamtunes.backend.Auth.User;
import com.streamtunes.backend.Auth.repository.UserRepository;
import com.streamtunes.backend.Auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleVerifier;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Value("${google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public AuthResponse register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username cannot be empty");
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        String displayName = extractDisplayName(username);

        User user = new User(
                username,
                passwordEncoder.encode(password),
                displayName,
                User.AuthProvider.LOCAL);
        userRepository.save(user);

        String token = jwtUtil.generateToken(username);
        return new AuthResponse(username, displayName, token);
    }

    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new RuntimeException("This account uses Google sign-in. Please use Google to log in.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(username);
        return new AuthResponse(username, user.getDisplayName(), token);
    }

    public AuthResponse googleLogin(String credential) {
        try {
            GoogleIdToken idToken = googleVerifier.verify(credential);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            if (email == null || email.isBlank()) {
                throw new RuntimeException("Google account has no email");
            }

            String displayName = (name != null && !name.isBlank()) ? name : extractDisplayName(email);

            User user = userRepository.findByUsername(email).orElse(null);

            if (user == null) {
                // Create new Google user
                user = new User(email, null, displayName, User.AuthProvider.GOOGLE);
                userRepository.save(user);
            } else if (user.getAuthProvider() != User.AuthProvider.GOOGLE) {
                throw new RuntimeException(
                        "An account with this email already exists. Please log in with your password.");
            }

            String token = jwtUtil.generateToken(email);
            return new AuthResponse(email, displayName, token);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    private String extractDisplayName(String username) {
        if (username.contains("@")) {
            return username.substring(0, username.indexOf("@"));
        }
        return username;
    }
}
