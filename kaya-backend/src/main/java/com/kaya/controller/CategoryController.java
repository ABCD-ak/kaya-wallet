package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.common.PageResult;
import com.kaya.entity.Category;
import com.kaya.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * {@code GET /categories?type=expense&page=1&pageSize=10}
     */
    @GetMapping
    public ApiResponse<PageResult<Category>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(categoryService.page(type, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> detail(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getById(id));
    }
}
