package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 값이 0 인 칸은 눈에는 "-" 를 보이고 보조기술에는 <b>까닭</b>을 읽힌다. 그러려면 "-" 를 가려야 한다.
 *
 * <p>가리지 않으면 둘 다 읽혀 <b>"-0원"</b> 이 된다 - 0 이 아니라 <b>음수 0</b> 처럼 들린다. 실측 2026-09-13: 자산 성장 21 칸 · 매매
 * 20 칸 · 자산 현황 6 칸, 모두 {@code aria-hidden} 이 없었다(합계 47 칸). 자산 현황 쪽은 "-아직 판 적이 없습니다" 로 읽혔다.
 *
 * <p>같은 저장소의 다른 기호(정렬 방향 · 색 견본)는 이미 {@code aria-hidden="true"} 를 쓴다. 여기 "-" 는 값이 아니라 <b>자리표시
 * 기호</b>이고 sr-only 글이 그것을 <b>대신</b>하므로 가리는 것이 맞다 - 값을 보충만 하는 {@code srExact} 와는 경우가 다르다.
 */
class ZeroDashAriaHiddenTest {

  private static final List<String> TEMPLATES =
      List.of(
          "src/main/jte/stock/htmx/fragments/assetStatus.jte",
          "src/main/jte/stock/htmx/fragments/stockContributionTable.jte",
          "src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte",
          "src/main/jte/stock/htmx/fragments/yearlyCostSummary.jte");

  /** 자리표시 "-" 와 그 대체 글이 붙어 있는 자리. */
  private static final String PAIR = ">-</span><span class=\"sr-only\">";

  private List<String> lines(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).lines().toList();
  }

  /** 그런 자리가 하나도 없으면 이 가드가 아무것도 지키지 않는 셈이다. */
  @Test
  void 지킬_자리가_실제로_있다() throws IOException {
    int found = 0;
    for (String path : TEMPLATES) {
      for (String line : lines(path)) {
        if (line.contains(PAIR)) {
          found++;
        }
      }
    }
    assertThat(found).as("자리표시 대시 칸 수").isGreaterThanOrEqualTo(15);
  }

  /** 모든 자리에서 "-" 가 가려져 있다. */
  @Test
  void 모든_자리표시_대시가_가려져_있다() throws IOException {
    for (String path : TEMPLATES) {
      for (String line : lines(path)) {
        if (!line.contains(PAIR)) {
          continue;
        }
        assertThat(line).as(path + " 의 자리표시 대시: " + line.strip()).contains("aria-hidden=\"true\"");
      }
    }
  }

  /** 까닭은 그대로 남는다 - 가리기만 하고 sr-only 를 지우면 아무것도 안 읽힌다. */
  @Test
  void 까닭은_그대로_남는다() throws IOException {
    for (String path : TEMPLATES) {
      for (String line : lines(path)) {
        if (!line.contains(PAIR)) {
          continue;
        }
        assertThat(line).as(path).contains("<span class=\"sr-only\">");
        assertThat(line).as(path + " title 도 함께 둔다(마우스)").contains("title=");
      }
    }
  }

  /** 값을 보충하는 srExact 는 반대 규칙이다 - 화면 값을 가리면 값이 사라진다. */
  @Test
  void 값_보충_컴포넌트는_가리지_않는다() throws IOException {
    String srExact =
        Files.readString(
            Path.of("src/main/jte/_components/ui/srExact.jte"), StandardCharsets.UTF_8);

    // 속성만 본다 - 이 파일의 주석에 "aria-hidden 하지 않는다" 라는 문장이 있어 글자로 찾으면 걸린다.
    assertThat(srExact).doesNotContain("aria-hidden=");
  }
}
