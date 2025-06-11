package com.example.demo.service;

import com.example.demo.config.JWTGenerator;
import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.FavoriteRecipeDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.*;
import com.example.demo.repositories.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired

    private final UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RecipeReportRepository recipeReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private GlobalLeaderboardRepository leaderboardRepository;

    @Autowired
    private ChallengeService challengeService;

    private AuthenticationManager authenticationManager;

    private JWTGenerator jwtGenerator;

    @Autowired
    private RecipeSubmissionRepository recipeSubmissionRepository;


    public UserServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, JWTGenerator jwtGenerator) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtGenerator = jwtGenerator;
    }
    @Override public User getUserEntityByEmail(String email) { return userRepository.findByEmail(email) .orElseThrow(() -> new RuntimeException("User not found")); }
    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());


        user.setImageUri(userDTO.getImageUri());

        // Set default subscription type
        user.setSubscriptionType(User.SubscriptionType.FREE);
        user.setSubscriptionExpiry(null); // FREE tier, no expiry

        user = userRepository.save(user);

        // Map entity back to DTO, including the imageUri
        UserDTO createdUser = new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                userDTO.getPassword(),
                user.getRole(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiry(),
                user.getFollowers().size(),
                user.getFollowing().size()
        );
        createdUser.setImageUri(user.getImageUri());

        return createdUser;
    }

    @Override
    @Transactional
    public User save(User user) {

        return userRepository.saveAndFlush(user);
    }
    @Override
    public List<FavoriteRecipeDTO> getFavoriteRecipesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFavoriteRecipes().stream()
                .map(recipe -> new FavoriteRecipeDTO(
                        recipe.getId(),
                        recipe.getTitle(),
                        recipe.getImageUri(),
                        recipe.getAuthor().getId()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFavoriteRecipeForUser(Long userId, Long recipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (!user.getFavoriteRecipes().contains(recipe)) {
            throw new RuntimeException("Recipe is not in favorites");
        }
        user.getFavoriteRecipes().remove(recipe);
        userRepository.save(user);
    }


    @Override
    @Transactional
    public UserDTO updateUserByEmail(String email, UserDTO userDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only allowed fields
        if (userDTO.getUsername() != null) {
            user.setUsername(userDTO.getUsername());
        }
        if (userDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Update the imageUri if provided
        if (userDTO.getImageUri() != null) {
            user.setImageUri(userDTO.getImageUri());
        }

        user = userRepository.save(user);

        // Map entity back to DTO, including the imageUri
        UserDTO dto = new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                "[PROTECTED]", // Do not expose the password
                user.getRole(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiry(),
                user.getFollowers().size(),
                user.getFollowing().size()
        );
        dto.setImageUri(user.getImageUri());
        dto.setBadges(user.getBadges());

        // **Here's the key**:
        List<Long> followerIds = user.getFollowers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        dto.setFollowerIds(followerIds);
        dto.setFollowingIds(followingIds);
        return dto;
    }
    @Override
    @Transactional
    public void addOrUpdateBadge(Long userId, String badge, int count) {
        User user = getUserEntityById(userId);
        // Normalize the badge key (for example, lowercase and trimmed)
        String badgeKey = badge.trim().toLowerCase();
        Map<String, Integer> badges = user.getBadges();
        if (badges == null) {
            badges = new HashMap<>();
        }
        // Set or update the count
        badges.put(badgeKey, count);
        user.setBadges(badges);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteBadge(Long userId, String badge) {
        User user = getUserEntityById(userId);
        Map<String, Integer> badges = user.getBadges();
        if (badges != null) {
            badges.remove(badge.trim().toLowerCase());
            userRepository.save(user);
        }
    }
    @Override
    @Transactional
    public void awardBadgeToUser(String userEmail, String badge) {
        // Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("❌ User not found: " + userEmail));

        // Ensure badge storage is initialized
        if (user.getBadges() == null) {
            user.setBadges(new HashMap<>());
        }

        // Normalize badge name and increment count
        String badgeNormalized = badge.trim().toLowerCase();
        int previousCount = user.getBadges().getOrDefault(badgeNormalized, 0);
        user.getBadges().put(badgeNormalized, previousCount + 1);

        // Save the updated user
        userRepository.save(user);
        System.out.println("🏆 Badge Awarded: " + badge + " to " + user.getEmail() + " (Now has " + (previousCount + 1) + ")");
    }


    @Override
    @Transactional
    public void updateSubscription(String userEmail, String subscriptionType, int durationMonths) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User.SubscriptionType newType;
        try {
            newType = User.SubscriptionType.valueOf(subscriptionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid subscription type: " + subscriptionType);
        }

        // Prevent downgrades
        if (user.getSubscriptionType().ordinal() > newType.ordinal()) {
            throw new RuntimeException("Contact support to downgrade your subscription");
        }

        // ✅ Extend or set new subscription expiry
        LocalDateTime newExpiry = LocalDateTime.now().plusMonths(durationMonths);
        if (user.getSubscriptionType() == newType && user.getSubscriptionExpiry() != null) {
            newExpiry = user.getSubscriptionExpiry().plusMonths(durationMonths);
        }

        user.setSubscriptionType(newType);
        user.setSubscriptionExpiry(newExpiry);

        // ✅ Reset generation cycle if upgrading
        if (newType != User.SubscriptionType.FREE) {
            user.setRecipeGenerationCycleStart(LocalDateTime.now());
            user.setRecipeGenerationCount(0);
        }

        userRepository.save(user);
    }



    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // --- Update allowed fields from userDTO to user entity ---
        // Email / username / role
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setRole(userDTO.getRole());

        // Password (encoded if not null)
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Stripe customer ID
        user.setStripeCustomerId(userDTO.getStripeCustomerId());

        // Recipe generation
        user.setRecipeGenerationCount(userDTO.getRecipeGenerationCount());
        user.setRecipeGenerationCycleStart(userDTO.getRecipeGenerationCycleStart());

        // Reset token fields
        user.setResetToken(userDTO.getResetToken());
        user.setResetTokenExpiry(userDTO.getResetTokenExpiry());
        user.setResetTokenVerified(userDTO.isResetTokenVerified());

        // Subscription type (need to convert string -> enum)
        if (userDTO.getSubscriptionType() != null) {
            try {
                user.setSubscriptionType(User.SubscriptionType.valueOf(userDTO.getSubscriptionType()));
            } catch (IllegalArgumentException e) {
                // handle invalid enum string if needed
                throw new RuntimeException("Invalid subscription type: " + userDTO.getSubscriptionType());
            }
        }
        user.setSubscriptionExpiry(userDTO.getSubscriptionExpiry());

        // Profile image URI
        if (userDTO.getImageUri() != null) {
            user.setImageUri(userDTO.getImageUri());
        }

        // Verified status
        user.setVerified(userDTO.isVerified());

        // Save updated entity
        user = userRepository.save(user);

        // --- Now map updated entity back to a UserDTO for return ---
        UserDTO dto = new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(), // or "[PROTECTED]" if you don't want to return the raw password
                user.getRole(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiry(),
                user.getFollowers().size(),
                user.getFollowing().size()
        );

        // Set all additional fields
        dto.setStripeCustomerId(user.getStripeCustomerId());
        dto.setRecipeGenerationCount(user.getRecipeGenerationCount());
        dto.setRecipeGenerationCycleStart(user.getRecipeGenerationCycleStart());
        dto.setResetToken(user.getResetToken());
        dto.setResetTokenExpiry(user.getResetTokenExpiry());
        dto.setResetTokenVerified(user.isResetTokenVerified());
        dto.setImageUri(user.getImageUri());
        dto.setVerified(user.isVerified());

        // followerIds, followingIds
        List<Long> followerIds = user.getFollowers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        dto.setFollowerIds(followerIds);

        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        dto.setFollowingIds(followingIds);

        // Badges
        dto.setBadges(user.getBadges());

        return dto;
    }
    @Override
    public long countUsers() {
        return userRepository.count();
    }
    @Transactional
    @Override
    public void deleteUser(Long userId) {
        // 1) Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) Remove all meal plans
        List<MealPlan> mealPlans = mealPlanRepository.findByUserId(userId);
        mealPlanRepository.deleteAll(mealPlans);
        recipeReportRepository.deleteAllByReporter(user);

        // 3) Remove books authored by this user
        List<Book> books = bookRepository.findByAuthorId(userId);
        bookRepository.deleteAll(books);  // This ensures no FK violations in `books` table

        List<Recipe> likedRecipes = recipeRepository.findRecipesLikedByUser(userId);
        for (Recipe recipe : likedRecipes) {
            recipe.getLikedBy().remove(user);
            recipe.setLikes(recipe.getLikes() - 1);
            recipeRepository.save(recipe);
        }


            List<RecipeSubmission> submissions = recipeSubmissionRepository.findByUser(user);
            if (!submissions.isEmpty()) {
                recipeSubmissionRepository.deleteAll(submissions);

            }




        leaderboardRepository.deleteByUserEmail(user.getEmail());
        System.out.println("🚀 User removed from the leaderboard: " + user.getEmail());
        // 4) Remove all recipes authored by the user
        List<Recipe> recipes = recipeRepository.findByAuthorId(userId);
        for (Recipe recipe : recipes) {
            // 4a) Remove from `book_recipes` before deleting the recipe
            List<Book> booksContainingRecipe = bookRepository.findBooksByRecipeId(recipe.getId());
            for (Book book : booksContainingRecipe) {
                book.getRecipes().remove(recipe);
                bookRepository.save(book);  // Ensure changes persist
            }

            // 4b) Remove from users' favorites
            for (User favoriter : recipe.getFavoritedBy()) {
                favoriter.getFavoriteRecipes().remove(recipe);
            }
            recipe.getFavoritedBy().clear();

            // 4c) Delete the recipe
            recipeRepository.delete(recipe);
        }

        // 5) Remove user from all follow relationships
        for (User followedUser : user.getFollowing()) {
            followedUser.getFollowers().remove(user);
            userRepository.save(followedUser);
        }
        user.getFollowing().clear();

        for (User follower : user.getFollowers()) {
            follower.getFollowing().remove(user);
            userRepository.save(follower);
        }
        user.getFollowers().clear();
        userRepository.save(user);

        // 6) Finally, delete the user
        userRepository.delete(user);

        challengeService.recalculateLeaderboard();
    }



    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDTO dto = new UserDTO(user.getId(), user.getEmail(),
                user.getUsername(), user.getPassword(), user.getRole(),
                user.getSubscriptionType().name(), user.getSubscriptionExpiry(),
                user.getFollowers().size(), user.getFollowing().size());
        dto.setImageUri(user.getImageUri());  // Include the imageUri field

        // **Here's the key**:
        List<Long> followerIds = user.getFollowers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        dto.setBadges(user.getBadges());

        dto.setFollowerIds(followerIds);
        dto.setFollowingIds(followingIds);
        return dto;
    }



    @Override
    @Transactional
    public void updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(newRole);
        userRepository.save(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDTO dto = new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                "[PROTECTED]",
                user.getRole(),
                user.getSubscriptionType().name(),
                user.getSubscriptionExpiry(),
                user.getFollowers().size(),
                user.getFollowing().size()
        );

        dto.setImageUri(user.getImageUri());

        dto.setBadges(user.getBadges());


        List<Long> followerIds = user.getFollowers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        dto.setFollowerIds(followerIds);
        dto.setFollowingIds(followingIds);
        return dto;
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }
    @Override
    public UserDTO getUserByName(String name) {
        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserDTO(user.getId(), user.getEmail(),
                user.getUsername(), user.getPassword(),user.getRole(), user.getSubscriptionType().name(), user.getSubscriptionExpiry(),user.getFollowers().size(),
                user.getFollowing().size());

    }
    @Override
    @Transactional
    public void addFavoriteRecipe(String userEmail, Long recipeId) {
        User user = userRepository.findWithFavoritesByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (user.getFavoriteRecipes().contains(recipe)) {
            throw new RuntimeException("Recipe is already in favorites");
        }

        user.getFavoriteRecipes().add(recipe);
        userRepository.save(user);
    }

    @Override
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // If user is already verified, no need to resend
        if (user.isVerified()) {
            throw new RuntimeException("User is already verified.");
        }

        // Check if existing OTP is still valid or expired
        boolean otpExpiredOrNull = (user.getOtpCode() == null
                || user.getOtpExpiry() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now()));

        // If expired or missing, generate a new OTP
        if (otpExpiredOrNull) {
            String newCode = generateRandomSixDigitCode();
            user.setOtpCode(newCode);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
        }
        // else, we keep the current code & expiry

        // Save any changes
        userRepository.save(user);

        // Re-send via email
        sendOtpEmail(user.getEmail(), user.getOtpCode());
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        // 1) Load user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) Check if user is already verified
        if (user.isVerified()) {
            throw new RuntimeException("User already verified.");
        }

        // 3) Check OTP match & expiry
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("Invalid Verification code");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired. Please request a new one.");
        }

        // 4) If valid => mark verified
        user.setVerified(true);
        // Optional: clear the OTP fields
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeFavoriteRecipe(String userEmail, Long recipeId) {
        User user = userRepository.findWithFavoritesByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!user.getFavoriteRecipes().contains(recipe)) {
            throw new RuntimeException("Recipe is not in favorites");
        }

        user.getFavoriteRecipes().remove(recipe);
        userRepository.save(user);
    }

    @Override
    public List<FavoriteRecipeDTO> getFavoriteRecipes(String userEmail) {
        User user = userRepository.findWithFavoritesByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getFavoriteRecipes().stream()
                .map(recipe -> new FavoriteRecipeDTO(
                        recipe.getId(),
                        recipe.getTitle(),
                        recipe.getImageUri(),
                        recipe.getAuthor().getId() // Populate authorId
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            UserDTO dto = new UserDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole(),
                    user.getSubscriptionType().name(),
                    user.getSubscriptionExpiry(),
                    user.getFollowers().size(),
                    user.getFollowing().size()
            );
            dto.setImageUri(user.getImageUri());
            // Map the verified field
            dto.setVerified(user.isVerified());

            List<Long> followerIds = user.getFollowers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            List<Long> followingIds = user.getFollowing().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            dto.setStripeCustomerId(user.getStripeCustomerId());
            dto.setRecipeGenerationCount(user.getRecipeGenerationCount());
            dto.setRecipeGenerationCycleStart(user.getRecipeGenerationCycleStart());
            dto.setResetToken(user.getResetToken());
            dto.setResetTokenExpiry(user.getResetTokenExpiry());
            dto.setResetTokenVerified(user.isResetTokenVerified());
            dto.setFollowerIds(followerIds);
            dto.setFollowingIds(followingIds);
            return dto;
        }).collect(Collectors.toList());
    }


    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        // 1) Check if user with this email already exists
        Optional<User> existingOpt = userRepository.findByEmail(userDTO.getEmail());
        if (existingOpt.isPresent()) {
            User existingUser = existingOpt.get();

            // 2) If user is unverified => we can just re-send OTP instead of creating a new record
            if (!existingUser.isVerified()) {
                // Option A: automatically re-send OTP
                resendOtpInternal(existingUser);

                // Option B: throw a specific exception or return a special response
                // The front-end can catch this message and say:
                // "We resent you the OTP, please check your email to verify."
                throw new RuntimeException("Email already in use but not verified. Resent OTP to " + existingUser.getEmail());
            } else {
                // If they are verified => truly "email already in use"
                // Return or throw an error
                throw new RuntimeException("Email already in use (and verified).");
            }
        }

        // 3) If no existing user => proceed with normal registration
        User newUser = new User();
        newUser.setEmail(userDTO.getEmail());
        newUser.setUsername(userDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        newUser.setVerified(false);
        newUser.setRole(userDTO.getRole());
        // Generate a fresh OTP code (6 digits) & expiry
        String otp = generateRandomSixDigitCode();
        newUser.setOtpCode(otp);
        newUser.setOtpExpiry(LocalDateTime.now().plusMinutes(15));

        // Save user
        newUser = userRepository.save(newUser);

        // Send OTP email
        sendOtpEmail(newUser.getEmail(), otp);

        // Return the DTO
        return new UserDTO(
                newUser.getId(),
                newUser.getEmail(),
                newUser.getUsername(),
                "[PROTECTED]",
                newUser.getRole(),
                newUser.getSubscriptionType().name(),
                newUser.getSubscriptionExpiry(),
                newUser.getFollowers().size(),
                newUser.getFollowing().size()
        );
    }

    private void resendOtpInternal(User user) {
        // If old OTP is expired or null, generate a new one
        if (user.getOtpCode() == null
                || user.getOtpExpiry() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            String newCode = generateRandomSixDigitCode();
            user.setOtpCode(newCode);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
        }
        // re-send whatever OTP they have
        sendOtpEmail(user.getEmail(), user.getOtpCode());
    }

    // HELPER: send verification email
    // Helper to send email
    private void sendOtpEmail(String toEmail, String otp) {
        String subject = "Your Verification Code (Resent)";
        String message = "Welcome to LeGourmand app"
                + "\nHere is your Verification code: " + otp
                + "\nIt expires in 15 minutes."
                + "\nIf you didn't request this, ignore this email.";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }



    public UserDTO loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }



        return new UserDTO(user.getId(), user.getEmail(),
                user.getUsername(), user.getPassword(),user.getRole(), user.getSubscriptionType().name(), user.getSubscriptionExpiry(),user.getFollowers().size(),
                user.getFollowing().size());
    }

    @Override
    @Transactional
    public AuthResponseDTO loginWithGoogle(String idToken) {
        // 1) Verify the ID token with Google
        GoogleIdToken.Payload payload = verifyGoogleIdToken(idToken);
        // if null => token invalid
        if (payload == null) {
            throw new RuntimeException("Invalid Google token");
        }

        String email = payload.getEmail();
        String nameFromGoogle = (String) payload.get("name"); // you can also get given_name, family_name, etc.

        // 2) Check if user exists by email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // 2a) Create new user
            user = new User();
            user.setEmail(email);
            // For username, you could set from the Google "name" or something custom
            user.setUsername(nameFromGoogle != null ? nameFromGoogle : "googleuser");

            // Password is required in your table, so store something (random) if you want:
            user.setPassword(passwordEncoder.encode("GOOGLE_USER_" + System.currentTimeMillis()));
            user.setRole("user");
            user.setSubscriptionType(User.SubscriptionType.FREE);
            user.setSubscriptionExpiry(null);
            userRepository.save(user);
        }

        // 3) Generate your own JWT using the existing logic
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList());
        // Or load actual roles if you have them

        // Actually authenticate it in the Spring context, if needed:
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 4) Use your JWTGenerator to create a token
        String token = jwtGenerator.generateToken(authToken);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(token);
        return response;
    }
    private static final String WEB_CLIENT_ID =
            "516900387761-m4jveee923usef9enhdfrd5btct2sm97.apps.googleusercontent.com";
    private static final String ANDROID_CLIENT_ID =
            "516900387761-dg915krhb3q73eu8burgmljkret3an66.apps.googleusercontent.com";

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), new JacksonFactory())
                    // Either accept only one …
                    //.setAudience(Collections.singletonList(WEB_CLIENT_ID))
                    // … or accept both mobile & web IDs:
                    .setAudience(Arrays.asList(WEB_CLIENT_ID, ANDROID_CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            return idToken != null ? idToken.getPayload() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private String generateResetCode() {
        // Generate a random 6-digit code
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
    private void sendResetEmail(String toEmail, String code) {
        // Pseudo code for sending email (configure with your actual mail provider)

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toEmail);
    message.setSubject("Your Password Reset Code");
    message.setText("Your password reset code is: " + code + "\nIt will expire in 15 minutes.");
    mailSender.send(message);


    }

    @Override
    @Transactional
    public void requestPasswordResetMultiStep(String email) {
        // 1) Lookup user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user found with that email"));

        // 2) Generate code + set 15-minute expiry
        String resetCode = generateRandomSixDigitCode();
        user.setResetToken(resetCode);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setResetTokenVerified(false); // Not yet verified

        userRepository.save(user);

        // 3) Send code via email
        sendResetEmail(user.getEmail(), resetCode);
    }

    @Override
    @Transactional
    public void verifyResetCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user found with that email"));

        // Check if code matches
        if (!code.equals(user.getResetToken())) {
            throw new RuntimeException("Invalid or expired reset code");
        }
        // Check if not expired
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset code has expired");
        }

        // If we got here, code is valid => mark verified
        user.setResetTokenVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePasswordAfterVerification(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user found with that email"));

        // Make sure the user is verified and code hasn’t expired
        if (!user.isResetTokenVerified()) {
            throw new RuntimeException("Reset code not verified yet");
        }
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset code has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Clear out reset fields
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setResetTokenVerified(false);

        userRepository.save(user);
    }

    /** Helper to generate a random 6-digit code as a string */
    private String generateRandomSixDigitCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
    @Transactional
    public void followUser(String currentUserEmail, Long userIdToFollow) {
        if (currentUserEmail == null) {
            throw new RuntimeException("Not logged in");
        }

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (currentUser.getId().equals(userIdToFollow)) {
            throw new RuntimeException("You cannot follow yourself.");
        }

        User userToFollow = userRepository.findById(userIdToFollow)
                .orElseThrow(() -> new RuntimeException("User to follow not found"));

        // Check if already following
        if (currentUser.getFollowing().contains(userToFollow)) {
            throw new RuntimeException("Already following this user");
        }


        currentUser.getFollowing().add(userToFollow);


        userRepository.save(currentUser);

    }

    @Transactional
    public void unfollowUser(String currentUserEmail, Long userIdToUnfollow) {
        if (currentUserEmail == null) {
            throw new RuntimeException("Not logged in");
        }

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (currentUser.getId().equals(userIdToUnfollow)) {
            throw new RuntimeException("You cannot unfollow yourself.");
        }

        User userToUnfollow = userRepository.findById(userIdToUnfollow)
                .orElseThrow(() -> new RuntimeException("User to unfollow not found"));

        // Check if actually following
        if (!currentUser.getFollowing().contains(userToUnfollow)) {
            throw new RuntimeException("You are not following this user");
        }

        // Remove from the following set
        currentUser.getFollowing().remove(userToUnfollow);
        // userToUnfollow.getFollowers().remove(currentUser);

        userRepository.save(currentUser);
    }

}
