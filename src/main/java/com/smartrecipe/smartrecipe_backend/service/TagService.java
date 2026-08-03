package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();
    TagResponse getTagById(Integer id);
    TagResponse createTag(String name);
    TagResponse updateTag(Integer id, String name);
    void deleteTag(Integer id);
}