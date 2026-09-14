package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 아직 오지 않은 날은 시뮬레이션하지 않는다.
 *
 * <p>일별 시뮬레이션은 요청의 끝날까지 굴리면서 마지막으로 아는 종가로 평가한다. 그래서 끝날이 미래면 <b>모르는 날의 기록</b>이 만들어졌다.
 *
 * <p>실측 2026-09-13: {@code startDate=2030-01-01 &amp; endDate=2030-12-31} 을 물으면 시계열 점 54 개가 돌아오고,
 * 화면의 '월별 성과' 가 2030-01~12 를 손익 ₩0 · 수익률 0.00% 로 그렸으며 '연도별 성과' 는 2030 년 <b>기말 평가액</b>을
 * 1,622,109,770 원이라고 적었다. 손으로 만든 극단값만의 이야기가 아니다 - 달력에서 2026 년 한 해를 고르면 ({@code
 * 2026-01-01~2026-12-31}) 10 · 11 · 12 월이 0 원 줄로 붙었고, 9 월은 13 일까지만 자료가 있는데도 '일부 구간' 표시가 사라져 온전한 달처럼
 * 보였다.
 *
 * <p>자료가 아예 없는 <b>과거</b> 구간(2000 년)은 이미 빈 표로 나온다({@code isEmptyBucket} 이 거른다). 미래도 같아야 한다.
 */
class FutureDaysNotSimulatedTest {

  private static final String SERVICE =
      "src/main/java/net/luversof/api/stock/service/TradeProfitService.java";

  private String read() throws IOException {
    return Files.readString(Path.of(SERVICE), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 출력 끝날을 오늘로 자른다. */
  @Test
  void 출력_끝날은_오늘을_넘지_않는다() throws IOException {
    String src = read();

    assertThat(src).contains("LocalDate lastRealDay = LocalDate.now(zoneId);");
    assertThat(src).contains("if (outputEnd.isAfter(lastRealDay)) { outputEnd = lastRealDay; }");
  }

  /** 요청 존으로 오늘을 정해야 한다 - 서버 존으로 자르면 자정 무렵 하루가 어긋난다. */
  @Test
  void 오늘은_요청_존으로_정한다() throws IOException {
    assertThat(read()).doesNotContain("LocalDate lastRealDay = LocalDate.now();");
  }

  /** 자르는 자리는 끝날을 정한 <b>직후</b>여야 한다 - 시세 조회 구간도 같은 값을 쓰기 때문이다. */
  @Test
  void 자르는_자리가_끝날_계산_바로_뒤다() throws IOException {
    String src = read();
    int at = src.indexOf("LocalDate outputEnd = toInclusiveEndDate(end, zoneId);");
    int cap = src.indexOf("LocalDate lastRealDay = LocalDate.now(zoneId);");
    int priceWindow = src.indexOf("LocalDate endLocalDate = outputEnd;");

    assertThat(at).isPositive();
    assertThat(cap).isGreaterThan(at);
    assertThat(priceWindow).as("시세 조회 구간보다 앞에서 잘라야 한다").isGreaterThan(cap);
  }

  /** 루프는 잘린 끝날을 경계로 쓴다 - 빈 창이면 한 번도 돌지 않는다. */
  @Test
  void 루프가_잘린_끝날을_쓴다() throws IOException {
    assertThat(read()).contains("while (!currentDay.isAfter(outputEnd))");
  }

  /** 과거 쪽 규칙은 그대로 둔다 - 전부 0 인 구간을 거르는 검사가 사라지면 2000 년이 12 줄로 돌아온다. */
  @Test
  void 전부_0_인_구간을_거르는_규칙이_남아_있다() throws IOException {
    assertThat(read()).contains("if (isEmptyBucket(summary)) { return; }");
  }
}
