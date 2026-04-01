package net.luversof.app.google.stock.service;

import java.util.List;
import java.util.UUID;
import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import net.luversof.app.google.service.sheets.GoogleSheetService;
import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockGoogleSheetService {

    @Autowired private GoogleSheetService googleSheetService;

    public List<GoogleSheetDividend> getGoogleSheetDividendList(UUID userId) {
        return googleSheetService.getSpreadSheetValueList(
                userId, GoogleSpreadSheetInfoType.STOCK_DIVIDEND);
    }

    public List<GoogleSheetStockItem> getGoogleSheetStockItemList(UUID userId) {
        return googleSheetService.getSpreadSheetValueList(
                userId, GoogleSpreadSheetInfoType.STOCK_STOCKITEM);
    }

    public List<GoogleSheetTrade> getGoogleSheetTradeList(UUID userId) {
        return googleSheetService.getSpreadSheetValueList(
                userId, GoogleSpreadSheetInfoType.STOCK_TRADE);
    }
}
