package com.smartrecipe.smartrecipe_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "grocery_items")
public class GroceryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_list_id", nullable = false)
    private GroceryList groceryList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "total_needed", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalNeeded;

    @Column(name = "pantry_deducted", nullable = false, precision = 10, scale = 2)
    private BigDecimal pantryDeducted;

    @Column(name = "final_to_buy", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalToBuy;

    @Builder.Default
    @Column(name = "is_bought")
    private Boolean isBought = false;
}
