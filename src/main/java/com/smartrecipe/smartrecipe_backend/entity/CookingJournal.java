package com.smartrecipe.smartrecipe_backend.entity;

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
@Table(name = "cooking_journals")
public class CookingJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @CreationTimestamp
    @Column(name = "cooked_at", updatable = false)
    private LocalDateTime cookedAt;

    @Column(name = "iteration_notes", columnDefinition = "TEXT")
    private String iterationNotes;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "actual_servings", nullable = false)
    private Integer actualServings;

    private Integer rating;
}
