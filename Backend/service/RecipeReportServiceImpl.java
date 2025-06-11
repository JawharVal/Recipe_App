package com.example.demo.service;

import com.example.demo.dto.RecipeReportDTO;
import com.example.demo.model.Recipe;
import com.example.demo.model.RecipeReport;
import com.example.demo.model.User;
import com.example.demo.repositories.RecipeReportRepository;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecipeReportServiceImpl implements RecipeReportService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeReportRepository recipeReportRepository;

    @Override
    public RecipeReportDTO reportRecipe(Long recipeId, String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User reporter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Optional<RecipeReport> existingReport = recipeReportRepository.findByRecipeAndReporter(recipe, reporter);
        if (existingReport.isPresent()) {
            throw new RuntimeException("You have already reported this recipe.");
        }

        RecipeReport report = new RecipeReport(recipe, reporter, reason);
        RecipeReport savedReport = recipeReportRepository.save(report);
        return mapToDTO(savedReport);
    }
    @Override
    public long countRecipeReports() {
        return recipeReportRepository.count();
    }

    @Override
    public List<RecipeReportDTO> getAllRecipeReports() {
        return recipeReportRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRecipeReport(Long reportId) {
        recipeReportRepository.deleteById(reportId);
    }

    private RecipeReportDTO mapToDTO(RecipeReport report) {
        Long recipeId = report.getRecipe() != null ? report.getRecipe().getId() : null;
        String recipeTitle = report.getRecipe() != null ? report.getRecipe().getTitle() : "N/A";
        String reporterUsername = report.getReporter() != null ? report.getReporter().getUsername() : "N/A";
        return new RecipeReportDTO(
                report.getId(),
                recipeId,
                recipeTitle,
                reporterUsername,
                report.getReason(),
                report.getReportedAt()
        );
    }
}
