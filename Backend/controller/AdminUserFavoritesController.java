package com.example.demo.controller;

import com.example.demo.dto.FavoriteRecipeDTO;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserFavoritesController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}/favorites")
    @Operation(summary = "Get user favorites", description = "Retrieves all favorite recipes for a specific user.")
    @ApiResponse(responseCode = "200", description = "Favorites retrieved successfully")
    public ResponseEntity<List<FavoriteRecipeDTO>> getUserFavorites(@PathVariable Long userId) {
        List<FavoriteRecipeDTO> favorites = userService.getFavoriteRecipesByUserId(userId);
        return ResponseEntity.ok(favorites);
    }

    @DeleteMapping("/{userId}/favorites/{recipeId}")
    @Operation(summary = "Remove user favorite", description = "Removes a recipe from the specified user's favorites.")
    @ApiResponse(responseCode = "204", description = "Favorite removed successfully")
    public ResponseEntity<Void> removeUserFavorite(@PathVariable Long userId, @PathVariable Long recipeId) {
        userService.removeFavoriteRecipeForUser(userId, recipeId);
        return ResponseEntity.noContent().build();
    }
}
