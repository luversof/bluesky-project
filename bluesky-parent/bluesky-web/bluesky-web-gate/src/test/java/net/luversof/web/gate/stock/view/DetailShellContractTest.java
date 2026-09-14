package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 상세 두 화면(종목·계좌)은 <b>첫 진입에 껍데기만</b> 그리고 내용은 htmx 로 받는다.
 *
 * <p>그래야 전체 새로고침에서도 전역 기간이 서버에 함께 실린다. 대신 이 규칙은 조용히 깨질 수 있다 - 껍데기도 200 이고 머리글·꼬리글이 다 있어서, 내용이 안 실려도
 * "화면이 나온다"로 보인다.
 *
 * <p>실측 2026-09-12: 같은 주소를 그냥 가져오면 29,800 바이트 껍데기(본문 263자)가 오고, {@code HX-Request} 를 붙이면 78,777 바이트
 * 조각이 온다. 이 차이를 모르고 껍데기를 훑어 "종목 43개 전수 이상 없음" 이라고 잘못 보고한 적이 있다.
 *
 * <p>고정하는 것은 셋이다. (1) 껍데기 분기가 {@code HX-Request} 없음으로 갈린다. (2) 껍데기는 {@code contentReady=false},
 * htmx 응답은 {@code true} 로 표시한다. (3) 템플릿이 그 표시를 보고 자리를 채운다.
 */
class DetailShellContractTest {

  private static final char Q = (char) 34;
  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  /** 두 화면 모두 같은 규칙으로 갈린다 - 한쪽만 고치면 두 화면이 다르게 동작한다. */
  @Test
  void 두_상세_화면이_같은_조건으로_껍데기를_고른다() throws IOException {
    String source = read(CONTROLLER);
    assertThat(count(source, "if (request.getHeader(" + Q + "HX-Request" + Q + ") == null) {"))
        .as("종목 상세와 계좌 상세")
        .isEqualTo(2);
  }

  /** 껍데기는 내용이 없다고 말하고, htmx 응답은 있다고 말한다. */
  @Test
  void 껍데기와_조각이_서로_다른_표시를_단다() throws IOException {
    String source = read(CONTROLLER);
    assertThat(count(source, "model.addAttribute(" + Q + "contentReady" + Q + ", false);"))
        .as("껍데기 둘")
        .isEqualTo(2);
    assertThat(count(source, "model.addAttribute(" + Q + "contentReady" + Q + ", true);"))
        .as("조각 둘")
        .isEqualTo(2);
  }

  /** 표시를 달기만 하고 템플릿이 안 보면 아무 소용이 없다. */
  @Test
  void 템플릿이_그_표시를_본다() throws IOException {
    for (String template :
        new String[] {
          "src/main/jte/stock/stockItemDetail.jte", "src/main/jte/stock/accountDetail.jte"
        }) {
      String jte = read(template);
      assertThat(jte).as(template + " 는 표시를 받는다").contains("@param boolean contentReady");
      assertThat(jte).as(template + " 는 표시를 보고 가른다").contains("@if(!contentReady)");
    }
  }
}
