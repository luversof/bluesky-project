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
    // 지금 보유가 없으면 평균단가가 0 이 아니라 "없음" 이다. 0 으로 채우면 종가가 20 만 원인 차트
    // 바닥에 "현재 평균단가 0" 선이 깔려 공짜로 산 것처럼 읽힌다 - 실측 2026-09-12: 매매 이력이 있는
    // 43 종목 중 34 종목이 수량 0 이고, 표본 4 종목 모두 1,151~2,740 점이 전부 0 이었다(기아 · 나노팀 ·
    // 삼성SDI · 에스디바이오센서). 보조기술에도 "최고 0, 최저 0" 으로 읽혔다. 비워서 선을 긋지 않는다.
    boolean hasAverage = averageBuyPrice != null && averageBuyPrice.signum() != 0;
    String cost =
        hasAverage ? "new Array(" + safe.size() + ").fill(" + won(averageBuyPrice) + ")" : "[]";
    return "{labels:["
        + labels
        + "],value:["
        + value
        + "],cost:"
        + cost
        + ",buyCount:[],dailyRealized:[]}";
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

  /**
   * 주가 캔들 시리즈(2026-10-02): labels · open · high · low · close · avg(그 날 평균 단가, 보유가 없던 날 null). 봉
   * 묶기(주봉 · 월봉)는 브라우저(StockCharts.candleBuckets)가 한다 - 툴팁이 날짜별 원값을 알아야 해서 서버는 일봉을 그대로 준다.
   */
  public static String candleSeries(
      List<net.luversof.web.gate.stock.dto.response.StockPriceChartPoint> points) {
    return candleSeries(points, List.of(), java.time.ZoneOffset.UTC);
  }

  /**
   * 캔들 시리즈 + 날짜별 내 매수 · 매도 수량(buy · sell, 2026-10-02 - 캔들 위 ▲ · ▼). 시세가 없는 날의 매매(공모주 청약일 · 휴장일 기록)는
   * 그 다음 시세일 봉에 붙인다 - 버리면 첫 매수 표시가 사라진다. 기간 밖 매매는 뺀다.
   */
  public static String candleSeries(
      List<net.luversof.web.gate.stock.dto.response.StockPriceChartPoint> points,
      List<net.luversof.web.gate.stock.dto.response.TradeResponse> trades,
      java.time.ZoneId zone) {
    return candleSeries(points, trades, zone, null);
  }

  /**
   * 캔들 시리즈 + 그 날 보유 평가액 · 원가(hv · hc, 사용자 요청 2026-10-07: "주가 추이에 평균 단가 선을 이어서 보여주고 마우스 오버시 보유 평가액을
   * 보여주는 식으로 하나의 그래프에서"). 보유 평가액 추이 차트를 따로 두지 않고 캔들 툴팁에 싣는다. 보유가 없던 날 · 시계열에 없는 날은 null(툴팁에 줄을 안 단다
   * - 0 원이라고 적으면 다 판 것처럼 읽힌다).
   *
   * @param holdings 일별 보유 시계열(DAILY). timestamp 를 zone 의 날짜로 맞춰 시세일에 붙인다
   */
  public static String candleSeries(
      List<net.luversof.web.gate.stock.dto.response.StockPriceChartPoint> points,
      List<net.luversof.web.gate.stock.dto.response.TradeResponse> trades,
      java.time.ZoneId zone,
      List<TradeProfitTimeSeriesPoint> holdings) {
    List<net.luversof.web.gate.stock.dto.response.StockPriceChartPoint> safe =
        points == null ? List.of() : points;
    java.util.TreeMap<java.time.LocalDate, Integer> indexByDate = new java.util.TreeMap<>();
    for (int i = 0; i < safe.size(); i++) {
      if (safe.get(i).tradeDate() != null) {
        indexByDate.put(safe.get(i).tradeDate(), i);
      }
    }
    long[] buyQty = new long[safe.size()];
    long[] sellQty = new long[safe.size()];
    if (trades != null && !indexByDate.isEmpty()) {
      for (var trade : trades) {
        if (trade == null || trade.tradeDate() == null || trade.type() == null) {
          continue;
        }
        var at = indexByDate.ceilingEntry(trade.tradeDate().atZone(zone).toLocalDate());
        if (at == null) {
          continue;
        }
        if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.BUY) {
          buyQty[at.getValue()] += trade.quantity();
        } else if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.SELL) {
          sellQty[at.getValue()] += trade.quantity();
        }
      }
    }
    StringBuilder buy = new StringBuilder();
    StringBuilder sell = new StringBuilder();
    // 분배락일의 주당 분배금(없으면 0) - 원주가 차트에서 매달 분배금만큼 떨어지는 날을 ◆ 로 표시한다.
    StringBuilder dist = new StringBuilder();
    for (int i = 0; i < safe.size(); i++) {
      sep(buy).append(buyQty[i]);
      sep(sell).append(sellQty[i]);
      var amount = safe.get(i).distribution();
      sep(dist)
          .append(
              amount != null && amount.signum() > 0
                  ? amount.stripTrailingZeros().toPlainString()
                  : "0");
    }
    java.util.Map<java.time.LocalDate, TradeProfitTimeSeriesPoint> holdingByDate =
        new java.util.HashMap<>();
    if (holdings != null) {
      for (TradeProfitTimeSeriesPoint h : holdings) {
        if (h != null && h.timestamp() != null) {
          holdingByDate.put(h.timestamp().atZone(zone).toLocalDate(), h);
        }
      }
    }
    StringBuilder hv = new StringBuilder();
    StringBuilder hc = new StringBuilder();
    StringBuilder labels = new StringBuilder();
    StringBuilder open = new StringBuilder();
    StringBuilder high = new StringBuilder();
    StringBuilder low = new StringBuilder();
    StringBuilder close = new StringBuilder();
    StringBuilder avg = new StringBuilder();
    for (var pt : safe) {
      TradeProfitTimeSeriesPoint h =
          pt.tradeDate() != null ? holdingByDate.get(pt.tradeDate()) : null;
      boolean held =
          h != null && h.totalHoldingsValue() != null && h.totalHoldingsValue().signum() > 0;
      sep(hv).append(held ? won(h.totalHoldingsValue()) : "null");
      sep(hc).append(held && h.totalHoldingsCost() != null ? won(h.totalHoldingsCost()) : "null");
      sep(labels).append(jsString(pt.tradeDate() != null ? pt.tradeDate().toString() : ""));
      sep(open).append(pt.open() != null ? won(pt.open()) : "null");
      sep(high).append(pt.high() != null ? won(pt.high()) : "null");
      sep(low).append(pt.low() != null ? won(pt.low()) : "null");
      sep(close).append(won(pt.close()));
      // 보유가 없던 날은 0 이 아니라 null - 0 이면 선이 바닥으로 떨어져 공짜로 산 것처럼 읽힌다(priceSeries 와 같은 이유).
      sep(avg)
          .append(
              pt.averageCost() != null && pt.averageCost().signum() != 0
                  ? won(pt.averageCost())
                  : "null");
    }
    return "{labels:["
        + labels
        + "],open:["
        + open
        + "],high:["
        + high
        + "],low:["
        + low
        + "],close:["
        + close
        + "],avg:["
        + avg
        + "],buy:["
        + buy
        + "],sell:["
        + sell
        + "],dist:["
        + dist
        + "],hv:["
        + hv
        + "],hc:["
        + hc
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
