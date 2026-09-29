package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendLinkRegisterService;
import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendSourceMetaParser.SourceMeta;

/**
 * 월배당 ETF 총보수(연) · 상장일(사용자 승인 DDL 2026-09-28).
 *
 * <p>값은 운용사에서 읽고, 모르면 null 이다. 화면은 0% 가 아니라 "미확인" 을 적고, 정렬은 방향과 무관하게 모르는 종목을 뒤로 보낸다. 저장이 전체 덮어쓰기라 폼
 * · 링크 등록 · 새로 가져오기가 기존 값을 실어 보내야 한다.
 */
class MonthlyEtfExpenseListingTest {

  private static final String ETF_TEMPLATE = "src/main/jte/stock/monthlyEtf.jte";

  private static final String ADMIN_TEMPLATE =
      "src/main/jte/stock/fragments/monthlyDividendReference.jte";

  private final MonthlyEtfViewSupport support =
      new MonthlyEtfViewSupport(new MonthlyContributionPickSupport());

  @Test
  void 표가_총보수_열과_상장일을_적고_모르면_미확인이다() throws IOException {
    String template = squash(Files.readString(Path.of(ETF_TEMPLATE), StandardCharsets.UTF_8));

    assertThat(template)
        .contains("sort=expense-ratio")
        .contains("stock.monthly.etf.table.header.expense.ratio.desc")
        .contains(
            "<divdata-expense-ratio>${row.totalExpenseRatioPct().stripTrailingZeros().toPlainString()}%</div>")
        .contains(
            "@else<divclass=\"text-base-content/60\"data-expense-missing>${MessageUtil.getMessage(\"stock.monthly.etf.table.cell.expense.unknown\")}</div>")
        .contains("data-listing-date>");
    // 두 자리로 자르면 0.0795% 와 0.08% 가 같아 보인다 - 퍼센트 서식기를 쓰지 않는다.
    assertThat(template).doesNotContain("percentFormat.format(row.totalExpenseRatioPct()");
  }

  @Test
  void 총보수_정렬은_싼_것부터이고_모르면_늘_뒤다() {
    MonthlyEtfRowView cheap = row("A00001", new BigDecimal("0.09"));
    MonthlyEtfRowView pricey = row("A00002", new BigDecimal("0.8"));
    MonthlyEtfRowView unknown = row("A00003", null);
    List<MonthlyEtfRowView> rows = List.of(unknown, pricey, cheap);

    assertThat(support.resolveSort("expense-ratio")).isEqualTo("expense-ratio");
    assertThat(support.resolveDirection("expense-ratio", null)).isEqualTo("asc");
    assertThat(symbols(support.sortRows(rows, "expense-ratio", "asc")))
        .containsExactly("A00001", "A00002", "A00003");
    assertThat(symbols(support.sortRows(rows, "expense-ratio", "desc")))
        .as("내림차순에서도 모르는 종목이 맨 위로 오면 안 된다")
        .containsExactly("A00002", "A00001", "A00003");
  }

  @Test
  void 관리_폼이_두_값을_싣고_돌려받는다() throws IOException {
    String admin = squash(Files.readString(Path.of(ADMIN_TEMPLATE), StandardCharsets.UTF_8));
    assertThat(admin)
        .contains("name=\"totalExpenseRatioPct\"")
        .contains("name=\"listingDate\"type=\"date\"")
        .contains("action=\"/stock/dividend/monthly-reference/profile/facts/refresh\"");

    var support = new net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport();
    var form =
        support.buildDefaultMonthlyDividendProfileForm(
            profile(new BigDecimal("0.0900"), LocalDate.of(2024, 3, 5)));
    assertThat(form.getTotalExpenseRatioPct()).isEqualByComparingTo("0.09");
    assertThat(form.getListingDate()).isEqualTo(LocalDate.of(2024, 3, 5));
  }

  /**
   * 링크를 다시 등록하면 기존 메모 · 최종 검증일 · 총보수 · 상장일을 지우지 않는다(발견 2026-09-28: 저장이 전체 덮어쓰기인데 링크 등록은 네 값만 보내 메모
   * · 검증일이 지워졌다). 이번에 읽은 총보수 · 상장일이 있으면 그것으로 바꾼다.
   */
  @Test
  void 링크_재등록은_기존_값을_지우지_않는다() {
    var existing = profile(new BigDecimal("0.3000"), LocalDate.of(2025, 9, 2));

    var withoutFacts =
        MonthlyDividendLinkRegisterService.buildProfileRequest(
            new SourceMeta("0094M0", "RISE 코리아밸류업"),
            "https://example.test/new",
            "MID_MONTH",
            existing);
    assertThat(withoutFacts.getNote()).isEqualTo("사람이 적은 메모");
    assertThat(withoutFacts.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 9, 17));
    assertThat(withoutFacts.getDisplayOrder()).isEqualTo(4);
    assertThat(withoutFacts.getTotalExpenseRatioPct()).isEqualByComparingTo("0.3");
    assertThat(withoutFacts.getListingDate()).isEqualTo(LocalDate.of(2025, 9, 2));
    assertThat(withoutFacts.getSourceUrl()).isEqualTo("https://example.test/new");

    var withFacts =
        MonthlyDividendLinkRegisterService.buildProfileRequest(
            new SourceMeta(
                "0094M0", "RISE 코리아밸류업", new BigDecimal("0.2500"), LocalDate.of(2025, 9, 3)),
            "https://example.test/new",
            "MID_MONTH",
            existing);
    assertThat(withFacts.getTotalExpenseRatioPct()).isEqualByComparingTo("0.25");
    assertThat(withFacts.getListingDate()).isEqualTo(LocalDate.of(2025, 9, 3));
    assertThat(withFacts.getNote()).isEqualTo("사람이 적은 메모");

    // RISE 옛 주소로 링크를 넣어도 새 주소로 저장한다(2026-09-29 주소 이전).
    var rise =
        MonthlyDividendLinkRegisterService.buildProfileRequest(
            new SourceMeta("475720", "RISE 200위클리커버드콜"),
            "https://www.riseetf.co.kr/prod/finderDetail/44G3",
            "MID_MONTH",
            existing);
    assertThat(rise.getSourceUrl()).isEqualTo("https://kbam.co.kr/products/44G3");
  }

  /** 새로 가져오기는 지급 시기 · 활성 · 링크까지 기존 그대로 두고 두 값만 얹는다. 못 읽은 값은 지우지 않는다. */
  @Test
  void 새로_가져오기는_두_값만_바꾸고_못_읽으면_둔다() {
    var existing = profile(new BigDecimal("0.3000"), LocalDate.of(2025, 9, 2));

    var partial =
        MonthlyDividendLinkRegisterService.withFacts(
            existing, new SourceMeta("0094M0", "RISE", new BigDecimal("0.2000"), null));
    assertThat(partial.getTotalExpenseRatioPct()).isEqualByComparingTo("0.2");
    assertThat(partial.getListingDate())
        .as("못 읽은 상장일은 지우지 않는다")
        .isEqualTo(LocalDate.of(2025, 9, 2));
    assertThat(partial.getPayoutWindow()).isEqualTo("MONTH_END");
    assertThat(partial.getActive()).isFalse();
    assertThat(partial.getSourceUrl()).isEqualTo("https://example.test/old");
    assertThat(partial.getNote()).isEqualTo("사람이 적은 메모");
  }

  private static MonthlyDividendProfileResponse profile(BigDecimal expense, LocalDate listing) {
    return new MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "0094M0",
        "RISE 코리아밸류업",
        "https://example.test/old",
        "MONTH_END",
        4,
        false,
        "사람이 적은 메모",
        LocalDate.of(2026, 9, 17),
        Instant.parse("2026-09-20T00:00:00Z"),
        expense,
        listing);
  }

  private static List<String> symbols(List<MonthlyEtfRowView> rows) {
    return rows.stream().map(MonthlyEtfRowView::stockItemSymbol).toList();
  }

  private static String squash(String text) {
    return text.replaceAll("\\s+", "");
  }

  private static MonthlyEtfRowView row(String symbol, BigDecimal expense) {
    return new MonthlyEtfRowView(
        UUID.randomUUID(),
        symbol,
        symbol,
        "MID_MONTH",
        "https://example.test/" + symbol,
        LocalDate.of(2026, 9, 17),
        true,
        1,
        12,
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 17),
        new BigDecimal("300"),
        new BigDecimal("260"),
        new BigDecimal("4.06"),
        new BigDecimal("10.56"),
        new BigDecimal("20365"),
        LocalDate.of(2026, 9, 18),
        new BigDecimal("1.00"),
        new BigDecimal("12.00"),
        LocalDate.of(2025, 9, 1),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        expense,
        expense != null ? LocalDate.of(2024, 3, 5) : null);
  }
}
