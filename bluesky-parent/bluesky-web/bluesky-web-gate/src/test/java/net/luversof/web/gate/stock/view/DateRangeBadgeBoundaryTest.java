package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
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
 * 기간 배지가 실제 조회 구간의 마지막 날을 그대로 보여주는지 본다.
 *
 * <p>이 앱에서 {@code endDate} 는 <b>배타적</b>이다. 그래서 기간 내비바 컴포넌트는 받은 종료일에서 하루를 빼서 배지에 찍는다. 목록 화면
 * 4개(활동/매매/배당 이력/자산 성장)는 규칙대로 배타적 경계를 넘긴다.
 *
 * <p>그런데 계좌 상세·종목 상세가 쓰는 {@code detailDateFilter} 는 컨트롤러가 이미 {@code minusDays(1)} 로 <b>포함</b> 마지막
 * 날로 바꿔 둔 {@code filterEndLocal} 을 그대로 넘긴다. 내비바가 거기서 또 하루를 빼므로 배지가 하루 이르게 나온다 &mdash; 바로 아래 날짜 입력칸은
 * 옳은 날짜를 보여주므로, 같은 화면의 두 위젯이 하루씩 어긋난다.
 */
class DateRangeBadgeBoundaryTest {

  private static final String DETAIL_FILTER =
      "stock/htmx/fragments/components/detailDateFilter.jte";
  private static final String NAV_BAR = "stock/htmx/fragments/dateRangeNavBar.jte";

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

  private String renderDetailFilter(LocalDate startLocal, LocalDate endLocal) {
    Map<String, Object> params = new HashMap<>();
    params.put("pickerVar", "accountDetailPicker");
    params.put("formId", "accountDetailSearchForm");
    params.put("fragmentId", "accountDetailFragment");
    params.put("contentUrl", "/stock/account");
    params.put("idPrefix", "accountDetail");
    params.put("extraName", "accountId");
    params.put("extraValue", "01a0289d-8900-74b1-8d01-1e857fa3b2c6");
    params.put("startLocal", startLocal);
    params.put("endLocal", endLocal);
    params.put("startInstant", Instant.parse("2026-08-01T00:00:00Z"));
    params.put("endInstant", Instant.parse("2026-09-01T00:00:00Z"));
    params.put("timeZone", "Asia/Seoul");
    params.put("rangeMode", "1");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(DETAIL_FILTER, params, output);
    return output.toString();
  }

  private String renderNavBar(LocalDate startDate, LocalDate endDate) {
    Map<String, Object> params = new HashMap<>();
    params.put("pickerName", "activityPicker");
    params.put("btnClass", "date-range-btn");
    params.put("canPrev", true);
    params.put("canNext", true);
    params.put("rangeMode", "1");
    params.put("startDate", startDate);
    params.put("endDate", endDate);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(NAV_BAR, params, output);
    return output.toString();
  }

  /** 배지 텍스트(공백 정리 후)만 뽑는다. */
  private String badge(String html) {
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("badge badge-ghost[^>]*>(.*?)</span>", 32).matcher(html);
    assertThat(matcher.find()).as("기간 배지를 찾지 못했다").isTrue();
    return matcher.group(1).replaceAll("\s+", " ").trim();
  }

  @Test
  void 목록_화면은_배타적_종료일을_넘기므로_배지가_마지막_날을_가리킨다() {
    // 활동/매매/배당 이력/자산 성장이 넘기는 값: 8월 한 달이면 endLocal = 9/1(배타적)
    assertThat(badge(renderNavBar(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-09-01"))))
        .isEqualTo("2026-08-01 ~ 2026-08-31");
  }

  @Test
  void 상세_화면_배지도_날짜_입력칸과_같은_마지막_날을_보여준다() {
    // 컨트롤러가 넘기는 filterEndLocal 은 이미 포함 마지막 날이다(8월 한 달이면 8/31).
    String html = renderDetailFilter(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"));

    assertThat(html)
        .as("날짜 입력칸은 포함 마지막 날을 그대로 보여준다")
        .contains("id=\"accountDetailEndDateInput\"")
        .contains("value=\"2026-08-31\"");
    assertThat(badge(html))
        .as("배지가 입력칸보다 하루 이르면 같은 화면의 두 위젯이 어긋난다")
        .isEqualTo("2026-08-01 ~ 2026-08-31");
  }

  @Test
  void 월초_하루짜리_구간도_같은_날짜로_표시된다() {
    String html = renderDetailFilter(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-01"));
    assertThat(badge(html)).isEqualTo("2026-08-01 ~ 2026-08-01");
  }
}
