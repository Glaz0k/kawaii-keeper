package ru.spbstu.ssa.kawaiikeeper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.config.ApiConfig;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;
import ru.spbstu.ssa.kawaiikeeper.repository.CategoryRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final ApiConfig apiConfig;
    private final CategoryRepository categoryRepository;

    public boolean hasCategory(long userId) {
        return categoryRepository.findById(userId).isPresent();
    }

    public Optional< Category > findCategory(long userId) {
        return categoryRepository.findById(userId);
    }

    public String getDefaultCategoryName() {
        return apiConfig.getApiCategories().get(0);
    }

    public Category setDefaultCategory(long userId) {
        return categoryRepository.save(new Category(userId, getDefaultCategoryName()));
    }

    public Category updateCategory(long userId, String categoryName) {
        if (!apiConfig.getApiCategories().contains(categoryName)) {
            throw new RuntimeException("Category " + categoryName + " not found");
        }
        return categoryRepository.save(new Category(userId, categoryName));
    }

}


