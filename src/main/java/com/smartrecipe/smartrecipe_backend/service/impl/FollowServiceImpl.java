package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.response.AuthorSummaryResponse;
import com.smartrecipe.smartrecipe_backend.entity.Follow;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.FollowRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    @Override
    public void followUser(Long targetUserId, String currentUsername) {
        User follower = getUserByUsername(currentUsername);
        User following = getUserById(targetUserId);

        if (follower.getId().equals(following.getId())) {
            throw new BadRequestException("Bạn không thể tự theo dõi chính mình");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
            throw new BadRequestException("Bạn đã theo dõi người dùng này rồi");
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        
        followRepository.save(follow);
    }

    @Override
    public void unfollowUser(Long targetUserId, String currentUsername) {
        User follower = getUserByUsername(currentUsername);
        User following = getUserById(targetUserId);

        if (!followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
            throw new BadRequestException("Bạn chưa theo dõi người dùng này");
        }

        followRepository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorSummaryResponse> getFollowers(Long userId, int page, int size) {
        User user = getUserById(userId); // Ensure user exists
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return followRepository.findByFollowingId(user.getId(), pageable)
                .map(follow -> mapToAuthorSummaryResponse(follow.getFollower()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorSummaryResponse> getFollowing(Long userId, int page, int size) {
        User user = getUserById(userId); // Ensure user exists
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return followRepository.findByFollowerId(user.getId(), pageable)
                .map(follow -> mapToAuthorSummaryResponse(follow.getFollowing()));
    }

    private AuthorSummaryResponse mapToAuthorSummaryResponse(User user) {
        return AuthorSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
