package com.example.demo.service;

import com.example.demo.dto.RecipeReportDTO;
import java.util.List;

public interface RecipeReportService {
    RecipeReportDTO reportRecipe(Long recipeId, String reason);
    List<RecipeReportDTO> getAllRecipeReports();
    void deleteRecipeReport(Long reportId);
    long countRecipeReports();

}
