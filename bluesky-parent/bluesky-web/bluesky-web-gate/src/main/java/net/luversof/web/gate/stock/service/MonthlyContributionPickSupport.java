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
 *
 * <p><b>추천 기준</b>(사용자 요청 2026-10-06: "etf 추천 기준을 다양하게"). 위 점수는 기본 기준(배당)이고, 같은 자리 · 같은 후보를 두고 셈만 바꾼
 * 기준을 둘 더 둔다 &mdash; {@link Basis}. 실측 2026-10-06: 네 자리 중 총수익 기준은 세 자리, 안정성 기준은 두세 자리에서 1위가
 * 바뀌었다(분배율이 높은 커버드콜이 가격을 내주며 분배하는 경우). 후보 조건은 기준과 상관없이 같다 - 기준마다 후보가 다르면 "다른 기준이면" 이 같은 종목들 사이의 말이
 * 아니게 된다.
 */
@Component
public class MonthlyContributionPickSupport {

  /** 이 비중까지는 위탁계좌, 넘으면 ISA/연금계좌(사용자 규칙). */
  public static final int TAXABLE_LIMIT_PCT = 10;

  /**
   * 추천 기준(사용자 요청 2026-10-06). 셈만 다르고 자리 · 후보는 같다.
   *
   * <ul>
   *   <li>{@link #DIVIDEND} 배당 많이 - 연배당 수익률 &minus; 분배금 감소(기본, 원래 점수)
   *   <li>{@link #TOTAL_RETURN} 총수익 - 12 개월 총수익률(가격 변화 + 분배금). 분배하면서 원금을 지키는가
   *   <li>{@link #STABILITY} 안정성 - 배당 점수 &divide; 변동성. 덜 흔들리면서 배당하는가(분배금 감소 감점은 그대로 안고 간다)
   *   <li>{@link #STEADY} 꾸준함 - 최근 12 회 중 분배금이 직전보다 줄어든 횟수가 적은 차례(같으면 배당 점수). 값은 "안 줄어든 비율(%)"
   * </ul>
   *
   * <p>모든 기준은 값이 같으면 배당 점수가 높은 쪽, 그래도 같으면 종목코드 차례다.
   */
  public enum Basis {
    DIVIDEND("dividend"),
    TOTAL_RETURN("total"),
    STABILITY("stable"),
    STEADY("steady");

    private final String param;

    Basis(String param) {
      this.param = param;
    }

    /** 주소에 싣는 값. */
    public String param() {
      return param;
    }

    /** 주소 값 -&gt; 기준. 모르는 값 · 빈 값은 기본(배당). */
    public static Basis of(String param) {
      for (Basis basis : values()) {
        if (basis.param.equals(param)) {
          return basis;
        }
      }
      return DIVIDEND;
    }
  }

  /** 총수익 기준이 보는 기간(개월). 자리 추천은 화면의 기간 단추와 상관없이 이 기간이다 - 시뮬레이터에는 기간 단추가 없다. */
  public static final int TOTAL_RETURN_MONTHS = 12;

  /**
   * 후보 한 종목. 이 클래스는 응답 DTO 를 모른다.
   *
   * @param totalReturnPct {@value #TOTAL_RETURN_MONTHS} 개월 총수익률(없으면 총수익 기준의 후보가 못 된다)
   * @param volatilityPct 연환산 변동성(없거나 0 이면 안정성 기준의 후보가 못 된다)
   * @param payoutCutCount 최근 12 회 중 분배금이 직전보다 줄어든 횟수(없으면 꾸준함 기준의 후보가 못 된다)
   * @param payoutCutPairs 견준 쌍의 수
   */
  public record ContributionCandidate(
      String symbol,
      String name,
      String payoutWindow,
      BigDecimal taxableRatioPct,
      BigDecimal annualYieldPct,
      BigDecimal payoutTrendPct,
      BigDecimal totalReturnPct,
      BigDecimal volatilityPct,
      Integer payoutCutCount,
      Integer payoutCutPairs) {

    /** 배당 기준만 쓸 때. */
    public ContributionCandidate(
        String symbol,
        String name,
        String payoutWindow,
        BigDecimal taxableRatioPct,
        BigDecimal annualYieldPct,
        BigDecimal payoutTrendPct) {
      this(
          symbol,
          name,
          payoutWindow,
          taxableRatioPct,
          annualYieldPct,
          payoutTrendPct,
          null,
          null,
          null,
          null);
    }

    /** 배당 점수(감점 뒤). 근거로 적는다. */
    public BigDecimal dividendScore() {
      return score(annualYieldPct, payoutTrendPct);
    }
  }

  /**
   * 한 자리에서 한 기준의 1위(비교표 한 줄 - 사용자 질문 2026-10-06 "기준에 대해 추천을 어떻게 보여줄거야").
   *
   * @param value 그 기준의 값
   * @param candidate 근거를 적을 원값(연배당 · 추세 · 총수익률 · 변동성 · 감소 횟수)
   */
  public record BasisTop(Basis basis, ContributionCandidate candidate, BigDecimal value) {}

  /**
   * 같은 자리에서 다른 기준이면 뽑혔을 종목. 고른 기준의 1위와 같으면 싣지 않는다.
   *
   * @param name 종목명(없으면 코드)
   */
  public record Alternative(Basis basis, String symbol, String name) {}

  /**
   * 한 자리의 결과.
   *
   * @param runnerUpSymbol 같은 자리의 다음 후보. 화면이 "무엇을 제치고 뽑혔는지" 를 말할 수 있게 함께 준다
   * @param runnerUpName 다음 후보의 종목명. 코드만 적으면 무슨 종목인지 알 수 없다(사용자 요청 2026-09-30) - 이름이 없으면 코드
   * @param score 고른 기준의 값(배당 = 점수, 총수익 = 총수익률 %, 안정성 = 점수 &divide; 변동성)
   * @param tied 점수가 같은 후보가 있는가. 그러면 화면이 "고를 것이 갈리지 않는다" 를 알려야 한다
   * @param basis 이 결과를 낸 기준
   * @param totalReturnPct 뽑힌 종목의 총수익률(근거로 적는다)
   * @param volatilityPct 뽑힌 종목의 변동성(근거로 적는다)
   * @param alternatives 다른 기준이면 다른 종목이 뽑히는 경우(기준 차례). 같으면 비어 있다
   * @param tops 기준마다의 1위(기준 차례, 값을 낼 후보가 없는 기준은 빠진다) - 비교표
   * @param agreement 고른 기준의 1위와 같은 종목을 1위로 꼽은 기준 수(자기 포함). 여럿이 꼽을수록 굳은 추천이다
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
      boolean tied,
      Basis basis,
      BigDecimal totalReturnPct,
      BigDecimal volatilityPct,
      List<Alternative> alternatives,
      List<BasisTop> tops,
      int agreement) {}

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
   * <p>적립 추천({@link #pickFromCatalog})과 같은 출처(카탈로그의 지급 시기 &middot; 1 년 과세표준 비중)와 같은 경계({@link
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

  /**
   * 기준 값. 낼 수 없으면 null(그 기준의 후보가 못 된다). 소수 둘째 자리 - 화면에 적는 값과 순위를 정하는 값이 같아야 한다.
   *
   * <p>안정성은 배당 점수를 변동성으로 나눈다. 연배당만 나누면 분배금을 줄이고 있는 종목이 다시 위로 온다 - 감점은 기준과 상관없는 경고다.
   */
  public static BigDecimal valueOf(
      Basis basis,
      BigDecimal annualYieldPct,
      BigDecimal payoutTrendPct,
      BigDecimal totalReturnPct,
      BigDecimal volatilityPct) {
    BigDecimal dividendScore = score(annualYieldPct, payoutTrendPct);
    return switch (basis) {
      case DIVIDEND -> dividendScore;
      case TOTAL_RETURN ->
          totalReturnPct == null ? null : totalReturnPct.setScale(2, RoundingMode.HALF_UP);
      case STABILITY ->
          dividendScore == null || volatilityPct == null || volatilityPct.signum() <= 0
              ? null
              : dividendScore.divide(volatilityPct, 2, RoundingMode.HALF_UP);
      // 꾸준함은 감소 횟수가 있어야 낸다 - 이 오버로드로는 알 수 없다.
      case STEADY -> null;
    };
  }

  /** 기준 값(후보의 원값으로). 꾸준함 = 안 줄어든 비율(%) = (견준 쌍 &minus; 감소) &divide; 견준 쌍. */
  public static BigDecimal valueOf(Basis basis, ContributionCandidate candidate) {
    if (basis == Basis.STEADY) {
      if (candidate.dividendScore() == null
          || candidate.payoutCutCount() == null
          || candidate.payoutCutPairs() == null
          || candidate.payoutCutPairs() <= 0) {
        return null;
      }
      return BigDecimal.valueOf(candidate.payoutCutPairs() - candidate.payoutCutCount())
          .multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(candidate.payoutCutPairs()), 2, RoundingMode.HALF_UP);
    }
    return valueOf(
        basis,
        candidate.annualYieldPct(),
        candidate.payoutTrendPct(),
        candidate.totalReturnPct(),
        candidate.volatilityPct());
  }

  /** 카탈로그 한 줄 &rarr; 후보. 두 화면(자리 추천 · 지금 눈여겨볼 종목)이 같은 원값을 쓰게 한 곳에서 만든다. */
  public static ContributionCandidate candidateOf(
      net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse item) {
    return new ContributionCandidate(
        item.stockItemSymbol(),
        item.stockItemName(),
        item.payoutWindow(),
        item.averageTaxableBaseRatio1y(),
        item.annualYieldPct(),
        item.payoutTrendPct(),
        totalReturnOf(item),
        item.volatilityPct(),
        item.payoutCutCount(),
        item.payoutCutPairs());
  }

  /** 한 기준의 순서: 값 높은 차례 &rarr; 배당 점수 높은 차례 &rarr; 종목코드. */
  public static Comparator<ContributionCandidate> order(Basis basis) {
    return Comparator.comparing((ContributionCandidate one) -> valueOf(basis, one))
        .reversed()
        .thenComparing(
            ContributionCandidate::dividendScore, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(ContributionCandidate::symbol);
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
   * 등록된 월배당 종목 전부를 후보로 삼아 고른다 &mdash; 시뮬레이터 월배당 탭과 월배당 ETF 목록의 "이번 적립" 배지가 같이 쓴다.
   *
   * <p>보유 여부는 묻지 않는다(사용자 요청 2026-10-02: "보유 여부와 상관없이 추천하는게 좋을거 같아"). 예전에는 보유 종목만 후보였다 - 그러면 점수가 더
   * 높은 종목을 아직 안 샀다는 이유로 권하지 못했다.
   *
   * <p>두 화면이 후보를 따로 만들면 언젠가 다른 답을 낸다. 그래서 여기 하나만 둔다.
   *
   * @param catalog 월배당 카탈로그(연배당 수익률 · 추세 · 과세표준 비중이 종목 단위로 들어 있다)
   */
  public List<ContributionPick> pickFromCatalog(
      List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse> catalog) {
    return pickFromCatalog(catalog, Basis.DIVIDEND);
  }

  /** 기준을 골라 자리마다 하나씩(사용자 요청 2026-10-06). 후보는 기준과 상관없이 같다. */
  public List<ContributionPick> pickFromCatalog(
      List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse> catalog,
      Basis basis) {
    if (catalog == null) {
      return List.of();
    }
    return pick(
        catalog.stream()
            .filter(item -> item != null && item.stockItemSymbol() != null)
            .map(MonthlyContributionPickSupport::candidateOf)
            .toList(),
        basis);
  }

  /** 카탈로그의 {@value #TOTAL_RETURN_MONTHS} 개월 총수익률. 이력이 짧아 그 기간이 없으면 null. */
  public static BigDecimal totalReturnOf(
      net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse item) {
    if (item == null || item.periodReturns() == null) {
      return null;
    }
    return item.periodReturns().stream()
        .filter(one -> one != null && one.months() == TOTAL_RETURN_MONTHS)
        .map(
            net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse.PeriodReturnView
                ::totalReturnPct)
        .findFirst()
        .orElse(null);
  }

  /** 배당 기준으로 자리마다 하나씩. */
  public List<ContributionPick> pick(List<ContributionCandidate> candidates) {
    return pick(candidates, Basis.DIVIDEND);
  }

  /**
   * 자리마다 하나씩 고른다.
   *
   * <p>지급 시기나 과세표준 비중을 모르는 종목은 <b>자리를 못 정하므로 뺀다</b> &mdash; 아무 자리에나 넣으면 엉뚱한 계좌를 권하게 된다. 점수를 못 내는
   * 종목(연배당 수익률이 없는 종목)도 뺀다. 고른 기준의 값을 못 내는 종목(총수익률 · 변동성이 없는 종목)은 그 기준에서만 빠진다.
   *
   * @return 지급 시기 &rarr; 계좌 순서로 정렬된 결과. 후보가 하나뿐인 자리도 그대로 넣는다(그 자리에서는 고민할 것이 없다는 뜻)
   */
  public List<ContributionPick> pick(List<ContributionCandidate> candidates, Basis basis) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    Basis chosen = basis != null ? basis : Basis.DIVIDEND;

    Map<String, List<ContributionCandidate>> buckets = new LinkedHashMap<>();
    for (ContributionCandidate candidate : candidates) {
      if (candidate == null || !hasWindow(candidate.payoutWindow())) {
        continue;
      }
      String account = accountOf(candidate.taxableRatioPct());
      // 분배금 추세를 모르는 종목(지급 이력 6 회 미만)은 뺀다(사용자 결정 2026-10-01). 추세를 모르면 감점할 수 없어, 삭감 중인지 모르는
      // 종목이 연배당만으로 자리 1위가 될 수 있었다 - "지금 눈여겨볼 종목" · 표 점수 열은 이미 이 종목에 점수를 안 낸다(두 곳이 같은 말).
      // 이 조건은 기준과 상관없다 - 기준마다 후보가 다르면 "다른 기준이면" 이 같은 종목들 사이의 비교가 아니게 된다.
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
      List<ContributionCandidate> list = ranked(entry.getValue(), chosen);
      if (list.isEmpty()) {
        // 이 기준의 값을 낼 수 있는 후보가 없는 자리 - 빈 카드를 내지 않는다.
        continue;
      }

      ContributionCandidate best = list.get(0);
      ContributionCandidate next = list.size() > 1 ? list.get(1) : null;
      BigDecimal bestScore = valueOf(chosen, best);
      BigDecimal nextScore = next != null ? valueOf(chosen, next) : null;

      // 다른 기준이면 누가 뽑히나 - 같은 후보에서 셈만 바꿔 본다. 고른 기준의 1위와 같으면 말하지 않는다.
      List<Alternative> alternatives = new ArrayList<>();
      List<BasisTop> tops = new ArrayList<>();
      int agreement = 0;
      for (Basis other : Basis.values()) {
        List<ContributionCandidate> otherRanked = ranked(entry.getValue(), other);
        if (otherRanked.isEmpty()) {
          continue;
        }
        ContributionCandidate top = otherRanked.get(0);
        tops.add(new BasisTop(other, top, valueOf(other, top)));
        if (top.symbol().equals(best.symbol())) {
          agreement++;
        } else if (other != chosen) {
          alternatives.add(new Alternative(other, top.symbol(), displayName(top)));
        }
      }

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
              next == null ? null : displayName(next),
              nextScore,
              nextScore != null && nextScore.compareTo(bestScore) == 0,
              chosen,
              best.totalReturnPct(),
              best.volatilityPct(),
              List.copyOf(alternatives),
              List.copyOf(tops),
              agreement));
    }

    // 화면 순서: 월중 먼저, 그 안에서 위탁 먼저. 매번 같은 순서로 보여야 눈이 익는다.
    picks.sort(
        Comparator.comparingInt((ContributionPick pick) -> windowOrder(pick.payoutWindow()))
            .thenComparingInt(pick -> ACCOUNT_BROKERAGE.equals(pick.account()) ? 0 : 1));
    return List.copyOf(picks);
  }

  /** 한 자리의 후보를 기준 차례로({@link #order}). 값을 못 내는 후보는 뺀다 - 새로 고칠 때마다 답이 바뀌면 추천이 아니다. */
  private static List<ContributionCandidate> ranked(List<ContributionCandidate> slot, Basis basis) {
    List<ContributionCandidate> list = new ArrayList<>();
    for (ContributionCandidate one : slot) {
      if (valueOf(basis, one) != null) {
        list.add(one);
      }
    }
    list.sort(order(basis));
    return list;
  }

  private static String displayName(ContributionCandidate candidate) {
    return candidate.name() != null && !candidate.name().isBlank()
        ? candidate.name()
        : candidate.symbol();
  }

  private static boolean hasWindow(String payoutWindow) {
    return "MID_MONTH".equals(payoutWindow) || "MONTH_END".equals(payoutWindow);
  }

  private static int windowOrder(String payoutWindow) {
    return "MID_MONTH".equals(payoutWindow) ? 0 : 1;
  }
}
