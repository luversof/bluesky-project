package net.luversof.web.gate.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * TradeProfit 의 이름 주입(withNames). 스물일곱 자리짜리 <b>위치 기반</b> 생성자를 다시 채운다.
 *
 * <p>자리 하나가 밀리면 화면의 숫자가 통째로 다른 뜻이 되는데, 타입이 거의 다 {@code BigDecimal} 이라 컴파일러는 아무 말도 하지 않는다. 그래서 자리마다
 * 다른 값을 넣은 행으로 성분을 하나씩 비교한다 - 성분이 늘면 검사도 저절로 늘어난다(2026-10-22 까지는 스물넷을 손으로 적어 currentPriceUpdatedAt
 * 을 빠뜨렸다).
 *
 * <p>2026-10-22 까지 이 파일은 포트폴리오 팩토리 둘(ofPortfolioStock · ofPortfolioAccount)도 봤다. 그 둘을 부르던 포트폴리오 화면은
 * 2026-09-09 에 지워졌고(StockPortfolioHtmxController 머리 주석), 이 시험만 부르고 있어 팩토리와 함께 지웠다.
 */
class TradeProfitFactoryTest {

  @Test
  void 이름_주입은_나머지를_건드리지_않는다() throws ReflectiveOperationException {
    RecordComponent[] components = TradeProfit.class.getRecordComponents();
    Class<?>[] types = new Class<?>[components.length];
    Object[] values = new Object[components.length];
    for (int i = 0; i < components.length; i++) {
      types[i] = components[i].getType();
      values[i] = distinct(types[i], i);
    }
    TradeProfit source = TradeProfit.class.getDeclaredConstructor(types).newInstance(values);

    TradeProfit renamed = TradeProfit.withNames(source, "새 이름", "새 계좌");

    assertThat(renamed.stockItemName()).isEqualTo("새 이름");
    assertThat(renamed.accountName()).isEqualTo("새 계좌");
    int compared = 0;
    for (RecordComponent component : components) {
      String name = component.getName();
      if (name.equals("stockItemName") || name.equals("accountName")) {
        continue;
      }
      assertThat(component.getAccessor().invoke(renamed))
          .as(name)
          .isEqualTo(component.getAccessor().invoke(source));
      compared++;
    }
    assertThat(compared).isEqualTo(components.length - 2);
  }

  /** 성분마다 다른 값 - 같은 값을 쓰면 두 자리가 바뀌어도 시험이 통과한다. */
  private static Object distinct(Class<?> type, int i) {
    if (type == BigDecimal.class) {
      return BigDecimal.valueOf(1000 + i);
    }
    if (type == int.class) {
      return 10 + i;
    }
    if (type == String.class) {
      return "s" + i;
    }
    if (type == UUID.class) {
      return new UUID(0, i + 1);
    }
    if (type == LocalDate.class) {
      return LocalDate.of(2026, 1, 1).plusDays(i);
    }
    if (type == Instant.class) {
      return Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i);
    }
    throw new IllegalArgumentException("시험에 값 규칙이 없는 성분 형: " + type);
  }
}
