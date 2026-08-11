package com.smartrecipe.smartrecipe_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_pantry", indexes = {
        @Index(name = "idx_pantry_user_ingredient_expiry", columnList = "user_id,ingredient_id,expiry_date")
})
public class UserPantry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /** Số lượng của lot, luôn được lưu theo Ingredient.baseUnit. */
    @Column(name = "quantity_available", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityAvailable;

    /** Ngưỡng theo base unit; giữ trên lot trong giai đoạn tương thích schema. */
    @Column(name = "low_stock_threshold", precision = 14, scale = 4)
    private BigDecimal lowStockThreshold;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
