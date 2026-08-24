package net.luversof.web.gate.stock.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;

/**
 * 관리자용 대량 적재 API.
 *
 * <p>본문 없이 호출되고 세션 사용자를 그대로 백엔드에 넘기는데, {@code @BlueskyPreAuthorize} 는 메서드 보안이 꺼져 있어 실효가 없다(실측: 같은
 * 방식의 {@code /api/stock/stockItem/search/...} 가 비로그인 200). 그대로 두면 로그인 없이 대량 적재가 트리거되고 userId 로 null
 * 이 넘어간다. 세션 사용자를 직접 확인해 401 로 끊는다.
 */
@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminApiController {

  @Autowired private StockAdminClient stockAdminClient;

  @BlueskyPreAuthorize
  @PostMapping("/stock-items")
  public ResponseEntity<Integer> stockItemBulkInsert() {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockAdminClient.stockItemBulkInsert(userId));
  }

  @BlueskyPreAuthorize
  @PostMapping("/trades")
  public ResponseEntity<net.luversof.web.gate.stock.dto.response.LedgerImportResult>
      tradeBulkInsert() {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockAdminClient.tradeBulkInsert(userId));
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividends")
  public ResponseEntity<net.luversof.web.gate.stock.dto.response.LedgerImportResult>
      dividendBulkInsert() {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockAdminClient.dividendBulkInsert(userId));
  }

  /**
   * 가격 이력 갱신. 실패한 종목 수를 본문으로 돌려준다.
   *
   * <p>예전에는 본문이 비어 있어, 몇 종목이 실패하든 화면에는 늘 성공으로 보였다(실측: 평일인데 가격이 아예 없는 날이 2일 있었고 화면에서는 "기준일이 오래됐다" 로만
   * 간접적으로 드러났다).
   */
  @BlueskyPreAuthorize
  @PostMapping("/price-histories")
  public ResponseEntity<net.luversof.web.gate.stock.dto.response.PriceHistoryUpdateResult>
      priceHistoriesUpdate() {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockAdminClient.priceHistoriesUpdate(userId));
  }
}
