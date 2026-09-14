package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.support.StockSortParamException;

/**
 * 주소의 {@code sort} 를 읽는 규칙.
 *
 * <p>실측 2026-09-11(배당 상세 202 행): 지정이 없으면 오래된 것부터(2020-04-08), {@code sort=BOGUS} 면 최근 것부터
 * (2026-09-02) 나왔다. 모르는 열 이름은 비교기를 못 만들어 정렬을 건너뛰는데, 표시 순서를 뒤집는 규칙은 {@code sort == null} 에만 걸려 있어서다
 * &mdash; 지정 없음과도 다른 <b>세 번째 순서</b>가 조용히 나간다. 방향도 같아서 {@code payDate,sideways} 는 오름차순이었다.
 *
 * <p>화면의 열 머리 정렬은 전부 클라이언트 쪽이라({@code data-sort-key}) 이 파라미터를 보내는 UI 는 없다 &mdash; 실측: 배당·매매에서
 * {@code sort=} 가 실린 요청 0 건, 머리 클릭 후에도 주소 변화 없음. 그래서 이 값은 손으로 만든 주소에서만 온다.
 */
class StockSortUtilTest {

  @Test
  void 필드는_쉼표_앞이다() {
    assertThat(StockSortUtil.field("payDate,desc")).isEqualTo("payDate");
    assertThat(StockSortUtil.field(" payDate ")).isEqualTo("payDate");
    assertThat(StockSortUtil.field(null)).isNull();
  }

  @Test
  void 방향은_asc_desc_만_받는다() {
    assertThat(StockSortUtil.descending("sort", "payDate")).isFalse();
    assertThat(StockSortUtil.descending("sort", "payDate,asc")).isFalse();
    assertThat(StockSortUtil.descending("sort", "payDate,DESC")).isTrue();
    assertThatThrownBy(() -> StockSortUtil.descending("sort", "payDate,sideways"))
        .as("조용히 오름차순으로 읽으면 안 된다")
        .isInstanceOf(StockSortParamException.class);
    assertThat(
            catchThrowableOfType(
                    () -> StockSortUtil.descending("order", "payDate,sideways"),
                    StockSortParamException.class)
                .getName())
        .isEqualTo("order");
  }

  /** 모르는 열 이름은 두 목록 모두에서 끊어야 한다 - 한쪽만 고치면 화면끼리 규칙이 갈린다. */
  @Test
  void 모르는_열_이름은_두_목록_모두에서_끊는다() throws IOException {
    for (String rel :
        new String[] {
          "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java",
          "src/main/java/net/luversof/web/gate/stock/controller/StockTradeHtmxController.java"
        }) {
      String source = Files.readString(Path.of(rel), StandardCharsets.UTF_8);
      // 서식에는 묶지 않는다 - spotless 가 default 화살표와 throw 를 두 줄로 나눈다(실측 2026-09-11).
      assertThat(source)
          .as(rel + " 의 비교기 switch 는 모르는 이름에서 끊어야 한다")
          .contains(
              "throw new net.luversof.web.gate.stock.support.StockSortParamException(\"sort\", sort);");
      assertThat(source).as(rel + " 에 조용한 무시가 남아 있으면 안 된다").doesNotContain("default -> null;");
    }
  }

  @Test
  void 끊은_뒤에는_이름_있는_문구가_나간다() throws IOException {
    String resolver =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java"),
            StandardCharsets.UTF_8);
    assertThat(countOccurrences(resolver, "StockSortParamException"))
        .as("4xx 판정 한 번 + 문구 선택 한 번")
        .isEqualTo(2);
    assertThat(resolver).contains("stock.error.badrequest.sort.desc");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.error.badrequest.sort.desc");
    }
  }

  private int countOccurrences(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
