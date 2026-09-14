package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;

/**
 * 폼을 저장한 뒤 돌아갈 주소를 짓는 규칙과, 그 곁의 순수 도우미들.
 *
 * <p>이 도우미들에는 테스트가 없었다(실측 2026-09-12: 주식 패키지에서 테스트가 한 번도 부르지 않는 public 메서드 76 개). 저장 후 리다이렉트는 값이
 * 하나만 어긋나도 방금 저장한 화면이 아니라 다른 화면으로 돌아가는데, 그건 저장이 실패한 것처럼 읽힌다.
 */
class StockViewSupportTest {

  private StockItem item(String symbol) {
    return new StockItem(UUID.randomUUID(), symbol, symbol + " 이름", "KOSPI", List.of());
  }

  private String appended(String key, Object value) {
    StringBuilder url = new StringBuilder("redirect:/stock/admin?tab=monthly-reference");
    StockViewSupport.appendQueryParam(url, key, value);
    return url.toString();
  }

  @Test
  void 값이_있으면_앰퍼샌드로_이어_붙인다() {
    assertThat(appended("symbol", "005930"))
        .isEqualTo("redirect:/stock/admin?tab=monthly-reference&symbol=005930");
  }

  /** 빈 값은 아예 싣지 않는다 - "symbol=" 만 실리면 뒤 화면이 빈 값을 고른 것으로 읽는다. */
  @Test
  void 널과_빈_값은_싣지_않는다() {
    String base = "redirect:/stock/admin?tab=monthly-reference";
    assertThat(appended("symbol", null)).isEqualTo(base);
    assertThat(appended("symbol", "")).isEqualTo(base);
    assertThat(appended("symbol", "   ")).isEqualTo(base);
  }

  @Test
  void 앞뒤_공백은_떼고_싣는다() {
    assertThat(appended("symbol", "  005930  ")).endsWith("&symbol=005930");
  }

  /** 주소에 그대로 실을 수 없는 글자는 인코딩한다 - 안 하면 뒤 화면이 값을 잘라 읽는다. */
  @Test
  void 특수문자와_한글은_인코딩한다() {
    assertThat(appended("keyword", "삼성 전자"))
        .endsWith("&keyword=%EC%82%BC%EC%84%B1+%EC%A0%84%EC%9E%90");
    assertThat(appended("keyword", "a&b=c")).endsWith("&keyword=a%26b%3Dc");
  }

  /**
   * 이 도우미는 언제나 {@code &} 를 앞에 붙인다 - 그러니 기반 주소에 이미 질의가 시작돼 있어야 한다.
   *
   * <p>{@code ?} 없는 주소에 붙이면 {@code /stock/adminsymbol=...} 같은 경로가 되어 404 가 된다. 실측 2026-09-12: 호출부 세
   * 곳 모두 {@code ?tab=} 으로 시작한다. 그 계약을 여기서 지킨다.
   */
  @Test
  void 호출부의_기반_주소에는_질의_시작이_있다() throws IOException {
    List<String> bases = new ArrayList<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/java/net/luversof/web/gate/stock"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (!source.contains("appendQueryParam")) {
          continue;
        }
        String flat = String.join(" ", source.split("\\s+"));
        int at = flat.indexOf("new StringBuilder(" + (char) 34 + "redirect:");
        while (at >= 0) {
          int from = at + ("new StringBuilder(" + (char) 34).length();
          int to = flat.indexOf((char) 34, from);
          bases.add(flat.substring(from, to));
          at = flat.indexOf("new StringBuilder(" + (char) 34 + "redirect:", to);
        }
      }
    }
    assertThat(bases).as("리다이렉트를 짓는 곳이 있어야 한다").isNotEmpty();
    for (String base : bases) {
      assertThat(base).as(base + " 에는 질의 시작(?)이 있어야 한다").contains("?");
    }
  }

  @Test
  void 널_문자열은_빈_문자열로_읽는다() {
    assertThat(StockViewSupport.safeString(null)).isEmpty();
    assertThat(StockViewSupport.safeString("x")).isEqualTo("x");
  }

  /** 목록은 종목코드 순이고 대소문자를 가리지 않는다 - 가리면 소문자 코드가 뒤로 몰린다. */
  @Test
  void 종목은_코드순이고_대소문자를_가리지_않는다() {
    // 대문자가 먼저 오는 기본 정렬이면 B002 가 a001 앞에 온다 - 표본이 그 차이를 드러내야 한다.
    List<StockItem> sorted =
        StockViewSupport.sortedBySymbol(
            Arrays.asList(item("b005"), null, item("B002"), item("a001")));
    assertThat(sorted.stream().map(StockItem::symbol).toList())
        .containsExactly("a001", "B002", "b005");
    assertThat(StockViewSupport.sortedBySymbol(null)).isEmpty();
    assertThat(StockViewSupport.sortedBySymbol(List.of())).isEmpty();
  }

  /** 음수와 널은 같은 자리에서 끊는다 - 널을 통과시키면 나중에 NPE 로 터진다. */
  @Test
  void 음수와_널은_끊는다() {
    StockViewSupport.requireNonNegative(BigDecimal.ZERO, "영은 괜찮다");
    StockViewSupport.requireNonNegative(new BigDecimal("0.01"), "양수는 괜찮다");
    assertThatThrownBy(() -> StockViewSupport.requireNonNegative(new BigDecimal("-1"), "음수 안 됨"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("음수 안 됨");
    assertThatThrownBy(() -> StockViewSupport.requireNonNegative(null, "널 안 됨"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("널 안 됨");
  }
}
