package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 자산현황의 원금이 '직접 입력한 값' 일 때 그 사실이 드러나는지 본다.
 *
 * <p>계좌 설정 {@code manualPrincipalAmount} 는 수익률의 기준을 계산 원가 대신 실제 납입액으로 바꾼다(실측: 연금저축1 이 -9.85% ->
 * +13.80%). 그런데 화면은 두 경우를 같은 모양으로 그려서, 그 값이 사라져도 <b>숫자만 조용히 달라진다</b>.
 *
 * <p>이 설정에는 갱신 UI 도 API 도 없어 한 번 사라지면 사람이 다시 넣어야 한다(실측 사고 2026-08-22: 계좌 삭제로 3 계좌의 값이 사라졌고 원장은
 * 복구했지만 이 값은 복구하지 못했다). 그래서 최소한 "지금 이 숫자가 직접 입력한 값" 이라는 표시는 있어야 한다.
 */
class ManualPrincipalDisclosureTest {

  private static final Path FRAGMENT = Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");
  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java");
  private static final String KEY = "stock.asset.status.label.manual.principal";

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 컨트롤러가_수동_원금_계좌를_화면에_넘긴다() throws IOException {
    String source = read(CONTROLLER);
    assertThat(source)
        .as("어느 계좌가 수동 원금인지 넘기지 않으면 화면이 구분할 수 없다")
        .contains("manualPrincipalAccountIds")
        .contains("model.addAttribute(\"manualPrincipalAccountIds\"");
  }

  @Test
  void 화면이_그_집합으로_표시를_가른다() throws IOException {
    String fragment = read(FRAGMENT);
    assertThat(fragment)
        .as("파라미터를 받지 않으면 컨트롤러가 넘겨도 쓰이지 않는다")
        .contains("@param java.util.Set<java.util.UUID> manualPrincipalAccountIds");
    assertThat(fragment)
        .as("집합으로 갈라 표시하지 않으면 계산 원가와 구분되지 않는다")
        .contains("manualPrincipalAccountIds.contains(");
    assertThat(fragment).contains(KEY);
  }

  @Test
  void 안내_문구_키가_두_번들에_있다() throws IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      assertThat(read(Path.of("src/main/resources", bundle)))
          .as(bundle + " 에 " + KEY + " 이 없다")
          .contains(KEY);
    }
  }

  /** 한국어 번들은 ASCII 로만 쓴다(편집기 코드페이지에서 깨지는 것을 피한다). */
  @Test
  void 새_한국어_문구는_이스케이프되어_있다() throws IOException {
    for (String line : read(Path.of("src/main/resources/uiMessage_ko.properties")).split("\\R")) {
      if (!line.startsWith(KEY)) {
        continue;
      }
      assertThat(line.chars().allMatch(c -> c < 128)).as("ASCII 가 아닌 문자: " + line).isTrue();
    }
  }
}
