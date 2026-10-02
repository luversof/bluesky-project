package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.ContributionCandidate;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.ContributionPick;

/**
 * 이번에 무엇을 적립할까(사용자 요청 2026-09-22).
 *
 * <p>자리는 <b>지급 시기 &times; 계좌</b>로 갈린다. 계좌는 과세표준 비중 10% 를 경계로 위탁 / ISA·연금이다(사용자 규칙 &mdash; 실제 운영도
 * 그렇다).
 *
 * <p>점수는 <b>연배당 수익률 + min(분배금 추세, 0)</b>(사용자 결정). 여기서 못 박는 것은 "줄고 있으면 깎는다" 는 쪽이다 &mdash; 그게 없으면 삭감
 * 중인 종목을 계속 담게 된다.
 */
class MonthlyContributionPickSupportTest {

  private final MonthlyContributionPickSupport support = new MonthlyContributionPickSupport();

  private static ContributionCandidate candidate(
      String symbol, String payoutWindow, String taxableRatio, String annualYield, String trend) {
    return new ContributionCandidate(
        symbol,
        "이름 " + symbol,
        payoutWindow,
        taxableRatio == null ? null : new BigDecimal(taxableRatio),
        annualYield == null ? null : new BigDecimal(annualYield),
        trend == null ? null : new BigDecimal(trend));
  }

  private static net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse catalog(
      String symbol, String payoutWindow, String taxableRatio, String annualYield, String trend) {
    return new net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse(
        null,
        symbol,
        "이름 " + symbol,
        payoutWindow,
        null,
        null,
        true,
        1,
        12,
        null,
        null,
        null,
        null,
        taxableRatio == null ? null : new BigDecimal(taxableRatio),
        null,
        null,
        null,
        null,
        annualYield == null ? null : new BigDecimal(annualYield),
        null,
        java.util.List.of(),
        null,
        trend == null ? null : new BigDecimal(trend),
        null,
        null,
        null,
        null,
        null);
  }

  /**
   * 보유 여부와 상관없이 등록된 종목 전부가 후보다(사용자 요청 2026-10-02: "보유 여부와 상관없이 추천하는게 좋을거 같아"). 예전(2026-09-23)에는 보유
   * 종목만 후보라 점수가 더 높은 종목을 아직 안 샀다는 이유로 못 권했다.
   */
  @Test
  void 보유_여부와_상관없이_전_종목이_후보다() {
    var picks =
        support.pickFromCatalog(
            java.util.List.of(
                catalog("A00001", "MID_MONTH", "4.00", "10.00", "0.00"),
                catalog("B00002", "MID_MONTH", "4.00", "8.00", "0.00"),
                catalog("C00003", "MID_MONTH", "4.00", "30.00", "0.00")));

    assertThat(picks)
        .as("점수가 가장 높은 C00003 이 고른 종목 - 보유 목록을 묻지 않는다")
        .extracting(ContributionPick::symbol)
        .containsExactly("C00003");
    assertThat(picks.get(0).runnerUpSymbol()).as("차점은 A00001").isEqualTo("A00001");
    assertThat(support.pickFromCatalog(null)).isEmpty();
    assertThat(support.pickFromCatalog(java.util.List.of())).isEmpty();
  }

  /** 두 화면이 같은 메서드를 써야 같은 답이 나온다 - 후보를 따로 만들면 언젠가 갈린다. */
  @Test
  void 시뮬레이터와_목록이_같은_메서드를_쓴다() throws java.io.IOException {
    String base = "src/main/java/net/luversof/web/gate/stock/controller/";
    String simulator =
        java.nio.file.Files.readString(java.nio.file.Path.of(base + "StockViewController.java"));
    String etf =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(base + "StockMonthlyEtfViewController.java"));

    assertThat(simulator)
        .contains("monthlyContributionPickSupport.pickFromCatalog(catalog)")
        .as("시뮬레이터가 후보를 따로 만들면 두 화면이 갈린다")
        .doesNotContain("ContributionCandidate(");
    assertThat(etf)
        .contains("monthlyContributionPickSupport.pickFromCatalog(catalog)")
        .as("후보에 보유 여부를 묻지 않는다 - 보유 스냅샷을 이 추천 때문에 부르지 않는다(2026-10-02)")
        .doesNotContain("heldSnapshotFuture")
        .contains("model.addAttribute(" + (char) 34 + "monthlyEtfContributionPicks" + (char) 34);
    String template =
        java.nio.file.Files.readString(java.nio.file.Path.of("src/main/jte/stock/monthlyEtf.jte"));
    assertThat(template)
        .as("배지는 행의 종목코드로 추천을 찾아 단다")
        .contains("monthlyEtfContributionPicks.get(row.stockItemSymbol())")
        .contains(
            "data-contribution-pick="
                + (char) 34
                + "${contributionPick.payoutWindow()}|${contributionPick.account()}"
                + (char) 34);
  }

  @Test
  void 과세표준_10퍼센트가_위탁과_연금을_가른다() {
    // 사용자 규칙: 10% 이내면 위탁, 그보다 크면 ISA/연금.
    assertThat(support.accountOf(new BigDecimal("4.04")))
        .isEqualTo(MonthlyContributionPickSupport.ACCOUNT_BROKERAGE);
    assertThat(support.accountOf(new BigDecimal("10.00")))
        .as("10% 는 위탁 쪽이다(이내)")
        .isEqualTo(MonthlyContributionPickSupport.ACCOUNT_BROKERAGE);
    assertThat(support.accountOf(new BigDecimal("10.01")))
        .isEqualTo(MonthlyContributionPickSupport.ACCOUNT_PENSION);
    assertThat(support.accountOf(new BigDecimal("100.00")))
        .isEqualTo(MonthlyContributionPickSupport.ACCOUNT_PENSION);
    assertThat(support.accountOf(null)).as("모르는 비중을 위탁으로 단정하면 엉뚱한 계좌를 권한다").isNull();
  }

  @Test
  void 분배금이_줄고_있으면_그만큼_깎는다() {
    // 실측 2026-09-22: 연배당 17.04% 인데 추세 -8.45% 라 8.59 가 된다.
    assertThat(support.scoreOf(new BigDecimal("17.04"), new BigDecimal("-8.45")))
        .isEqualByComparingTo("8.59");
    // 늘고 있으면 더 주지 않는다 - 이미 수익률에 들어오고 있다.
    assertThat(support.scoreOf(new BigDecimal("9.24"), new BigDecimal("21.96")))
        .isEqualByComparingTo("9.24");
    assertThat(support.scoreOf(new BigDecimal("11.64"), null))
        .as("추세를 모르면 깎지 않는다")
        .isEqualByComparingTo("11.64");
    assertThat(support.scoreOf(null, new BigDecimal("5"))).isNull();
  }

  @Test
  void 자리마다_하나씩_고른다() {
    // 실측 2026-09-22 의 보유 8 종목.
    List<ContributionPick> picks =
        support.pick(
            List.of(
                candidate("0094M0", "MID_MONTH", "4.04", "23.88", "62.86"),
                candidate("498400", "MID_MONTH", "4.20", "14.76", "14.27"),
                candidate("0018C0", "MID_MONTH", "25.05", "17.04", "-8.45"),
                candidate("476800", "MID_MONTH", "91.12", "9.24", "21.96"),
                candidate("472150", "MONTH_END", "3.36", "20.52", "24.79"),
                candidate("475720", "MONTH_END", "4.61", "18.96", "22.27"),
                candidate("0104P0", "MONTH_END", "31.26", "11.64", "-0.34"),
                candidate("329200", "MONTH_END", "100.00", "9.72", "0.00")));

    assertThat(picks).hasSize(4);
    assertThat(picks)
        .extracting(ContributionPick::symbol)
        .as("월중 위탁 · 월중 연금 · 월말 위탁 · 월말 연금 순")
        .containsExactly("0094M0", "476800", "472150", "0104P0");

    // 월중 · 연금은 감점이 결과를 뒤집는 자리다 - 0018C0 은 연배당이 더 높은데도 밀린다.
    ContributionPick midPension = picks.get(1);
    assertThat(midPension.score()).isEqualByComparingTo("9.24");
    assertThat(midPension.runnerUpSymbol()).isEqualTo("0018C0");
    assertThat(midPension.runnerUpName())
        .as("코드만 적으면 무슨 종목인지 알 수 없다(사용자 요청 2026-09-30)")
        .isEqualTo("이름 0018C0");
    assertThat(midPension.runnerUpScore()).isEqualByComparingTo("8.59");
    assertThat(midPension.tied()).isFalse();
  }

  @Test
  void 자리를_못_정하는_종목은_뺀다() {
    // 지급 시기를 모르면 어느 자리에 넣을지 알 수 없다 - 아무 자리에나 넣으면 엉뚱한 계좌를 권한다.
    List<ContributionPick> picks =
        support.pick(
            List.of(
                candidate("AAAAAA", "UNKNOWN", "4.00", "20.00", "1.00"),
                candidate("BBBBBB", "OTHER", "4.00", "20.00", "1.00"),
                candidate("CCCCCC", "MID_MONTH", null, "20.00", "1.00"),
                candidate("DDDDDD", "MID_MONTH", "4.00", null, "1.00"),
                candidate("EEEEEE", "MID_MONTH", "4.00", "10.00", "1.00")));

    assertThat(picks).extracting(ContributionPick::symbol).containsExactly("EEEEEE");
  }

  /**
   * 분배금 추세를 모르는 종목(지급 이력 6 회 미만)은 자리 후보에서 뺀다(사용자 결정 2026-10-01). 예전에는 감점 없이 연배당만으로 점수를 매겨, 삭감 중인지
   * 모르는 종목이 자리 1위가 될 수 있었다 - "지금 눈여겨볼 종목" · 월배당 ETF 점수 열과 같은 규칙.
   */
  @Test
  void 추세를_모르는_종목은_자리_후보에서_뺀다() {
    List<ContributionPick> picks =
        support.pick(
            List.of(
                candidate("NEW001", "MID_MONTH", "4.00", "30.00", null),
                candidate("OLD001", "MID_MONTH", "4.00", "12.00", "-1.00"),
                candidate("OLD002", "MID_MONTH", "4.00", "10.00", "0.00"),
                candidate("NEW002", "MONTH_END", "4.00", "25.00", null)));

    assertThat(picks)
        .extracting(ContributionPick::symbol)
        .as("연배당 30% 여도 추세를 모르면 1위가 될 수 없다 · 추세 모르는 종목뿐인 월말 자리는 비운다")
        .containsExactly("OLD001");
    assertThat(picks.get(0).runnerUpSymbol()).as("차점에서도 뺀다").isEqualTo("OLD002");
  }

  @Test
  void 점수가_같으면_같은_답을_낸다() {
    // 새로 고칠 때마다 답이 바뀌면 추천이 아니다 - 종목코드 순으로 정한다.
    List<ContributionCandidate> forward =
        List.of(
            candidate("BBBBBB", "MID_MONTH", "4.00", "12.00", "0.00"),
            candidate("AAAAAA", "MID_MONTH", "4.00", "12.00", "0.00"));
    List<ContributionCandidate> backward =
        List.of(
            candidate("AAAAAA", "MID_MONTH", "4.00", "12.00", "0.00"),
            candidate("BBBBBB", "MID_MONTH", "4.00", "12.00", "0.00"));

    assertThat(support.pick(forward).get(0).symbol()).isEqualTo("AAAAAA");
    assertThat(support.pick(backward).get(0).symbol()).isEqualTo("AAAAAA");
    assertThat(support.pick(forward).get(0).tied()).as("갈리지 않는다는 것을 화면이 말해야 한다").isTrue();
  }

  @Test
  void 후보가_하나뿐인_자리도_알린다() {
    // 그 자리에서는 고민할 것이 없다는 뜻이다 - 빼 버리면 화면에서 자리가 사라진다.
    List<ContributionPick> picks =
        support.pick(List.of(candidate("AAAAAA", "MONTH_END", "50.00", "8.00", "1.00")));

    assertThat(picks).hasSize(1);
    assertThat(picks.get(0).account()).isEqualTo(MonthlyContributionPickSupport.ACCOUNT_PENSION);
    assertThat(picks.get(0).runnerUpSymbol()).isNull();
    assertThat(picks.get(0).runnerUpName()).isNull();
    assertThat(picks.get(0).tied()).isFalse();
  }

  @Test
  void 없는_목록을_견딘다() {
    assertThat(support.pick(null)).isEmpty();
    assertThat(support.pick(List.of())).isEmpty();
  }

  /**
   * 시뮬레이터 월배당 표의 지급 시기 &middot; 계좌 필터(사용자 요청 2026-09-23, 결정: 적립 추천과 같은 규칙).
   *
   * <p>실측 2026-09-23: 표에 적힌 저장 비중(0018C0 0%)으로 가르면 위탁이 되지만 카드는 카탈로그 비중(25.05%)으로 ISA/연금 자리에 둔다. 필터는
   * 카드와 같은 답을 내야 한다.
   */
  @Test
  void 필터는_추천_카드와_같은_자리로_가른다() {
    List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse> items =
        List.of(
            catalog("0018C0", "MID_MONTH", "25.05", "17.04", "-8.45"),
            catalog("0094M0", "MID_MONTH", "4.04", "24.00", "62.86"),
            catalog("498400", "MID_MONTH", "4.2", "14.88", "10.00"),
            catalog("476800", "MID_MONTH", "91.12", "9.12", "21.96"),
            catalog("472150", "MONTH_END", "3.36", "20.64", "24.79"),
            catalog("329200", "MONTH_END", "100", "8.00", "1.00"),
            catalog("UNKNWN", "MONTH_END", null, "9.00", "1.00"));

    assertThat(support.symbolsInSlot("MID_MONTH", "BROKERAGE", items))
        .containsExactlyInAnyOrder("0094M0", "498400");
    assertThat(support.symbolsInSlot("MID_MONTH", "PENSION", items))
        .as("표에는 0% 로 적혀도 카탈로그 비중 25.05% 라 ISA/연금")
        .containsExactlyInAnyOrder("0018C0", "476800");
    assertThat(support.symbolsInSlot("", "PENSION", items))
        .containsExactlyInAnyOrder("0018C0", "476800", "329200");
    assertThat(support.symbolsInSlot("MONTH_END", "", items))
        .as("계좌 조건이 없으면 비중을 몰라도 남긴다")
        .containsExactlyInAnyOrder("472150", "329200", "UNKNWN");
    assertThat(support.symbolsInSlot("MONTH_END", "BROKERAGE", items))
        .as("계좌 조건이 걸렸는데 비중을 모르면 자리를 못 정해 뺀다")
        .containsExactly("472150");

    // 카드가 고른 종목은 반드시 그 자리의 필터 결과 안에 있다.
    var picks = support.pickFromCatalog(items);
    assertThat(picks).isNotEmpty();
    for (var pick : picks) {
      assertThat(support.symbolsInSlot(pick.payoutWindow(), pick.account(), items))
          .as(pick.payoutWindow() + " / " + pick.account())
          .contains(pick.symbol());
    }
  }

  @Test
  void 모르는_필터_값은_조건을_걸지_않는다() {
    assertThat(MonthlyContributionPickSupport.resolveSlotWindow("MID_MONTH"))
        .isEqualTo("MID_MONTH");
    assertThat(MonthlyContributionPickSupport.resolveSlotWindow("MONTH_END"))
        .isEqualTo("MONTH_END");
    assertThat(MonthlyContributionPickSupport.resolveSlotWindow("OTHER")).isEmpty();
    assertThat(MonthlyContributionPickSupport.resolveSlotWindow(null)).isEmpty();
    assertThat(MonthlyContributionPickSupport.resolveSlotAccount("BROKERAGE"))
        .isEqualTo("BROKERAGE");
    assertThat(MonthlyContributionPickSupport.resolveSlotAccount("PENSION")).isEqualTo("PENSION");
    assertThat(MonthlyContributionPickSupport.resolveSlotAccount("pension")).isEmpty();
    assertThat(support.symbolsInSlot("MID_MONTH", "", null)).isEmpty();
  }
}
