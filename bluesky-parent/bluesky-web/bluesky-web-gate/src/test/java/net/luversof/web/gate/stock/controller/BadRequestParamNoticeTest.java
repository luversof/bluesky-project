package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 잘못된 입력이면 어느 값이 문제였는지 말해야 한다.
 *
 * <p>실측 2026-09-11: 주소에 {@code startDate=2026-08-21} 처럼 날짜만 적으면(컨트롤러가 받는 타입은 {@code Instant} 다)
 * 배당·매매·활동·자산성장 네 화면이 전부 "입력한 값(기간·필터 등)을 확인해 주세요" 만 띄웠다 &mdash; 무엇을 어떻게 고쳐야 하는지가 없었다. 앱이 스스로 만드는
 * 링크 28 곳은 모두 ISO 형식이라 정상 흐름은 멀쩡하다; 주소를 직접 고치거나 손으로 만든 링크가 이 길로 온다.
 */
class BadRequestParamNoticeTest {

  private static final Path RESOLVER =
      Path.of("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 형_변환_실패는_파라미터_이름을_싣는다() throws IOException {
    String resolver = read(RESOLVER);

    assertThat(resolver).contains("MethodArgumentTypeMismatchException mismatch");
    assertThat(resolver).as("파라미터 이름을 버리지 않는다").contains("clientDescArg = mismatch.getName()");
    assertThat(resolver)
        .as("날짜·시각 타입은 형식까지 알려 준다")
        .contains("Temporal.class.isAssignableFrom(requiredType)");
    assertThat(resolver).contains("stock.error.badrequest.date.desc");
    assertThat(resolver).contains("stock.error.badrequest.param.desc");
  }

  @Test
  void 묶음_바인딩_실패도_필드_이름을_싣는다() throws IOException {
    String resolver = read(RESOLVER);

    // 실측 2026-09-11: /stock/htmx/asset-growth/view 만 MethodArgumentNotValidException 이라
    // MethodArgumentTypeMismatchException 갈래를 타지 못하고 혼자 옛 문구를 띄웠다.
    assertThat(resolver).contains("ex instanceof BindException bindException");
    assertThat(resolver).contains("clientDescArg = fieldError.getField()");
    assertThat(resolver)
        .as("형 변환이면 요구 타입까지 꺼내 날짜 문구를 고른다")
        .contains("fieldError.unwrap(TypeMismatchException.class).getRequiredType()");
  }

  @Test
  void 조각과_페이지_양쪽에_전달한다() throws IOException {
    String resolver = read(RESOLVER);

    assertThat(resolver).as("htmx 조각").contains("loadError.addObject(" + '"' + "descArg" + '"');
    assertThat(resolver).as("전체 화면").contains("clientErrorView.addObject(" + '"' + "descArg" + '"');
  }

  @Test
  void 두_오류_화면이_이름을_문구에_끼운다() throws IOException {
    for (String name :
        new String[] {
          "src/main/jte/stock/htmx/fragments/loadError.jte", "src/main/jte/stock/pageError.jte"
        }) {
      String template = read(Path.of(name));
      assertThat(template)
          .as(name + " 는 descArg 를 받아야 한다")
          .contains("@param String descArg = null");
      assertThat(template)
          .as(name + " 는 이름이 있을 때만 MessageFormat 을 쓴다")
          .contains("descArg == null || descArg.isBlank() ? MessageUtil.getMessage(descKey)");
    }
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read(Path.of("src/main/resources").resolve(name));
      assertThat(bundle).contains("stock.error.badrequest.param.desc");
      assertThat(bundle).contains("stock.error.badrequest.date.desc");
    }
  }
}
