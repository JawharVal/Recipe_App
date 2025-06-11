package com.example.demo.controller;

import com.example.demo.dto.BadgeRequest;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserBadgesController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}/badges")
    @Operation(summary = "Get user badges", description = "Retrieves all badges for a specific user.")
    @ApiResponse(responseCode = "200", description = "Badges retrieved successfully")
    public ResponseEntity<Map<String, Integer>> getUserBadges(@PathVariable Long userId) {

        return ResponseEntity.ok(userService.getUserEntityById(userId).getBadges());
    }

    @PostMapping("/{userId}/badges")
    @Operation(summary = "Add or update a badge", description = "Adds or updates a badge for the specified user.")
    @ApiResponse(responseCode = "200", description = "Badge added/updated successfully")
    public ResponseEntity<String> addOrUpdateBadge(@PathVariable Long userId, @RequestBody BadgeRequest badgeRequest) {
        userService.addOrUpdateBadge(userId, badgeRequest.getBadge(), badgeRequest.getCount());
        return ResponseEntity.ok("Badge added/updated successfully");
    }

    @DeleteMapping("/{userId}/badges/{badge}")
    @Operation(summary = "Delete a badge", description = "Deletes the specified badge from the user.")
    @ApiResponse(responseCode = "204", description = "Badge deleted successfully")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long userId, @PathVariable String badge) {
        userService.deleteBadge(userId, badge);
        return ResponseEntity.noContent().build();
    }
}
