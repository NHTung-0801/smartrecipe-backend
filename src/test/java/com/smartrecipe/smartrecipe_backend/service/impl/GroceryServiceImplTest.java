package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.entity.GroceryList;
import com.smartrecipe.smartrecipe_backend.enums.GroceryListStatus;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.repository.GroceryItemRepository;
import com.smartrecipe.smartrecipe_backend.repository.GroceryListRecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.GroceryListRepository;
import com.smartrecipe.smartrecipe_backend.repository.IngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeRepository;
import com.smartrecipe.smartrecipe_backend.repository.RecipeIngredientRepository;
import com.smartrecipe.smartrecipe_backend.repository.PantryRepository;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.service.PantryService;
import com.smartrecipe.smartrecipe_backend.service.UnitNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroceryServiceImplTest {

    @Mock private GroceryListRepository groceryListRepository;
    @Mock private GroceryItemRepository groceryItemRepository;
    @Mock private GroceryListRecipeRepository listRecipeRepository;
    @Mock private IngredientRepository ingredientRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;
    @Mock private PantryRepository pantryRepository;
    @Mock private UnitNormalizationService unitNormalizationService;
    @Mock private PantryService pantryService;
    @Mock private UserRepository userRepository;

    private GroceryServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new GroceryServiceImpl(
                groceryListRepository,
                groceryItemRepository,
                listRecipeRepository,
                ingredientRepository,
                recipeRepository,
                recipeIngredientRepository,
                pantryRepository,
                unitNormalizationService,
                pantryService,
                userRepository
        );
        user = User.builder().id(7L).username("testuser").build();
    }

    @Test
    void getActiveListThrowsWhenNoneExists() {
        when(groceryListRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(7L, GroceryListStatus.ACTIVE))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getActiveList(7L))
                .isInstanceOf(com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException.class);
    }
    
    @Test
    void getMyListsReturnsLists() {
        GroceryList activeList = GroceryList.builder().id(10L).user(user).status(GroceryListStatus.ACTIVE).build();
        when(groceryListRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(activeList));

        var lists = service.getMyLists(7L);
        assertThat(lists).hasSize(1);
    }

    @Test
    void updateListUpdatesName() {
        GroceryList activeList = GroceryList.builder().id(10L).user(user).status(GroceryListStatus.ACTIVE).name("Old").build();
        when(groceryListRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(activeList));
        when(groceryListRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        com.smartrecipe.smartrecipe_backend.dto.request.GroceryListRequest request = new com.smartrecipe.smartrecipe_backend.dto.request.GroceryListRequest();
        request.setName("New");

        var response = service.updateList(7L, 10L, request);
        assertThat(response.getName()).isEqualTo("New");
    }

    @Test
    void clearItemsDeletesItems() {
        GroceryList activeList = GroceryList.builder().id(10L).user(user).status(GroceryListStatus.ACTIVE).build();
        when(groceryListRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(activeList));

        service.clearItems(7L, 10L);

        verify(groceryItemRepository).deleteByGroceryListId(10L);
    }
}
