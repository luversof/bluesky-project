package net.luversof.api.stock.web.controller;

import java.util.UUID;
import net.luversof.api.stock.repository.DailyAccountSnapshotRepository;
import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.service.kis.KisStockPriceUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminController {

    @Autowired private StockAdminService stockAdminService;

    @Autowired private KisStockPriceUpdateService kisStockPriceUpdateService;

    @Autowired private DailyAccountSnapshotRepository dailyAccountSnapshotRepository;

    @PostMapping("/stock-items")
    public int stockItemBulkInsert(@RequestParam UUID userId) {
        return stockAdminService.stockItemBulkInsert(userId);
    }

    @PostMapping("/trades")
    public void tradeBulkInsert(@RequestParam UUID userId) {
        stockAdminService.tradeBulkInsert(userId);
    }

    @PostMapping("/dividends")
    public void dividendBulkInsert(@RequestParam UUID userId) {
        stockAdminService.dividendBulkInsert(userId);
    }

    @PostMapping("/price-histories")
    public void priceHistoryUpdate(@RequestParam UUID userId) {
        kisStockPriceUpdateService.updatePriceHistory();
    }

    /**
     * WmaState 스키마 변경(quantity: long→BigDecimal) 또는 수정주가 재조정 이후 전체 스냅샷을 초기화합니다.
     * 다음 aggregateTimeSeries() 호출 시 처음부터 재계산됩니다.
     */
    @PostMapping("/reset-snapshots")
    public long resetAllSnapshots() {
        long count = dailyAccountSnapshotRepository.count();
        dailyAccountSnapshotRepository.deleteAll();
        return count;
    }
}
