package net.luversof.web.gate.stock.util;

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

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.util.KodexMonthlyDividendPayoutSourceParser.KodexDividendResponse;
import net.luversof.web.gate.stock.util.KodexMonthlyDividendPayoutSourceParser.KodexDividendRow;
import net.luversof.web.gate.stock.util.PlusMonthlyDividendPayoutSourceParser.PlusDividendPage;
import net.luversof.web.gate.stock.util.PlusMonthlyDividendPayoutSourceParser.PlusDividendRow;

@Component
public class MonthlyDividendPayoutSourceImportService {

  private static final int PLUS_PAGE_SAFETY_LIMIT = 20;
  private static final int TIGER_PAGE_SIZE = 200;

  private final RestClient restClient;

  private final MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser;

  private final PlusMonthlyDividendPayoutSourceParser plusMonthlyDividendPayoutSourceParser;

  private final RiseMonthlyDividendPayoutSourceParser riseMonthlyDividendPayoutSourceParser;

  private final KodexMonthlyDividendPayoutSourceParser kodexMonthlyDividendPayoutSourceParser;

  private final TigerMonthlyDividendPayoutSourceParser tigerMonthlyDividendPayoutSourceParser;

  public MonthlyDividendPayoutSourceImportService(
      RestClient.Builder restClientBuilder,
      MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser,
      KodexMonthlyDividendPayoutSourceParser kodexMonthlyDividendPayoutSourceParser,
      PlusMonthlyDividendPayoutSourceParser plusMonthlyDividendPayoutSourceParser,
      RiseMonthlyDividendPayoutSourceParser riseMonthlyDividendPayoutSourceParser,
      TigerMonthlyDividendPayoutSourceParser tigerMonthlyDividendPayoutSourceParser) {
    this.restClient =
        restClientBuilder
            .defaultHeader(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0 Safari/537.36")
            .build();
    this.monthlyDividendPayoutImportParser = monthlyDividendPayoutImportParser;
    this.kodexMonthlyDividendPayoutSourceParser = kodexMonthlyDividendPayoutSourceParser;
    this.plusMonthlyDividendPayoutSourceParser = plusMonthlyDividendPayoutSourceParser;
    this.riseMonthlyDividendPayoutSourceParser = riseMonthlyDividendPayoutSourceParser;
    this.tigerMonthlyDividendPayoutSourceParser = tigerMonthlyDividendPayoutSourceParser;
  }

  public List<MonthlyDividendPayoutUpsertRequest> fetchImportRequests(
      String symbol, String sourceUrl) {
    if (!StringUtils.hasText(sourceUrl)) {
      throw new IllegalArgumentException("저장된 출처 URL이 없습니다.");
    }

    URI sourceUri;
    try {
      sourceUri = URI.create(sourceUrl.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("저장된 출처 URL 형식이 올바르지 않습니다.", ex);
    }

    String host = sourceUri.getHost() != null ? sourceUri.getHost().toLowerCase(Locale.ROOT) : "";
    String bulkInput;
    if (host.contains("riseetf.co.kr")) {
      bulkInput =
          riseMonthlyDividendPayoutSourceParser.toBulkInput(
              fetchBody(sourceUri, "출처 URL에서 지급 이력을 가져오지 못했습니다. 주소와 공개 여부를 확인해 주세요."));
    } else if (host.contains("plusetf.co.kr")) {
      bulkInput = plusMonthlyDividendPayoutSourceParser.toBulkInput(fetchPlusRows(sourceUri));
    } else if (host.contains("samsungfund.com")) {
      bulkInput = kodexMonthlyDividendPayoutSourceParser.toBulkInput(fetchKodexRows(sourceUri));
    } else if (host.contains("investments.miraeasset.com")
        && sourceUri.getPath() != null
        && sourceUri.getPath().contains("/tigeretf/")) {
      bulkInput = tigerMonthlyDividendPayoutSourceParser.toBulkInput(fetchTigerRows(sourceUri));
    } else {
      throw new IllegalArgumentException(
          "현재 자동 가져오기는 RISE ETF, PLUS ETF, KODEX ETF, TIGER ETF 출처만 지원합니다.");
    }

    return monthlyDividendPayoutImportParser.parse(symbol, bulkInput);
  }

  private List<KodexDividendRow> fetchKodexRows(URI sourceUri) {
    String productId =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("id");
    if (!StringUtils.hasText(productId)) {
      throw new IllegalArgumentException("KODEX ETF 출처 URL에서 상품 식별자(id)를 찾지 못했습니다.");
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
            fetchJsonBody(apiUri, "KODEX ETF 출처에서 지급 이력 데이터를 가져오지 못했습니다."));
    if (response.dividList() == null || response.dividList().isEmpty()) {
      throw new IllegalArgumentException("KODEX ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    return response.dividList();
  }

  private List<PlusDividendRow> fetchPlusRows(URI sourceUri) {
    String productId =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("n");
    if (!StringUtils.hasText(productId)) {
      throw new IllegalArgumentException("PLUS ETF 출처 URL에서 상품 식별자(n)를 찾지 못했습니다.");
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
              fetchJsonBody(apiUri, "PLUS ETF 출처에서 지급 이력 데이터를 가져오지 못했습니다."));
      if (response.content() == null || response.content().isEmpty()) {
        break;
      }

      rows.addAll(response.content());
      if (response.last()) {
        break;
      }
    }

    if (rows.isEmpty()) {
      throw new IllegalArgumentException("PLUS ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    return rows;
  }

  private String fetchTigerRows(URI sourceUri) {
    String ksdFund =
        UriComponentsBuilder.fromUri(sourceUri).build().getQueryParams().getFirst("ksdFund");
    if (!StringUtils.hasText(ksdFund)) {
      throw new IllegalArgumentException("TIGER ETF 출처 URL에서 상품 식별자(ksdFund)를 찾지 못했습니다.");
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

    return fetchBody(apiUri, "TIGER ETF 출처에서 지급 이력 데이터를 가져오지 못했습니다.");
  }

  private String fetchBody(URI uri, String errorMessage) {
    try {
      return restClient.get().uri(uri).retrieve().body(String.class);
    } catch (Exception ex) {
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
      throw new IllegalArgumentException(errorMessage, ex);
    }
  }
}
