package net.luversof.api.stock;

import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;

public enum GoogleSheetsApiCase {
	
	GoogleSheetsStockItem(0, "주식 검색!A1:E", GoogleSheetStockItem.class),
	GoogleSheetsDividend(0, "주식 배당 기록!A1:J", GoogleSheetDividend.class),
	GoogleSheetsTrade(1, "주식 매매 기록!A1:S", GoogleSheetTrade.class)
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

	public static final String CREDENTIALS_FILE_PATH = "file:/D:/dev/credentials.json";
	public static final String SPREADSHEET_ID_PATH = "file:/D:/dev/spreadsheet_id.txt";
}
