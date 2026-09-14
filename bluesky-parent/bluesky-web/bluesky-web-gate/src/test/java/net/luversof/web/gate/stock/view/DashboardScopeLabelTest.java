package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 대시보드에 같은 이름의 카드가 둘 있다 - 범위를 이름에 담는다.
 *
 * <p>실측 2026-09-11(ko/en 모두): 대시보드 카드 8 장 중 <b>"실현 손익" 이 둘</b>이었다 &mdash; 전체 기간 225,630,135 와 올해
 * 131,261,409. 화면에는 아래 넉 장 위에만 "올해" 제목이 있어 눈으로는 갈리지만, 낭독기에는 두 카드가 같은 이름으로 들리고 카드를 덮는 링크 이름도 둘 다 "실현
 * 손익" 이다(목적지는 {@code rangeMode=all} 과 {@code ytd} 로 다르다).
 */
class DashboardScopeLabelTest {

  private static final Path SUMMARY = Path.of("src/main/jte/stock/htmx/fragments/summary.jte");
  private static final Path STAT_CARD = Path.of("src/main/jte/_components/ui/statCard.jte");

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 카드는_범위를_sr_only_로_덧붙일_수_있다() throws IOException {
    String card = Files.readString(STAT_CARD, StandardCharsets.UTF_8);

    assertThat(card).contains("@param String scopeNote");
    assertThat(card)
        .as("라벨 뒤에 붙는다")
        .contains("${label}@if(scopeNote != null && !scopeNote.isEmpty())");
    assertThat(card).as("카드를 덮는 링크 이름에도 들어가야 한다").contains("${label}${scopeNote != null");
  }

  @Test
  void 대시보드_두_줄이_각각_범위를_적는다() throws IOException {
    String summary = Files.readString(SUMMARY, StandardCharsets.UTF_8);

    assertThat(count(summary, "scopeNote = allScopeNote")).as("전기간 넉 장").isEqualTo(4);
    assertThat(count(summary, "scopeNote = ytdScopeNote")).as("올해 넉 장").isEqualTo(4);
    assertThat(summary).contains("stock.summary.scope.all");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.summary.scope.all");
    }
  }
}
