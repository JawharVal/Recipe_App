package com.example.recipeapp.utils

import com.example.recipeapp.utils.ChallengeDTO

class ChallengeRepository(private val challengeApi: ChallengeApi) {

    suspend fun getAllChallenges(): List<ChallengeDTO> {
        return challengeApi.getAllChallenges()
    }

    suspend fun voteChallenge(id: Long, voteValue: Int) {
        challengeApi.voteChallenge(id, voteValue)
    }

    suspend fun submitRecipe(challengeId: Long, recipeId: Long): ChallengeDTO {
        return challengeApi.submitRecipe(challengeId, recipeId)
    }

    // Create a new challenge
    suspend fun createChallenge(challengeDTO: ChallengeDTO): ChallengeDTO {
        return challengeApi.createChallenge(challengeDTO)
    }

    // Get challenge by ID
    suspend fun getChallengeById(id: Long): ChallengeDTO {
        return challengeApi.getChallengeById(id)
    }

    // Update an existing challenge
    suspend fun updateChallenge(id: Long, challengeDTO: ChallengeDTO): ChallengeDTO {
        return challengeApi.updateChallenge(id, challengeDTO)
    }

    // Delete a challenge
    suspend fun deleteChallenge(id: Long) {
        challengeApi.deleteChallenge(id)
    }

    // Get leaderboard data (sorted list of challenges by points)
    suspend fun getLeaderboard(): List<ChallengeDTO> {
        return challengeApi.getLeaderboard()
    }
}
