package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import net.luversof.api.stock.service.kis.KisAuthService;

/**
 * 시황 분석의 시장 상황 블록: 간밤 미국 시장 · 변동성 · 금리 · 환율 · 국내 지수의 1 · 5 거래일 변화율.
 *
 * <p>사용자 요청 2026-11-10: 종목 뉴스만으로는 판단 근거가 좁다 - 특히 보유 종목 다수가 ETF 라 기초지수 · 환율이 방향을 정한다. 이미 쓰는 KIS 키로
 * 하루 한 번 8 개를 부른다(일별 시세). 한 지표가 실패해도 나머지는 싣고 실패한 것은 unavailable 에 적는다 - 예측은 그 사실을 알고 판단한다.
 */
@Service
public class MarketContextService {

  private static final Logger log = LoggerFactory.getLogger(MarketContextService.class);

  private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  /** 아침 작업이 입력을 만들 때 한 번, 예측 저장 때 한 번 - 그 사이 KIS 를 다시 부르지 않는다. */
  private static final Duration CACHE_TTL = Duration.ofMinutes(30);

  /** 1 · 5 거래일 변화율을 내려면 6 거래일이 필요하다 - 미국 연휴가 끼어도 남게 넉넉히. */
  private static final int LOOKBACK_DAYS = 20;

  /** KIS 일별 시세: OVERSEAS = 해외지수 · 환율 · 금리(FHKST03030100), DOMESTIC = 국내 업종지수(FHKUP03500100). */
  enum Kind {
    OVERSEAS,
    DOMESTIC
  }

  /** marketCode: 해외는 N(지수) · X(환율) · I(금리), 국내는 U(업종). */
  record Def(String key, String name, Kind kind, String marketCode, String code) {}

  static final List<Def> DEFS =
      List.of(
          new Def("SPX", "S&P 500", Kind.OVERSEAS, "N", "SPX"),
          new Def("COMP", "NASDAQ", Kind.OVERSEAS, "N", "COMP"),
          new Def("SOX", "PHLX Semiconductor", Kind.OVERSEAS, "N", "SOX"),
          new Def("VIX", "VIX", Kind.OVERSEAS, "N", "VIX"),
          new Def("USDKRW", "USD/KRW", Kind.OVERSEAS, "X", "FX@KRW"),
          new Def("US10Y", "US 10Y", Kind.OVERSEAS, "I", "Y0202"),
          new Def("KOSPI", "KOSPI", Kind.DOMESTIC, "U", "0001"),
          new Def("KOSDAQ", "KOSDAQ", Kind.DOMESTIC, "U", "1001"));

  public record MarketLine(
      String key,
      String name,
      LocalDate lastDate,
      BigDecimal close,
      BigDecimal change1dPct,
      BigDecimal change5dPct) {}

  public record MarketContext(
      Instant fetchedAt, List<MarketLine> lines, List<String> unavailable) {}

  @Autowired private KisAuthService kisAuthService;
  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  private volatile MarketContext cached;

  /** 리플레이(지난 기준일) 결과 - 지난 날의 일별 시세는 바뀌지 않으니 TTL 없이 둔다. */
  private final java.util.Map<String, MarketContext> pastCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  /** 마지막으로 가져온 시장 상황(없으면 null). 예측 저장 때 입력 기록에 함께 남긴다. */
  public MarketContext lastFetched() {
    return cached;
  }

  public MarketContext fetch(UUID userId) {
    return fetch(userId, null);
  }

  /** 저녁 분석 기준의 리플레이(아래 morning=false). */
  public MarketContext fetch(UUID userId, LocalDate asOf) {
    return fetch(userId, asOf, false);
  }

  /**
   * asOf: 리플레이 기준일. 국내 지수는 asOf 까지. 해외는 저녁 분석(asOf 20:00 KST)이면 asOf 전날(미국 날짜)까지 - asOf 날짜의 미국 장은
   * 한국 시간으로 다음 날 새벽에 끝난다. morning(다음 거래일 08:00 KST, 아침 작업 시각)이면 asOf 날짜의 미국 장까지 들어간다. null 이면 지금.
   */
  public MarketContext fetch(UUID userId, LocalDate asOf, boolean morning) {
    if (asOf != null) {
      String key = asOf + (morning ? "@morning" : "@evening");
      MarketContext past = pastCache.get(key);
      if (past != null) {
        return past;
      }
      MarketContext result = load(userId, asOf, morning);
      if (!result.lines().isEmpty()) {
        pastCache.put(key, result);
      }
      return result;
    }
    MarketContext hit = cached;
    if (hit != null && hit.fetchedAt().isAfter(Instant.now().minus(CACHE_TTL))) {
      return hit;
    }
    MarketContext result = load(userId, null, false);
    if (!result.lines().isEmpty()) {
      cached = result;
    }
    return result;
  }

  private MarketContext load(UUID userId, LocalDate asOf, boolean morning) {
    if (userId == null) {
      return new MarketContext(Instant.now(), List.of(), DEFS.stream().map(Def::key).toList());
    }
    OpenApiConfig config;
    try {
      config = kisAuthService.getValidConfig(userId);
    } catch (RuntimeException e) {
      log.warn("market context: KIS config unavailable", e);
      return new MarketContext(Instant.now(), List.of(), DEFS.stream().map(Def::key).toList());
    }
    LocalDate today = asOf != null ? asOf : LocalDate.now(PredictionMath.MARKET_ZONE_ID);
    List<MarketLine> lines = new ArrayList<>();
    List<String> unavailable = new ArrayList<>();
    for (Def def : DEFS) {
      try {
        LocalDate end =
            asOf != null && !morning && def.kind() == Kind.OVERSEAS ? today.minusDays(1) : today;
        MarketLine line = lineOf(def, fetchRows(config, def, end.minusDays(LOOKBACK_DAYS), end));
        if (line == null) {
          unavailable.add(def.key());
        } else {
          lines.add(line);
        }
        // shortcut: 초당 한도(EGW00201)를 피하려고 고정 간격만 둔다, 지표를 크게 늘리면 KisStockPriceUpdateService 의 재시도로
        // 옮긴다.
        Thread.sleep(150);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (RuntimeException e) {
        log.warn("market context: {} failed: {}", def.key(), e.getMessage());
        unavailable.add(def.key());
      }
    }
    return new MarketContext(Instant.now(), lines, unavailable);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> fetchRows(
      OpenApiConfig config, Def def, LocalDate start, LocalDate end) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("authorization", "Bearer " + config.getAccessToken());
    headers.set("appkey", config.getAppKey());
    headers.set("appsecret", config.getAppSecret());
    headers.set("custtype", "P");
    boolean overseas = def.kind() == Kind.OVERSEAS;
    headers.set("tr_id", overseas ? "FHKST03030100" : "FHKUP03500100");
    String path =
        overseas
            ? "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice"
            : "/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice";
    String url =
        UriComponentsBuilder.fromUriString(baseUrl + path)
            .queryParam("FID_COND_MRKT_DIV_CODE", def.marketCode())
            .queryParam("FID_INPUT_ISCD", def.code())
            .queryParam("FID_INPUT_DATE_1", start.format(KIS_DATE))
            .queryParam("FID_INPUT_DATE_2", end.format(KIS_DATE))
            .queryParam("FID_PERIOD_DIV_CODE", "D")
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
    Object rows = body.get("output2");
    return rows instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
  }

  /** 일별 행 → 마지막 날 종가와 1 · 5 거래일 변화율. 종가가 없거나 0 인 행(휴장 자리)은 뺀다. 행이 2 개 미만이면 null. */
  static MarketLine lineOf(Def def, List<Map<String, Object>> rows) {
    List<Map.Entry<LocalDate, BigDecimal>> closes = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Object date = row.get("stck_bsop_date");
      Object close = row.get(def.kind() == Kind.OVERSEAS ? "ovrs_nmix_prpr" : "bstp_nmix_prpr");
      if (date == null || close == null || String.valueOf(close).isBlank()) {
        continue;
      }
      BigDecimal value = new BigDecimal(String.valueOf(close).trim());
      if (value.signum() > 0) {
        closes.add(Map.entry(LocalDate.parse(String.valueOf(date), KIS_DATE), value));
      }
    }
    closes.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));
    if (closes.size() < 2) {
      return null;
    }
    int last = closes.size() - 1;
    BigDecimal close = closes.get(last).getValue();
    return new MarketLine(
        def.key(),
        def.name(),
        closes.get(last).getKey(),
        close,
        PredictionMath.changePct(closes.get(last - 1).getValue(), close),
        last >= 5 ? PredictionMath.changePct(closes.get(last - 5).getValue(), close) : null);
  }
}
