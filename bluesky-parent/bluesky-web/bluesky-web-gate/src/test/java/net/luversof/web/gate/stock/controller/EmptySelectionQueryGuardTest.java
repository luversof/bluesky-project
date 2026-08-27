package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;

/**
 * 고른 계좌가 하나도 유효하지 않을 때(= 좁힌 결과가 빈 목록) 조회를 보내지 않는지 본다.
 *
 * <p>빈 목록은 {@code toParams()} 에서 파라미터가 <b>통째로 빠진다</b>. 백엔드는 파라미터가 없으면 필터가 없다고 보고 전체를 돌려준다(실측: 계좌
 * 파라미터 없이 {@code /api/dividend/total} 이 전 기간 합계를, 배당 193 건 · 거래 250 건을 그대로 돌려준다). 그래서 "해당 없음" 이
 * "전부" 로 뒤집힌다.
 *
 * <p>요약 화면에서 실제로 그랬다 &mdash; 손익과 추이는 건너뛰는데 배당 합계만 그대로 나가, 실현손익 0 옆에 전 기간 배당 합계가 찍혔다. 같은 어긋남으로 예전에 한
 * 계좌 자리에 그 6.1 배인 전 계좌 합계가 나온 적이 있고, 그때 고친 것과 다른 문이었다.
 */
class EmptySelectionQueryGuardTest {

  private static final Path SUMMARY_CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java");

  /** 빈 목록이 필터를 지운다는 사실 자체를 고정한다. 이게 성립하지 않으면 아래 가드는 필요 없다. */
  @Test
  void 빈_목록은_파라미터를_지운다() {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(UUID.randomUUID());
    request.setAccountIdList(List.of());

    assertThat(request.toParams().get("accountIdList"))
        .as("빈 목록이면 파라미터가 빠져 백엔드가 '필터 없음'(= 전체)으로 읽는다")
        .isNull();

    request.setAccountIdList(List.of(UUID.randomUUID()));
    assertThat(request.toParams().get("accountIdList")).hasSize(1);
  }

  /** 날짜·타임존은 목록과 무관하게 그대로 실린다(가드가 다른 조건까지 지우면 안 된다). */
  @Test
  void 빈_목록이어도_기간과_타임존은_남는다() {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(UUID.randomUUID());
    request.setAccountIdList(List.of());
    request.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));
    request.setTimeZone("Asia/Seoul");

    assertThat(request.toParams().keySet())
        .containsExactlyInAnyOrder("userId", "startDate", "timeZone");
  }

  /**
   * 요약 화면에서 필터가 실린 파라미터를 쓰는 원격 호출은 모두 빈 선택 가드 안에 있어야 한다.
   *
   * <p>본문에 있는 조건이라 리플렉션으로는 보이지 않아 소스를 읽는다.
   */
  @Test
  void 요약화면의_필터_조회는_모두_빈선택_가드_안에_있다() throws IOException {
    String source = Files.readString(SUMMARY_CONTROLLER, StandardCharsets.UTF_8);

    Pattern call = Pattern.compile("(\\w+Client)\\.(\\w+)\\(\\s*(profitParams|trendParams)\\s*\\)");
    Matcher matcher = call.matcher(source);
    List<String> unguarded = new ArrayList<>();
    int found = 0;
    while (matcher.find()) {
      found++;
      // 호출 앞쪽에서 가장 가까운 'var ... =' 부터 호출까지 사이에 가드가 있어야 한다.
      int assignment = source.lastIndexOf("var ", matcher.start());
      String context = source.substring(Math.max(0, assignment), matcher.start());
      if (!context.contains("emptyAccountSelection")) {
        unguarded.add(matcher.group(1) + "." + matcher.group(2) + "(" + matcher.group(3) + ")");
      }
    }

    // 정규식이 조용히 0건을 반환하면 검사가 무력해진다(현재 3건).
    assertThat(found).as("필터 파라미터를 쓰는 원격 호출을 찾지 못했다 — 정규식이 낡았다").isGreaterThanOrEqualTo(3);
    assertThat(unguarded)
        .as("빈 계좌 선택이면 이 조회는 필터 없이 나가 전체 값을 받는다." + " emptyAccountSelection 일 때 건너뛰고 0 으로 처리할 것")
        .isEmpty();
  }
}
