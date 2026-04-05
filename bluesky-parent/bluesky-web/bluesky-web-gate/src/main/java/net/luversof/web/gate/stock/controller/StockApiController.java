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
  public ResponseEntity<List<TradeProfitTimeSeriesPoint>> timeSeries(TradeProfitRequest request) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    request.setUserId(userId);
    List<TradeProfitTimeSeriesPoint> series = tradeProfitClient.timeSeries(request.toParams());
    return ResponseEntity.ok(series);
  }
}
