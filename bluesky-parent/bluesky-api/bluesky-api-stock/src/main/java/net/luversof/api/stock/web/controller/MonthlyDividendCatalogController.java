package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.MonthlyDividendCatalogService;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse;

/**
 * 월배당 카탈로그
 *
 * <p>사용자 값이 없으므로 userId 를 받지 않는다 &mdash; 보유 여부 표시는 게이트가 원장에서 따로 붙인다.
 */
@RestController
@RequestMapping("/api/monthlyDividendCatalog")
public class MonthlyDividendCatalogController {

  @Autowired private MonthlyDividendCatalogService monthlyDividendCatalogService;

  /**
   * 월배당 카탈로그 조회
   *
   * <p>등록된 월배당 프로필 전부(표시 순서대로)와 지급 · 수익률 지표.
   *
   * @param activeOnly true 면 활성 프로필만
   * @return 종목별 카탈로그
   */
  @GetMapping
  public List<MonthlyDividendCatalogResponse> findCatalog(
      @RequestParam(required = false) Boolean activeOnly) {
    return monthlyDividendCatalogService.findCatalog(activeOnly);
  }
}
