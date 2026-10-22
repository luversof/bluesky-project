package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 수익 분석 화면(연도별 표 · 종목별/계좌별 카드)의 합계 셈과 칸 근거 문구.
 *
 * <p>2026-10-22 까지 {@code dividendYieldAnalytics.jte} 와 {@code dividendYieldGroupCard.jte} 가 같은 람다를
 * 한 벌씩 들고 있었다(카드를 조각으로 빼면서 부모의 연도별 표도 같은 셈을 써서 둘로 갈렸다). 검사는 템플릿 글자를 세고 있어, 한쪽 사본만 고쳐도 다른 쪽이 그대로 남을
 * 수 있었다(검토 2026-10-22). 식은 여기 한 곳에 두고 템플릿은 이름 붙은 변수로 부르기만 한다 - 템플릿 본문의 {@code
 * yieldBasisTitle.apply(…)} 같은 사용처는 그대로다.
 */
public final class DividendYieldViewUtil {

  private DividendYieldViewUtil() {}

  /** 배당 건수 합(null 목록 · null 행은 0). */
  public static long dividendCount(List<DividendYieldGroupView> rows) {
    if (rows == null) {
      return 0L;
    }
    long total = 0L;
    for (DividendYieldGroupView row : rows) {
      if (row != null) {
        total += row.dividendCount();
      }
    }
    return total;
  }

  /** 합계 줄 수익률(%) - 분모가 0 이하이거나 없으면 0, 분자가 없으면 0 으로 읽는다. 소수 둘째 자리. */
  public static BigDecimal yieldPct(BigDecimal netAmount, BigDecimal baseAmount) {
    return StockAmountUtil.nz(
        StockAmountUtil.percentOrNull(StockAmountUtil.nz(netAmount), baseAmount, 2));
  }

  /**
   * 합계 줄 연 수익률. 분자는 <b>기준일 원금이 있는 배당</b>의 세후 합(netAmountWithPrincipalCost)이고 분모는 원금 x 일 합 &mdash;
   * 행과 같은 규칙이다. 화면의 배당금(세후) 열을 분자로 쓰면 분모에 기여하지 않은 배당까지 들어가 과대 계상된다.
   */
  public static BigDecimal footerAnnualizedPct(List<DividendYieldGroupView> rows) {
    BigDecimal daySum = StockAmountUtil.sum(rows, DividendYieldGroupView::principalCostDaySum);
    BigDecimal usedNet =
        StockAmountUtil.sum(rows, DividendYieldGroupView::netAmountWithPrincipalCost);
    return StockYieldUtil.annualizedPctOnHeldDays(usedNet, daySum);
  }

  /**
   * 수익률 칸 근거: 분자는 "기준일 원금이 있는 배당" 의 세후 합이라 화면의 배당금(세후) 열과 다르다(실측: 193 건 중 5 건이 분모에 기여하지 않아 제외 - 화면
   * 숫자를 그대로 나누면 7.14% 인데 표시는 7.12% 라 틀린 것처럼 보였다). 실제 분자와 제외액을 밝혀 표시값을 화면에서 재현하게 한다. 걸러진 배당이 없으면
   * null &mdash; JTE 는 null 속성을 통째로 빼므로 의미 없는 title="" 이 나가지 않는다.
   */
  public static String basisTitle(
      BigDecimal usedNet, BigDecimal totalNet, DecimalFormat decimalFormat) {
    if (usedNet == null || totalNet == null) {
      return null;
    }
    BigDecimal excludedNet = totalNet.subtract(usedNet);
    if (excludedNet.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return msg(
        "stock.dividend.yield.basis.excluded",
        decimalFormat.format(usedNet),
        decimalFormat.format(excludedNet));
  }

  /**
   * '기준일 평균시가 수익률' 칸 근거: 형제 둘(기간 일평균 투입원금 · 배당 기준일 평균원금)은 분모가 제 열로 나와 검산할 수 있는데 이 수익률만 못 해서(실측
   * 2026-09-15: 종목 16 칸 + 계좌 6 칸) 분자와 분모를 함께 밝힌다. 분모가 없으면 null.
   */
  public static String marketBasisTitle(
      BigDecimal usedNet, BigDecimal base, DecimalFormat decimalFormat) {
    if (base == null || base.signum() <= 0) {
      return null;
    }
    return msg(
        "stock.dividend.yield.market.basis",
        decimalFormat.format(usedNet == null ? BigDecimal.ZERO : usedNet),
        decimalFormat.format(base));
  }

  /** 기준일 원금이 아예 없으면(없음 · 0) 금액도 수익률도 "-" 가 되는데, 그 까닭(2026-09-12 까지 없었다). 원금이 있으면 null. */
  public static String basisNoneOrNull(BigDecimal principalCost) {
    return principalCost == null || principalCost.signum() == 0
        ? MessageUtil.getMessage("stock.dividend.yield.basis.none")
        : null;
  }

  /** "보유 N일". 숫자 그대로 넘긴다 - 문자열로 넘기면 문구의 choice 형식이 예외를 던진다. */
  public static String heldDaysText(Long days) {
    return msg("stock.dividend.yield.cell.held.days", days);
  }

  /** 짧게 보유 배지 설명(보유 N일). 숫자 그대로 넘긴다. */
  public static String shortHeldDesc(Long days) {
    return msg("stock.dividend.yield.cell.short.held.desc", days);
  }
}
