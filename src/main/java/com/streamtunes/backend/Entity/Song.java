package com.streamtunes.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "songs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song {
    @Id
    private UUID id;

    private String title;
    private String artist;
    private String album;

    private Long duration;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
