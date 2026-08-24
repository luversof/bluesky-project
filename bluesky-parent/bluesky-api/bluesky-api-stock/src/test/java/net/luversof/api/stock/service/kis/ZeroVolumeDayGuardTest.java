package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 거래가 없던 날의 시세 행을 새로 만들지 않는지 본다.
 *
 * <p>거래가 없으면 KIS 는 종가 자리에 직전 종가를 넣고 거래량을 0 으로 준다. 그 응답을 그대로 새 행으로 저장하면 그 날짜에 '확정 종가'가 하나 생기고, 평가 기준
 * 일자가 실제보다 앞당겨진다(실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0, 종가는 2026-08-19 와 동일 - 시가/고가/저가/거래량까지 같은
 * 건 0 건이라 단순 행 복제는 아니다). 평가액 자체는 어차피 같은 종가를 쓰므로 달라지지 않고, 달라지는 것은 화면에 적히는 날짜뿐이다.
 *
 * <p>이 검사는 소스를 읽는다. 판단이 KIS 응답을 도는 반복문 안에 있어 HTTP·인증을 세우지 않고는 호출할 수 없기 때문이다. 대신 <b>새 행을 만들 때만</b>
 * 건너뛰는지(이미 있는 행은 과거 보정으로 거래량이 0 으로 정정될 수 있어 건드리면 안 된다)를 위치로 확인한다.
 */
class ZeroVolumeDayGuardTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/kis/KisStockPriceUpdateService.java");

  private String source() throws IOException {
    assertThat(SERVICE).as("파일이 옮겨졌다: " + SERVICE).exists();
    return Files.readString(SERVICE, StandardCharsets.UTF_8);
  }

  @Test
  void 거래량이_0_이면_새_행을_만들지_않는다() throws IOException {
    String source = source();
    assertThat(source)
        .as("거래량 0 인 날을 그대로 저장하면 없는 종가가 생긴다")
        .contains("if (newVolume == 0L) {")
        .contains("skippedZeroVolume++");
  }

  @Test
  void 건너뛰기는_새_행_생성_분기_안에서만_일어난다() throws IOException {
    String source = source();
    int newRowBranch = source.indexOf("if (history == null) {");
    int skip = source.indexOf("if (newVolume == 0L) {");
    int createRow = source.indexOf("history = new StockPriceHistory();");
    int elseBranch = source.indexOf("} else {", newRowBranch);

    assertThat(newRowBranch).as("새 행 생성 분기를 찾지 못했다").isGreaterThan(0);
    assertThat(skip)
        .as("거래량 0 검사가 새 행 생성 분기 안에 없다. 기존 행 갱신까지 막으면 과거 보정을 놓친다")
        .isGreaterThan(newRowBranch)
        .isLessThan(elseBranch);
    assertThat(skip).as("행을 만든 뒤에 건너뛰면 이미 늦다").isLessThan(createRow);
  }

  /** 조용히 건너뛰면 "왜 어제까지만 있지?"를 설명할 수 없다. */
  @Test
  void 건너뛴_건수를_남긴다() throws IOException {
    assertThat(source()).as("건너뛴 사실이 로그에 남지 않는다").contains("skipped {} zero-volume day(s)");
  }
}
