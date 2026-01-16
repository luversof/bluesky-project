package net.luversof.app.google.stock.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import net.luversof.app.google.service.sheets.GoogleSheetService;
import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;

@Service
public class StockGoogleSheetService {

	@Autowired
	private GoogleSheetService googleSheetService;
	
	public List<GoogleSheetDividend> getGoogleSheetDividendList(@NonNull UUID userId) {
		return googleSheetService.getSpreadSheetValueList(userId, GoogleSpreadSheetInfoType.STOCK_DIVIDEND);
	}
	
	public List<GoogleSheetStockItem>getGoogleSheetStockItemList(@NonNull UUID userId) {
		return googleSheetService.getSpreadSheetValueList(userId, GoogleSpreadSheetInfoType.STOCK_STOCKITEM);
	} 
	
	public List<GoogleSheetTrade> getGoogleSheetTradeList(@NonNull UUID userId) {
		return googleSheetService.getSpreadSheetValueList(userId, GoogleSpreadSheetInfoType.STOCK_TRADE);
	}

}
