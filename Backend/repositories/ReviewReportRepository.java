// ReviewReportRepository.java
package com.example.demo.repositories;

import com.example.demo.model.Review;
import com.example.demo.model.ReviewReport;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    List<ReviewReport> findByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);

    Optional<ReviewReport> findByReviewAndReporter(Review review, User reporter);
}
