package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 목록을 돌려주는 조회가 순서를 정해서 내보내는지 본다.
 *
 * <p>{@code Collectors.groupingBy} 는 {@code HashMap} 을 만든다. 그 {@code values()} 를 그대로 내보내면 같은 입력에서는
 * 재현되지만 뜻이 없는 순서가 되고, 계좌가 하나 늘기만 해도 전체가 뒤섞인다. 거래 목록에는 그 위험이 이미 적혀 있다 &mdash; 받는 쪽이 페이지로 자르면 같은 행이 두
 * 페이지에 나오거나 빠질 수 있다.
 *
 * <p>실측 2026-08-23: 이 서비스가 돌려주는 목록 중 보유 스냅샷·시계열·거래 목록·연도 요약은 모두 정렬하는데 손익 목록만 정렬하지 않았다. 값이 아니라
 * <b>정렬한다는 사실</b>을 고정한다.
 */
class TradeProfitOrderingTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/TradeProfitService.java");

  /** 메서드 본문(첫 '{' 부터 짝이 맞는 '}' 까지). */
  private String bodyOf(String source, String signature) {
    int at = source.indexOf(signature);
    assertThat(at).as(signature + " 를 찾지 못했다 - 이름이 바뀌었다").isGreaterThan(0);
    int open = source.indexOf('{', at);
    int depth = 1;
    int index = open + 1;
    while (index < source.length() && depth > 0) {
      char c = source.charAt(index++);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return source.substring(open, index);
  }

  @Test
  void 목록을_돌려주는_조회는_순서를_정한다() throws IOException {
    String source = Files.readString(SERVICE, StandardCharsets.UTF_8);

    List<String> unsorted = new ArrayList<>();
    for (String signature :
        List.of(
            "public List<TradeProfit> calculateProfitByAccountAndStock(",
            "public List<TradeResponse> getTradeHistory(")) {
      String body = bodyOf(source, signature);
      if (!body.contains(".sort(") && !body.contains(".sorted(")) {
        unsorted.add(signature);
      }
    }

    assertThat(unsorted).as("HashMap 순서를 그대로 내보내면 계좌가 하나 늘어도 전체가 뒤섞인다").isEmpty();
  }

  /** 손익 목록은 업무상 순위를 주장하지 않는 키로만 고정한다(화면이 각자 다시 정렬한다). */
  @Test
  void 손익_목록은_종목_계좌_순으로_고정한다() throws IOException {
    String body =
        bodyOf(
            Files.readString(SERVICE, StandardCharsets.UTF_8),
            "public List<TradeProfit> calculateProfitByAccountAndStock(");

    assertThat(body).contains("TradeProfit::getStockItemId").contains("TradeProfit::getAccountId");
  }

  /** 정렬 호출을 찾는 방식이 낡지 않았는지(다른 목록들도 여전히 정렬한다). */
  @Test
  void 다른_목록들도_여전히_정렬한다() throws IOException {
    String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
    Matcher matcher = Pattern.compile("result\\.sort\\(|\\.sorted\\(").matcher(source);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    assertThat(count).as("정렬 호출을 찾지 못했다 - 검사가 무력하다").isGreaterThanOrEqualTo(5);
  }
}
