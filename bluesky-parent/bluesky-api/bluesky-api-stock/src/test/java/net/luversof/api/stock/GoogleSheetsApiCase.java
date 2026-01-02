package net.luversof.api.stock;

import net.luversof.api.stock.domain.GoogleSheetsBaseStockItem;

public enum GoogleSheetsApiCase {
	GoogleSheetsBaseStockItem("주식 검색!A1:E", GoogleSheetsBaseStockItem.class)
	
	;
	
	private String range;
	private Class<?> type;
	
	private GoogleSheetsApiCase(String range, Class<?> type) {
		this.range = range;
		this.type = type;
	}
	
	public String getRange() {
		return range;
	}
	
	public Class<?> getType() {
		return type;
	}
	
}
