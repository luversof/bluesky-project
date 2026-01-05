package net.luversof.api.stock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.api.services.sheets.v4.model.ValueRange;

import net.luversof.GeneralTest;
import net.luversof.api.stock.provider.InputStreamProvider;
import net.luversof.api.stock.service.GoogleSheetsApiService;

class GoogleSheetsApiTest implements GeneralTest {

	@Autowired
	private GoogleSheetsApiService googleSheetsApiService;
	
	@Autowired
	private InputStreamProvider inputStreamProvider;

	private static final Logger log = LoggerFactory.getLogger(GoogleSheetsApiTest.class);

	@Test
	void getSpreadsheetIdTest() throws IOException {
		String spreadsheetId = getSpreadsheetId();
		assertNotNull(spreadsheetId);
		log.debug("spreadsheetId: {}", spreadsheetId);
	}
	
	String getSpreadsheetId() throws IOException {
		InputStream credentialsStream = inputStreamProvider.open(GoogleSheetsApiCase.SPREADSHEET_ID_PATH);
		return new String(credentialsStream.readAllBytes()).trim();
	}

	@ParameterizedTest
	@EnumSource(GoogleSheetsApiCase.class)
	void googleSheetsApiTest(GoogleSheetsApiCase googleSheetsApiCase) throws IOException, GeneralSecurityException {
		if (!googleSheetsApiCase.isEnabled()) {
			return;
		}
		
		String spreadsheetId = getSpreadsheetId();
		var resultList = googleSheetsApiService.getSpreadsheetValues(
				GoogleSheetsApiCase.CREDENTIALS_FILE_PATH,
				spreadsheetId,
				googleSheetsApiCase.getRange(),
				googleSheetsApiCase.getType());

		assertNotNull(resultList);
		if (resultList == null || resultList.isEmpty()) {
			log.debug("No data found.");
		} else {
			resultList.forEach(item -> log.debug("{}", item) );
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
