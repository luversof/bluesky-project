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

  /**
   * 영어 감소 횟수는 1 과 나머지를 나눠 적는다(2026-10-08 영어 화면 판독: "0 of 11 cut" · "1 cuts" 가 어색했다). 한국어는 단 · 복수가
   * 없어 그대로.
   */
  @Test
  void 영어_감소_횟수는_단수와_복수를_나눈다() throws IOException {
    Properties en = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/uiMessage.properties")) {
      en.load(in);
    }
    String compare = en.getProperty("stock.pick.compare.value.steady");
    assertThat(java.text.MessageFormat.format(compare, 0, 11)).isEqualTo("0 cuts in 11 changes");
    assertThat(java.text.MessageFormat.format(compare, 1, 11)).isEqualTo("1 cut in 11 changes");
    assertThat(java.text.MessageFormat.format(compare, 3, 11)).isEqualTo("3 cuts in 11 changes");
    String unit = en.getProperty("stock.pick.basis.unit.steady");
    assertThat(java.text.MessageFormat.format(unit, 1)).isEqualTo("cut");
    assertThat(java.text.MessageFormat.format(unit, 2)).isEqualTo("cuts");
    String value = en.getProperty("stock.pick.basis.value.steady");
    assertThat(java.text.MessageFormat.format(value, 11, 1, "9.84"))
        .isEqualTo("1 cut in 11 payout changes · dividend score 9.84");
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
