package com.example.demo.controller;


import com.example.demo.dto.RecipeDTO;
import com.example.demo.model.Recipe;
import com.example.demo.model.RecipeReport;
import com.example.demo.service.*;
//
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;




import com.example.demo.config.JWTGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.*;
import com.example.demo.model.SubscriptionRequest;
import com.example.demo.model.User;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    @Autowired
    private RecipeReportService recipeReportService;
    @Autowired
    private RecipeService recipeService;

    private final String APPWRITE_ENDPOINT = "https://cloud.appwrite.io/v1";
    private final String BUCKET_ID = "67b7edd400131c188c97";
    private final String PROJECT_ID = "67b7ea4e000d06d8d51a";

    @Value("${appwrite.api.key}")
    private String appwriteApiKey;
    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    @Autowired
    private ChallengeService challengeService;

    @PostMapping
    public ResponseEntity<RecipeDTO> createRecipe(@RequestBody RecipeDTO recipeDTO) {
        Recipe createdRecipe = recipeService.createRecipe(recipeDTO);
        RecipeDTO createdRecipeDTO = recipeService.mapEntityToDTO(createdRecipe);
        return ResponseEntity.ok(createdRecipeDTO);
    }

    @PostMapping("/{recipeId}/report")
    @Operation(summary = "Report a recipe", description = "Allows an authenticated user to report a recipe.")
    @ApiResponse(responseCode = "201", description = "Recipe reported successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeReportDTO> reportRecipe(@PathVariable Long recipeId,
                                                        @RequestBody(required = false) String reason) {
        RecipeReportDTO reportDTO = recipeReportService.reportRecipe(recipeId, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(reportDTO);
    }
    @PostMapping("/{id}/like")
    public ResponseEntity<RecipeDTO> likeRecipe(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        RecipeDTO updatedRecipe = recipeService.likeRecipe(id, userEmail);
        return ResponseEntity.ok(updatedRecipe);
    }

    @GetMapping("/{id}/likes")
    public ResponseEntity<Integer> getRecipeLikes(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeById(id);
        return ResponseEntity.ok(recipe.getLikes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDTO> updateRecipe(@PathVariable Long id, @RequestBody RecipeDTO recipeDTO) {
        Recipe updatedRecipe = recipeService.updateRecipe(id, recipeDTO);
        RecipeDTO updatedRecipeDTO = recipeService.mapEntityToDTO(updatedRecipe);
        return ResponseEntity.ok(updatedRecipeDTO);
    }

    @GetMapping("/{id}/submitted")
    public ResponseEntity<?> getSubmittedRecipes(@PathVariable Long id) {
        try {
            log.info("Fetching submitted recipes for Challenge ID: {}", id);

            List<Recipe> submittedRecipes = challengeService.getSubmittedRecipes(id);

            if (submittedRecipes.isEmpty()) {
                log.warn("No recipes found for Challenge ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No submitted recipes found.");
            }

            log.info("Found {} submitted recipes for Challenge ID: {}", submittedRecipes.size(), id);
            return ResponseEntity.ok(submittedRecipes);
        } catch (Exception e) {
            log.error("Error retrieving submitted recipes for challenge ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch submitted recipes.");
        }
    }


    @PostMapping("/{id}/image")
    public ResponseEntity<String> uploadRecipeImage(@PathVariable Long id,
                                                    @RequestParam("image") MultipartFile file) {
        try {

            java.io.File tempFile = convertMultiPartToFile(file);

            String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            String uploadedFileId = uploadFileToAppwrite(tempFile, uniqueFileName);

            String uploadedUrl = APPWRITE_ENDPOINT + "/storage/buckets/" + BUCKET_ID +
                    "/files/" + uploadedFileId + "/view?project=" + PROJECT_ID;


            Recipe recipe = recipeService.getRecipeById(id);
            recipe.setImageUri(uploadedUrl);

            recipeService.updateRecipe(recipe.getId(), recipeService.mapEntityToDTO(recipe));

            return ResponseEntity.ok(uploadedUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload recipe image: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipeById(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeById(id);
        RecipeDTO recipeDTO = recipeService.mapEntityToDTO(recipe);
        return ResponseEntity.ok(recipeDTO);
    }


    @GetMapping
    public ResponseEntity<List<RecipeDTO>> getAllRecipes() {
        List<RecipeDTO> recipeDTOs = recipeService.getAllRecipes()
                .stream()
                .map(recipeService::mapEntityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recipeDTOs);
    }


    @GetMapping("/user")
    public ResponseEntity<List<Recipe>> getUserRecipes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        List<Recipe> userRecipes = recipeService.getRecipesByUserEmail(userEmail);
        return ResponseEntity.ok(userRecipes);
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteRecipes(@RequestBody List<Long> recipeIds) {
        System.out.println("Received IDs for deletion: " + recipeIds);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        recipeService.deleteRecipesByIdsAndUserEmail(recipeIds, userEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Recipe>> searchRecipes(@RequestParam String title) {
        List<Recipe> recipes = recipeService.searchRecipesByTitle(title);
        return ResponseEntity.ok(recipes);
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

    private java.io.File convertMultiPartToFile(MultipartFile file) throws IOException {
        Path tempFilePath = Files.createTempFile("upload_", ".jpg");
        java.io.File convFile = tempFilePath.toFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
