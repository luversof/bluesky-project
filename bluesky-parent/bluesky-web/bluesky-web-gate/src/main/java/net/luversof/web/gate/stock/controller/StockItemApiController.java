package net.luversof.web.gate.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

/**
 * 종목 마스터 API.
 *
 * <p>{@code @BlueskyPreAuthorize} 는 {@code @PreAuthorize("hasRole('USER')")} 메타 애노테이션인데, 이 앱에는 메서드
 * 보안이 켜져 있지 않아(@EnableMethodSecurity 없음) 실제로는 아무것도 막지 않는다. 실측: 로그인 없이 {@code GET
 * /api/stock/stockItem/search/findByName/삼성전자} 가 200 과 종목 정보를 그대로 돌려줬다. 화면 경로(/stock/**)는 URL 단위
 * 보안이 로그인으로 보내주지만 {@code /api/**} 는 그렇지 않으므로, 같은 파일군의 TradeProfitApiController 처럼 세션 사용자를 직접 확인한다.
 */
@RestController
@RequestMapping("/api/stock/stockItem")
public class StockItemApiController {

  private StockItemClient stockItemClient;

  @Autowired
  public void setStockItemClient(StockItemClient stockItemClient) {
    this.stockItemClient = stockItemClient;
  }

  @BlueskyPreAuthorize
  @PostMapping
  public ResponseEntity<StockItem> createStockItem(@RequestBody StockItem stockItem) {
    if (UserUtil.getUserId() == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockItemClient.createStockItem(stockItem));
  }

  @BlueskyPreAuthorize
  @GetMapping("/search/findByName/{name}")
  public ResponseEntity<StockItem> findByName(@PathVariable String name) {
    if (UserUtil.getUserId() == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(stockItemClient.findByName(name));
  }
}
