package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 가져오기 오버레이는 화면 전체를 덮어야 한다.
 *
 * <p>실측 2026-09-14: 덮지 못했다. 이 패널은 관리 화면에서 {@code space-y-6} 컨테이너의 자식이라 {@code margin-block-end:
 * 1.5rem} 을 물려받는데, {@code top:0} + {@code bottom:0} 과 겹치면 쓰이는 높이가 <b>뷰포트 - 24px</b> 가 된다(1280x800
 * 에서 776px, 1600x1000 에서 976px, 375x812 에서 788px - 세 폭에서 모두 24px). 화면 아래 24px 이 그대로 눌리는 채로 남는다.
 *
 * <p>그래서 {@code m-0} 으로 부모의 간격을 끊는다. 이 가드는 그 세 조각(고정 · 사방 0 · 여백 0)이 함께 남아 있는지 본다 - 하나만 빠져도 증상이
 * 돌아온다. 간격 유틸리티는 {@code :where()} 안에 있어 특정도 0 이므로 {@code m-0} 이 이긴다.
 */
class SubmitOverlayFullViewportTest {

  private static final Pattern PANEL =
      Pattern.compile("<div[^>]*data-submit-overlay-panel=\"true\"[^>]*class=\"([^\"]*)\"");

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

  private String render(String template, Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  private String panelClasses(String html) {
    Matcher matcher = PANEL.matcher(html);
    assertThat(matcher.find()).as("오버레이 패널을 찾지 못했다 - 검사가 무력해진다").isTrue();
    return " " + matcher.group(1) + " ";
  }

  @Test
  void 오버레이는_고정_사방영_여백영을_함께_갖는다() {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("adminTab", "monthly-reference");
    String classes = panelClasses(render("stock/admin.jte", model));

    assertThat(classes).as("뷰포트 기준이어야 한다").contains(" fixed ");
    assertThat(classes).as("사방 0 이어야 뷰포트를 채운다").contains(" inset-0 ");
    assertThat(classes).as("부모의 space-y-* 가 준 margin 이 남으면 그만큼 아래가 안 덮인다").contains(" m-0 ");
  }

  /** 시뮬레이터 화면도 같은 조각을 쓴다. 한 곳만 고치고 끝내면 다른 화면에서 증상이 남는다. */
  @Test
  void 시뮬레이터_화면의_오버레이도_같다() {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("simulatorTab", "monthly-dividend");
    String classes = panelClasses(render("stock/simulator.jte", model));

    assertThat(classes).contains(" fixed ").contains(" inset-0 ").contains(" m-0 ");
  }

  /** 처음에는 숨어 있어야 한다 - 열려 있는 채로 나가면 화면을 못 쓴다. */
  @Test
  void 오버레이는_처음에_숨어_있다() {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("adminTab", "monthly-reference");

    assertThat(panelClasses(render("stock/admin.jte", model))).contains(" hidden ");
  }
}
