package com.example.demo.service;

import com.example.demo.dto.BookDTO;
import com.example.demo.model.Book;

import java.util.List;

public interface BookService {
    BookDTO createBook(BookDTO bookDTO);
    BookDTO updateBook(Long id, BookDTO bookDTO);
    void deleteBook(Long id);
    BookDTO getBookById(Long id);
    List<BookDTO> getAllBooks();
    List<BookDTO> getBooksByUserId(Long userId);
    void addRecipeToBook(Long bookId, Long recipeId);
    void removeRecipeFromBook(Long bookId, Long recipeId);
    List<BookDTO> getPublicBooksByAuthor(Long userId);
    List<BookDTO> getBooksByAuthorId(Long userId);
    List<BookDTO> getAllPublicBooks();
    BookDTO getPublicBookById(Long id);
}
