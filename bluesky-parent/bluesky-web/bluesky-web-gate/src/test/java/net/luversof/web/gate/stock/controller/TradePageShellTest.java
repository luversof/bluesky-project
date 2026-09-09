package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 매매 화면의 껍데기(/stock/trade)는 백엔드를 부르지 않는다.
 *
 * <p>실측 2026-09-09(api-stock 을 내리고 화면 10개를 연 결과): 배당·활동·자산 성장은 껍데기가 그려지고 조각마다 "불러오지 못했습니다" 가 나왔는데,
 * 매매만 전체 오류 화면이었다. 원인은 페이지 핸들러가 계좌·종목 목록을 api-stock 에서 받아 모델에 넣는데 trade.jte 가 그 값을 읽지 않는 것 - 성공할 때는
 * 헛호출 2회, 실패할 때는 껍데기까지 죽였다. 목록은 조각(tradeList.jte)이 스스로 받는다.
 */
class TradePageShellTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");
  private static final Path TEMPLATE = Path.of("src/main/jte/stock/trade.jte");

  // 줄바꿈은 \R 로 맞춘다 - 다른 세션의 포매터가 컨트롤러를 CRLF 로 저장해 \n 만 보던 정규식이 빈손이 됐다(실측 2026-09-09).
  private static String handlerBody(String source, String path) {
    Matcher m =
        Pattern.compile(
                "@GetMapping\\(\""
                    + Pattern.quote(path)
                    + "\"\\)\\R  public String \\w+\\((.*?)\\R  \\}\\R",
                Pattern.DOTALL)
            .matcher(source);
    assertThat(m.find()).as("핸들러를 찾지 못했다: " + path).isTrue();
    return m.group(1);
  }

  @Test
  void 매매_페이지_핸들러는_원격_호출과_모델_주입이_없다() throws IOException {
    String body = handlerBody(Files.readString(CONTROLLER, StandardCharsets.UTF_8), "/trade");

    assertThat(body).as("껍데기가 백엔드에 기대면 백엔드가 내려갔을 때 화면 전체가 죽는다").doesNotContain("Client.");
    assertThat(body).doesNotContain("loadAccounts(").doesNotContain("loadStockItems(");
    assertThat(body).as("템플릿이 읽지 않는 값을 넣지 않는다").doesNotContain("model.addAttribute(");
    assertThat(body).contains("return \"stock/trade\";");
  }

  @Test
  void 매매_템플릿은_계좌_종목_목록을_받지_않는다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
    assertThat(template).doesNotContain("accounts").doesNotContain("stockItems");
  }
}
