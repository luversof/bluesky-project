package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 상세 두 화면(종목·계좌)의 날짜 선택기 하한.
 *
 * <p>'가장 이른 기간으로'(«) 는 이 하한을 알아야 목표 창을 정한다. 없으면 눌러도 아무 일이 일어나지 않는다 - 실측 2026-09-13: 두 화면이 {@code
 * dataFirstDate} 를 안 넘겨 « 를 두 번 눌러도 기간이 그대로였다(매매 화면은 2009-10-06 으로 이동).
 *
 * <p>하한은 <b>이 종목/이 계좌의</b> 최초일이어야 한다. 사용자 전체의 최초일을 쓰면 그 종목이 아직 없던 창으로 뛴다 - 실측 2026-09-13: 삼성전자 최초
 * 매매 2020-03-04 · 연금저축1 계좌 2025-03-28 · 사용자 전체 2009-10-06.
 *
 * <p>고정하는 것은 셋이다. (1) 종목 상세는 종목으로, 계좌 상세는 계좌로 좁혀 묻는다. (2) 조회가 실패해도 화면은 뜬다(하한만 빈다). (3) 두 템플릿이 그 값을
 * 선택기에 넘긴다.
 */
class DetailDataFirstDateTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 종목_상세는_종목으로_좁혀_묻는다() throws IOException {
    assertThat(read(CONTROLLER))
        .as("종목 아이디를 주고 계좌는 비운다")
        .contains("detailDataFirstDate(userId, resolvedId, null, filterZone)");
  }

  @Test
  void 계좌_상세는_계좌로_좁혀_묻는다() throws IOException {
    assertThat(read(CONTROLLER))
        .as("계좌 아이디를 주고 종목은 비운다")
        .contains("detailDataFirstDate(userId, null, resolvedId, filterZone)");
  }

  /** 이른 쪽 고르기는 util 이 한다 - 규칙 자체는 StockFirstDateUtilTest 가 동작으로 검증한다. */
  @Test
  void 이른_쪽_고르기를_util_에_맡긴다() throws IOException {
    assertThat(read(CONTROLLER))
        .contains(
            "StockFirstDateUtil.earliestLocalDate( response.tradeFirstDate(),"
                + " response.dividendFirstDate(), zone)");
  }

  /** 하한을 못 구해도 화면 전체를 막지 않는다 - « 가 비활성이 될 뿐이다. */
  @Test
  void 조회가_실패해도_화면은_뜬다() throws IOException {
    String source = read(CONTROLLER);
    int at = source.indexOf("private String detailDataFirstDate(");
    assertThat(at).isGreaterThanOrEqualTo(0);
    String body = source.substring(at, Math.min(source.length(), at + 1200));

    assertThat(body).as("예외를 삼키되").contains("catch (Exception ex)");
    assertThat(body).as("빈 값을 돌려주고").contains("return \"\";");
    assertThat(body).as("로그는 남긴다").contains("log.warn(");
  }

  @Test
  void 두_템플릿이_선택기에_넘긴다() throws IOException {
    for (String template :
        new String[] {
          "src/main/jte/stock/htmx/stockItemDetailContent.jte",
          "src/main/jte/stock/htmx/accountDetailContent.jte"
        }) {
      String jte = read(template);
      assertThat(jte).as(template + " 는 값을 받는다").contains("@param String dataFirstDate");
      assertThat(jte).as(template + " 는 선택기에 넘긴다").contains("dataFirstDate = dataFirstDate)");
    }
  }

  /**
   * '전체' 배지의 시작도 이 값을 쓴다 - 시계열만 보면 첫 거래보다 늦게 시작한다고 적어 표에 있는 행보다 좁은 구간을 말하게 된다(실측 2026-09-13: 삼성전자
   * 19 일 · 한투 위탁 13 일 · 연금저축1 2 일).
   */
  @Test
  void 배지_시작도_최초일로_보정한다() throws IOException {
    String source = read(CONTROLLER);

    assertThat(source)
        .contains("StockFirstDateUtil.coveredStart( covered.startDate(), detailFirstDate)");
    int count = 0;
    int at = source.indexOf("StockFirstDateUtil.coveredStart(");
    while (at >= 0) {
      count++;
      at = source.indexOf("StockFirstDateUtil.coveredStart(", at + 1);
    }
    assertThat(count).as("두 화면 모두").isEqualTo(2);
  }

  /** 배지의 끝도 화면이 그린 자료로 메운다 - 시계열이 없으면 "~ ?" 가 나간다. */
  @Test
  void 배지_끝도_자료로_메운다() throws IOException {
    String source = read(CONTROLLER);

    assertThat(source).contains("StockFirstDateUtil.coveredEnd( covered.endDate(), contentLast)");
    assertThat(source).as("매매와 배당 둘 다 본다").contains("TradeResponse::tradeDate");
    assertThat(source).contains("DividendResponse::payDate");
  }
}
