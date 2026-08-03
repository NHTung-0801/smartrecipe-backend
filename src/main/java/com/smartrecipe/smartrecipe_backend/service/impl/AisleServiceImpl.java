package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.response.AisleResponse;
import com.smartrecipe.smartrecipe_backend.entity.Aisle;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.AisleRepository;
import com.smartrecipe.smartrecipe_backend.service.AisleService;
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
public class AisleServiceImpl implements AisleService {

    private final AisleRepository aisleRepository;

    @Override
    @Cacheable("aisles")
    @Transactional(readOnly = true)
    public List<AisleResponse> getAllAisles() {
        return aisleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "aisle", key = "#id")
    @Transactional(readOnly = true)
    public AisleResponse getAisleById(Integer id) {
        Aisle aisle = aisleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + id));
        return mapToResponse(aisle);
    }

    @Override
    @CacheEvict(value = {"aisles", "aisle"}, allEntries = true)
    public AisleResponse createAisle(String name) {
        Aisle aisle = Aisle.builder()
                .name(name)
                .build();
        Aisle saved = aisleRepository.save(aisle);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"aisles", "aisle"}, allEntries = true)
    public AisleResponse updateAisle(Integer id, String name) {
        Aisle aisle = aisleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + id));
        aisle.setName(name);
        Aisle saved = aisleRepository.save(aisle);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"aisles", "aisle"}, allEntries = true)
    public void deleteAisle(Integer id) {
        if (!aisleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy quầy hàng với ID: " + id);
        }
        aisleRepository.deleteById(id);
    }

    private AisleResponse mapToResponse(Aisle aisle) {
        return AisleResponse.builder()
                .id(aisle.getId())
                .name(aisle.getName())
                .build();
    }
}