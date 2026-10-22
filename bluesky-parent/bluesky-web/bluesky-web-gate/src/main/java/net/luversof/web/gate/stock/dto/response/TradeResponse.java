package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.luversof.web.gate.stock.constant.TradeType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeResponse(
    UUID id,
    UUID accountId,
    UUID stockItemId,
    String stockItemName,
    TradeType type,
    int quantity,
    BigDecimal price,
    BigDecimal fee,
    BigDecimal tax,
    BigDecimal amount,
    BigDecimal realizedProfit,
    Instant tradeDate) {

  /**
   * 종목 이름만 바꾼 사본 - 매매 목록 · 자산 성장이 api 응답에 게이트가 아는 종목 이름을 붙일 때 쓴다(2026-10-22 까지 두 곳이 성분 12 개를 하나씩 옮겨
   * 적었다).
   */
  public TradeResponse withStockItemName(String name) {
    return new TradeResponse(
        id,
        accountId,
        stockItemId,
        name,
        type,
        quantity,
        price,
        fee,
        tax,
        amount,
        realizedProfit,
        tradeDate);
  }
}
