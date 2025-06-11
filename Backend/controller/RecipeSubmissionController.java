package com.example.demo.controller;

import com.example.demo.model.RecipeSubmission;
import com.example.demo.model.Challenge;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.RecipeSubmissionRepository;
import com.example.demo.repositories.ChallengeRepository;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/challenges/recipe-submissions")
public class RecipeSubmissionController {

    @Autowired
    private RecipeSubmissionRepository recipeSubmissionRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSubmissions() {
        List<RecipeSubmission> submissions = recipeSubmissionRepository.findAll();

        List<Map<String, Object>> submissionList = submissions.stream().map(submission -> {
            Map<String, Object> submissionMap = new HashMap<>();
            submissionMap.put("id", submission.getId());
            submissionMap.put("submissionDate", submission.getSubmissionDate().toString());

            if (submission.getChallenge() != null) {
                submissionMap.put("challenge", Map.of("id", submission.getChallenge().getId(), "title", submission.getChallenge().getTitle()));
            } else {
                submissionMap.put("challenge", null);
            }

            if (submission.getRecipe() != null) {
                submissionMap.put("recipe", Map.of("id", submission.getRecipe().getId(), "name", submission.getRecipe().getTitle()));
            } else {
                submissionMap.put("recipe", null);
            }

            if (submission.getUser() != null) {
                submissionMap.put("user", Map.of("id", submission.getUser().getId(), "username", submission.getUser().getUsername(), "email", submission.getUser().getEmail()));
            } else {
                submissionMap.put("user", null);
            }

            return submissionMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(submissionList);
    }

    @PostMapping
    public ResponseEntity<?> createSubmission(@RequestBody RecipeSubmission submission) {
        Optional<Challenge> challengeOpt = challengeRepository.findById(submission.getChallenge().getId());
        Optional<Recipe> recipeOpt = recipeRepository.findById(submission.getRecipe().getId());
        Optional<User> userOpt = userRepository.findById(submission.getUser().getId());

        if (!challengeOpt.isPresent() || !recipeOpt.isPresent() || !userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Invalid Challenge, Recipe, or User ID");
        }

        submission.setSubmissionDate(LocalDate.now());
        RecipeSubmission savedSubmission = recipeSubmissionRepository.save(submission);
        return ResponseEntity.ok(savedSubmission);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubmission(@PathVariable Long id, @RequestBody RecipeSubmission updatedSubmission) {
        Optional<RecipeSubmission> existingSubmissionOpt = recipeSubmissionRepository.findById(id);
        if (!existingSubmissionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        RecipeSubmission submission = existingSubmissionOpt.get();
        submission.setChallenge(updatedSubmission.getChallenge());
        submission.setRecipe(updatedSubmission.getRecipe());
        submission.setUser(updatedSubmission.getUser());
        submission.setSubmissionDate(updatedSubmission.getSubmissionDate());

        recipeSubmissionRepository.save(submission);
        return ResponseEntity.ok(submission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubmission(@PathVariable Long id) {
        if (!recipeSubmissionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        recipeSubmissionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
