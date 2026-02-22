package com.streamtunes.backend.Auth;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    public User(String username, @Nullable String encode, String displayName, AuthProvider authProvider) {
        this.username = username;
        this.password = encode;
        this.displayName = displayName;
        this.authProvider = (authProvider != null) ? authProvider : AuthProvider.LOCAL;
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}
