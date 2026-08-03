package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.response.TagResponse;
import com.smartrecipe.smartrecipe_backend.entity.Tag;
import com.smartrecipe.smartrecipe_backend.exception.DuplicateResourceException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.TagRepository;
import com.smartrecipe.smartrecipe_backend.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Cacheable("tags")
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "tag", key = "#id")
    @Transactional(readOnly = true)
    public TagResponse getTagById(Integer id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tag với ID: " + id));
        return mapToResponse(tag);
    }

    @Override
    @CacheEvict(value = {"tags", "tag"}, allEntries = true)
    public TagResponse createTag(String name) {
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag \"" + name + "\" đã tồn tại");
        }
        Tag tag = Tag.builder()
                .name(name)
                .build();
        Tag saved = tagRepository.save(tag);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"tags", "tag"}, allEntries = true)
    public TagResponse updateTag(Integer id, String name) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tag với ID: " + id));

        // Kiểm tra tên mới có trùng với tag khác không
        tagRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Tag \"" + name + "\" đã tồn tại");
            }
        });

        tag.setName(name);
        Tag saved = tagRepository.save(tag);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"tags", "tag"}, allEntries = true)
    public void deleteTag(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy tag với ID: " + id);
        }
        tagRepository.deleteById(id);
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}