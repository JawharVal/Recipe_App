package com.example.demo.repositories;


import com.example.demo.model.ShoppingListItem;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
    List<ShoppingListItem> findByUser(User user);
    Optional<ShoppingListItem> findByIdAndUser(Long id, User user);


}