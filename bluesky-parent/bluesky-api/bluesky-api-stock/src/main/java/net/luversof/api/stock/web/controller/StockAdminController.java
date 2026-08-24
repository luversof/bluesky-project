package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.service.kis.KisStockPriceUpdateService;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminController {

  @Autowired private StockAdminService stockAdminService;

  @Autowired private KisStockPriceUpdateService kisStockPriceUpdateService;

  @PostMapping("/stock-items")
  public int stockItemBulkInsert(@RequestParam UUID userId) {
    return stockAdminService.stockItemBulkInsert(userId);
  }

  /** 시트 몇 행 중 몇 행이 들어갔는지 돌려준다. void 였을 때는 버려진 행이 흔적 없이 사라졌다. */
  @PostMapping("/trades")
  public net.luversof.api.stock.web.dto.response.LedgerImportResult tradeBulkInsert(
      @RequestParam UUID userId) {
    return stockAdminService.tradeBulkInsert(userId);
  }

  @PostMapping("/dividends")
  public net.luversof.api.stock.web.dto.response.LedgerImportResult dividendBulkInsert(
      @RequestParam UUID userId) {
    return stockAdminService.dividendBulkInsert(userId);
  }

  /** 실패 종목 수를 함께 돌려준다. void 였을 때는 몇 개가 실패해도 관리 화면이 늘 성공으로 보였다. */
  @PostMapping("/price-histories")
  public net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult priceHistoryUpdate(
      @RequestParam UUID userId) {
    return kisStockPriceUpdateService.updatePriceHistory(userId);
  }

  /** "배당주 검색" 시트의 보유/평단가를 월배당 기준 등록 종목에 한해 월배당 스냅샷에 추가/갱신한다. */
  @PostMapping("/monthly-dividend-snapshots/import-from-sheet")
  public int monthlyDividendSnapshotImportFromSheet(@RequestParam UUID userId) {
    return stockAdminService.importMonthlyDividendSnapshotsFromGoogleSheet(userId);
  }
}
