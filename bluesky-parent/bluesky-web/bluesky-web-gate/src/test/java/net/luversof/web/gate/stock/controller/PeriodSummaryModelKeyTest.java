package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.ui.ExtendedModelMap;

import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 기간 요약의 <b>모든</b> 값이 화면 모델까지 실리는지 본다.
 *
 * <p>요약에 필드를 늘리고 컨트롤러에서 모델에 넣는 것을 잊으면, 템플릿은 그 자리에 조용히 "계산 불가" 를 그린다. 그 화면은 <b>api-stock 이 값을 못 낸
 * 경우와 똑같이</b> 보이기 때문에, 원인을 백엔드에서 찾게 된다.
 *
 * <p>그래서 값 하나하나를 세는 대신 <b>record 성분 이름 전부</b>가 모델 키에 있는지 리플렉션으로 훑는다. 필드가 늘어나면 검사도 저절로 늘어난다.
 */
class PeriodSummaryModelKeyTest {

  /**
   * 요약 성분 이름과 모델 키가 다른 자리.
   *
   * <p>{@code growthRatePct} 는 화면에서 '자산 증가율' 이라 모델 키가 {@code periodReturnRatePct} 다. 이름을 맞추는 것이 더
   * 낫지만, 지금 바꾸면 템플릿·테스트가 함께 흔들리므로 예외로 적어 둔다.
   */
  private static final Map<String, String> RENAMED = Map.of("growthRatePct", "periodReturnRatePct");

  private StockAssetGrowthHtmxController controller() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    return new StockAssetGrowthHtmxController(null, null, null, null, null, null, null, messages);
  }

  @Test
  void 요약의_모든_값이_모델에_실린다() {
    // 스프링 MVC 가 컨트롤러에 넘기는 Model 은 BindingAwareModelMap(= ExtendedModelMap) 이라 null 값을
    // 그대로 담는다. ConcurrentModel 로 검사하면 null 을 넣는 순간 키가 지워져 검사가 헛돈다.
    var model = new ExtendedModelMap();
    // 값이 아니라 '키가 있는지' 를 보므로 빈 요약으로 충분하다. 값 검증은 렌더 테스트가 한다.
    controller()
        .addPeriodSummaryAttributes(
            model,
            new TradeProfitTimeSeriesSummary(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null));

    List<String> missing = new ArrayList<>();
    for (RecordComponent component : TradeProfitTimeSeriesSummary.class.getRecordComponents()) {
      String key = RENAMED.getOrDefault(component.getName(), component.getName());
      if (!model.containsAttribute(key)) {
        missing.add(component.getName());
      }
    }

    assertThat(missing).as("모델에 넣지 않은 요약 값은 화면에서 '계산 불가' 로 보인다 - 백엔드가 못 낸 것과 구분되지 않는다").isEmpty();
  }

  /** 요약이 아예 없으면(계산 불가) 키는 있고 값이 null 이어야 한다. 키가 없으면 템플릿 기본값이 살아난다. */
  @Test
  void 요약이_없어도_키는_남는다() {
    var model = new ExtendedModelMap();

    controller().addPeriodSummaryAttributes(model, null);

    assertThat(model.containsAttribute("periodProfitRatePct")).isTrue();
    assertThat(model.getAttribute("periodProfitRatePct")).isNull();
  }
}
