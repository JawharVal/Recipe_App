package com.example.demo.dto;

public class FavoriteRecipeDTO {
    private Long id;
    private String title;
    private String imageUri;
    private Long authorId;

    public FavoriteRecipeDTO() {}

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public FavoriteRecipeDTO(Long id, String title, String imageUri, Long authorId) {
        this.id = id;
        this.title = title;
        this.imageUri = imageUri;
        this.authorId = authorId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
