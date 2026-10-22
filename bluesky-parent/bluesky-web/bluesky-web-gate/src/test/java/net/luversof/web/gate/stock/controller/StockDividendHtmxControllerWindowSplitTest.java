package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 요약의 월중 · 월말 · 기타 몫(StockDividendHtmxController.splitByPayoutWindow).
 *
 * <p>2026-10-22 까지 배당 목록 핸들러 안에 지역 변수 여섯으로 적혀 있었고, 이 나눔을 직접 보는 시험이 없었다.
 */
class StockDividendHtmxControllerWindowSplitTest {

  private static final UUID MID = new UUID(0, 1);
  private static final UUID END = new UUID(0, 2);
  private static final UUID BOTH = new UUID(0, 3);
  private static final UUID QUARTERLY = new UUID(0, 4);
  private static final UUID NOT_LISTED = new UUID(0, 5);

  /** 종목 id 와 세후 금액만 채운 배당 행(나머지 성분은 이 계산이 보지 않는다). */
  private static DividendView view(UUID stockItemId, String net) {
    try {
      RecordComponent[] components = DividendView.class.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] values = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        String name = components[i].getName();
        if (name.equals("stockItemId")) {
          values[i] = stockItemId;
        } else if (name.equals("netAmount")) {
          values[i] = net == null ? null : new BigDecimal(net);
        } else if (types[i] == boolean.class) {
          values[i] = false;
        } else if (types[i] == int.class) {
          values[i] = 0;
        } else if (types[i] == long.class) {
          values[i] = 0L;
        }
      }
      return DividendView.class.getDeclaredConstructor(types).newInstance(values);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final List<StockItem> STOCKS =
      List.of(
          new StockItem(MID, "a", "월중 ETF", "KRX", List.of("월중배당", "ETF")),
          new StockItem(END, "b", "월말 ETF", "KRX", List.of("월말배당")),
          new StockItem(BOTH, "c", "둘 다", "KRX", List.of("월말배당", "월중배당")),
          new StockItem(QUARTERLY, "d", "분기배당주", "KRX", null));

  @Test
  void 태그로_나누고_목록에_없는_종목과_태그_없는_종목은_기타다() {
    var split =
        StockDividendHtmxController.splitByPayoutWindow(
            Arrays.asList(
                view(MID, "1000"),
                view(MID, "250"),
                view(END, "700"),
                view(QUARTERLY, "300"),
                view(NOT_LISTED, "40"),
                view(END, null)),
            STOCKS);
    assertThat(split.midMonthNetAmount()).isEqualByComparingTo("1250");
    assertThat(split.midMonthCount()).isEqualTo(2);
    assertThat(split.monthEndNetAmount()).as("값 없는 행은 0 으로 더하되 건수는 센다").isEqualByComparingTo("700");
    assertThat(split.monthEndCount()).isEqualTo(2);
    assertThat(split.otherNetAmount()).isEqualByComparingTo("340");
    assertThat(split.otherCount()).isEqualTo(2);
  }

  @Test
  void 두_태그가_다_붙으면_월중으로_센다() {
    var split = StockDividendHtmxController.splitByPayoutWindow(List.of(view(BOTH, "90")), STOCKS);
    assertThat(split.midMonthNetAmount()).isEqualByComparingTo("90");
    assertThat(split.monthEndCount()).isZero();
  }

  @Test
  void 배당이_없으면_모두_0_이다() {
    var split = StockDividendHtmxController.splitByPayoutWindow(List.of(), STOCKS);
    assertThat(split.midMonthNetAmount()).isEqualByComparingTo("0");
    assertThat(split.monthEndNetAmount()).isEqualByComparingTo("0");
    assertThat(split.otherNetAmount()).isEqualByComparingTo("0");
    assertThat(split.midMonthCount() + split.monthEndCount() + split.otherCount()).isZero();
  }
}
