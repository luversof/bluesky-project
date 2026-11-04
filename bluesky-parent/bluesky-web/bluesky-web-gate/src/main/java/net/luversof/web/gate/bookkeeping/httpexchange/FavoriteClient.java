package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.time.Instant;
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

import net.luversof.web.gate.bookkeeping.constant.EntryType;

/** api-bookkeeping 즐겨찾기(내역 틀). 종류 · 이름 말고는 비어 있을 수 있다. */
@HttpExchange(url = "/api/books/{bookId}/favorites", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface FavoriteClient {

  record Favorite(
      UUID id,
      UUID bookId,
      String name,
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo,
      int displayOrder,
      UUID createdBy,
      Instant createdDate) {}

  record FavoriteCommand(
      String name,
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo) {}

  @GetExchange
  List<Favorite> list(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PostExchange
  Favorite create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody FavoriteCommand command);

  @DeleteExchange("/{favoriteId}")
  void delete(@PathVariable UUID bookId, @PathVariable UUID favoriteId, @RequestParam UUID userId);
}
