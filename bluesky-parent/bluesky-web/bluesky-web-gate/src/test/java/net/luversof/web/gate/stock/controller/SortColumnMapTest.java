package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockSortUtil;

/**
 * 서버 정렬 목록(배당 · 매매)의 열 지도와 템플릿 정렬 머리칸이 같은 열을 말한다.
 *
 * <p>머리칸이 지도에 없는 열을 보내면 누를 때마다 400(정렬 인자 오류)이 나고, 지도에만 있는 열은 누를 곳이 없다. 2026-10-22 까지 비교기는 switch 의
 * case 로 적혀 있어 이 대조를 할 수 없었다.
 */
class SortColumnMapTest {

  private static final Pattern FIELD =
      Pattern.compile("components\\.sortHeader\\(\\s*field\\s*=\\s*\"(\\w+)\"");

  private static Set<String> headerFields(String template) throws IOException {
    String text =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/" + template), StandardCharsets.UTF_8);
    Set<String> fields = new LinkedHashSet<>();
    Matcher m = FIELD.matcher(text);
    while (m.find()) {
      fields.add(m.group(1));
    }
    return fields;
  }

  @Test
  void 배당_목록의_머리칸과_정렬_지도가_같은_열이다() throws IOException {
    Set<String> fields = headerFields("dividend/dividendTable.jte");
    assertThat(fields).as("머리칸을 하나도 못 찾았다").hasSizeGreaterThan(3);
    assertThat(StockDividendHtmxController.DIVIDEND_SORTS.keySet()).isEqualTo(fields);
  }

  @Test
  void 매매_목록의_머리칸과_정렬_지도가_같은_열이다() throws IOException {
    Set<String> fields = headerFields("trade/tradeDetailList.jte");
    assertThat(fields).as("머리칸을 하나도 못 찾았다").hasSizeGreaterThan(3);
    assertThat(StockTradeHtmxController.TRADE_SORTS.keySet()).isEqualTo(fields);
  }

  /** 열 비교기는 오름차순이고 값이 없는 줄은 뒤로 간다(내림차순은 부르는 쪽이 뒤집는다 - 그때 값 없는 줄은 앞으로 온다, 예전 그대로). */
  @Test
  void 열_비교기는_오름차순이고_값이_없으면_뒤로() {
    List<String> values = new ArrayList<>(Arrays.asList("b", null, "a", "c"));
    values.sort(StockSortUtil.ascendingNullsLast(s -> s));
    assertThat(values).containsExactly("a", "b", "c", null);
  }
}
