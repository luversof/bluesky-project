package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터의 머리칸 정렬은 <b>전체 이동 링크</b>다. 그래서 링크가 화면의 필터를 함께 싣지 않으면 정렬 한 번에 필터가 조용히 풀린다.
 *
 * <p>실측 2026-09-13: {@code keyword=TIGER} 로 8 행 중 3 행만 남은 표에서 머리칸을 누르니 주소가 {@code
 * /stock/simulator?tab=monthly-dividend&sort=symbol&direction=asc} 가 되면서 8 행으로 돌아왔다. 검색어 칸도 비었다. 링크
 * 여섯 개 전부에 {@code keyword} · {@code minAnnualYield} · {@code positiveOnly} 가 하나도 없었다.
 *
 * <p>같은 화면의 관리 탭({@code monthlyDividendReference.jte})은 이미 선택 종목과 지급일을 실어 보내고 있었다 - 규칙이 아니라 한 곳이 빠진
 * 것이었다.
 *
 * <p>고정하는 것은 둘이다. (1) 기준 주소가 세 필터를 모두 싣는다. (2) 모든 정렬 링크가 그 기준 주소를 쓴다 - 하나라도 직접 주소를 적으면 그 열만 필터를
 * 버린다.
 */
class SimulatorSortKeepsFilterTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  /** 정렬 링크가 쓰는 기준 주소 - 이 문자열을 만드는 줄들. */
  private String baseUrlBlock(String source) {
    int at = source.indexOf("String monthlyDividendTableBaseUrl =");
    assertThat(at).as("기준 주소를 만드는 줄이 있다").isGreaterThanOrEqualTo(0);
    int end = source.indexOf(";", at);
    return source.substring(at, end);
  }

  @Test
  void 기준_주소가_검색어를_싣는다() throws IOException {
    String block = baseUrlBlock(read(FRAGMENT));
    assertThat(block).as("검색어").contains("&keyword=");
    assertThat(block).as("검색어는 URL 인코딩해서 싣는다").contains("URLEncoder.encode(monthlyDividendKeyword");
  }

  @Test
  void 기준_주소가_최소_수익률과_양수만을_싣는다() throws IOException {
    String block = baseUrlBlock(read(FRAGMENT));
    assertThat(block).as("최소 연배당 수익률").contains("&minAnnualYield=");
    assertThat(block).as("양수만 보기").contains("&positiveOnly=true");
  }

  /** 빈 값까지 실어 보내면 "필터 없음" 이 주소에 박혀 되돌리기 어려워진다. */
  @Test
  void 필터가_없으면_붙이지_않는다() throws IOException {
    String block = baseUrlBlock(read(FRAGMENT));
    assertThat(block).as("빈 검색어는 뺀다").contains("monthlyDividendKeyword.isBlank()");
    assertThat(block).as("값 없는 수익률은 뺀다").contains("monthlyDividendMinAnnualYield == null");
  }

  /** 링크 하나가 기준 주소를 안 쓰면 그 열만 필터를 버린다 - 실제로 여섯 칸에 열 개가 넘는 링크가 있어 눈으로는 못 센다. */
  @Test
  void 모든_정렬_링크가_기준_주소를_쓴다() throws IOException {
    String source = read(FRAGMENT);
    int direct = 0;
    int viaBase = 0;
    int at = source.indexOf("&sort=");
    while (at >= 0) {
      int lineStart = source.lastIndexOf((char) 10, at) + 1;
      String line = source.substring(lineStart, at);
      if (line.contains("${monthlyDividendTableBaseUrl}")) {
        viaBase++;
      } else if (line.contains("data-filter-reset")) {
        // 필터 지우기 링크는 필터를 일부러 버린다(2026-09-21 표 좁히기 폼과 함께 들어왔다). 세지 않는다.
        // (여기서 continue 하면 아래 다음 자리 찾기를 건너뛰어 멈추지 않는다.)
        direct += 0;
      } else if (line.contains("href=")) {
        direct++;
      }
      at = source.indexOf("&sort=", at + 1);
    }
    assertThat(viaBase).as("기준 주소를 쓰는 정렬 링크").isGreaterThanOrEqualTo(8);
    assertThat(direct).as("기준 주소를 건너뛴 정렬 링크").isZero();
  }
}
