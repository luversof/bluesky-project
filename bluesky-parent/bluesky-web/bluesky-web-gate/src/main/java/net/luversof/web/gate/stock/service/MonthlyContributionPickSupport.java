package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 이번에 무엇을 적립할까(사용자 요청 2026-09-22).
 *
 * <p>적립식으로 매달 사는데, 같은 자리를 두고 겹치는 종목이 둘씩 있어 매번 고민하게 된다. 그 자리를 <b>지급 시기 &times; 계좌</b>로 가른다.
 *
 * <ul>
 *   <li><b>계좌</b>는 과세표준 비중으로 갈린다 &mdash; {@value #TAXABLE_LIMIT_PCT}% 이내면 위탁계좌, 그보다 크면 ISA/연금계좌.
 *       사용자가 실제로 그렇게 운영한다(사용자 규칙 2026-09-22).
 *   <li><b>지급 시기</b>는 프로필의 월중 · 월말이다.
 * </ul>
 *
 * <p><b>점수 = 연배당 수익률 + min(분배금 추세, 0)</b>(사용자 결정 2026-09-22). 적립식의 목적은 배당을 쌓는 것이라 연배당 수익률이 기본이고,
 * 분배금이 <b>줄고 있으면 그만큼 깎는다</b> &mdash; 수익률은 최근 1 년 평균이라 최근 삭감을 앞쪽 큰 분배금이 가려 주기 때문이다. 늘고 있는 것은 더 주지
 * 않는다(이미 수익률에 들어오고 있다).
 *
 * <p>실측 2026-09-22: 월중 · 연금 자리에서 PLUS 고배당주위클리고정커버드콜(연배당 17.04%)이 분배금 추세 &minus;8.45% 로 8.59 로 내려가,
 * KODEX 한국부동산리츠인프라(9.24%, 추세 +21.96%)가 뽑힌다. 감점이 없으면 삭감 중인 종목을 계속 담게 된다.
 */
@Component
public class MonthlyContributionPickSupport {

  /** 이 비중까지는 위탁계좌, 넘으면 ISA/연금계좌(사용자 규칙). */
  public static final int TAXABLE_LIMIT_PCT = 10;

  /** 후보 한 종목. 이 클래스는 응답 DTO 를 모른다. */
  public record ContributionCandidate(
      String symbol,
      String name,
      String payoutWindow,
      BigDecimal taxableRatioPct,
      BigDecimal annualYieldPct,
      BigDecimal payoutTrendPct) {}

  /**
   * 한 자리의 결과.
   *
   * @param runnerUpSymbol 같은 자리의 다음 후보. 화면이 "무엇을 제치고 뽑혔는지" 를 말할 수 있게 함께 준다
   * @param runnerUpName 다음 후보의 종목명. 코드만 적으면 무슨 종목인지 알 수 없다(사용자 요청 2026-09-30) - 이름이 없으면 코드
   * @param tied 점수가 같은 후보가 있는가. 그러면 화면이 "고를 것이 갈리지 않는다" 를 알려야 한다
   */
  public record ContributionPick(
      String payoutWindow,
      String account,
      String symbol,
      String name,
      BigDecimal score,
      BigDecimal annualYieldPct,
      BigDecimal payoutTrendPct,
      String runnerUpSymbol,
      String runnerUpName,
      BigDecimal runnerUpScore,
      boolean tied) {}

  /** 위탁계좌 자리. */
  public static final String ACCOUNT_BROKERAGE = "BROKERAGE";

  /** ISA/연금계좌 자리. */
  public static final String ACCOUNT_PENSION = "PENSION";

  /** 과세표준 비중으로 계좌를 가른다. 비중을 모르면 위탁으로 두지 않는다 &mdash; 모르는 것을 단정하면 잘못된 계좌에 넣는다. */
  public String accountOf(BigDecimal taxableRatioPct) {
    if (taxableRatioPct == null) {
      return null;
    }

    return taxableRatioPct.compareTo(BigDecimal.valueOf(TAXABLE_LIMIT_PCT)) <= 0
        ? ACCOUNT_BROKERAGE
        : ACCOUNT_PENSION;
  }

  /**
   * 시뮬레이터 월배당 표를 지급 시기 &middot; 계좌 자리로 좁힐 때 남길 종목코드(사용자 요청 2026-09-23, 결정: 적립 추천과 같은 규칙).
   *
   * <p>적립 추천({@link #pickHeld})과 같은 출처(카탈로그의 지급 시기 &middot; 1 년 과세표준 비중)와 같은 경계({@link
   * #accountOf})로 가른다 &mdash; 표에 적힌 저장 비중으로 가르면 카드와 다른 답이 나온다(실측 2026-09-23: 0018C0 은 표 0% 인데 카탈로그
   * 25.05% 라 카드는 ISA/연금 자리에 둔다). 빈 값은 그 조건을 걸지 않는다. 계좌 조건이 걸렸는데 비중을 모르는 종목은 자리를 못 정하므로 뺀다.
   */
  public java.util.Set<String> symbolsInSlot(
      String payoutWindow,
      String account,
      List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse> catalog) {
    String window = resolveSlotWindow(payoutWindow);
    String slotAccount = resolveSlotAccount(account);
    java.util.Set<String> symbols = new java.util.HashSet<>();
    if (catalog == null) {
      return symbols;
    }
    for (net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse item : catalog) {
      if (item == null || item.stockItemSymbol() == null || item.stockItemSymbol().isBlank()) {
        continue;
      }
      if (!window.isEmpty() && !window.equals(item.payoutWindow())) {
        continue;
      }
      if (!slotAccount.isEmpty()
          && !slotAccount.equals(accountOf(item.averageTaxableBaseRatio1y()))) {
        continue;
      }
      symbols.add(item.stockItemSymbol().trim());
    }
    return symbols;
  }

  /** 지급 시기 자리 값 - 월중 &middot; 월말만 받고 그 밖에는 빈 값(조건 없음). 적립 추천도 이 둘만 자리로 삼는다. */
  public static String resolveSlotWindow(String payoutWindow) {
    return "MID_MONTH".equals(payoutWindow) || "MONTH_END".equals(payoutWindow) ? payoutWindow : "";
  }

  /** 계좌 자리 값 - 위탁 &middot; ISA/연금만 받고 그 밖에는 빈 값(조건 없음). */
  public static String resolveSlotAccount(String account) {
    return ACCOUNT_BROKERAGE.equals(account) || ACCOUNT_PENSION.equals(account) ? account : "";
  }

  /** 점수. 분배금이 줄고 있으면 그만큼 깎는다(늘고 있으면 더 주지 않는다). */
  public BigDecimal scoreOf(BigDecimal annualYieldPct, BigDecimal payoutTrendPct) {
    return score(annualYieldPct, payoutTrendPct);
  }

  /** 같은 점수 - 화면(월배당 ETF 표의 점수 열 · 정렬)이 인스턴스 없이 쓴다. 셈은 여기 하나다. */
  public static BigDecimal score(BigDecimal annualYieldPct, BigDecimal payoutTrendPct) {
    if (annualYieldPct == null) {
      return null;
    }

    // 두 수를 먼저 소수 둘째 자리로 맞추고 뺀다 - 화면이 "연배당 − 감소 = 점수" 를 적는데, 합친 뒤 반올림하면 0.01 어긋날 수 있다.
    return annualYieldPct.setScale(2, RoundingMode.HALF_UP).subtract(cutOf(payoutTrendPct));
  }

  /** 감점(0 이상). 분배금 추세가 음수일 때만 그 크기, 아니면 0. 소수 둘째 자리. */
  public static BigDecimal cutOf(BigDecimal payoutTrendPct) {
    BigDecimal cut =
        payoutTrendPct != null && payoutTrendPct.signum() < 0
            ? payoutTrendPct.negate()
            : BigDecimal.ZERO;
    return cut.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * 자리마다 하나씩 고른다.
   *
   * <p>지급 시기나 과세표준 비중을 모르는 종목은 <b>자리를 못 정하므로 뺀다</b> &mdash; 아무 자리에나 넣으면 엉뚱한 계좌를 권하게 된다. 점수를 못 내는
   * 종목(연배당 수익률이 없는 종목)도 뺀다.
   *
   * @return 지급 시기 &rarr; 계좌 순서로 정렬된 결과. 후보가 하나뿐인 자리도 그대로 넣는다(그 자리에서는 고민할 것이 없다는 뜻)
   */
  /**
   * 보유 종목만 후보로 삼아 고른다 &mdash; 시뮬레이터 월배당 탭과 월배당 ETF 목록의 "이번 적립" 배지가 같이 쓴다.
   *
   * <p>두 화면이 후보를 따로 만들면 언젠가 다른 답을 낸다(보유 출처 · 거르는 조건이 조금만 달라져도). 그래서 여기 하나만 둔다.
   *
   * @param heldSymbols 지금 보유한 종목코드(시뮬레이터 스냅샷 기준)
   * @param catalog 월배당 카탈로그(연배당 수익률 · 추세 · 과세표준 비중이 종목 단위로 들어 있다)
   */
  public List<ContributionPick> pickHeld(
      java.util.Collection<String> heldSymbols,
      List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse> catalog) {
    if (heldSymbols == null || heldSymbols.isEmpty() || catalog == null) {
      return List.of();
    }
    java.util.Set<String> held = new java.util.HashSet<>();
    for (String symbol : heldSymbols) {
      if (symbol != null && !symbol.isBlank()) {
        held.add(symbol.trim());
      }
    }
    return pick(
        catalog.stream()
            .filter(item -> held.contains(item.stockItemSymbol()))
            .map(
                item ->
                    new ContributionCandidate(
                        item.stockItemSymbol(),
                        item.stockItemName(),
                        item.payoutWindow(),
                        item.averageTaxableBaseRatio1y(),
                        item.annualYieldPct(),
                        item.payoutTrendPct()))
            .toList());
  }

  public List<ContributionPick> pick(List<ContributionCandidate> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }

    Map<String, List<ContributionCandidate>> buckets = new LinkedHashMap<>();
    for (ContributionCandidate candidate : candidates) {
      if (candidate == null || !hasWindow(candidate.payoutWindow())) {
        continue;
      }
      String account = accountOf(candidate.taxableRatioPct());
      // 분배금 추세를 모르는 종목(지급 이력 6 회 미만)은 뺀다(사용자 결정 2026-10-01). 추세를 모르면 감점할 수 없어, 삭감 중인지 모르는
      // 종목이 연배당만으로 자리 1위가 될 수 있었다 - "지금 눈여겨볼 종목" · 표 점수 열은 이미 이 종목에 점수를 안 낸다(두 곳이 같은 말).
      if (account == null
          || candidate.payoutTrendPct() == null
          || scoreOf(candidate.annualYieldPct(), candidate.payoutTrendPct()) == null) {
        continue;
      }
      buckets
          .computeIfAbsent(candidate.payoutWindow() + "|" + account, key -> new ArrayList<>())
          .add(candidate);
    }

    List<ContributionPick> picks = new ArrayList<>();
    for (Map.Entry<String, List<ContributionCandidate>> entry : buckets.entrySet()) {
      String[] key = entry.getKey().split("[|]");
      List<ContributionCandidate> list = new ArrayList<>(entry.getValue());
      // 점수가 같으면 종목코드 순으로 정한다 - 새로 고칠 때마다 답이 바뀌면 추천이 아니다.
      list.sort(
          Comparator.comparing(
                  (ContributionCandidate one) ->
                      scoreOf(one.annualYieldPct(), one.payoutTrendPct()))
              .reversed()
              .thenComparing(ContributionCandidate::symbol));

      ContributionCandidate best = list.get(0);
      ContributionCandidate next = list.size() > 1 ? list.get(1) : null;
      BigDecimal bestScore = scoreOf(best.annualYieldPct(), best.payoutTrendPct());
      BigDecimal nextScore =
          next != null ? scoreOf(next.annualYieldPct(), next.payoutTrendPct()) : null;
      picks.add(
          new ContributionPick(
              key[0],
              key[1],
              best.symbol(),
              best.name(),
              bestScore,
              best.annualYieldPct(),
              best.payoutTrendPct(),
              next != null ? next.symbol() : null,
              next == null
                  ? null
                  : (next.name() != null && !next.name().isBlank() ? next.name() : next.symbol()),
              nextScore,
              nextScore != null && nextScore.compareTo(bestScore) == 0));
    }

    // 화면 순서: 월중 먼저, 그 안에서 위탁 먼저. 매번 같은 순서로 보여야 눈이 익는다.
    picks.sort(
        Comparator.comparingInt((ContributionPick pick) -> windowOrder(pick.payoutWindow()))
            .thenComparingInt(pick -> ACCOUNT_BROKERAGE.equals(pick.account()) ? 0 : 1));
    return List.copyOf(picks);
  }

  private static boolean hasWindow(String payoutWindow) {
    return "MID_MONTH".equals(payoutWindow) || "MONTH_END".equals(payoutWindow);
  }

  private static int windowOrder(String payoutWindow) {
    return "MID_MONTH".equals(payoutWindow) ? 0 : 1;
  }
}
