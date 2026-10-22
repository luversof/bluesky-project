package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;

/**
 * 올해 과세 대상 배당이 금융소득 종합과세 기준(2,000 만 원)에 얼마나 다가갔는지(2026-10-20 자율 개선).
 *
 * <p>실측 2026-10-20: 위탁 계좌 둘의 과세표준이 2024 년 1,058 만 원 &rarr; 2025 년 1,371 만 원 &rarr; 2026 년 10 월 20
 * 일까지 818 만 원이다. 월배당을 늘려 가는 중이라 기준에 다가가는지를 한 해 내내 볼 곳이 필요하다 &mdash; 배당 내역의 기간 합계는 고른 기간만 말하고, 과세이연
 * 계좌까지 섞인다.
 *
 * <p>계산은 {@link #compute} 에 있는 순수 함수다(조회 없이 시험한다). 조회가 실패하면 null &mdash; 구역을 숨긴다(없는 합을 0 으로 보이면
 * "여유가 2,000 만 원" 으로 읽힌다).
 */
@Component
public class FinancialIncomeThresholdSupport {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(FinancialIncomeThresholdSupport.class);

  /** 연말 예상에 쓰는 최근 기간(일). 석 달이면 월배당 세 번 · 분기 배당 한 번이 들어온다. */
  public static final int RECENT_WINDOW_DAYS = 90;

  /** 지급 간격 중앙값이 이 날 수 이하면 달마다 나오는 종목(월배당 · 월말 묶음 지급 차이를 넉넉히). */
  static final int MONTHLY_MEDIAN_GAP_DAYS = 45;

  @Autowired private AccountClient accountClient;

  @Autowired private DividendClient dividendClient;

  /** 과세 계좌에 지금 들고 있는 종목(연말 예상에서 판 종목을 빼려고). */
  @Autowired private net.luversof.web.gate.stock.httpexchange.TradeProfitClient tradeProfitClient;

  public void setTradeProfitClient(
      net.luversof.web.gate.stock.httpexchange.TradeProfitClient tradeProfitClient) {
    this.tradeProfitClient = tradeProfitClient;
  }

  public void setAccountClient(AccountClient accountClient) {
    this.accountClient = accountClient;
  }

  public void setDividendClient(DividendClient dividendClient) {
    this.dividendClient = dividendClient;
  }

  /** 작년 1 월 1 일부터 오늘까지의 배당을 한 번에 읽어 계산한다. 실패하면 null. */
  public FinancialIncomeThresholdView load(UUID userId, LocalDate today, ZoneId zone) {
    if (userId == null || today == null || zone == null) {
      return null;
    }
    try {
      Set<UUID> deferredAccountIds = new HashSet<>();
      List<Account> accounts = accountClient.getAccountsByUserId(userId);
      if (accounts != null) {
        for (Account account : accounts) {
          if (account != null && account.isTaxDeferred()) {
            deferredAccountIds.add(account.id());
          }
        }
      }
      LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("userId", userId.toString());
      params.add(
          "startDate",
          LocalDate.of(today.getYear() - 1, 1, 1).atStartOfDay(zone).toInstant().toString());
      params.add("endDate", today.plusDays(1).atStartOfDay(zone).toInstant().toString());
      return compute(
          dividendClient.findDividends(params),
          deferredAccountIds,
          today,
          zone,
          loadHeldTaxableItemsQuietly(userId, deferredAccountIds));
    } catch (RuntimeException ex) {
      log.warn("금융소득 종합과세 기준 계산용 배당 조회 실패: userId={}", userId, ex);
      return null;
    }
  }

  /**
   * 과세 계좌(과세이연 제외)에 지금 1 주라도 있는 종목. 못 읽으면 null - 그때는 아무 종목도 빼지 않는다(예상이 조금 클 수는 있어도 없는 걸 지어내지는 않는다).
   */
  private Set<UUID> loadHeldTaxableItemsQuietly(UUID userId, Set<UUID> deferredAccountIds) {
    try {
      LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("userId", userId.toString());
      params.add(
          "groupBy",
          net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM
              .name());
      Set<UUID> held = new HashSet<>();
      List<net.luversof.web.gate.stock.domain.TradeProfit> rows =
          tradeProfitClient.calculateProfit(params);
      if (rows != null) {
        for (net.luversof.web.gate.stock.domain.TradeProfit row : rows) {
          if (row != null
              && row.stockItemId() != null
              && row.holdingQuantity() > 0
              && !deferredAccountIds.contains(row.accountId())) {
            held.add(row.stockItemId());
          }
        }
      }
      return held;
    } catch (RuntimeException ex) {
      log.warn("과세 계좌 보유 종목 조회 실패 - 연말 예상에서 판 종목을 빼지 못한다: userId={}", userId, ex);
      return null;
    }
  }

  /** 보유 종목을 모를 때(시험 · 조회 실패) - 아무 종목도 빼지 않는다. */
  public static FinancialIncomeThresholdView compute(
      List<DividendResponse> dividends,
      Set<UUID> deferredAccountIds,
      LocalDate today,
      ZoneId zone) {
    return compute(dividends, deferredAccountIds, today, zone, null);
  }

  /**
   * 과세 계좌의 과세표준을 올해 · 작년 · 최근 {@value #RECENT_WINDOW_DAYS} 일로 나눠 더한다.
   *
   * <p>날짜는 지급일(서버 존)이다. 과세표준이 빈 행은 0 으로 세지 않고 건수만 센다 &mdash; 0 으로 세면 기준까지 여유가 실제보다 커 보인다. 오늘 뒤에
   * 지급일이 찍힌 행(미리 넣은 예정)은 넣지 않는다.
   */
  public static FinancialIncomeThresholdView compute(
      List<DividendResponse> dividends,
      Set<UUID> deferredAccountIds,
      LocalDate today,
      ZoneId zone,
      Set<UUID> heldTaxableItems) {
    int year = today.getYear();
    LocalDate recentStart = today.minusDays(RECENT_WINDOW_DAYS - 1L);
    BigDecimal ytd = BigDecimal.ZERO;
    BigDecimal lastYear = BigDecimal.ZERO;
    long unknown = 0;
    // 종목 -> 지급일 -> 그날 과세표준 합(한 종목이 한 날 여러 줄일 수 있다 - 계좌 · 매수 묶음). 연말 예상을 종목마다 나눠 세려고 모은다.
    Map<UUID, TreeMap<LocalDate, BigDecimal>> payDaysByItem = new HashMap<>();
    if (dividends != null) {
      for (DividendResponse dividend : dividends) {
        if (dividend == null || dividend.payDate() == null) {
          continue;
        }
        if (deferredAccountIds != null && deferredAccountIds.contains(dividend.accountId())) {
          continue;
        }
        LocalDate payDay = dividend.payDate().atZone(zone).toLocalDate();
        if (payDay.isAfter(today)) {
          continue;
        }
        BigDecimal taxable = dividend.taxableAmount();
        if (payDay.getYear() == year) {
          if (taxable == null) {
            unknown++;
          } else {
            ytd = ytd.add(taxable);
          }
        } else if (payDay.getYear() == year - 1 && taxable != null) {
          lastYear = lastYear.add(taxable);
        }
        if (taxable != null && taxable.signum() > 0 && dividend.stockItemId() != null) {
          payDaysByItem
              .computeIfAbsent(dividend.stockItemId(), key -> new TreeMap<>())
              .merge(payDay, taxable, BigDecimal::add);
        }
      }
    }

    BigDecimal recentMonthly = BigDecimal.ZERO;
    BigDecimal periodicRest = BigDecimal.ZERO;
    int periodicCount = 0;
    for (Map.Entry<UUID, TreeMap<LocalDate, BigDecimal>> item : payDaysByItem.entrySet()) {
      // 과세 계좌에서 판 종목은 앞으로 안 나온다(보유를 모르면 빼지 않는다).
      if (heldTaxableItems != null && !heldTaxableItems.contains(item.getKey())) {
        continue;
      }
      TreeMap<LocalDate, BigDecimal> payDays = item.getValue();
      // 과세 지급이 아직 한 번뿐인데 최근 90 일 안이면(새로 산 종목 - 2026-10-20 검토) 주기를 모른다. 달마다로 보고 최근 흐름에 넣는다 -
      // 빼면 0 이 되고, 기준 경고에는 모자라게 잡는 것보다 넉넉하게 잡는 쪽이 낫다.
      boolean freshSingle = payDays.size() == 1 && !payDays.firstKey().isBefore(recentStart);
      if (freshSingle || isMonthlyPayer(payDays)) {
        for (Map.Entry<LocalDate, BigDecimal> entry :
            payDays.tailMap(recentStart, true).entrySet()) {
          recentMonthly = recentMonthly.add(entry.getValue());
        }
        continue;
      }
      // 분기 · 연 배당: 작년 이맘때 ~ 12 월 31 일에 받은 횟수 x 가장 최근 1 회 금액(수량이 바뀐 것을 따른다).
      Map.Entry<LocalDate, BigDecimal> latest = payDays.lastEntry();
      // 12 월 31 일엔 늦는 차례도 올해 안에 더 들어올 날이 없다(검토 2026-10-20: "더 받을 예상 0" 아래 "분기 1 회 = X" 가 함께 나왔다).
      int times =
          today.equals(LocalDate.of(year, 12, 31)) ? 0 : pendingCyclesThisYear(payDays, today);
      if (times > 0) {
        periodicRest = periodicRest.add(latest.getValue().multiply(BigDecimal.valueOf(times)));
        periodicCount += times;
      }
    }
    long remainingDays =
        java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(year, 12, 31));
    return new FinancialIncomeThresholdView(
        year,
        today,
        ytd,
        unknown,
        lastYear,
        FinancialIncomeThresholdView.THRESHOLD,
        RECENT_WINDOW_DAYS,
        recentMonthly,
        remainingDays,
        periodicRest.setScale(0, java.math.RoundingMode.HALF_UP),
        periodicCount);
  }

  /** 작년 지급일과 올해 같은 차례 지급을 짝짓는 폭(일). 분기 · 연 배당 지급일은 해마다 하루 이틀 ~ 몇 주 움직인다. */
  static final int CYCLE_MATCH_DAYS = 20;

  /**
   * 분기 · 연 배당 종목이 올해 남은 기간에 더 받을 횟수. 작년 지급일 L 마다 올해 같은 차례(L + 1 년 앞뒤 {@value #CYCLE_MATCH_DAYS} 일)를
   * 본다:
   *
   * <ul>
   *   <li>그 차례를 올해 이미 받았으면(오늘까지 짝이 있으면) 세지 않는다 - 지금까지 합에 이미 들어 있다(2026-10-20 검토: 작년 11-20 · 올해
   *       11-19 이면 11-19 에 두 번 셌다).
   *   <li>짝이 없고 L + 1 년이 올해 안이며 {@value #CYCLE_MATCH_DAYS} 일 넘게 지나지 않았으면 센다 - 늦어지는 지급도 아직 올 것으로 본다.
   * </ul>
   */
  static int pendingCyclesThisYear(TreeMap<LocalDate, BigDecimal> payDays, LocalDate today) {
    int year = today.getYear();
    int times = 0;
    for (LocalDate last :
        payDays
            .subMap(
                today.minusYears(1).minusDays(CYCLE_MATCH_DAYS),
                false,
                LocalDate.of(year - 1, 12, 31),
                true)
            .keySet()) {
      LocalDate due = last.plusYears(1);
      LocalDate matchFrom = due.minusDays(CYCLE_MATCH_DAYS);
      LocalDate matchTo =
          due.plusDays(CYCLE_MATCH_DAYS).isAfter(today) ? today : due.plusDays(CYCLE_MATCH_DAYS);
      // 짝을 찾을 구간이 아직 오지 않았으면(matchFrom > 오늘) 받았을 리 없다 - subMap 은 거꾸로 된 구간에 예외를 던진다.
      boolean paidThisYear =
          !matchFrom.isAfter(matchTo) && !payDays.subMap(matchFrom, true, matchTo, true).isEmpty();
      if (!paidThisYear && due.getYear() == year) {
        times++;
      }
    }
    return times;
  }

  /** 지급 간격(이웃한 지급일 사이 날 수)의 중앙값이 {@value #MONTHLY_MEDIAN_GAP_DAYS} 일 이하면 달마다 나오는 종목. 한 번뿐이면 아니다. */
  static boolean isMonthlyPayer(TreeMap<LocalDate, BigDecimal> payDays) {
    if (payDays == null || payDays.size() < 2) {
      return false;
    }
    List<Long> gaps = new java.util.ArrayList<>();
    LocalDate previous = null;
    for (LocalDate day : payDays.keySet()) {
      if (previous != null) {
        gaps.add(java.time.temporal.ChronoUnit.DAYS.between(previous, day));
      }
      previous = day;
    }
    java.util.Collections.sort(gaps);
    long median = gaps.get(gaps.size() / 2);
    return median <= MONTHLY_MEDIAN_GAP_DAYS;
  }
}
