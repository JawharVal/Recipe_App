
package com.example.demo.controller;

import com.example.demo.model.ShoppingListItem;
import com.example.demo.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

    @Autowired
    private ShoppingListService shoppingListService;

    @GetMapping
    public ResponseEntity<List<ShoppingListItem>> getAllItems() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        List<ShoppingListItem> items = shoppingListService.getAllItemsForUser(userEmail);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<ShoppingListItem> addItem(@RequestBody ShoppingListItem item) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        ShoppingListItem createdItem = shoppingListService.addItem(userEmail, item);
        return ResponseEntity.ok(createdItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListItem> updateItem(@PathVariable Long id, @RequestBody ShoppingListItem itemDetails) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        ShoppingListItem updatedItem = shoppingListService.updateItem(userEmail, id, itemDetails);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        shoppingListService.deleteItem(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteItems(@RequestBody List<Long> itemIds) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        shoppingListService.bulkDeleteItems(userEmail, itemIds);
        return ResponseEntity.noContent().build();
    }
}
