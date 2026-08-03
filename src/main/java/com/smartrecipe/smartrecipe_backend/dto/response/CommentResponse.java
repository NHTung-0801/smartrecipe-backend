package com.smartrecipe.smartrecipe_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private AuthorSummaryResponse author;
    private Long parentId;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}