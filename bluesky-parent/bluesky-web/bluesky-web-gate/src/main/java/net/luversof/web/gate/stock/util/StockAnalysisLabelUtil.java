package net.luversof.web.gate.stock.util;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 시황 분석의 <b>회차 시점</b> · <b>방향</b> 코드를 사람이 읽는 말로 바꾼다.
 *
 * <p>키를 템플릿에서 {@code "stock.analysis.slot." + slot} 처럼 조립하면 모르는 코드가 왔을 때 빈 칸이 된다({@code
 * StockMessageKeyTest}). 모르는 코드는 감추지 않고 코드 그대로, 값이 없으면 {@code -} 로 보여 준다.
 */
public final class StockAnalysisLabelUtil {

  private StockAnalysisLabelUtil() {}

  /** 회차 시점(PREMARKET · NIGHT · EVENING · CLOSE · INTRADAY, 고르기용 all). */
  public static String slot(String slot) {
    if (slot == null) {
      return "-";
    }
    String key =
        switch (slot) {
          case "PREMARKET", "NIGHT", "EVENING", "CLOSE", "INTRADAY", "all" ->
              "stock.analysis.slot." + slot;
          default -> null;
        };
    return key == null ? slot : MessageUtil.getMessage(key);
  }

  /** 예상 · 실제 방향(UP · DOWN · FLAT). */
  public static String direction(String direction) {
    if (direction == null) {
      return "-";
    }
    String key =
        switch (direction) {
          case "UP", "DOWN", "FLAT" -> "stock.analysis.dir." + direction;
          default -> null;
        };
    return key == null ? direction : MessageUtil.getMessage(key);
  }
}
