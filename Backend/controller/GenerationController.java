package com.example.demo.controller;

import com.example.demo.dto.GenerationLimitResponse;
import com.example.demo.model.User;
import com.example.demo.service.SubscriptionLimitException;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class GenerationController {

    @Autowired
    private UserService userService;

    @GetMapping("/generation-limit")
    public ResponseEntity<GenerationLimitResponse> checkGenerationLimit(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);

        LocalDateTime now = LocalDateTime.now();
        if (user.getRecipeGenerationCycleStart() == null || now.isAfter(user.getRecipeGenerationCycleStart().plusMonths(1))) {
            user.setRecipeGenerationCycleStart(now);
            user.setRecipeGenerationCount(0);
            userService.save(user);
        }

        int currentCount = user.getRecipeGenerationCount();
        int limit;
        switch (user.getSubscriptionType()) {
            case FREE:
                limit = 3;
                break;
            case PLUS:
                limit = 10;
                break;
            case PRO:
                limit = Integer.MAX_VALUE;
                break;
            default:
                limit = 3;
        }

        boolean allowed = currentCount < limit;
        int remaining = (limit == Integer.MAX_VALUE) ? -1 : limit - currentCount;

        GenerationLimitResponse response = new GenerationLimitResponse(allowed, remaining, currentCount, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generation-event")
    public ResponseEntity<?> recordGenerationEvent(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);

        LocalDateTime now = LocalDateTime.now();

        if (user.getRecipeGenerationCycleStart() == null || now.isAfter(user.getRecipeGenerationCycleStart().plusMonths(1))) {
            user.setRecipeGenerationCycleStart(now);
            user.setRecipeGenerationCount(0);
        }

        int currentCount = user.getRecipeGenerationCount();
        int limit;
        switch (user.getSubscriptionType()) {
            case FREE:
                limit = 3;
                break;
            case PLUS:
                limit = 10;
                break;
            case PRO:
                limit = Integer.MAX_VALUE;
                break;
            default:
                limit = 3;
        }

        if (currentCount >= limit) {
            throw new SubscriptionLimitException("Monthly recipe generation limit reached for your subscription tier.");
        }

        user.setRecipeGenerationCount(currentCount + 1);
        userService.save(user);

        return ResponseEntity.ok().build();
    }
}
