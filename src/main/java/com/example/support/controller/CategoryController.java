package com.example.support.controller;

import com.example.support.model.Category;
import com.example.support.repository.CategoryRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryRepository categoryRepository;

  public CategoryController(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @GetMapping
  public List<Category> getCategories() {
    return categoryRepository.findAll();
  }
}
