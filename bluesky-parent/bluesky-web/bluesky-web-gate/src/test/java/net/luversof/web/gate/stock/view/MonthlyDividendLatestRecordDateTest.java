package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendLatestPayoutView;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;

/**
 * 관리 &gt; 월배당 기준 데이터 목록의 "최근 지급 기준일" 열.
 *
 * <p>사용자 요청 2026-10-13: 종목마다 가장 최근 지급 기준일을 목록에서 보면 지급 이력을 갱신했는지 확인하기 좋다. 그 전에는 종목을 하나씩 열어(지급 이력
 * 펼치기) 봐야 했다. 실측 2026-10-13: 21 종목 중 월중 9 종목은 09-15, 월말 12 종목은 09-30 이 마지막이었다.
 */
class MonthlyDividendLatestRecordDateTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static MonthlyDividendPayoutResponse payout(String symbol, String record, String pay) {
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol + " 이름",
        record == null ? null : LocalDate.parse(record),
        pay == null ? null : LocalDate.parse(pay),
        null,
        null,
        null,
        null);
  }

  private static MonthlyDividendProfileResponse profile(String symbol, int displayOrder) {
    return new MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol + " 이름",
        "",
        "MONTH_END",
        displayOrder,
        true,
        "",
        null,
        Instant.parse("2026-09-01T00:00:00Z"),
        null,
        null);
  }

  @Test
  void 종목마다_가장_늦은_기준일을_고르고_같은_날이면_지급일이_늦은_행이다() {
    Map<String, MonthlyDividendLatestPayoutView> latest =
        MonthlyDividendReferenceSupport.latestPayoutBySymbol(
            Arrays.asList(
                payout("aaa ", "2026-08-29", "2026-09-02"),
                payout("AAA", "2026-09-30", "2026-10-01"),
                payout("AAA", "2026-09-30", "2026-10-02"),
                payout("AAA", "2026-07-31", "2026-08-04"),
                payout("BBB", "2026-09-15", null),
                payout("CCC", null, "2026-09-17"),
                payout(null, "2026-10-01", "2026-10-02"),
                null),
            TODAY);

    assertThat(latest).containsOnlyKeys("AAA", "BBB");
    assertThat(latest.get("AAA").recordDate()).isEqualTo(LocalDate.parse("2026-09-30"));
    assertThat(latest.get("AAA").payDate()).isEqualTo(LocalDate.parse("2026-10-02"));
    assertThat(latest.get("AAA").daysSinceRecord()).isEqualTo(13);
    assertThat(latest.get("BBB").payDate()).isNull();
    assertThat(MonthlyDividendReferenceSupport.latestPayoutBySymbol(null, TODAY)).isEmpty();
  }

  @Test
  void 기준일이_35일을_넘기면_갱신_확인_대상이다() {
    assertThat(MonthlyDividendLatestPayoutView.of(TODAY.minusDays(35), null, TODAY).stale())
        .as("35 일은 아직 기다릴 만하다")
        .isFalse();
    assertThat(MonthlyDividendLatestPayoutView.of(TODAY.minusDays(36), null, TODAY).stale())
        .isTrue();
  }

  /**
   * 기준일 규칙이 선 종목은 규칙의 기준일이 지나면 바로 갱신 확인(2026-10-20) - 월말 종목은 10-30(금) 기준일이 지난 10-31 부터, 35
   * 일(11-04)을 기다리지 않는다. 기준일 전날까지는 아니다.
   */
  @Test
  void 규칙_종목은_새_기준일이_지나면_바로_갱신_확인() {
    List<MonthlyDividendPayoutResponse> history =
        Arrays.asList(
            payout("END1", "2026-07-31", "2026-08-04"),
            payout("END1", "2026-08-31", "2026-09-02"),
            payout("END1", "2026-09-30", "2026-10-02"));

    MonthlyDividendLatestPayoutView before =
        MonthlyDividendReferenceSupport.latestPayoutBySymbol(history, LocalDate.parse("2026-10-29"))
            .get("END1");
    assertThat(before.stale()).isFalse();
    assertThat(before.expectedRecordDate()).isEqualTo(LocalDate.parse("2026-09-30"));

    // 기준일 당일(10-30)은 아직 "지난" 게 아니다(검토 2026-10-20 - 당일에 배지가 떴다).
    assertThat(
            MonthlyDividendReferenceSupport.latestPayoutBySymbol(
                    history, LocalDate.parse("2026-10-30"))
                .get("END1")
                .stale())
        .isFalse();

    MonthlyDividendLatestPayoutView after =
        MonthlyDividendReferenceSupport.latestPayoutBySymbol(history, LocalDate.parse("2026-10-31"))
            .get("END1");
    assertThat(after.expectedRecordDate()).isEqualTo(LocalDate.parse("2026-10-30"));
    assertThat(after.daysSinceRecord()).as("35 일 규칙이면 아직 아니다").isEqualTo(31);
    assertThat(after.stale()).isTrue();
  }

  @Test
  void 최근_기준일로_정렬하고_이력이_없는_종목은_늘_뒤다() {
    MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();
    List<MonthlyDividendProfileResponse> profiles =
        List.of(profile("NEW", 1), profile("NONE", 2), profile("OLD", 3));
    Map<String, LocalDate> dates =
        Map.of("NEW", LocalDate.parse("2026-09-30"), "OLD", LocalDate.parse("2026-08-15"));

    assertThat(support.resolveProfileSort("latest-record-date")).isEqualTo("latest-record-date");
    assertThat(support.resolveProfileDirection("latest-record-date", null))
        .as("기본은 오름차순 - 갱신이 밀린 종목이 맨 위")
        .isEqualTo("asc");
    assertThat(
            support.sortProfiles(profiles, "latest-record-date", "asc", dates).stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol))
        .containsExactly("OLD", "NEW", "NONE");
    assertThat(
            support.sortProfiles(profiles, "latest-record-date", "desc", dates).stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol))
        .containsExactly("NEW", "OLD", "NONE");
  }

  @Test
  void 목록에_기준일과_며칠_전과_갱신_확인을_적는다() {
    Map<String, MonthlyDividendLatestPayoutView> latest = new HashMap<>();
    latest.put(
        "NEW", MonthlyDividendLatestPayoutView.of(LocalDate.parse("2026-09-30"), null, TODAY));
    latest.put(
        "OLD", MonthlyDividendLatestPayoutView.of(LocalDate.parse("2026-08-15"), null, TODAY));
    Map<String, Object> model = new HashMap<>();
    model.put(
        "monthlyDividendProfiles",
        List.of(profile("NEW", 1), profile("OLD", 2), profile("NONE", 3)));
    model.put("monthlyDividendLatestPayoutBySymbol", latest);
    model.put("selectedMonthlyDividendSymbol", "NEW");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendReference.jte", model, output);
    String html = output.toString();

    assertThat(html)
        .contains("profileSort=latest-record-date")
        .contains(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.profile.latest.record.date"));
    String newCell = cell(html, "2026-09-30");
    assertThat(newCell)
        .contains("data-profile-latest-record-stale=\"false\"")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage(
                    "stock.page.dividend.monthly.reference.profile.latest.record.date.days.ago"),
                "13"))
        .doesNotContain("badge-warning");
    String oldCell = cell(html, "2026-08-15");
    assertThat(oldCell)
        .contains("data-profile-latest-record-stale=\"true\"")
        .contains("badge-warning")
        .contains(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.profile.latest.record.date.stale"));
    assertThat(html)
        .contains("data-profile-latest-record-date=\"\"")
        .contains(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.profile.latest.record.date.none"));
  }

  /**
   * 이 조각은 관리 화면(admin.jte)이 인자를 하나씩 넘겨 그린다 - 넘기는 줄이 빠지면 기본값(빈 맵)으로 모든 줄이 "지급 이력 없음" 이 된다. 실측
   * 2026-10-13: 조각만 그린 위 시험은 통과했는데 화면은 21 줄 모두 "-" 였다.
   */
  @Test
  void 관리_화면이_기준일_맵을_조각에_넘긴다() throws java.io.IOException {
    String admin =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/admin.jte"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(admin)
        .contains("monthlyDividendLatestPayoutBySymbol = monthlyDividendLatestPayoutBySymbol,");
  }

  /** 기준일 칸 하나(여는 td 부터 닫는 td 까지). */
  private static String cell(String html, String recordDate) {
    int start = html.indexOf("data-profile-latest-record-date=\"" + recordDate + "\"");
    assertThat(start).as("기준일 칸이 없다: %s", recordDate).isNotNegative();
    int end = html.indexOf("</td>", start);
    return html.substring(start, end);
  }

  /** 미리 발표돼 등록된 기준일(오늘 이후)은 "N일 뒤", 오늘이면 "오늘" - "-1일 전" 이 찍혔다(실측 2026-10-14). */
  @Test
  void 아직_안_온_기준일은_며칠_뒤로_적는다() {
    Map<String, MonthlyDividendLatestPayoutView> latest = new HashMap<>();
    latest.put(
        "NEW", MonthlyDividendLatestPayoutView.of(LocalDate.parse("2026-10-15"), null, TODAY));
    latest.put("OLD", MonthlyDividendLatestPayoutView.of(TODAY, null, TODAY));
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendProfiles", List.of(profile("NEW", 1), profile("OLD", 2)));
    model.put("monthlyDividendLatestPayoutBySymbol", latest);
    model.put("selectedMonthlyDividendSymbol", "NEW");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendReference.jte", model, output);
    String html = output.toString();

    assertThat(cell(html, "2026-10-15"))
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage(
                    "stock.page.dividend.monthly.reference.profile.latest.record.date.days.ahead"),
                "2"))
        .doesNotContain("-2");
    assertThat(cell(html, TODAY.toString()))
        .contains(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.profile.latest.record.date.today"));
  }
}
