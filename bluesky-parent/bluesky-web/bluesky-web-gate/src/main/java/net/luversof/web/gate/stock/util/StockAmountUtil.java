package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/**
 * 표의 <b>합계를 내는 규칙</b>.
 *
 * <p>합계 줄을 표마다 새로 만들다 보니 같은 코드를 되풀이하게 됐다(실측 2026-09-07: 합계를 내는 유틸 4 개 · 합계 줄을 그리는 조각 11 개). 되풀이
 * 자체보다 나쁜 것은 <b>규칙이 표마다 갈린다</b>는 점이다 &mdash; 실제로 어떤 표는 0 을 '-' 로, 어떤 표는 ₩0 으로 적고 있었다.
 *
 * <p>합치는 규칙은 셋뿐이다.
 *
 * <ul>
 *   <li>{@link #sum} &mdash; <b>더한다</b>. 금액·건수처럼 겹쳐 쌓이는 값.
 *   <li>{@link #chain} &mdash; <b>곱해서 잇는다</b>({@code Π(1+r)-1}). 수익률. 더하면 복리를 놓쳐 실제보다 작게 나온다.
 *   <li>합계가 <b>없는 것</b> &mdash; 기말 평가액 같은 값. 마지막 구간의 값이 곧 전체의 값이라 호출부가 직접 고른다.
 * </ul>
 *
 * <p>표시 쪽 규칙(0 을 '-' 로 쓸지, 부호를 붙일지)은 {@code _components/ui/amountCell.jte} 에 있다.
 */
public final class StockAmountUtil {

  private StockAmountUtil() {}

  public static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /**
   * amount 가 base 의 몇 % 인가. <b>100 을 먼저 곱한 뒤</b> 나눈다 &mdash; 순서를 바꾸면 표시 자릿수보다 굵게 미리 반올림된다(실측
   * 2026-09-12: 8,847,600 / 1,622,109,770 = 0.5454% 가 0.6% 로 나갔다). 값이 없거나 base 가 0 이하면 null &mdash;
   * 0 으로 읽어야 하는 화면은 {@link #nz} 로 감싼다.
   *
   * <p>같은 식이 주식 코드 열두 곳에 따로 있었다(도우미 6 · 인라인 6, 자릿수 1 · 2 · 4 와 빈 값 처리 null · 0 이 섞여 있었다 -
   * 2026-10-22 정리). 순서 실수는 그중 두 곳에서 실제로 났다.
   */
  public static BigDecimal percentOrNull(BigDecimal amount, BigDecimal base, int scale) {
    if (amount == null || base == null || base.signum() <= 0) {
      return null;
    }
    return amount.multiply(BigDecimal.valueOf(100)).divide(base, scale, RoundingMode.HALF_UP);
  }

  /** null 줄과 null 값을 0 으로 읽고 더한다. */
  public static <T> BigDecimal sum(Collection<T> rows, Function<T, BigDecimal> getter) {
    BigDecimal total = BigDecimal.ZERO;
    if (rows == null) {
      return total;
    }
    for (T row : rows) {
      if (row != null) {
        total = total.add(nz(getter.apply(row)));
      }
    }
    return total;
  }

  /** 같은 규칙으로 int 를 더한다(건수). */
  public static <T> int count(Collection<T> rows, java.util.function.ToIntFunction<T> getter) {
    int total = 0;
    if (rows == null) {
      return total;
    }
    for (T row : rows) {
      if (row != null) {
        total += getter.applyAsInt(row);
      }
    }
    return total;
  }

  /**
   * 퍼센트 수익률을 곱해서 잇는다. 낼 수 있는 구간이 하나도 없으면 null.
   *
   * <p>0% 로 내면 '원금 그대로' 라는 뜻이 되어 거짓말이 된다. 자본이 없던 구간(null)은 배수 1 이라 건너뛴다.
   *
   * <p>실측 2026-09-03: 전 기간 15 줄의 연쇄곱 1463.6231% 가 요약 카드의 투자 수익률과 소수점 넷째 자리까지 같았다.
   */
  public static <T> Double chain(Collection<T> rows, Function<T, Double> getter) {
    double factor = 1.0d;
    boolean any = false;
    if (rows == null) {
      return null;
    }
    for (T row : rows) {
      if (row == null) {
        continue;
      }
      Double rate = getter.apply(row);
      if (rate != null) {
        factor *= 1.0d + rate / 100.0d;
        any = true;
      }
    }
    return any ? (factor - 1.0d) * 100.0d : null;
  }
}
