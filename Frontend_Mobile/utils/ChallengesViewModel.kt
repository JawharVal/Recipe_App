// File: ChallengesViewModel.kt
package com.example.recipeapp.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeapp.utils.ChallengeDTO
import com.example.recipeapp.utils.ChallengeRepository
import com.example.recipeapp.utils.UploadImageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChallengesViewModel(private val repository: ChallengeRepository) : ViewModel() {

    private val _challenges = MutableStateFlow<List<ChallengeDTO>>(emptyList())
    val challenges: StateFlow<List<ChallengeDTO>> = _challenges

    init {
        fetchChallenges()
    }

    suspend fun getChallengeById(id: Long): ChallengeDTO {
        return repository.getChallengeById(id)
    }

    fun fetchChallenges() {
        viewModelScope.launch {
            try {
                val data = repository.getAllChallenges()
                _challenges.value = data
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ChallengesViewModel", "Error fetching challenges: ${e.localizedMessage}")
            }
        }
    }

    fun submitRecipe(
        challengeId: Long,
        recipeId: Long,
        onResult: (ChallengeDTO?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedChallenge = repository.submitRecipe(challengeId, recipeId)
                onResult(updatedChallenge)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ChallengesViewModel", "Error submitting recipe: ${e.localizedMessage}")
                onResult(null)
            }
        }
    }

    fun uploadProfileImage(context: Context, imageUri: Uri, onUploadComplete: (String?) -> Unit) {
        viewModelScope.launch {
            UploadImageUtil.uploadImage(
                context,
                imageUri,
                isAvatar = true, // Mark this upload as an avatar update
                onSuccess = { imageUrl ->
                    Log.d("ChallengesViewModel", "Uploaded Image URL: $imageUrl")
                    onUploadComplete(imageUrl)
                },
                onError = { error ->
                    Log.e("ChallengesViewModel", "Upload failed: $error")
                    onUploadComplete(null)
                }
            )
        }
    }


    fun voteChallenge(id: Long, voteValue: Int) {
        viewModelScope.launch {
            try {
                repository.voteChallenge(id, voteValue)
                fetchChallenges()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ChallengesViewModel", "Error voting on challenge: ${e.localizedMessage}")
            }
        }
    }

    fun createChallenge(challenge: ChallengeDTO, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // Create the challenge on the backend.
                val createdChallenge = repository.createChallenge(challenge)
                // Option 1: Refresh the entire list after creation.
                fetchChallenges()
                // Option 2 (optimistic update): Prepend the newly created challenge to the existing list.
                // _challenges.value = listOf(createdChallenge) + _challenges.value
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ChallengesViewModel", "Error creating challenge: ${e.localizedMessage}")
            }
        }
    }
}
