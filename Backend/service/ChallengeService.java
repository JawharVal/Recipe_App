package com.example.demo.service;

import com.example.demo.dto.ChallengeDTO;
import com.example.demo.model.FeaturedWinner;
import com.example.demo.model.GlobalLeaderboardEntry;
import com.example.demo.model.Recipe;

import java.util.List;

public interface ChallengeService {
    List<ChallengeDTO> getAllChallenges();
    ChallengeDTO getChallengeById(Long id);
    ChallengeDTO createChallenge(ChallengeDTO challengeDTO);
    ChallengeDTO updateChallenge(Long id, ChallengeDTO challengeDTO);
    void deleteChallenge(Long id);
    List<GlobalLeaderboardEntry> getGlobalLeaderboard();
    boolean isRecipeAlreadySubmitted(Long recipeId);
    ChallengeDTO submitRecipe(Long challengeId, Long recipeId, String userEmail);

    List<Recipe> getSubmittedRecipes(Long challengeId);

    List<FeaturedWinner> getFeaturedWinners();
    void voteChallenge(Long id, int voteValue);
    // ✅ Add this method declaration
    void recalculateLeaderboard();
    List<ChallengeDTO> getFeaturedChallenges();
    ChallengeDTO featureChallenge(Long challengeId);
    void unfeatureChallenge(Long challengeId);
}