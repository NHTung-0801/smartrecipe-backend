package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.entity.Ingredient;
import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.repository.UnitConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UnitNormalizationService {

    private static final MathContext CONTEXT = MathContext.DECIMAL128;
    private static final int MAX_HOPS = 4;
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("g (gram)", "g"), Map.entry("gram", "g"), Map.entry("grams", "g"),
            Map.entry("kilogram", "kg"), Map.entry("kilograms", "kg"),
            Map.entry("lit", "l"), Map.entry("litre", "l"), Map.entry("liter", "l"), Map.entry("liters", "l"),
            Map.entry("thia cafe (tsp)", "muong ca phe"), Map.entry("tsp", "muong ca phe"),
            Map.entry("thia canh (tbsp)", "muong canh"), Map.entry("tbsp", "muong canh"),
            Map.entry("chen/bat", "chen"), Map.entry("qua/trai", "qua")
    );

    private final UnitConversionRepository conversionRepository;

    /** Quy đổi số lượng sang baseUnit của nguyên liệu; hỗ trợ cạnh đảo và chuỗi quy đổi ngắn. */
    public BigDecimal toBaseUnit(BigDecimal quantity, String inputUnit, Ingredient ingredient) {
        String source = normalize(inputUnit);
        String target = normalize(ingredient.getBaseUnit());
        if (source.equals(target)) {
            return quantity;
        }

        Map<String, List<Edge>> graph = buildGraph(ingredient.getId());
        Deque<State> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new State(source, BigDecimal.ONE, 0));

        while (!queue.isEmpty()) {
            State state = queue.removeFirst();
            if (state.depth > MAX_HOPS || !visited.add(state.unit)) continue;
            if (state.unit.equals(target)) {
                return quantity.multiply(state.multiplier, CONTEXT);
            }
            for (Edge edge : graph.getOrDefault(state.unit, List.of())) {
                queue.addLast(new State(edge.unit,
                        state.multiplier.multiply(edge.multiplier, CONTEXT), state.depth + 1));
            }
        }

        throw new BadRequestException("Không thể quy đổi đơn vị '" + inputUnit + "' sang '"
                + ingredient.getBaseUnit() + "' cho nguyên liệu " + ingredient.getName());
    }

    private Map<String, List<Edge>> buildGraph(Long ingredientId) {
        Map<String, List<Edge>> graph = new HashMap<>();
        for (UnitConversion conversion : conversionRepository.findByIngredientIdOrIngredientIsNull(ingredientId)) {
            String from = normalize(conversion.getFromUnit());
            String to = normalize(conversion.getToUnit());
            graph.computeIfAbsent(from, ignored -> new ArrayList<>())
                    .add(new Edge(to, conversion.getMultiplier()));
            if (conversion.getMultiplier().signum() != 0) {
                graph.computeIfAbsent(to, ignored -> new ArrayList<>())
                        .add(new Edge(from, BigDecimal.ONE.divide(conversion.getMultiplier(), CONTEXT)));
            }
        }
        return graph;
    }

    private String normalize(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new BadRequestException("Đơn vị không được để trống");
        }
        String normalized = Normalizer.normalize(unit.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        return ALIASES.getOrDefault(normalized, normalized);
    }

    private record Edge(String unit, BigDecimal multiplier) {}
    private record State(String unit, BigDecimal multiplier, int depth) {}
}