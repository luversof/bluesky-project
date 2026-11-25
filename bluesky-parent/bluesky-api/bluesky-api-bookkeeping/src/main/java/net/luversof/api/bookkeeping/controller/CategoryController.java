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

/**
 * 가계부 분류
 *
 * <p>대 · 소 2단계.
 */
@RestController
@RequestMapping(
    value = "/api/books/{bookId}/categories",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  /**
   * 분류 목록 조회
   *
   * <p>숨긴 분류도 들어 있다(화면이 거른다).
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @return 분류 목록
   */
  @GetMapping
  public List<Category> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return categoryService.list(bookId, userId);
  }

  /**
   * 분류 생성
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param command 이름 · 종류 · 부모 분류(소분류일 때)
   * @return 만든 분류
   */
  @PostMapping
  public Category create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody CategoryCommand command) {
    return categoryService.create(bookId, userId, command);
  }

  /**
   * 분류 수정
   *
   * <p>이름 · 숨김만 바뀐다.
   */
  @PutMapping("/{categoryId}")
  public Category update(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestBody CategoryCommand command) {
    return categoryService.update(bookId, userId, categoryId, command);
  }

  /**
   * 분류 삭제
   *
   * <p>내역이나 소분류가 있으면 지우지 않고 자신과 소분류를 숨긴다.
   *
   * @param bookId 가계부 id
   * @param categoryId 분류 id
   * @param userId 요청한 사용자
   * @return 지웠으면 deleted=true, 숨기기만 했으면 false
   */
  @DeleteMapping("/{categoryId}")
  public DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID categoryId, @RequestParam UUID userId) {
    return new DeleteResult(categoryService.delete(bookId, userId, categoryId));
  }

  /**
   * 분류 순서 변경
   *
   * <p>같은 부모 · 같은 종류 안에서 한 칸 옮긴다.
   *
   * @param bookId 가계부 id
   * @param categoryId 분류 id
   * @param userId 요청한 사용자
   * @param direction up 또는 down
   */
  @PostMapping("/{categoryId}/move")
  public void move(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestParam String direction) {
    categoryService.move(bookId, userId, categoryId, Direction.isUp(direction));
  }
}
