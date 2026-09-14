package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간이 걸린 자리의 부재 문구는 기간을 밝힌다.
 *
 * <p>실측 2026-09-12(아홉 화면에서 부재 문구 19 종 수집): 같은 화면에 "해당 기간 …" 형제가 있는데도 범위를 안 밝힌 문구가 있었다. 매매는 "해당 기간
 * 거래 내역이 없습니다"(차트) 옆에서 목록이 "거래 내역이 없습니다" 였고, 배당도 마찬가지였다.
 *
 * <p>활동 문구는 <b>공유 키</b>였다 &mdash; 대시보드의 "최근 활동" 카드({@code recentActivities})는 기간 파라미터를 받지 않으므로 그
 * 문구에 기간을 붙이면 거짓이 된다. 그래서 목록 전용 키를 따로 두고 카드 쪽은 건드리지 않는다.
 */
class PeriodScopedEmptyMessageTest {

  private static final String KO = "src/main/resources/uiMessage_ko.properties";
  private static final String EN = "src/main/resources/uiMessage.properties";

  @Test
  void 기간이_걸린_문구는_기간을_밝힌다() throws IOException {
    String ko = read(KO), en = read(EN);
    for (String key :
        new String[] {
          "stock.trade.message.no.data",
          "stock.dividend.yield.message.no.analytics",
          "stock.activity.message.no.period.data"
        }) {
      assertThat(valueOf(ko, key)).as(key + " (ko)").contains(periodKo());
      assertThat(valueOf(en, key)).as(key + " (en)").contains("in this period");
    }
  }

  /** 대시보드의 최근 활동 카드는 기간을 받지 않는다 - 그 문구에 기간을 붙이면 거짓이다. */
  @Test
  void 기간을_안_받는_자리는_그대로_둔다() throws IOException {
    assertThat(valueOf(read(KO), "stock.activity.message.no.data"))
        .as("대시보드 카드 문구")
        .doesNotContain(periodKo());
    String card =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/recentActivities.jte"),
            StandardCharsets.UTF_8);
    assertThat(card).contains("stock.activity.message.no.data");
    assertThat(card).doesNotContain("stock.activity.message.no.period.data");
  }

  /** 목록 쪽은 기간 문구를 쓴다. */
  @Test
  void 목록은_기간_문구를_쓴다() throws IOException {
    String list =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/activityList.jte"), StandardCharsets.UTF_8);
    assertThat(list).contains("stock.activity.message.no.period.data");
    String dividend =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte"),
            StandardCharsets.UTF_8);
    assertThat(dividend)
        .as("이미 있던 기간 문구 키를 쓴다 - 같은 뜻의 문구를 새로 만들지 않는다")
        .contains("stock.dividend.message.no.period.history");
  }

  /** "기간" 을 유니코드 이스케이프로 적는다(.properties 는 ASCII 로 둔다). */
  private static String periodKo() {
    char bs = (char) 92;
    return bs + "uAE30" + bs + "uAC04";
  }

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.ISO_8859_1);
  }

  private static String valueOf(String bundle, String key) {
    for (String line : bundle.split(String.valueOf((char) 10))) {
      String row = line.trim();
      if (row.startsWith(key) && row.contains("=")) {
        return row.substring(row.indexOf('=') + 1).trim();
      }
    }
    throw new IllegalStateException("키를 찾지 못했다: " + key);
  }
}
