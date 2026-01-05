package net.luversof.api.stock;

import net.luversof.api.stock.domain.GoogleSheetsBaseStockItem;
import net.luversof.api.stock.domain.GoogleSheetsDividendItem;

public enum GoogleSheetsApiCase {
	GoogleSheetsBaseStockItem(0, "주식 검색!A1:E", GoogleSheetsBaseStockItem.class),
	GoogleSheetsDividendItem(1, "주식 배당 기록!A1:J", GoogleSheetsDividendItem.class)
	
	;
	
	private int enabled;
	private String range;
	private Class<?> type;
	
	private GoogleSheetsApiCase(int enabled, String range, Class<?> type) {
		this.enabled = enabled;
		this.range = range;
		this.type = type;
	}
	
	public boolean isEnabled() {
		return enabled == 1;
	}
	
	public String getRange() {
		return range;
	}
	
	public Class<?> getType() {
		return type;
	}
	
}
