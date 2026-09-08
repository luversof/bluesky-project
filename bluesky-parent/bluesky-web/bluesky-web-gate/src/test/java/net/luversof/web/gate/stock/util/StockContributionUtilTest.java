package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.util.StockContributionUtil.Contribution;

/**
 * 기간 손익을 <b>종목으로</b> 쪼갠 줄.
 *
 * <p>자산 성장은 기간 손익을 시간으로만 쪼갤 수 있었다. 종목별 표는 두 군데 있었지만 둘 다 반쪽이었다 &mdash; 자산 현황은 지금 보유한 것만, 매매 화면의 종목별
 * 실현손익은 배당이 빠져 있었다. 그래서 배당까지 받고 판 종목은(실측 2026-09-07: 9 종목) 종목 단위로 어디에도 나타나지 않았다.
 */
class StockContributionUtilTest {

  private static final UUID HELD = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID SOLD = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
  private static final UUID DIVIDEND_ONLY = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static HoldingsSnapshotItem snap(
      UUID id, String name, String quantity, String unrealized) {
    return new HoldingsSnapshotItem(
        id,
        name,
        "000000",
        bd(quantity),
        bd("1000"),
        bd("1100"),
        LocalDate.parse("2026-09-07"),
        bd("11000"),
        bd(unrealized));
  }

  private static TradeProfit realized(UUID id, String name, String amount) {
    return TradeProfit.ofStockStatus(
        id,
        name,
        bd("1000"),
        0,
        bd("1100"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(amount),
        BigDecimal.ZERO);
  }

  private static Contribution find(List<Contribution> rows, UUID id) {
    return rows.stream().filter(r -> id.equals(r.stockItemId())).findFirst().orElseThrow();
  }

  /** 기여 = 평가 변동 + 실현손익 + 배당. 월별/연도별 성과가 쓰는 쪼갬과 같아야 두 표가 같은 뜻을 말한다. */
  @Test
  void 기여는_평가변동과_실현손익과_배당을_더한_값이다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            List.of(snap(HELD, "보유종목", "10", "300")),
            List.of(snap(HELD, "보유종목", "10", "1000")),
            List.of(realized(HELD, "보유종목", "200")),
            Map.of(HELD, bd("50")));

    Contribution row = find(rows, HELD);
    assertThat(row.unrealizedDelta()).as("1000 - 300").isEqualByComparingTo(bd("700"));
    assertThat(row.realizedProfit()).isEqualByComparingTo(bd("200"));
    assertThat(row.dividendTotal()).isEqualByComparingTo(bd("50"));
    assertThat(row.contribution()).isEqualByComparingTo(bd("950"));
  }

  /**
   * 기초 스냅샷이 없으면(= '전체' 를 골라 시작일이 없을 때) 기초를 0 으로 둔다.
   *
   * <p>실측 2026-09-07 전체 기간: 요약의 {@code unrealizedStart} 가 0 이고, 종목별 기여 합 1,195,952,068 이 기간 손익과 정확히
   * 같았다.
   */
  @Test
  void 기초_스냅샷이_없으면_기초를_0_으로_둔다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            null, List.of(snap(HELD, "보유종목", "10", "1000")), List.of(), Map.of());

    assertThat(find(rows, HELD).unrealizedDelta()).isEqualByComparingTo(bd("1000"));
  }

  /**
   * 그 기간에 <b>다 판</b> 종목도 줄로 나온다.
   *
   * <p>이것이 이 표를 만든 까닭이다 &mdash; 자산 현황은 보유 중인 것만 내므로 판 종목의 실현손익·배당이 종목 단위로 사라졌다.
   */
  @Test
  void 기간에_다_판_종목도_줄로_나온다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            List.of(snap(SOLD, "판종목", "10", "400")),
            // 기말에는 없다(다 팔았다).
            List.of(),
            List.of(realized(SOLD, "판종목", "900")),
            Map.of(SOLD, bd("100")));

    Contribution row = find(rows, SOLD);
    assertThat(row.heldAtEnd()).as("기말에 없으면 그 기간에 다 판 종목이다").isFalse();
    assertThat(row.unrealizedDelta()).as("0 - 400").isEqualByComparingTo(bd("-400"));
    assertThat(row.contribution()).as("-400 + 900 + 100").isEqualByComparingTo(bd("600"));
  }

  /** 배당만 받은 종목도 놓치지 않는다. 스냅샷·실현만 보면 이런 종목이 통째로 빠진다. */
  @Test
  void 배당만_받은_종목도_놓치지_않는다() {
    List<Contribution> rows =
        StockContributionUtil.of(null, List.of(), List.of(), Map.of(DIVIDEND_ONLY, bd("777")));

    assertThat(find(rows, DIVIDEND_ONLY).contribution()).isEqualByComparingTo(bd("777"));
  }

  /** 그 기간에 아무 일도 없던 종목은 줄로 내지 않는다. 0 원 줄만 늘면 표가 길어지기만 한다. */
  @Test
  void 아무_일도_없던_종목은_줄을_내지_않는다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            List.of(snap(HELD, "그대로", "10", "500")),
            List.of(snap(HELD, "그대로", "10", "500")),
            List.of(),
            Map.of());

    assertThat(rows).isEmpty();
  }

  /** 많이 벌어준 순. 이 표를 읽는 이유가 "누가 벌어줬나" 다. */
  @Test
  void 기여가_큰_종목이_위로_온다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            null,
            List.of(snap(HELD, "작게", "10", "100"), snap(SOLD, "크게", "10", "900")),
            List.of(),
            Map.of());

    assertThat(rows).extracting(Contribution::stockItemName).containsExactly("크게", "작게");
  }

  /** 합계는 위 카드의 기간 손익과 같아야 한다(실측: 세 기간 모두 차이 0). */
  @Test
  void 합계는_네_값을_각각_더한다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            List.of(snap(HELD, "보유종목", "10", "300")),
            List.of(snap(HELD, "보유종목", "10", "1000")),
            List.of(realized(HELD, "보유종목", "200"), realized(SOLD, "판종목", "900")),
            Map.of(HELD, bd("50")));

    Contribution total = StockContributionUtil.total(rows);
    assertThat(total.unrealizedDelta()).isEqualByComparingTo(bd("700"));
    assertThat(total.realizedProfit()).isEqualByComparingTo(bd("1100"));
    assertThat(total.dividendTotal()).isEqualByComparingTo(bd("50"));
    assertThat(total.contribution()).as("700 + 1,100 + 50").isEqualByComparingTo(bd("1850"));
  }

  /** 재료가 하나도 없으면 줄이 없다. */
  @Test
  void 자료가_없으면_줄이_없다() {
    assertThat(StockContributionUtil.of(null, null, null, null)).isEmpty();
    assertThat(StockContributionUtil.total(null).contribution())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  /**
   * 이름을 밖에서 채운다.
   *
   * <p>api-stock 의 {@code TradeProfit} 은 id 만 싣는 규약이라 응답에 이름 키가 없다(결함이 아니다 &mdash; 게이트의 다른 화면도
   * {@code withNames} 로 붙인다). 스냅샷에는 이름이 있지만 그건 <b>기말에 들고 있는 종목만</b>이라, 채우지 않으면 이미 다 판 종목이 전부 '-' 로
   * 나간다. 줄은 있는데 읽을 수가 없어 "보유 중인 것만 나온다" 로 보였다(44 줄 중 35 줄).
   */
  @Test
  void 다_판_종목의_이름을_밖에서_채운다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            null,
            // 기말 스냅샷에 없다 = 이미 다 판 종목이라 이름을 줄 곳이 없다.
            List.of(),
            List.of(realized(SOLD, null, "900")),
            Map.of(),
            Map.of(SOLD, "판종목"));

    assertThat(find(rows, SOLD).stockItemName()).as("이름이 없으면 줄은 있는데 읽을 수가 없다").isEqualTo("판종목");
  }

  /** 원격이 이름을 준 경우에는 그것을 그대로 쓴다. */
  @Test
  void 원격이_준_이름이_있으면_그대로_쓴다() {
    List<Contribution> rows =
        StockContributionUtil.of(
            null,
            List.of(snap(HELD, "스냅샷이름", "10", "700")),
            List.of(),
            Map.of(),
            Map.of(HELD, "마스터이름"));

    assertThat(find(rows, HELD).stockItemName()).isEqualTo("스냅샷이름");
  }

  private static List<Contribution> manyRows() {
    java.util.List<HoldingsSnapshotItem> end = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      // 기여가 10, 20, ... 200. 그리고 크게 까먹은 종목 하나.
      end.add(
          snap(
              UUID.fromString(String.format("00000000-0000-0000-0000-%012d", i)),
              "종목" + i,
              "10",
              String.valueOf((i + 1) * 10L)));
    }
    end.add(snap(SOLD, "크게까먹은종목", "10", "-5000"));
    return StockContributionUtil.of(null, end, List.of(), Map.of());
  }

  /** 기여가 큰 쪽부터 limit 줄만 내고 나머지는 한 줄로 접는다. */
  @Test
  void 줄이_많으면_접는다() {
    var folded = StockContributionUtil.fold(manyRows(), 5);

    assertThat(folded.rows()).hasSize(5);
    assertThat(folded.othersCount()).isEqualTo(16);
  }

  /**
   * 고르는 기준은 기여의 <b>절댓값</b>이다.
   *
   * <p>크게 까먹은 종목도 "누가 벌어줬나" 를 읽는 데 꼭 필요한데, 부호 순으로 자르면 그런 줄이 가장 먼저 잘려 나간다.
   */
  @Test
  void 크게_까먹은_종목은_접지_않는다() {
    var folded = StockContributionUtil.fold(manyRows(), 5);

    assertThat(folded.rows())
        .extracting(Contribution::stockItemName)
        .as("-5,000 은 어느 양수 기여보다 크다 - 부호로 자르면 이 줄이 먼저 사라진다")
        .contains("크게까먹은종목");
  }

  /** 접어도 합계는 그대로다. 접었다고 합계가 달라지면 위 카드의 기간 손익과 어긋난다. */
  @Test
  void 접어도_합계는_그대로다() {
    List<Contribution> all = manyRows();
    var folded = StockContributionUtil.fold(all, 5);

    BigDecimal visible =
        folded.rows().stream()
            .map(Contribution::contribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    assertThat(visible.add(folded.others().contribution()))
        .as("보이는 줄 + 기타 = 전체 (실측 2026-09-07: 15 줄 + 기타 29 종목, 차이 0)")
        .isEqualByComparingTo(StockContributionUtil.total(all).contribution());
  }

  /** 보이는 줄은 다시 기여 순으로. 표는 많이 벌어준 순으로 읽는다. */
  @Test
  void 보이는_줄은_기여_순으로_되돌린다() {
    var folded = StockContributionUtil.fold(manyRows(), 5);

    assertThat(folded.rows())
        .extracting(Contribution::contribution)
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
  }

  /** limit 보다 줄이 적으면 접지 않는다. 접을 것이 없는데 '기타 0 종목' 줄이 붙으면 안 된다. */
  @Test
  void 접을_것이_없으면_접지_않는다() {
    var folded = StockContributionUtil.fold(manyRows(), 100);

    assertThat(folded.othersCount()).isZero();
    assertThat(folded.others()).isNull();
  }

  // ---------------------------------------------------------------- 스냅샷 날짜

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.parse("2026-09-08");

  private static Instant seoul(String text) {
    return Instant.parse(text.replace("+09:00", "Z")).minusSeconds(9 * 3600);
  }

  /**
   * 기말은 endDate <b>직전 순간</b>의 날짜다. endDate 는 배타적이라 자정이면 하루 전이다.
   *
   * <p>실측 2026-09-08: end=06-10T00:00 인 요약의 기말 평가손익은 06-09 스냅샷(1,261,330,159)과 같고 06-10
   * 스냅샷(1,161,055,494)과는 다르다. 기말을 endDate 당일로 잡던 예전 코드는 올해 구간에서 76,066,520 원 어긋났다.
   */
  @Test
  void 기말은_배타적_endDate_의_하루_전이다() {
    var dates =
        StockContributionUtil.snapshotDates(
            seoul("2026-06-09T00:00:00+09:00"), seoul("2026-06-10T00:00:00+09:00"), SEOUL, TODAY);

    assertThat(dates.end())
        .as("end=06-10T00:00(배타) 의 기말은 06-09 다. 06-10 을 찍으면 요약보다 하루 뒤 평가액을 쓴다")
        .isEqualTo(LocalDate.parse("2026-06-09"));
    assertThat(dates.start())
        .as("기초는 시작일 당일이다(실측: start=06-09 의 기초 = 06-09 스냅샷)")
        .isEqualTo(LocalDate.parse("2026-06-09"));
  }

  /** 자정이 아닌 endDate 는 그날을 포함한다(실측: end=06-09T12:00 의 기말 = 06-09 스냅샷). */
  @Test
  void 자정이_아닌_endDate_는_그날이_기말이다() {
    var dates =
        StockContributionUtil.snapshotDates(null, seoul("2026-06-09T12:00:00+09:00"), SEOUL, TODAY);

    assertThat(dates.end()).isEqualTo(LocalDate.parse("2026-06-09"));
  }

  /** '전체' 는 시작일이 없다 - 기초 없음(0), 기말은 오늘. 요약의 기말도 현재 시점이다. */
  @Test
  void 기간이_없으면_기초는_없고_기말은_오늘이다() {
    var dates = StockContributionUtil.snapshotDates(null, null, SEOUL, TODAY);

    assertThat(dates.start()).isNull();
    assertThat(dates.end()).isEqualTo(TODAY);
  }

  /** 시간대는 요청의 것이다. 같은 순간이라도 UTC 로 읽으면 하루 앞선 날짜가 나온다. */
  @Test
  void 날짜는_요청의_시간대로_읽는다() {
    Instant end = seoul("2026-06-10T00:00:00+09:00"); // = 2026-06-09T15:00Z

    assertThat(StockContributionUtil.snapshotDates(null, end, SEOUL, TODAY).end())
        .isEqualTo(LocalDate.parse("2026-06-09"));
    assertThat(StockContributionUtil.snapshotDates(null, end, ZoneId.of("UTC"), TODAY).end())
        .as("UTC 로는 06-09T15:00 직전 = 06-09")
        .isEqualTo(LocalDate.parse("2026-06-09"));
    assertThat(
            StockContributionUtil.snapshotDates(
                    seoul("2026-06-10T00:00:00+09:00"), null, ZoneId.of("UTC"), TODAY)
                .start())
        .as("서울 06-10 자정은 UTC 로 06-09 다 - 시간대를 무시하면 기초가 하루 밀린다")
        .isEqualTo(LocalDate.parse("2026-06-09"));
  }
}
