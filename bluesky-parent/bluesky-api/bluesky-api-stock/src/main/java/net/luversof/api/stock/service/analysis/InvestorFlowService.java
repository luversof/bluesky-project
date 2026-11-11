package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.api.stock.domain.OpenApiConfig;

/**
 * 종목별 수급: 외국인 · 기관 순매수 대금(KIS 주식현재가 투자자, FHKST01010900). 국내 종목은 외국인 지분이 클수록 간밤 미국 장과 같이 움직인다는 연구가
 * 있어(리서치 2026-11-11) 예측 입력에 붙인다. KIS 는 최근 약 30 거래일만 주므로 그보다 오래된 리플레이 기준일에는 null 이다.
 */
@Service
public class InvestorFlowService {

  private static final Logger log = LoggerFactory.getLogger(InvestorFlowService.class);

  private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  /** 순매수 대금 단위는 백만 원. sum5d: 기준일까지 최근 5 거래일 합. */
  public record InvestorFlow(
      LocalDate lastDate,
      BigDecimal foreignNetBuy1d,
      BigDecimal institutionNetBuy1d,
      BigDecimal foreignNetBuy5d,
      BigDecimal institutionNetBuy5d) {}

  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  /** 실패하면 null - 수급이 없어도 예측은 나머지로 한다. */
  public InvestorFlow fetch(OpenApiConfig config, String symbol, LocalDate asOf) {
    if (config == null) {
      return null;
    }
    try {
      return flowOf(fetchRows(config, symbol), asOf);
    } catch (RuntimeException e) {
      log.warn("investor flow {} failed: {}", symbol, e.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> fetchRows(OpenApiConfig config, String symbol) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("authorization", "Bearer " + config.getAccessToken());
    headers.set("appkey", config.getAppKey());
    headers.set("appsecret", config.getAppSecret());
    headers.set("custtype", "P");
    headers.set("tr_id", "FHKST01010900");
    String url =
        UriComponentsBuilder.fromUriString(
                baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-investor")
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", symbol)
            .build()
            .toUriString();
    Map<String, Object> body =
        kisRestTemplate
            .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
            .getBody();
    if (body == null) {
      return List.of();
    }
    Object rtCd = body.get("rt_cd");
    if (rtCd != null && !"0".equals(rtCd)) {
      throw new IllegalStateException("KIS " + body.get("msg_cd") + " " + body.get("msg1"));
    }
    Object rows = body.get("output");
    return rows instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
  }

  /** asOf(null 이면 최신)까지의 행으로 1 · 5 거래일 순매수. 장중 행(대금 비어 있음)과 asOf 뒤 행은 뺀다. 행이 없으면 null. */
  static InvestorFlow flowOf(List<Map<String, Object>> rows, LocalDate asOf) {
    List<Object[]> days = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Object date = row.get("stck_bsop_date");
      BigDecimal foreign = decimal(row.get("frgn_ntby_tr_pbmn"));
      BigDecimal institution = decimal(row.get("orgn_ntby_tr_pbmn"));
      if (date == null || foreign == null || institution == null) {
        continue;
      }
      LocalDate d = LocalDate.parse(String.valueOf(date), KIS_DATE);
      if (asOf == null || !d.isAfter(asOf)) {
        days.add(new Object[] {d, foreign, institution});
      }
    }
    if (days.isEmpty()) {
      return null;
    }
    days.sort(Comparator.comparing((Object[] r) -> (LocalDate) r[0]).reversed());
    BigDecimal f5 = BigDecimal.ZERO;
    BigDecimal i5 = BigDecimal.ZERO;
    for (Object[] r : days.subList(0, Math.min(5, days.size()))) {
      f5 = f5.add((BigDecimal) r[1]);
      i5 = i5.add((BigDecimal) r[2]);
    }
    Object[] last = days.get(0);
    return new InvestorFlow(
        (LocalDate) last[0], (BigDecimal) last[1], (BigDecimal) last[2], f5, i5);
  }

  private static BigDecimal decimal(Object value) {
    if (value == null || String.valueOf(value).isBlank()) {
      return null;
    }
    return new BigDecimal(String.valueOf(value).trim());
  }
}
