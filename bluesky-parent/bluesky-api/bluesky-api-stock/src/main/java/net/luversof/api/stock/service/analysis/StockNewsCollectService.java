package net.luversof.api.stock.service.analysis;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockNews;
import net.luversof.api.stock.repository.OpenApiConfigRepository;
import net.luversof.api.stock.repository.StockNewsRepository;

/**
 * 분석 대상 종목의 뉴스(네이버 증권 종목 뉴스)와 공시(DART OpenAPI)를 모은다.
 *
 * <p>뉴스는 키가 없다. DART 키는 KIS 와 같이 OpenApiConfig 에 둔다: provider='DART'(appKey=인증키, appSecret 은 쓰지
 * 않음). 키가 없으면 건너뛰고 결과에 적는다.
 */
@Service
public class StockNewsCollectService {

  private static final Logger log = LoggerFactory.getLogger(StockNewsCollectService.class);

  // shortcut: 네이버 증권 화면이 쓰는 비공식 주소다(키 불필요, 사용자 결정 2026-11-09) - 형식이 바뀌거나 막히면 종목별 실패로 남고 예측은 지표만으로
  // 돈다.
  static final String NAVER_NEWS_URL = "https://stock.naver.com/api/domestic/detail/news";
  static final String NAVER_ARTICLE_URL = "https://n.news.naver.com/mnews/article/";
  static final DateTimeFormatter NAVER_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
  static final int NAVER_PAGE_SIZE = 15;

  /**
   * 네이버 종목 뉴스 API 의 상한(실측 2026-11-11: page 100 · pageSize 20 을 넘으면 400). 그래서 되채우기는 종목마다 최근 2000
   * 묶음까지다 - 기사가 많은 대형주는 며칠, ETF 는 수개월을 거슬러 간다.
   */
  static final int NAVER_MAX_PAGE = 100;

  static final int NAVER_MAX_PAGE_SIZE = 20;
  static final String DART_LIST_URL = "https://opendart.fss.or.kr/api/list.json";
  static final String DART_VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

  /** 분석이 보는 기간보다 하루 넉넉히 모은다. */
  static final int COLLECT_DAYS = 3;

  /** DART 는 corp_code 없이 기간 전체를 받아 종목코드로 거른다. 하루 공시가 수천 건이라 쪽 수에 상한을 둔다. */
  static final int DART_MAX_PAGES = 100;

  @Autowired private OpenApiConfigRepository openApiConfigRepository;

  @Autowired private StockNewsRepository stockNewsRepository;

  // 타임아웃이 걸린 외부 API 용 RestTemplate(이름은 KIS 용이지만 설정은 일반적이다).
  @Autowired private RestTemplate kisRestTemplate;

  public record CollectResult(int naverSaved, int dartSaved, List<String> messages) {}

  /** oldestReached: 마지막으로 읽은 기사 날짜(from 까지 못 갔으면 그보다 늦다). */
  public record BackfillResult(String symbol, int pages, int saved, LocalDate oldestReached) {}

  /**
   * 과거 뉴스 되채우기(사용자 요청 2026-11-11): 리플레이가 과거 기준일에도 뉴스를 보게 한다. 네이버 증권 종목 뉴스는 쪽을 넘기면 과거로 간다 - from 보다
   * 오래된 기사가 나오거나 maxPages 에 닿을 때까지 읽는다. 기사가 많은 종목(대형주는 하루 30 쪽 넘게)은 쪽을 건너뛰어 하루에 몇 쪽만 읽는다 - 입력은 종목마다
   * 최근 15 건만 쓰므로 그날의 일부면 된다.
   */
  public BackfillResult backfill(StockItem item, LocalDate from, int maxPages) {
    Instant fromInstant = from.atStartOfDay(PredictionMath.MARKET_ZONE_ID).toInstant();
    Set<String> existingKeys =
        stockNewsRepository.findByPublishedDateAfter(fromInstant.minus(1, ChronoUnit.DAYS)).stream()
            .map(StockNewsCollectService::key)
            .collect(Collectors.toCollection(HashSet::new));
    int page = 1;
    int pages = 0;
    int stride = 1;
    int saved = 0;
    Instant oldest = null;
    while (pages < maxPages && page <= NAVER_MAX_PAGE) {
      URI uri =
          UriComponentsBuilder.fromUriString(NAVER_NEWS_URL)
              .queryParam("itemCode", item.getSymbol())
              .queryParam("page", page)
              .queryParam("pageSize", NAVER_MAX_PAGE_SIZE)
              .build()
              .encode()
              .toUri();
      @SuppressWarnings("unchecked")
      Map<String, Object> body = kisRestTemplate.getForObject(uri, Map.class);
      pages++;
      List<StockNews> news = naverClustersToNews(item.getId(), body);
      if (news.isEmpty()) {
        break;
      }
      List<StockNews> inRange =
          news.stream().filter(n -> !n.getPublishedDate().isBefore(fromInstant)).toList();
      saved += saveNew(inRange, existingKeys);
      Instant newest = news.stream().map(StockNews::getPublishedDate).max(Instant::compareTo).get();
      Instant pageOldest =
          news.stream().map(StockNews::getPublishedDate).min(Instant::compareTo).get();
      oldest = pageOldest;
      if (pageOldest.isBefore(fromInstant)) {
        break;
      }
      stride = nextStride(stride, java.time.Duration.between(pageOldest, newest));
      page += stride;
      try {
        // 비공식 화면 API 라 천천히 읽는다.
        Thread.sleep(150);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    return new BackfillResult(
        item.getSymbol(),
        pages,
        saved,
        oldest == null ? null : oldest.atZone(PredictionMath.MARKET_ZONE_ID).toLocalDate());
  }

  /** 한 쪽이 몇 시간을 덮는지로 다음 보폭을 정한다: 6 시간보다 짧으면 두 배(하루 몇 쪽만 읽게), 하루보다 길면 다시 한 쪽씩. */
  static int nextStride(int stride, java.time.Duration pageSpan) {
    if (pageSpan.compareTo(java.time.Duration.ofHours(6)) < 0) {
      return Math.min(stride * 2, 64);
    }
    if (pageSpan.compareTo(java.time.Duration.ofDays(1)) > 0) {
      return 1;
    }
    return stride;
  }

  public CollectResult collect(UUID userId, List<StockItem> targets) {
    List<String> messages = new ArrayList<>();
    Instant since = Instant.now().minus(COLLECT_DAYS, ChronoUnit.DAYS);
    Set<String> existingKeys =
        stockNewsRepository.findByPublishedDateAfter(since.minus(1, ChronoUnit.DAYS)).stream()
            .map(StockNewsCollectService::key)
            .collect(Collectors.toCollection(HashSet::new));

    int naverSaved = saveNew(collectNaver(targets, since, messages), existingKeys);

    int dartSaved =
        openApiConfigRepository
            .findByProviderAndUserId("DART", userId)
            .map(config -> saveNew(collectDart(config, targets, messages), existingKeys))
            .orElseGet(() -> {
              messages.add("DART: OpenApiConfig(provider='DART') 없음 - 건너뜀");
              return 0;
            });

    return new CollectResult(naverSaved, dartSaved, messages);
  }

  private int saveNew(List<StockNews> collected, Set<String> existingKeys) {
    int saved = 0;
    for (StockNews news : collected) {
      if (existingKeys.add(key(news))) {
        stockNewsRepository.save(news);
        saved++;
      }
    }
    return saved;
  }

  private static String key(StockNews news) {
    return news.getStockItemId() + "|" + news.getSource() + "|" + news.getExternalId();
  }

  private List<StockNews> collectNaver(
      List<StockItem> targets, Instant since, List<String> messages) {
    List<StockNews> result = new ArrayList<>();
    for (StockItem item : targets) {
      URI uri =
          UriComponentsBuilder.fromUriString(NAVER_NEWS_URL)
              .queryParam("itemCode", item.getSymbol())
              .queryParam("page", 1)
              .queryParam("pageSize", NAVER_PAGE_SIZE)
              .build()
              .encode()
              .toUri();
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = kisRestTemplate.getForObject(uri, Map.class);
        for (StockNews news : naverClustersToNews(item.getId(), body)) {
          if (news.getPublishedDate().isAfter(since)) {
            result.add(news);
          }
        }
      } catch (RuntimeException e) {
        log.warn("naver news fetch failed: {}", item.getSymbol(), e);
        messages.add("NAVER 실패: " + item.getSymbol() + " - " + e.getMessage());
      }
    }
    return result;
  }

  /** 비슷한 기사는 묶음(cluster)으로 온다 - 묶음마다 첫 기사(대표) 하나만 쓴다. */
  @SuppressWarnings("unchecked")
  static List<StockNews> naverClustersToNews(UUID stockItemId, Map<String, Object> body) {
    List<StockNews> result = new ArrayList<>();
    Object clusters = body == null ? null : body.get("clusters");
    if (!(clusters instanceof List<?> clusterList)) {
      return result;
    }
    for (Object cluster : clusterList) {
      Object items = cluster instanceof Map<?, ?> map ? map.get("items") : null;
      if (items instanceof List<?> itemList
          && !itemList.isEmpty()
          && itemList.get(0) instanceof Map<?, ?> first) {
        StockNews news = naverToNews(stockItemId, (Map<String, Object>) first);
        if (news != null) {
          result.add(news);
        }
      }
    }
    return result;
  }

  static StockNews naverToNews(UUID stockItemId, Map<String, Object> raw) {
    String officeId = stringOrNull(raw.get("officeId"));
    String articleId = stringOrNull(raw.get("articleId"));
    String datetime = stringOrNull(raw.get("datetime"));
    String title = stripHtml(stringOrNull(raw.get("title")));
    if (officeId == null
        || articleId == null
        || datetime == null
        || title == null
        || title.isEmpty()) {
      return null;
    }
    StockNews news = new StockNews();
    news.setStockItemId(stockItemId);
    news.setSource(StockNews.SOURCE_NAVER);
    news.setExternalId(officeId + "/" + articleId);
    news.setUrl(NAVER_ARTICLE_URL + officeId + "/" + articleId);
    news.setTitle(truncate(title, 500));
    String office = stringOrNull(raw.get("officeName"));
    String body = stripHtml(stringOrNull(raw.get("body")));
    news.setSummary(
        truncate(office == null ? body : "[" + office + "] " + (body == null ? "" : body), 2000));
    news.setPublishedDate(
        java.time.LocalDateTime.parse(datetime, NAVER_DATETIME)
            .atZone(PredictionMath.MARKET_ZONE_ID)
            .toInstant());
    return news;
  }

  private List<StockNews> collectDart(
      OpenApiConfig config, List<StockItem> targets, List<String> messages) {
    Map<String, StockItem> bySymbol =
        targets.stream()
            .collect(Collectors.toMap(StockItem::getSymbol, Function.identity(), (a, b) -> a));
    LocalDate today = LocalDate.now(PredictionMath.MARKET_ZONE_ID);
    DateTimeFormatter yyyymmdd = DateTimeFormatter.BASIC_ISO_DATE;

    List<StockNews> result = new ArrayList<>();
    int page = 1;
    int totalPage = 1;
    while (page <= totalPage && page <= DART_MAX_PAGES) {
      URI uri =
          UriComponentsBuilder.fromUriString(DART_LIST_URL)
              .queryParam("crtfc_key", config.getAppKey())
              .queryParam("bgn_de", today.minusDays(COLLECT_DAYS - 1L).format(yyyymmdd))
              .queryParam("end_de", today.format(yyyymmdd))
              .queryParam("page_no", page)
              .queryParam("page_count", 100)
              .build()
              .encode()
              .toUri();
      Map<String, Object> body;
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = kisRestTemplate.getForObject(uri, Map.class);
        body = response;
      } catch (RuntimeException e) {
        // 키가 URL 에 실리므로 예외 메시지(URL 포함 가능)는 로그에만 남긴다.
        log.warn("dart list fetch failed: page {}", page, e);
        messages.add("DART 실패: page " + page);
        break;
      }
      String status = body == null ? null : stringOrNull(body.get("status"));
      if (!"000".equals(status)) {
        // 013 = 조회된 데이터 없음
        if (!"013".equals(status)) {
          messages.add("DART status " + status + ": " + (body == null ? "" : body.get("message")));
        }
        break;
      }
      totalPage = ((Number) body.getOrDefault("total_page", 1)).intValue();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
      for (Map<String, Object> raw : list == null ? List.<Map<String, Object>>of() : list) {
        StockItem item = bySymbol.get(stringOrNull(raw.get("stock_code")));
        if (item != null) {
          result.add(dartToNews(item.getId(), raw));
        }
      }
      page++;
    }
    if (totalPage > DART_MAX_PAGES) {
      messages.add("DART: 쪽 상한(" + DART_MAX_PAGES + ") 도달 - 오래된 공시 일부 누락");
    }
    return result;
  }

  static StockNews dartToNews(UUID stockItemId, Map<String, Object> raw) {
    String rceptNo = stringOrNull(raw.get("rcept_no"));
    StockNews news = new StockNews();
    news.setStockItemId(stockItemId);
    news.setSource(StockNews.SOURCE_DART);
    news.setExternalId(rceptNo);
    news.setUrl(DART_VIEWER_URL + rceptNo);
    news.setTitle(truncate(stringOrNull(raw.get("report_nm")), 500));
    news.setSummary(truncate(stringOrNull(raw.get("flr_nm")), 2000));
    // 접수일만 온다 - 그날 00:00(KST)으로 둔다.
    news.setPublishedDate(
        LocalDate.parse(stringOrNull(raw.get("rcept_dt")), DateTimeFormatter.BASIC_ISO_DATE)
            .atStartOfDay(PredictionMath.MARKET_ZONE_ID)
            .toInstant());
    return news;
  }

  /** 네이버 검색 결과의 &lt;b&gt; 강조와 HTML 엔티티를 걷어낸다. */
  static String stripHtml(String text) {
    if (text == null) {
      return null;
    }
    return text.replaceAll("<[^>]*>", "")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .trim();
  }

  private static String stringOrNull(Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? null : text;
  }

  private static String truncate(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }
}
