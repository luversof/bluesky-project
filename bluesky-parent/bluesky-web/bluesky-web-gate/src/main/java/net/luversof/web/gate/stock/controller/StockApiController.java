package net.luversof.web.gate.stock.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/api")
public class StockApiController {

  private TradeProfitClient tradeProfitClient;

  @Autowired
  public void setTradeProfitClient(TradeProfitClient tradeProfitClient) {
    this.tradeProfitClient = tradeProfitClient;
  }

  @BlueskyPreAuthorize
  @GetMapping(value = "/timeSeries", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<List<TradeProfitTimeSeriesPoint>> timeSeries(
      TradeProfitRequest request, String granularity) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    request.setUserId(userId);
    // granularity 는 TradeProfitRequest 의 필드가 아니라 toParams() 에 실리지 않는다. 그래서 무엇을
    // 넘기든 백엔드가 기본값(일별)으로 답했다(실측: AUTO/MONTHLY 를 줘도 1,546.5KB·6,165 점).
    // 별도 파라미터로 받아 그대로 전달한다.
    var params = request.toParams();
    if (org.springframework.util.StringUtils.hasText(granularity)) {
      params.add("granularity", granularity);
    }
    List<TradeProfitTimeSeriesPoint> series = tradeProfitClient.timeSeries(params);
    return ResponseEntity.ok(series);
  }
}
