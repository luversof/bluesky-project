package net.luversof.web.gate.stock.util;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.Basis;

/**
 * 추천 기준(사용자 요청 2026-10-06)을 사람이 읽는 말로 바꾼다 &mdash; 이름과 한 줄 설명.
 *
 * <p>키를 화면에서 {@code "stock.pick.basis." + 값} 으로 조립하면 모르는 값이 조용히 빈 칸이 된다(MessageUtil 은 없는 키에 빈 문자열을
 * 돌려준다). 그래서 기준마다 키를 적어 두고, 모르는 값은 받은 값 그대로 돌려준다.
 */
public final class StockPickBasisLabelUtil {

  private StockPickBasisLabelUtil() {}

  /** 기준 이름(단추 · "○○ 기준이면" 에 쓴다). 모르는 값은 그대로. */
  public static String label(String param) {
    String key =
        switch (param == null ? "" : param) {
          case "dividend" -> "stock.pick.basis.dividend";
          case "total" -> "stock.pick.basis.total";
          case "stable" -> "stock.pick.basis.stable";
          case "steady" -> "stock.pick.basis.steady";
          default -> null;
        };
    return key == null ? (param == null ? "" : param) : MessageUtil.getMessage(key);
  }

  /** 기준 이름. */
  public static String label(Basis basis) {
    return basis == null ? "" : label(basis.param());
  }

  /**
   * 비교표의 기준 값 한 칸(MessageFormat 틀). 배당 "{0}점" · 총수익 "총수익 {0}%" · 안정성 "안정성 {0}" · 꾸준함 "감소 {0}/{1}회".
   * 모르는 값은 "{0}".
   */
  public static String comparePattern(String param) {
    String key =
        switch (param == null ? "" : param) {
          case "dividend" -> "stock.pick.compare.value.dividend";
          case "total" -> "stock.pick.compare.value.total";
          case "stable" -> "stock.pick.compare.value.stable";
          case "steady" -> "stock.pick.compare.value.steady";
          default -> null;
        };
    return key == null ? "{0}" : MessageUtil.getMessage(key);
  }

  /** 고른 기준이 무엇을 보는지 한 줄. 모르는 값은 빈 문자열(설명을 지어내지 않는다). */
  public static String description(String param) {
    String key =
        switch (param == null ? "" : param) {
          case "dividend" -> "stock.pick.basis.dividend.desc";
          case "total" -> "stock.pick.basis.total.desc";
          case "stable" -> "stock.pick.basis.stable.desc";
          case "steady" -> "stock.pick.basis.steady.desc";
          default -> null;
        };
    return key == null ? "" : MessageUtil.getMessage(key);
  }
}
