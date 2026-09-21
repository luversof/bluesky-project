package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.MonthlyDividendCatalogService;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse;

/** 월배당 ETF 목록(종목 단위). 사용자 값이 없으므로 userId 를 받지 않는다 &mdash; 보유 여부 표시는 게이트가 원장에서 따로 붙인다. */
@RestController
@RequestMapping("/api/monthlyDividendCatalog")
public class MonthlyDividendCatalogController {

  @Autowired private MonthlyDividendCatalogService monthlyDividendCatalogService;

  @GetMapping
  public List<MonthlyDividendCatalogResponse> findCatalog(
      @RequestParam(required = false) Boolean activeOnly) {
    return monthlyDividendCatalogService.findCatalog(activeOnly);
  }
}
