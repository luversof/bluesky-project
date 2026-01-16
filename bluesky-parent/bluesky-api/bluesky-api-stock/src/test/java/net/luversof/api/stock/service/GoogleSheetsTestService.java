//package net.luversof.api.stock.service;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.GeneralSecurityException;
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import net.luversof.api.stock.GoogleSheetsApiCase;
//import net.luversof.api.stock.provider.InputStreamProvider;
//
//@Service
//public class GoogleSheetsTestService {
//	
//	@Autowired
//	private InputStreamProvider inputStreamProvider;
//	
//	@Autowired
//	private GoogleSheetsApiService googleSheetsApiService;
//
//	public String getSpreadsheetId() throws IOException {
//		InputStream credentialsStream = inputStreamProvider.open(GoogleSheetsApiCase.SPREADSHEET_ID_PATH);
//		return new String(credentialsStream.readAllBytes()).trim();
//	}
//	
//	@SuppressWarnings("unchecked")
//	public <T> List<T> getList(GoogleSheetsApiCase googleSheetsApiCase) {
//		try {
//			return (List<T>) googleSheetsApiService.getSpreadsheetValues(
//					GoogleSheetsApiCase.CREDENTIALS_FILE_PATH,
//					getSpreadsheetId(),
//					googleSheetsApiCase.getRange(),
//					googleSheetsApiCase.getType());
//		} catch (GeneralSecurityException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return List.of();
//		}
//	}
//}
