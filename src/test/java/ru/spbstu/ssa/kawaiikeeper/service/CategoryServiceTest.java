package ru.spbstu.ssa.kawaiikeeper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbstu.ssa.kawaiikeeper.config.ApiConfig;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;
import ru.spbstu.ssa.kawaiikeeper.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class })
class CategoryServiceTest {

    @Mock
    private ApiConfig apiConfig;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void hasCategory_whenCategoryExists_shouldReturnTrue() {
        long userId = 123L;
        Category category = new Category(userId, "cats");
        when(categoryRepository.findById(userId)).thenReturn(Optional.of(category));

        boolean result = categoryService.hasCategory(userId);

        assertTrue(result);
        verify(categoryRepository).findById(userId);
    }

    @Test
    void hasCategory_whenCategoryDoesNotExist_shouldReturnFalse() {
        long userId = 456L;
        when(categoryRepository.findById(userId)).thenReturn(Optional.empty());

        boolean result = categoryService.hasCategory(userId);

        assertFalse(result);
        verify(categoryRepository).findById(userId);
    }

    @Test
    void hasCategory_withZeroUserId_shouldWork() {
        long userId = 0L;
        when(categoryRepository.findById(userId)).thenReturn(Optional.empty());

        boolean result = categoryService.hasCategory(userId);

        assertFalse(result);
        verify(categoryRepository).findById(0L);
    }

    @Test
    void findCategory_whenCategoryExists_shouldReturnCategory() {
        long userId = 123L;
        Category expectedCategory = new Category(userId, "dogs");
        when(categoryRepository.findById(userId)).thenReturn(Optional.of(expectedCategory));

        Optional< Category > result = categoryService.findCategory(userId);

        assertTrue(result.isPresent());
        assertEquals(expectedCategory, result.get());
        verify(categoryRepository).findById(userId);
    }

    @Test
    void findCategory_whenCategoryDoesNotExist_shouldReturnEmptyOptional() {
        long userId = 456L;
        when(categoryRepository.findById(userId)).thenReturn(Optional.empty());

        Optional< Category > result = categoryService.findCategory(userId);

        assertFalse(result.isPresent());
        verify(categoryRepository).findById(userId);
    }

    @Test
    void getDefaultCategoryName_shouldReturnFirstCategoryFromConfig() {
        List< String > categories = List.of("animals", "nature", "art");
        when(apiConfig.getApiCategories()).thenReturn(categories);

        String result = categoryService.getDefaultCategoryName();

        assertEquals("animals", result);
        verify(apiConfig).getApiCategories();
    }

    @Test
    void getDefaultCategoryName_whenConfigHasSingleCategory_shouldReturnIt() {
        List< String > singleCategory = List.of("only_category");
        when(apiConfig.getApiCategories()).thenReturn(singleCategory);

        String result = categoryService.getDefaultCategoryName();

        assertEquals("only_category", result);
        verify(apiConfig).getApiCategories();
    }

    @Test
    void getDefaultCategoryName_whenConfigIsEmpty_shouldThrowIndexOutOfBounds() {
        List< String > emptyList = List.of();
        when(apiConfig.getApiCategories()).thenReturn(emptyList);

        assertThrows(IndexOutOfBoundsException.class, () -> categoryService.getDefaultCategoryName());

        verify(apiConfig).getApiCategories();
    }

    @Test
    void setDefaultCategory_shouldSaveNewCategoryWithDefaultName() {
        long userId = 123L;
        String defaultCategoryName = "default_cat";
        Category expectedCategory = new Category(userId, defaultCategoryName);
        when(apiConfig.getApiCategories()).thenReturn(List.of(defaultCategoryName, "other"));
        when(categoryRepository.save(any(Category.class))).thenReturn(expectedCategory);

        Category result = categoryService.setDefaultCategory(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(defaultCategoryName, result.getCategoryName());
        verify(apiConfig).getApiCategories();
        verify(categoryRepository).save(argThat(category ->
            category.getUserId() == userId &&
                category.getCategoryName().equals(defaultCategoryName)
        ));
    }

    @Test
    void setDefaultCategory_withZeroUserId_shouldWork() {
        long userId = 0L;
        String defaultCategoryName = "test";
        when(apiConfig.getApiCategories()).thenReturn(List.of(defaultCategoryName));
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category(userId, defaultCategoryName));

        Category result = categoryService.setDefaultCategory(userId);

        assertNotNull(result);
        assertEquals(0L, result.getUserId());
        verify(categoryRepository).save(argThat(category -> category.getUserId() == 0L));
    }

    @Test
    void updateCategory_shouldSaveCategoryWithNewName() {
        long userId = 123L;
        String newCategoryName = "nature";
        List< String > availableCategories = List.of("animals", "nature", "art");
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category(userId, newCategoryName));

        Category result = categoryService.updateCategory(userId, newCategoryName);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(newCategoryName, result.getCategoryName());
        verify(apiConfig).getApiCategories();
        verify(categoryRepository).save(argThat(category ->
            category.getUserId() == userId &&
                category.getCategoryName().equals(newCategoryName)
        ));
    }

    @Test
    void updateCategory_whenCategoryNameNotInConfig_shouldThrowRuntimeException() {
        long userId = 123L;
        String invalidCategoryName = "invalid_category";
        List< String > availableCategories = List.of("animals", "nature", "art");
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> categoryService.updateCategory(userId, invalidCategoryName));

        assertTrue(exception.getMessage().contains("Category " + invalidCategoryName + " not found"));
        verify(apiConfig).getApiCategories();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_whenCategoryNameIsNull_shouldThrowNullPointerException() {
        long userId = 123L;
        List< String > availableCategories = List.of("animals", "nature");
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);

        assertThrows(NullPointerException.class, () -> categoryService.updateCategory(userId, null));

        verify(apiConfig).getApiCategories();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_withCaseSensitiveCategoryName_shouldRespectCase() {
        long userId = 123L;
        String categoryName = "Nature"; // с заглавной буквы
        List< String > availableCategories = List.of("animals", "nature", "art"); // все строчные
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> categoryService.updateCategory(userId, categoryName));

        assertTrue(exception.getMessage().contains("Category " + categoryName + " not found"));
        verify(apiConfig).getApiCategories();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_whenConfigHasEmptyList_shouldAlwaysThrowException() {
        long userId = 123L;
        String anyCategoryName = "any_category";
        when(apiConfig.getApiCategories()).thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> categoryService.updateCategory(userId, anyCategoryName));

        assertTrue(exception.getMessage().contains("Category " + anyCategoryName + " not found"));
        verify(apiConfig).getApiCategories();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_whenCategoryNameIsEmptyString_shouldCheckConfig() {
        long userId = 123L;
        String emptyCategoryName = "";
        List< String > availableCategories = List.of("animals", "", "art"); // включаем пустую строку
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category(userId, emptyCategoryName));

        Category result = categoryService.updateCategory(userId, emptyCategoryName);

        assertNotNull(result);
        assertEquals(emptyCategoryName, result.getCategoryName());
        verify(apiConfig).getApiCategories();
        verify(categoryRepository).save(argThat(category ->
            category.getCategoryName().isEmpty()
        ));
    }

    @Test
    void updateCategory_shouldOverwriteExistingCategory() {
        long userId = 123L;
        String newCategoryName = "nature";
        List< String > availableCategories = List.of("animals", "nature", "art");
        when(apiConfig.getApiCategories()).thenReturn(availableCategories);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category(userId, newCategoryName));

        Category result = categoryService.updateCategory(userId, newCategoryName);

        assertNotNull(result);
        assertEquals(newCategoryName, result.getCategoryName());
        verify(categoryRepository).save(argThat(category ->
            category.getUserId() == userId &&
                category.getCategoryName().equals(newCategoryName)
        ));
    }
}