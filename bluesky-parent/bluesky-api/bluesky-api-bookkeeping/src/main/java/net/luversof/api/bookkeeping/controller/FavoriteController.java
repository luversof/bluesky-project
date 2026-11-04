package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.BookFavorite;
import net.luversof.api.bookkeeping.service.FavoriteService;
import net.luversof.api.bookkeeping.service.FavoriteService.FavoriteCommand;

/** 즐겨찾기(내역 틀). */
@RestController
@RequestMapping(
    value = "/api/books/{bookId}/favorites",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class FavoriteController {

  private final FavoriteService favoriteService;

  public FavoriteController(FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }

  @GetMapping
  public List<BookFavorite> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return favoriteService.list(bookId, userId);
  }

  @PostMapping
  public BookFavorite create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody FavoriteCommand command) {
    return favoriteService.create(bookId, userId, command);
  }

  @DeleteMapping("/{favoriteId}")
  public void delete(
      @PathVariable UUID bookId, @PathVariable UUID favoriteId, @RequestParam UUID userId) {
    favoriteService.delete(bookId, userId, favoriteId);
  }
}
