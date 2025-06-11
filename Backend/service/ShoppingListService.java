package com.example.demo.service;


import com.example.demo.model.ShoppingListItem;
import java.util.List;

public interface ShoppingListService {
    List<ShoppingListItem> getAllItemsForUser(String userEmail);
    ShoppingListItem addItem(String userEmail, ShoppingListItem item);
    ShoppingListItem updateItem(String userEmail, Long itemId, ShoppingListItem itemDetails);
    void deleteItem(String userEmail, Long itemId);
    void bulkDeleteItems(String userEmail, List<Long> itemIds);
}