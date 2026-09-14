package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 지속가능성 시뮬레이터는 <b>펼친 해</b>의 월별 표만 그린다.
 *
 * <p>실측 2026-09-11(기본 진입): 표 <b>43 개 중 42 개가 접힌 채로</b> 그려져 있었고 칸 7,110 개 중 <b>6,552 개(92%)</b>가 그
 * 안이었다 &mdash; DOM 8,483 노드 &middot; 454KB 로 다른 화면(1,100~2,800 노드)의 3~8 배였다. 펼칠 때 표 전체를 다시
 * 그리므로({@code toggleYearlyDetails -> render}) 그때 만들면 된다.
 *
 * <p>고친 뒤 실측: 1,091 노드 &middot; 96KB(-87% / -79%), 한 해를 펼치면 1,267 노드로 월별 표 1 개만 생기고 접으면 다시 사라진다.
 * 펼치기 동기 시간 16~18ms.
 */
class SimulatorMonthlyLazyRenderTest {

  private static final Path SOURCE = Path.of("src/main/frontend/src/stock/stockSimulator.ts");
  private static final Path BUILT = Path.of("src/main/resources/static/js/stock/stockSimulator.js");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 접힌_해는_월별_표를_만들지_않는다() throws IOException {
    String source = read(SOURCE);

    String empty = String.valueOf((char) 34) + String.valueOf((char) 34);
    assertThat(source)
        .as("접혀 있어도 그리면 안 보는 칸이 DOM 의 92% 를 차지한다")
        .contains("${expanded ? renderMonthlyDetailsTable(record) : " + empty + "}");
  }

  @Test
  void 빌드_산출물에도_들어_있다() throws IOException {
    assertThat(read(BUILT).replace(" ", ""))
        .contains("expanded?renderMonthlyDetailsTable(record):");
  }
}
