package com.example.demo.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDTO {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String email;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private String stripeCustomerId;
    @Getter
    @Setter
    private int recipeGenerationCount;
    @Getter
    @Setter
    private LocalDateTime recipeGenerationCycleStart;
    @Getter
    @Setter
    private String resetToken;
    @Getter
    @Setter
    private LocalDateTime resetTokenExpiry;
    @Getter
    @Setter
    private boolean resetTokenVerified;
    @Getter
    @Setter
    private String role;
    @Getter @Setter
    private String subscriptionType;
    @Getter @Setter
    private List<Long> followerIds;
    @Getter @Setter
    private Map<String, Integer> badges = new HashMap<>();
    @Getter @Setter
    private List<Long> followingIds;
    @Getter @Setter
    private LocalDateTime subscriptionExpiry;
    @Getter @Setter
    private int followerCount;
    @Getter @Setter
    private boolean isFollowed;
    @Getter @Setter
    private int followingCount;

    @Getter @Setter
    private String imageUri;
    private boolean isVerified;

    public boolean isVerified() {
        return isVerified;
    }
    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public UserDTO(Long id, String email, String username, String password, String role,
                   String subscriptionType, LocalDateTime subscriptionExpiry,
                   int followerCount, int followingCount) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.subscriptionType = subscriptionType;
        this.subscriptionExpiry = subscriptionExpiry;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
    }
}

