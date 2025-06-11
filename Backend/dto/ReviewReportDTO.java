package com.example.demo.dto;

import java.time.LocalDateTime;

public class ReviewReportDTO {
    private Long id;
    private Long reviewId;
    private String reviewComment;
    private String reporterUsername;
    private String reason;
    private LocalDateTime reportedAt;

    public ReviewReportDTO() {}

    public ReviewReportDTO(Long id, Long reviewId, String reviewComment, String reporterUsername, String reason, LocalDateTime reportedAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.reviewComment = reviewComment;
        this.reporterUsername = reporterUsername;
        this.reason = reason;
        this.reportedAt = reportedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
