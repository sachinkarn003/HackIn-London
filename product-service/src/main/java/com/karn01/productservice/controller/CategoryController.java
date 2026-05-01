package com.karn01.productservice.controller;

import com.karn01.productservice.dto.CreateCategoryDto;
import com.karn01.productservice.entity.Category;
import com.karn01.productservice.exception.BadRequestException;
import com.karn01.productservice.exception.ResourceNotFoundException;
import com.karn01.productservice.repository.CategoryRepository;
import com.karn01.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @GetMapping("/categories")
    public Iterable<Category> getAllPublic() {
        return categoryRepository.findAll();
    }

    @PostMapping("/admin/categories")
    public Category create(@RequestBody CreateCategoryDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());

        return categoryRepository.save(category);
    }

    @GetMapping("/admin/categories")
    public Iterable<Category> getAll() {
        return categoryRepository.findAll();
    }

    @PutMapping("/admin/categories/{id}")
    public Category update(@PathVariable UUID id, @RequestBody CreateCategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        return categoryRepository.save(category);
    }

    @DeleteMapping("/admin/categories/{id}")
    public String delete(@PathVariable UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (productRepository.countByCategory(category) > 0) {
            throw new BadRequestException("Cannot delete category while products are assigned to it");
        }

        categoryRepository.delete(category);
        return "Category deleted successfully";
    }
}
