package com.streamtunes.backend.Service;

import com.streamtunes.backend.Auth.User;
import com.streamtunes.backend.Auth.repository.UserRepository;
import com.streamtunes.backend.Entity.Song;
import com.streamtunes.backend.Repository.SongLikeRepository;
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class SongService {

    private final SongRepository repository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final SongLikeRepository songLikeRepository;

    public SongService(SongRepository repository, StorageService storageService, UserRepository userRepository,
                       SongLikeRepository songLikeRepository) {
        this.repository = repository;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.songLikeRepository = songLikeRepository;
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
                    .uploadedBy(username)
                    .isGlobal(false)
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
        Page<Song> page = repository.findByIsGlobalTrue(pageable);
        
        // Populate isLikedByCurrentUser for the currently logged-in user if available
        String username = getCurrentUsername();
        if (username != null && !page.getContent().isEmpty()) {
            List<com.streamtunes.backend.Entity.SongLike> userLikes = songLikeRepository.findAllByUsername(username);
            Set<UUID> likedSongIds = new HashSet<>();
            for (com.streamtunes.backend.Entity.SongLike like : userLikes) {
                likedSongIds.add(like.getSongId());
            }
            for (Song song : page.getContent()) {
                if (likedSongIds.contains(song.getId())) {
                    song.setIsLikedByCurrentUser(true);
                }
            }
        }

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
        
        if (!page.getContent().isEmpty()) {
            List<com.streamtunes.backend.Entity.SongLike> userLikes = songLikeRepository.findAllByUsername(username);
            Set<UUID> likedSongIds = new HashSet<>();
            for (com.streamtunes.backend.Entity.SongLike like : userLikes) {
                likedSongIds.add(like.getSongId());
            }
            for (Song song : page.getContent()) {
                if (likedSongIds.contains(song.getId())) {
                    song.setIsLikedByCurrentUser(true);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("songs", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalItems", page.getTotalElements());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getAnalytics(String username, String sortBy, int page, int size) {
        Sort sort;
        if ("date".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            // Default to sorting by likes
            sort = Sort.by(Sort.Direction.DESC, "likeCount");
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Song> analyticsPage = repository.findByUploadedByAndIsGlobalTrue(username, pageable);
        
        if (!analyticsPage.getContent().isEmpty()) {
            List<com.streamtunes.backend.Entity.SongLike> userLikes = songLikeRepository.findAllByUsername(username);
            Set<UUID> likedSongIds = new HashSet<>();
            for (com.streamtunes.backend.Entity.SongLike like : userLikes) {
                likedSongIds.add(like.getSongId());
            }
            for (Song song : analyticsPage.getContent()) {
                if (likedSongIds.contains(song.getId())) {
                    song.setIsLikedByCurrentUser(true);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("songs", analyticsPage.getContent());
        response.put("currentPage", analyticsPage.getNumber());
        response.put("totalPages", analyticsPage.getTotalPages());
        response.put("totalItems", analyticsPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    public Page<Song> search(String query, Pageable pageable) {
        return repository.search(query, pageable);
    }

    public Song get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
    }

    public Song toggleGlobal(UUID id, String username) {
        Song song = get(id);
        
        // If the song doesn't have an owner recorded yet, we can retroactively assign it to the user who toggle it
        // since we only show songs in "Your Songs" if the title exists in their user.getUserSongs() list anyway.
        if (song.getUploadedBy() == null) {
            song.setUploadedBy(username);
        } else if (!song.getUploadedBy().equals(username)) {
            throw new RuntimeException("Only the owner can toggle the global status of this song");
        }
        
        song.setIsGlobal(song.getIsGlobal() == null ? true : !song.getIsGlobal());
        return repository.save(song);
    }
    
    @org.springframework.transaction.annotation.Transactional
    public void toggleLike(UUID songId, String username) {
        boolean alreadyLiked = songLikeRepository.existsBySongIdAndUsername(songId, username);
        
        if (alreadyLiked) {
            songLikeRepository.deleteBySongIdAndUsername(songId, username);
            repository.decrementLikeCount(songId);
        } else {
            com.streamtunes.backend.Entity.SongLike like = com.streamtunes.backend.Entity.SongLike.builder()
                    .songId(songId)
                    .username(username)
                    .likedAt(LocalDateTime.now())
                    .build();
            songLikeRepository.save(like);
            repository.incrementLikeCount(songId);
        }
    }
    
    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return null;
    }
}
