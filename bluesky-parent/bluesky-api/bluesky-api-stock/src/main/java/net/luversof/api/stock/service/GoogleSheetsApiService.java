package net.luversof.api.stock.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import net.luversof.api.stock.provider.InputStreamProvider;

@Service
public class GoogleSheetsApiService {

	private InputStreamProvider inputStreamProvider;

	private ObjectMapper objectMapper;
	
	public GoogleSheetsApiService(InputStreamProvider inputStreamProvider, ObjectMapper objectMapper) {
		this.inputStreamProvider = inputStreamProvider;
		this.objectMapper = objectMapper;
	}

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	private GoogleCredentials getCredentials(String credentialJsonLocation) throws IOException {
		InputStream credentialsStream = inputStreamProvider.open(credentialJsonLocation);
		ServiceAccountCredentials serviceAccountCredentials = ServiceAccountCredentials.fromStream(credentialsStream);

		return serviceAccountCredentials.createScoped(SheetsScopes.SPREADSHEETS_READONLY);
	}

	private Sheets getSheets(String credentialJsonLocation) throws GeneralSecurityException, IOException {
		return new Sheets.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JSON_FACTORY,

				new HttpCredentialsAdapter(getCredentials(credentialJsonLocation)))
				.build();
	}

	public ValueRange getSpreadsheetValues(String credentialJsonLocation, String spreadsheetId, String range)
			throws GeneralSecurityException, IOException {
		return getSheets(credentialJsonLocation)
				.spreadsheets()
				.values()
				.get(spreadsheetId, range)
				.execute();
	}

	public <T> List<T> getSpreadsheetValues(String credentialJsonLocation, String spreadsheetId, String range,
			Function<ValueRange, List<T>> rowMapper) throws GeneralSecurityException, IOException {
		return rowMapper.apply(getSpreadsheetValues(credentialJsonLocation, spreadsheetId, range));
	}

	public <T> List<T> getSpreadsheetValues(String credentialJsonLocation, String spreadsheetId, String range,
			Class<T> type) throws GeneralSecurityException, IOException {
		ValueRange response = getSpreadsheetValues(credentialJsonLocation, spreadsheetId, range);
		List<List<Object>> values = response.getValues();

		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}

		List<Object> header = values.get(0);
		List<T> result = new ArrayList<>();

		for (int i = 1; i < values.size(); i++) {
			List<Object> row = values.get(i);
			Map<String, Object> map = new HashMap<>();
			for (int j = 0; j < header.size(); j++) {
				if (row.size() > j) {
					map.put(String.valueOf(header.get(j)), row.get(j));
				}
			}
			result.add(objectMapper.convertValue(map, type));
		}

		return result;
	}

}
