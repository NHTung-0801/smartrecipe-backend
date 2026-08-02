package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;

import java.util.List;

public interface AisleService {
    List<AisleResponse> getAllAisles();
    AisleResponse getAisleById(Integer id);
    AisleResponse createAisle(String name);
    AisleResponse updateAisle(Integer id, String name);
    void deleteAisle(Integer id);
}