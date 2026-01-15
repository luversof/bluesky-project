package net.luversof.app.google.constant;

import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;

/**
 * 저장할 info type 정의
 */
public enum GoogleSpreadSheetInfoType {

	STOCK_DIVIDEND(GoogleSheetDividend.class),
	STOCK_STOCKITEM(GoogleSheetStockItem.class),
	STOCK_TRADE(GoogleSheetTrade.class)
	;
	
	private Class<?> targetClass;
	
	private GoogleSpreadSheetInfoType(Class<?> targetClass) {
		this.targetClass = targetClass;
	}
	
	public Class<?> getTargetClass() {
		return targetClass;
	}
}
