package com.example.demo.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Email
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    @Getter
    @Setter
    private String email;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    private String stripeCustomerId;
    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_badges", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "badge")
    @Column(name = "count")
    private Map<String, Integer> badges = new HashMap<>();


    public Map<String, Integer> getBadges() {
        return badges;
    }

    public void setBadges(Map<String, Integer> badges) {
        this.badges = badges;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ShoppingListItem> getShoppingListItems() {
        return shoppingListItems;
    }

    public void setShoppingListItems(List<ShoppingListItem> shoppingListItems) {
        this.shoppingListItems = shoppingListItems;
    }

    @NotBlank
    @Size(min = 8, max = 100)
    @Column(nullable = false)
    @Getter @Setter
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    @Getter @Setter
    private String username;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    @Getter @Setter
    private String role;
    @ManyToMany
    @JoinTable(
            name = "user_following",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "followed_id")
    )
    @JsonIdentityReference(alwaysAsId = true)
    private Set<User> following = new HashSet<>();


    @ManyToMany(mappedBy = "following")
    @JsonIdentityReference(alwaysAsId = true)
    private Set<User> followers = new HashSet<>();


    public Set<User> getFollowing() {
        return following;
    }
    public void setFollowing(Set<User> following) {
        this.following = following;
    }

    public Set<User> getFollowers() {
        return followers;
    }
    public void setFollowers(Set<User> followers) {
        this.followers = followers;
    }

    @ManyToMany
    @JoinTable(
            name = "user_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    @Getter
    @Setter
    @JsonIgnore
    private Set<Recipe> favoriteRecipes = new HashSet<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();


    public List<Review> getReviews() {
        return reviews;
    }
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private SubscriptionType subscriptionType = SubscriptionType.FREE;

    @Column(name = "image_uri")
    private String imageUri;

    public String getImageUri() {
        return imageUri;
    }

    public Set<Recipe> getFavoriteRecipes() {
        return favoriteRecipes;
    }

    public void setFavoriteRecipes(Set<Recipe> favoriteRecipes) {
        this.favoriteRecipes = favoriteRecipes;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;



    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public LocalDateTime getSubscriptionExpiry() {
        return subscriptionExpiry;
    }

    public void setSubscriptionExpiry(LocalDateTime subscriptionExpiry) {
        this.subscriptionExpiry = subscriptionExpiry;
    }

    @Column(name = "recipe_generation_count")
    private int recipeGenerationCount = 0;

    @Column(name = "recipe_generation_cycle_start")
    private LocalDateTime recipeGenerationCycleStart = LocalDateTime.now();


    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "reset_token_verified", nullable = false, columnDefinition = "TINYINT(1) default 0")
    private boolean resetTokenVerified = false;



    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public boolean isResetTokenVerified() { return resetTokenVerified; }
    public void setResetTokenVerified(boolean resetTokenVerified) { this.resetTokenVerified = resetTokenVerified; }

    // Getters and setters:
    public int getRecipeGenerationCount() {
        return recipeGenerationCount;
    }

    public void setRecipeGenerationCount(int recipeGenerationCount) {
        this.recipeGenerationCount = recipeGenerationCount;
    }

    public LocalDateTime getRecipeGenerationCycleStart() {
        return recipeGenerationCycleStart;
    }

    public void setRecipeGenerationCycleStart(LocalDateTime recipeGenerationCycleStart) {
        this.recipeGenerationCycleStart = recipeGenerationCycleStart;
    }

    public enum SubscriptionType {
        FREE,
        PLUS,
        PRO
    }
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    @Column(name = "is_verified", nullable = false, columnDefinition = "TINYINT(1) default 0")
    private boolean isVerified = false;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

}
