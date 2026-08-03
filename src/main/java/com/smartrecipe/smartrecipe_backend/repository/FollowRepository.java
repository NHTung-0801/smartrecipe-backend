package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    long countByFollowingId(Long followingId);
    
    long countByFollowerId(Long followerId);
    
    List<Follow> findByFollowerId(Long followerId);
    
    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);
    
    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);
    
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
