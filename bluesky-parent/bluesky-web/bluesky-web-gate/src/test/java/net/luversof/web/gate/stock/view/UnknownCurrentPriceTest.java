package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 종목 상세의 '현재가' 는 <b>기간 값이 아니라 시점 값</b>이다 - 보유 종목은 기간을 좁혀도 같은 값을 보여 준다(실측 2026-09-13 삼성전자: '전체' 든
 * 2016 년이든 269,500).
 *
 * <p>그런데 거래한 적 없는 종목은 손익 행이 없어 가격 이력으로 메우는데, 그 이력은 <b>고른 기간으로 잘려</b> 있었다. 창 안에 시세 점이 하나도 없으면 폴백이
 * 걸리지 않아 0 이 그대로 남는다 - 실측 2026-09-13 기업은행(024110): '전체' 는 21,350 인데 '최근 1개월' 과 2016 년은 둘 다 "현재가 0"
 * 이었다(이 종목 이력은 2026-03-25~04-03 여덟 점뿐이다). 무거래 종목이 43 개라 흔한 조합이다.
 *
 * <p>기간 없는 호출로 한 번 더 구하고, 그래도 없으면 0 대신 없다고 적는다 - 실측: 86 종목 중 4 종목(0177R0 · 0190G0 · 0210E0 ·
 * 0219E0)은 가격 이력 자체가 없어 네 종목 화면이 모두 "현재가 0" 이었다. 같은 화면의 매매 · 배당 · 주가 구역은 이미 "…없습니다" 라고 말한다.
 */
class UnknownCurrentPriceTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java";
  private static final String FRAGMENT = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String SHELL = "src/main/jte/stock/stockItemDetail.jte";
  private static final String KEY = "stock.item.detail.current.price.unknown";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /**
   * 창이 비었을 때만 한 번 더 부른다 - 늘 부르면 보유 종목마다 전 구간 이력이 따라온다.
   *
   * <p>조건과 호출을 따로 본다 - 한 덩어리로 묶으면 포매터가 줄을 다시 나눌 때 내용이 그대로여도 깨진다(실측 2026-09-13: 이웃 가드가 그렇게 깨졌다).
   */
  @Test
  void 창에_시세_점이_없으면_기간_없이_다시_구한다() throws IOException {
    String src = read(CONTROLLER);

    assertThat(src).contains("if (currentPrice.signum() == 0 && lastPricePoint == null) {");
    assertThat(src).contains("fullPriceHistory(resolvedId));");
  }

  /** 기간 없는 호출이어야 한다 - 파라미터를 실으면 같은 창을 다시 묻는 꼴이라 아무것도 달라지지 않는다. */
  @Test
  void 그_호출은_기간_파라미터를_싣지_않는다() throws IOException {
    assertThat(helperBody())
        .contains("stockItemClient.getPriceHistory(stockItemId, new LinkedMultiValueMap<>())");
  }

  /** 시세 하나 때문에 화면 전체가 막히면 안 된다. */
  @Test
  void 그_호출이_실패해도_빈_목록으로_넘긴다() throws IOException {
    String body = helperBody();

    assertThat(body).contains("catch (Exception ex)");
    assertThat(body).contains("log.warn(\"전 구간 가격 이력 조회 실패: stockItemId={}\", stockItemId, ex)");
    assertThat(body).contains("return List.of();");
  }

  /** 값이 끝내 없으면 화면이 그 사실을 알아야 한다. */
  @Test
  void 모르는_값이라는_사실을_화면에_넘긴다() throws IOException {
    assertThat(read(CONTROLLER))
        .contains("model.addAttribute(\"currentPriceUnknown\", currentPrice.signum() == 0);");
  }

  /** 0 은 잰 값처럼 읽힌다 - 값 자리에 문구를 넣고, 금액이 아니므로 금액 숨김도 걸지 않는다. */
  @Test
  void 카드가_0_대신_문구를_적는다() throws IOException {
    String branch = unknownBranch();

    assertThat(branch).contains("MessageUtil.getMessage(\"" + KEY + "\")");
    assertThat(branch).contains("amount = false");
    assertThat(branch).doesNotContain("StockFormatUtil.displayWon(currentPrice)");
  }

  /** 값을 아는 종목은 그대로 금액으로 그린다. */
  @Test
  void 값을_알면_금액_그대로_그린다() throws IOException {
    String jte = read(FRAGMENT);
    int at = jte.indexOf("@else", jte.indexOf("@if(currentPriceUnknown)"));

    assertThat(at).isPositive();
    assertThat(jte.substring(at, jte.indexOf("@endif", at)))
        .contains("String.format(\"%,d\", StockFormatUtil.displayWon(currentPrice))");
  }

  /** 조각과 껍데기가 같은 값을 봐야 한다 - 껍데기가 안 넘기면 전체 페이지에서만 기본값 false 로 되돌아간다. */
  @Test
  void 껍데기가_그_값을_넘긴다() throws IOException {
    assertThat(read(FRAGMENT)).contains("@param boolean currentPriceUnknown = false");
    assertThat(read(SHELL)).contains("@param boolean currentPriceUnknown = false");
    assertThat(read(SHELL)).contains("currentPriceUnknown = currentPriceUnknown,");
  }

  @Test
  void 문구가_두_로케일에_있다() throws IOException {
    assertThat(valueOf("src/main/resources/uiMessage_ko.properties")).as("한글 값").isNotBlank();
    assertThat(valueOf("src/main/resources/uiMessage.properties").toLowerCase())
        .as("영문 값")
        .contains("price");
  }

  /** 헬퍼 한 덩어리만 떼어 낸다 - 파일 전체로 보면 다른 try/catch 에 걸린다. */
  private String helperBody() throws IOException {
    String src = read(CONTROLLER);
    int at =
        src.indexOf(
            "private List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> fullPriceHistory(");
    if (at < 0) {
      return "";
    }
    return src.substring(at, Math.min(src.length(), at + 700));
  }

  /** '모를 때' 가지만 떼어 낸다 - 같은 카드가 @else 쪽에도 있어 파일 전체로는 둘이 섞인다. */
  private String unknownBranch() throws IOException {
    String jte = read(FRAGMENT);
    int at = jte.indexOf("@if(currentPriceUnknown)");
    if (at < 0) {
      return "";
    }
    return jte.substring(at, jte.indexOf("@else", at));
  }

  /** 그 키의 값 한 줄만 떼어 낸다 - 파일 전체로 보면 다른 메시지에 걸린다. */
  private String valueOf(String path) throws IOException {
    String properties = Files.readString(Path.of(path), StandardCharsets.UTF_8);
    int at = properties.indexOf(KEY);
    if (at < 0) {
      return "";
    }
    return properties.substring(properties.indexOf("=", at) + 1, properties.indexOf((char) 10, at));
  }
}
