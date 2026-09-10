package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 없는 id 의 종목·계좌 상세: 껍데기는 404 로 답하고, 조각의 빈 안내는 "찾을 수 없음" 이다.
 *
 * <p>실측 2026-09-09: {@code /stock/item?stockItemId=garbage} 가 200 + "데이터 없음" 이었다. 사용자는 '종목은 있는데
 * 데이터가 비었다' 로 읽고, 감시·북마크·검색엔진은 '있는 화면' 으로 본다. 화면은 그대로 그리되(껍데기 + 안내) 상태 코드와 문구가 사실을 말한다.
 */
class DetailNotFoundTest {

  private static final Path MAIN = Path.of("src/main");

  private static String read(String relative) throws IOException {
    return Files.readString(MAIN.resolve(relative), StandardCharsets.UTF_8);
  }

  private static String handlerBody(String source, String path) {
    // 줄바꿈은 \R - 다른 세션의 포매터가 CRLF 로 저장한다.
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
  void 없는_id_의_껍데기는_404_다() throws IOException {
    String source =
        read("java/net/luversof/web/gate/stock/controller/StockDetailViewController.java");
    for (String[] c : new String[][] {{"/item", "stockItem"}, {"/account", "account"}}) {
      String body = handlerBody(source, c[0]);
      int shell = body.indexOf("request.getHeader(\"HX-Request\") == null");
      int status = body.indexOf("response.setStatus(HttpServletResponse.SC_NOT_FOUND)");
      assertThat(shell).as(c[0] + " 껍데기 분기").isPositive();
      assertThat(status).as(c[0] + " 껍데기 분기 안에서 404").isGreaterThan(shell);
      assertThat(body.substring(shell, status))
          .as(c[0] + " 404 는 엔티티가 없을 때만")
          .contains("if (" + c[1] + " == null || " + c[1] + ".id() == null)");
    }
  }

  @Test
  void 조각의_빈_안내는_찾을_수_없음_문구다() throws IOException {
    for (String[] c :
        new String[][] {
          {"jte/stock/htmx/stockItemDetailContent.jte", "stockItem", "stock.item.detail.notfound"},
          {"jte/stock/htmx/accountDetailContent.jte", "account", "stock.account.detail.notfound"}
        }) {
      String template = read(c[0]);
      int nullBranch = template.indexOf("@if(" + c[1] + " == null)");
      int elseBranch = template.indexOf("@else", nullBranch);
      String branch = template.substring(nullBranch, elseBranch);
      assertThat(branch).as(c[0]).contains("MessageUtil.getMessage(\"" + c[2] + "\")");
      assertThat(branch)
          .as(c[0] + " 는 더 이상 '데이터 없음' 을 쓰지 않는다")
          .doesNotContain("common.message.no.data");
    }
  }

  @Test
  void 문구_키가_두_번들에_있다() throws IOException {
    for (String bundle :
        List.of("resources/uiMessage.properties", "resources/uiMessage_ko.properties")) {
      String source = read(bundle);
      assertThat(source)
          .as(bundle)
          .contains("stock.item.detail.notfound")
          .contains("stock.account.detail.notfound");
    }
  }
}
