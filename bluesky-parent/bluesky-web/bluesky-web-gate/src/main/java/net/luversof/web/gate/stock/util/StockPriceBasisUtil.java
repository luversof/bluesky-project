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
