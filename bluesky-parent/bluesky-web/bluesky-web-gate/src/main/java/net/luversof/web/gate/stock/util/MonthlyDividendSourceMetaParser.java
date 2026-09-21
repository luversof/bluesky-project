package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;

/**
 * 운용사 상세 링크에서 <b>종목코드와 종목 이름</b>을 읽는다(사용자 요청 2026-09-21: "링크만 주면 등록").
 *
 * <p>지급 이력 파서({@link MonthlyDividendPayoutSourceImportService})는 이미 네 곳을 다루지만 종목코드를 <b>받아서</b> 쓴다.
 * 링크만으로 등록하려면 코드와 이름을 링크 쪽에서 얻어야 해서 이 판이 따로 필요하다. 네 곳 모두 실측으로 확인했다(2026-09-21).
 *
 * <ul>
 *   <li>KODEX(samsungfund): 상세 HTML 안 JSON-LD 의 {@code "identifier": "476800"} · {@code "name"}
 *   <li>PLUS(plusetf): {@code <div class="summary__product-code">0018C0</div>} · {@code <title>… |
 *       PLUS ETF</title>}
 *   <li>TIGER(miraeasset): 주소의 {@code ksdFund=KR7329200000}(표준코드) 가운데 여섯 자리 · {@code <title>… | ETF
 *       상품 …</title>}
 *   <li>RISE(kbam): {@code /api/products/etfs/{fund_cd}/header} 의 {@code krx_cd} · {@code name}
 * </ul>
 */
@Component
public class MonthlyDividendSourceMetaParser {

  /** KODEX 상세는 검색엔진용 JSON-LD 에 코드와 이름을 함께 적어 둔다. */
  private static final Pattern KODEX_IDENTIFIER =
      Pattern.compile("\"identifier\"\\s*:\\s*\"([0-9A-Z]{6})\"");

  private static final Pattern KODEX_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

  private static final Pattern PLUS_CODE =
      Pattern.compile("class=\"summary__product-code\"\\s*>\\s*([0-9A-Z]{6})\\s*<");

  /**
   * SOL 상세는 제목 줄에 이름과 코드를 함께 적는다: {@code <h1 class="fv-name"><span>이름</span><small
   * class="fd-code">(코드)</small>}
   */
  private static final Pattern SOL_CODE = Pattern.compile("fd-code[^>]*>\\s*\\(?([0-9A-Z]{6})");

  private static final Pattern SOL_NAME =
      Pattern.compile("(?s)fv-name[^>]*>\\s*<span[^>]*>(.*?)</span>");

  private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

  /** 표준코드(ISIN) 가운데 여섯 자리가 단축코드다: KR7 <b>329200</b> 000. */
  private static final Pattern ISIN = Pattern.compile("(?i)KR[0-9A-Z]([0-9A-Z]{6})[0-9A-Z]{3}");

  private final JsonMapper objectMapper;

  public MonthlyDividendSourceMetaParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 종목코드와 이름. 둘 중 하나라도 비면 등록하지 않는다. */
  public record SourceMeta(String symbol, String name) {}

  public SourceMeta fromKodexHtml(String html) {
    String symbol = first(KODEX_IDENTIFIER, html);
    String name = first(KODEX_NAME, html);
    return require(symbol, name, "KODEX");
  }

  public SourceMeta fromPlusHtml(String html) {
    String symbol = first(PLUS_CODE, html);
    // 제목은 "PLUS 고배당주위클리고정커버드콜 | PLUS ETF" - 앞부분만 이름이다.
    String name = beforeBar(first(TITLE, html));
    return require(symbol, name, "PLUS");
  }

  /**
   * TIGER 는 주소에 표준코드가 실려 있어 페이지를 안 봐도 코드를 안다. 이름은 제목에서 가져온다.
   *
   * <p>제목은 "TIGER 리츠부동산인프라 | ETF 상품 | 미래에셋 TIGER ETF" 꼴이다.
   */
  public SourceMeta fromTiger(URI sourceUri, String html) {
    String query = sourceUri.getQuery() != null ? sourceUri.getQuery() : "";
    String symbol = first(ISIN, query);
    String name = beforeBar(first(TITLE, html));
    return require(symbol, name, "TIGER");
  }

  /**
   * SOL(신한자산운용). 상세 HTML 이 서버에서 그려지므로 페이지 한 번이면 이름 · 코드를 다 읽는다.
   *
   * <p>주소의 마지막 조각은 펀드 코드(211097)라 종목코드(0105E0)와 다르다 &mdash; 코드는 본문에서 읽어야 한다.
   */
  public SourceMeta fromSolHtml(String html) {
    String symbol = first(SOL_CODE, html);
    String name = first(SOL_NAME, html);
    return require(symbol, name, "SOL");
  }

  public SourceMeta fromRiseHeaderJson(String json) {
    RiseHeader header;
    try {
      header = objectMapper.readValue(json, RiseHeader.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.unparseable", "RISE"), ex);
    }
    return require(header.krxCode(), header.name(), "RISE");
  }

  private SourceMeta require(String symbol, String name, String source) {
    if (!StringUtils.hasText(symbol) || !StringUtils.hasText(name)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.link.meta.missing", source));
    }

    return new SourceMeta(symbol.trim().toUpperCase(Locale.ROOT), name.trim());
  }

  private String first(Pattern pattern, String text) {
    if (!StringUtils.hasText(text)) {
      return "";
    }

    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  /** "이름 | 사이트" 에서 앞부분. 구분 기호가 없으면 통째로 이름이다. */
  private String beforeBar(String title) {
    if (!StringUtils.hasText(title)) {
      return "";
    }

    String normalized = title.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    int bar = normalized.indexOf('|');
    return (bar > 0 ? normalized.substring(0, bar) : normalized).trim();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RiseHeader(@JsonProperty("krx_cd") String krxCode, String name) {}
}
