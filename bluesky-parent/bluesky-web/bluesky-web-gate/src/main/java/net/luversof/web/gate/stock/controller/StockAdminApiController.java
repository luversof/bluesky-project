package net.luversof.web.gate.stock.controller;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminApiController {

    @Autowired private StockAdminClient stockAdminClient;

    @BlueskyPreAuthorize
    @PostMapping("/stock-items")
    public int stockItemBulkInsert() {
        return stockAdminClient.stockItemBulkInsert(UserUtil.getUserId());
    }

    @BlueskyPreAuthorize
    @PostMapping("/trades")
    public void tradeBulkInsert() {
        stockAdminClient.tradeBulkInsert(UserUtil.getUserId());
    }

    @BlueskyPreAuthorize
    @PostMapping("/dividends")
    public void dividendBulkInsert() {
        stockAdminClient.dividendBulkInsert(UserUtil.getUserId());
    }

    @BlueskyPreAuthorize
    @PostMapping("/price-histories")
    public void priceHistoriesUpdate() {
        stockAdminClient.priceHistoriesUpdate(UserUtil.getUserId());
    }
}
