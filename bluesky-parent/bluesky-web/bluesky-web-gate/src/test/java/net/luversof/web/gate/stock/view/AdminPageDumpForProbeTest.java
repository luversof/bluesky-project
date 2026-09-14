package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

/**
 * 관리(월배당 기준) 화면을 통째로 그려 파일로 남긴다.
 *
 * <p>브라우저 탐침이 로그인 없이 레이아웃을 재려면 실제 페이지 HTML 이 있어야 한다. 조각만 그리면 바깥 래퍼(사이드바 · 고정 헤더)가 빠져서, 지금 쫓는 "가져오기
 * 오버레이가 화면 전체를 안 덮는다" 같은 <b>바깥 요소가 원인인</b> 문제를 재현하지 못한다.
 */
class AdminPageDumpForProbeTest {

  private static final Path OUT = Path.of("target", "probe", "admin-monthly-reference.html");

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

  @Test
  void 관리_월배당_기준_화면을_파일로_남긴다() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("username", "probe");
    model.put("adminTab", "monthly-reference");

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render("stock/admin.jte", model, output);
    String html = output.toString();

    assertThat(html).contains("data-submit-overlay-panel");
    Files.createDirectories(OUT.getParent());
    Files.writeString(OUT, html, StandardCharsets.UTF_8);
    assertThat(Files.size(OUT)).isGreaterThan(10_000L);
  }
}
