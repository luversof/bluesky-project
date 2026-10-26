package net.luversof.web.gate.stock.controller;

import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;

/** 다른 패키지의 시험이 자산 성장 컨트롤러의 패키지 전용 도우미를 부르게 해 주는 창구(시험 전용). */
public final class StockAssetGrowthHtmxControllerAccess {

  private StockAssetGrowthHtmxControllerAccess() {}

  public static void addFilterIds(
      org.springframework.util.MultiValueMap<String, String> params, TradeProfitRequest request) {
    StockAssetGrowthHtmxController.addFilterIds(params, request);
  }
}
