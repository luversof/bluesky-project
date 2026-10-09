package net.luversof.web.gate.stock.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 화면에 적는 "현재가"가 실제로 어느 날 종가인지 고른다.
 *
 * <p>이 앱은 시세를 자동으로 모으지 않는다 - {@code @Scheduled} 도 k8s CronJob 도 없고, 관리 화면에서 사람이 눌러야 수집된다. 그래서 평가액이
 * 며칠 전 종가로 계산되는 일이 실제로 생긴다(실측 2026-08-22 토요일: 마지막 수집이 2026-08-20 이라 거래일인 08-21 금요일이 빠진 채 그 전날 종가로
 * 총자산이 표시됐다).
 *
 * <p>보유 종목 중 가장 최근 종가 일자를 쓴다. 종목마다 일자가 다를 수 있지만 화면의 합계는 각자 마지막 종가로 계산되므로, 사용자에게 알려야 할 것은 "이 화면이
 * 최신으로 반영한 날"이다.
 *
 * <p>같은 계산이 컨트롤러 세 곳에 각각 적혀 있었다(자산 현황 · 포트폴리오 · 종목/계좌 상세). 네 번째로 요약 화면에 붙이면서 한곳으로 모았다 - 한 곳만 고치고
 * 나머지를 잊으면 화면마다 다른 날짜를 적게 된다.
 */
public final class StockPriceBasisUtil {

  private StockPriceBasisUtil() {}

  /**
   * 종목 상세용 기준일. 보유가 남아 있으면 그 기준일, 전량 매도했으면 그 종목의 마지막 종가 일자.
   *
   * <p>시세 수집은 보유 중인 종목만 오늘까지 따라간다. 그래서 전량 매도한 종목의 "현재가" 는 마지막 보유 시점에 멈춰 있다(실측: 보유 0 인 33 종목 중 NAVER
   * 는 210,000 원 / 2026-04-01, 쌍방울은 2025-11-27 이다).
   *
   * <p>그런데 기준일을 보유 종목에서만 고르면 전량 매도 화면에서는 {@code null} 이 되어 <b>안내 줄이 사라지고 값만 남는다</b> &mdash; 넉 달 전
   * 종가가 아무 표시 없이 '현재가' 로 보인다. 값을 감추는 대신 그 값이 언제 것인지 같이 적는다.
   */
  public static LocalDate priceBasisDateWithFallback(List<TradeProfit> rows) {
    LocalDate fromHoldings = latestPriceBasisDate(rows);
    if (fromHoldings != null) {
      return fromHoldings;
    }
    if (rows == null) {
      return null;
    }
    return rows.stream()
        .map(TradeProfit::currentPriceDate)
        .filter(Objects::nonNull)
        .max(LocalDate::compareTo)
        .orElse(null);
  }

  /**
   * 손익 행이 아예 없을 때 쓸 마지막 종가 한 점(가격 이력에서).
   *
   * <p>거래한 적이 없는 종목은 손익 행이 없어 현재가가 {@code 0} 으로 떨어졌다 &mdash; 실측 2026-09-11: 기업은행(024110) 상세가 "현재가
   * 0" 을 찍었는데, 그 종목의 가격 이력에는 2026-04-03 종가 21,350 이 있었다. 값이 없는 것과 0 원인 것은 다르고, 여기서는 값이 <b>있다</b>.
   * 화면이 이미 같은 이력으로 가격 차트를 그리므로 그 마지막 점을 쓴다.
   *
   * @return 마지막 점(날짜 오름차순 기준). 이력이 비었으면 {@code null}
   */
  public static net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint lastPricePoint(
      List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> priceHistory) {
    if (priceHistory == null || priceHistory.isEmpty()) {
      return null;
    }
    return priceHistory.stream()
        .filter(Objects::nonNull)
        .filter(point -> point.tradeDate() != null && point.closePrice() != null)
        .filter(point -> point.closePrice().signum() > 0)
        .max((left, right) -> left.tradeDate().compareTo(right.tradeDate()))
        .orElse(null);
  }

  /** 국내 정규장 마감(KST). 그 거래일 이 시각 전에 받은 시세는 종가가 아니다. */
  static final java.time.LocalTime MARKET_CLOSE = java.time.LocalTime.of(15, 30);

  static final java.time.ZoneId MARKET_ZONE = java.time.ZoneId.of("Asia/Seoul");

  /**
   * 기준일 시세가 장중 값이면 받은 시각("HH:mm", KST), 종가면 {@code null}(2026-10-02).
   *
   * <p>시세 갱신은 관리 화면에서 손으로 돌린다. 장중에 돌리면 그 날 행은 장중 값인 채로 남는데 화면은 "평가 기준 2026-10-02 종가" 라고 불렀다 &mdash;
   * 실측: 10:16 에 받은 행이 장 마감 뒤까지 남았다. 기준일 행들 중 가장 이르게 받은 시각을 본다(하나라도 장중이면 그 시각).
   */
  public static String intradayTime(List<TradeProfit> rows, LocalDate basisDate) {
    if (rows == null || basisDate == null) {
      return null;
    }
    // 보유 행이 있으면 보유 행만 본다(기준일이 보유 종목에서 나오므로) - 다 판 종목의 같은 날 행이 끼면 엉뚱한 시각이 나온다.
    boolean anyHeld = rows.stream().anyMatch(row -> row != null && row.holdingQuantity() > 0);
    return intradayTime(
        rows.stream()
            .filter(Objects::nonNull)
            .filter(row -> !anyHeld || row.holdingQuantity() > 0)
            .filter(row -> basisDate.equals(row.currentPriceDate()))
            .map(TradeProfit::currentPriceUpdatedAt)
            .filter(Objects::nonNull)
            .min(java.time.Instant::compareTo)
            .orElse(null),
        basisDate);
  }

  /** 받은 시각이 그 거래일(KST) 마감 전이면 "HH:mm", 아니면 {@code null}. 다음 날 이후에 받았으면 종가다. */
  static String intradayTime(java.time.Instant updatedAt, LocalDate basisDate) {
    if (updatedAt == null || basisDate == null) {
      return null;
    }
    java.time.ZonedDateTime at = updatedAt.atZone(MARKET_ZONE);
    if (!at.toLocalDate().equals(basisDate) || !at.toLocalTime().isBefore(MARKET_CLOSE)) {
      return null;
    }
    return at.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
  }

  /**
   * 기준일 안내 문구의 틀({0} = 기준일). 장중 값이면 "평가 기준 {0} 10:16 장중 시세", 아니면 "평가 기준 {0} 종가".
   *
   * <p>{0} 자리는 그대로 남긴다 &mdash; 자산 성장 화면은 날짜만 줄바꿈 없이 감싸려고 {0} 앞뒤를 잘라 쓴다.
   */
  public static String basisMessage(String intradayTime) {
    if (intradayTime == null) {
      return io.github.luversof.boot.context.support.MessageUtil.getMessage(
          "stock.asset.status.price.basis");
    }
    return io.github.luversof.boot.context.support.MessageUtil.getMessage(
            "stock.asset.status.price.basis.intraday")
        .replace("{1}", intradayTime);
  }

  /** 보유 수량이 남은 종목들의 종가 일자 중 가장 늦은 날. 하나도 없으면 {@code null}. */
  public static LocalDate latestPriceBasisDate(List<TradeProfit> holdings) {
    if (holdings == null) {
      return null;
    }
    return holdings.stream()
        .filter(profit -> profit.holdingQuantity() > 0)
        .map(TradeProfit::currentPriceDate)
        .filter(Objects::nonNull)
        .max(LocalDate::compareTo)
        .orElse(null);
  }
}
