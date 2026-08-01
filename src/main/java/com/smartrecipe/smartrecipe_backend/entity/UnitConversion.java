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
@Table(name = "unit_conversions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_unit", "to_unit", "ingredient_id"})
})
public class UnitConversion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "from_unit", nullable = false, length = 20)
    private String fromUnit;

    @Column(name = "to_unit", nullable = false, length = 20)
    private String toUnit;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal multiplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
}
