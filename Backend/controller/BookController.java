package com.example.demo.controller;

import com.example.demo.dto.BookDTO;
import com.example.demo.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new book", description = "Creates a new book of recipes.")
    @ApiResponse(responseCode = "201", description = "Book created successfully")
    public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
        BookDTO createdBook = bookService.createBook(bookDTO);
        return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a book", description = "Updates an existing book by its ID.")
    @ApiResponse(responseCode = "200", description = "Book updated successfully")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        BookDTO updatedBook = bookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(updatedBook);
    }
    @GetMapping("/author/{userId}")
    @Operation(summary = "Get books by author ID", description = "Retrieves all public books for the given author ID.")
    public ResponseEntity<List<BookDTO>> getBooksByAuthorId(@PathVariable Long userId) {
        List<BookDTO> books = bookService.getBooksByAuthorId(userId);
        return ResponseEntity.ok(books);
    }
    @GetMapping("/public/{id}")
    @Operation(summary = "Get a public book by ID", description = "Retrieves a book by its ID for public viewing.")
    public ResponseEntity<BookDTO> getPublicBookById(@PathVariable Long id) {
        BookDTO bookDTO = bookService.getPublicBookById(id);
        return ResponseEntity.ok(bookDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a book", description = "Deletes a book by its ID.")
    @ApiResponse(responseCode = "204", description = "Book deleted successfully")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a book by ID", description = "Retrieves a book by its ID.")
    @ApiResponse(responseCode = "200", description = "Book retrieved successfully")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
        BookDTO bookDTO = bookService.getBookById(id);
        return ResponseEntity.ok(bookDTO);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all books", description = "Retrieves all books of the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        List<BookDTO> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get books by user ID", description = "Retrieves all books created by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    public ResponseEntity<List<BookDTO>> getBooksByUserId(@PathVariable Long userId) {
        List<BookDTO> books = bookService.getBooksByUserId(userId);
        return ResponseEntity.ok(books);
    }
    @GetMapping("/public/author/{userId}")
    @Operation(summary = "Get all public books by a specific author", description = "Retrieves all public books for the given author ID.")
    public ResponseEntity<List<BookDTO>> getPublicBooksByAuthor(@PathVariable Long userId) {
        List<BookDTO> books = bookService.getPublicBooksByAuthor(userId);
        return ResponseEntity.ok(books);
    }

    @PostMapping("/{bookId}/recipes/{recipeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a recipe to a book", description = "Adds a recipe to a specific book.")
    @ApiResponse(responseCode = "200", description = "Recipe added to book successfully")
    public ResponseEntity<Void> addRecipeToBook(@PathVariable Long bookId, @PathVariable Long recipeId) {
        bookService.addRecipeToBook(bookId, recipeId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/public")
    @Operation(summary = "Get all public books", description = "Retrieves all publicly available books.")
    public ResponseEntity<List<BookDTO>> getAllPublicBooks() {
        List<BookDTO> books = bookService.getAllPublicBooks();
        return ResponseEntity.ok(books);
    }

    @DeleteMapping("/{bookId}/recipes/{recipeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a recipe from a book", description = "Removes a recipe from a specific book.")
    @ApiResponse(responseCode = "200", description = "Recipe removed from book successfully")
    public ResponseEntity<Void> removeRecipeFromBook(@PathVariable Long bookId, @PathVariable Long recipeId) {
        bookService.removeRecipeFromBook(bookId, recipeId);
        return ResponseEntity.ok().build();
    }
}
