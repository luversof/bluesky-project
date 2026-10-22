package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 수익 분석의 합계 셈 · 칸 근거 문구(DividendYieldViewUtil)를 <b>동작으로</b>지킨다.
 *
 * <p>2026-10-22 까지 이 식들은 두 템플릿(연도별 표가 있는 부모 · 종목/계좌 카드)에 한 벌씩 있었고, 검사는 템플릿 글자를 셌다 &mdash; 한쪽 사본만
 * 고쳐도 남은 쪽 글자 때문에 통과할 수 있었다(검토 2026-10-22). 식을 한 곳으로 모으면서 글자 대신 값을 본다.
 */
class DividendYieldViewUtilTest {

  private static final DecimalFormat WON = new DecimalFormat("#,##0");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source, Locale.KOREAN));
    LocaleContextHolder.setLocale(Locale.KOREAN);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static DividendYieldGroupView row(
      String totalNet, String usedNet, String daySum, long count) {
    return new DividendYieldGroupView(
        null,
        "r",
        new BigDecimal(totalNet),
        new BigDecimal(totalNet),
        BigDecimal.ZERO,
        new BigDecimal(usedNet),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        count,
        Instant.parse("2026-01-01T00:00:00Z"),
        new BigDecimal(daySum),
        365L,
        BigDecimal.ZERO,
        null,
        false,
        365L);
  }

  @Test
  void 건수_합은_빈_목록과_빈_행을_0_으로_읽는다() {
    assertThat(DividendYieldViewUtil.dividendCount(null)).isZero();
    assertThat(
            DividendYieldViewUtil.dividendCount(
                Arrays.asList(row("1", "1", "1", 3), null, row("1", "1", "1", 4))))
        .isEqualTo(7L);
  }

  @Test
  void 합계_수익률은_분모가_없으면_0_분자가_없으면_0_으로_읽는다() {
    assertThat(DividendYieldViewUtil.yieldPct(new BigDecimal("150"), new BigDecimal("10000")))
        .isEqualByComparingTo("1.50");
    assertThat(DividendYieldViewUtil.yieldPct(new BigDecimal("1"), new BigDecimal("3")))
        .isEqualByComparingTo("33.33");
    assertThat(DividendYieldViewUtil.yieldPct(null, new BigDecimal("100")))
        .isEqualByComparingTo("0");
    assertThat(DividendYieldViewUtil.yieldPct(new BigDecimal("1"), BigDecimal.ZERO))
        .isEqualByComparingTo("0");
    assertThat(DividendYieldViewUtil.yieldPct(new BigDecimal("1"), null)).isEqualByComparingTo("0");
  }

  /** 분자는 기준일 원금이 있는 배당의 세후 합 - 배당금(세후) 열이 아니다. */
  @Test
  void 합계_연_수익률은_걸러진_세후_합을_분자로_쓴다() {
    // 세후 합 1,000 중 기준일 원금이 있는 배당 730 · 원금 x 일 합 365,000 → 730 x 365 / 365,000 = 73%
    List<DividendYieldGroupView> rows =
        List.of(row("600", "430", "200000", 1), row("400", "300", "165000", 1));
    BigDecimal expected =
        StockYieldUtil.annualizedPctOnHeldDays(new BigDecimal("730"), new BigDecimal("365000"));
    assertThat(DividendYieldViewUtil.footerAnnualizedPct(rows)).isEqualByComparingTo(expected);
    assertThat(DividendYieldViewUtil.footerAnnualizedPct(rows))
        .isNotEqualByComparingTo(
            StockYieldUtil.annualizedPctOnHeldDays(
                new BigDecimal("1000"), new BigDecimal("365000")));
    assertThat(DividendYieldViewUtil.footerAnnualizedPct(List.of())).isNull();
  }

  @Test
  void 제외된_배당이_있을_때만_분자_근거를_단다() {
    assertThat(
            DividendYieldViewUtil.basisTitle(new BigDecimal("1000"), new BigDecimal("1000"), WON))
        .isNull();
    assertThat(
            DividendYieldViewUtil.basisTitle(new BigDecimal("1200"), new BigDecimal("1000"), WON))
        .isNull();
    assertThat(DividendYieldViewUtil.basisTitle(null, new BigDecimal("1000"), WON)).isNull();
    String title =
        DividendYieldViewUtil.basisTitle(new BigDecimal("700"), new BigDecimal("1000"), WON);
    assertThat(title).contains("700").contains("300").doesNotContain("{0}");
  }

  @Test
  void 시가_수익률_근거는_분모가_있을_때만_분자와_분모를_함께_밝힌다() {
    assertThat(DividendYieldViewUtil.marketBasisTitle(BigDecimal.TEN, null, WON)).isNull();
    assertThat(DividendYieldViewUtil.marketBasisTitle(BigDecimal.TEN, BigDecimal.ZERO, WON))
        .isNull();
    String title = DividendYieldViewUtil.marketBasisTitle(null, new BigDecimal("2500000"), WON);
    assertThat(title).contains("2,500,000").contains("0").doesNotContain("{1}");
  }

  @Test
  void 기준일_원금이_없거나_0_이면_까닭을_돌려준다() {
    String none = MessageUtil.getMessage("stock.dividend.yield.basis.none");
    assertThat(none).isNotBlank().isNotEqualTo("stock.dividend.yield.basis.none");
    assertThat(DividendYieldViewUtil.basisNoneOrNull(null)).isEqualTo(none);
    assertThat(DividendYieldViewUtil.basisNoneOrNull(BigDecimal.ZERO)).isEqualTo(none);
    assertThat(DividendYieldViewUtil.basisNoneOrNull(new BigDecimal("0.00"))).isEqualTo(none);
    assertThat(DividendYieldViewUtil.basisNoneOrNull(BigDecimal.ONE)).isNull();
  }

  @Test
  void 보유_일수_문구는_숫자로_넘긴다() {
    assertThat(DividendYieldViewUtil.heldDaysText(260L)).contains("260").doesNotContain("{0}");
    assertThat(DividendYieldViewUtil.shortHeldDesc(40L)).contains("40").doesNotContain("{0}");
  }
}
