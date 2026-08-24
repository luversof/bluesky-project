package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
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

/**
 * "필터 적용됨" 배지가 실제로 신호가 되는지 본다.
 *
 * <p>예전 조건은 <b>기간이 있으면 필터로 인정</b>이었다. 그런데 날짜가 안 실려 오면 컨트롤러가 프리셋으로 채운다({@code
 * StockBaseHtmxController.resolvePresetRange}, 알 수 없는 값은 YTD). 즉 startDate/endDate 는 사실상 항상 있고 배지는
 * 늘 켜져 있어 아무것도 알려 주지 못했다.
 *
 * <p>더구나 신호가 뒤집혀 있었다 &mdash; 기간을 "전체"로 두면 날짜가 둘 다 null 이 되어 배지가 <b>꺼졌다</b>. 필터를 가장 적게 건 상태에서만 꺼지는
 * 표시였다.
 *
 * <p>기간은 바로 위 기간 내비바가 "시작 ~ 끝"으로 늘 보여 주므로, 이 배지는 계좌/종목/태그 선택만 본다.
 */
class FilterBadgeSignalTest {

  private static final String TEMPLATE = "stock/htmx/fragments/components/filterBadge.jte";
  private static final UUID ACCOUNT_ID = UUID.fromString("01a0289d-8900-74b1-8d01-1e857fa3b2c6");
  private static final UUID STOCK_ITEM_ID = UUID.fromString("019d271d-ca42-7ad6-bd37-29cc9f7a0eef");

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

  private boolean badgeShown(Map<String, Object> params) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString().contains("badge-primary");
  }

  @Test
  void 아무것도_고르지_않으면_배지가_꺼진다() {
    assertThat(badgeShown(new HashMap<>())).isFalse();
  }

  @Test
  void 계좌를_고르면_배지가_켜진다() {
    Map<String, Object> params = new HashMap<>();
    params.put("selectedAccountIds", List.of(ACCOUNT_ID));
    assertThat(badgeShown(params)).isTrue();
  }

  @Test
  void 종목을_고르면_배지가_켜진다() {
    Map<String, Object> params = new HashMap<>();
    params.put("selectedStockItemIds", List.of(STOCK_ITEM_ID));
    assertThat(badgeShown(params)).isTrue();
  }

  @Test
  void 태그를_고르면_배지가_켜진다() {
    Map<String, Object> params = new HashMap<>();
    params.put("selectedStockTags", List.of("배당"));
    assertThat(badgeShown(params)).isTrue();
  }

  @Test
  void 단수_선택값만_와도_배지가_켜진다() {
    // 매매·배당 화면은 목록의 첫 값을 단수로도 함께 넘긴다.
    Map<String, Object> params = new HashMap<>();
    params.put("selectedAccountId", ACCOUNT_ID);
    assertThat(badgeShown(params)).isTrue();

    Map<String, Object> stockOnly = new HashMap<>();
    stockOnly.put("selectedStockItemId", STOCK_ITEM_ID);
    assertThat(badgeShown(stockOnly)).isTrue();
  }

  @Test
  void 빈_선택_목록은_필터가_아니다() {
    Map<String, Object> params = new HashMap<>();
    params.put("selectedAccountIds", List.of());
    params.put("selectedStockItemIds", List.of());
    params.put("selectedStockTags", List.of());
    assertThat(badgeShown(params)).isFalse();
  }

  @Test
  void 직접_준_배지_문구는_그대로_쓰인다() {
    Map<String, Object> params = new HashMap<>();
    params.put("badgeText", "3건 선택");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    assertThat(output.toString()).contains("3건 선택");
  }
}
