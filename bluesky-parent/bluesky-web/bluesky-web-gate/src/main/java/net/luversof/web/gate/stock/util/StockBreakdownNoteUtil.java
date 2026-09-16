package net.luversof.web.gate.stock.util;

import java.util.List;

import net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary;

/**
 * 기간 쪼갬 표를 <b>못 낼 때</b> 그 자리에 남길 까닭.
 *
 * <p>버리는 판단 자체는 옳다 &mdash; 구간이 하나뿐이면 표가 위 요약을 되풀이할 뿐이고, 해 단위로 묶인 줄은 '연도별 성과'가 같은 답을 한다. 틀린 것은
 * <b>말없이</b> 버리는 쪽이다: 실측 2026-09-16(종목 상세, 빈 기간) '월별 성과'가 제목째 사라졌다 &mdash; 같은 조각을 쓰는 자산 성장은 같은 상황에서
 * 까닭을 남기고 있었다. 한 조각이 화면마다 다르게 굴면 어느 쪽이 옳은지 알 수 없다.
 *
 * <p>그래서 규칙을 여기 한 곳에 둔다. 사본이 흩어지면 갈라진다.
 */
public final class StockBreakdownNoteUtil {

  /** 기간에 기록이 없어 나눌 것이 없다. */
  public static final String EMPTY_KEY = "stock.asset.growth.breakdown.empty.note";

  /** 구간이 하나뿐이라 표가 위 요약을 되풀이한다. */
  public static final String SINGLE_KEY = "stock.asset.growth.breakdown.single.note";

  /** 해 단위로 묶여 달 단위 표를 못 낸다(달 단위만 싣는 화면에서만 난다). */
  public static final String YEARLY_KEY = "stock.asset.growth.breakdown.yearly.note";

  /** 매매 집계 표(매매 내역)의 같은 자리 - 세는 것이 성과가 아니라 거래라 문구가 따로다. */
  public static final String TRADE_EMPTY_KEY = "stock.trade.breakdown.empty.note";

  /** 매매 집계 표의 '나눌 구간이 하나뿐' 인 경우. */
  public static final String TRADE_SINGLE_KEY = "stock.trade.breakdown.single.note";

  private StockBreakdownNoteUtil() {}

  /**
   * 남길 까닭의 메시지 키. 표를 낼 수 있으면 빈 문자열이다.
   *
   * @param rows api-stock 이 준 구간들
   * @param monthlyOnly 그 화면이 <b>달 단위만</b> 싣는가(자산 성장은 그렇고, 종목 상세는 해 단위 표도 그린다)
   */
  public static String noteKey(List<TradeProfitPeriodSummary> rows, boolean monthlyOnly) {
    if (rows == null || rows.isEmpty()) {
      return EMPTY_KEY;
    }
    if (monthlyOnly && !"MONTH".equals(rows.get(0).unit())) {
      return YEARLY_KEY;
    }
    return noteKeyBySize(rows.size(), EMPTY_KEY, SINGLE_KEY);
  }

  /**
   * 구간 수만으로 고르는 같은 규칙. 표를 낼 수 있으면 빈 문자열이다.
   *
   * <p>구간이 하나뿐이면 표가 위 요약을 되풀이할 뿐이라 그리지 않는다 &mdash; 그래도 <b>자리는 남긴다</b>.
   */
  public static String noteKeyBySize(int size, String emptyKey, String singleKey) {
    if (size <= 0) {
      return emptyKey;
    }
    return size > 1 ? "" : singleKey;
  }

  /** 매매 집계 표의 까닭(문구까지). 낼 수 있으면 빈 문자열이다. */
  public static String tradeNote(int size) {
    String key = noteKeyBySize(size, TRADE_EMPTY_KEY, TRADE_SINGLE_KEY);
    return key.isEmpty() ? "" : io.github.luversof.boot.context.support.MessageUtil.getMessage(key);
  }

  /** 위 규칙을 그대로 쓰되 문구까지 찾아 준다(자체 MessageSource 가 없는 화면용). */
  public static String note(List<TradeProfitPeriodSummary> rows, boolean monthlyOnly) {
    String key = noteKey(rows, monthlyOnly);
    return key.isEmpty() ? "" : io.github.luversof.boot.context.support.MessageUtil.getMessage(key);
  }
}
