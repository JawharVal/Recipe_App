package com.example.demo.repositories;

import com.example.demo.model.NewsletterSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {
    Optional<NewsletterSubscription> findByEmail(String email);
}
