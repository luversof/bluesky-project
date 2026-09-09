package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * 카드 컴포넌트는 id 를 안 주면 {@code id} 속성을 내지 않는다.
 *
 * <p>JTE 는 {@code null} 속성만 빼고 빈 문자열은 {@code id=""} 로 그대로 낸다. 컴포넌트의 기본값이 {@code ""} 라 카드마다 빈 id 가
 * 붙었고, 13화면 실측(2026-09-09)에서 화면마다 2~9개씩 겹쳤다. 빈 id 는 HTML 에서 무효이고 {@code hx-target}/{@code label
 * for} 가 기대하는 "id 는 문서에서 하나" 를 깨뜨린다.
 */
class UiCardEmptyIdTest {

  private static String render(String template, Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  @Test
  void id_를_안_주면_id_속성이_없다() {
    for (String template :
        new String[] {"_components/ui/card.jte", "_components/ui/tableCard.jte"}) {
      Map<String, Object> model = new HashMap<>();
      model.put("body", gg.jte.Content.class.cast(null));
      String html = render(template, model);
      assertThat(html).as(template).doesNotContain("id=\"\"").doesNotContain("id=\"null\"");
    }
  }

  @Test
  void id_를_주면_그대로_낸다() {
    Map<String, Object> model = new HashMap<>();
    model.put("id", "dividendChangeCard");
    model.put("body", gg.jte.Content.class.cast(null));
    assertThat(render("_components/ui/card.jte", model)).contains("id=\"dividendChangeCard\"");
  }
}
