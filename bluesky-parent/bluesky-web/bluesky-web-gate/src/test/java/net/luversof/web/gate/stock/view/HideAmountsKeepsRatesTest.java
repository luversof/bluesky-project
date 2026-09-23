package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.AssetStatusAccountHoldingView;
import net.luversof.web.gate.stock.dto.response.AssetStatusStockAccountView;

/**
 * 금액 숨김을 켜도 수익률(%)은 보여야 한다 &mdash; 사용자 요청 2026-09-23.
 *
 * <p>금액 숨김은 {@code html.hide-amounts .amount-value} 에 흐림을 건다. 흐림은 자식에게 그대로 물려지므로, 금액과 수익률을 한 칸에
 * 적으면서 {@code amount-value} 를 칸({@code td}) 전체에 붙이면 수익률까지 흐려진다. 실측: 계좌 보유 상세 18 개 · 종목 보유 계좌 18 개가
 * 그랬다(그 위의 계좌 줄 · 종목 줄은 금액만 {@code span} 으로 감싸 수익률이 보였다).
 *
 * <p>소스를 읽는 가드는 모양을 조금만 바꿔 적어도 빠져나가므로, 조각을 <b>실제로 그려서</b> 수익률 글자의 조상에 {@code amount-value} 가 있는지
 * 태그를 거슬러 올라가며 본다.
 */
class HideAmountsKeepsRatesTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetStatus.jte";

  private static final UUID STOCK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

  /** 태그를 닫지 않는 요소. 스택에 올리면 짝이 안 맞아 판정이 어긋난다. */
  private static final Set<String> VOID =
      Set.of("br", "img", "input", "meta", "link", "hr", "col", "source", "wbr");

  private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>");

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

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 계좌 하나 · 종목 하나를 서로 펼칠 수 있게 그린다. 수익률은 일부러 서로 다른 숫자로 둔다. */
  private String render() {
    Map<UUID, TradeProfit> accounts = new LinkedHashMap<>();
    accounts.put(
        ACCOUNT,
        TradeProfit.ofAccountStatus(
            "위탁", bd("1150000"), bd("150000"), bd("0"), bd("1000000"), bd("0")));

    Map<UUID, List<AssetStatusAccountHoldingView>> holdings = new LinkedHashMap<>();
    holdings.put(
        ACCOUNT,
        List.of(
            new AssetStatusAccountHoldingView(
                STOCK,
                "테스트종목",
                100,
                bd("10000"),
                bd("11500"),
                bd("1150000"),
                bd("1000000"),
                bd("150000"),
                bd("12.3"),
                bd("100"),
                bd("100"))));

    TradeProfit stock =
        TradeProfit.ofStockStatus(
            STOCK,
            "테스트종목",
            bd("10000"),
            100,
            bd("11500"),
            bd("1150000"),
            bd("150000"),
            bd("0"),
            bd("1000000"));

    Map<UUID, List<AssetStatusStockAccountView>> stockAccounts = new LinkedHashMap<>();
    stockAccounts.put(
        STOCK,
        List.of(
            new AssetStatusStockAccountView(
                ACCOUNT,
                "위탁",
                100,
                bd("10000"),
                bd("1150000"),
                bd("1000000"),
                bd("-45000"),
                bd("-4.5"),
                bd("100"),
                bd("100"))));

    Map<String, Object> model = new HashMap<>();
    model.put("accountTotalMap", accounts);
    model.put("accountProfitBasisMap", new LinkedHashMap<UUID, BigDecimal>());
    model.put("manualPrincipalAccountIds", Set.of());
    model.put("accountHoldingMap", holdings);
    model.put("stockItemList", List.of());
    model.put("stockAggregated", List.of(stock));
    model.put("stockHoldingAccountMap", stockAccounts);
    model.put("totalEvaluationAmount", bd("1150000"));
    model.put("totalEvaluationProfit", bd("150000"));
    model.put("priceBasisDate", LocalDate.parse("2026-09-23"));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /**
   * 글자 조각마다 "흐려지는 조상이 있는가" 를 돌려준다.
   *
   * @param html 그린 조각
   * @param needle 찾을 글자
   * @return 그 글자가 나온 자리마다 true(흐려짐) / false(보임). 못 찾으면 빈 목록
   */
  private static List<Boolean> blurredAt(String html, String needle) {
    List<Boolean> found = new ArrayList<>();
    Deque<Boolean> stack = new ArrayDeque<>();
    Matcher tag = TAG.matcher(html);
    int textFrom = 0;
    while (tag.find()) {
      String text = html.substring(textFrom, tag.start());
      if (text.contains(needle)) {
        found.add(stack.contains(Boolean.TRUE));
      }
      textFrom = tag.end();
      String name = tag.group(2).toLowerCase();
      boolean closing = !tag.group(1).isEmpty();
      if (VOID.contains(name) || tag.group(3).endsWith("/")) {
        continue;
      }
      if (closing) {
        if (!stack.isEmpty()) {
          stack.pop();
        }
      } else {
        stack.push(hasAmountValue(tag.group(3)));
      }
    }
    return found;
  }

  /** 표시(data-*) 가 달린 요소가 흐려지는가 - 자기 자신이나 조상에 amount-value 가 있으면 흐려진다. */
  private static boolean blurredMarker(String html, String marker) {
    Deque<Boolean> stack = new ArrayDeque<>();
    Matcher tag = TAG.matcher(html);
    while (tag.find()) {
      String name = tag.group(2).toLowerCase();
      boolean closing = !tag.group(1).isEmpty();
      if (!closing && tag.group(3).contains(marker)) {
        return stack.contains(Boolean.TRUE) || hasAmountValue(tag.group(3));
      }
      if (VOID.contains(name) || tag.group(3).endsWith("/")) {
        continue;
      }
      if (closing) {
        if (!stack.isEmpty()) {
          stack.pop();
        }
      } else {
        stack.push(hasAmountValue(tag.group(3)));
      }
    }
    throw new AssertionError(marker + " 를 단 요소를 못 찾았다 - 검사가 헛돈다");
  }

  private static boolean hasAmountValue(String attributes) {
    Matcher cls = Pattern.compile("class=\"([^\"]*)\"").matcher(attributes);
    if (!cls.find()) {
      return false;
    }
    for (String token : cls.group(1).trim().split("\\s+")) {
      if (token.equals("amount-value")) {
        return true;
      }
    }
    return false;
  }

  @Test
  void 계좌_보유_상세의_평가_손익률은_가리지_않는다() {
    List<Boolean> rate = blurredAt(render(), "+12.3%");

    assertThat(rate).as("계좌 보유 상세의 평가 손익률을 못 찾았다 - 검사가 헛돈다").isNotEmpty();
    assertThat(rate).as("금액 숨김에 수익률까지 흐려진다(amount-value 가 칸 전체에 붙어 있다)").containsOnly(false);
  }

  @Test
  void 종목_보유_계좌_상세의_평가_손익률도_가리지_않는다() {
    List<Boolean> rate = blurredAt(render(), "-4.5%");

    assertThat(rate).as("종목 보유 계좌 상세의 평가 손익률을 못 찾았다 - 검사가 헛돈다").isNotEmpty();
    assertThat(rate).as("금액 숨김에 수익률까지 흐려진다").containsOnly(false);
  }

  /** 선택 합산 카드의 비중도 비율이다 - 계좌 쪽은 원래 보였고 종목 쪽만 가렸다(사용자 결정 2026-09-23). */
  @Test
  void 선택_합산의_비중은_가리지_않는다() {
    String html = render();

    assertThat(blurredMarker(html, "data-selection-weight")).as("종목 선택 합산의 비중").isFalse();
    assertThat(blurredMarker(html, "data-account-selection-weight")).as("계좌 선택 합산의 비중").isFalse();
    assertThat(blurredMarker(html, "data-selection-evaluation-profit"))
        .as("비중을 꺼내면서 같은 카드의 금액까지 꺼내면 안 된다")
        .isTrue();
  }

  @Test
  void 금액은_여전히_가린다() {
    // 수익률을 꺼내면서 금액까지 꺼내면 숨김이 무너진다 - 두 상세의 평가 손익 금액이 가려지는지 함께 본다.
    String html = render();

    assertThat(blurredAt(html, "-45,000"))
        .as("종목 보유 계좌 상세의 평가 손익 금액")
        .isNotEmpty()
        .containsOnly(true);
    assertThat(blurredAt(html, "+150,000"))
        .as("계좌 보유 상세의 평가 손익 금액(계좌 줄과 종목 줄에도 같은 금액이 나온다 - 모두 가려져야 한다)")
        .isNotEmpty()
        .containsOnly(true);
  }
}
