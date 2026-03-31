package net.luversof.api.stock.service.kis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.repository.OpenApiConfigRepository;

@Service
public class KisAuthService {

    @Autowired private OpenApiConfigRepository openApiConfigRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "https://openapi.koreainvestment.com:9443";
    private final String tokenPath = "/oauth2/tokenP";

    /** KIS 토큰을 조회하거나 만료된 경우(24시간) 재발급 받습니다. */
    public OpenApiConfig getValidConfig() {
        OpenApiConfig config =
                openApiConfigRepository
                        .findByProvider("KIS")
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "KIS API 설정이 DB에 없습니다. (provider='KIS')"));

        Instant now = Instant.now();
        // 토큰이 없거나 발행된지 23시간이 넘었으면 재발급 (KIS 토큰 유효기간 24시간)
        if (config.getAccessToken() == null
                || config.getTokenUpdatedDate() == null
                || config.getTokenUpdatedDate().isBefore(now.minus(23, ChronoUnit.HOURS))) {

            renewToken(config);
        }

        return config;
    }

    private void renewToken(OpenApiConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", config.getAppKey());
        body.put("appsecret", config.getAppSecret());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response =
                restTemplate.postForEntity(baseUrl + tokenPath, request, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("access_token")) {
            String newToken = (String) response.getBody().get("access_token");
            config.setAccessToken(newToken);
            config.setTokenUpdatedDate(Instant.now());
            openApiConfigRepository.save(config);
        } else {
            throw new RuntimeException("KIS access token 발급에 실패했습니다: " + response.getBody());
        }
    }
}
