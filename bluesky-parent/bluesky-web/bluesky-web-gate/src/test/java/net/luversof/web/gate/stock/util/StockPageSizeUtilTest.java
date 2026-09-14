package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.support.StockPageSizeParamException;

/**
 * 주소의 {@code size}(한 쪽에 담을 줄 수)를 읽는 규칙.
 *
 * <p>실측 2026-09-12(매매 이력 258 행 · /stock/htmx/trade-history): 형제 값들은 이미 이름을 들고 끊는데 ({@code
 * sort=BOGUS} · {@code page=abc} · {@code size=abc} 모두 400) {@code size} 의 범위만 규칙 밖이었다 &mdash;
 * {@code size=0} 과 {@code size=-5} 는 기본값을 준 것과 <b>바이트까지 같은</b> 25,053 바이트였고(조용히 20 으로 바뀜), {@code
 * size=100000} 은 상한 없이 258 행 239,866 바이트를 한 번에 그렸다.
 *
 * <p>화면은 이 값을 보내지 않는다 &mdash; 실측: 매매 · 자산성장 어느 요청에도 {@code size=} 가 실리지 않는다. 그러니 이 값은 손으로 만든 주소에서만
 * 오고, 조용히 다른 수로 바꾸면 사용자는 자기가 적은 줄 수가 먹혔다고 읽는다.
 */
class StockPageSizeUtilTest {

  @Test
  void 쓸_수_있는_줄_수는_그대로_돌려준다() {
    assertThat(StockPageSizeUtil.resolve("size", 1)).isEqualTo(1);
    assertThat(StockPageSizeUtil.resolve("size", 20)).isEqualTo(20);
    assertThat(StockPageSizeUtil.resolve("size", StockPageSizeUtil.MAX_PAGE_SIZE))
        .isEqualTo(StockPageSizeUtil.MAX_PAGE_SIZE);
    assertThatCode(() -> StockPageSizeUtil.resolve("size", 199)).doesNotThrowAnyException();
  }

  @Test
  void 조용히_기본값으로_바꾸지_않는다() {
    for (int bad : new int[] {0, -1, -5, Integer.MIN_VALUE}) {
      assertThatThrownBy(() -> StockPageSizeUtil.resolve("size", bad))
          .as(bad + " 는 조용히 20 이 되면 안 된다")
          .isInstanceOf(StockPageSizeParamException.class);
    }
  }

  @Test
  void 위끝을_넘으면_끊는다() {
    for (int bad :
        new int[] {StockPageSizeUtil.MAX_PAGE_SIZE + 1, 1000, 100000, Integer.MAX_VALUE}) {
      assertThatThrownBy(() -> StockPageSizeUtil.resolve("size", bad))
          .as(bad + " 는 전체 목록을 한 번에 그리면 안 된다")
          .isInstanceOf(StockPageSizeParamException.class);
    }
  }

  @Test
  void 어느_파라미터였는지_들고_다닌다() {
    assertThat(
            catchThrowableOfType(
                    () -> StockPageSizeUtil.resolve("rows", 0), StockPageSizeParamException.class)
                .getName())
        .isEqualTo("rows");
  }

  /** 목록 조각이 실제로 이 규칙을 거쳐야 한다 - 안 거치면 조용한 폴백이 그대로 돌아온다. */
  @Test
  void 매매_이력_조각은_이_규칙을_거친다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);
    assertThat(flatten(source))
        .as("size 는 StockPageSizeUtil.resolve 를 거쳐야 한다")
        .contains(
            "size = net.luversof.web.gate.stock.util.StockPageSizeUtil.resolve(\"size\", size);");
    assertThat(source).as("조용한 폴백이 남아 있으면 안 된다").doesNotContain("if (size <= 0) size = 20;");
  }

  /** 끊은 뒤에는 화면이 조용히 비면 안 된다 - 공통 처리기가 4xx 로 알아보고 이름 있는 문구를 골라야 한다. */
  @Test
  void 끊은_뒤에는_이름_있는_문구가_나간다() throws IOException {
    String resolver =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java"),
            StandardCharsets.UTF_8);
    assertThat(countOccurrences(resolver, "StockPageSizeParamException"))
        .as("4xx 판정 한 번 + 문구 선택 한 번")
        .isEqualTo(2);
    assertThat(resolver).contains("stock.error.badrequest.pagesize.desc");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.error.badrequest.pagesize.desc");
    }
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
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
