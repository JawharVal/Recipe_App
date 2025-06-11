package com.example.demo.repositories;


import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "favoriteRecipes")
    Optional<User> findWithFavoritesByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

}


