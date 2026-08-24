package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * '지금 보유' 를 그리는 핸들러가 기간을 떨어내는지 본다.
 *
 * <p>api-stock 은 요청에 기간이 실리면 평가(현재가·평가금액·평가손익)를 <b>아예 계산하지 않는다</b>. 실측(같은 사용자 보유 18행): 기간이 없으면 평가액
 * 합이 1,493,281,835 인데 기간을 주면 <b>0</b> 이 된다. 그래서 보유 평가를 그리는 화면이 기간을 그대로 넘기면 "수량과 평단은 있는데 평가액이 0" 인
 * 모순된 표가 된다.
 *
 * <p>{@code TradeProfitRequest} 를 URL 에서 바인딩하는 핸들러는 화면이 기간을 안 보내더라도 엔드포인트를 직접 부르면 그대로 드러난다. 그래서 방어는
 * 핸들러 안에 있어야 하는데, 지금은 <b>관례</b>로만 지켜지고 있었다 &mdash; 포트폴리오/자산현황은 떨어내고 요약은 떨어내지 않았다.
 */
class HoldingEvaluationRangeGuardTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static final Pattern MAPPING =
      Pattern.compile("@(Get|Post)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"[^)]*\\))?");

  /** 바인딩된 요청을 그대로 쓰는 핸들러만 대상이다(요청을 직접 새로 만들면 기간이 실릴 수 없다). */
  private record Handler(String key, String body) {}

  private String methodBody(String source, int from) {
    int open = source.indexOf('{', from);
    if (open < 0) {
      return "";
    }
    int depth = 1;
    int i = open + 1;
    while (i < source.length() && depth > 0) {
      char c = source.charAt(i++);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return source.substring(open, i);
  }

  private List<Handler> handlersBindingRequest() throws IOException {
    List<Handler> handlers = new ArrayList<>();
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        String simpleName = file.getFileName().toString().replace(".java", "");
        Matcher matcher = MAPPING.matcher(source);
        while (matcher.find()) {
          String body = methodBody(source, matcher.end());
          // 시그니처에 TradeProfitRequest 를 받는 핸들러만: 그때만 URL 로 기간이 들어올 수 있다.
          String signature =
              source.substring(matcher.end(), source.indexOf('{', matcher.end()) + 1);
          if (!signature.contains("TradeProfitRequest ")) {
            continue;
          }
          handlers.add(
              new Handler(
                  simpleName + ":" + (matcher.group(2) == null ? "" : matcher.group(2)), body));
        }
      }
    }
    return handlers;
  }

  @Test
  void 보유_평가를_쓰는_핸들러는_기간을_떨어낸다() throws IOException {
    List<Handler> handlers = handlersBindingRequest();
    assertThat(handlers).as("TradeProfitRequest 를 바인딩하는 핸들러를 하나도 못 찾았다").isNotEmpty();

    List<String> unguarded =
        handlers.stream()
            .filter(
                h -> h.body().contains("evaluationAmount") || h.body().contains("evaluationProfit"))
            .filter(h -> !h.body().contains("setStartDate(null)"))
            .map(Handler::key)
            .toList();

    assertThat(unguarded)
        .as("기간이 실리면 평가액이 0 으로 나간다. 핸들러 안에서 setStartDate(null)/setEndDate(null) 로 떨어낼 것")
        .isEmpty();
  }

  /** 떨어낼 때는 시작·종료를 함께 지운다. 한쪽만 지우면 api-stock 은 여전히 기간으로 본다. */
  @Test
  void 기간을_떨어낼_때는_양끝을_함께_지운다() throws IOException {
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        long starts = source.split("setStartDate\\(null\\)", -1).length - 1;
        long ends = source.split("setEndDate\\(null\\)", -1).length - 1;
        assertThat(ends)
            .as(file.getFileName() + " 에서 setStartDate(null) 과 setEndDate(null) 의 개수가 다르다")
            .isEqualTo(starts);
      }
    }
  }
}
