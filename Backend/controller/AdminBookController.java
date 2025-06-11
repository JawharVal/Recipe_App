package com.example.demo.controller;

import com.example.demo.dto.BookDTO;
import com.example.demo.model.Book;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminBookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get books by user ID (admin access)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookDTO.class)))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires admin privileges")
    })
    public ResponseEntity<List<BookDTO>> getBooksByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Book> books = bookRepository.findByAuthor(user);
        List<BookDTO> bookDTOs = books.stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookDTOs);
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "Admin updates a book by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book updated successfully",
                    content = @Content(schema = @Schema(implementation = BookDTO.class))),
            @ApiResponse(responseCode = "404", description = "Book or recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires admin privileges")
    })
    public ResponseEntity<BookDTO> adminUpdateBook(@PathVariable Long bookId, @RequestBody BookDTO bookDTO) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setColor(bookDTO.getColor());


        if (bookDTO.getRecipeIds() != null) {
            Set<Recipe> recipes = new HashSet<>();
            for (Long recipeId : bookDTO.getRecipeIds()) {
                Recipe recipe = recipeRepository.findById(recipeId)
                        .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + recipeId));
                recipes.add(recipe);
            }

            for (Recipe r : book.getRecipes()) {
                r.getBooks().remove(book);
            }
            book.setRecipes(recipes);
            for (Recipe r : recipes) {
                r.getBooks().add(book);
            }
        }
        Book updatedBook = bookRepository.save(book);
        return ResponseEntity.ok(mapEntityToDTO(updatedBook));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Admin deletes a book by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires admin privileges")
    })
    public ResponseEntity<Void> adminDeleteBook(@PathVariable Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        for (Recipe recipe : book.getRecipes()) {
            recipe.getBooks().remove(book);
        }
        bookRepository.delete(book);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bookId}/recipes/{recipeId}")
    @Operation(summary = "Admin removes a recipe from a book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe removed from book successfully"),
            @ApiResponse(responseCode = "404", description = "Book or recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires admin privileges")
    })
    public ResponseEntity<Void> adminRemoveRecipeFromBook(@PathVariable Long bookId, @PathVariable Long recipeId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        if (book.getRecipes().contains(recipe)) {
            book.getRecipes().remove(recipe);
            recipe.getBooks().remove(book);
            bookRepository.save(book);
        }
        return ResponseEntity.ok().build();
    }

    private BookDTO mapEntityToDTO(Book book) {
        Set<Long> recipeIds = book.getRecipes().stream()
                .map(Recipe::getId)
                .collect(Collectors.toSet());
        String color = book.getColor() != null ? book.getColor() : "#866232";
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getAuthor().getId(),
                recipeIds,
                color,
                book.getPublic()
        );
    }
}
