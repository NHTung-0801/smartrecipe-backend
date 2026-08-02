package com.smartrecipe.smartrecipe_backend.repository;

import com.smartrecipe.smartrecipe_backend.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Integer> {

    List<UnitConversion> findByIngredientId(Long ingredientId);

    List<UnitConversion> findByIngredientIsNull();

    Optional<UnitConversion> findByFromUnitAndToUnitAndIngredientId(String fromUnit, String toUnit, Long ingredientId);

    Optional<UnitConversion> findByFromUnitAndToUnitAndIngredientIsNull(String fromUnit, String toUnit);
}