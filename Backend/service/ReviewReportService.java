package com.example.demo.service;

import com.example.demo.dto.ReviewReportDTO;
import java.util.List;

public interface ReviewReportService {
    ReviewReportDTO reportReview(Long reviewId, String reason);
    List<ReviewReportDTO> getAllReportedReviews();
    void deleteReviewReport(Long reportId);
    long countReviewReports();
}
