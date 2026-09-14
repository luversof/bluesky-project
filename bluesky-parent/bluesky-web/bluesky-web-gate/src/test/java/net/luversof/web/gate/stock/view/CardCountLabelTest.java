package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 카드 오른쪽 끝에 붙는 건수는 축을 라벨로 밝힌다.
 *
 * <p>{@code <span class="ml-auto">} 자리는 같은 줄 왼쪽 끝에서 밀려난 자리라, 라벨이 없으면 <b>바로 왼쪽 항목의 건수</b>로 읽힌다.
 *
 * <p>실측 2026-09-12('올해' 2026-01-01~09-12, api-stock {@code periodSummary} 대조)
 *
 * <ul>
 *   <li>배당 카드: {@code 세전 ₩28,580,530 차감 -₩1,418,030 116건} &mdash; 116 은 배당 건수인데 왼쪽이 "차감" 이었다. 같은 카드
 *       아랫줄은 월중 56건 &middot; 월말 57건 &middot; 기타 3건 으로 모두 라벨이 있고 합이 116 이다.
 *   <li>매매 카드(2026-09-12 에 먼저 고침): {@code 매도 ₩346,574,445 98건} &mdash; 98 은 매수 91 + 매도 7 인데 옆 카드는
 *       같은 화면에서 "매도 7건" 이라고 적었다.
 * </ul>
 *
 * <p>활동 화면은 두 수량 모두에 이미 {@code stock.activity.label.trade} / {@code stock.activity.label.dividend}
 * 를 쓰고 있었다. 같은 라벨을 쓴다.
 */
class CardCountLabelTest {

  @Test
  void 배당_요약_카드의_건수에_라벨이_붙는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendSummaryCards.jte"),
            StandardCharsets.UTF_8);
    assertThat(flatten(template))
        .as("라벨이 없으면 왼쪽의 차감 건수로 읽힌다")
        .contains("${dividendCountLabel} ${totalItemsCountText}");
    assertThat(template)
        .as("활동 화면과 같은 라벨을 쓴다")
        .contains("MessageUtil.getMessage(\"stock.activity.label.dividend\")");
  }

  /**
   * 라벨 없는 {@code ml-auto} 건수가 다시 생기지 않게 한다.
   *
   * <p>세 곳이 이 모양이었다 &mdash; 매매 요약 카드 &middot; 자산 성장의 매매 이력 패널 &middot; 배당 요약 카드.
   */
  @Test
  void 라벨_없는_ml_auto_건수가_없다() throws IOException {
    List<String> offenders = new ArrayList<>();
    Path root = Path.of("src/main/jte/stock");
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String flat = flatten(Files.readString(file, StandardCharsets.UTF_8));
        for (String bad :
            new String[] {
              "<span class=\"ml-auto\">${countMessage.apply(",
              "<span class=\"ml-auto\">${totalItemsCountText}"
            }) {
          if (flat.contains(bad)) {
            offenders.add(file + " :: " + bad);
          }
        }
      }
    }
    assertThat(offenders).as("건수 앞에 축 라벨을 적을 것").isEmpty();
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
