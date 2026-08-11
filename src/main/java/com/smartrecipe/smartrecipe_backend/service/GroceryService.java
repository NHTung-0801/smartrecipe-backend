package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.request.CompleteGroceryRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryItemRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.GroceryListRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryItemResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.GroceryListResponse;

import java.util.List;

public interface GroceryService {
    GroceryListResponse createList(Long userId, GroceryListRequest request);
    GroceryListResponse getActiveList(Long userId);
    List<GroceryListResponse> getMyLists(Long userId);
    GroceryListResponse getList(Long userId, Long listId);
    void deleteList(Long userId, Long listId);
    GroceryListResponse updateList(Long userId, Long listId, GroceryListRequest request);
    void clearItems(Long userId, Long listId);
    
    GroceryItemResponse addItem(Long userId, Long listId, GroceryItemRequest request);
    GroceryItemResponse updateItem(Long userId, Long listId, Long itemId, GroceryItemRequest request);
    void removeItem(Long userId, Long listId, Long itemId);
    GroceryItemResponse togglePurchased(Long userId, Long listId, Long itemId);
    
    GroceryListResponse completeList(Long userId, Long listId, CompleteGroceryRequest request);
    GroceryListResponse generateFromRecipe(Long userId, Long recipeId, Integer servings);
    GroceryListResponse generateFromPantry(Long userId);
}
