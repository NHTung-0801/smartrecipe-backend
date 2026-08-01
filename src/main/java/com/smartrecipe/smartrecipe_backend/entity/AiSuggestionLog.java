package com.smartrecipe.smartrecipe_backend.entity;

import com.smartrecipe.smartrecipe_backend.enums.AiSuggestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_suggestion_logs")
public class AiSuggestionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSuggestionType type;

    @Column(name = "input_ingredients", columnDefinition = "JSON", nullable = false)
    private String inputIngredients;

    @Column(name = "output_response", columnDefinition = "JSON", nullable = false)
    private String outputResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_recipe_id")
    private Recipe savedRecipe;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
