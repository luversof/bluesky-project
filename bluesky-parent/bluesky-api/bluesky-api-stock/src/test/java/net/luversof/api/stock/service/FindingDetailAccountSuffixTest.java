package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 원장 점검 예시 문구에 계좌명이 두 번 들어가지 않는지 본다.
 *
 * <p>{@code tradeRule} 은 예시를 만들 때 문장 끝에 {@code accountSuffix(...)} 를 <b>스스로 붙인다</b>. 그런데 규칙이 넘기는
 * detail 함수 안에서 또 붙이면 화면에 두 번 나온다. 화면은 {@code ${example.date()} ${example.stockItemName()} ·
 * ${example.detail()}} 로 그대로 찍으므로 사용자가 그 중복을 본다.
 *
 * <p>실측 2026-08-24: {@code SELL_WITHOUT_FEE_AND_TAX} 가 그랬다 &mdash; <i>"매도금액=39380000, 수수료=0, 거래세=0
 * <b>[동양증권]</b> (같은 해 관측 거래세율 0.2500% 기준이면 98446원이 빠졌다) <b>[동양증권]</b>"</i>. 같은 {@code tradeRule} 을
 * 쓰는 다른 두 규칙({@code TRADE_ON_WEEKEND}, {@code TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN})은 한 번만 나왔다.
 *
 * <p>규칙이 늘 때 같은 실수가 되돌아오는 것을 막는다.
 */
class FindingDetailAccountSuffixTest {

  private static final Path SOURCE =
      Path.of("src/main/java/net/luversof/api/stock/service/LedgerIntegrityService.java");

  private static final String CALL = "tradeRule(";

  /** {@code tradeRule(...)} 한 번의 인자 전체를 괄호 짝을 맞춰 잘라 낸다. */
  private String argumentsOf(String source, int callAt) {
    int open = source.indexOf('(', callAt);
    int depth = 1;
    int at = open + 1;
    while (at < source.length() && depth > 0) {
      char c = source.charAt(at++);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      }
    }
    return source.substring(open + 1, Math.max(open + 1, at - 1));
  }

  private record Call(int line, String arguments) {}

  private List<Call> calls() throws IOException {
    String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
    List<Call> found = new ArrayList<>();
    int at = source.indexOf(CALL);
    while (at >= 0) {
      // 선언부(private void tradeRule(...))는 호출이 아니다.
      boolean declaration = source.lastIndexOf("void ", at) > source.lastIndexOf(";", at);
      if (!declaration) {
        found.add(
            new Call(
                source.substring(0, at).split("\n", -1).length,
                argumentsOf(source, at + CALL.length() - 1)));
      }
      at = source.indexOf(CALL, at + 1);
    }
    return found;
  }

  @Test
  void tradeRule_에_넘기는_문구는_계좌를_스스로_붙이지_않는다() throws IOException {
    List<String> offenders =
        calls().stream()
            .filter(call -> call.arguments().contains("accountSuffix("))
            .map(call -> "LedgerIntegrityService.java:" + call.line())
            .toList();

    assertThat(offenders)
        .as(
            "tradeRule 이 문장 끝에 계좌를 붙이므로 여기서 또 붙이면 화면에 두 번 나온다"
                + " (실측: \"… 거래세=0 [동양증권] (…) [동양증권]\")")
        .isEmpty();
  }

  /** 검사가 실제로 호출을 훑는지. 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_tradeRule_호출을_훑는다() throws IOException {
    // 실측 2026-08-24: 호출 6 곳.
    assertThat(calls()).as("tradeRule 호출을 하나도 찾지 못했다").hasSizeGreaterThanOrEqualTo(3);
  }

  /** 예시를 만드는 쪽이 실제로 계좌를 붙이는지. 그게 없어지면 위 검사는 반대로 해로워진다. */
  @Test
  void 예시를_만드는_쪽이_계좌를_붙인다() throws IOException {
    String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
    assertThat(source)
        .as("tradeRule 이 계좌를 붙이지 않게 됐다면 detail 쪽에서 붙여야 한다 - 이 검사를 뒤집을 것")
        .contains("detail.apply(hit) + accountSuffix(hit.getAccountId(), accountNames)");
  }
}
