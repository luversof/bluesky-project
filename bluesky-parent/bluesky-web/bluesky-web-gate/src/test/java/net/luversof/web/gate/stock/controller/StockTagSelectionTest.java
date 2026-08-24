package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;

/**
 * 태그 필터가 "아무것도 안 걸림" 을 "필터 없음" 으로 흘리지 않는지 고정한다.
 *
 * <p>이 구분이 무너지면 화면이 <b>정반대</b>를 보여준다. 종목 id 목록이 비어 있으면 요청에 파라미터가 아예 실리지 않고, api-stock 은 그것을 "필터 없음"
 * 으로 읽어 전체를 돌려준다 &mdash; 실측으로 없는 태그를 골랐는데 전체 106 행이 그대로 나온 적이 있다.
 *
 * <p>그래서 세 값이 서로 달라야 한다.
 *
 * <ul>
 *   <li>{@code null} &mdash; 필터를 걸지 않았다(전체를 보여준다)
 *   <li>빈 목록 &mdash; 필터를 걸었는데 대상이 하나도 없다(아무것도 보여주지 않는다)
 *   <li>값이 있는 목록 &mdash; 그 대상만 보여준다
 * </ul>
 *
 * <p>이 규칙에 매매·배당·활동·자산성장 네 화면이 걸려 있다. 매매/자산성장은 빈 목록을 보고 원격 호출을 건너뛰고, 배당은 빈 목록을 "아무것도 일치하지 않음" 으로 걸러
 * 조기 반환한다. 어느 쪽이든 {@code null} 로 새는 순간 전체가 나온다.
 */
class StockTagSelectionTest {

  /**
   * {@code protected} 메서드를 부르기 위한 최소 하위 클래스(같은 패키지).
   *
   * <p>이 두 메서드는 원격 클라이언트를 쓰지 않는 순수 계산이라 의존성은 전부 null 로 둔다.
   */
  private static final class Probe extends StockBaseHtmxController {
    Probe() {
      super(null, null, null, null, null, null);
    }
  }

  private final Probe probe = new Probe();

  private static final UUID TAGGED = UUID.randomUUID();
  private static final UUID UNTAGGED = UUID.randomUUID();

  private List<StockItem> stockItems() {
    return List.of(
        new StockItem(TAGGED, "000001", "태그있는종목", "KOSPI", List.of("월배당")),
        new StockItem(UNTAGGED, "000002", "태그없는종목", "KOSPI", List.of()));
  }

  @Test
  void 필터를_걸지_않으면_종목_목록은_null_이다() {
    var selection = probe.resolveStockTagSelection(stockItems(), null, null);

    assertThat(selection.hasFilter()).isFalse();
    assertThat(selection.requestedStockItemIds()).as("필터가 없으면 null 이어야 '전체'로 읽힌다").isNull();
    assertThat(probe.retainAvailableStockItemIds(selection, Set.of(TAGGED))).isNull();
  }

  @Test
  void 태그를_골랐는데_해당_종목이_없으면_빈_목록이다() {
    var selection = probe.resolveStockTagSelection(stockItems(), null, List.of("없는태그"));

    assertThat(selection.hasFilter()).as("태그를 골랐으므로 필터가 걸린 것이다").isTrue();
    assertThat(selection.requestedStockItemIds())
        .as("null 이면 '필터 없음'이 되어 전체가 나온다")
        .isNotNull()
        .isEmpty();
  }

  /**
   * 여기가 실제로 새기 쉬운 지점이다.
   *
   * <p>{@code retainAvailableIds} 는 <b>요청 목록이 비어 있으면 null</b> 을 돌려준다(원래는 "요청이 없다" 는 뜻이다). 태그를 골랐는데
   * 해당 종목이 하나도 없으면 요청 목록이 정확히 그 빈 목록이므로, 보정하지 않으면 null 이 그대로 나가 "필터 없음 = 전체" 가 된다.
   */
  @Test
  void 태그가_아무_종목도_고르지_못했을_때_null_이_새지_않는다() {
    var selection = probe.resolveStockTagSelection(stockItems(), null, List.of("없는태그"));
    assertThat(selection.requestedStockItemIds()).isEmpty();

    var retained = probe.retainAvailableStockItemIds(selection, Set.of(TAGGED, UNTAGGED));

    assertThat(retained).as("null 이면 파라미터가 빠져 전체가 나온다").isNotNull().isEmpty();
  }

  /** 고른 종목이 현재 화면 선택지에 없어도 빈 목록이다(다른 경로 - 여기서는 null 이 나오지 않는다). */
  @Test
  void 고른_종목이_선택지에_없으면_빈_목록이다() {
    var selection = probe.resolveStockTagSelection(stockItems(), null, List.of("월배당"));
    assertThat(selection.requestedStockItemIds()).containsExactly(TAGGED);

    var retained = probe.retainAvailableStockItemIds(selection, Set.of(UNTAGGED));

    assertThat(retained).isNotNull().isEmpty();
  }

  @Test
  void 태그로_고른_종목과_직접_고른_종목이_합쳐진다() {
    var selection = probe.resolveStockTagSelection(stockItems(), List.of(UNTAGGED), List.of("월배당"));

    assertThat(selection.hasFilter()).isTrue();
    assertThat(selection.requestedStockItemIds()).containsExactlyInAnyOrder(TAGGED, UNTAGGED);
    assertThat(probe.retainAvailableStockItemIds(selection, Set.of(TAGGED, UNTAGGED)))
        .containsExactlyInAnyOrder(TAGGED, UNTAGGED);
  }

  /** 빈 문자열·공백 태그는 필터로 치지 않는다. 치면 아무것도 안 고른 화면이 빈 결과가 된다. */
  @Test
  void 공백_태그는_필터가_아니다() {
    var selection = probe.resolveStockTagSelection(stockItems(), null, List.of("", "   "));

    assertThat(selection.hasFilter()).isFalse();
    assertThat(selection.requestedStockItemIds()).isNull();
  }
}
