package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 없는 종목/계좌 화면은 문서 제목으로도 그렇게 말한다.
 *
 * <p>실측 2026-09-12: {@code /stock/item?stockItemId=00000000-0000-7000-8000-000000000000} 은 404 를
 * 내면서도 문서 제목이 <b>"종목 상세 · Bluesky Stock"</b> &mdash; 정상 화면과 글자 하나 다르지 않았다. 계좌도 같았다. 없는 경로의 404 는
 * "페이지를 찾을 수 없습니다" 로 제목부터 말하는데 이 둘만 달랐다.
 *
 * <p>본문에는 "종목을 찾을 수 없습니다" 가 있었으므로 화면을 보는 사람은 안다. 문제는 <b>제목만 남는 자리</b>다 &mdash; 브라우저 탭, 방문 기록, 북마크,
 * 그리고 화면 읽기 프로그램이 페이지에 들어설 때 읽는 첫 마디.
 *
 * <p>셸(비 htmx 진입)과 조각(htmx 교체) 두 경로 모두에서 제목이 서야 한다.
 */
class DetailNotFoundTitleTest {

  @Test
  void 셸은_못_찾았을_때_다른_제목을_쓴다() throws IOException {
    assertShellTitle(
        "src/main/jte/stock/stockItemDetail.jte",
        "stock.item.detail.notfound.title",
        "stock.item.detail.breadcrumb");
    assertShellTitle(
        "src/main/jte/stock/accountDetail.jte",
        "stock.account.detail.notfound.title",
        "stock.account.detail.breadcrumb");
  }

  @Test
  void 조각도_못_찾았을_때_제목을_싣는다() throws IOException {
    // data-page-title 이 비면 common.ts 가 제목을 그대로 둔다 - 그래서 여기서도 값을 줘야 한다.
    for (String[] pair :
        new String[][] {
          {
            "src/main/jte/stock/htmx/stockItemDetailContent.jte", "stock.item.detail.notfound.title"
          },
          {
            "src/main/jte/stock/htmx/accountDetailContent.jte",
            "stock.account.detail.notfound.title"
          }
        }) {
      String template = Files.readString(Path.of(pair[0]), StandardCharsets.UTF_8);
      String attribute = valueOfAttribute(template, "data-page-title");
      assertThat(attribute).as(pair[0] + " 에 data-page-title 이 없다").isNotNull();
      assertThat(attribute)
          .as(pair[0] + " 은 못 찾았을 때 제목을 비운다 - 정상 화면 제목이 그대로 남는다")
          .doesNotContain(": null");
      assertThat(attribute).as(pair[0]).contains(pair[1]);
    }
  }

  /** 두 언어 모두에 문구가 있어야 한다 - 하나라도 빠지면 키 이름이 그대로 제목이 된다. */
  @Test
  void 문구는_두_언어에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains("stock.item.detail.notfound.title");
      assertThat(text).as(bundle).contains("stock.account.detail.notfound.title");
    }
  }

  private void assertShellTitle(String templatePath, String notFoundKey, String normalKey)
      throws IOException {
    String template = Files.readString(Path.of(templatePath), StandardCharsets.UTF_8);
    assertThat(template)
        .as(templatePath + " 에 notFound 파라미터가 없다")
        .contains("@param boolean notFound");
    int at = template.indexOf("pageTitle = ");
    assertThat(at).as(templatePath + " 에 pageTitle 지정이 없다").isGreaterThanOrEqualTo(0);
    String line = template.substring(at, template.indexOf((char) 10, at));
    assertThat(line).as(templatePath + " 의 제목이 찾음/못 찾음으로 갈리지 않는다").contains("notFound ?");
    assertThat(line).as(templatePath).contains(notFoundKey);
    assertThat(line).as(templatePath + " 의 정상 제목이 사라졌다").contains(normalKey);
  }

  /** 정규식 없이 속성 값을 읽는다(빌드 도구가 이스케이프를 먹는 일이 반복돼 단순하게 둔다). */
  private String valueOfAttribute(String template, String attribute) {
    // 값 안에 MessageUtil.getMessage("...") 의 따옴표가 들어 있다 - 닫는 따옴표가 아니라
    // 표현식의 끝까지 읽어야 한다(첫 따옴표에서 끊으면 가짜로 실패한다, 실측 2026-09-12).
    String marker = attribute + "=" + (char) 34 + "${";
    int at = template.indexOf(marker);
    if (at < 0) {
      return null;
    }
    int from = at + marker.length();
    int to = template.indexOf("}" + (char) 34, from);
    return to < 0 ? null : template.substring(from, to);
  }
}
