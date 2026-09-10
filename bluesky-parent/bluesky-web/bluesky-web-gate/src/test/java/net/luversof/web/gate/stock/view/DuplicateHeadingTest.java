package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 한 화면에 같은 이름의 제목이 두 번 나오지 않는다.
 *
 * <p>실측 2026-09-10(qa/heading-dupes-states.cjs): "선택 합산" 이라는 제목이 자산 현황·매매에 2개씩, 배당은 3개월을 고르면 3개까지
 * 나왔다(변동 요인·종목별 랭킹·계좌별 랭킹). 화면 낭독기의 제목 목록에서는 이름이 전부라, 어느 표의 합산인지 구분할 수 없다.
 *
 * <p>이름은 이미 키가 넷으로 나뉘어 있었는데 값이 모두 같았다 - 값만 구분하면 마크업은 그대로여도 된다.
 */
class DuplicateHeadingTest {

  private static final Path EN = Path.of("src/main/resources/uiMessage.properties");
  private static final Path KO = Path.of("src/main/resources/uiMessage_ko.properties");

  /** 한 화면에 함께 나올 수 있는 선택 합산 제목들. 화면이 다르면 같은 이름을 써도 헷갈리지 않는다(자산 현황과 매매는 각각 계좌·종목 한 쌍씩만 쓴다). */
  private static final Map<String, List<String>> TOGETHER =
      Map.of(
          "배당 내역",
          List.of(
              "stock.dividend.change.selection.summary",
              "stock.dividend.yield.selection.stock.summary",
              "stock.dividend.yield.selection.account.summary"),
          "매매 내역 · 자산 현황",
          List.of(
              "stock.analytics.selection.summary", "stock.analytics.account.selection.summary"));

  private static Map<String, String> values(Path path, List<String> keys) throws IOException {
    Map<String, String> found = new LinkedHashMap<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      int equals = line.indexOf((char) 61);
      if (equals < 0) continue;
      String key = line.substring(0, equals).trim();
      if (keys.contains(key)) found.put(key, line.substring(equals + 1).trim());
    }
    return found;
  }

  private static void assertDistinct(Path path) throws IOException {
    List<String> repeated = new ArrayList<>();
    for (Map.Entry<String, List<String>> screen : TOGETHER.entrySet()) {
      List<String> keys = screen.getValue();
      Map<String, String> found = values(path, keys);
      assertThat(found.keySet()).as(path + " 에 없는 키가 있다: " + screen.getKey()).containsAll(keys);
      for (int i = 0; i < keys.size(); i++) {
        for (int j = i + 1; j < keys.size(); j++) {
          if (found.get(keys.get(i)).equals(found.get(keys.get(j)))) {
            repeated.add(screen.getKey() + ": " + keys.get(i) + " 와 " + keys.get(j));
          }
        }
      }
    }
    assertThat(repeated).as(path + " 에서 한 화면의 선택 합산 제목이 서로 같다 - 어느 표의 합산인지 알 수 없다").isEmpty();
  }

  @Test
  void 선택_합산_제목은_영어에서_서로_다르다() throws IOException {
    assertDistinct(EN);
  }

  @Test
  void 선택_합산_제목은_한국어에서_서로_다르다() throws IOException {
    assertDistinct(KO);
  }

  @Test
  void 매매의_계좌_블록은_계좌용_이름을_쓴다() throws IOException {
    String html =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte"),
            StandardCharsets.UTF_8);
    assertThat(html)
        .as("계좌 블록과 종목 블록이 같은 이름 변수를 쓰면 한 화면에 같은 제목이 두 번 나온다")
        .contains("accountSelectionSummaryLabel");
  }
}
