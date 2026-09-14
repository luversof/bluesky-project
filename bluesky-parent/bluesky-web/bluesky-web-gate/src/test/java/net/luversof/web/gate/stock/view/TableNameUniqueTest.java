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
 * 한 화면에 같은 이름의 표가 여럿이면 안 된다.
 *
 * <p>실측 2026-09-12(자산 현황 /stock/analytics, 계좌 상세를 모두 펼친 상태): 보이는 표 7개 중 <b>다섯 개</b>가 "계좌 보유 종목 상세"
 * 라는 같은 이름이었다. 화면으로 보면 각각 어느 계좌의 것인지 위치로 알 수 있지만, 표 목록으로 건너뛰는 사람에게는 다섯 줄이 모두 같은 글자다 &mdash; 대시보드의
 * "실현 손익" 카드 두 장과 같은 결함이다.
 *
 * <p>다른 표 이름들(계좌별 현황·종목별 현황·상세 목록 …)은 실측상 이미 화면마다 유일했다.
 */
class TableNameUniqueTest {

  /** 계좌마다 펼쳐지는 표는 계좌 이름을 달고 나온다. */
  @Test
  void 계좌_보유_종목_상세는_계좌_이름을_단다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte"), StandardCharsets.UTF_8);

    String label = attributeAfter(template, "<table class=\"table table-xs w-full\" aria-label=");
    assertThat(label).as("계좌 보유 종목 상세 표의 aria-label 을 찾지 못했다").isNotNull();
    assertThat(label)
        .as("이름이 고정 문구면 계좌가 다섯이어도 이름이 하나다")
        .contains("accountHoldingsDetailNamedPattern");
    assertThat(template)
        .as("이름 틀이 그 메시지 키에 묶여 있어야 한다")
        .contains(
            "accountHoldingsDetailNamedPattern ="
                + " MessageUtil.getMessage(\"stock.analytics.account.holdings.detail.named\")");
    assertThat(label).as("계좌 이름이 들어가야 한다").contains("accountName()");
    // 이름을 못 읽는 계좌가 있어도 표는 이름을 가져야 한다.
    assertThat(label).contains("accountHoldingsDetailLabel");
  }

  /** 문구는 두 언어 모두에 있어야 하고 {0} 자리를 가져야 한다. */
  @Test
  void 문구는_두_언어에_다_있고_자리를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      List<String> lines =
          Files.readAllLines(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      String line =
          lines.stream()
              .filter(l -> l.startsWith("stock.analytics.account.holdings.detail.named"))
              .findFirst()
              .orElse(null);
      assertThat(line).as(bundle + " 에 문구가 없다").isNotNull();
      assertThat(line).as(bundle + " 문구에 {0} 자리가 없다 - 계좌 이름이 사라진다").contains("{0}");
    }
  }

  /**
   * 주식 화면 템플릿 전체에서 같은 고정 문구 키로 이름 붙인 표가 둘 이상이면 안 된다.
   *
   * <p>한 파일 안에서 같은 메시지 키를 두 번 이상 {@code aria-label} 로 쓰면, 그 표들은 렌더된 뒤 구분되지 않는다.
   */
  @Test
  void 한_템플릿_안에서_같은_키로_이름_붙인_표가_없다() throws IOException {
    Path root = Path.of("src/main/jte/stock");
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        List<String> keys = tableLabelKeys(source);
        for (String key : keys) {
          if (keys.stream().filter(key::equals).count() > 1 && !offenders.contains(key)) {
            offenders.add(file.getFileName() + ":" + key);
          }
        }
      }
    }
    assertThat(offenders).as("같은 메시지 키로 이름 붙은 표가 한 템플릿에 여럿 있다").isEmpty();
  }

  /** {@code <table ... aria-label="${...}">} 에서 고정 메시지 키만 모은다(정규식 없이). */
  private List<String> tableLabelKeys(String source) {
    List<String> keys = new ArrayList<>();
    int at = source.indexOf("<table");
    while (at >= 0) {
      int end = source.indexOf(">", at);
      if (end < 0) {
        break;
      }
      String tag = source.substring(at, end);
      String marker = "aria-label=" + (char) 34 + "${MessageUtil.getMessage(" + (char) 34;
      int label = tag.indexOf(marker);
      if (label >= 0) {
        int from = label + marker.length();
        int to = tag.indexOf((char) 34, from);
        if (to > from) {
          keys.add(tag.substring(from, to));
        }
      }
      at = source.indexOf("<table", end);
    }
    return keys;
  }

  /** 정규식 없이 여는 태그의 속성 값을 읽는다(표현식 안의 따옴표 때문에 표현식 끝까지 읽는다). */
  private String attributeAfter(String source, String marker) {
    int at = source.indexOf(marker);
    if (at < 0) {
      return null;
    }
    int from = at + marker.length();
    int to = source.indexOf("}" + (char) 34, from);
    return to < 0 ? null : source.substring(from, to);
  }
}
