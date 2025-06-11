package com.example.demo.service;

import com.example.demo.dto.ReviewReportDTO;
import com.example.demo.model.Review;
import com.example.demo.model.ReviewReport;
import com.example.demo.model.User;
import com.example.demo.repositories.ReviewReportRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewReportServiceImpl implements ReviewReportService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private UserRepository userRepository;
    @Override
    public long countReviewReports() {
        return reviewReportRepository.count();
    }
    @Override
    public ReviewReportDTO reportReview(Long reviewId, String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User reporter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        Optional<ReviewReport> existingReport = reviewReportRepository.findByReviewAndReporter(review, reporter);
        if (existingReport.isPresent()) {
            throw new RuntimeException("You have already reported this review.");
        }

        ReviewReport report = new ReviewReport(review, reporter, reason);
        ReviewReport savedReport = reviewReportRepository.save(report);
        return mapToDTO(savedReport);
    }

    @Override
    public List<ReviewReportDTO> getAllReportedReviews() {
        return reviewReportRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReviewReport(Long reportId) {
        reviewReportRepository.deleteById(reportId);
    }

    private ReviewReportDTO mapToDTO(ReviewReport report) {
        Long reviewId = report.getReview() != null ? report.getReview().getId() : null;
        // Here we extract the review's comment
        String reviewComment = report.getReview() != null ? report.getReview().getComment() : "N/A";
        String reporterUsername = report.getReporter() != null ? report.getReporter().getUsername() : "N/A";
        return new ReviewReportDTO(
                report.getId(),
                reviewId,
                reviewComment,
                reporterUsername,
                report.getReason(),
                report.getReportedAt()
        );
    }

}
