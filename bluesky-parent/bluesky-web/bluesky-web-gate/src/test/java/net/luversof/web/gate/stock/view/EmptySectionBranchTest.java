package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 자료가 없는 구역은 <b>말없이 사라지지 않는다</b> &mdash; 자리는 남기고 그렇다고 말한다.
 *
 * <p>실측 2026-09-16: 네 자리가 같은 모양으로 빠져 있었다. (1) 거래가 없는 43 종목(86 중)의 종목 상세에서 "보유 평가액 추이" 가 제목째, (2) 빈
 * 기간의 매매 내역에서 "계좌별 실현손익" 과 "종목별 실현손익" 이 제목째 &mdash; 같은 화면의 형제 구역(매수 집중도 · 월별 매매 금액 · 상세 목록)은 모두
 * "없습니다" 라고 말하고 있었다. (3) 매매 내역의 "기간별 집계" 도 마찬가지. (4) 종목 상세의 "월별 성과" 는 조각에 빈 가지가 <b>이미 있는데</b> 호출부가
 * 까닭을 안 넘겨 침묵했다.
 *
 * <p>글자만 세면 딴 구역의 안내를 빌려 와도 통과한다. 그래서 <b>그 @if 가 제 빈 가지를 품는지</b>를 깊이로 따진다 &mdash; 자리가 값을 감싸는지까지 보는
 * 것이다.
 */
class EmptySectionBranchTest {

  private static final String ITEM = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String ACCOUNT = "src/main/jte/stock/htmx/accountDetailContent.jte";
  private static final String REALIZED =
      "src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte";
  private static final String TRADE_BREAKDOWN =
      "src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte";
  private static final String NOTE_UTIL =
      "src/main/java/net/luversof/web/gate/stock/util/StockBreakdownNoteUtil.java";
  private static final String JTE_ROOT = "src/main/jte";

  @Test
  void 보유_평가액_추이는_비어도_안내를_남긴다() throws IOException {
    assertThat(branchBodyOf(ITEM, "@if(timeSeries != null", false))
        .as("종목 상세: 시계열이 비면 빈 상태를 그린다")
        .contains("emptyState(")
        .contains("stock.item.detail.empty.valuation.series")
        .contains("stock.item.detail.chart.title");

    assertThat(branchBodyOf(ACCOUNT, "@if(timeSeries != null", false))
        .as("계좌 상세: 시계열이 비면 빈 상태를 그린다")
        .contains("emptyState(")
        .contains("stock.account.detail.empty.valuation.series")
        .contains("stock.item.detail.chart.title");
  }

  /** 형제 구역(주가 추이)도 같은 규칙을 지킨다 - 함께 묶어 되돌아가지 않게 한다. */
  @Test
  void 주가_추이의_빈_가지도_그대로다() throws IOException {
    assertThat(branchBodyOf(ITEM, "@if(priceHistory != null", false))
        .as("종목 상세: 시세가 비면 빈 상태를 그린다")
        .contains("emptyState(")
        .contains("stock.item.detail.empty.price.history")
        .contains("stock.item.detail.price.chart.title");
  }

  /** 매매 내역의 실현손익 두 구역 - 판 것이 없는 기간은 흔하다(전체 거래 중 매도는 7 건뿐이다). */
  @Test
  void 실현손익_두_구역은_비어도_안내를_남긴다() throws IOException {
    assertThat(branchBodyOf(REALIZED, "@if(accountRealizedList != null", false))
        .as("계좌별 실현손익")
        .contains("emptyState(")
        .contains("stock.realized.empty.by.account")
        .contains("byAccountSectionLabel");

    assertThat(branchBodyOf(REALIZED, "@if(stockRealizedList != null", false))
        .as("종목별 실현손익")
        .contains("emptyState(")
        .contains("stock.realized.empty.by.stock")
        .contains("byStockSectionLabel");
  }

  /** 매매 내역의 기간별 집계 - 형제 조각과 같은 모양(@elseif 로 까닭을 적는다)이라야 한다. */
  @Test
  void 매매_기간별_집계도_비면_까닭을_남긴다() throws IOException {
    String branch = branchBodyOf(TRADE_BREAKDOWN, "@if(tradePeriods != null", true);
    assertThat(branch).as("빈 가지에 제목과 까닭이 함께 있다").contains("${title}").contains("breakdownNote");
    // 까닭을 고르는 규칙은 조각이 따로 들고 있지 않다.
    String src = Files.readString(Path.of(TRADE_BREAKDOWN), StandardCharsets.UTF_8);
    assertThat(src).as("규칙은 util 에서 온다").contains("StockBreakdownNoteUtil");
    for (String key :
        new String[] {"stock.trade.breakdown.empty.note", "stock.trade.breakdown.single.note"}) {
      assertThat(src).as(key + " 를 조각이 따로 들고 있다").doesNotContain(key);
      assertThat(Files.readString(Path.of(NOTE_UTIL), StandardCharsets.UTF_8))
          .as("규칙의 집")
          .contains(key);
    }
  }

  /**
   * 기간 쪼갬 표는 조각에 빈 가지가 이미 있다 - 빠진 것은 <b>호출부</b>였다.
   *
   * <p>한 조각을 쓰는 자리가 여럿이면 하나가 조용히 다르게 굴 수 있다. 그래서 자리마다 본다.
   */
  @Test
  void 기간_쪼갬_표를_부르는_자리는_모두_까닭을_넘긴다() throws IOException {
    List<String> calls = callsOf("@template.stock.htmx.fragments.periodBreakdownTable(");
    assertThat(calls).as("이 조각을 부르는 자리").hasSizeGreaterThanOrEqualTo(2);
    for (String call : calls) {
      assertThat(call).as(call).contains("periodBreakdownNote =");
    }
  }

  /** 그 까닭을 고르는 규칙은 한 곳에만 있다 - 사본이 흩어지면 갈라진다. */
  @Test
  void 까닭을_고르는_규칙은_한_곳에만_있다() throws IOException {
    String util = Files.readString(Path.of(NOTE_UTIL), StandardCharsets.UTF_8);
    for (String key :
        new String[] {
          "stock.asset.growth.breakdown.empty.note",
          "stock.asset.growth.breakdown.single.note",
          "stock.asset.growth.breakdown.yearly.note"
        }) {
      assertThat(util).as("규칙의 집").contains(key);
      for (String owner :
          new String[] {
            "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java",
            "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"
          }) {
        assertThat(Files.readString(Path.of(owner), StandardCharsets.UTF_8))
            .as(owner + " 가 " + key + " 를 따로 들고 있다")
            .doesNotContain(key);
      }
    }
  }

  @Test
  void 새_문구는_두_번들에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(
              Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
      assertThat(text)
          .as(bundle)
          .contains("stock.item.detail.empty.valuation.series")
          .contains("stock.account.detail.empty.valuation.series")
          .contains("stock.realized.empty.by.account")
          .contains("stock.realized.empty.by.stock")
          .contains("stock.trade.breakdown.empty.note")
          .contains("stock.trade.breakdown.single.note");
    }
  }

  /** 조각을 부르는 자리들의 본문(여는 괄호부터 짝이 맞는 닫는 괄호까지). */
  private static List<String> callsOf(String needle) throws IOException {
    List<String> out = new ArrayList<>();
    for (Path path : jteFiles()) {
      String src = String.join(" ", stripComments(Files.readString(path, StandardCharsets.UTF_8)));
      int at = src.indexOf(needle);
      while (at >= 0) {
        int depth = 0;
        int end = at + needle.length() - 1;
        for (int i = at + needle.length() - 1; i < src.length(); i++) {
          char c = src.charAt(i);
          if (c == '(') {
            depth++;
          } else if (c == ')') {
            depth--;
            if (depth == 0) {
              end = i;
              break;
            }
          }
        }
        out.add(path.getFileName() + ": " + src.substring(at, end + 1));
        at = src.indexOf(needle, end);
      }
    }
    return out;
  }

  private static List<Path> jteFiles() throws IOException {
    List<Path> out = new ArrayList<>();
    try (var walk = Files.walk(Path.of(JTE_ROOT))) {
      walk.filter(p -> p.toString().endsWith(".jte")).forEach(out::add);
    }
    return out;
  }

  /**
   * 주어진 @if 로 시작하는 블록에서 <b>제 짝인</b> 빈 가지의 몸통을 돌려준다. 안쪽에 또 @if 가 있어도 깊이로 가려낸다.
   *
   * @param allowElseIf 조건이 붙은 가지(@elseif)도 빈 가지로 칠지 - 까닭이 있을 때만 그리는 조각이 그렇다
   *     <p>판정 전에 JTE 주석을 지운다 - 주석에 적힌 문구를 코드로 세면 안 된다.
   */
  private static String branchBodyOf(String file, String ifNeedle, boolean allowElseIf)
      throws IOException {
    List<String> lines = stripComments(Files.readString(Path.of(file), StandardCharsets.UTF_8));
    int start = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith(ifNeedle)) {
        assertThat(start).as(file + " 의 " + ifNeedle + " 가 여럿이다").isEqualTo(-1);
        start = i;
      }
    }
    assertThat(start).as(file + " 에서 " + ifNeedle + " 를 못 찾았다").isGreaterThanOrEqualTo(0);

    int depth = 0;
    int branchAt = -1;
    StringBuilder collected = new StringBuilder();
    for (int i = start + 1; i < lines.size(); i++) {
      String line = lines.get(i);
      boolean isBranch = line.equals("@else") || (allowElseIf && line.startsWith("@elseif("));
      if (line.startsWith("@if(")) {
        depth++;
      } else if (line.equals("@endif")) {
        if (depth == 0) {
          assertThat(branchAt).as(file + " 의 " + ifNeedle + " 블록에 빈 가지가 없다").isGreaterThan(0);
          return collected.toString();
        }
        depth--;
      } else if (isBranch && depth == 0) {
        branchAt = i;
        continue;
      }
      if (branchAt > 0) {
        collected.append(line).append('\n');
      }
    }
    throw new AssertionError(file + " 의 " + ifNeedle + " 블록이 닫히지 않았다");
  }

  /** JTE 주석(<%-- --%>)을 지운, 앞뒤 공백을 턴 줄 목록. */
  private static List<String> stripComments(String source) {
    StringBuilder sb = new StringBuilder();
    int at = 0;
    while (at < source.length()) {
      int open = source.indexOf("<%--", at);
      if (open < 0) {
        sb.append(source, at, source.length());
        break;
      }
      sb.append(source, at, open);
      int close = source.indexOf("--%>", open);
      if (close < 0) {
        break;
      }
      at = close + 4;
    }
    List<String> lines = new ArrayList<>();
    for (String line : sb.toString().split("\n", -1)) {
      lines.add(line.trim());
    }
    return lines;
  }
}
