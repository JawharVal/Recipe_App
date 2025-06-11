package com.example.demo.controller;

import com.example.demo.dto.ChallengeDTO;
import com.example.demo.dto.RecipeDTO;
import com.example.demo.model.FeaturedWinner;
import com.example.demo.model.GlobalLeaderboardEntry;
import com.example.demo.model.Recipe;
import com.example.demo.service.ChallengeService;
import com.example.demo.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;
    @Autowired
    private RecipeService recipeService;

    @GetMapping
    @Operation(summary = "Get all challenges", description = "Retrieves all cooking challenges")
    public ResponseEntity<List<ChallengeDTO>> getAllChallenges() {
        List<ChallengeDTO> challenges = challengeService.getAllChallenges();
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}/submitted")
    @Operation(summary = "Get submitted recipes", description = "Retrieves all submitted recipes for a challenge")
    public ResponseEntity<List<RecipeDTO>> getSubmittedRecipes(@PathVariable Long id) {
        System.out.println("Fetching submitted recipes for Challenge ID: " + id);

        List<RecipeDTO> submittedRecipes = challengeService.getSubmittedRecipes(id)
                .stream()
                .map(recipeService::mapEntityToDTO)
                .collect(Collectors.toList());

        System.out.println("Found " + submittedRecipes.size() + " submitted recipes for Challenge ID: " + id);

        return ResponseEntity.ok(submittedRecipes);
    }

    @PostMapping("/{challengeId}/submit")
    @Operation(summary = "Submit a recipe to a challenge")
    public ResponseEntity<?> submitRecipe(
            @PathVariable Long challengeId,
            @RequestParam Long recipeId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        System.out.println("Submitting Recipe ID: " + recipeId + " to Challenge ID: " + challengeId + " by User: " + userEmail);

        try {
            ChallengeDTO updatedChallenge = challengeService.submitRecipe(challengeId, recipeId, userEmail);
            return ResponseEntity.ok(updatedChallenge);
        } catch (IllegalStateException e) {
            System.out.println("Submission error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get challenge by ID", description = "Retrieves a cooking challenge by its ID")
    public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable Long id) {
        ChallengeDTO challengeDTO = challengeService.getChallengeById(id);
        return ResponseEntity.ok(challengeDTO);
    }

    @PostMapping
    @Operation(summary = "Create a challenge", description = "Creates a new cooking challenge")
    public ResponseEntity<ChallengeDTO> createChallenge(@RequestBody ChallengeDTO challengeDTO) {
        ChallengeDTO createdChallenge = challengeService.createChallenge(challengeDTO);
        return new ResponseEntity<>(createdChallenge, HttpStatus.CREATED);
    }
    @GetMapping("/leaderboard/global")
    public ResponseEntity<List<GlobalLeaderboardEntry>> getGlobalLeaderboard() {
        List<GlobalLeaderboardEntry> leaderboard = challengeService.getGlobalLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Vote on a challenge", description = "Casts a vote for the challenge by updating its points")
    public ResponseEntity<Void> voteChallenge(@PathVariable Long id, @RequestParam int voteValue) {
        challengeService.voteChallenge(id, voteValue);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/featuredWinners")
    public ResponseEntity<List<FeaturedWinner>> getFeaturedWinners() {
        List<FeaturedWinner> winners = challengeService.getFeaturedWinners();
        return ResponseEntity.ok(winners);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<ChallengeDTO>> getLeaderboard() {
        List<ChallengeDTO> leaderboard = challengeService.getAllChallenges()
                .stream()
                .sorted(Comparator.comparingInt(ChallengeDTO::getPoints).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(leaderboard);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update challenge", description = "Updates an existing cooking challenge by its ID")
    public ResponseEntity<ChallengeDTO> updateChallenge(@PathVariable Long id, @RequestBody ChallengeDTO challengeDTO) {
        ChallengeDTO updatedChallenge = challengeService.updateChallenge(id, challengeDTO);
        return ResponseEntity.ok(updatedChallenge);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete challenge", description = "Deletes a cooking challenge by its ID")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ChallengeDTO>> getFeaturedChallenges() {
        return ResponseEntity.ok(challengeService.getFeaturedChallenges());
    }

    @PostMapping("/{id}/feature")
    public ResponseEntity<ChallengeDTO> featureChallenge(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.featureChallenge(id));
    }
    @GetMapping("/featured/names")
    public ResponseEntity<List<String>> getFeaturedChallengeNames() {
        List<String> featuredNames = challengeService.getFeaturedChallenges()
                .stream()
                .map(ChallengeDTO::getTitle)
                .collect(Collectors.toList());
        return ResponseEntity.ok(featuredNames);
    }

    @DeleteMapping("/{id}/unfeature")
    public ResponseEntity<Void> unfeatureChallenge(@PathVariable Long id) {
        challengeService.unfeatureChallenge(id);
        return ResponseEntity.noContent().build();
    }

}
