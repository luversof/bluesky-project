package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import net.luversof.web.gate.stock.domain.Account;

/**
 * 계좌 설정에 넣은 '수동 원금' 을 읽는 규칙을 고정한다.
 *
 * <p>이 값은 자산현황의 수익률 <b>기준</b>이 된다. 실측(사용자 계좌 7개): 3개가 {@code manualPrincipalAmount} 를 갖고 있고 (ISA
 * 60,000,000 · 연금저축1 12,000,000 · 연금저축2 18,000,000), 이 값을 쓰면 수익률이 계산 원가 기준과 크게 달라진다 (연금저축1: -9.85%
 * -> +13.80%). 조용히 무시되면 사용자가 의도한 기준이 사라진다.
 */
class AccountManualPrincipalTest {

  private static final class TestController extends StockBaseHtmxController {
    private TestController() {
      super(null, null, null, null, null, new StaticMessageSource());
    }
  }

  private final TestController controller = new TestController();

  private static Account account(Map<String, Object> config) {
    return new Account(UUID.randomUUID(), UUID.randomUUID(), "계좌", null, config);
  }

  @Test
  void 설정이_없으면_null이다() {
    assertThat(controller.resolveAccountManualPrincipal(null)).isNull();
    assertThat(controller.resolveAccountManualPrincipal(account(null))).isNull();
    assertThat(controller.resolveAccountManualPrincipal(account(Map.of()))).isNull();
  }

  /** 실측 데이터와 같은 형태: JSON 숫자로 들어온다. */
  @Test
  void 숫자로_들어온_값을_읽는다() {
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", 60000000))))
        .isEqualByComparingTo("60000000");
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", 12000000L))))
        .isEqualByComparingTo("12000000");
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", 1234.5d))))
        .isEqualByComparingTo("1234.5");
  }

  /** 화면에서 콤마를 넣어 저장할 수 있으므로 문자열도 받는다. */
  @Test
  void 문자열과_콤마_표기도_읽는다() {
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", "18,000,000"))))
        .isEqualByComparingTo("18000000");
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", "  1000  "))))
        .isEqualByComparingTo("1000");
  }

  @Test
  void 숫자가_아니면_무시한다() {
    assertThat(
            controller.resolveAccountManualPrincipal(account(Map.of("manualPrincipalAmount", ""))))
        .isNull();
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", "abc"))))
        .isNull();
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", true))))
        .isNull();
  }

  /** 음수는 원금이 될 수 없다. 0 은 사용자가 의도적으로 둘 수 있어 받아들인다. */
  @Test
  void 음수는_받지_않고_0은_받는다() {
    assertThat(
            controller.resolveAccountManualPrincipal(
                account(Map.of("manualPrincipalAmount", -100))))
        .isNull();
    assertThat(
            controller.resolveAccountManualPrincipal(account(Map.of("manualPrincipalAmount", 0))))
        .isEqualByComparingTo("0");
  }

  /** 키는 선언 순서대로 본다. 앞의 키가 있으면 뒤의 키는 보지 않는다. */
  @Test
  void 키를_선언_순서대로_찾는다() {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("principal", 111);
    config.put("manualPrincipalAmount", 999);
    assertThat(controller.resolveAccountManualPrincipal(account(config)))
        .isEqualByComparingTo("999");

    // 앞 키가 숫자가 아니면 다음 키로 넘어간다.
    Map<String, Object> mixed = new LinkedHashMap<>();
    mixed.put("manualPrincipalAmount", "not-a-number");
    mixed.put("manualPrincipal", 555);
    assertThat(controller.resolveAccountManualPrincipal(account(mixed)))
        .isEqualByComparingTo("555");
  }

  /** 모르는 키는 무시한다(과세이연 플래그 등이 같은 맵에 들어 있다). */
  @Test
  void 모르는_키는_무시한다() {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("isTaxDeferred", true);
    config.put("someOtherAmount", 12345);
    assertThat(controller.resolveAccountManualPrincipal(account(config))).isNull();
  }
}
