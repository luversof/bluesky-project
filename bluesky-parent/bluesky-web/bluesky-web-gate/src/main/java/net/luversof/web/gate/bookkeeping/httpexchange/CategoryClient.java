package net.luversof.web.gate.bookkeeping.httpexchange;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.DeleteResult;

/** api-bookkeeping 분류(대 · 소 2단계, parentId 가 null 이면 대분류). */
@HttpExchange(
    url = "/api/books/{bookId}/categories",
    contentType = MediaType.APPLICATION_JSON_VALUE)
public interface CategoryClient {

  record Category(
      UUID id,
      UUID bookId,
      UUID parentId,
      CategoryKind kind,
      String name,
      int displayOrder,
      boolean hidden) {}

  /** 추가: parentId 가 있으면 kind 는 비워도 된다. 수정: name · hidden 만 쓴다. */
  record CategoryCommand(UUID parentId, CategoryKind kind, String name, Boolean hidden) {}

  @GetExchange
  List<Category> list(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PostExchange
  Category create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody CategoryCommand command);

  @PutExchange("/{categoryId}")
  Category update(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestBody CategoryCommand command);

  @DeleteExchange("/{categoryId}")
  DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID categoryId, @RequestParam UUID userId);

  /** direction: up · down. */
  @PostExchange("/{categoryId}/move")
  void move(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam UUID userId,
      @RequestParam String direction);
}
