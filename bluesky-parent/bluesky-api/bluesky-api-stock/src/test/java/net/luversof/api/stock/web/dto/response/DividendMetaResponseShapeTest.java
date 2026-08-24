package net.luversof.api.stock.web.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * 배당 메타 응답에 '합계' 성격의 값을 두지 않는다.
 *
 * <p>이 응답은 필터와 무관한 사용자 전체 메타다(userId 만 받는다). 예전에 {@code totalNetAmount} 가 들어 있었고, 요약 화면이 그것을 '누적 확정
 * 수익'으로 썼다가 계좌·기간 필터를 걸어도 값이 전체로 남는 사고가 났다(실측: 10,113,820 자리에 61,646,257).
 *
 * <p>필드가 있으면 언젠가 누가 쓴다. 그래서 값을 지우는 데서 그치지 않고 모양을 고정한다. 필터가 반영된 합계는 {@code GET /api/dividend/total}
 * 이 낸다.
 */
class DividendMetaResponseShapeTest {

  /** 합계로 읽힐 수 있는 이름들. */
  private static final List<String> AGGREGATE_HINTS =
      List.of("total", "sum", "amount", "profit", "count");

  @Test
  void 합계로_읽힐_이름의_필드를_두지_않는다() {
    RecordComponent[] components = DividendMetaResponse.class.getRecordComponents();
    // 리플렉션이 조용히 빈 배열을 주면 검사가 무력해진다.
    assertThat(components).isNotEmpty();

    List<String> aggregateLike =
        Arrays.stream(components)
            .map(RecordComponent::getName)
            .filter(
                name ->
                    AGGREGATE_HINTS.stream()
                        .anyMatch(hint -> name.toLowerCase(Locale.ROOT).contains(hint)))
            .toList();

    assertThat(aggregateLike)
        .as(
            "이 응답은 필터를 받지 않으므로 합계를 담으면 화면이 필터를 건 값과 어긋난다."
                + " 필터가 반영된 합계는 /api/dividend/total 을 쓸 것")
        .isEmpty();
  }

  @Test
  void 필터_UI_구성에_필요한_두_값만_남긴다() {
    assertThat(
            Arrays.stream(DividendMetaResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList())
        .containsExactly("firstBasisDate", "stockItemIds");
  }
}
