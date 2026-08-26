package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.Account;

/**
 * 수수료 기록을 되받을 수 없는 계좌가 <b>그 사실을 실현손익 옆에서 밝히는지</b> 렌더해서 본다.
 *
 * <p>원장 점검(api-stock {@code ACCOUNT_WITHOUT_ANY_FEE})은 이런 계좌를 지적하지 않는다 &mdash; 폐쇄된 증권사 계좌라 원본 기록이
 * 사라져 영영 고칠 수 없고, 남겨 두면 점검 화면이 늘 빨간 상태가 되어 새로 생긴 문제를 가린다.
 *
 * <p>그런데 지적만 빼고 끝내면 <b>부풀려진 실현손익이 아무 표시 없이 정상값처럼</b> 보인다. 실측 2026-08-25(동양증권): 매매 12 건 전부 수수료·거래세가
 * 비어 있어, 빠진 수수료 최소 25,011 원 + 거래세 최소 462,827 원 만큼 그 계좌 실현손익이 크게 잡혀 있다. 그래서 값이 어긋나는 자리에서 밝힌다.
 */
class FeeRecordsUnavailableNoticeTest {

  private static final String TEMPLATE = "stock/accountDetail.jte";
  private static final String KEY = "stock.account.detail.fee.records.unavailable";

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

  private String render(Map<String, Object> jsonConfig) {
    Map<String, Object> model = new HashMap<>();
    model.put("contentReady", true);
    model.put(
        "account", new Account(UUID.randomUUID(), UUID.randomUUID(), "동양증권", null, jsonConfig));

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 기록을_되받을_수_없는_계좌면_실현손익_위에서_밝힌다() {
    String html = render(Map.of("feeRecordsUnavailable", Boolean.TRUE));

    String notice = MessageUtil.getMessage(KEY);
    assertThat(notice).as("문구가 없으면 검사가 무력해진다").doesNotStartWith("stock.");
    assertThat(html).as("부풀려진 실현손익이 아무 표시 없이 정상값처럼 보인다").contains(notice);
    assertThat(html.indexOf(notice))
        .as("실현손익보다 아래에 있으면 숫자를 먼저 읽고 만다")
        .isLessThan(html.indexOf(MessageUtil.getMessage("stock.profit.realized")));
  }

  @Test
  void 보통_계좌에는_그리지_않는다() {
    assertThat(render(null))
        .as("모든 계좌에 붙으면 경고가 무의미해진다")
        .doesNotContain(MessageUtil.getMessage(KEY));
    assertThat(render(Map.of("isTaxDeferred", Boolean.TRUE)))
        .as("다른 설정 키에 반응하면 안 된다")
        .doesNotContain(MessageUtil.getMessage(KEY));
  }
}
