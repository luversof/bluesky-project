package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
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

import net.luversof.web.gate.bookkeeping.constant.AssetKind;

/** api-bookkeeping 자산. 목록은 숨긴 자산까지 잔액과 함께 온다. */
@HttpExchange(url = "/api/books/{bookId}/assets", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface AssetClient {

  record Asset(
      UUID id,
      UUID bookId,
      AssetKind kind,
      String name,
      String currency,
      BigDecimal openingBalance,
      Integer paymentDay,
      int displayOrder,
      boolean hidden) {}

  record AssetBalance(Asset asset, BigDecimal balance) {}

  /** paymentDay 는 CREDIT_CARD 만. hidden 이 null 이면 그대로. */
  record AssetCommand(
      AssetKind kind,
      String name,
      String currency,
      BigDecimal openingBalance,
      Integer paymentDay,
      Boolean hidden) {}

  /** 내역이 있어 숨기기만 했으면 deleted=false. */
  record DeleteResult(boolean deleted) {}

  @GetExchange
  List<AssetBalance> list(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PostExchange
  Asset create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody AssetCommand command);

  @PutExchange("/{assetId}")
  Asset update(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestBody AssetCommand command);

  @DeleteExchange("/{assetId}")
  DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID assetId, @RequestParam UUID userId);

  /** direction: up · down. */
  @PostExchange("/{assetId}/move")
  void move(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestParam String direction);
}
