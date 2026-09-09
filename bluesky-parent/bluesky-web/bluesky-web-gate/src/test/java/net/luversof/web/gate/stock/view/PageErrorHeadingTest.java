package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 오류·빈 화면도 페이지다 &mdash; 제목(h1)이 있어야 한다.
 *
 * <p>실측 2026-09-09: api-stock 을 내리고 axe 를 돌리니 정상 화면 13개는 0건인데, 전체 화면 오류로 떨어지는 4화면(시뮬레이터·관리·종목·계좌
 * 상세)만 {@code page-has-heading-one} 에 걸렸다. pageError 의 제목이 div 였고, 종목·계좌 상세는 없는 id 로 들어오면 빵부스러기도
 * 제목도 없이 "데이터 없음" 만 그렸다. 조각 오류 알림(loadError)은 페이지 h1 아래에 들어가므로 해당 없다.
 */
class PageErrorHeadingTest {

  private static String read(String relative) throws IOException {
    return Files.readString(Path.of("src/main/jte/stock", relative), StandardCharsets.UTF_8);
  }

  @Test
  void 오류_화면의_제목은_h1_이다() throws IOException {
    String template = read("pageError.jte");
    assertThat(template)
        .as("제목 요소")
        .contains("<h1 class=\"font-semibold text-base\">${MessageUtil.getMessage(titleKey)}</h1>");
    assertThat(template.split("<h1")).as("h1 은 하나").hasSize(2);
  }

  /** 없는 id 의 상세 화면: '데이터 없음' 앞에 빵부스러기와 pageHeader(h1) 가 온다. */
  @Test
  void 없는_id_의_상세_화면에도_제목이_있다() throws IOException {
    for (String[] c :
        new String[][] {
          {"stockItemDetail.jte", "stockItem", "stock.item.detail.breadcrumb"},
          {"accountDetail.jte", "account", "stock.account.detail.breadcrumb"}
        }) {
      String template = read(c[0]);
      int nullBranch = template.indexOf("@if(" + c[1] + " == null)");
      int empty = template.indexOf("detail.notfound", nullBranch);
      assertThat(nullBranch).as(c[0] + " null 분기").isPositive();
      assertThat(empty).as(c[0] + " 빈 안내").isGreaterThan(nullBranch);
      String branch = template.substring(nullBranch, empty);
      assertThat(branch)
          .as(c[0] + " 의 null 분기에는 h1 을 그리는 pageHeader 가 있어야 한다")
          .contains(
              "@template._components.ui.pageHeader(title = MessageUtil.getMessage(\""
                  + c[2]
                  + "\"))")
          .contains("@template._components.ui.breadcrumbBack(");
    }
  }
}
