package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.web.gate.stock.util.KodexMonthlyDividendPayoutSourceParser.KodexDividendResponse;
import net.luversof.web.gate.stock.util.KodexMonthlyDividendPayoutSourceParser.KodexDividendRow;
import net.luversof.web.gate.stock.util.PlusMonthlyDividendPayoutSourceParser.PlusDividendPage;
import net.luversof.web.gate.stock.util.PlusMonthlyDividendPayoutSourceParser.PlusDividendRow;
import net.luversof.web.gate.stock.util.RiseMonthlyDividendPayoutSourceParser.RiseDividendResponse;
import net.luversof.web.gate.stock.util.RiseMonthlyDividendPayoutSourceParser.RiseDividendRow;
import net.luversof.web.gate.stock.util.SolMonthlyDividendPayoutSourceParser.SolDividendResponse;
import net.luversof.web.gate.stock.util.SolMonthlyDividendPayoutSourceParser.SolDividendRow;

@Component
public class MonthlyDividendPayoutSourceImportService {

  /**
   * PLUS 출처를 몇 페이지까지 따라갈지.
   *
   * <p><b>이 한계에 걸려 멈추면 조용히 잘린다</b> &mdash; 마지막 페이지 표시({@code last})를 보지 못한 채 반복문이 끝나고, 호출자는 짧아진 목록을
   * 정상 결과로 받는다. 그러면 예상 월배당의 12 개월 평균이 일부 달만으로 계산된다.
   *
   * <p>지금은 여유가 크다(실측 2026-08-24: PLUS 고배당주위클리고정커버드콜의 저장된 지급 이력 17 건). 그래서 한계를 바꾸지 않고 성질만 적어 둔다.
   *
   * <p>이 상수와 {@link #TIGER_PAGE_SIZE} 는 <b>검사로 고정돼 있지 않다</b>. 값을 1 로 낮춰도 게이트 검사 360 개가 모두 통과한다 (실측
   * 2026-08-24). 이 코드 경로는 호스트 이름으로 출처를 가려 내므로(예: {@code host.contains("plusetf.co.kr")}) 로컬 가짜 서버로는
   * 탈 수 없고, 그걸 태우려면 호스트 판정을 주입 가능하게 바꿔야 한다. 결함이 확인되지 않은 상태에서 운영 코드를 그렇게 바꾸지는 않았다.
   */
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(MonthlyDividendPayoutSourceImportService.class);

  private static final int PLUS_PAGE_SAFETY_LIMIT = 20;

  /** TIGER 출처에 한 번에 요청할 행 수. 위와 같은 이유로 검사로 고정돼 있지 않다. */
  private static final int TIGER_PAGE_SIZE = 200;

  private final RestClient restClient;

  private final MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser;

  private final MonthlyDividendSourceMetaParser monthlyDividendSourceMetaParser;

  private final PlusMonthlyDividendPayoutSourceParser plusMonthlyDividendPayoutSourceParser;

  private final RiseMonthlyDividendPayoutSourceParser riseMonthlyDividendPayoutSourceParser;

  private final SolMonthlyDividendPayoutSourceParser solMonthlyDividendPayoutSourceParser;

  private final KodexMonthlyDividendPayoutSourceParser kodexMonthlyDividendPayoutSourceParser;

  private final TigerMonthlyDividendPayoutSourceParser tigerMonthlyDividendPayoutSourceParser;

  public MonthlyDividendPayoutSourceImportService(
      RestClient.Builder restClientBuilder,
      MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser,
      MonthlyDividendSourceMetaParser monthlyDividendSourceMetaParser,
      KodexMonthlyDividendPayoutSourceParser kodexMonthlyDividendPayoutSourceParser,
      PlusMonthlyDividendPayoutSourceParser plusMonthlyDividendPayoutSourceParser,
      RiseMonthlyDividendPayoutSourceParser riseMonthlyDividendPayoutSourceParser,
      SolMonthlyDividendPayoutSourceParser solMonthlyDividendPayoutSourceParser,
      TigerMonthlyDividendPayoutSourceParser tigerMonthlyDividendPayoutSourceParser) {
    // 요청 팩터리를 갈아 끼우지 않는다. localdev 는 GateRestClientConfig 가 "모든 인증서 신뢰" 팩터리를 얹어 두는데
    // (로컬 서비스가 자체 서명), requestFactory(...) 로 덮으면 그 신뢰가 사라져 공개 사이트 인증서까지 못 믿는다
    // - 실측 2026-09-21: 삼성자산운용에서 PKIX path building failed 로 링크 등록이 실패했다(사용자 보고).
    // 타임아웃은 spring.http.clients.connect-timeout=3s / read-timeout=10s 로 이미 걸려 있다.
    this.restClient =
        restClientBuilder
            .defaultHeader(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0 Safari/537.36")
            .build();
    this.monthlyDividendPayoutImportParser = monthlyDividendPayoutImportParser;
    this.monthlyDividendSourceMetaParser = monthlyDividendSourceMetaParser;
    this.kodexMonthlyDividendPayoutSourceParser = kodexMonthlyDividendPayoutSourceParser;
    this.plusMonthlyDividendPayoutSourceParser = plusMonthlyDividendPayoutSourceParser;
    this.riseMonthlyDividendPayoutSourceParser = riseMonthlyDividendPayoutSourceParser;
    this.solMonthlyDividendPayoutSourceParser = solMonthlyDividendPayoutSourceParser;
    this.tigerMonthlyDividendPayoutSourceParser = tigerMonthlyDividendPayoutSourceParser;
  }

  /**
   * 출처 사이트에서 지급 이력을 받아 가져올 행으로 바꾼다.
   *
   * <p>표의 잘못된 행은 건너뛰고 사유와 함께 돌려준다({@link MonthlyDividendPayoutImportParser#parseLenient}) &mdash;
   * 사이트의 오타 한 줄 때문에 가져오기 전체가 실패하던 것을 막는다(실측 2026-09-17: RISE 44J2, 사용자 결정). 사람이 붙여넣는 가져오기는 여전히
   * 엄격하다.
   */
  public MonthlyDividendPayoutImportParser.LenientParseResult fetchImport(
      String symbol, String sourceUrl) {
    if (!StringUtils.hasText(sourceUrl)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.source.url.missing"));
    }

    URI sourceUri;
    try {
      sourceUri = URI.create(sourceUrl.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.url.invalid"), ex);
    }

    String host = sourceUri.getHost() != null ? sourceUri.getHost().toLowerCase(Locale.ROOT) : "";
    String bulkInput;
    if (RiseMonthlyDividendPayoutSourceParser.supportsHost(host)) {
      bulkInput = riseMonthlyDividendPayoutSourceParser.toBulkInput(fetchRiseRows(sourceUri));
    } else if (SolMonthlyDividendPayoutSourceParser.supportsHost(host)) {
      bulkInput = solMonthlyDividendPayoutSourceParser.toBulkInput(fetchSolRows(sourceUri));
    } else if (host.contains("plusetf.co.kr")) {
      bulkInput = plusMonthlyDividendPayoutSourceParser.toBulkInput(fetchPlusRows(sourceUri));
    } else if (host.contains("samsungfund.com")) {
      bulkInput = kodexMonthlyDividendPayoutSourceParser.toBulkInput(fetchKodexRows(sourceUri));
    } else if (host.contains("investments.miraeasset.com")
        && sourceUri.getPath() != null
        && sourceUri.getPath().contains("/tigeretf/")) {
      bulkInput = tigerMonthlyDividendPayoutSourceParser.toBulkInput(fetchTigerRows(sourceUri));
    } else {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.source.unsupported"));
    }

    return monthlyDividendPayoutImportParser.parseLenient(symbol, bulkInput);
  }

  /**
   * RISE 지급 이력. 상세 주소의 마지막 조각(펀드 코드)으로 화면이 쓰는 JSON 을 그대로 부른다.
   *
   * <p>예전에는 상세 HTML 의 표를 읽었는데 2026-09-21 사이트가 단일 페이지 앱으로 바뀌어 표가 서버 HTML 에 없다. 주소가 riseetf.co.kr 이든
   * kbam.co.kr 이든 펀드 코드는 같다.
   */
  private List<RiseDividendRow> fetchRiseRows(URI sourceUri) {
    String fundCode = RiseMonthlyDividendPayoutSourceParser.fundCodeFrom(sourceUri.getPath());
    if (!StringUtils.hasText(fundCode)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.id.missing", "RISE", "fund_cd"));
    }

    URI apiUri =
        UriComponentsBuilder.fromUriString("https://kbam.co.kr")
            .path("/api/products/etfs/{fundCode}/dividend")
            .build(fundCode);

    RiseDividendResponse response =
        riseMonthlyDividendPayoutSourceParser.parseResponse(
            fetchJsonBody(
                apiUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "RISE")));
    if (response.history() == null || response.history().isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "RISE"));
    }

    return response.history();
  }

  /**
   * SOL 지급 이력. 상세 화면의 "분배금 현황" 팝업이 부르는 것과 같은 주소다.
   *
   * <p>실측 2026-09-21: {@code /api/etf/pds/dividend/211097} 이 11 건을 준다(팝업 표와 같은 순서).
   */
  private List<SolDividendRow> fetchSolRows(URI sourceUri) {
    String fundCode = SolMonthlyDividendPayoutSourceParser.fundCodeFrom(sourceUri.getPath());
    if (!StringUtils.hasText(fundCode)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.id.missing", "SOL", "fundCd"));
    }

    URI apiUri =
        UriComponentsBuilder.fromUri(sourceUri)
            .replacePath("/api/etf/pds/dividend/{fundCode}")
            .replaceQuery(null)
            .build(fundCode);

    SolDividendResponse response =
        solMonthlyDividendPayoutSourceParser.parseResponse(
            fetchJsonBody(
                apiUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "SOL")));
    if (response.items() == null || response.items().isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "SOL"));
    }

    return response.items();
  }

  private List<KodexDividendRow> fetchKodexRows(URI sourceUri) {
    String productId =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("id");
    if (!StringUtils.hasText(productId)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.id.missing", "KODEX", "id"));
    }

    URI apiUri =
        UriComponentsBuilder.fromUri(sourceUri)
            .replacePath("/api/v1/kodex/divid-info.do")
            .replaceQuery(null)
            .queryParam("id", productId)
            .build(true)
            .toUri();

    KodexDividendResponse response =
        kodexMonthlyDividendPayoutSourceParser.parseResponse(
            fetchJsonBody(
                apiUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "KODEX")));
    if (response.dividList() == null || response.dividList().isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "KODEX"));
    }

    return response.dividList();
  }

  private List<PlusDividendRow> fetchPlusRows(URI sourceUri) {
    String productId =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("n");
    if (!StringUtils.hasText(productId)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.id.missing", "PLUS", "n"));
    }

    List<PlusDividendRow> rows = new ArrayList<>();
    for (int page = 0; page < PLUS_PAGE_SAFETY_LIMIT; page++) {
      URI apiUri =
          UriComponentsBuilder.fromUri(sourceUri)
              .replacePath("/api/v1/product/dividend/list")
              .replaceQuery(null)
              .queryParam("n", productId)
              .queryParam("page", page)
              .build(true)
              .toUri();

      PlusDividendPage response =
          plusMonthlyDividendPayoutSourceParser.parsePage(
              fetchJsonBody(
                  apiUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "PLUS")));
      if (response.content() == null || response.content().isEmpty()) {
        break;
      }

      rows.addAll(response.content());
      if (response.last()) {
        break;
      }
    }

    if (rows.isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "PLUS"));
    }

    return rows;
  }

  private String fetchTigerRows(URI sourceUri) {
    String ksdFund =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("ksdFund");
    if (!StringUtils.hasText(ksdFund)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.id.missing", "TIGER", "ksdFund"));
    }

    URI apiUri =
        UriComponentsBuilder.fromUri(sourceUri)
            .replacePath("/tigeretf/ko/product/search/detail/refDivAjax.ajax")
            .replaceQuery(null)
            .queryParam("ksdFund", ksdFund)
            .queryParam("pageIndex", 1)
            .queryParam("listCnt", TIGER_PAGE_SIZE)
            .build(true)
            .toUri();

    return fetchBody(
        apiUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "TIGER"));
  }

  /**
   * 링크에서 종목코드 · 이름만 읽는다(등록용). 지급 이력과 달리 페이지 한 번이면 끝난다.
   *
   * <p>TIGER 는 주소에 표준코드가 실려 있어 코드만 보면 되지만 이름은 제목에서 가져온다. RISE 는 상세가 단일 페이지 앱이라 header API 를 쓴다.
   */
  public MonthlyDividendSourceMetaParser.SourceMeta fetchMeta(URI sourceUri) {
    String host = sourceUri.getHost() != null ? sourceUri.getHost().toLowerCase(Locale.ROOT) : "";
    if (RiseMonthlyDividendPayoutSourceParser.supportsHost(host)) {
      String fundCode = RiseMonthlyDividendPayoutSourceParser.fundCodeFrom(sourceUri.getPath());
      if (!StringUtils.hasText(fundCode)) {
        throw new IllegalArgumentException(
            msg("stock.monthly.reference.error.source.id.missing", "RISE", "fund_cd"));
      }

      URI headerUri =
          UriComponentsBuilder.fromUriString("https://kbam.co.kr")
              .path("/api/products/etfs/{fundCode}/header")
              .build(fundCode);
      return monthlyDividendSourceMetaParser.fromRiseHeaderJson(
          fetchJsonBody(
              headerUri, msg("stock.monthly.reference.error.source.data.fetch.failed", "RISE")));
    }

    String html = fetchBody(sourceUri, msg("stock.monthly.reference.error.source.fetch.failed"));
    if (SolMonthlyDividendPayoutSourceParser.supportsHost(host)) {
      return monthlyDividendSourceMetaParser.fromSolHtml(html);
    }
    if (host.contains("plusetf.co.kr")) {
      return monthlyDividendSourceMetaParser.fromPlusHtml(html);
    }
    if (host.contains("samsungfund.com")) {
      return monthlyDividendSourceMetaParser.fromKodexHtml(html);
    }
    if (host.contains("investments.miraeasset.com")) {
      return monthlyDividendSourceMetaParser.fromTiger(sourceUri, html);
    }

    throw new IllegalArgumentException(msg("stock.monthly.reference.error.source.unsupported"));
  }

  private String fetchBody(URI uri, String errorMessage) {
    try {
      return restClient.get().uri(uri).retrieve().body(String.class);
    } catch (Exception ex) {
      // 화면에는 짧은 안내만 간다 - 까닭을 안 남기면 "가져오지 못했습니다" 하나로 끝나 무엇이 막혔는지 알 수 없다.
      log.warn("출처 페이지를 못 받았다: {}", uri, ex);
      throw new IllegalArgumentException(errorMessage, ex);
    }
  }

  private String fetchJsonBody(URI uri, String errorMessage) {
    try {
      return restClient
          .get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception ex) {
      log.warn("출처 데이터를 못 받았다: {}", uri, ex);
      throw new IllegalArgumentException(errorMessage, ex);
    }
  }
}
