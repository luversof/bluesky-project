package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간을 좁혀서 빈 것을 "원래 없다" 고 말하지 않는다.
 *
 * <p>상세 두 화면의 빈 안내는 "이 계좌의 매매 내역이 없습니다" 처럼 <b>기간을 말하지 않았다</b>. 그런데 그 문구가 뜨는 자리는 기간이 걸린 목록이다 &mdash;
 * 실측 2026-09-12: 이 계좌는 전체 기간에 매매 27 건 · 배당 40 건이 있는데 2016 년만 보면 "이 계좌의 매매 내역이 없습니다" 가 떴다(종목 상세도 매매
 * 2 건이 있는데 같은 문구).
 *
 * <p>형제 화면들은 이미 기간을 밝힌다("해당 기간 거래 내역이 없습니다" · "해당 기간에 배당 내역이 없습니다"). 같은 화면의 시세 안내도 "이 기간에는 …" 이다
 * &mdash; 네 문구를 그 규칙에 맞춘다.
 */
class DetailEmptyScopeWordingTest {

  private static final String[] KEYS = {
    "stock.item.detail.empty.trades",
    "stock.item.detail.empty.dividends",
    "stock.account.detail.empty.trades",
    "stock.account.detail.empty.dividends",
  };

  @Test
  void 네_문구가_기간을_밝힌다() throws IOException {
    String ko =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.ISO_8859_1);
    String en =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.ISO_8859_1);
    for (String key : KEYS) {
      assertThat(valueOf(ko, key)).as(key + " (ko) 는 기간을 밝혀야 한다").startsWith(inThisPeriodKo());
      assertThat(valueOf(en, key)).as(key + " (en)").contains("in this period");
    }
  }

  /** 같은 화면의 시세 안내가 쓰던 접두와 같아야 화면 안에서 말투가 갈리지 않는다. */
  @Test
  void 시세_안내와_같은_말투다() throws IOException {
    String ko =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.ISO_8859_1);
    assertThat(valueOf(ko, "stock.item.detail.empty.price.history")).startsWith(inThisPeriodKo());
  }

  /** "이 기간에는 " 를 유니코드 이스케이프로 적는다(.properties 는 ASCII 로 둔다). */
  private static String inThisPeriodKo() {
    char bs = (char) 92;
    return bs + "uC774 " + bs + "uAE30" + bs + "uAC04" + bs + "uC5D0" + bs + "uB294 ";
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
