package net.luversof.web.gate.stock.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@RestController
@RequestMapping("/api/stock/tradeProfit")
public class TradeProfitApiController {

  private TradeProfitClient tradeProfitClient;

  @Autowired
  public void setTradeProfitClient(TradeProfitClient tradeProfitClient) {
    this.tradeProfitClient = tradeProfitClient;
  }

  @BlueskyPreAuthorize
  @GetMapping("/calculateProfit")
  public ResponseEntity<List<TradeProfit>> calculateProfit(TradeProfitRequest request) {
    // 요청에 실려 온 userId 를 그대로 백엔드로 넘기고 있었다. 로그인만 되어 있으면 남의 userId 로
    // 조회가 되는 셈이라, 같은 파일군의 StockApiController 처럼 세션 사용자로 덮어쓴다
    // (실측: 임의 UUID 는 계좌가 없어 400 이 났을 뿐 검증이 있었던 것은 아니다).
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    request.setUserId(userId);
    return ResponseEntity.ok(tradeProfitClient.calculateProfit(request.toParams()));
  }
}
