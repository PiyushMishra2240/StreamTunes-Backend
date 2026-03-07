package com.streamtunes.backend.Service;

import com.streamtunes.backend.Auth.User;
import com.streamtunes.backend.Auth.repository.UserRepository;
import com.streamtunes.backend.Entity.Song;
import com.streamtunes.backend.Repository.SongRepository;
import com.streamtunes.backend.Repository.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SongService {

    private final SongRepository repository;
    private final StorageService storageService;
    private final UserRepository userRepository;

    public SongService(SongRepository repository, StorageService storageService, UserRepository userRepository) {
        this.repository = repository;
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    public Song upload(MultipartFile file, String title, String artist, String album, String username) throws IOException {
        // Check if an identical song already exists (same title + artist + album)
        Optional<Song> existingSong = repository.findByTitleAndArtistAndAlbum(title, artist, album);

        Song song;
        if (existingSong.isPresent()) {
            // Reuse existing song — no new file upload or Song entity creation
            song = existingSong.get();
        } else {
            // New song — upload file and create entity
            UUID id = UUID.randomUUID();
            String key = id + "_" + file.getOriginalFilename();

            storageService.upload(
                    key,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            song = Song.builder()
                    .id(id)
                    .title(title)
                    .artist(artist)
                    .album(album)
                    .fileKey(key)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            song = repository.save(song);
        }

        // Add the song title to the user's song list (avoid duplicates)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getUserSongs() == null) {
            user.setUserSongs(new ArrayList<>());
        }

        if (!user.getUserSongs().contains(title)) {
            user.getUserSongs().add(title);
            userRepository.save(user);
        }

        return song;
    }

    public ResponseEntity<?> getSongs(Pageable pageable) {
        Page<Song> page = repository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("songs", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalItems", page.getTotalElements());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getUserSongs(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<String> userSongTitles = user.getUserSongs();
        if (userSongTitles == null || userSongTitles.isEmpty()) {
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("songs", Collections.emptyList());
            emptyResponse.put("currentPage", 0);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("totalItems", 0);
            return ResponseEntity.ok(emptyResponse);
        }

        Page<Song> page = repository.findByTitleIn(userSongTitles, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("songs", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalItems", page.getTotalElements());

        return ResponseEntity.ok(response);
    }

    public Page<Song> search(String query, Pageable pageable) {
        return repository.search(query, pageable);
    }

    public Song get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
    }
}
