package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;

/**
 * 월배당 프로필 폼의 종목코드 목록은 코드 &rarr; 종목 이름 순이다(사용자 요청 2026-09-21).
 *
 * <p>예전에는 두 덩이였다: 태그가 붙은 종목(코드 순) 뒤에 프로필에만 있는 종목(코드 순)이 붙어, 목록 가운데서 코드가 다시 작아졌다. 게다가 화면이 고른 종목 하나를
 * 맨 위로 끌어올려 순서를 한 번 더 깼다 &mdash; 끌어올리기는 {@code selected} 속성으로 바꿨다.
 */
class MonthlyProfileSymbolOrderTest {

  private static final String TEMPLATE_PATH =
      "src/main/jte/stock/fragments/monthlyDividendReference.jte";

  private final MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();

  @Test
  void 태그_종목과_프로필_종목을_한_줄로_코드_순으로_섞는다() {
    List<StockItem> tagged =
        List.of(item("476800", "KODEX 한국부동산리츠인프라"), item("498400", "KODEX 200타겟위클리커버드콜"));
    // 프로필에만 있는 종목(태그가 아직 없다). 예전에는 이들이 통째로 뒤에 붙었다.
    List<StockItem> profileOnly =
        List.of(item("0018C0", "PLUS 고배당주위클리고정커버드콜"), item("0219E0", "KODEX 200커버드콜액티브"));

    // 병합 경로를 그대로 지난다 - 정렬을 병합 뒤에 안 걸면 두 덩이로 끊긴 채 나간다(변이 실험 2026-09-21).
    List<StockItem> merged =
        support.mergeMonthlyDividendReferenceStockItems(
            tagged, profileOnly.stream().map(MonthlyProfileSymbolOrderTest::profileOf).toList());

    assertThat(merged)
        .extracting(StockItem::name)
        .as("이름 순 한 줄 - 태그 여부로 덩이가 갈리면 안 된다")
        .containsExactly(
            "KODEX 200커버드콜액티브", "KODEX 200타겟위클리커버드콜", "KODEX 한국부동산리츠인프라", "PLUS 고배당주위클리고정커버드콜");
  }

  /** 이름이 같으면 코드로 가른다(순서가 입력 순서에 흔들리지 않게). */
  @Test
  void 이름이_같으면_코드_순이다() {
    List<StockItem> items =
        List.of(
            item("498400", "KODEX 같은이름"), item("0219E0", "KODEX 같은이름"), item("0018C0", "PLUS 고배당"));

    assertThat(support.sortSelectableStockItems(items))
        .extracting(StockItem::symbol)
        .as("이름이 먼저, 같은 이름 안에서는 코드")
        .containsExactly("0219E0", "498400", "0018C0");
  }

  @Test
  void 코드나_이름이_비어도_터지지_않는다() {
    List<StockItem> items =
        List.of(item(null, "이름만 있는 종목"), item("0018C0", null), item("0052D0", "TIGER"));

    assertThat(support.sortSelectableStockItems(items)).hasSize(3);
    assertThat(support.sortSelectableStockItems(items).get(0).name()).as("빈 이름이 먼저").isNull();
  }

  /** 화면이 고른 종목을 맨 위로 옮기면 정렬이 깨진다 - 제자리에서 selected 로 고른다. */
  @Test
  void 화면은_고른_종목을_끌어올리지_않는다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);
    int select = template.indexOf("<select name=\"profileSymbolSelection\"");
    assertThat(select).as("전제: 종목코드 select 를 찾는다").isGreaterThanOrEqualTo(0);
    String block = template.substring(select, template.indexOf("</select>", select));

    assertThat(block.split("@for\\(StockItem", -1).length - 1)
        .as("목록을 두 번 돌면 고른 것을 앞으로 빼는 것이다")
        .isEqualTo(1);
    assertThat(block)
        .as("고른 종목은 자리를 지키고 selected 로만 표시한다")
        .contains(
            "selected=\"${stockItem.symbol() != null && stockItem.symbol().equalsIgnoreCase(profileSymbolValue)}\"");
  }

  /** 프로필에만 있는 종목(종목 마스터에 태그가 아직 없다). */
  private static net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse profileOf(
      StockItem stockItem) {
    return new net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        stockItem.id(),
        stockItem.symbol(),
        stockItem.name(),
        "https://example.test/" + stockItem.symbol(),
        "MONTH_END",
        1,
        true,
        null,
        null,
        null);
  }

  private static StockItem item(String symbol, String name) {
    return new StockItem(UUID.randomUUID(), symbol, name, null, List.of());
  }
}
