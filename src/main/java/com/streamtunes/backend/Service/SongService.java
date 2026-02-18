package com.streamtunes.backend.Service;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SongService {

    private final SongRepository repository;
    private final StorageService storageService;

    public SongService(SongRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    public Song upload(MultipartFile file, String title, String artist, String album) throws IOException {

        UUID id = UUID.randomUUID();
        String key = id + "_" + file.getOriginalFilename();

        storageService.upload(
                key,
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );

        Song song = Song.builder()
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

        return repository.save(song);
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

    public Page<Song> search(String query, Pageable pageable) {
        return repository.search(query, pageable);
    }

    public Song get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
    }
}
