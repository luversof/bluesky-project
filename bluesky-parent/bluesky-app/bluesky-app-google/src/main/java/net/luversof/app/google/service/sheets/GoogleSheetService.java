package net.luversof.app.google.service.sheets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import net.luversof.app.google.service.GoogleIamServiceAccountInfoService;
import net.luversof.app.google.service.GoogleSpreadSheetInfoService;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GoogleSheetService {

    @Autowired private JsonMapper jsonMapper;

    @Autowired private GoogleSpreadSheetInfoService googleSpreadSheetInfoService;

    @Autowired private GoogleIamServiceAccountInfoService googleIamServiceAccountInfoService;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @SuppressWarnings("unchecked")
    public <T> List<T> getSpreadSheetValueList(UUID userId, GoogleSpreadSheetInfoType type) {
        var googleIamServiceAccountInfo = googleIamServiceAccountInfoService.findByUserId(userId);
        var googleSpreadSheetInfo =
                googleSpreadSheetInfoService.findByGoogleIamServiceAccountInfoIdAndType(
                        googleIamServiceAccountInfo.getId(), type);

        GoogleCredentials googleCredentials = null;
        try {
            googleCredentials =
                    ServiceAccountCredentials.fromStream(
                            new ByteArrayInputStream(
                                    googleIamServiceAccountInfo
                                            .getKeyStr()
                                            .getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        Sheets sheets = null;
        try {
            sheets =
                    new Sheets.Builder(
                                    GoogleNetHttpTransport.newTrustedTransport(),
                                    JSON_FACTORY,
                                    new HttpCredentialsAdapter(googleCredentials))
                            .setApplicationName("bluesky-project")
                            .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        ValueRange valueRange = null;
        try {
            valueRange =
                    sheets.spreadsheets()
                            .values()
                            .get(
                                    googleSpreadSheetInfo.getSpreadsheetId(),
                                    googleSpreadSheetInfo.getRange())
                            .execute();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        List<List<Object>> values = valueRange.getValues();
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> header = values.get(0);
        System.out.println("====== GOOGLE SHEET HEADERS ======");
        for (Object h : header) {
            System.out.println("[" + h + "]");
        }
        System.out.println("==================================");
        List<T> result = new ArrayList<>();

        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            Map<String, Object> map = new HashMap<>();
            for (int j = 0; j < header.size(); j++) {
                if (row.size() > j) {
                    String h = String.valueOf(header.get(j)).replaceAll("\\s+", " ").trim();
                    map.put(h, row.get(j));
                    if (h.contains("매도") && h.contains("손익")) {
                        map.put("매도 실현 손익", row.get(j));
                    }
                }
            }
            result.add((T) jsonMapper.convertValue(map, type.getTargetClass()));
        }

        return result;
    }
}
