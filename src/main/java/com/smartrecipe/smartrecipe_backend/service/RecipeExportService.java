package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeIngredient;
import com.smartrecipe.smartrecipe_backend.entity.RecipeStep;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeExportService {

    private final RecipeRepository recipeRepository;

    public byte[] exportRecipeToWord(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công thức"));

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(recipe.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setColor("D94833"); // Smart Recipe Red

            // Author & Time
            XWPFParagraph subTitlePara = document.createParagraph();
            subTitlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subTitleRun = subTitlePara.createRun();
            subTitleRun.setText("Tác giả: " + (recipe.getAuthor() != null ? recipe.getAuthor().getDisplayName() : "Ẩn danh"));
            subTitleRun.addBreak();
            subTitleRun.setText("Chuẩn bị: " + recipe.getPrepTime() + " phút | Nấu: " + recipe.getCookTime() + " phút");
            subTitleRun.setItalic(true);
            subTitleRun.setFontSize(12);

            // Description
            if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
                XWPFParagraph descPara = document.createParagraph();
                XWPFRun descRun = descPara.createRun();
                descRun.setText(recipe.getDescription());
                descRun.setFontSize(12);
                document.createParagraph().createRun().addBreak();
            }

            // Ingredients
            XWPFParagraph ingTitlePara = document.createParagraph();
            XWPFRun ingTitleRun = ingTitlePara.createRun();
            ingTitleRun.setText("Nguyên liệu (" + recipe.getBaseServings() + " phần):");
            ingTitleRun.setBold(true);
            ingTitleRun.setFontSize(16);

            List<RecipeIngredient> ingredients = recipe.getIngredients();
            if (ingredients != null) {
                for (RecipeIngredient ri : ingredients) {
                    XWPFParagraph p = document.createParagraph();
                    XWPFRun r = p.createRun();
                    String ingredientText = "- " + ri.getIngredient().getName();
                    if (ri.getAmount() != null) {
                        ingredientText += ": " + ri.getAmount();
                    }
                    if (ri.getUnit() != null) {
                        ingredientText += " " + ri.getUnit();
                    }
                    r.setText(ingredientText);
                }
            }

            document.createParagraph().createRun().addBreak();

            // Steps
            XWPFParagraph stepTitlePara = document.createParagraph();
            XWPFRun stepTitleRun = stepTitlePara.createRun();
            stepTitleRun.setText("Cách làm:");
            stepTitleRun.setBold(true);
            stepTitleRun.setFontSize(16);

            List<RecipeStep> steps = recipe.getSteps();
            if (steps != null) {
                for (int i = 0; i < steps.size(); i++) {
                    RecipeStep step = steps.get(i);
                    XWPFParagraph p = document.createParagraph();
                    XWPFRun r = p.createRun();
                    r.setBold(true);
                    r.setText("Bước " + (i + 1) + ":");
                    r.addBreak();
                    XWPFRun contentRun = p.createRun();
                    contentRun.setText(step.getInstruction());
                    p.setSpacingAfter(200);
                }
            }

            document.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Word", e);
        }
    }
}
