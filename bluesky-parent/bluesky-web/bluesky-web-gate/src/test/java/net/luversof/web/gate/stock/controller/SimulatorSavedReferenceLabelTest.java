package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터의 왼쪽 3열은 저장해 둔 스냅샷이지, 관리 탭의 기준 데이터를 그때 계산한 값이 아니다.
 *
 * <p>두 화면이 같은 이름('1년 평균 과세표준 비중')으로 다른 수를 보여 준다 &mdash; 실측 2026-09-11, 8 종목 <b>전부</b> 달랐다:
 *
 * <pre>
 *   476800 KODEX 한국부동산리츠인프라  관리 82.79%  /  시뮬 17.35%
 *   329200 TIGER 리츠부동산인프라      관리 100.00% /  시뮬 13.42%   (이력 58행 전부 과표=분배금이라 어느 12개월 창도 100%)
 *   498400 KODEX 200타겟위클리커버드콜 관리 4.46%   /  시뮬 4.06%
 * </pre>
 *
 * <p>주당 분배금은 두 화면이 일치한다(₩29/₩33/₩270) &mdash; 비중만 벌어졌다. 값을 맞추는 것은 데이터 쓰기라 하지 않고, 열 이름이 관리 탭의 기준
 * 데이터와 같은 값처럼 읽히지 않게만 한다.
 */
class SimulatorSavedReferenceLabelTest {

  private Properties messages(String path) throws IOException {
    Properties props = new Properties();
    try (var reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    return props;
  }

  @Test
  void 열_이름이_저장값임을_말한다() throws IOException {
    Properties ko = messages("src/main/resources/uiMessage_ko.properties");
    Properties en = messages("src/main/resources/uiMessage.properties");

    String koLabel = ko.getProperty("stock.simulator.monthly.table.group.reference");
    assertThat(koLabel).as("관리 탭 이름('기준 데이터')과 같으면 같은 값으로 읽힌다").isNotEqualTo("기준 데이터");
    assertThat(koLabel).contains("저장");

    assertThat(en.getProperty("stock.simulator.monthly.table.group.reference"))
        .isNotEqualTo("Reference Data")
        .contains("Saved");
  }

  @Test
  void 관리_탭_이름은_그대로다() throws IOException {
    Properties ko = messages("src/main/resources/uiMessage_ko.properties");

    assertThat(ko.getProperty("stock.page.dividend.tab.monthly.reference"))
        .as("관리 탭은 실제로 기준 데이터를 그때 계산해 보여 준다")
        .isEqualTo("월배당 기준 데이터");
  }
}
