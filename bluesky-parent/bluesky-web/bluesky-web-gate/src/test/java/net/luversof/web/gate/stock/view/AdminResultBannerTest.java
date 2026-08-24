package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 관리 화면의 갱신 결과 배너가 응답과 계속 맞물려 있는지 본다.
 *
 * <p>갱신 버튼은 {@code hx-swap="none"} + 전체 새로고침이라, 조각의 인라인 스크립트가 응답을 sessionStorage 에 담았다가 새로고침된 화면에서
 * 그린다. 그래서 <b>JS 가 읽는 필드 이름</b>이 응답 DTO 와 어긋나면 배너가 조용히 아무것도 그리지 않는다 &mdash; 그러면 몇 행이 빠졌는지 다시 알 수 없게
 * 되고, 이 기능이 있었다는 사실만 남는다.
 */
class AdminResultBannerTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte");
  private static final Path LEDGER_DTO =
      Path.of("src/main/java/net/luversof/web/gate/stock/dto/response/LedgerImportResult.java");
  private static final Path PRICE_DTO =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/dto/response/PriceHistoryUpdateResult.java");

  /** 스크립트가 응답에서 읽는 필드 이름({@code body.xxx}). */
  private static final Pattern BODY_FIELD = Pattern.compile("body\\.(\\w+)");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private Set<String> referencedFields() throws IOException {
    Set<String> names = new LinkedHashSet<>();
    Matcher matcher = BODY_FIELD.matcher(read(FRAGMENT));
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
    return names;
  }

  @Test
  void 스크립트가_읽는_필드가_응답_DTO_에_모두_있다() throws IOException {
    Set<String> referenced = referencedFields();
    assertThat(referenced).as("응답 필드를 하나도 읽지 않는다면 배너가 죽은 것이다").isNotEmpty();

    String dtoSource = read(LEDGER_DTO) + read(PRICE_DTO);
    List<String> missing = new ArrayList<>();
    for (String field : referenced) {
      if (!dtoSource.contains(field)) {
        missing.add(field);
      }
    }
    assertThat(missing).as("스크립트가 읽는 필드가 응답 DTO 에 없다. 이름이 바뀌면 배너가 조용히 아무것도 안 그린다").isEmpty();
  }

  @Test
  void 세_가지_안내_문구를_모두_넘긴다() throws IOException {
    String fragment = read(FRAGMENT);
    assertThat(fragment)
        .as("문구를 data-* 로 넘겨야 로케일이 반영된다(JS 안에 한국어를 박으면 영어 화면에서도 한국어가 나온다)")
        .contains("data-import-ok-template")
        .contains("data-import-dropped-template")
        .contains("data-price-failed-template");
  }

  @Test
  void 안내_문구_키가_두_번들에_모두_있다() throws IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      String source = read(Path.of("src/main/resources", bundle));
      for (String key :
          List.of(
              "stock.admin.import.result.ok",
              "stock.admin.import.result.dropped",
              "stock.admin.import.result.price.failed")) {
        assertThat(source).as(bundle + " 에 " + key + " 이 없다").contains(key);
      }
    }
  }

  /** 한국어 번들은 ASCII 로만 쓴다(편집기 코드페이지에서 깨지는 것을 피한다). */
  @Test
  void 새로_넣은_한국어_문구는_이스케이프되어_있다() throws IOException {
    String source = read(Path.of("src/main/resources/uiMessage_ko.properties"));
    for (String line : source.split("\n")) {
      if (!line.startsWith("stock.admin.import.result")) {
        continue;
      }
      assertThat(line.chars().allMatch(c -> c < 128)).as("ASCII 가 아닌 문자가 있다: " + line).isTrue();
    }
  }
}
