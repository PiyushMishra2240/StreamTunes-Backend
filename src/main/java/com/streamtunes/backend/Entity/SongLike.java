package com.streamtunes.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "song_likes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(SongLikeId.class)
public class SongLike {

    @Id
    @Column(name = "song_id")
    private UUID songId;

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "liked_at")
    @Builder.Default
    private LocalDateTime likedAt = LocalDateTime.now();
}
