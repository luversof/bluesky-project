package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
 * 월배당 시뮬레이터의 "스냅샷 이후 보유 수량이 바뀐 종목 N개" 경고.
 *
 * <p>이 경고는 <b>한 번도 화면에 나온 적이 없었다</b>(2026-09-14 발견). 컨트롤러는 {@code
 * monthlyDividendStaleQuantityCount} 와 {@code monthlyDividendCurrentQuantityTotal} 을 늘 모델에 넣었고 조각도
 * 그 값을 받아 쓰게 돼 있었는데, 그 사이의 {@code simulator.jte} 가 조각 호출 인자에 두 값을 빼먹어 조각이 기본값 0 을 썼다. 같은 경고가 배당
 * 캘린더에서는 정상 동작했다(그쪽은 페이지 템플릿 파라미터라 모델이 그대로 바인딩된다).
 *
 * <p>스냅샷 수량은 사람이 갱신하는 값이라 원장과 어긋난다 &mdash; 실측 2026-08-23: 8 종목 중 7 종목이 어긋나 예상 월배당이 1.66% 낮았다. 그 사실을
 * 안 밝히면 화면의 숫자가 "지금 보유 기준" 으로 읽힌다.
 *
 * <p>페이지를 통째로 그려서 본다. 조각만 그리면 <b>바로 이 전달 누락</b>을 재현하지 못한다.
 */
class SimulatorStaleQuantityWarningTest {

  private static final String PAGE = "stock/simulator.jte";

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

  private String render(long staleCount, String currentTotal) {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("simulatorTab", "monthly-dividend");
    model.put("monthlyDividendStaleQuantityCount", staleCount);
    model.put("monthlyDividendCurrentQuantityTotal", new BigDecimal(currentTotal));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(PAGE, model, output);
    return output.toString();
  }

  @Test
  void 수량이_어긋나면_경고와_현재_수량_기준_합계를_적는다() {
    String html = render(7L, "3105136");

    // 숫자 하나만 찾으면 화면 어딘가의 7 에 걸려 늘 통과한다 - 문구 전체로 본다.
    String expected =
        java.text.MessageFormat.format(
            MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
            "7",
            // 2026-09-29: 세 곳의 표기를 로케일을 따르는 fullKrw 하나로 맞췄다(예전 여기만 "₩3,105,136").
            net.luversof.web.gate.stock.util.StockFormatUtil.fullKrw(3105136L));

    assertThat(html).as("전달이 빠지면 조각이 기본값 0 을 써서 경고가 통째로 사라진다").contains(expected);
  }

  @Test
  void 어긋난_것이_없으면_경고를_적지_않는다() {
    String html = render(0L, "0");

    String expected =
        java.text.MessageFormat.format(
            MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
            "0",
            net.luversof.web.gate.stock.util.StockFormatUtil.fullKrw(0L));

    assertThat(html).as("어긋난 종목이 없는데 경고를 띄우면 늘 켜져 있는 경고가 된다").doesNotContain(expected);
  }
}
