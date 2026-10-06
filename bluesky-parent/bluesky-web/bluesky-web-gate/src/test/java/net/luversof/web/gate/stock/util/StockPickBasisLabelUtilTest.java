package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.Basis;

/** 추천 기준(사용자 요청 2026-10-06)의 이름 · 설명. 키를 화면에서 조립하지 않고 여기서 기준마다 적는다 - 모르는 값이 빈 칸이 되지 않게. */
class StockPickBasisLabelUtilTest {

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
  void 기준마다_이름과_설명이_있다() {
    for (Basis basis : Basis.values()) {
      assertThat(StockPickBasisLabelUtil.label(basis))
          .as(basis + " 이름")
          .isNotBlank()
          .doesNotContain("stock.");
      assertThat(StockPickBasisLabelUtil.description(basis.param()))
          .as(basis + " 설명")
          .isNotBlank()
          .doesNotContain("stock.");
    }
  }

  @Test
  void 모르는_값은_그대로_두고_설명은_지어내지_않는다() {
    assertThat(StockPickBasisLabelUtil.label("weekly")).isEqualTo("weekly");
    assertThat(StockPickBasisLabelUtil.label((String) null)).isEmpty();
    assertThat(StockPickBasisLabelUtil.description("weekly")).isEmpty();
  }

  /** 로케일 가드: 영어 · 한국어 파일 둘 다 키가 있어야 한다(한쪽이 빠지면 그 언어 화면만 빈 칸이 된다). */
  @Test
  void 영어와_한국어_파일_모두_키가_있다() throws IOException {
    for (String file : new String[] {"/uiMessage.properties", "/uiMessage_ko.properties"}) {
      Properties properties = new Properties();
      try (InputStream in = getClass().getResourceAsStream(file)) {
        assertThat(in).as(file).isNotNull();
        properties.load(in);
      }
      for (Basis basis : Basis.values()) {
        assertThat(properties.getProperty("stock.pick.basis." + basis.param()))
            .as(file + " " + basis + " 이름")
            .isNotBlank();
        assertThat(properties.getProperty("stock.pick.basis." + basis.param() + ".desc"))
            .as(file + " " + basis + " 설명")
            .isNotBlank();
      }
    }
  }
}
