package net.luversof.api.stock.service.kis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  private final String tokenPath = "/oauth2/tokenP";

  /** 사용자의 KIS 토큰을 조회하거나 만료된 경우(24시간) 재발급 받습니다. */
  public OpenApiConfig getValidConfig(UUID userId) {
    OpenApiConfig config =
        openApiConfigRepository
            .findByProviderAndUserId("KIS", userId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "KIS API 설정이 DB에 없습니다. (provider='KIS', userId=" + userId + ")"));

    Instant now = Instant.now();
    // 토큰이 없거나 발행된지 23시간이 넘었으면 재발급 (KIS 토큰 유효기간 24시간)
    if (config.getAccessToken() == null
        || config.getTokenUpdatedDate() == null
        || config.getTokenUpdatedDate().isBefore(now.minus(23, ChronoUnit.HOURS))) {

      renewToken(config);
    }

    return config;
  }

  /**
   * 발급 시각으로 적을 값. KIS 는 짧은 사이에 다시 요청하면 이미 발급한 토큰을 그대로 준다 - 그때 지금을 발급 시각으로 적으면 실제 만료보다 몇 시간 늦게까지 그
   * 토큰을 써서 그사이 갱신이 모두 실패했다(2026-10-27 감사). 응답의 남은 수명(expires_in, 초)이 있으면 "만료 - 24 시간" 을 적어 위의 23 시간
   * 판정이 실제 만료 1 시간 전에 걸리게 한다. 모르는 꼴이면 예전처럼 지금.
   */
  static Instant tokenIssuedAt(Instant now, Object expiresIn) {
    if (expiresIn != null) {
      try {
        long seconds = Long.parseLong(String.valueOf(expiresIn).trim());
        if (seconds > 0) {
          return now.plusSeconds(seconds).minus(24, ChronoUnit.HOURS);
        }
      } catch (NumberFormatException ignored) {
        // 숫자가 아니면 지금으로 둔다.
      }
    }
    return now;
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
        kisRestTemplate.postForEntity(baseUrl + tokenPath, request, Map.class);

    if (response.getBody() != null && response.getBody().containsKey("access_token")) {
      String newToken = (String) response.getBody().get("access_token");
      config.setAccessToken(newToken);
      config.setTokenUpdatedDate(
          tokenIssuedAt(Instant.now(), response.getBody().get("expires_in")));
      openApiConfigRepository.save(config);
    } else {
      throw new RuntimeException("KIS access token 발급에 실패했습니다: " + response.getBody());
    }
  }
}
