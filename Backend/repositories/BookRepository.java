package com.example.demo.repositories;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthor(User author);
    long countByAuthor(User author);
    List<Book> findByAuthorId(Long authorId);
    @Query("SELECT b FROM Book b JOIN b.recipes r WHERE r.id = :recipeId")
    List<Book> findBooksByRecipeId(@Param("recipeId") Long recipeId);
    List<Book> findByAuthorAndIsPublicTrue(User author);
}
