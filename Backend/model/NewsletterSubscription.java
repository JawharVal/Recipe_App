package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class NewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String email;

    private LocalDateTime subscribedAt;

    @PrePersist
    protected void onSubscribe() {
        subscribedAt = LocalDateTime.now();
    }

    // Constructors
    public NewsletterSubscription() {}

    public NewsletterSubscription(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
}
