package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 운용사 상세에서 <b>총보수(연, %) · 상장일</b>을 읽는다(사용자 승인 2026-09-28, 프로필 열 두 개).
 *
 * <p>실측 2026-09-28(운용사마다 한 종목):
 *
 * <ul>
 *   <li>RISE: {@code /api/products/etfs/{fund_cd}/basic-info} 의 {@code fees.bosu_total}(0.3) ·
 *       {@code basic_info.listing_dt}("2025-09-02T00:00:00")
 *   <li>KODEX: {@code /api/v1/kodex/product/{id}.do} 의 {@code info.product.bosuInfo}("0.090% (…)")
 *       · {@code info.product.listD}("20240305")
 *   <li>TIGER · SOL · PLUS · TIME: 상세 HTML 의 "총보수"(PLUS 는 "보수 (연)") 칸과 "상장일" 칸
 * </ul>
 *
 * <p><b>못 읽으면 null 이다 &mdash; 예외를 올리지 않는다.</b> 이 값은 목록을 풍성하게 할 뿐이라 없다고 링크 등록이나 지급 이력 가져오기를 막으면 안
 * 된다. 0 으로 채우지도 않는다(0 은 "보수 없음" 으로 읽힌다).
 */
@Component
public class MonthlyDividendSourceFactsParser {

  /** 한 곳에서 읽은 두 값. 모르는 값은 null. */
  public record SourceFacts(BigDecimal totalExpenseRatioPct, LocalDate listingDate) {

    public static final SourceFacts NONE = new SourceFacts(null, null);
  }

  private static final Pattern PERCENT = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*%");

  /** TIME 처럼 세부 보수(운용 · 수탁 …)를 먼저 적고 끝에 "총 0.80%" 로 합계를 적는 곳이 있다 - 합계가 있으면 그것이 먼저다. */
  // 아래 한글은 화면 문구가 아니라 운용사 페이지의 칸 이름을 알아보는 데이터라 상수로 둔다(HardcodedKoreanTextTest 의 예외 모양).
  private static final String TOTAL_MARK = "총";

  private static final String EXPENSE_LABEL = "총보수";

  private static final String PLUS_EXPENSE_LABEL = "보수 (연)";

  private static final String PLUS_EXPENSE_LABEL_COMPACT = "보수(연)";

  private static final String LISTING_LABEL = "상장일";

  private static final Pattern TOTAL_PERCENT =
      Pattern.compile(TOTAL_MARK + "\\s*([0-9]+(?:\\.[0-9]+)?)\\s*%");

  private static final Pattern DATE =
      Pattern.compile("((?:19|20)[0-9]{2})\\s*[.\\-/]\\s*([0-9]{1,2})\\s*[.\\-/]\\s*([0-9]{1,2})");

  private static final Pattern COMPACT_DATE =
      Pattern.compile("^((?:19|20)[0-9]{2})([0-9]{2})([0-9]{2})");

  private static final Pattern SCRIPT_OR_STYLE =
      Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");

  private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");

  private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#([0-9]+);");

  /** 총보수 칸 이름. PLUS 는 "보수 (연)" 이라 적는다. */
  private static final List<String> EXPENSE_LABELS =
      List.of(EXPENSE_LABEL, PLUS_EXPENSE_LABEL, PLUS_EXPENSE_LABEL_COMPACT);

  private static final List<String> LISTING_LABELS = List.of(LISTING_LABEL);

  /** 칸 이름 뒤 값을 찾는 거리(글자). 표 한 칸 안이면 충분하고, 멀리 가면 다른 칸의 % 를 집는다. */
  private static final int EXPENSE_WINDOW = 200;

  private static final int LISTING_WINDOW = 40;

  private final JsonMapper objectMapper;

  public MonthlyDividendSourceFactsParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** TIGER · SOL · PLUS · TIME 상세 HTML. */
  public SourceFacts fromDetailHtml(String html) {
    String text = htmlToText(html);
    return new SourceFacts(expenseAfterLabels(text), listingAfterLabels(text));
  }

  /** RISE {@code basic-info} JSON. */
  public SourceFacts fromRiseBasicInfoJson(String json) {
    JsonNode root = readTree(json);
    if (root == null) {
      return SourceFacts.NONE;
    }
    JsonNode bosu = root.path("fees").path("bosu_total");
    BigDecimal expense = bosu.isNumber() ? sane(bosu.decimalValue()) : parsePercentText(bosu);
    return new SourceFacts(expense, parseDate(root.path("basic_info").path("listing_dt")));
  }

  /** KODEX {@code /api/v1/kodex/product/{id}.do} JSON. */
  public SourceFacts fromKodexProductJson(String json) {
    JsonNode root = readTree(json);
    if (root == null) {
      return SourceFacts.NONE;
    }
    JsonNode product = root.path("info").path("product");
    return new SourceFacts(
        parsePercentText(product.path("bosuInfo")), parseDate(product.path("listD")));
  }

  static String htmlToText(String html) {
    if (!StringUtils.hasText(html)) {
      return "";
    }
    String text = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
    text = TAG.matcher(text).replaceAll(" ");
    Matcher entity = NUMERIC_ENTITY.matcher(text);
    StringBuilder decoded = new StringBuilder();
    while (entity.find()) {
      int code = Integer.parseInt(entity.group(1));
      entity.appendReplacement(decoded, Matcher.quoteReplacement(String.valueOf((char) code)));
    }
    entity.appendTail(decoded);
    return decoded
        .toString()
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(' ', ' ')
        .replaceAll("\\s+", " ");
  }

  private static BigDecimal expenseAfterLabels(String text) {
    for (String label : EXPENSE_LABELS) {
      int from = 0;
      int at;
      while ((at = text.indexOf(label, from)) >= 0) {
        String window =
            text.substring(
                at + label.length(), Math.min(text.length(), at + label.length() + EXPENSE_WINDOW));
        Matcher total = TOTAL_PERCENT.matcher(window);
        BigDecimal value = total.find() ? sane(new BigDecimal(total.group(1))) : null;
        if (value == null) {
          Matcher first = PERCENT.matcher(window);
          value = first.find() ? sane(new BigDecimal(first.group(1))) : null;
        }
        if (value != null) {
          return value;
        }
        from = at + label.length();
      }
    }
    return null;
  }

  private static LocalDate listingAfterLabels(String text) {
    for (String label : LISTING_LABELS) {
      int from = 0;
      int at;
      // "상장일" 은 탭 이름 · 스크립트 설명에도 나온다 - 바로 뒤에 날짜가 있는 곳만 칸으로 본다.
      while ((at = text.indexOf(label, from)) >= 0) {
        String window =
            text.substring(
                at + label.length(), Math.min(text.length(), at + label.length() + LISTING_WINDOW));
        Matcher date = DATE.matcher(window);
        if (date.find()) {
          LocalDate value = toDate(date.group(1), date.group(2), date.group(3));
          if (value != null) {
            return value;
          }
        }
        from = at + label.length();
      }
    }
    return null;
  }

  private static BigDecimal parsePercentText(JsonNode node) {
    if (node == null || !node.isString()) {
      return null;
    }
    Matcher first = PERCENT.matcher(node.asString());
    return first.find() ? sane(new BigDecimal(first.group(1))) : null;
  }

  private static LocalDate parseDate(JsonNode node) {
    if (node == null || !node.isString() || !StringUtils.hasText(node.asString())) {
      return null;
    }
    String value = node.asString().trim();
    Matcher compact = COMPACT_DATE.matcher(value);
    if (compact.find()) {
      return toDate(compact.group(1), compact.group(2), compact.group(3));
    }
    Matcher date = DATE.matcher(value);
    return date.find() ? toDate(date.group(1), date.group(2), date.group(3)) : null;
  }

  private static LocalDate toDate(String year, String month, String day) {
    try {
      return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    } catch (DateTimeException | NumberFormatException ex) {
      return null;
    }
  }

  /** 총보수는 0 이상 100 미만(%)이어야 한다 - 밖이면 다른 칸을 집은 것이라 버린다. 저장 자릿수(소수 넷째)에 맞춘다. */
  static BigDecimal sane(BigDecimal value) {
    if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) >= 0) {
      return null;
    }
    return value.setScale(4, RoundingMode.HALF_UP);
  }

  private JsonNode readTree(String json) {
    if (!StringUtils.hasText(json)) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      return null;
    }
  }
}
