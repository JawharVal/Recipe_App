package com.example.demo.controller;

import com.example.demo.dto.RecipeReportDTO;
import com.example.demo.service.RecipeReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipeReports")
public class RecipeReportController {

    @Autowired
    private RecipeReportService recipeReportService;

    @PostMapping("/{recipeId}")
    @Operation(summary = "Report a recipe", description = "Allows an authenticated user to report a recipe.")
    @ApiResponse(responseCode = "201", description = "Recipe reported successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeReportDTO> reportRecipe(@PathVariable Long recipeId,
                                                        @RequestBody(required = false) String reason) {
        RecipeReportDTO reportDTO = recipeReportService.reportRecipe(recipeId, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(reportDTO);
    }

    @GetMapping("/")
    @Operation(summary = "Get reported recipes", description = "Retrieves all reported recipes.")
    @ApiResponse(responseCode = "200", description = "Reported recipes retrieved successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RecipeReportDTO>> getReportedRecipes() {
        List<RecipeReportDTO> reports = recipeReportService.getAllRecipeReports();
        return ResponseEntity.ok(reports);
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete a recipe report", description = "Allows an admin to delete a reported recipe.")
    @ApiResponse(responseCode = "204", description = "Recipe report deleted successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecipeReport(@PathVariable Long reportId) {
        recipeReportService.deleteRecipeReport(reportId);
        return ResponseEntity.noContent().build();
    }
}
