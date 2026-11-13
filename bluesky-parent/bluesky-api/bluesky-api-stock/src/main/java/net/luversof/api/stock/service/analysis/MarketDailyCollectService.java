package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockMarketDaily;
import net.luversof.api.stock.repository.StockMarketDailyRepository;
import net.luversof.api.stock.service.kis.KisAuthService;

/**
 * 시장 일별 시세 수집(2026-11-13): KOSPI200 선물 최근월물의 주간 종가와 야간 종가(KIS 국내선물옵션 기간별시세, FHKIF03020100 - 시장 F 주간
 * · CM 야간). 야간 거래는 그날 18:00 에 열려 다음 날 05:00 에 닫혀 간밤 미국 장에 대한 한국 시장의 반응을 담는다 - 리플레이(삼성전자 245 일)에서 다음
 * 날 변화율과 상관 0.60, SOX(0.36)를 뺀 뒤에도 0.53.
 */
@Service
public class MarketDailyCollectService {

  private static final Logger log = LoggerFactory.getLogger(MarketDailyCollectService.class);

  private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  /** 2025 년까지의 KOSPI200 선물 코드 연도 글자(101 + 글자 + 월). 2026 년부터는 새 코드(A01 + 연도 끝자리 + 월). */
  private static final Map<Integer, Character> YEAR_LETTERS =
      Map.of(2022, 'S', 2023, 'T', 2024, 'V', 2025, 'W');

  /** 주간 종가는 장 마감(15:45) 뒤에 확정 - 그 전 당일 행은 넣지 않는다. */
  static final LocalTime DAY_SETTLED_AT = LocalTime.of(16, 0);

  public record CollectResult(int contracts, int saved, LocalDate from, LocalDate to) {}

  /** 선물 한 종목의 주간 · 야간 종가. */
  record Contract(
      String code,
      LocalDate expiry,
      Map<LocalDate, BigDecimal> day,
      Map<LocalDate, BigDecimal> night) {}

  @Autowired private RestTemplate kisRestTemplate;

  @Autowired private KisAuthService kisAuthService;

  @Autowired private StockMarketDailyRepository stockMarketDailyRepository;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  /** 최근분: 지난 2 주. 매일 돌리면 빈 날이 생기지 않는다. */
  public CollectResult collectRecent(UUID userId) {
    LocalDate today = LocalDate.now(PredictionMath.MARKET_ZONE_ID);
    return backfill(userId, today.minusDays(14), today);
  }

  public CollectResult backfill(UUID userId, LocalDate from, LocalDate to) {
    OpenApiConfig config = kisAuthService.getValidConfig(userId);
    List<Contract> contracts = new ArrayList<>();
    // from 이 속한 분기물부터 to 다음 분기물까지 - 만기 전후 겹치는 날을 다음 물로 넘기려면 둘 다 필요하다.
    LocalDate month = quarterMonth(from);
    while (!month.isAfter(quarterMonth(to).plusMonths(3))) {
      LocalDate expiry = expiryOf(month);
      LocalDate start = expiryOf(month.minusMonths(3)).minusDays(7);
      LocalDate a = start.isAfter(from) ? start : from;
      LocalDate b = expiry.isBefore(to) ? expiry : to;
      if (!a.isAfter(b)) {
        String code = contractCode(month);
        contracts.add(
            new Contract(
                code, expiry, fetch(config, "F", code, a, b), fetch(config, "CM", code, a, b)));
      }
      month = month.plusMonths(3);
    }
    List<StockMarketDaily> rows = rowsOf(contracts, from, to, now());
    upsert(rows);
    return new CollectResult(contracts.size(), rows.size(), from, to);
  }

  private static ZonedDateTime now() {
    return ZonedDateTime.now(PredictionMath.MARKET_ZONE_ID);
  }

  /**
   * 날짜마다 그날 뒤에 만기인 가장 가까운 종목(최근월물)의 주간 · 야간 종가. 만기일에는 그 종목의 야간 거래가 없어 다음 물로 넘어간다. 확정 전 값(당일 장중 주간
   * 종가, 아직 열려 있는 야간 거래)은 뺀다.
   */
  static List<StockMarketDaily> rowsOf(
      List<Contract> contracts, LocalDate from, LocalDate to, ZonedDateTime now) {
    LocalDate today = now.toLocalDate();
    LocalDate lastDay = now.toLocalTime().isBefore(DAY_SETTLED_AT) ? today.minusDays(1) : today;
    Map<LocalDate, Contract> front = new TreeMap<>();
    for (Contract c : contracts) {
      for (LocalDate d : c.day().keySet()) {
        if (d.isBefore(from)
            || d.isAfter(to)
            || !c.expiry().isAfter(d)
            || !c.night().containsKey(d)) {
          continue;
        }
        front.merge(d, c, (x, y) -> x.expiry().isBefore(y.expiry()) ? x : y);
      }
    }
    List<StockMarketDaily> rows = new ArrayList<>();
    front.forEach((d, c) -> {
      if (!d.isAfter(lastDay)) {
        rows.add(row(StockMarketDaily.K200F_DAY, d, c.day().get(d), c.code()));
      }
      // 야간 거래는 다음 날 05:00 에 닫힌다 - 오늘 날짜의 야간은 아직 열려 있다.
      if (d.isBefore(today)) {
        rows.add(row(StockMarketDaily.K200F_NIGHT, d, c.night().get(d), c.code()));
      }
    });
    return rows;
  }

  private static StockMarketDaily row(String code, LocalDate d, BigDecimal close, String contract) {
    StockMarketDaily row = new StockMarketDaily();
    row.setCode(code);
    row.setTradeDate(d);
    row.setClosePrice(close);
    row.setContract(contract);
    return row;
  }

  /** 분기 월(3 · 6 · 9 · 12)의 1 일: d 가 그 달 만기일 뒤면 다음 분기. */
  static LocalDate quarterMonth(LocalDate d) {
    LocalDate month = d.withDayOfMonth(1).plusMonths((3 - d.getMonthValue() % 3) % 3);
    return d.isAfter(expiryOf(month)) ? month.plusMonths(3) : month;
  }

  /** 만기일 = 그 달 두 번째 목요일(휴장이면 앞당겨지지만 여기선 무시 - 실제 마지막 거래일은 KIS 행으로 맞춰진다). */
  static LocalDate expiryOf(LocalDate month) {
    return month.withDayOfMonth(1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.THURSDAY));
  }

  static String contractCode(LocalDate month) {
    int year = month.getYear();
    if (year >= 2026) {
      return "A01%d%02d".formatted(year % 10, month.getMonthValue());
    }
    Character letter = YEAR_LETTERS.get(year);
    if (letter == null) {
      throw new IllegalArgumentException("no KOSPI200 futures code for " + year);
    }
    return "101%c%02d".formatted(letter, month.getMonthValue());
  }

  @SuppressWarnings("unchecked")
  private Map<LocalDate, BigDecimal> fetch(
      OpenApiConfig config, String market, String code, LocalDate from, LocalDate to) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("authorization", "Bearer " + config.getAccessToken());
    headers.set("appkey", config.getAppKey());
    headers.set("appsecret", config.getAppSecret());
    headers.set("custtype", "P");
    headers.set("tr_id", "FHKIF03020100");
    String url =
        UriComponentsBuilder.fromUriString(
                baseUrl + "/uapi/domestic-futureoption/v1/quotations/inquire-daily-fuopchartprice")
            .queryParam("FID_COND_MRKT_DIV_CODE", market)
            .queryParam("FID_INPUT_ISCD", code)
            .queryParam("FID_INPUT_DATE_1", from.format(KIS_DATE))
            .queryParam("FID_INPUT_DATE_2", to.format(KIS_DATE))
            .queryParam("FID_PERIOD_DIV_CODE", "D")
            .build()
            .toUriString();
    for (int attempt = 1; ; attempt++) {
      Map<String, Object> body;
      try {
        body =
            kisRestTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
                .getBody();
      } catch (RestClientException e) {
        log.warn("market daily {} {} failed: {}", market, code, e.getMessage());
        return Map.of();
      }
      sleep(150);
      if (body != null && "EGW00201".equals(body.get("msg_cd")) && attempt < 5) {
        sleep(1000);
        continue;
      }
      Map<LocalDate, BigDecimal> result = new HashMap<>();
      if (body != null && body.get("output2") instanceof List<?> list) {
        for (Object o : list) {
          Map<String, Object> row = (Map<String, Object>) o;
          Object date = row.get("stck_bsop_date");
          Object close = row.get("futs_prpr");
          if (date != null && close != null && !close.toString().isBlank()) {
            result.put(
                LocalDate.parse(date.toString(), KIS_DATE), new BigDecimal(close.toString()));
          }
        }
      }
      return result;
    }
  }

  private void upsert(List<StockMarketDaily> rows) {
    if (rows.isEmpty()) {
      return;
    }
    LocalDate min =
        rows.stream().map(StockMarketDaily::getTradeDate).min(Comparator.naturalOrder()).get();
    LocalDate max =
        rows.stream().map(StockMarketDaily::getTradeDate).max(Comparator.naturalOrder()).get();
    Map<String, Map<LocalDate, StockMarketDaily>> existing = new HashMap<>();
    for (String code : List.of(StockMarketDaily.K200F_DAY, StockMarketDaily.K200F_NIGHT)) {
      existing.put(
          code,
          stockMarketDailyRepository
              .findByCodeAndTradeDateBetweenOrderByTradeDate(code, min, max)
              .stream()
              .collect(Collectors.toMap(StockMarketDaily::getTradeDate, Function.identity())));
    }
    rows.forEach(r -> {
      StockMarketDaily old = existing.get(r.getCode()).get(r.getTradeDate());
      if (old != null) {
        r.setId(old.getId());
      }
    });
    stockMarketDailyRepository.saveAll(rows);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
