package com.example.demo.service;

import com.example.demo.dto.RecipeDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.model.*;
import com.example.demo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

import static com.sun.activation.registries.LogSupport.log;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private GlobalLeaderboardRepository leaderboardRepository ;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ChallengeRepository challengeRepository;


    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private RecipeSubmissionRepository recipeSubmissionRepository;

    @Autowired
    @Lazy
    private ChallengeService challengeService;

    @Override
    public Recipe createRecipe(RecipeDTO recipeDTO) {
        Recipe recipe = mapDTOToEntity(recipeDTO);

        // Get the authenticated user's email
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Check if the user is an admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin && recipeDTO.getAuthorId() != null) {

            User specifiedAuthor = userRepository.findById(recipeDTO.getAuthorId())
                    .orElseThrow(() -> new RuntimeException("User with ID " + recipeDTO.getAuthorId() + " not found"));
            recipe.setAuthor(specifiedAuthor);

        } else {
            // Fallback to current authenticated user.
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            // Perform subscription check for non-admins
            switch(currentUser.getSubscriptionType()) {
                case FREE:
                    long freeCount = recipeRepository.countByAuthor(currentUser);
                    if (freeCount >= 10) {
                        throw new SubscriptionLimitException("Free tier limited to 10 recipes");
                    }
                    break;
                case PLUS:
                    long plusCount = recipeRepository.countByAuthor(currentUser);
                    if (plusCount >= 25) {
                        throw new SubscriptionLimitException("Plus tier limited to 25 recipes");
                    }
                    break;
                case PRO:
                    // No limit for PRO users
                    break;
            }
            recipe.setAuthor(currentUser);
        }

        return recipeRepository.save(recipe);
    }

    @Override
    public void deleteRecipesByIdsAndUserEmail(List<Long> recipeIds, String userEmail) {
        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);

        // 1) Check ownership
        for (Recipe recipe : recipes) {
            if (!recipe.getAuthor().getEmail().equals(userEmail)) {
                throw new RuntimeException("Unauthorized to delete recipe with ID: " + recipe.getId());
            }
        }
        for (Recipe recipe : recipes) {
            List<RecipeSubmission> submissions = recipeSubmissionRepository.findByRecipe_Id(recipe.getId());
            recipeSubmissionRepository.deleteAll(submissions);
        }
        // 2) Remove from user favorites
        for (Recipe recipe : recipes) {
            for (User user : recipe.getFavoritedBy()) {
                user.getFavoriteRecipes().remove(recipe);
            }
            recipe.getFavoritedBy().clear();
        }

        // 3) Remove from all books
        for (Recipe recipe : recipes) {
            // Remove from all books
            List<Book> booksWithRecipe = bookRepository.findBooksByRecipeId(recipe.getId());
            for (Book book : booksWithRecipe) {
                book.getRecipes().remove(recipe);
                bookRepository.save(book);
            }
        }
// 3.5) Remove from all meal plans
        for (Recipe recipe : recipes) {
            List<MealPlan> mealPlansWithRecipe = mealPlanRepository.findByRecipes_Id(recipe.getId());
            for (MealPlan mealPlan : mealPlansWithRecipe) {
                mealPlan.getRecipes().remove(recipe);
                mealPlanRepository.save(mealPlan);
            }
        }

        // 4) Finally delete the recipes
        recipeRepository.deleteAll(recipes);
    }
    private static final List<Pattern> BANNED_PATTERNS = List.of(
            // English examples without word boundaries
            Pattern.compile("(?i)fuck(?:ed|ing|er)?"),
            Pattern.compile("(?i)shit(?:ty|ed)?"),
            Pattern.compile("(?i)bitch"),
            Pattern.compile("(?i)asshole"),
            Pattern.compile("(?i)dick"),
            Pattern.compile("(?i)cunt(?:s|ing)?"),
            Pattern.compile("(?i)motherfucker(?:ed|ing|s)?"),
            Pattern.compile("(?i)bastard(?:s)?"),
            Pattern.compile("(?i)damn(?:ed)?"),
            Pattern.compile("(?i)crap"),
            Pattern.compile("(?i)piss(?:ed|ing)?"),
            Pattern.compile("(?i)slut(?:ty)?"),
            Pattern.compile("(?i)douche(?:bag)?"),
            Pattern.compile("(?i)fagg?ot"),
            Pattern.compile("(?i)whore"),
            Pattern.compile("(?i)bollocks"),
            Pattern.compile("(?i)arsehole"),
            Pattern.compile("(?i)twat"),
            Pattern.compile("(?i)bugger"),
            Pattern.compile("(?i)shag(?:ging)?"),
            Pattern.compile("(?i)wanker"),

            // Russian examples (similarly, without word boundaries)
            Pattern.compile("(?i)бляд(?:ь|ный|и)?"),
            Pattern.compile("(?i)сука"),
            Pattern.compile("(?i)хуй(?:[а-я]*)?"),
            Pattern.compile("(?i)пизда"),
            Pattern.compile("(?i)еб(?:ать|ается|ался|ут|ют|ешь|ала)?"),
            Pattern.compile("(?i)ёб(?:ать|ается|ался|ут|ют|ешь|ала)?"),
            Pattern.compile("(?i)нахуй"),
            Pattern.compile("(?i)мудило"),
            Pattern.compile("(?i)говно"),
            Pattern.compile("(?i)дерьмо"),
            Pattern.compile("(?i)пиздец"),
            Pattern.compile("(?i)залупа"),
            Pattern.compile("(?i)пидор(?:а|ы|ов)?"),
            Pattern.compile("(?i)хер(?:[а-я]*)?"),
            Pattern.compile("(?i)трах(?:аться|аюсь|ался|ались)?"),
            Pattern.compile("(?i)сучка")
    );

    private boolean foundBadWords(String original, String censored) {
        return !original.equals(censored);
    }

    private String censorText(String input) {
        if (input == null) return null;
        String output = input;
        for (Pattern pattern : BANNED_PATTERNS) {
            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                String match = matcher.group();
                String stars = "*".repeat(match.length());
                output = matcher.replaceFirst(stars);
                matcher = pattern.matcher(output);
            }
        }
        return output;
    }
    private boolean detectProfanity(String... fields) {
        // For each field (title, ingredients, instructions, notes, etc.)
        for (String field : fields) {
            if (field == null) continue;  // Skip null fields

            // Check if any banned pattern matches
            for (Pattern pattern : BANNED_PATTERNS) {
                Matcher matcher = pattern.matcher(field);
                if (matcher.find()) {
                    // As soon as we find a match, return true
                    return true;
                }
            }
        }
        // No profane words found
        return false;
    }
    private List<String> censorTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(tag -> censorText(tag))
                .collect(Collectors.toList());
    }

    @Override
    public Recipe updateRecipe(Long id, RecipeDTO recipeDTO) {
        Recipe existingRecipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Get the current authentication details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        // Check if the current user is an admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("ROLE_ADMIN"));

        // Allow update if the current user is either the author or an admin
        if (!existingRecipe.getAuthor().getEmail().equals(userEmail) && !isAdmin) {
            throw new RuntimeException("You are not authorized to update this recipe");
        }


        String censoredTitle = censorText(recipeDTO.getTitle());
        String censoredIngredients = censorText(recipeDTO.getIngredients());
        String censoredInstructions = censorText(recipeDTO.getInstructions());
        String censoredNotes = censorText(recipeDTO.getNotes());
        recipeDTO.setTitle(censoredTitle);
        recipeDTO.setIngredients(censoredIngredients);
        recipeDTO.setInstructions(censoredInstructions);
        recipeDTO.setNotes(censoredNotes);
        List<String> censoredTags = censorTags(recipeDTO.getTags());
        recipeDTO.setTags(censoredTags);
        if (detectProfanity(recipeDTO.getTitle(), recipeDTO.getIngredients(), recipeDTO.getInstructions(), recipeDTO.getNotes())
                && Boolean.TRUE.equals(recipeDTO.getPublic())) {
            recipeDTO.setPublic(false);
        }

        // Update the fields of the existing recipe
        existingRecipe.setTitle(recipeDTO.getTitle());
        existingRecipe.setPrepTime(recipeDTO.getPrepTime());
        existingRecipe.setCookTime(recipeDTO.getCookTime());
        existingRecipe.setIngredients(recipeDTO.getIngredients());
        existingRecipe.setInstructions(recipeDTO.getInstructions());
        existingRecipe.setImageUri(recipeDTO.getImageUri());
        existingRecipe.setUrl(recipeDTO.getUrl());
        existingRecipe.setServings(recipeDTO.getServings());
        existingRecipe.setDifficulty(recipeDTO.getDifficulty());
        existingRecipe.setCuisine(recipeDTO.getCuisine());
        existingRecipe.setSource(recipeDTO.getSource());
        existingRecipe.setVideo(recipeDTO.getVideo());
        existingRecipe.setCalories(recipeDTO.getCalories());
        existingRecipe.setCarbohydrates(recipeDTO.getCarbohydrates());
        existingRecipe.setProtein(recipeDTO.getProtein());
        existingRecipe.setFat(recipeDTO.getFat());
        existingRecipe.setSugar(recipeDTO.getSugar());
        existingRecipe.setTags(recipeDTO.getTags());
        existingRecipe.setPublic(recipeDTO.getPublic());

        // Handle AI-generated flag (if applicable)
        if (existingRecipe.getIsAiGenerated() != null && existingRecipe.getIsAiGenerated()) {
            String disclaimer = "Generated by the LeGourmand AI";
            String existingNotes = recipeDTO.getNotes() != null ? recipeDTO.getNotes() : "";
            if (!existingNotes.contains(disclaimer)) {
                existingRecipe.setNotes(disclaimer + "\n\n" + existingNotes);
            } else {
                existingRecipe.setNotes(existingNotes);
            }
        } else {
            existingRecipe.setNotes(recipeDTO.getNotes() != null ? recipeDTO.getNotes() : "");
        }

        return recipeRepository.save(existingRecipe);
    }
    @Override
    public long countRecipes() {
        return recipeRepository.count();
    }

    @Override
    public long countTotalLikes() {
        return recipeRepository.sumAllLikes();  // We'll define this next
    }

    @Override
    public void deleteRecipe(Long id) {
        Recipe existingRecipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Check that the authenticated user is the author
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        if (!existingRecipe.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to delete this recipe");
        }
        List<RecipeSubmission> submissions = recipeSubmissionRepository.findByRecipe_Id(id);
        recipeSubmissionRepository.deleteAll(submissions);
// Remove the recipe from all meal plans
        List<MealPlan> mealPlans = mealPlanRepository.findAll();
        for (MealPlan mealPlan : mealPlans) {
            mealPlan.getRecipes().remove(existingRecipe);
            mealPlanRepository.saveAll(mealPlans);
        }

        // Remove the recipe from all books
        List<Book> booksWithRecipe = bookRepository.findBooksByRecipeId(id);
        for (Book book : booksWithRecipe) {
            // Remove the recipe from the book's collection
            book.getRecipes().remove(existingRecipe);
            // Save the updated book
            bookRepository.save(book);
        }

        // Remove recipe from favorites or any other associations as needed
        for (User user : existingRecipe.getFavoritedBy()) {
            user.getFavoriteRecipes().remove(existingRecipe);
        }
        existingRecipe.getFavoritedBy().clear();

        // Now delete the recipe
        recipeRepository.delete(existingRecipe);
    }



    @Override
    public Recipe getRecipeById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
    }

    @Override
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @Override
    public List<Recipe> searchRecipesByTitle(String title) {
        return recipeRepository.findByTitleContainingIgnoreCase(title);
    }

    // Utility method to map RecipeDTO to Recipe entity
    private Recipe mapDTOToEntity(RecipeDTO recipeDTO) {
        Recipe recipe = new Recipe();
        recipe.setTitle(recipeDTO.getTitle());

        recipe.setPrepTime(recipeDTO.getPrepTime());
        recipe.setCookTime(recipeDTO.getCookTime());
        recipe.setIngredients(recipeDTO.getIngredients());
        recipe.setInstructions(recipeDTO.getInstructions());
        recipe.setImageUri(recipeDTO.getImageUri());
        recipe.setUrl(recipeDTO.getUrl());
        recipe.setServings(recipeDTO.getServings());
        recipe.setDifficulty(recipeDTO.getDifficulty());
        recipe.setCuisine(recipeDTO.getCuisine());
        recipe.setSource(recipeDTO.getSource());
        recipe.setVideo(recipeDTO.getVideo());
        recipe.setCalories(recipeDTO.getCalories());
        recipe.setCarbohydrates(recipeDTO.getCarbohydrates());
        recipe.setProtein(recipeDTO.getProtein());
        recipe.setFat(recipeDTO.getFat());
        recipe.setSugar(recipeDTO.getSugar());
        recipe.setTags(recipeDTO.getTags());
        recipe.setPublic(recipeDTO.getPublic());

        // Set AI-generated flag from DTO
        recipe.setIsAiGenerated(recipeDTO.getIsAiGenerated() != null ? recipeDTO.getIsAiGenerated() : false);

        if (recipe.getIsAiGenerated()) {
            String disclaimer = "Generated by the LeGourmand AI";
            String existingNotes = recipeDTO.getNotes() != null ? recipeDTO.getNotes() : "";


            if (!existingNotes.contains("Generated by the LeGourmand AI")) {
                recipe.setNotes(disclaimer + "\n\n" + existingNotes);
            } else {
                recipe.setNotes(existingNotes);
            }
        } else {
            recipe.setNotes(recipeDTO.getNotes() != null ? recipeDTO.getNotes() : "");
        }

        return recipe;
    }


    @Override
    public RecipeDTO mapEntityToDTO(Recipe recipe) {
        // You may get the current user from the SecurityContext if needed.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : "";
        return mapEntityToDTO(recipe, currentUserEmail);
    }

    // Add an overloaded method that takes the currentUserEmail:
    public RecipeDTO mapEntityToDTO(Recipe recipe, String currentUserEmail) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setAuthorUsername(recipe.getAuthor().getUsername());
        dto.setPrepTime(recipe.getPrepTime());
        dto.setAuthorId(recipe.getAuthor().getId());
        dto.setCookTime(recipe.getCookTime());
        dto.setIngredients(recipe.getIngredients());
        dto.setInstructions(recipe.getInstructions());
        dto.setNotes(recipe.getNotes());
        dto.setImageUri(recipe.getImageUri());
        dto.setUrl(recipe.getUrl());
        dto.setServings(recipe.getServings());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setCuisine(recipe.getCuisine());
        dto.setSource(recipe.getSource());
        dto.setVideo(recipe.getVideo());
        dto.setCalories(recipe.getCalories());
        dto.setCarbohydrates(recipe.getCarbohydrates());
        dto.setProtein(recipe.getProtein());
        dto.setFat(recipe.getFat());
        dto.setSugar(recipe.getSugar());
        dto.setTags(recipe.getTags());
        dto.setPublic(recipe.getPublic());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setIsAiGenerated(recipe.getIsAiGenerated());

        dto.setLikes(recipe.getLikes());

        // Compute likedByUser using the current user's email (if provided)
        if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
            boolean likedByUser = recipe.getLikedBy().stream()
                    .anyMatch(user -> user.getEmail().equals(currentUserEmail));
            dto.setLikedByUser(likedByUser);
        } else {
            dto.setLikedByUser(false);
        }

        List<ReviewDTO> reviews = reviewService.getReviewsByRecipe(recipe.getId());
        dto.setReviews(reviews);

        return dto;
    }


    @Override
    @Transactional
    public RecipeDTO likeRecipe(Long recipeId, String userEmail) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isLiking = !recipe.getLikedBy().contains(user);

        if (isLiking) {
            recipe.getLikedBy().add(user);
            recipe.setLikes(recipe.getLikes() + 1);
            System.out.println("✅ User " + userEmail + " liked Recipe ID " + recipeId);
        } else {
            recipe.getLikedBy().remove(user);
            recipe.setLikes(recipe.getLikes() - 1);
            System.out.println("❌ User " + userEmail + " unliked Recipe ID " + recipeId);
        }

        recipeRepository.save(recipe);
        int updatedLikes = recipe.getLikes();
        System.out.println("🔄 Recipe ID " + recipeId + " Likes updated: " + updatedLikes);

        // 🔥 Call ChallengeService to recalculate leaderboard
        challengeService.recalculateLeaderboard();

        return mapEntityToDTO(recipe, userEmail);
    }



    private void updateGlobalLeaderboard(Recipe recipe, String userEmail) {
        GlobalLeaderboardEntry entry = leaderboardRepository.findByUserEmail(userEmail);

        // Calculate the new total points (you may modify this logic)
        int newTotalPoints = calculateUserPoints(userEmail);

        // Fetch the user's username from the user repository
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String username = (user != null && user.getUsername() != null) ? user.getUsername() : userEmail;

        if (entry == null) {
            entry = new GlobalLeaderboardEntry(userEmail, username, newTotalPoints);
        } else {
            entry.setTotalPoints(newTotalPoints);
            entry.setUsername(username); // Update username in case it has changed
        }

        leaderboardRepository.save(entry);
    }


    private int calculateUserPoints(String userEmail) {
        // Fetch all challenges and compute the total points for this user
        List<Challenge> challenges = challengeRepository.findAll();
        int totalPoints = 0;

        for (Challenge challenge : challenges) {
            int maxPoints = challenge.getPoints();
            List<RecipeSubmission> submissions = challenge.getSubmissions();
            if (submissions == null || submissions.isEmpty()) continue;

            // Sort submissions by likes (descending)

            List<RecipeSubmission> validSubmissions = submissions.stream()
                    .filter(s -> s.getRecipe().getLikes() > 0)
                    .sorted(Comparator.comparingInt(s -> -s.getRecipe().getLikes()))
                    .collect(Collectors.toList());


            for (int i = 0; i < submissions.size(); i++) {
                RecipeSubmission sub = submissions.get(i);
                if (!sub.getUser().getEmail().equals(userEmail)) continue;

                int pointsEarned;
                if (i == 0) {
                    pointsEarned = maxPoints;
                } else if (i == 1) {
                    pointsEarned = (int) (maxPoints * 0.7);
                } else if (i == 2) {
                    pointsEarned = (int) (maxPoints * 0.5);
                } else {
                    pointsEarned = (int) (maxPoints * 0.1);
                }

                totalPoints += pointsEarned;
            }
        }
        return totalPoints;
    }

    @Override
    public List<Recipe> getRecipesByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return recipeRepository.findByAuthor(user);
    }
    private boolean detectProfanityInTags(List<String> tags) {
        for (String tag : tags) {
            if (detectProfanity(tag)) {
                return true;
            }
        }
        return false;
    }

}
