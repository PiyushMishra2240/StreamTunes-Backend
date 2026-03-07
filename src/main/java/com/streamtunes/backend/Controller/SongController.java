package com.streamtunes.backend.Controller;

import com.streamtunes.backend.Entity.Song;
import com.streamtunes.backend.Repository.StorageService;
import com.streamtunes.backend.Service.SongService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin
public class SongController {

    private final SongService service;
    private final StorageService storageService;

    public SongController(SongService service, StorageService storageService) {
        this.service = service;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Song> upload(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam(required = false) String album,
            Principal principal
    ) throws IOException {
        String username = principal.getName();
        return ResponseEntity.ok(service.upload(file, title, artist, album, username));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return service.getSongs(pageable);
    }

    @GetMapping("/user")
    public ResponseEntity<?> userSongs(Principal principal, Pageable pageable) {
        String username = principal.getName();
        return service.getUserSongs(username, pageable);
    }

    @GetMapping("/search")
    public Page<Song> search(@RequestParam String q, Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> stream(
            @PathVariable UUID id,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {

        Song song = service.get(id);

        long fileSize = storageService.getObjectSize(song.getFileKey());
        InputStream inputStream = storageService.download(song.getFileKey());

        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(song.getContentType()))
                    .contentLength(fileSize)
                    .body(new InputStreamResource(inputStream));
        }

        String range = rangeHeader.replace("bytes=", "");
        String[] parts = range.split("-");
        long start = Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty()
                ? Long.parseLong(parts[1])
                : fileSize - 1;

        long contentLength = end - start + 1;

        inputStream.skip(start);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Ranges", "bytes");
        headers.add("Content-Range",
                "bytes " + start + "-" + end + "/" + fileSize);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentLength(contentLength)
                .contentType(MediaType.parseMediaType(song.getContentType()))
                .body(new InputStreamResource(inputStream));
    }
}
