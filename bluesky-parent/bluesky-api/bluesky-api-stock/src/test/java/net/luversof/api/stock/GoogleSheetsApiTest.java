package net.luversof.api.stock;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.api.services.sheets.v4.model.ValueRange;

import net.luversof.GeneralTest;
import net.luversof.api.stock.service.GoogleSheetsApiService;

public class GoogleSheetsApiTest implements GeneralTest {

	@Autowired
	private GoogleSheetsApiService googleSheetsApiService;

	private static final Logger log = LoggerFactory.getLogger(GoogleSheetsApiTest.class);

	private static final String CREDENTIALS_FILE_PATH = "file:/C:/Users/bluesky/Downloads/credentials.json";

	@ParameterizedTest
	@EnumSource(GoogleSheetsApiCase.class)
	void googleSheetsApiTest(GoogleSheetsApiCase googleSheetsApiCase) throws IOException, GeneralSecurityException {
		final String spreadsheetId = "";
		var resultList = googleSheetsApiService.getSpreadsheetValues(
				CREDENTIALS_FILE_PATH,
				spreadsheetId,
				googleSheetsApiCase.getRange(),
				googleSheetsApiCase.getType());

		if (resultList == null || resultList.isEmpty()) {
			log.debug("No data found.");
		} else {
			log.debug("{}", resultList);
		}
	}

	public static record StockItem(String 종목코드, String 종목이름, String 현재가) {
	}

	public static class StockItemRowMapper implements Function<ValueRange, List<StockItem>> {

		@Override
		public List<StockItem> apply(ValueRange valueRange) {
			if (valueRange == null || valueRange.getValues() == null) {
				return Collections.emptyList();
			}
			return valueRange.getValues().stream().map(row -> new StockItem(
					row.get(0).toString(),
					row.get(1).toString(),
					row.get(2).toString())).toList();
		}

	}
}
