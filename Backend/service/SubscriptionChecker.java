package com.example.demo.service;


import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionChecker {
    private final UserRepository userRepository;

    public SubscriptionChecker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void checkSubscription(User user) {
        if (user.getSubscriptionType() == User.SubscriptionType.FREE) {
            return; // Free tier has no expiry
        }

        if (LocalDateTime.now().isAfter(user.getSubscriptionExpiry())) {
            user.setSubscriptionType(User.SubscriptionType.FREE);
            userRepository.save(user);
        }
    }
}