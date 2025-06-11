package com.example.demo.controller;

import com.example.demo.model.ShoppingListItem;
import com.example.demo.model.User;
import com.example.demo.repositories.ShoppingListItemRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users/{userId}/shopping-list")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminShoppingListController {

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ShoppingListItem>> getShoppingListItems(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ShoppingListItem> items = shoppingListItemRepository.findByUser(user);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<ShoppingListItem> addShoppingListItem(@PathVariable Long userId,
                                                                @RequestBody ShoppingListItem item) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        item.setUser(user);
        ShoppingListItem createdItem = shoppingListItemRepository.save(item);
        return ResponseEntity.ok(createdItem);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ShoppingListItem> updateShoppingListItem(@PathVariable Long userId,
                                                                   @PathVariable Long itemId,
                                                                   @RequestBody ShoppingListItem itemDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ShoppingListItem existingItem = shoppingListItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new RuntimeException("Shopping list item not found or unauthorized"));
        existingItem.setName(itemDetails.getName());
        existingItem.setCategory(itemDetails.getCategory());
        existingItem.setCount(itemDetails.getCount());
        ShoppingListItem updatedItem = shoppingListItemRepository.save(existingItem);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteShoppingListItem(@PathVariable Long userId,
                                                       @PathVariable Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ShoppingListItem existingItem = shoppingListItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new RuntimeException("Shopping list item not found or unauthorized"));
        shoppingListItemRepository.delete(existingItem);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteShoppingListItems(@PathVariable Long userId,
                                                            @RequestBody List<Long> itemIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ShoppingListItem> items = shoppingListItemRepository.findAllById(itemIds);

        items.stream()
                .filter(item -> item.getUser().equals(user))
                .forEach(shoppingListItemRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
