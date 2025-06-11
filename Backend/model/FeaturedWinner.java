package com.example.demo.model;

import javax.persistence.*;

@Entity
@Table(name = "featured_winners")
public class FeaturedWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private String username;

    private int totalPoints;

    public FeaturedWinner() {}

    public FeaturedWinner(String userEmail, String username, int totalPoints) {
        this.userEmail = userEmail;
        this.username = username;
        this.totalPoints = totalPoints;
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalPoints() {
        return totalPoints;
    }
    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }
}
