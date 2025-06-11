package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class BookDTO {
    private Long id;
    private String title;
    private String description;
    private Long authorId;
    private Set<Long> recipeIds;
    private String color;
    public Long getId() {
        return id;
    }

    private Boolean isPublic;

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BookDTO() {}

    public BookDTO(Long id, String title, String description, Long authorId, Set<Long> recipeIds, String color, Boolean isPublic) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.authorId = authorId;
        this.recipeIds = recipeIds;
        this.color = color;
        this.isPublic = isPublic;
    }
}
