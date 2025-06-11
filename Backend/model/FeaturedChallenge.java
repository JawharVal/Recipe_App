package com.example.demo.model;

import com.example.demo.model.Challenge;

import javax.persistence.*;

@Entity
@Table(name = "featured_challenges")
public class FeaturedChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "challenge_id", unique = true)
    private Challenge challenge;

    public FeaturedChallenge() {}

    public FeaturedChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Challenge getChallenge() { return challenge; }
    public void setChallenge(Challenge challenge) { this.challenge = challenge; }
}
