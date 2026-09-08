package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.util.StockCoreHoldingUtil.Split;

/**
 * 핵심 보유 vs 나머지 &mdash; 핵심은 종목 태그가 정한다.
 *
 * <p>실측 2026-09-08: 한 종목이 평가액의 83.9% 라 모든 차트가 그 종목 그래프였다. 배당 ETF 8 종의 성적은 어디에도 나란히 없었다. 평가액 1 위를
 * 자동으로 핵심으로 삼던 것을 태그 선언으로 바꿨다 &mdash; 주가가 움직이는 것만으로 기준이 말없이 바뀌면 안 된다.
 */
class StockCoreHoldingUtilTest {

  private static final UUID CORE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID ETF1 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
  private static final UUID ETF2 = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
  private static final UUID SOLD = UUID.fromString("00000000-0000-0000-0000-0000000000d4");

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private static TradeProfit row(
      UUID id, String name, String evalAmount, String evalProfit, String realized) {
    return TradeProfit.ofStockStatus(
        id, name, null, 0, null, bd(evalAmount), bd(evalProfit), bd(realized), bd("0"));
  }

  private static StockItem item(UUID id, String name, String... tags) {
    return new StockItem(id, "000000", name, "KOSPI", List.of(tags));
  }

  // ---------------------------------------------------------------- 태그로 고르기

  @Test
  void 핵심_태그가_달린_종목만_고른다() {
    List<StockItem> items =
        List.of(
            item(CORE, "삼성전자", StockCoreHoldingUtil.CORE_TAG),
            item(ETF1, "KODEX 리츠", "ETF", "월배당"),
            item(ETF2, "RISE 200", "ETF"));

    assertThat(StockCoreHoldingUtil.coreStockItemIds(items)).containsExactly(CORE);
  }

  @Test
  void 태그가_없거나_목록이_없으면_빈_집합이다() {
    assertThat(StockCoreHoldingUtil.coreStockItemIds(List.of(item(ETF1, "ETF만", "ETF")))).isEmpty();
    assertThat(StockCoreHoldingUtil.coreStockItemIds(null)).isEmpty();
  }

  /** 핵심 태그는 화면 문구가 아니라 DB(StockItemTag) 에 저장된 데이터 값이다. 로케일에 따라 바뀌면 안 된다. */
  @Test
  void 핵심_태그는_고정된_데이터_값이다() {
    assertThat(StockCoreHoldingUtil.CORE_TAG).isEqualTo("핵심");
  }

  // ---------------------------------------------------------------- 가르기

  /** 실측의 모양: 삼성전자 13.6 억(태그) + ETF 둘 + 다 판 종목 하나. */
  @Test
  void 태그_종목을_핵심으로_떼고_나머지를_합친다() {
    Split split =
        StockCoreHoldingUtil.split(
            List.of(
                row(CORE, "삼성전자", "1361610000", "999084921", "138569333"),
                row(ETF1, "KODEX 리츠", "75935770", "-12715200", "906369"),
                row(ETF2, "RISE 200", "72853290", "-234419", "0"),
                row(SOLD, "삼성SDI", "0", "0", "11097312")),
            Map.of(CORE, bd("35340449"), ETF1, bd("5385714"), ETF2, bd("9149432")),
            Set.of(CORE));

    assertThat(split).isNotNull();
    assertThat(split.core().label()).as("핵심이 하나면 그 이름").isEqualTo("삼성전자");
    assertThat(split.core().weightPct()).isEqualByComparingTo("90.1");
    assertThat(split.core().combinedProfit()).as("평가 + 실현 + 배당").isEqualByComparingTo("1172994703");
    assertThat(split.rest().stockCount()).as("보유 중인 ETF 둘 - 다 판 종목은 세지 않는다").isEqualTo(2);
    assertThat(split.rest().evaluationAmount()).isEqualByComparingTo("148789060");
    assertThat(split.rest().weightPct()).isEqualByComparingTo("9.9");
    assertThat(split.rest().realizedProfit())
        .as("다 판 종목의 실현손익도 나머지에 든다")
        .isEqualByComparingTo("12003681");
    assertThat(split.rest().dividendTotal()).isEqualByComparingTo("14535146");
    assertThat(split.rest().combinedProfit()).isEqualByComparingTo("13589208");
  }

  /** 핵심이 여럿이면 이름 대신 개수로 적는다(화면이 그렇게 쓴다). */
  @Test
  void 핵심이_여럿이면_이름을_비운다() {
    Split split =
        StockCoreHoldingUtil.split(
            List.of(
                row(CORE, "삼성전자", "600", "100", "0"),
                row(ETF1, "삼성SDI", "300", "0", "0"),
                row(ETF2, "ETF", "100", "0", "0")),
            Map.of(),
            Set.of(CORE, ETF1));

    assertThat(split.core().label()).isNull();
    assertThat(split.core().stockCount()).isEqualTo(2);
    assertThat(split.core().evaluationAmount()).isEqualByComparingTo("900");
    assertThat(split.core().weightPct()).isEqualByComparingTo("90.0");
    assertThat(split.rest().stockCount()).isEqualTo(1);
  }

  /** 같은 종목이 계좌별로 여러 줄이면 합쳐서 본다(계좌x종목 집계가 그렇게 온다). */
  @Test
  void 같은_종목의_여러_줄은_합친다() {
    Split split =
        StockCoreHoldingUtil.split(
            List.of(
                row(CORE, "삼성전자", "600", "100", "0"),
                row(CORE, "삼성전자", "400", "50", "0"),
                row(ETF1, "ETF", "100", "0", "0")),
            Map.of(),
            Set.of(CORE));

    assertThat(split.core().label()).as("두 줄이어도 한 종목이다").isEqualTo("삼성전자");
    assertThat(split.core().evaluationAmount()).isEqualByComparingTo("1000");
    assertThat(split.core().evaluationProfit()).isEqualByComparingTo("150");
    assertThat(split.core().weightPct()).isEqualByComparingTo("90.9");
  }

  /** 비중이 작아도 사용자가 태그로 선언했으면 가른다 - 자동 판정 시절의 문턱(50%)은 없앴다. */
  @Test
  void 비중이_작아도_태그를_달았으면_가른다() {
    Split split =
        StockCoreHoldingUtil.split(
            List.of(row(CORE, "작은 핵심", "100", "0", "0"), row(ETF1, "큰 나머지", "900", "0", "0")),
            Map.of(),
            Set.of(CORE));

    assertThat(split).isNotNull();
    assertThat(split.core().weightPct()).isEqualByComparingTo("10.0");
  }

  /** 태그를 하나도 안 달았으면 가를 것이 없다. */
  @Test
  void 태그가_없으면_null_이다() {
    List<TradeProfit> rows =
        List.of(row(CORE, "A", "500", "0", "0"), row(ETF1, "B", "500", "0", "0"));

    assertThat(StockCoreHoldingUtil.split(rows, Map.of(), Set.of())).isNull();
    assertThat(StockCoreHoldingUtil.split(rows, Map.of(), null)).isNull();
  }

  /** 태그는 달렸지만 이 사용자에게 그 종목의 손익도 배당도 없으면 카드가 할 말이 없다. */
  @Test
  void 태그_종목의_자료가_없으면_null_이다() {
    assertThat(
            StockCoreHoldingUtil.split(
                List.of(row(ETF1, "ETF", "500", "0", "0")), Map.of(ETF1, bd("10")), Set.of(CORE)))
        .isNull();
    assertThat(StockCoreHoldingUtil.split(List.of(), Map.of(), Set.of(CORE))).isNull();
    assertThat(StockCoreHoldingUtil.split(null, null, Set.of(CORE))).isNull();
  }

  /** 다 팔아 평가액이 0 이어도 태그가 달렸으면 실현손익·배당을 보여 준다(비중만 0). */
  @Test
  void 다_판_핵심도_실현손익과_배당을_보여준다() {
    Split split =
        StockCoreHoldingUtil.split(
            List.of(row(CORE, "판 핵심", "0", "0", "100"), row(ETF1, "ETF", "0", "0", "0")),
            Map.of(CORE, bd("50")),
            Set.of(CORE));

    assertThat(split).isNotNull();
    assertThat(split.core().stockCount()).as("보유 중이 아니다").isZero();
    assertThat(split.core().weightPct()).isEqualByComparingTo("0");
    assertThat(split.core().combinedProfit()).isEqualByComparingTo("150");
  }
}
