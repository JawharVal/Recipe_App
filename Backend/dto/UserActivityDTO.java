package com.example.demo.dto;

public class UserActivityDTO {
    private Long userId;
    private String username;
    private long followerCount;
    private long publicRecipeCount;

    public UserActivityDTO(Long userId, String username, long followerCount, long publicRecipeCount) {
        this.userId = userId;
        this.username = username;
        this.followerCount = followerCount;
        this.publicRecipeCount = publicRecipeCount;
    }

    public UserActivityDTO() {}


    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getFollowerCount() {
        return followerCount;
    }

    public Long getPublicRecipeCount() {
        return publicRecipeCount;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }

    public void setPublicRecipeCount(Long publicRecipeCount) {
        this.publicRecipeCount = publicRecipeCount;
    }
}
