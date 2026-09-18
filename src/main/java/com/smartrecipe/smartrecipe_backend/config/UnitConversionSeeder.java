package com.smartrecipe.smartrecipe_backend.config;

import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import com.smartrecipe.smartrecipe_backend.repository.UnitConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds global unit conversion rules into the database on startup.
 * These rules apply to ALL ingredients (ingredient_id = NULL).
 * Rules are idempotent — only inserted if they don't already exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnitConversionSeeder implements ApplicationRunner {

    private final UnitConversionRepository unitConversionRepository;

    /**
     * Global conversion table (ingredient-agnostic, approximate values for nutrition calculation):
     *
     *  Khối lượng:
     *    kg → g : x1000
     *    g  → g : x1 (identity — handled by normalizer, no DB entry needed)
     *
     *  Thể tích (density ≈ 1 g/ml for liquids, used as best-effort for cooking):
     *    ml → g  : x1
     *    l  → g  : x1000
     *
     *  Thìa / Chén (chuẩn quốc tế):
     *    muong ca phe → g : x5   (1 tsp = ~5 ml ≈ 5 g)
     *    muong canh   → g : x15  (1 tbsp = ~15 ml ≈ 15 g)
     *    chen         → g : x240 (1 cup = ~240 ml ≈ 240 g)
     *
     *  Đơn vị đếm (giá trị trung bình phổ biến — dùng cho ước tính calo):
     *    qua  → g : x100  (quả/trái trung bình ~100g, ví dụ: táo, cam)
     *    cu   → g : x80   (củ hành, tỏi ~80g)
     *    tep  → g : x5    (tép tỏi ~5g)
     *    bo   → g : x150  (bó rau ~150g)
     *    con  → g : x200  (con tôm, cá trung bình ~200g)
     *    lat  → g : x10   (lát thịt/dưa ~10g)
     */
    private static final List<GlobalRule> GLOBAL_RULES = List.of(
            // === Khối lượng ===
            new GlobalRule("kg",           "g",    new BigDecimal("1000")),

            // === Thể tích (ml <-> g: density ≈ 1 cho liquid ước tính) ===
            new GlobalRule("ml",           "g",    BigDecimal.ONE),
            new GlobalRule("l",            "g",    new BigDecimal("1000")),
            new GlobalRule("l",            "ml",   new BigDecimal("1000")),

            // === Thìa / Chén → g (cho solid, ví dụ: muối, đường, bột) ===
            new GlobalRule("muong ca phe", "g",    new BigDecimal("5")),
            new GlobalRule("muong canh",   "g",    new BigDecimal("15")),
            new GlobalRule("chen",         "g",    new BigDecimal("240")),

            // === Thìa / Chén → ml (cho liquid, ví dụ: nước mắm, dầu ăn) ===
            // BFS sẽ dùng rule này khi ingredient.baseUnit = 'ml'
            new GlobalRule("muong ca phe", "ml",   new BigDecimal("5")),
            new GlobalRule("muong canh",   "ml",   new BigDecimal("15")),
            new GlobalRule("chen",         "ml",   new BigDecimal("240")),

            // === Đơn vị đếm → g (ước tính trung bình) ===
            new GlobalRule("qua",          "g",    new BigDecimal("100")),
            new GlobalRule("cu",           "g",    new BigDecimal("80")),
            new GlobalRule("tep",          "g",    new BigDecimal("5")),
            new GlobalRule("bo",           "g",    new BigDecimal("150")),
            new GlobalRule("con",          "g",    new BigDecimal("200")),
            new GlobalRule("lat",          "g",    new BigDecimal("10"))
    );

    @Override
    public void run(ApplicationArguments args) {
        int inserted = 0;
        for (GlobalRule rule : GLOBAL_RULES) {
            boolean exists = unitConversionRepository
                    .existsByFromUnitAndToUnitAndIngredientIsNull(rule.from(), rule.to());
            if (!exists) {
                unitConversionRepository.save(
                        UnitConversion.builder()
                                .fromUnit(rule.from())
                                .toUnit(rule.to())
                                .multiplier(rule.multiplier())
                                .ingredient(null)
                                .build()
                );
                inserted++;
                log.info("[UnitConversionSeeder] Inserted global rule: {} → {} × {}", rule.from(), rule.to(), rule.multiplier());
            }
        }
        if (inserted > 0) {
            log.info("[UnitConversionSeeder] Seeded {} new global unit conversion rule(s).", inserted);
        } else {
            log.debug("[UnitConversionSeeder] All global unit conversion rules already exist. Skipping.");
        }
    }

    private record GlobalRule(String from, String to, BigDecimal multiplier) {}
}
