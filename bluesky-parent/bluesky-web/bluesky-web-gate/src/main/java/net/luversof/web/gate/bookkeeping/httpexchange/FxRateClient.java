package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/** api-bookkeeping 월 환율: 외화 1 단위 = 기준통화 rate. month 는 가계부 월 "2026-11". */
@HttpExchange(url = "/api/books/{bookId}/fx-rates", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface FxRateClient {

  record FxRate(UUID bookId, String month, String currency, BigDecimal rate) {}

  /** rate 가 null 이거나 0 이면 지운다. */
  record FxRateCommand(String month, String currency, BigDecimal rate) {}

  @GetExchange
  List<FxRate> list(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestParam String month);

  @PutExchange
  List<FxRate> set(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody FxRateCommand command);
}
