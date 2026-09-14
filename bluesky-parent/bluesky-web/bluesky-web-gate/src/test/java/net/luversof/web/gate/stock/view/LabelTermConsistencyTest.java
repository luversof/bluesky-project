package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 같은 지표를 화면마다 다르게 적지 않는다.
 *
 * <p>실측 2026-09-11(10 화면의 표 머리글 · 카드 라벨 232 종): 띄어쓰기만 다른 짝이 둘 있었다 &mdash; "실현 손익"(대시보드 · 자산 현황 · 자산
 * 성장 · 종목 상세 · 계좌 상세) vs "실현손익"(대시보드 · 자산 성장 · 매매), "과세 금액"(배당) vs "과세금액"(자산 성장). 대시보드와 자산 성장은 <b>한
 * 화면 안에서</b> 두 표기가 같이 나왔다.
 *
 * <p>머리글은 띄어 쓰는 쪽이 다수라(평가 손익 · 누적 배당 · 매수 금액) 그쪽으로 맞췄다. 영어도 같은 지표가 "Realized P&amp;L" 과 "Realized
 * Profit" 으로 갈려 있어 후자로 통일했다. 문장 안의 복합어(실현손익 · 평가손익)는 그대로 둔다.
 */
class LabelTermConsistencyTest {

  private Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources").resolve(name), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  void 라벨_키는_한_가지_표기만_쓴다() throws IOException {
    Properties ko = load("uiMessage_ko.properties");

    List<String> realizedKeys =
        List.of(
            "stock.profit.realized",
            "stock.summary.period.realized",
            "stock.trade.label.realized.profit");
    List<String> shown = new ArrayList<>();
    for (String key : realizedKeys) {
      shown.add(ko.getProperty(key));
    }
    assertThat(shown).as("실현 손익 라벨 세 키").containsOnly("실현 손익");

    assertThat(ko.getProperty("stock.dividend.taxable.amount")).isEqualTo("과세 금액");
    assertThat(ko.getProperty("stock.asset.growth.cost.col.dividend.taxable")).isEqualTo("과세 금액");
  }

  @Test
  void 영어도_같은_지표를_같은_말로_적는다() throws IOException {
    Properties en = load("uiMessage.properties");

    assertThat(en.getProperty("stock.summary.period.realized")).isEqualTo("Realized Profit");
    assertThat(en.getProperty("stock.trade.label.realized.profit")).isEqualTo("Realized Profit");
    assertThat(en.getProperty("stock.profit.realized")).isEqualTo("Realized Profit");
  }
}
