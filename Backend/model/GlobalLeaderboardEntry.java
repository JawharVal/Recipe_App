package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "global_leaderboard")
public class GlobalLeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private String username;

    private int totalPoints;

    @Transient
    private LocalDate earliestSubmission;
    public GlobalLeaderboardEntry() {
    }

    public GlobalLeaderboardEntry(String userEmail, String username, int totalPoints) {
        this.userEmail = userEmail;
        this.username = username;
        this.totalPoints = totalPoints;
    }

    public GlobalLeaderboardEntry(String userEmail, String username, int totalPoints, LocalDate earliestSubmission) {
        this.userEmail = userEmail;
        this.username = username;
        this.totalPoints = totalPoints;
        this.earliestSubmission = earliestSubmission;
    }

    public LocalDate getEarliestSubmission() {
        return earliestSubmission;
    }

    public void setEarliestSubmission(LocalDate earliestSubmission) {
        this.earliestSubmission = earliestSubmission;
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
}
