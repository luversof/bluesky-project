package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;

/**
 * 상세 화면 차트 시리즈를 인라인 스크립트용 JS 객체 리터럴 한 줄로 만든다.
 *
 * <p>실측 2026-09-10(qa/item-requests.cjs): 종목 상세 조각이 239 KB 였는데, 주가 이력 1,599일을 템플릿 {@code @for} 가
 * 점마다 들여쓴 {@code series.labels.push(...)} 세 줄(약 120 바이트)로 찍은 몫이 대부분이었다(api-stock 원본은 71.8 KB). 배열
 * 리터럴로 모으면 점당 약 20 바이트다. 라벨은 JSON 문자열로 이스케이프하고, 금액은 원 단위 반올림 정수로 쓴다(예전 출력과 같은 값).
 */
public final class ChartSeriesJs {

  private ChartSeriesJs() {}

  /** 주가 차트: 종가 시리즈 + 지금 평균단가를 같은 길이로 그은 점선. */
  public static String priceSeries(
      List<StockPriceHistoryPoint> points, BigDecimal averageBuyPrice) {
    List<StockPriceHistoryPoint> safe = points == null ? List.of() : points;
    StringBuilder labels = new StringBuilder();
    StringBuilder value = new StringBuilder();
    for (StockPriceHistoryPoint pt : safe) {
      sep(labels).append(jsString(pt.tradeDate() != null ? pt.tradeDate().toString() : ""));
      sep(value).append(won(pt.closePrice()));
    }
    return "{labels:["
        + labels
        + "],value:["
        + value
        + "],cost:new Array("
        + safe.size()
        + ").fill("
        + won(averageBuyPrice)
        + "),buyCount:[],dailyRealized:[]}";
  }

  /** 보유 평가액·원가 추이(종목·계좌 상세): 평가액, 원가, 매수 건수, 일별 실현손익. */
  public static String holdingsSeries(
      List<TradeProfitTimeSeriesPoint> points, DateTimeFormatter labelFormatter) {
    List<TradeProfitTimeSeriesPoint> safe = points == null ? List.of() : points;
    StringBuilder labels = new StringBuilder();
    StringBuilder value = new StringBuilder();
    StringBuilder cost = new StringBuilder();
    StringBuilder buyCount = new StringBuilder();
    StringBuilder dailyRealized = new StringBuilder();
    for (TradeProfitTimeSeriesPoint pt : safe) {
      String label =
          pt.timestamp() == null
              ? ""
              : labelFormatter != null
                  ? labelFormatter.format(pt.timestamp())
                  : pt.timestamp().toString();
      sep(labels).append(jsString(label));
      sep(value).append(won(pt.totalHoldingsValue()));
      sep(cost).append(won(pt.totalHoldingsCost()));
      sep(buyCount).append(pt.buyCount());
      sep(dailyRealized).append(won(pt.dailyRealizedProfit()));
    }
    return "{labels:["
        + labels
        + "],value:["
        + value
        + "],cost:["
        + cost
        + "],buyCount:["
        + buyCount
        + "],dailyRealized:["
        + dailyRealized
        + "]}";
  }

  static String won(BigDecimal amount) {
    return amount == null ? "0" : amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
  }

  static StringBuilder sep(StringBuilder sb) {
    if (sb.length() > 0) sb.append(',');
    return sb;
  }

  /** JSON 규칙의 문자열 리터럴. 라벨은 날짜·기간 문구지만 따옴표·역슬래시·제어문자·{@code </} 는 반드시 피한다. */
  public static String jsString(String s) {
    StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '<' -> sb.append("\\u003C");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        default -> {
          if (c < 0x20) sb.append(String.format("\\u%04X", (int) c));
          else sb.append(c);
        }
      }
    }
    return sb.append('"').toString();
  }
}
