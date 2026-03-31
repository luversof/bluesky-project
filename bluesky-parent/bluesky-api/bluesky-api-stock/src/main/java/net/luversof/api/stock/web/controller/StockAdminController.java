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
}
