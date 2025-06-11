// src/main/java/com/example/demo/service/ShoppingListServiceImpl.java
package com.example.demo.service;

import com.example.demo.model.ShoppingListItem;
import com.example.demo.model.User;
import com.example.demo.repositories.ShoppingListItemRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingListServiceImpl implements ShoppingListService {

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<ShoppingListItem> getAllItemsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return shoppingListItemRepository.findByUser(user);
    }

    @Override
    public ShoppingListItem addItem(String userEmail, ShoppingListItem item) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        item.setUser(user);
        return shoppingListItemRepository.save(item);
    }

    @Override
    public ShoppingListItem updateItem(String userEmail, Long itemId, ShoppingListItem itemDetails) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ShoppingListItem existingItem = shoppingListItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new RuntimeException("Shopping list item not found or unauthorized"));

        existingItem.setName(itemDetails.getName());
        existingItem.setCategory(itemDetails.getCategory());
        existingItem.setCount(itemDetails.getCount());

        return shoppingListItemRepository.save(existingItem);
    }

    @Override
    public void deleteItem(String userEmail, Long itemId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ShoppingListItem existingItem = shoppingListItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new RuntimeException("Shopping list item not found or unauthorized"));
        shoppingListItemRepository.delete(existingItem);
    }

    @Override
    public void bulkDeleteItems(String userEmail, List<Long> itemIds) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ShoppingListItem> items = shoppingListItemRepository.findAllById(itemIds);
        items.stream()
                .filter(item -> item.getUser().equals(user))
                .forEach(shoppingListItemRepository::delete);
    }
}
