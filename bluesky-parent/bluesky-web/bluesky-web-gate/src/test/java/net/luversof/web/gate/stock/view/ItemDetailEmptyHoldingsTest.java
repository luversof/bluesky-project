package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 종목 상세의 '계좌별 보유 현황' 이 비었을 때 하는 말.
 *
 * <p>이 구역만 범용 "데이터가 없습니다" 를 썼다 - 실측 2026-09-13: 86 종목 중 <b>77 종목</b>(전량 매도 34 · 무거래 43)이 그 문구를 냈다.
 * 자료가 유실된 것과 보유가 없는 것은 다르다. 같은 성격인 계좌 상세의 '보유 종목' 은 이미 "이 계좌의 보유 종목이 없습니다" 라고 말하고, 같은 화면의 매매 · 배당 ·
 * 주가 구역도 각자 전용 문구를 가지고 있었다 - 이 하나만 빠져 있었다.
 *
 * <p>다만 형제 문구와 달리 <b>"이 기간에는" 을 붙이면 안 된다</b> - 이 표는 기간과 무관한 현재 보유 스냅샷이라, 기간을 넓히면 나올 것처럼 말하는 셈이 된다.
 *
 * <p>한글 파일의 값은 {@code &#92;uXXXX} 이스케이프로 적혀 있다. 파일 글자 그대로 비교하면 어떤 한글 단언도 그냥 통과한다 - 실측: 첫 판에 "보유" 를
 * 찾는 단언이 그래서 헛돌았다. {@link Properties} 로 읽어 <b>디코드된 값</b>을 본다.
 */
class ItemDetailEmptyHoldingsTest {

  private static final String FRAGMENT = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String KEY = "stock.item.detail.empty.account.holdings";
  private static final String KO = "src/main/resources/uiMessage_ko.properties";
  private static final String EN = "src/main/resources/uiMessage.properties";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 빈 가지에서 전용 키를 쓴다. */
  @Test
  void 빈_계좌별_보유는_전용_문구를_쓴다() throws IOException {
    assertThat(emptyBranch()).contains("MessageUtil.getMessage(\"" + KEY + "\")");
  }

  /** 범용 문구는 이 화면에서 사라져야 한다 - 남아 있으면 고친 자리가 아닌 다른 자리를 고친 것이다. */
  @Test
  void 이_화면에_범용_문구가_남아_있지_않다() throws IOException {
    assertThat(read(FRAGMENT)).doesNotContain("common.message.no.data");
  }

  /** 기간과 무관한 스냅샷이므로 기간을 암시하면 안 된다. */
  @Test
  void 문구가_기간을_말하지_않는다() throws IOException {
    assertThat(value(KO)).as("한글 값").doesNotContain("이 기간");
    assertThat(value(EN).toLowerCase()).as("영문 값").doesNotContain("in this period");
  }

  /** 보유가 없다는 사실을 말해야 한다 - 그래야 '자료 없음' 과 갈린다. */
  @Test
  void 문구가_보유가_없다고_말한다() throws IOException {
    assertThat(value(KO)).as("한글 값").contains("보유").contains("없");
    assertThat(value(EN).toLowerCase()).as("영문 값").contains("hold");
  }

  /** 형제 구역은 그대로 각자 문구를 쓴다 - 한 키로 뭉뚱그리면 다시 범위가 흐려진다. */
  @Test
  void 형제_구역은_각자_문구를_유지한다() throws IOException {
    String jte = read(FRAGMENT);

    assertThat(jte).contains("stock.item.detail.empty.price.history");
    assertThat(jte).contains("stock.item.detail.empty.trades");
    assertThat(jte).contains("stock.item.detail.empty.dividends");
  }

  /** 빈 가지 한 덩어리만 떼어 낸다 - 파일 전체로 보면 형제 구역의 같은 상자에 걸린다. */
  private String emptyBranch() throws IOException {
    String jte = read(FRAGMENT);
    int at = jte.indexOf("@if(accountHoldings == null || accountHoldings.isEmpty())");
    if (at < 0) {
      return "";
    }
    return jte.substring(at, jte.indexOf("@else", at));
  }

  /** 이스케이프를 푼 값. 없으면 빈 문자열이라 단언이 실패한다. */
  private String value(String path) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(Files.readString(Path.of(path), StandardCharsets.UTF_8)));
    return properties.getProperty(KEY, "");
  }
}
