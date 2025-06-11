package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDTO {
    private long totalUsers;
    private long totalRecipes;
    private long totalLikes;
    private long totalComments;
    private long totalRecipeReports;
    private long totalReviewReports;
}
