package com.example.demo.controller;


import com.example.demo.config.JWTGenerator;
import com.example.demo.repositories.UserRepository;
import com.example.demo.service.OtpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.*;
import com.example.demo.model.SubscriptionRequest;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import io.appwrite.Client;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.File;
import io.appwrite.models.InputFile;
import io.appwrite.services.Storage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/auth")
public class UserController {
    private static final String BUCKET_ID = "67b7edd400131c188c97";
    private static final String APPWRITE_ENDPOINT = "https://cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "67b7ea4e000d06d8d51a";

    @Value("${appwrite.api.key}")
    private String appwriteApiKey;

    private final UserService userService;
    private AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;


    private JWTGenerator jwtGenerator;

    Authentication authentication;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, JWTGenerator jwtGenerator,UserRepository userRepository ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user.")
    @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO newUser = userService.createUser(userDTO);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id, Authentication authentication) {

        UserDTO userDTO = userService.getUserById(id);


        if (authentication != null) {
            String currentUserEmail = authentication.getName();

            User currentUser = userService.getUserEntityByEmail(currentUserEmail);

            User targetUser = userService.getUserEntityById(id);

            boolean isFollowed = currentUser.getFollowing().contains(targetUser);
            userDTO.setFollowed(isFollowed);
        } else {
            userDTO.setFollowed(false);
        }

        return ResponseEntity.ok(userDTO);
    }
    @GetMapping("/isFollowing/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isFollowingUser(@PathVariable Long userId, Authentication authentication) {

        String currentUserEmail = authentication.getName();

        User currentUser = userService.getUserEntityByEmail(currentUserEmail);

        User targetUser = userService.getUserEntityById(userId);

        boolean isFollowing = currentUser.getFollowing().contains(targetUser);
        return ResponseEntity.ok(isFollowing);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        System.out.println("Fetching profile for email: " + email);
        UserDTO userDTO = userService.getUserByEmail(email);
        System.out.println("Fetched user: " + userDTO);
        System.out.println("User badges: " + userDTO.getBadges());
        return ResponseEntity.ok(userDTO);
    }
    @PutMapping("/profile/avatar")
    public ResponseEntity<String> updateAvatar(@RequestBody Map<String, String> request, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);

        String newAvatarUrl = request.get("imageUri");

        if (newAvatarUrl == null || newAvatarUrl.isBlank()) {
            return ResponseEntity.badRequest().body("Invalid image URL");
        }

        user.setImageUri(newAvatarUrl);
        userService.save(user);

        return ResponseEntity.ok(newAvatarUrl);
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<String> uploadAvatar(@RequestParam("avatar") MultipartFile file,
                                               Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);

        logger.info("Received avatar upload request for user: {}", email);
        logger.info("Current imageUri in DB: {}", user.getImageUri());

        try {

            java.io.File tempFile = convertMultiPartToFile(file);
            String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String uploadedFileId = uploadFileToAppwrite(tempFile, uniqueFileName);
            String uploadedUrl = "https://cloud.appwrite.io/v1/storage/buckets/67b7edd400131c188c97/files/"
                    + uploadedFileId + "/view?project=67b7ea4e000d06d8d51a";

            logger.info("New image URL generated: {}", uploadedUrl);

            user.setImageUri(uploadedUrl);
            userService.save(user);
            logger.info("Saved new imageUri for user: {}", email);


            User updatedUser = userService.getUserEntityByEmail(email);
            logger.info("DB Check: Retrieved updated imageUri: {}", updatedUser.getImageUri());

            return ResponseEntity.ok(uploadedUrl);
        } catch (Exception e) {
            logger.error("Error uploading avatar for user: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    private java.io.File convertMultiPartToFile(MultipartFile file) throws IOException {
        Path tempFilePath = Files.createTempFile("upload_", ".jpg");
        java.io.File convFile = tempFilePath.toFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private String uploadFileToAppwrite(java.io.File file, String fileName) throws IOException {
        String url = APPWRITE_ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Appwrite-Project", PROJECT_ID);
        headers.set("X-Appwrite-Key", appwriteApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("fileId", "unique()");

        body.add("filename", fileName);

        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {

            JSONObject json = new JSONObject(response.getBody());
            if (json.has("$id")) {
                return json.getString("$id");
            }
        }
        throw new IOException("Failed to upload file to Appwrite, status: " + response.getStatusCode());
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all users.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates a user by their ID.")
    @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody UserDTO userDTO, Authentication authentication) {
        String email = authentication.getName();
        UserDTO updatedUser = userService.updateUserByEmail(email, userDTO);
        return ResponseEntity.ok(updatedUser);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID.")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/profile/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        user.setImageUri(null);
        userService.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Registers a new user with a default role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "409", description = "User already exists but not verified, or email in use"),
            @ApiResponse(responseCode = "400", description = "Bad request or other validation error")
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO) {
        userDTO.setRole("user");

        try {
            UserDTO registeredUser = userService.registerUser(userDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);

        } catch (RuntimeException e) {

            String message = e.getMessage() != null ? e.getMessage() : "Registration error";

            if (message.toLowerCase().contains("not verified") ||
                    message.toLowerCase().contains("already in use")) {

                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            } else {

                return ResponseEntity.badRequest().body(message);
            }
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserDTO userDTO) {

        User user = userService.getUserEntityByEmail(userDTO.getEmail());
        if (!user.isVerified()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Email not verified. Please verify your email before logging in.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDTO.getEmail(),
                        userDTO.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtGenerator.generateToken(authentication);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
        try {
            userService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok("Email verified successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logs out the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "User logged out successfully")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().body("Logged out successfully");
    }

    @PostMapping("/favorites/{recipeId}")
    @Operation(summary = "Add favorite recipe", description = "Adds a recipe to the user's favorites.")
    @ApiResponse(responseCode = "200", description = "Recipe added to favorites successfully")
    public ResponseEntity<?> addFavoriteRecipe(@PathVariable Long recipeId, Authentication authentication) {
        String userEmail = authentication.getName();
        try {
            userService.addFavoriteRecipe(userEmail, recipeId);
            return ResponseEntity.ok("Recipe added to favorites successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        try {
            userService.resendOtp(email);
            return ResponseEntity.ok("OTP resent successfully!");
        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/favorites/{recipeId}")
    @Operation(summary = "Remove favorite recipe", description = "Removes a recipe from the user's favorites.")
    @ApiResponse(responseCode = "200", description = "Recipe removed from favorites successfully")
    public ResponseEntity<?> removeFavoriteRecipe(@PathVariable Long recipeId, Authentication authentication) {
        String userEmail = authentication.getName();
        try {
            userService.removeFavoriteRecipe(userEmail, recipeId);
            return ResponseEntity.ok("Recipe removed from favorites successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite recipes", description = "Retrieves all favorite recipes of the user.")
    @ApiResponse(responseCode = "200", description = "Favorite recipes retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FavoriteRecipeDTO.class)))
    public ResponseEntity<List<FavoriteRecipeDTO>> getFavoriteRecipes(Authentication authentication) {
        String userEmail = authentication.getName();
        List<FavoriteRecipeDTO> favoriteRecipes = userService.getFavoriteRecipes(userEmail);
        return ResponseEntity.ok(favoriteRecipes);
    }

    @PutMapping("/subscription")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update subscription plan", description = "Upgrade or renew user subscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid subscription request"),
            @ApiResponse(responseCode = "403", description = "Unauthorized operation")
    })
    public ResponseEntity<?> updateSubscription(
            @RequestBody SubscriptionRequest request,
            Authentication authentication
    ) {
        try {
            userService.updateSubscription(authentication.getName(), request.getSubscriptionType(), request.getDurationMonths());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/googleLogin")
    public ResponseEntity<AuthResponseDTO> googleLogin(@RequestBody Map<String, String> body) {

        String idToken = body.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {

            AuthResponseDTO response = userService.loginWithGoogle(idToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.requestPasswordResetMultiStep(request.getEmail());

            return ResponseEntity.ok("If that email exists, a reset code was sent.");
        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyReset(@RequestBody VerifyResetCodeRequest request) {
        try {
            userService.verifyResetCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok("Reset code verified. You can now set a new password.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password/update")
    public ResponseEntity<?> updateAfterVerification(@RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePasswordAfterVerification(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok("Password updated successfully. You can now log in.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/follow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> followUser(@PathVariable Long userId, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        try {
            userService.followUser(currentUserEmail, userId);
            return ResponseEntity.ok("Now following user with ID: " + userId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/unfollow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        try {
            userService.unfollowUser(currentUserEmail, userId);
            return ResponseEntity.ok("Unfollowed user with ID: " + userId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @PostMapping("/awardBadge")
    public ResponseEntity<String> awardBadge(
            @RequestParam String userEmail,
            @RequestParam String badge) {
        try {
            userService.awardBadgeToUser(userEmail, badge);
            return ResponseEntity.ok("Badge awarded successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}



