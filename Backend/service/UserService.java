package com.example.demo.service;


import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.FavoriteRecipeDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.SubscriptionRequest;
import com.example.demo.model.User;

import java.util.List;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    UserDTO getUserById(Long id);
    List<UserDTO> getAllUsers();
    UserDTO registerUser(UserDTO userDTO);
    List<FavoriteRecipeDTO> getFavoriteRecipesByUserId(Long userId);
    void removeFavoriteRecipeForUser(Long userId, Long recipeId);

    // Add this method
    void updateUserRole(Long userId, String newRole);
    UserDTO loginUser(String email, String password);
    AuthResponseDTO loginWithGoogle(String idToken);
    UserDTO getUserByEmail(String email);
    void resendOtp(String email);
    long countUsers();
    User getUserEntityByEmail(String email);
    User getUserEntityById(Long id);
    UserDTO updateUserByEmail(String email, UserDTO userDTO);
    UserDTO getUserByName(String name);
    void addFavoriteRecipe(String userEmail, Long recipeId);
    void removeFavoriteRecipe(String userEmail, Long recipeId);
    List<FavoriteRecipeDTO> getFavoriteRecipes(String userEmail);
    void updateSubscription(String userEmail, String subscriptionType, int durationMonths);
    void awardBadgeToUser(String userEmail, String badge);

    void requestPasswordResetMultiStep(String email);
    void verifyResetCode(String email, String code);
    void updatePasswordAfterVerification(String email, String newPassword);
    User save(User user);
    void followUser(String currentUserEmail, Long userIdToFollow);
    void unfollowUser(String currentUserEmail, Long userIdToUnfollow);
    void addOrUpdateBadge(Long userId, String badge, int count);
    void deleteBadge(Long userId, String badge);
    void verifyOtp(String email, String otpCode);

}