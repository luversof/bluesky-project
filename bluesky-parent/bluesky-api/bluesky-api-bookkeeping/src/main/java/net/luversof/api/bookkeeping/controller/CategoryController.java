package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.service.CategoryService;
import net.luversof.api.bookkeeping.service.CategoryService.CategoryCommand;

/** 분류(대 · 소 2단계). */
@RestController
@RequestMapping(
    value = "/api/books/{bookId}/categories",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<Category> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return categoryService.list(bookId, userId);
  }

  @PostMapping
  public Category create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody CategoryCommand command) {
    return categoryService.create(bookId, userId, command);
  }

  /** 이름 · 숨김만 바뀐다. */
  @PutMapping("/{categoryId}")
  public Category update(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestBody CategoryCommand command) {
    return categoryService.update(bookId, userId, categoryId, command);
  }

  @DeleteMapping("/{categoryId}")
  public DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID categoryId, @RequestParam UUID userId) {
    return new DeleteResult(categoryService.delete(bookId, userId, categoryId));
  }

  @PostMapping("/{categoryId}/move")
  public void move(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestParam String direction) {
    categoryService.move(bookId, userId, categoryId, Direction.isUp(direction));
  }
}
