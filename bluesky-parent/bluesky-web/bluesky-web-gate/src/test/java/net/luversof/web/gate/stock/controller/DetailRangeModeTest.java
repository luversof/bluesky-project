package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockRangePresetUtil;

/**
 * 상세 화면(종목·계좌)도 기간 프리셋을 해석해야 한다.
 *
 * <p>실측 2026-09-11: 상세 컨트롤러는 {@code rangeMode} 를 모델에 넘기기만 하고 기간으로 바꾸지 않았다. 저장된 기간이 없는 상태에서 {@code
 * /stock/item?stockItemId=..&rangeMode=ytd} 로 들어가면 배지가 "전체 · 2020-03-23 ~ 2026-09-11", 실현손익은 전 기간
 * 값인 +138,569,333(올해는 +119,289,504)이었고 프리셋 버튼도 하나도 눌린 상태가 아니었다.
 *
 * <p>화면 쪽에도 같은 원인이 있었다. 상세 셸의 조각 요청이 전역 기간(hx-include)만 싣고 URL 쿼리는 싣지 않아, 저장된 기간이 1개월이면 {@code
 * ?rangeMode=ytd} 를 줘도 1개월이 그대로 나왔다.
 */
class DetailRangeModeTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 상세_컨트롤러가_프리셋을_기간으로_바꾼다() throws IOException {
    String controller =
        read("src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java");

    assertThat(controller)
        .as("두 상세 핸들러(/item, /account)가 모두 기간을 확정해야 한다")
        .containsOnlyOnce("private java.time.Instant[] resolveRange(");
    assertThat(
            count(
                controller,
                "resolvedRange = resolveRange(rangeMode, startDate, endDate, timeZone);"))
        .as("/item 과 /account 두 곳에서 불러야 한다")
        .isEqualTo(2);
  }

  @Test
  void 상세_셸이_URL_기간을_조각에_싣는다() throws IOException {
    String item = read("src/main/jte/stock/stockItemDetail.jte");
    String account = read("src/main/jte/stock/accountDetail.jte");

    assertThat(item).contains("<div id=\"stockItemDetailFragment\" data-params-from-query");
    assertThat(account).contains("<div id=\"accountDetailFragment\" data-params-from-query");
    // id 를 hx-get 경로에 두면 URL 의 같은 키가 한 번 더 붙어 "id,id" 로 묶인다(실측: 상세가 '찾을 수 없습니다').
    assertThat(item).as("id 는 경로가 아니라 hx-vals 로").contains("hx-get=\"/stock/item\"");
    assertThat(item).contains("hx-vals='{\"stockItemId\":");
    assertThat(account).as("id 는 경로가 아니라 hx-vals 로").contains("hx-get=\"/stock/account\"");
    assertThat(account).contains("hx-vals='{\"accountId\":");
  }

  @Test
  void 프리셋_규칙은_한_벌이다() throws IOException {
    String base =
        read("src/main/java/net/luversof/web/gate/stock/controller/StockBaseHtmxController.java");

    assertThat(base)
        .as("목록 화면도 같은 유틸을 써야 한다 - 두 벌이면 같은 버튼이 화면마다 다른 구간을 뜻하게 된다")
        .contains("StockRangePresetUtil.resolve(rangeMode, zone)");
    assertThat(base)
        .as("계산이 컨트롤러에 남아 있으면 안 된다")
        .doesNotContain("today.minusMonths(Long.parseLong(mode)).plusDays(1)");
  }

  @Test
  void 올해는_1월_1일부터_오늘까지다() {
    LocalDate today = LocalDate.now(KST);
    var preset = StockRangePresetUtil.resolve("ytd", KST);

    assertThat(preset.mode()).isEqualTo("ytd");
    assertThat(preset.start())
        .isEqualTo(LocalDate.of(today.getYear(), 1, 1).atStartOfDay(KST).toInstant());
    assertThat(preset.end())
        .as("끝은 '오늘까지'를 뜻하는 배타적 경계다")
        .isEqualTo(today.plusDays(1).atStartOfDay(KST).toInstant());
  }

  @Test
  void N개월은_정확히_N개월이다() {
    LocalDate today = LocalDate.now(KST);
    var preset = StockRangePresetUtil.resolve("1", KST);

    Instant expected = today.minusMonths(1).plusDays(1).atStartOfDay(KST).toInstant();
    assertThat(preset.start()).as("양끝 포함이라 하루를 더해야 한 달이 된다").isEqualTo(expected);
    assertThat(preset.mode()).isEqualTo("1");
  }

  @Test
  void 전체와_빈값은_구분된다() {
    assertThat(StockRangePresetUtil.isAll("all")).isTrue();
    assertThat(StockRangePresetUtil.isAll("ALL")).isTrue();
    assertThat(StockRangePresetUtil.isAll("ytd")).isFalse();
    assertThat(StockRangePresetUtil.isAll(null)).isFalse();

    assertThat(StockRangePresetUtil.hasMode("ytd")).isTrue();
    assertThat(StockRangePresetUtil.hasMode("")).isFalse();
    assertThat(StockRangePresetUtil.hasMode("  ")).isFalse();
    assertThat(StockRangePresetUtil.hasMode(null)).isFalse();
  }

  @Test
  void 알_수_없는_값은_올해로_떨어진다() {
    assertThat(StockRangePresetUtil.resolve("0", KST).mode()).isEqualTo("ytd");
    assertThat(StockRangePresetUtil.resolve("garbage", KST).mode()).isEqualTo("ytd");
  }
}
