package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 상세 화면끼리 <b>양쪽으로</b> 오갈 수 있는지 본다.
 *
 * <p>계좌 상세의 보유 종목 표에는 종목 상세로 가는 링크가 있었는데, <b>반대 방향이 없었다</b> &mdash; 종목 상세의 계좌별 보유 표는 계좌 이름을 글자로만
 * 적었다. 그래서 종목에서 계좌로 가려면 뒤로 가서 계좌 목록을 다시 찾아야 했다.
 *
 * <p>전환기(같은 종류의 다른 대상으로 바로 가기)도 두 화면 모두에 붙어야 한다. 한쪽만 있으면 사용자는 어느 화면에서 되는지를 외워야 한다.
 */
class DetailCrossLinkTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");
  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /** 이 종목을 담고 있는 계좌 한 줄. */
  private static TradeProfit accountHolding(UUID accountId, String accountName) {
    TradeProfit base =
        TradeProfit.ofAccountStatus(
            accountName,
            new BigDecimal("1100000"),
            new BigDecimal("100000"),
            BigDecimal.ZERO,
            new BigDecimal("1000000"),
            BigDecimal.ZERO);
    return new TradeProfit(
        base.stockItemId(),
        base.stockItemName(),
        accountId,
        base.accountName(),
        base.totalBuyAmount(),
        base.averageBuyPrice(),
        base.totalSellQuantity(),
        base.averageSellPrice(),
        base.totalSellAmount(),
        base.realizedProfit(),
        10,
        base.currentPrice(),
        base.evaluationAmount(),
        base.evaluationProfit(),
        base.totalProfit(),
        base.totalBuyFee(),
        base.totalSellFee(),
        base.totalSellTax(),
        base.totalBuyCost(),
        base.totalSellProceeds(),
        base.averageBuyPriceNet(),
        base.averageSellPriceNet(),
        base.realizedProfitNet(),
        base.evaluationProfitNet(),
        base.totalProfitNet(),
        base.currentPriceDate());
  }

  @Test
  void 종목_상세에서_계좌_상세로_갈_수_있다() {
    UUID accountId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    Map<String, Object> model = new HashMap<>();
    model.put("contentReady", true);
    model.put("stockItem", new StockItem(UUID.randomUUID(), "005930", "표본종목", "KOSPI", List.of()));
    model.put("evaluationAmount", new BigDecimal("1100000"));
    model.put("accountHoldings", List.of(accountHolding(accountId, "표본계좌")));

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/stockItemDetail.jte", model, output);

    assertThat(output.toString())
        .as("이 방향 링크만 없어서 종목에서 계좌로 가려면 뒤로 가야 했다")
        .contains("/stock/account?accountId=" + accountId)
        .contains("표본계좌");
  }

  @Test
  void 두_상세_화면_모두_전환기를_붙인다() throws IOException {
    for (String page : List.of("stock/stockItemDetail.jte", "stock/accountDetail.jte")) {
      assertThat(read(JTE_ROOT.resolve(page)))
          .as(page + " 에 전환기가 없다 - 한쪽만 되면 어느 화면에서 되는지를 외워야 한다")
          .contains("detailNavSwitcher");
    }
  }

  @Test
  void 컨트롤러가_두_전환기_목록을_모두_채운다() throws IOException {
    String source = read(CONTROLLER);

    assertThat(source)
        .as("모델에 넣지 않으면 전환기는 조용히 사라진다 - 갈 곳이 없는 것과 구분되지 않는다")
        .contains("\"stockNavEntries\"")
        .contains("\"accountNavEntries\"");
  }
}
