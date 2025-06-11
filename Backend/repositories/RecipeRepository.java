package com.example.demo.repositories;


import com.example.demo.dto.UserActivityDTO;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByTitleContainingIgnoreCase(String title);
    List<Recipe> findByTagsContaining(String tag);
    List<Recipe> findByIsPublicTrue();

    List<Recipe> findByAuthorId(Long authorId);
    List<Recipe> findByAuthor(User author);
    long countByAuthor(User author);
    @Query("SELECT COALESCE(SUM(r.likes), 0) FROM Recipe r")
    long sumAllLikes();
    @Query("SELECT r FROM Recipe r JOIN r.likedBy u WHERE u.id = :userId")
    List<Recipe> findRecipesLikedByUser(@Param("userId") Long userId);
    @Query("SELECT new com.example.demo.dto.UserActivityDTO(" +
            "u.id, u.username, CAST(SIZE(u.followers) AS long), CAST(COUNT(r) AS long)) " +
            "FROM User u LEFT JOIN Recipe r ON r.author = u AND r.isPublic = true " +
            "GROUP BY u.id, u.username " +
            "ORDER BY SIZE(u.followers) DESC")
    List<UserActivityDTO> findTopFollowedUsers();






}
