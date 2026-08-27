package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "평균" 기준의 실제 표본 길이를 화면이 밝히는지 본다.
 *
 * <p>예상 월배당의 "평균" 은 최근 <b>12건</b>의 주당 배당을 평균한다(건수 기준이지 기간 기준이 아니다 &mdash; api-stock 의 {@code
 * payouts.stream().limit(12)}). 상장이 얼마 안 된 종목은 이력이 12건에 못 미쳐 그만큼 짧은 평균이 되는데, 화면은 그냥 "평균" 이라고만 적어 그
 * 차이를 알 수 없었다.
 *
 * <p>실측 2026-08-23: 8 종목 중 2 종목이 이력 10 건이었다(RISE 코리아밸류업위클리고정커버드콜 · TIGER 코리아배당다우존스위클리커버드콜, 각각 9 개월
 * 구간). 두 종목의 예상 월배당 합은 전체의 1.2% 라 금액 영향은 작지만, "1년 평균" 으로 읽히는 값이 아닌 것은 밝혀야 한다.
 */
class ShortPayoutHistoryNoticeTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

  private static final Path TEMPLATE = Path.of("src/main/jte/stock/dividend.jte");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 컨트롤러가_짧은_이력_종목을_모아_넘긴다() throws IOException {
    String source = read(CONTROLLER);
    assertThat(source).contains("shortHistorySymbols");
    assertThat(source)
        .as("기준 건수가 api-stock 의 limit(12) 와 달라지면 안내가 틀린다")
        .contains("MONTHLY_DIVIDEND_AVERAGE_WINDOW = 12");
    assertThat(source)
        .as("이력이 12건 미만인 종목만 골라야 한다")
        .contains("count < MONTHLY_DIVIDEND_AVERAGE_WINDOW");
  }

  @Test
  void 화면이_그_목록을_실제로_그린다() throws IOException {
    String template = read(TEMPLATE);
    assertThat(template).contains("shortHistorySymbols");
    assertThat(template).as("목록이 비면 안내를 내지 않아야 한다").contains("!shortHistorySymbols.isEmpty()");
    assertThat(template)
        .as("문구를 코드에 박으면 영어 화면에 한글이 그대로 나간다")
        .contains("stock.dividend.calendar.short.history");
  }

  @Test
  void 안내_문구가_두_언어에_모두_있다() throws IOException {
    for (String bundle : java.util.List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      assertThat(read(Path.of("src/main/resources", bundle)))
          .as(bundle + " 에 문구가 없다 - MessageUtil 은 없는 키에 빈 문자열을 돌려주므로 조용히 사라진다")
          .contains("stock.dividend.calendar.short.history");
    }
  }
}
