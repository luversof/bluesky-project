package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 장식 화살표는 낭독기에서 빠진다.
 *
 * <p>화살표가 <b>홀로 있는</b> 자리는 둘뿐이다 &mdash; 값 사이("기초 ₩A → 기말 ₩B")와 링크 끝("자산 현황 →"). 둘 다 뜻은 옆 글자가 말하고
 * 기호는 장식이라, 감싸지 않으면 낭독기가 "오른쪽 화살표" 를 그대로 읽는다(실측 2026-09-15).
 *
 * <p>{@code _components/ui/statCard.jte} 가 이미 같은 자리를 {@code aria-hidden} 으로 정해 뒀고, {@code
 * assetGrowthPeriodReturnSummary.jte} 는 구분자 {@code |} 를 그렇게 처리하면서 화살표만 빠뜨렸다.
 *
 * <p>날짜 범위("2020-02-19 → 2020-03-19")나 가격 변화("13450 → 2690")처럼 <b>화살표가 관계를 담당하는</b> 자리는 대상이 아니다
 * &mdash; 거기서 기호를 빼면 두 값이 그냥 나열된다.
 */
class DecorativeArrowHiddenTest {

  /** 화살표가 홀로 서는 자리. 여기 있는 것은 모두 감싸야 한다. */
  private static final List<String> STANDALONE =
      List.of(
          "src/main/jte/stock/htmx/fragments/allocationBars.jte",
          "src/main/jte/stock/htmx/fragments/recentActivities.jte",
          "src/main/jte/stock/htmx/fragments/upcomingDividends.jte",
          "src/main/jte/stock/htmx/fragments/summary.jte",
          "src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte",
          "src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte");

  private static final String ARROW = String.valueOf((char) 0x2192);

  private static final String ENTITY = "&#8594;";

  /**
   * <b>홀로 선</b> 화살표만 본다.
   *
   * <p>{@code <span ...>&rarr;</span>} 처럼 태그 안이 기호 하나뿐이거나, 링크 글자 끝에 기호만 붙은 자리다. 같은 태그 안에 값이 함께 있는
   * 것("(2020-02-19 &rarr; 2020-03-19)")은 화살표가 관계를 담당하므로 대상이 아니다.
   */
  @Test
  void 홀로_선_화살표는_모두_감싸져_있다() throws IOException {
    List<String> bare = new ArrayList<>();
    int checked = 0;
    for (String file : STANDALONE) {
      List<String> lines = Files.readAllLines(Path.of(file));
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (line.trim().startsWith("<%--")) {
          continue;
        }
        // (1) 태그 안이 화살표 하나뿐인 span
        boolean loneSpan =
            line.contains(">" + ARROW + "</span>") || line.contains(">" + ENTITY + "</span>");
        // (2) 링크 글자 끝에 맨 기호만 붙은 자리
        boolean bareLink =
            line.contains(" " + ARROW + "</a>") || line.contains(" " + ENTITY + "</a>");
        if (!loneSpan && !bareLink) {
          continue;
        }
        checked++;
        if (bareLink || !line.contains("aria-hidden")) {
          bare.add(file + ":" + (i + 1) + "  " + line.trim());
        }
      }
    }
    assertThat(checked).as("홀로 선 화살표를 하나도 못 읽었다 - 검사가 무력하다").isGreaterThanOrEqualTo(6);
    assertThat(bare).as("감싸지 않은 화살표 - 낭독기가 기호를 그대로 읽는다").isEmpty();
  }

  /** 이미 정해진 전례가 살아 있는지 함께 본다. */
  @Test
  void 기존_전례가_유지된다() throws IOException {
    String statCard = Files.readString(Path.of("src/main/jte/_components/ui/statCard.jte"));
    assertThat(statCard)
        .as("카드 이동 화살표는 이 규칙의 출발점이다")
        .contains("stat-card-go" + (char) 34 + " aria-hidden=" + (char) 34 + "true");

    String growth =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte"));
    assertThat(growth)
        .as("같은 파일의 구분자 | 도 같은 규칙을 따른다")
        .contains("aria-hidden=" + (char) 34 + "true" + (char) 34 + ">|</span>");
  }

  /** 화살표가 관계를 담당하는 자리는 건드리지 않았다 - 여기서 빼면 두 값이 그냥 나열된다. */
  @Test
  void 뜻을_담은_화살표는_그대로_둔다() throws IOException {
    long kept =
        Stream.of(
                "src/main/jte/stock/htmx/fragments/adminActions.jte",
                "src/main/jte/stock/htmx/fragments/dividend/dividendSummaryCards.jte")
            .filter(
                file -> {
                  try {
                    String text = Files.readString(Path.of(file));
                    return text.contains("&rarr;") || text.contains(ARROW);
                  } catch (IOException e) {
                    return false;
                  }
                })
            .count();
    assertThat(kept).as("값 사이 관계를 나타내는 화살표는 남아 있어야 한다").isEqualTo(2);
  }
}
