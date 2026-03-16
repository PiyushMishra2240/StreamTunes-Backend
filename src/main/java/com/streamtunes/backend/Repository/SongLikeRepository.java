package com.streamtunes.backend.Repository;

import com.streamtunes.backend.Entity.SongLike;
import com.streamtunes.backend.Entity.SongLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SongLikeRepository extends JpaRepository<SongLike, SongLikeId> {
    boolean existsBySongIdAndUsername(UUID songId, String username);
    void deleteBySongIdAndUsername(UUID songId, String username);
    List<SongLike> findAllByUsername(String username);
}
