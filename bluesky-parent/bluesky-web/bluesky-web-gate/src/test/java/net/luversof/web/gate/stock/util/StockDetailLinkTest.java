package net.luversof.web.gate.stock.util;

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
 * 목록에서 상세로 가는 링크의 주소.
 *
 * <p>실측 2026-09-14: 목록의 종목·계좌 링크 <b>1,027 개가 전부</b> 기간을 버리고 있었다(대시보드 10 · 자산 현황 32 · 자산 성장 54 · 매매
 * 289 · 배당 336 · 활동 306, 기간을 들고 간 것 0). 누른 사람 화면에서는 저장값이 받쳐 주어 이어져 보이지만 <b>그 주소를 공유하면</b> 받는 쪽에는 다른
 * 기간이 열린다.
 *
 * <p>같은 병을 상세 화면 전환기({@link StockDetailSwitchHrefTest})와 배당 캘린더 달 이동에서 이미 고쳤는데 목록 쪽만 남아 있었다.
 *
 * <p>단 <b>기간과 보기 설정만</b> 들고 간다. 쪽·정렬·목록 필터는 그 목록의 것이라 상세로 넘기면 안 된다.
 *
 * <p><b>대시보드·자산 현황의 링크는 기간을 안 싣는다 &mdash; 그게 맞다.</b> 두 화면에는 기간 선택기가 없고(실측 2026-09-14: 프리셋 버튼 0 개,
 * 주소의 기간 키 0/3) 자산 현황은 '현재 보유' 화면이라 기간에 반응하지도 않는다. 들고 갈 기간이 애초에 없다. 수정 뒤 실측: 기간이 있는 네 화면 985 개가 전부
 * 들고 가고, 이 두 화면 42 개는 그대로다.
 */
class StockDetailLinkTest {

  private static final String JTE_ROOT = "src/main/jte";

  @Test
  void 기간과_보기_설정만_들고_간다() {
    String href =
        StockDetailLinkUtil.detailHref(
            "/stock/item",
            "rangeMode=12&startDate=2025-09-14&endDate=2026-09-14&timeZone=Asia%2FSeoul"
                + "&page=3&size=50&sort=amount&accountIdList=A,B&tradeUiStartDate=2025-09-15",
            "stockItemId",
            "ITEM");

    assertThat(href)
        .isEqualTo(
            "/stock/item?rangeMode=12&startDate=2025-09-14&endDate=2026-09-14"
                + "&timeZone=Asia%2FSeoul&stockItemId=ITEM");
  }

  /** 질의를 다시 묶지 않는다 - 묶으면 인코딩이 바뀐다. */
  @Test
  void 인코딩을_건드리지_않는다() {
    assertThat(
            StockDetailLinkUtil.detailHref(
                "/stock/account", "timeZone=Asia%2FSeoul&locale=en", "accountId", "ACC"))
        .isEqualTo("/stock/account?timeZone=Asia%2FSeoul&locale=en&accountId=ACC");
  }

  /** 아이디가 비면 아예 적지 않는다 - {@code ?stockItemId=} 는 아무 뜻도 없다. */
  @Test
  void 아이디가_없으면_적지_않는다() {
    assertThat(StockDetailLinkUtil.detailHref("/stock/item", "rangeMode=12", "stockItemId", ""))
        .isEqualTo("/stock/item?rangeMode=12");
    assertThat(StockDetailLinkUtil.detailHref("/stock/item", null, "stockItemId", null))
        .isEqualTo("/stock/item");
  }

  /** 들고 갈 것이 없으면 예전과 같은 모양이다. */
  @Test
  void 들고_갈_것이_없으면_아이디만_붙는다() {
    assertThat(
            StockDetailLinkUtil.detailHref("/stock/item", "page=2&sort=name", "stockItemId", "X"))
        .isEqualTo("/stock/item?stockItemId=X");
  }

  /**
   * 템플릿이 주소를 손으로 짓지 않는다.
   *
   * <p>여기서 한 곳이라도 다시 지으면 그 화면만 조용히 기간을 버린다 &mdash; 화면으로는 안 보이는 손실이라 사람 눈으로 못 잡는다.
   */
  @Test
  void 템플릿은_상세_주소를_짓지_않는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    List<Path> templates = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Path.of(JTE_ROOT))) {
      walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".jte"))
          .forEach(templates::add);
    }
    for (Path path : templates) {
      List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (line.contains("/stock/item?stockItemId=")
            || line.contains("/stock/account?accountId=")) {
          offenders.add(path.toString().replace((char) 92, '/') + ":" + (i + 1));
        }
      }
    }

    assertThat(templates).as("템플릿을 하나도 못 읽었다면 이 검사는 공짜로 통과한다").hasSizeGreaterThan(50);
    assertThat(offenders).as("StockDetailLinkUtil.item(...) / .account(...) 을 쓸 것").isEmpty();
  }

  /** 훑기가 실제로 쓰이는 자리를 찾는지 &mdash; 0 이면 위 검사가 아무것도 안 지킨다. */
  @Test
  void 템플릿이_도구를_쓰고_있다() throws IOException {
    int uses = 0;
    try (Stream<Path> walk = Files.walk(Path.of(JTE_ROOT))) {
      List<Path> paths = walk.filter(Files::isRegularFile).toList();
      for (Path path : paths) {
        String body = Files.readString(path, StandardCharsets.UTF_8);
        int at = body.indexOf("StockDetailLinkUtil.");
        while (at >= 0) {
          uses++;
          at = body.indexOf("StockDetailLinkUtil.", at + 1);
        }
      }
    }

    // 실측 2026-09-14: 링크 26 곳. 하한은 빈 훑기로 공짜 통과하는 것만 막으면 된다
    assertThat(uses).as("목록의 상세 링크가 이 도구를 통해야 한다").isGreaterThanOrEqualTo(20);
  }
}
