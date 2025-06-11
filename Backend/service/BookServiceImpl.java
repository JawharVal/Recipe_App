package com.example.demo.service;

import com.example.demo.dto.BookDTO;
import com.example.demo.model.Book;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Override
    @Transactional
    public BookDTO createBook(BookDTO bookDTO) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check subscription limits
        switch(author.getSubscriptionType()) {
            case FREE:
                long freeBookCount = bookRepository.countByAuthor(author);
                if(freeBookCount >= 3) {
                    throw new SubscriptionLimitException("Free tier limited to 3 cookbooks");
                }
                break;

            case PLUS:
                long plusBookCount = bookRepository.countByAuthor(author);
                if(plusBookCount >= 10) {
                    throw new SubscriptionLimitException("Plus tier limited to 10 cookbooks");
                }
                break;

            case PRO:
                // Unlimited books
                break;

            default:
                throw new SubscriptionLimitException("Invalid subscription type");
        }

        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setAuthor(author);
        book.setColor(bookDTO.getColor());
        book.setPublic(bookDTO.getPublic() != null ? bookDTO.getPublic() : false);

        if (bookDTO.getRecipeIds() != null && !bookDTO.getRecipeIds().isEmpty()) {
            Set<Recipe> recipes = new HashSet<>();
            for (Long recipeId : bookDTO.getRecipeIds()) {
                Recipe recipe = recipeRepository.findById(recipeId)
                        .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + recipeId));
                if (!recipe.getAuthor().getEmail().equals(userEmail)) {
                    throw new RuntimeException("Cannot add recipe not owned by the user: " + recipeId);
                }
                recipes.add(recipe);
            }
            book.setRecipes(recipes);
            for (Recipe recipe : recipes) {
                recipe.getBooks().add(book);
            }
        }

        Book savedBook = bookRepository.save(book);

        return mapEntityToDTO(savedBook);
    }

    @Override
    public BookDTO getPublicBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        return mapEntityToDTO(book);
    }


    @Override
    public List<BookDTO> getBooksByAuthorId(Long userId) {
        // Fetch the user (author) by ID
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Return the list of books for that author (read-only)
        return bookRepository.findByAuthor(author).stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookDTO updateBook(Long id, BookDTO bookDTO) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Check if the authenticated user is the author
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        if (!existingBook.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to update this book");
        }

        existingBook.setTitle(bookDTO.getTitle());
        existingBook.setDescription(bookDTO.getDescription());
        existingBook.setColor(bookDTO.getColor());
        System.out.println("Before update: " + existingBook.getPublic());
        existingBook.setPublic(bookDTO.getPublic());
        if (bookDTO.getRecipeIds() != null) {
            Set<Recipe> newRecipes = new HashSet<>();
            // For each recipe ID in the incoming DTO
            for (Long recipeId : bookDTO.getRecipeIds()) {
                Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
                if (optRecipe.isPresent()) {
                    Recipe recipe = optRecipe.get();

                    if (existingBook.getRecipes().contains(recipe) ||
                            recipe.getAuthor().getEmail().equals(userEmail)) {
                        newRecipes.add(recipe);
                    } else {

                    }
                } else {

                }
            }

            for (Recipe recipe : existingBook.getRecipes()) {
                recipe.getBooks().remove(existingBook);
            }
            existingBook.setRecipes(newRecipes);
            for (Recipe recipe : newRecipes) {
                recipe.getBooks().add(existingBook);
            }
        }

        Book updatedBook = bookRepository.save(existingBook);
        System.out.println("After update: " + existingBook.getPublic());
        return mapEntityToDTO(updatedBook);
    }

    @Override
    public List<BookDTO> getAllPublicBooks() {
        List<Book> publicBooks = bookRepository.findAll()
                .stream()
                .filter(Book::getPublic)
                .collect(Collectors.toList());

        return publicBooks.stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }
    @Override
    public List<BookDTO> getPublicBooksByAuthor(Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookRepository.findByAuthor(author).stream()
                .filter(Book::getPublic)
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Check if the authenticated user is the author
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        if (!existingBook.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to delete this book");
        }

        // Remove associations with recipes
        for (Recipe recipe : existingBook.getRecipes()) {
            recipe.getBooks().remove(existingBook);
        }

        bookRepository.deleteById(id);
    }

    @Override
    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Check if the user is an admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("ROLE_ADMIN"));

        // Allow access if:
        // - The book is public
        // - The user is the owner
        // - The user is an admin
        if (!book.getPublic() && !book.getAuthor().getEmail().equals(userEmail) && !isAdmin) {
            throw new RuntimeException("You are not authorized to view this book");
        }

        return mapEntityToDTO(book);
    }


    @Override
    public List<BookDTO> getAllBooks() {
        // Optionally, return only books of the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookRepository.findByAuthor(user).stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDTO> getBooksByUserId(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the current user is an admin.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().contains("ADMIN"));

        // If the user is the owner OR an admin, return all books.
        if (isAdmin || user.getEmail().equals(userEmail)) {
            return bookRepository.findByAuthor(user).stream()
                    .map(this::mapEntityToDTO)
                    .collect(Collectors.toList());
        }

        // If not the owner/admin, return only public books.
        return bookRepository.findByAuthorAndIsPublicTrue(user).stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addRecipeToBook(Long bookId, Long recipeId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Check ownership
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        if (!book.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to modify this book");
        }



        book.addRecipe(recipe);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void removeRecipeFromBook(Long bookId, Long recipeId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Check ownership
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        if (!book.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to modify this book");
        }

        book.removeRecipe(recipe);
        bookRepository.save(book);
    }

    // Utility method to map Book entity to BookDTO
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
