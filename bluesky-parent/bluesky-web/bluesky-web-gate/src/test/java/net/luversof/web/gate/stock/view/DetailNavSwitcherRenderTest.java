package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.DetailNavEntry;

/**
 * 상세 화면에서 <b>같은 종류의 다른 대상</b>으로 바로 갈 수 있는지 렌더해서 본다.
 *
 * <p>예전에는 종목 상세에서 다른 종목을 보려면 뒤로 가서 목록을 다시 찾아야 했다. 계좌도 마찬가지다. 화면에서 다음으로 할 일이 대부분 "다른 것도 보기" 인데, 그
 * 경로만 없었다.
 */
class DetailNavSwitcherRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/detailNavSwitcher.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String render(List<DetailNavEntry> entries) {
    Map<String, Object> model = new HashMap<>();
    model.put("entries", entries);
    model.put("switchLabel", MessageUtil.getMessage("stock.detail.switch.stock"));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  private List<DetailNavEntry> holdings() {
    return List.of(
        new DetailNavEntry("가나종목", "1,311,180,000", "/stock/item?stockItemId=aaa", true),
        new DetailNavEntry("나다종목", "17,922,810", "/stock/item?stockItemId=bbb", false),
        new DetailNavEntry("다라종목", "3,026,000", "/stock/item?stockItemId=ccc", false));
  }

  @Test
  void 다른_대상으로_가는_링크를_낸다() {
    String html = render(holdings());

    assertThat(html)
        .as("전환기가 없으면 뒤로 가서 목록을 다시 찾아야 한다")
        .contains(MessageUtil.getMessage("stock.detail.switch.stock"))
        .contains("/stock/item?stockItemId=bbb")
        .contains("/stock/item?stockItemId=ccc")
        .contains("나다종목")
        .contains("다라종목");
  }

  /** 지금 보고 있는 것을 표시한다. 없으면 목록에서 내 위치를 찾느라 이름을 하나씩 읽게 된다. */
  @Test
  void 지금_보는_대상을_표시한다() {
    String html = render(holdings());

    int current = html.indexOf("가나종목");
    assertThat(current).as("지금 보는 대상을 목록에 넣지 않았다").isGreaterThan(0);
    // active 표시는 그 항목의 <a> 에만 붙는다.
    assertThat(html.substring(0, current)).contains("active");
    int other = html.indexOf("나다종목");
    assertThat(html.substring(current, other))
        .as("지금 보는 것 말고 다른 항목에도 표시가 붙으면 표시가 무의미해진다")
        .doesNotContain("active");
  }

  @Test
  void 보조_문구를_함께_적는다() {
    assertThat(render(holdings()))
        .as("이름만 있으면 어느 것이 큰 자리인지 알 수 없어 결국 하나씩 눌러 보게 된다")
        .contains("1,311,180,000")
        .contains("17,922,810");
  }

  /** 갈 곳이 하나뿐이면(=지금 보는 것) 전환기가 할 일이 없다. 눌러도 아무 데도 못 가는 버튼은 방해만 된다. */
  @Test
  void 갈_곳이_없으면_그리지_않는다() {
    assertThat(
            render(List.of(new DetailNavEntry("가나종목", "", "/stock/item?stockItemId=aaa", true)))
                .trim())
        .isEmpty();
    assertThat(render(List.of()).trim()).isEmpty();
  }
}
