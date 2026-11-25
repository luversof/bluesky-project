package net.luversof.web.dynamiccrud2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.domain.SearchSettings.Pick;

class SearchSettingsTest {

  @Test
  void pickRoundTripsAndOldValuesKeepTheirBehavior() {
    var settings =
        new SearchSettings(
            List.of(
                new SearchSettings.Item("a", SearchOperator.EQUALS, Pick.DISTINCT),
                new SearchSettings.Item("b", SearchOperator.CONTAINS)),
            List.of());
    var read = SearchSettings.of(settings.toMap());
    assertThat(read.fields().get(0).pick()).isEqualTo(Pick.DISTINCT);
    assertThat(read.fields().get(1).pick()).as("예전 저장값은 pick 없음").isNull();
    assertThat(
            SearchSettings.of(
                Map.of(
                    "fields", List.of(Map.of("label", "c", "operator", "EQUALS", "pick", "NOPE")))))
        .extracting(s -> s.fields().get(0).pick())
        .as("모르는 방식은 예전 동작")
        .isNull();
  }

  @Test
  void customValuesAndRequiredRoundTrip() {
    var item = new SearchSettings.Item("a", SearchOperator.EQUALS, Pick.CUSTOM, "1=사용", true);
    var read =
        SearchSettings.of(new SearchSettings(List.of(item), List.of()).toMap()).fields().get(0);
    assertThat(read.required()).isTrue();
    assertThat(read.customOptions()).containsExactly(Map.entry("1", "사용"));
    assertThat(
            new SearchSettings.Item("b", SearchOperator.EQUALS, Pick.LIST, "1=x", false)
                .customOptions())
        .as("CUSTOM 이 아니면 쓰지 않는다")
        .isEmpty();
  }

  @Test
  void resolveFallsBackByValueList() {
    assertThat(Pick.resolve(null, true)).isEqualTo(Pick.LIST);
    assertThat(Pick.resolve(null, false)).isEqualTo(Pick.INPUT);
    assertThat(Pick.resolve(Pick.LIST, false)).as("값 목록이 없으면 입력").isEqualTo(Pick.INPUT);
    assertThat(Pick.resolve(Pick.INPUT, true)).isEqualTo(Pick.INPUT);
    assertThat(Pick.resolve(Pick.DISTINCT, false)).isEqualTo(Pick.DISTINCT);
  }
}
