package com.streamtunes.backend.Repository;

import com.streamtunes.backend.Entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SongRepository extends JpaRepository<Song, UUID> {

    @Query("""
        SELECT s FROM Song s
        WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(s.album) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    Page<Song> search(@Param("q") String query, Pageable pageable);

    Optional<Song> findByTitleAndArtistAndAlbum(String title, String artist, String album);

    Page<Song> findByTitleIn(List<String> titles, Pageable pageable);

    Page<Song> findByIsGlobalTrue(Pageable pageable);
}
