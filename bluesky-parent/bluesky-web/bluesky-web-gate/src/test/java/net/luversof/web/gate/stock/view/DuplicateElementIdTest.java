package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity;

/**
 * 렌더된 조각 안에 같은 id 가 두 번 나오지 않는지 본다.
 *
 * <p>중복 id 는 조용히 깨진다 &mdash; 브라우저는 오류를 내지 않고 {@code getElementById} 와 htmx 의 {@code hx-target} 이 늘
 * 첫 번째 것만 잡는다. 두 번째 요소는 클릭해도 아무 일이 없는 상태가 된다.
 *
 * <p>활동 화면은 특히 위험하다. {@code activityView} 가 {@code "all"} 이면 달력·타임라인·목록 세 뷰를 <b>한 번에</b> 그리는데, 세 뷰가
 * 같은 활동을 각자 렌더하므로 행 단위 id 가 겹치기 쉽다.
 */
class DuplicateElementIdTest {

  private static final UUID ACCOUNT_ID = UUID.fromString("01a0289d-8900-74b1-8d01-1e857fa3b2c6");
  private static final UUID STOCK_ITEM_ID = UUID.fromString("019d271d-ca42-7ad6-bd37-29cc9f7a0eef");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static Instant kstNoon(String localDate) {
    return Instant.parse(localDate + "T03:00:00Z");
  }

  private static Activity trade(String localDate, String tradeType, String amount) {
    return new Activity(
        "TRADE",
        STOCK_ITEM_ID,
        "RISE 200위클리커버드콜",
        tradeType,
        10,
        null,
        new BigDecimal(amount),
        kstNoon(localDate),
        List.of(ACCOUNT_ID));
  }

  private static Activity dividend(String localDate, String amount) {
    return new Activity(
        "DIVIDEND",
        STOCK_ITEM_ID,
        "RISE 200위클리커버드콜",
        null,
        null,
        "배당",
        new BigDecimal(amount),
        kstNoon(localDate),
        List.of(ACCOUNT_ID));
  }

  private String renderActivityList(String view, List<Activity> activities) {
    Map<String, Object> params = new HashMap<>();
    params.put("activities", activities);
    params.put("activityView", view);
    params.put("startDate", Instant.parse("2026-01-01T00:00:00Z"));
    params.put("endDate", Instant.parse("2026-09-01T00:00:00Z"));
    params.put("timeZone", "Asia/Seoul");
    params.put("rangeMode", "ytd");
    params.put("dataFirstDate", "2009-10-01");
    params.put("accountList", List.of());
    params.put("stockItemList", List.of());
    params.put("accountNames", Map.of(ACCOUNT_ID, "한국투자증권 ISA"));
    params.put("buyCount", 0L);
    params.put("sellCount", 0L);
    params.put("dividendCount", 0L);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/activityList.jte", params, output);
    return output.toString();
  }

  /** id="..." 를 모두 뽑아 등장 횟수를 센다. 빈 값과 null 문자열은 세지 않는다. */
  private static Map<String, Integer> idCounts(String html) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    String marker = "id=\"";
    for (int at = html.indexOf(marker); at >= 0; at = html.indexOf(marker, at + 1)) {
      int from = at + marker.length();
      int end = html.indexOf('"', from);
      if (end < 0) {
        break;
      }
      String id = html.substring(from, end);
      if (id.isEmpty() || "null".equals(id)) {
        continue;
      }
      counts.merge(id, 1, Integer::sum);
    }
    return counts;
  }

  private static List<String> duplicates(String html) {
    List<String> found = new ArrayList<>();
    idCounts(html)
        .forEach(
            (id, count) -> {
              if (count > 1) {
                found.add(id + " x" + count);
              }
            });
    return found;
  }

  private List<Activity> sample() {
    return List.of(
        trade("2026-08-03", "BUY", "100000"),
        trade("2026-08-03", "SELL", "70000"),
        trade("2026-07-15", "BUY", "50000"),
        dividend("2026-08-06", "1234"),
        dividend("2026-07-20", "999"));
  }

  @Test
  void 세_뷰를_한번에_그려도_id_가_겹치지_않는다() {
    String html = renderActivityList("all", sample());

    // 스캔이 조용히 0건이 되면 검사가 무력해진다.
    assertThat(idCounts(html)).as("id 를 하나도 찾지 못했다").hasSizeGreaterThan(5);
    assertThat(duplicates(html))
        .as("중복 id 는 오류 없이 깨진다 — getElementById 와 hx-target 이 첫 번째만 잡는다")
        .isEmpty();
  }

  @Test
  void 뷰별로_그려도_id_가_겹치지_않는다() {
    for (String view : List.of("calendar", "timeline", "list")) {
      assertThat(duplicates(renderActivityList(view, sample())))
          .as(view + " 뷰에 중복 id 가 있다")
          .isEmpty();
    }
  }

  @Test
  void 활동이_없어도_id_가_겹치지_않는다() {
    assertThat(duplicates(renderActivityList("all", List.of()))).isEmpty();
  }
}
