package com.streamtunes.backend.Auth;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(
            name = "user_songs",
            joinColumns = @JoinColumn(name = "username", referencedColumnName = "username")
    )
    @Column(name = "song_title")
    private List<String> userSongs;

    public User(String username, @Nullable String encode, String displayName, AuthProvider authProvider) {
        this.username = username;
        this.password = encode;
        this.displayName = displayName;
        this.authProvider = (authProvider != null) ? authProvider : AuthProvider.LOCAL;
        this.userSongs = new ArrayList<>();
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}
