package com.example.demo.controller;

import com.example.demo.dto.ReviewReportDTO;
import com.example.demo.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviewReports")
public class ReviewReportController {

    @Autowired
    private ReviewReportService reviewReportService;

    @PostMapping("/{reviewId}")
    @Operation(summary = "Report a review", description = "Allows an authenticated user to report a review comment.")
    @ApiResponse(responseCode = "201", description = "Review reported successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewReportDTO> reportReview(@PathVariable Long reviewId,
                                                        @RequestBody(required = false) String reason) {
        ReviewReportDTO reportDTO = reviewReportService.reportReview(reviewId, reason);
        return new ResponseEntity<>(reportDTO, HttpStatus.CREATED);
    }

    @GetMapping("/")
    @Operation(summary = "Get reported reviews", description = "Retrieves all reported review comments.")
    @ApiResponse(responseCode = "200", description = "Reported reviews retrieved successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewReportDTO>> getReportedReviews() {
        List<ReviewReportDTO> reports = reviewReportService.getAllReportedReviews();
        return ResponseEntity.ok(reports);
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete a review report", description = "Allows an admin to delete a reported review.")
    @ApiResponse(responseCode = "204", description = "Review report deleted successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReviewReport(@PathVariable Long reportId) {
        reviewReportService.deleteReviewReport(reportId);
        return ResponseEntity.noContent().build();
    }
}
