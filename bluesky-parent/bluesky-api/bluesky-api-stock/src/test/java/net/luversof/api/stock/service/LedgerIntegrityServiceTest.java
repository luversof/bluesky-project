package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.response.LedgerIntegrityFinding;
import net.luversof.api.stock.web.dto.response.LedgerIntegrityResponse;

/**
 * 원장에서 산술적으로 불가능한 기록을 찾아내는지 본다.
 *
 * <p>이 앱의 원장은 증권사 화면을 사람이 옮겨 담은 것이라 실제로 잘못된 값이 들어와 있다(실측 2026-08-22: 배당 193 건 중 8 건이 세금 &gt; 과세표준).
 * 화면은 그 값을 그대로 더할 뿐이라 스스로 눈치채지 못하면 잘못된 값이 계속 합계에 섞인다.
 *
 * <p>정상 데이터를 이상으로 잡으면 경고가 무의미해지므로, 각 규칙마다 <b>걸려야 하는 값과 걸리면 안 되는 값</b>을 함께 넣는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LedgerIntegrityServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID STOCK_ITEM_ID = UUID.randomUUID();

  @Mock private TradeService tradeService;
  @Mock private DividendService dividendService;
  @Mock private StockItemRepository stockItemRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private net.luversof.api.stock.repository.MonthlyDividendPayoutRepository payoutRepository;

  @Mock
  private net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository snapshotRepository;

  private LedgerIntegrityService service() {
    StockItem stockItem = new StockItem();
    stockItem.setId(STOCK_ITEM_ID);
    stockItem.setName("KODEX 한국부동산리츠인프라");
    when(stockItemRepository.findAll()).thenReturn(List.of(stockItem));
    return new LedgerIntegrityService(
        tradeService,
        dividendService,
        stockItemRepository,
        accountRepository,
        payoutRepository,
        snapshotRepository);
  }

  private Dividend dividend(
      String gross, String tax, String taxable, Integer quantity, String perShare) {
    Dividend dividend = new Dividend();
    dividend.setId(UUID.randomUUID());
    dividend.setStockItemId(STOCK_ITEM_ID);
    dividend.setGrossAmount(gross == null ? null : new BigDecimal(gross));
    dividend.setTax(tax == null ? null : new BigDecimal(tax));
    dividend.setTaxableAmount(taxable == null ? null : new BigDecimal(taxable));
    dividend.setQuantity(quantity);
    dividend.setAmountPerShare(perShare == null ? null : new BigDecimal(perShare));
    dividend.setPayDate(Instant.parse("2026-08-19T00:00:00Z"));
    return dividend;
  }

  private Trade trade(
      TradeType type, int quantity, String price, String fee, String tax, String realized) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setStockItemId(STOCK_ITEM_ID);
    trade.setType(type);
    trade.setQuantity(quantity);
    trade.setPrice(new BigDecimal(price));
    trade.setFee(new BigDecimal(fee));
    trade.setTax(new BigDecimal(tax));
    trade.setRealizedProfit(realized == null ? null : new BigDecimal(realized));
    trade.setTradeDate(Instant.parse("2026-08-19T00:00:00Z"));
    return trade;
  }

  private LedgerIntegrityResponse run(List<Dividend> dividends, List<Trade> trades) {
    when(dividendService.findDividends(any())).thenReturn(dividends);
    when(tradeService.findByUserId(USER_ID)).thenReturn(trades);
    return service().check(USER_ID);
  }

  private LedgerIntegrityFinding finding(LedgerIntegrityResponse response, String code) {
    return response.findings().stream().filter(f -> f.code().equals(code)).findFirst().orElse(null);
  }

  @Test
  void 정상_원장에서는_아무것도_찾지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividend("300000", "46200", "300000", 10000, "30")),
            // 배당 수량(10,000주)을 감당할 만큼 사고 절반을 판다. 매수 100주로 두면 "10,000주에 붙은
            // 배당" 이 되어 DIVIDEND_QUANTITY_ABOVE_EVER_HELD 가 옳게 걸린다 - 정상 원장을 뜻하는
            // 이 검사의 자료 자체가 앞뒤가 맞지 않았다(2026-08-24 확인).
            List.of(
                trade(TradeType.BUY, 20512, "1000", "10", "0", "0"),
                trade(TradeType.SELL, 10256, "1500", "7", "150", "20000")));

    assertThat(response.findings()).as("정상 데이터를 이상으로 잡으면 경고가 무의미해진다").isEmpty();
    assertThat(response.dividendCount()).isEqualTo(1);
    assertThat(response.tradeCount()).isEqualTo(2);
  }

  /**
   * 매수 행의 실현손익 0 은 정상이다.
   *
   * <p>조회 응답은 매수의 실현손익을 null 로 비워 보내지만 저장값은 0 이다. 처음에 "null 이 아니면 이상" 으로 잡았다가 실데이터에서 매수 196 건이 통째로
   * 걸렸다(실측). 이상한 것은 0 이 아닌 손익이 붙은 경우뿐이다.
   */
  @Test
  void 매수의_실현손익_0_은_정상이고_0이_아니면_찾는다() {
    LedgerIntegrityResponse zero =
        run(List.of(), List.of(trade(TradeType.BUY, 10, "1000", "0", "0", "0")));
    assertThat(finding(zero, "TRADE_BUY_WITH_REALIZED_PROFIT")).isNull();

    LedgerIntegrityResponse nonZero =
        run(List.of(), List.of(trade(TradeType.BUY, 10, "1000", "0", "0", "5000")));
    assertThat(finding(nonZero, "TRADE_BUY_WITH_REALIZED_PROFIT")).isNotNull();
  }

  @Test
  void 나머지_규칙도_각각_잡는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(
                dividend("1000", "0", "1200", 10, "100"),
                dividend("1000", "0", "500", 10, "50"),
                dividend("-100", "0", "0", 10, null),
                dividend("1000", "0", "0", 0, null)),
            List.of(
                trade(TradeType.BUY, 10, "1000", "-5", "0", "0"),
                trade(TradeType.BUY, 10, "1000", "0", "30", "0"),
                trade(TradeType.SELL, 10, "1000", "0", "30", null),
                trade(TradeType.SELL, -1, "1000", "0", "30", "0"),
                trade(TradeType.BUY, 10, "-1000", "0", "0", "0")));

    for (String code :
        List.of(
            "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
            "DIVIDEND_PER_SHARE_MISMATCH",
            "DIVIDEND_NEGATIVE_AMOUNT",
            "DIVIDEND_QUANTITY_NOT_POSITIVE",
            "TRADE_NEGATIVE_FEE_OR_TAX",
            "TRADE_BUY_WITH_TAX",
            "TRADE_SELL_WITHOUT_REALIZED_PROFIT",
            "TRADE_QUANTITY_NOT_POSITIVE",
            "TRADE_NEGATIVE_PRICE")) {
      assertThat(finding(response, code)).as(code + " 를 잡지 못했다").isNotNull();
    }
  }

  /**
   * 화면은 비과세 계좌의 배당을 "세후액 = 총액" 으로 표시한다. 그 계좌에 세금이 기록돼 있으면 실제 받은 돈보다 크게 보인다.
   *
   * <p>실측 2026-08-22: 비과세 3계좌(ISA·연금저축1·2)의 기록 세금 합은 0 원이라 지금은 어긋나지 않는다. 화면이 이 전제에 기대므로 전제가 깨지는 순간
   * 알 수 있어야 한다.
   */
  @Test
  void 비과세_계좌에_세금이_기록되면_찾는다() {
    UUID deferredAccountId = UUID.randomUUID();
    UUID normalAccountId = UUID.randomUUID();
    net.luversof.api.stock.domain.Account deferred = new net.luversof.api.stock.domain.Account();
    deferred.setId(deferredAccountId);
    deferred.setJsonConfig(java.util.Map.of("isTaxDeferred", Boolean.TRUE));
    net.luversof.api.stock.domain.Account normal = new net.luversof.api.stock.domain.Account();
    normal.setId(normalAccountId);
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(deferred, normal));

    Dividend taxedInDeferred = dividend("1000", "150", "1000", 10, null);
    taxedInDeferred.setAccountId(deferredAccountId);
    Dividend taxedInNormal = dividend("1000", "150", "1000", 10, null);
    taxedInNormal.setAccountId(normalAccountId);
    Dividend untaxedInDeferred = dividend("1000", "0", "0", 10, null);
    untaxedInDeferred.setAccountId(deferredAccountId);

    LedgerIntegrityResponse response =
        run(List.of(taxedInDeferred, taxedInNormal, untaxedInDeferred), List.of());

    LedgerIntegrityFinding hit = finding(response, "DIVIDEND_TAX_IN_TAX_DEFERRED_ACCOUNT");
    assertThat(hit).as("비과세 계좌의 기록 세금을 잡지 못했다").isNotNull();
    assertThat(hit.count()).as("일반 계좌나 세금 0 건까지 잡으면 경고가 무의미해진다").isEqualTo(1);
  }

  /**
   * 원장상 취득한 적이 없는 주식으로 배당을 받을 수는 없다.
   *
   * <p>실측 2026-08-22: 하나금융지주 배당 2건에 대응하는 매매 기록이 0건이었다. 손익 집계는 매매에서 파생되므로 그 종목이 아예 빠지고, 종목별 시계열을 전부
   * 더해도 전체와 배당이 2,100원 어긋난다 - 화면에서는 원인을 알 수 없다.
   */
  /**
   * 예시는 상한을 둔다. 전부 실으면 응답 크기가 원장 크기를 따라간다.
   *
   * <p>예전에는 {@code DIVIDEND_TAX_EXCEEDS_TAXABLE} 로 확인했는데, 그 규칙은 분리과세 때문에 걷어냈다. 규칙이 아니라 예시 상한을 보는
   * 검사이므로 살아 있는 배당 규칙으로 옮겨 그대로 둔다.
   */
  @Test
  void 예시는_최대_3건만_담고_건수는_전부_센다() {
    // 과세표준(2,000) > 세전(1,000) 인 배당 12 건.
    List<Dividend> many =
        java.util.stream.IntStream.range(0, 12)
            .mapToObj(i -> dividend("1000", "0", "2000", 10, null))
            .toList();

    LedgerIntegrityFinding hit = finding(run(many, List.of()), "DIVIDEND_TAXABLE_EXCEEDS_GROSS");

    assertThat(hit).as("대상 규칙이 걸리지 않았다 - 검사가 무력해진다").isNotNull();
    assertThat(hit.count()).isEqualTo(12);
    assertThat(hit.examples()).hasSize(3);
  }

  /**
   * 한 행이 여러 규칙에 걸리면 행으로 묶어 낸다.
   *
   * <p>실측 2026-08-23: 발견 48 건이 30 행이고 그중 10 행이 사유를 2~4 개씩 달고 있었다. 규칙별 목록만 있으면 화면이 같은 행을 규칙 그룹마다 다시
   * 그린다.
   *
   * <p>예전에는 과세표준 규칙 두 개가 함께 걸리는 행으로 확인했다. 그 규칙들은 분리과세 때문에 걷어냈으므로, 지금도 함께 걸리는 두 규칙으로 바꿔 둔다.
   */
  @Test
  void 사유가_여러_개인_행을_묶어_낸다() {
    // 과세표준(2,000) > 세전(1,000) 이면서, 주당(29) x 수량(10) = 290 != 세전(1,000).
    LedgerIntegrityResponse response =
        run(List.of(dividend("1000", "0", "2000", 10, "29")), List.of());

    assertThat(response.multiReasonRows()).hasSize(1);
    var row = response.multiReasonRows().get(0);
    assertThat(row.date()).isEqualTo("2026-08-19");
    assertThat(row.codes())
        .as("한 행에 걸린 사유를 모두 담아야 화면이 묶어 그릴 수 있다")
        .hasSizeGreaterThanOrEqualTo(2)
        .contains("DIVIDEND_TAXABLE_EXCEEDS_GROSS");
    assertThat(response.distinctRowCount()).isEqualTo(1);
  }

  /**
   * 배당 발견에도 계좌명을 적는다.
   *
   * <p>실측 2026-08-23: 규칙마다 발견이 <b>한 계좌에 100% 집중</b>돼 있었다 &mdash; 매매 없는 배당 2 건은 한국투자증권 위탁, 수수료·거래세가
   * 없는 매도 12 건은 전부 동양증권이다. 각 결함이 한 계좌의 입력 경로에 묶여 있다는 뜻이라, 계좌를 적으면 어디를 고쳐야 하는지가 드러난다.
   */
  @Test
  void 배당_발견에도_계좌명을_적는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    var account = new net.luversof.api.stock.domain.Account();
    account.setId(accountId);
    account.setUserId(USER_ID);
    account.setName("KB증권 위탁");
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));

    Dividend dividend = dividend("1000", "0", "2000", 10, null);
    dividend.setAccountId(accountId);

    LedgerIntegrityFinding found =
        finding(run(List.of(dividend), List.of()), "DIVIDEND_TAXABLE_EXCEEDS_GROSS");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("어느 계좌의 입력 경로가 문제인지 알아야 한다")
        .contains("[KB증권 위탁]");
  }

  @Test
  void 매매_기록이_없는_종목의_배당을_찾는다() {
    UUID tradedOnly = UUID.randomUUID();
    Trade trade = trade(TradeType.BUY, 10, "1000", "0", "0", "0");
    trade.setStockItemId(tradedOnly);

    Dividend traded = dividend("1000", "0", "0", 10, null);
    traded.setStockItemId(tradedOnly);
    Dividend orphan = dividend("2100", "0", "0", 10, null);

    LedgerIntegrityResponse response = run(List.of(traded, orphan), List.of(trade));

    LedgerIntegrityFinding hit = finding(response, "DIVIDEND_WITHOUT_TRADE");
    assertThat(hit).as("매매 기록이 없는 종목의 배당을 잡지 못했다").isNotNull();
    assertThat(hit.count()).as("매매가 있는 종목까지 잡으면 경고가 무의미해진다").isEqualTo(1);
    assertThat(hit.examples().get(0).detail()).contains("2100");
  }

  /** 날짜를 지정할 수 있는 배당/매매. 기준일 보유 검사는 날짜가 전부이므로 따로 둔다. */
  /**
   * 기준일이 <b>따로</b> 적힌 배당. 지급일은 그보다 뒤로 둔다(결산배당의 실제 모양).
   *
   * <p>기준일과 지급일이 같으면 그건 기준일 칸이 없어 지급일이 복사된 자료라, 기준일 규칙이 판정하지 않는다. 예전에는 이 도우미가 둘을 같게 만들어서, 규칙을 검사한다고
   * 하면서 실은 "지급일 보유" 를 검사하고 있었다.
   */
  private Dividend dividendOn(String recordDate, Integer quantity) {
    Dividend dividend = dividend("10000", "1540", "10000", quantity, "100");
    dividend.setRecordDate(Instant.parse(recordDate + "T00:00:00Z"));
    dividend.setPayDate(
        Instant.parse(recordDate + "T00:00:00Z").plus(java.time.Duration.ofDays(90)));
    return dividend;
  }

  /** 기준일 칸이 없어 지급일이 그대로 복사된 배당(이 원장의 실제 모양). */
  private Dividend dividendWithoutOwnRecordDate(String payDate, Integer quantity) {
    Dividend dividend = dividend("10000", "1540", "10000", quantity, "100");
    dividend.setRecordDate(Instant.parse(payDate + "T00:00:00Z"));
    dividend.setPayDate(Instant.parse(payDate + "T00:00:00Z"));
    return dividend;
  }

  private Trade tradeOn(String date, TradeType type, int quantity) {
    Trade trade = trade(type, quantity, "1000", "0", "0", type == TradeType.SELL ? "0" : null);
    trade.setTradeDate(Instant.parse(date + "T00:00:00Z"));
    return trade;
  }

  /**
   * 기준일에 보유가 0 인 배당을 찾는다.
   *
   * <p>배당수익률의 분모(기준일 원금)가 그 날 보유 스냅샷에서 나오므로, 보유가 0 이면 그 배당은 분자에서도 빠진다. 합계만 보면 드러나지 않는다(실측
   * 2026-08-23: 3 건 &mdash; 배당 기준일 자리에 지급일을 적어 그 사이 전량 매도한 경우).
   */
  @Test
  void 기준일에_보유가_없는_배당을_찾는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2021-04-08", 300)),
            List.of(
                tradeOn("2020-01-30", TradeType.BUY, 300),
                tradeOn("2021-01-18", TradeType.SELL, 300)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("그 날 보유=0");
  }

  /**
   * 기준일 칸이 없어 지급일이 복사된 자료는 판정하지 않는다.
   *
   * <p>이 원장이 그렇다 &mdash; 실측 2026-08-24: 배당 193 건 전부 {@code recordDate == payDate}. 그 상태에서 "기준일 보유"
   * 를 따지면 사실은 "지급일 보유" 를 따지는 것이라, 결산배당이 거의 언제나 걸린다. 기준일이 전년 12-31 이면 그 뒤에 팔아도 배당은 나오기 때문이다.
   *
   * <p>실측으로 걸려 있던 3 건이 모두 그런 경우였다 &mdash; HK이노엔(2021-08-09 매수 &rarr; 2022-04-22 지급 + 같은 날 매도),
   * NAVER(2020-01-30 매수 &rarr; 2021-01-18 매도 &rarr; 2021-04-08 지급), 삼성SDI(2019-12-13 매수 &rarr;
   * 2020-01-28 매도 &rarr; 2020-04-17 지급). 셋 다 기준일에는 들고 있었다.
   */
  @Test
  void 기준일이_지급일과_같으면_판정하지_않는다() {
    // HK이노엔 실측 모양: 지급일에 전량 매도. 기준일을 모르므로 "보유 없음" 이라고 말할 수 없다.
    LedgerIntegrityResponse response =
        run(
            List.of(dividendWithoutOwnRecordDate("2022-04-22", 12)),
            List.of(
                tradeOn("2021-08-09", TradeType.BUY, 12),
                tradeOn("2022-04-22", TradeType.SELL, 12)));

    assertThat(finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE"))
        .as("기준일을 모르는 자료로 보유가 없다고 단정하면 결산배당이 전부 걸린다")
        .isNull();
  }

  /** 기준일에 들고 있었으면 걸리지 않는다. 정상을 이상으로 잡으면 경고가 무의미해진다. */
  @Test
  void 기준일에_보유가_있으면_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2024-04-19", 155)),
            List.of(
                tradeOn("2023-08-14", TradeType.BUY, 155),
                tradeOn("2025-06-13", TradeType.SELL, 155)));

    assertThat(finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE")).isNull();
  }

  /** 기준일 당일 매수도 그 날 보유로 본다(원장에는 시각이 없다). */
  @Test
  void 기준일_당일_매수는_보유로_본다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2024-04-19", 10)),
            List.of(tradeOn("2024-04-19", TradeType.BUY, 10)));

    assertThat(finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE")).isNull();
  }

  /**
   * 그때까지 한 번도 보유한 적 없는 수량에 붙은 배당을 찾는다.
   *
   * <p>앞의 두 규칙은 "거래가 아예 없다"와 "그 날 보유가 0"만 본다. 보유가 있는데 수량만 과하게 적힌 경우는 어느 쪽에도 걸리지 않는데, 그러면 그 배당의 주당
   * 배당금과 수익률 분모가 함께 틀어진다.
   *
   * <p>실측 2026-08-24: 배당 193 건을 (계좌·종목·지급일)로 묶은 188 묶음 중, 합계 수량이 직전 400 일의 어느 보유 수량과도 맞지 않는 것은
   * 하나금융지주 2 건뿐이었고 그건 {@code DIVIDEND_WITHOUT_TRADE} 가 이미 잡는다. 즉 이 규칙의 현재 발견은 0 이며, 앞으로 배당을 넣을 때를
   * 위한 그물이다.
   */
  @Test
  void 보유한_적_없는_수량의_배당을_찾는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2026-05-19", 10256)),
            List.of(tradeOn("2026-01-05", TradeType.BUY, 77)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_QUANTITY_ABOVE_EVER_HELD");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("배당수량=10256").contains("그때까지 최대 보유=77");
  }

  /**
   * 기준일과 지급일이 떨어져 그 사이 전량 매도한 배당은 걸리지 않는다.
   *
   * <p>"그 날 보유"로 견주면 이런 정상 자료가 걸린다. 그래서 "그때까지의 최대 보유"로 본다(실측: NAVER 2020-12 기준 배당의 지급일은 2021-04-08
   * 인데 2021-01-18 에 전량 매도했다).
   */
  @Test
  void 기준일_지급일_시차로_이미_판_배당은_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2021-04-08", 300)),
            List.of(
                tradeOn("2020-01-30", TradeType.BUY, 300),
                tradeOn("2021-01-18", TradeType.SELL, 300)));

    assertThat(finding(response, "DIVIDEND_QUANTITY_ABOVE_EVER_HELD")).isNull();
  }

  /** 같은 지급일에 여러 행으로 쪼개 적힌 배당(주간 분배금)은 행마다 보유보다 작아 걸리지 않는다. */
  @Test
  void 지급일에_쪼개_적힌_배당은_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2026-06-02", 989), dividendOn("2026-06-02", 4677)),
            List.of(tradeOn("2026-01-05", TradeType.BUY, 5666)));

    assertThat(finding(response, "DIVIDEND_QUANTITY_ABOVE_EVER_HELD")).isNull();
  }

  /** 거래가 아예 없는 종목은 DIVIDEND_WITHOUT_TRADE 가 알리므로 이 규칙에서는 빼 중복 경고를 만들지 않는다. */
  @Test
  void 거래가_없는_종목은_이_규칙에서_제외한다() {
    LedgerIntegrityResponse response = run(List.of(dividendOn("2020-04-08", 1)), List.of());

    assertThat(finding(response, "DIVIDEND_WITHOUT_TRADE")).isNotNull();
    assertThat(finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE")).isNull();
  }

  /** 사유가 하나뿐인 행은 묶음에 넣지 않는다. 전부 넣으면 묶음이 원래 목록과 같아져 쓸모가 없다. */
  @Test
  void 사유가_하나뿐인_행은_묶음에_넣지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                trade(TradeType.BUY, 10, "1000", "10", "0", null),
                sellOn("2020-01-28", 100, "10000", "0", "0")));

    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX")).isNotNull();
    assertThat(response.multiReasonRows()).isEmpty();
  }

  private net.luversof.api.stock.domain.MonthlyDividendPayout payoutWithRatio(
      String payDate, String perShare, String taxablePerShare) {
    var row = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    row.setStockItemId(STOCK_ITEM_ID);
    row.setPayDate(java.time.LocalDate.parse(payDate));
    // 참조 시트에는 기준일이 지급일보다 앞선다(실측: 2026-04-17 지급 / 2026-04-15 기준).
    row.setRecordDate(java.time.LocalDate.parse(payDate).minusDays(2));
    row.setDividendAmountPerShare(new BigDecimal(perShare));
    row.setTaxableBasePerShare(new BigDecimal(taxablePerShare));
    return row;
  }

  private void snapshotForUser() {
    var snapshot = new net.luversof.api.stock.domain.MonthlyDividendSnapshot();
    snapshot.setUserId(USER_ID);
    snapshot.setStockItemId(STOCK_ITEM_ID);
    when(snapshotRepository.findByUserIdOrderByUpdatedDateDesc(USER_ID))
        .thenReturn(List.of(snapshot));
  }

  private net.luversof.api.stock.domain.MonthlyDividendPayout payout(String payDate) {
    var row = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    row.setStockItemId(STOCK_ITEM_ID);
    row.setPayDate(java.time.LocalDate.parse(payDate));
    return row;
  }

  /**
   * 배당을 받았는데 그 달의 참조 지급 이력이 없는 경우.
   *
   * <p>데이터 상태 화면의 "밀림" 표시는 주기 추정이고(경과일 > 그 종목의 과거 최대 간격), 이 규칙은 증거로 판정한다 &mdash; 원장에 그 달 배당이 실제로
   * 들어와 있으면 참조에도 그 달이 있어야 한다. 실측 2026-08-23: 4 건이고 주기 추정이 짚은 4 종목과 정확히 같았다.
   */
  @Test
  void 배당을_받았는데_그_달_참조가_없으면_찾는다() {
    when(payoutRepository.findAllByOrderByPayDateDescRecordDateDesc())
        .thenReturn(List.of(payout("2026-07-20")));

    LedgerIntegrityResponse response =
        run(List.of(dividendInAccount("300000", "46200", "300000", 10000, "30")), List.of());

    LedgerIntegrityFinding found = finding(response, "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("2026-08", "자동 가져오기 필요");
  }

  /**
   * 같은 (종목, 달)이 여러 계좌에 있어도 한 건으로 센다.
   *
   * <p>이 규칙의 주어는 "이 종목의 이 달 참조가 없다" 이고 조치는 자동 가져오기 한 번이다. 원장 행으로 세면 계좌 수만큼 부풀어 할 일이 실제보다 많아 보인다
   * &mdash; 실측 2026-08-23: 원장 행으로는 9 건인데 가져오기는 4 번이면 끝난다.
   */
  @Test
  void 같은_종목_같은_달은_계좌가_여럿이어도_한_건이다() {
    when(payoutRepository.findAllByOrderByPayDateDescRecordDateDesc())
        .thenReturn(List.of(payout("2026-07-20")));

    java.util.UUID firstAccountId = java.util.UUID.randomUUID();
    java.util.UUID secondAccountId = java.util.UUID.randomUUID();
    var accountA = new net.luversof.api.stock.domain.Account();
    accountA.setId(firstAccountId);
    accountA.setUserId(USER_ID);
    accountA.setName("KB증권 위탁");
    var accountB = new net.luversof.api.stock.domain.Account();
    accountB.setId(secondAccountId);
    accountB.setUserId(USER_ID);
    accountB.setName("한국투자증권 ISA");
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(accountA, accountB));

    var first = dividendInAccount("300000", "46200", "300000", 10000, "30");
    first.setAccountId(firstAccountId);
    var second = dividendInAccount("300000", "46200", "300000", 10000, "30");
    second.setAccountId(secondAccountId);
    LedgerIntegrityResponse response = run(List.of(first, second), List.of());

    LedgerIntegrityFinding found = finding(response, "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH");
    assertThat(found).isNotNull();
    assertThat(found.count()).as("계좌가 둘이어도 가져오기는 한 번이다").isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("계좌 2곳");
  }

  /** 그 달 참조가 있으면 걸리지 않는다. */
  @Test
  void 그_달_참조가_있으면_걸리지_않는다() {
    when(payoutRepository.findAllByOrderByPayDateDescRecordDateDesc())
        .thenReturn(List.of(payout("2026-08-19"), payout("2026-07-20")));

    LedgerIntegrityResponse response =
        run(List.of(dividendInAccount("300000", "46200", "300000", 10000, "30")), List.of());

    assertThat(finding(response, "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH")).isNull();
  }

  /**
   * 참조를 아예 쓰지 않는 종목은 대상이 아니다.
   *
   * <p>개별 주식 배당까지 "참조가 없다" 고 하면 이 규칙이 전부 걸려 쓸모없어진다.
   */
  @Test
  void 참조가_없는_종목은_대상이_아니다() {
    when(payoutRepository.findAllByOrderByPayDateDescRecordDateDesc()).thenReturn(List.of());

    LedgerIntegrityResponse response =
        run(List.of(dividendInAccount("300000", "46200", "300000", 10000, "30")), List.of());

    assertThat(finding(response, "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH")).isNull();
  }

  /** 계좌를 붙인 배당. 과세 여부 판정이 계좌 설정에 걸려 있어 계좌 없는 행은 규칙 대상이 아니다. */
  private Dividend dividendInAccount(
      String gross, String tax, String taxable, Integer quantity, String perShare) {
    Dividend dividend = dividend(gross, tax, taxable, quantity, perShare);
    dividend.setAccountId(UUID.randomUUID());
    return dividend;
  }

  /**
   * 개별 주식 배당인데 원천징수가 없는 경우.
   *
   * <p>ETF 분배금은 재원이 매매차익이면 비과세라 세금 0 이 정상이다. 그래서 개별 주식인지부터 가려야 하는데, 그 판정을 원장에서 끌어낸다 &mdash; 증권거래세는
   * 개별 주식 매도에만 붙는다. 실측 2026-08-23 으로 오분류 0 을 확인했다(개별 6 종목 전부 매도 거래세 있음, ETF 4 종목 전부 없음).
   */
  @Test
  void 개별_주식_배당인데_원천징수가_없으면_찾는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(
                dividendInAccount("100000", "0", "0", 100, "1000"),
                // 하한을 잡을 재료. 실제로 15,400 원이 징수된 배당이 원장에 있다.
                dividendInAccount("100000", "15400", "100000", 100, "1000")),
            // 매도에 거래세가 붙었으므로 이 종목은 개별 주식이다.
            List.of(trade(TradeType.SELL, 10, "1000", "10", "23", "0")));

    LedgerIntegrityFinding found = finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail())
        .as("얼마를 떼었어야 하는지 함께 보여야 고칠 수 있다")
        .contains("gross=100000", "15.4% 기준 예상 세금=15400");
  }

  /**
   * 예상 세액이 아주 작으면 안 뗀 것이 정상이다.
   *
   * <p>세액이 작으면 원천징수를 하지 않는다. 그 경계를 바깥 지식으로 박지 않고 <b>원장에서 실제로 징수된 가장 작은 세액</b>으로 잡는다(수수료·거래세 규칙과 같은
   * 방식).
   *
   * <p>실측 2026-08-24: 개별주식 배당 36 건 중 세금이 0 인 것은 HK이노엔 2022-04-22(소액 배당) 한 건뿐인데, 그 건이 곧 <b>예상 세액이 가장
   * 작은 한 건</b>이었다. 나머지 35 건은 예상 세액이 3,690 원 이상이고 전부 15.34~15.40% 로 징수됐다. 과세 계좌 전체로 넓혀도 예상 세액 1,000
   * 원 미만인 9 건이 예외 없이 세금 0 이다. 즉 누락이 아니라 소액이라 떼지 않은 것이다.
   */
  @Test
  void 예상_세액이_원장의_최소_징수액보다_작으면_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(
                // HK이노엔과 같은 모양(소액 배당): 세전 4,000 · 예상 세금 616.
                dividendInAccount("4000", "0", "0", 12, "333.33"),
                // 원장에서 실제로 징수된 가장 작은 세액이 1,500 원이라고 하자.
                dividendInAccount("200000", "1500", "10000", 100, "2000")),
            List.of(trade(TradeType.SELL, 10, "1000", "10", "23", "0")));

    assertThat(finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING"))
        .as("소액이라 떼지 않은 건을 누락이라고 하면, 고칠 수 없는 지적이 계속 남는다")
        .isNull();
  }

  /**
   * 하한은 <b>가장 작은</b> 징수액이다. 가장 큰 것을 쓰면 진짜 누락까지 조용히 넘어간다.
   *
   * <p>징수된 세액이 1,500 원과 15,400 원 두 가지 있을 때, 예상 세액 6,160 원짜리 무징수 배당은 "작아서 안 뗀 것" 이 아니다 &mdash; 이미
   * 1,500 원짜리도 징수됐기 때문이다. 최대(15,400)를 하한으로 쓰면 이 건이 빠져나간다.
   */
  @Test
  void 하한은_가장_작은_징수액이다() {
    LedgerIntegrityResponse response =
        run(
            List.of(
                // 예상 세액 6,160 원. 하한(1,500)보다 크므로 누락이다.
                dividendInAccount("40000", "0", "0", 100, "400"),
                dividendInAccount("200000", "1500", "10000", 100, "2000"),
                dividendInAccount("100000", "15400", "100000", 100, "1000")),
            List.of(trade(TradeType.SELL, 10, "1000", "10", "23", "0")));

    LedgerIntegrityFinding found = finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING");
    assertThat(found).as("하한을 최대 징수액으로 잡으면 이 누락이 조용히 넘어간다").isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("gross=40000");
  }

  /** 원장에 징수된 세금이 하나도 없으면 하한을 알 수 없다. 그때는 예전처럼 판정한다. */
  @Test
  void 하한을_알_수_없으면_예전처럼_판정한다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendInAccount("4000", "0", "0", 12, "333.33")),
            List.of(trade(TradeType.SELL, 10, "1000", "10", "23", "0")));

    assertThat(finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING")).isNotNull();
  }

  /** ETF 는 매도에 거래세가 붙지 않는다. 세금 0 이 정상이므로 걸리면 안 된다. */
  @Test
  void ETF_분배금은_세금이_0_이어도_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendInAccount("1400000", "0", "0", 5000, "280")),
            List.of(trade(TradeType.SELL, 10, "1000", "10", "0", "0")));

    assertThat(finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING")).isNull();
  }

  /** 원천징수가 제대로 들어 있으면 걸리지 않는다. */
  @Test
  void 원천징수가_있는_개별_주식_배당은_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendInAccount("43000", "6620", "43000", 100, "430")),
            List.of(trade(TradeType.SELL, 10, "1000", "10", "23", "0")));

    assertThat(finding(response, "STOCK_DIVIDEND_WITHOUT_WITHHOLDING")).isNull();
  }

  /**
   * 수수료가 한 건도 없는 계좌는 <b>계좌 단위로 한 번만</b> 알린다.
   *
   * <p>거래마다 또 알리면 같은 사정이 열몇 번 반복돼 화면을 덮는다. 그 계좌는 증권사에서 기록을 되받을 수 없어 채울 방법이 없으므로, 반복될수록 나머지 지적까지 같이
   * 무시하게 된다(실측 2026-08-24: 동양증권 하나가 매도 12 건 + 계좌 1 건 = 13 건이었다).
   *
   * <p>접으면서 금액을 잃지 않도록, 빠진 거래세 몫을 계좌 지적이 대신 짊어진다(실측: 매도 12 건 · 추정 거래세는 그 매도금액의 0.25%).
   */
  @Test
  void 수수료가_없는_계좌는_거래마다_다시_알리지_않는다() {
    java.util.UUID feelessId = java.util.UUID.randomUUID();
    var feeless = new net.luversof.api.stock.domain.Account();
    feeless.setId(feelessId);
    feeless.setUserId(USER_ID);
    feeless.setName("동양증권");
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(feeless));

    List<Trade> trades = new java.util.ArrayList<>();
    // 수수료가 한 건도 없는 계좌를 만든다(하한 10건 이상).
    for (int i = 0; i < 9; i++) {
      Trade buy = trade(TradeType.BUY, 100, "10000", "0", "0", null);
      buy.setAccountId(feelessId);
      trades.add(buy);
    }
    // 수수료·거래세가 둘 다 없는 큰 매도. 예전에는 이 건이 따로 지적됐다.
    Trade sell = trade(TradeType.SELL, 100, "100000", "0", "0", "0");
    sell.setAccountId(feelessId);
    trades.add(sell);
    // 세율 관측 표본(다른 계좌의 정상 매도).
    Trade taxed = trade(TradeType.SELL, 100, "100000", "5000", "23000", "0");
    taxed.setAccountId(java.util.UUID.randomUUID());
    trades.add(taxed);

    LedgerIntegrityResponse response = run(List.of(), trades);

    assertThat(finding(response, "ACCOUNT_WITHOUT_ANY_FEE"))
        .as("계좌 단위 지적이 없으면 이 사정을 볼 곳이 사라진다")
        .isNotNull();
    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX"))
        .as("계좌 단위로 이미 알린 것을 거래마다 다시 알리면 고칠 수 없는 지적이 화면을 덮는다")
        .isNull();
    assertThat(finding(response, "ACCOUNT_WITHOUT_ANY_FEE").examples().get(0).detail())
        .as("접으면서 빠진 거래세 금액까지 사라지면 얼마나 어긋났는지 알 수 없다")
        .contains("거래세 없는 매도 1건");
  }

  private Trade tradeOn(String isoDate, TradeType type, int quantity, String price) {
    Trade trade = trade(type, quantity, price, "10", "0", type == TradeType.SELL ? "0" : null);
    trade.setTradeDate(Instant.parse(isoDate + "T00:00:00Z"));
    return trade;
  }

  /**
   * 한국 증시는 토·일에 열리지 않으므로 그 날짜의 거래는 입력 오류다.
   *
   * <p>실측 2026-08-23: 250 건 중 2 건 &mdash; 동양증권 한화오션 매수 2019-03-23(토), 매도 2019-04-21(일). 두 건 모두
   * 수수료·거래세가 0 이라 계좌 단위 규칙에도 걸리지만, 날짜 오류는 별개라 따로 낸다.
   */
  @Test
  void 주말_거래를_찾는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                tradeOn("2019-03-22", TradeType.BUY, 460, "21753"), // 금(정상)
                tradeOn("2019-03-23", TradeType.BUY, 460, "21753"), // 토
                tradeOn("2019-04-21", TradeType.SELL, 460, "22850"), // 일
                tradeOn("2019-04-22", TradeType.SELL, 460, "22850"))); // 월(정상)

    LedgerIntegrityFinding found = finding(response, "TRADE_ON_WEEKEND");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(2);
    assertThat(found.examples()).extracting("date").containsExactly("2019-04-21", "2019-03-23");
    assertThat(found.examples().get(0).detail()).contains("거래일=2019-04-21(일)", "460주 @22850");
  }

  /** 평일만 있으면 이 규칙은 아무것도 내지 않아야 한다 &mdash; 항상 켜져 있으면 화면이 쓸모없어진다. */
  @Test
  void 평일_거래만_있으면_주말_규칙은_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                tradeOn("2019-03-18", TradeType.BUY, 10, "1000"),
                tradeOn("2019-03-19", TradeType.BUY, 10, "1000"),
                tradeOn("2019-03-20", TradeType.BUY, 10, "1000"),
                tradeOn("2019-03-21", TradeType.BUY, 10, "1000"),
                tradeOn("2019-03-22", TradeType.BUY, 10, "1000")));

    assertThat(finding(response, "TRADE_ON_WEEKEND")).isNull();
  }

  /** 금액을 지정할 수 있는 매매. 이 규칙은 금액 임계값이 핵심이라 따로 둔다. */
  private Trade sell(String price, int quantity, String fee, String tax) {
    Trade trade = trade(TradeType.SELL, quantity, price, fee, tax, "0");
    return trade;
  }

  /**
   * 매도인데 수수료와 거래세가 둘 다 0 이면 원가·실현손익이 그만큼 부풀어 오른다.
   *
   * <p>ETF 매도는 거래세가 면제라 세금만 0 인 것은 정상이다. 하지만 수수료는 ETF 에도 붙는다 &mdash; 실측 2026-08-23: 세금만 0 인 매도 13
   * 건은 전부 수수료가 있었고(최소 1 원), 둘 다 0 인 12 건은 전부 개별주식이었다.
   */
  @Test
  void 매도인데_수수료와_거래세가_둘_다_0이면_찾는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                trade(TradeType.BUY, 10, "1000", "10", "0", null), // 수수료가 붙은 거래(임계값 보정용)
                sell("100000", 10, "0", "0")));

    LedgerIntegrityFinding found = finding(response, "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail()).contains("매도금액=1000000");
  }

  /** ETF 매도(거래세만 0, 수수료는 있음)는 정상이므로 걸리면 안 된다. */
  @Test
  void 거래세만_0인_매도는_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                trade(TradeType.BUY, 10, "1000", "10", "0", null), sell("100000", 10, "1", "0")));

    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX")).isNull();
  }

  /** 임계값은 원장에서 스스로 보정한다 - 수수료가 실제로 붙은 가장 작은 거래보다 작은 매도는 반올림으로 0 이 될 수 있으므로 제외한다. */
  @Test
  void 수수료가_붙는_최소금액보다_작은_매도는_제외한다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                trade(TradeType.BUY, 10, "10000", "10", "0", null), // 거래대금 100,000 에 수수료 10
                sell("100", 10, "0", "0"))); // 거래대금 1,000 - 임계값 미만

    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX")).isNull();
  }

  /** 매수는 이 규칙 대상이 아니다(매수에는 거래세가 없다). */
  @Test
  void 매수는_이_규칙에_걸리지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                trade(TradeType.SELL, 10, "100000", "100", "1000", "0"),
                trade(TradeType.BUY, 10, "100000", "0", "0", null)));

    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX")).isNull();
  }

  private net.luversof.api.stock.domain.Account account(UUID id, String name) {
    net.luversof.api.stock.domain.Account account = new net.luversof.api.stock.domain.Account();
    account.setId(id);
    account.setName(name);
    return account;
  }

  private Trade tradeIn(UUID accountId, TradeType type, String price, int quantity, String fee) {
    Trade trade = trade(type, quantity, price, fee, "0", type == TradeType.SELL ? "0" : null);
    trade.setAccountId(accountId);
    return trade;
  }

  /**
   * 계좌의 모든 거래에 수수료가 없으면 그 계좌의 원가·실현손익이 통째로 어긋난다.
   *
   * <p>건별로는 판정할 수 없다 &mdash; 공모주 청약은 수수료가 없는 것이 정상이라 오탐이 난다(실측 2026-08-23: 한국투자증권 위탁의 수수료 0 매수 14
   * 건은 대부분 2020~2021 공모주). 계좌 전체가 0 이면 그 설명이 성립하지 않는다.
   */
  @Test
  void 수수료가_한_건도_없는_계좌를_찾는다() {
    UUID feeless = UUID.randomUUID();
    UUID normal = UUID.randomUUID();
    when(accountRepository.findByUserId(USER_ID))
        .thenReturn(List.of(account(feeless, "동양증권"), account(normal, "한국투자증권 위탁")));

    List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 12; i++) {
      trades.add(tradeIn(feeless, TradeType.BUY, "10000", 10, "0"));
    }
    trades.add(tradeIn(normal, TradeType.BUY, "10000", 10, "50"));

    LedgerIntegrityResponse response = run(List.of(), trades);

    LedgerIntegrityFinding found = finding(response, "ACCOUNT_WITHOUT_ANY_FEE");
    assertThat(found).isNotNull();
    assertThat(found.count()).as("수수료가 붙는 계좌까지 세면 안 된다").isEqualTo(1);
    assertThat(found.examples().get(0).stockItemName()).isEqualTo("동양증권");
    assertThat(found.examples().get(0).detail()).contains("거래=12건").contains("거래대금=1200000");
  }

  /** 한 건이라도 수수료가 있으면 그 계좌는 기록이 되고 있는 것이므로 걸리지 않는다. */
  @Test
  void 수수료가_한_건이라도_있으면_걸리지_않는다() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account(accountId, "계좌")));

    List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 12; i++) {
      trades.add(tradeIn(accountId, TradeType.BUY, "10000", 10, "0"));
    }
    trades.add(tradeIn(accountId, TradeType.BUY, "10000", 10, "1"));

    assertThat(finding(run(List.of(), trades), "ACCOUNT_WITHOUT_ANY_FEE")).isNull();
  }

  /** 거래가 몇 건뿐이면 공모주만 있는 계좌일 수 있어 우연이다. 그런 계좌까지 울리면 경고가 무의미해진다. */
  @Test
  void 거래가_적은_계좌는_걸리지_않는다() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account(accountId, "청약전용")));

    List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 9; i++) {
      trades.add(tradeIn(accountId, TradeType.BUY, "10000", 10, "0"));
    }

    assertThat(finding(run(List.of(), trades), "ACCOUNT_WITHOUT_ANY_FEE")).isNull();
  }

  /**
   * 계좌 설정에 <b>기록을 되받을 수 없다</b>고 밝힌 계좌는 지적하지 않는다.
   *
   * <p>지적은 고칠 수 있는 것만 남아야 한다. 폐쇄된 증권사 계좌처럼 원본이 사라진 경우는 영원히 고칠 수 없어서, 남겨 두면 점검 화면이 늘 빨간 상태가 되고 새로 생긴
   * 진짜 문제를 가린다(실측 2026-08-25: 동양증권 1 건이 유일하게 남은 계좌 지적이었다).
   *
   * <p>대신 사실은 계좌 상세 화면이 밝힌다 &mdash; gate {@code accountDetail.jte} 가 같은 키를 읽는다.
   */
  @Test
  void 기록을_되받을_수_없다고_밝힌_계좌는_지적하지_않는다() {
    UUID excused = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    var excusedAccount = account(excused, "동양증권");
    excusedAccount.setJsonConfig(java.util.Map.of("feeRecordsUnavailable", Boolean.TRUE));
    when(accountRepository.findByUserId(USER_ID))
        .thenReturn(List.of(excusedAccount, account(other, "다른 폐쇄 계좌")));

    List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 12; i++) {
      trades.add(tradeIn(excused, TradeType.BUY, "10000", 10, "0"));
      trades.add(tradeIn(other, TradeType.BUY, "10000", 10, "0"));
    }

    LedgerIntegrityFinding found = finding(run(List.of(), trades), "ACCOUNT_WITHOUT_ANY_FEE");

    assertThat(found).as("밝히지 않은 계좌까지 함께 빠지면 규칙이 죽은 것이다").isNotNull();
    assertThat(found.count()).as("밝힌 계좌가 그대로 지적되고 있다").isEqualTo(1);
    assertThat(found.examples().get(0).stockItemName()).isEqualTo("다른 폐쇄 계좌");
  }

  /**
   * 계좌 지적을 뺐다고 거래 단위 지적이 되살아나면 안 된다.
   *
   * <p>{@code SELL_WITHOUT_FEE_AND_TAX} 는 "계좌 단위로 이미 알렸으니 생략" 이라는 이유로 접혀 있다. 계좌 지적이 사라지는 순간 그 이유가
   * 흔들려, 접었던 열몇 건이 한꺼번에 돌아올 수 있다 &mdash; 지적을 줄이려던 설정이 오히려 늘리는 셈이 된다.
   */
  @Test
  void 계좌_지적을_빼도_매도_지적이_되살아나지_않는다() {
    UUID excused = UUID.randomUUID();
    var excusedAccount = account(excused, "동양증권");
    excusedAccount.setJsonConfig(java.util.Map.of("feeRecordsUnavailable", Boolean.TRUE));
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(excusedAccount));

    List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 9; i++) {
      Trade buy = trade(TradeType.BUY, 100, "10000", "0", "0", null);
      buy.setAccountId(excused);
      trades.add(buy);
    }
    Trade sell = trade(TradeType.SELL, 100, "100000", "0", "0", "0");
    sell.setAccountId(excused);
    trades.add(sell);
    Trade taxed = trade(TradeType.SELL, 100, "100000", "5000", "23000", "0");
    taxed.setAccountId(UUID.randomUUID());
    trades.add(taxed);

    LedgerIntegrityResponse response = run(List.of(), trades);

    assertThat(finding(response, "ACCOUNT_WITHOUT_ANY_FEE")).isNull();
    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX"))
        .as("계좌 지적을 뺀 대가로 거래 단위 지적이 열몇 건 돌아오면 안 된다")
        .isNull();
  }

  private net.luversof.api.stock.domain.MonthlyDividendPayout payout(
      String payDate, String perShare, String taxablePerShare) {
    var row = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    row.setStockItemId(STOCK_ITEM_ID);
    row.setPayDate(java.time.LocalDate.parse(payDate));
    row.setRecordDate(java.time.LocalDate.parse(payDate).minusDays(4));
    row.setDividendAmountPerShare(new BigDecimal(perShare));
    row.setTaxableBasePerShare(new BigDecimal(taxablePerShare));
    return row;
  }

  private Dividend dividendPaidOn(String payDate, String gross, String tax, Integer quantity) {
    Dividend dividend = dividend(gross, tax, gross, quantity, null);
    dividend.setPayDate(Instant.parse(payDate + "T00:00:00Z"));
    return dividend;
  }

  /** 과세표준을 따로 지정하는 배당. 저장 과세표준과 참조를 견주는 검사에 필요하다. */
  private Dividend dividendPaidOn(
      String payDate, String gross, String tax, Integer quantity, String taxable) {
    Dividend dividend = dividend(gross, tax, taxable, quantity, null);
    dividend.setPayDate(Instant.parse(payDate + "T00:00:00Z"));
    return dividend;
  }

  /** 주당 과세표준까지 채운 배당. */
  private Dividend dividendWithTaxBase(
      String gross,
      String tax,
      String taxable,
      Integer quantity,
      String perShare,
      String taxBasePerShare) {
    Dividend dividend = dividend(gross, tax, taxable, quantity, perShare);
    dividend.setTaxPerShare(taxBasePerShare == null ? null : new BigDecimal(taxBasePerShare));
    return dividend;
  }

  /** 날짜를 지정한 매도. 연도별 관측 요율 검사를 위해 필요하다. */
  private Trade sellOn(String date, int quantity, String price, String fee, String tax) {
    Trade trade = trade(TradeType.SELL, quantity, price, fee, tax, "0");
    trade.setTradeDate(Instant.parse(date + "T00:00:00Z"));
    return trade;
  }

  /**
   * 수수료·거래세가 둘 다 없는 매도에, 같은 해 관측 요율로 빠진 금액을 되짚어 주는지.
   *
   * <p>실측 2026-08-23: 동양증권 12 건이 전부 0 이었고, 같은 해에 거래세가 기록된 매도가 있는 것은 2020 년 2 건뿐이었다(관측 중앙값 0.2500%,
   * 그 2 건 기준). 나머지 10 건(2010~2019)은 원장에 표본이 없다.
   */
  @Test
  void 같은_해_관측_요율이_있으면_빠진_거래세를_되짚어_준다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                // 기준선: 이 해에 실제로 거래세를 낸 매도(1,000,000 의 0.25%)
                sellOn("2020-01-31", 100, "10000", "1000", "2500"),
                // 같은 해인데 둘 다 0
                sellOn("2020-01-28", 100, "10000", "0", "0")));

    LedgerIntegrityFinding found = finding(response, "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).as("수수료·거래세가 둘 다 없는 매도를 찾지 못했다").isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("얼마가 빠졌는지 원장 자신의 관측값으로 말해 줘야 한다")
        .contains("0.2500%")
        .contains("2500원이 빠졌다");
  }

  @Test
  void 같은_해_표본이_없으면_금액을_단언하지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                sellOn("2020-01-31", 100, "10000", "1000", "2500"),
                // 표본이 없는 해
                sellOn("2015-01-20", 100, "10000", "0", "0")));

    LedgerIntegrityFinding found = finding(response, "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    // 관측이 시작되는 해(2020)보다 오래된 매도이므로 그 해 세율을 하한으로 쓴다.
    assertThat(found.examples().get(0).detail())
        .as("세율은 해마다 낮아져 왔으므로 가장 이른 관측 연도의 세율은 그보다 오래된 매도의 하한이 된다")
        .contains("관측이 시작되는 2020년 세율")
        .contains("최소 2500원")
        .contains("실제는 이보다 크다");
    assertThat(found.examples().get(0).detail()).as("하한이지 단언이 아니다").doesNotContain("원이 빠졌다)");
  }

  /**
   * 매매 예시는 최신순이어야 한다.
   *
   * <p>실측 2026-08-23: 오래된 순이던 시절, 수수료·거래세가 없는 매도 12 건의 예시 3 개가 모두 2010 년 건이라 "원장만으로는 알 수 없다"만 세 번
   * 나왔고 금액을 되짚어 줄 수 있는 2020 년 2 건은 가려졌다.
   */
  @Test
  void 매매_예시는_최신_거래부터_보여_준다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                sellOn("2020-01-31", 100, "10000", "1000", "2500"),
                sellOn("2010-01-05", 100, "10000", "0", "0"),
                sellOn("2015-01-20", 100, "10000", "0", "0"),
                sellOn("2018-03-13", 100, "10000", "0", "0"),
                sellOn("2020-01-28", 100, "10000", "0", "0")));

    LedgerIntegrityFinding found = finding(response, "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    assertThat(found.count()).isEqualTo(4);
    assertThat(found.examples())
        .extracting("date")
        .containsExactly("2020-01-28", "2018-03-13", "2015-01-20");
    assertThat(found.examples().get(0).detail())
        .as("최신 건이라야 같은 해 관측 요율로 금액을 되짚어 줄 수 있다")
        .contains("2500원이 빠졌다");
  }

  /**
   * 기준일에 보유가 없으면, 그 수량을 마지막으로 보유한 날을 함께 알려 준다.
   *
   * <p>실측 2026-08-23 의 3 건은 모두 지급일이 기준일 자리에 들어간 것이었다 &mdash; NAVER 2021-04-08(300주)은 2020-01-30 매수
   * · 2021-01-18 매도라 2021-01-17 까지 들고 있었고, 그 직전 배당기준일 2020-12-31 에는 보유가 있었다. 진짜 기준일은 원장이 알 수 없으므로
   * 단언하지 않고, 언제까지 들고 있었는지만 알려 준다.
   */
  @Test
  void 마지막으로_보유한_날을_함께_알려_준다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2021-04-08", 300)),
            List.of(
                tradeOn("2020-01-30", TradeType.BUY, 300),
                tradeOn("2021-01-18", TradeType.SELL, 300)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("언제까지 들고 있었는지를 알려 줘야 사람이 바로 맞출 수 있다")
        .contains("이 수량을 마지막으로 보유한 날=2021-01-17");
  }

  /** 한 번도 그 수량에 닿은 적이 없으면 아무 날짜도 만들어 내지 않는다. */
  @Test
  void 그_수량을_보유한_적이_없으면_날짜를_지어내지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2021-04-08", 300)),
            List.of(
                tradeOn("2020-01-30", TradeType.BUY, 10),
                tradeOn("2021-01-18", TradeType.SELL, 10)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail()).doesNotContain("마지막으로 보유한 날");
  }

  /** 기준일 전에 여러 번 깨졌다면 가장 최근 것을 말해야 한다. */
  @Test
  void 기준일_전에_여러_번_깨졌으면_가장_최근을_말한다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2026-04-08", 43)),
            List.of(
                tradeOn("2019-12-13", TradeType.BUY, 43),
                tradeOn("2020-01-28", TradeType.SELL, 43),
                tradeOn("2023-08-01", TradeType.BUY, 74),
                tradeOn("2026-01-14", TradeType.SELL, 74)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("첫 번째로 깨진 2020-01-27 이 아니라 가장 최근인 2026-01-13 이어야 한다")
        .contains("이 수량을 마지막으로 보유한 날=2026-01-13");
  }

  /**
   * 기준일 이후의 재매수는 힌트에 영향을 주지 않는다.
   *
   * <p>나중에 다시 사서 지금도 들고 있다고 해서, 그 배당이 어느 시점의 것인지 되짚는 데 도움이 되지는 않는다. 실측 삼성SDI 가 이 모양이다 &mdash;
   * 2020-01-28 에 전량 매도한 뒤 2023-08-01 부터 다시 모았다.
   */
  @Test
  void 기준일_이후의_재매수는_힌트를_바꾸지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(dividendOn("2020-04-17", 43)),
            List.of(
                tradeOn("2019-12-13", TradeType.BUY, 43),
                tradeOn("2020-01-28", TradeType.SELL, 43),
                tradeOn("2023-08-01", TradeType.BUY, 74),
                // 기준일보다 한참 뒤의 매도. 이걸 세면 기준일 이후의 날짜를 답으로 내게 된다.
                tradeOn("2026-01-14", TradeType.SELL, 74)));

    LedgerIntegrityFinding found = finding(response, "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("기준일 이후에 다시 샀다고 해서 기준일 이전의 마지막 보유일이 사라지면 안 된다")
        .contains("이 수량을 마지막으로 보유한 날=2020-01-27");
    assertThat(found.examples().get(0).detail())
        .as("기준일보다 뒤의 날짜를 마지막 보유일이라고 말하면 안 된다")
        .doesNotContain("2026-01-13");
  }

  private net.luversof.api.stock.domain.MonthlyDividendSnapshot snapshot(String taxableRatio) {
    var snapshot = new net.luversof.api.stock.domain.MonthlyDividendSnapshot();
    snapshot.setStockItemId(STOCK_ITEM_ID);
    snapshot.setUserId(USER_ID);
    snapshot.setHeldQuantity(10256);
    snapshot.setAverageMonthlyDividendPerShare1y(new BigDecimal("30.50"));
    snapshot.setAverageTaxableBaseRatio1y(new BigDecimal(taxableRatio));
    snapshot.setAsOfDate(java.time.LocalDate.parse("2026-08-19"));
    return snapshot;
  }

  /** 계좌를 지정한 매수. 계좌별 수수료율 중앙값 검사를 위해 필요하다. */
  private Trade buyWithFee(
      java.util.UUID accountId, String amountPerShare, int quantity, String fee) {
    Trade trade = trade(TradeType.BUY, quantity, amountPerShare, fee, "0", "0");
    trade.setAccountId(accountId);
    return trade;
  }

  /**
   * 그 계좌의 평소 수수료율에서 크게 벗어난 거래.
   *
   * <p>실측 2026-08-23: 5 개 계좌 중 4 개는 최대치가 중앙값의 1.0~1.2 배인데 한국투자증권 위탁만 152.5 배인 거래가 하나 있었다
   * (2025-09-03 거래대금 12,235 · 수수료 78 = 0.6375%, 계좌 중앙값 0.0042%).
   */
  @Test
  void 계좌_평소_수수료율에서_크게_벗어난_거래를_찾는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    // 평소: 100,000 에 4 원 = 0.004%
    for (int i = 0; i < 6; i++) {
      trades.add(buyWithFee(accountId, "1000", 100, "4"));
    }
    // 이상: 같은 금액에 400 원 = 0.4% (100 배)
    trades.add(buyWithFee(accountId, "1000", 100, "400"));

    LedgerIntegrityResponse response = run(List.of(), trades);

    LedgerIntegrityFinding found = finding(response, "TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN");
    assertThat(found).as("계좌 중앙값에서 크게 벗어난 수수료를 찾지 못했다").isNotNull();
    assertThat(found.count()).isEqualTo(1);
    assertThat(found.examples().get(0).detail())
        .as("무엇과 견줘 이상한지, 최소수수료로 설명되는지까지 적어야 한다")
        .contains("이 계좌 중앙값")
        .contains("최저 수수료는 4원");
  }

  @Test
  void 평소_수수료율과_비슷하면_걸리지_않는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) {
      trades.add(buyWithFee(accountId, "1000", 100, "4"));
    }
    // 1.5 배 정도의 흔들림은 정상이다.
    trades.add(buyWithFee(accountId, "1000", 100, "6"));

    assertThat(finding(run(List.of(), trades), "TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN")).isNull();
  }

  /**
   * 배수 임계(10 배) 바로 아래는 걸리지 않는다.
   *
   * <p>1.5 배만 재면 임계를 10 에서 2 로 낮춰도 검사가 통과한다(실제로 그렇게 뮤테이션이 살아남아 추가했다). 임계가 어디인지 못박으려면 그 <b>바로
   * 아래</b>를 재야 한다.
   */
  @Test
  void 배수_임계_바로_아래는_걸리지_않는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) {
      trades.add(buyWithFee(accountId, "1000", 100, "4"));
    }
    // 8 배 - 흔들림치고는 크지만 10 배 임계 아래다.
    trades.add(buyWithFee(accountId, "1000", 100, "32"));

    assertThat(finding(run(List.of(), trades), "TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN")).isNull();
  }

  /** 거래가 적은 계좌는 중앙값을 믿을 수 없으므로 판정하지 않는다. */
  @Test
  void 거래가_적은_계좌는_수수료율을_판정하지_않는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 3; i++) {
      trades.add(buyWithFee(accountId, "1000", 100, "4"));
    }
    trades.add(buyWithFee(accountId, "1000", 100, "400"));

    assertThat(finding(run(List.of(), trades), "TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN")).isNull();
  }

  /**
   * 관측이 시작되는 해보다 <b>나중</b>인데 그 해 표본만 없는 경우는 하한을 말할 수 없다.
   *
   * <p>세율이 해마다 낮아져 왔다는 근거는 "더 오래된 매도" 에만 적용된다. 관측 구간 안쪽의 빈 해에 가장 이른 해의 세율을 적용하면 하한이 아니라 과대추정이 된다.
   */
  @Test
  void 관측_구간_안쪽의_빈_해는_하한을_말하지_않는다() {
    LedgerIntegrityResponse response =
        run(
            List.of(),
            List.of(
                sellOn("2020-01-31", 100, "10000", "1000", "2500"),
                sellOn("2026-03-02", 100, "10000", "2000", "2000"),
                // 2020 과 2026 사이의 빈 해
                sellOn("2023-05-10", 100, "10000", "0", "0")));

    LedgerIntegrityFinding found = finding(response, "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("관측 구간 안쪽이면 가장 이른 해의 세율이 하한 근거가 되지 못한다")
        .contains("원장만으로는 알 수 없다");
  }

  /** 계좌·날짜를 지정한 매수. */
  private Trade buyOn(
      java.util.UUID accountId, String date, String price, int quantity, String fee) {
    Trade trade = trade(TradeType.BUY, quantity, price, fee, "0", "0");
    trade.setAccountId(accountId);
    trade.setTradeDate(Instant.parse(date + "T00:00:00Z"));
    return trade;
  }

  /**
   * 수수료가 한 건도 없는 계좌에 "적어도 얼마가" 빠졌는지 되짚는지.
   *
   * <p>실측 2026-08-23: 동양증권 26 건의 거래별 하한을 더할 수 있다. 수수료율은 해마다 낮아져 왔으므로(2019 0.004458% → 2026
   * 0.002172%) 그 해 최저값은 하한이 된다.
   */
  @Test
  void 수수료가_없는_계좌에_빠진_금액의_하한을_적는다() {
    java.util.UUID feeless = java.util.UUID.randomUUID();
    java.util.UUID normal = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    // 기준선 계좌. 2020 년 관측: 1,000,000 에 수수료 40(0.004%) 과 60(0.006%) 이 섞여 있다.
    // 하한이므로 <b>최저</b> 0.004% 를 써야 한다(최대를 쓰면 0.006% 가 되어 하한이 아니다).
    for (int i = 0; i < 3; i++) {
      trades.add(buyOn(normal, "2020-03-0" + (i + 1), "10000", 100, "40"));
    }
    for (int i = 0; i < 2; i++) {
      trades.add(buyOn(normal, "2020-04-0" + (i + 1), "10000", 100, "60"));
    }

    // 수수료가 하나도 없는 계좌.
    //   - 2020 년 8 건: 그 해 관측 최저 0.004% -> 건당 40
    //   - 2015 년 1 건: 관측 이전이므로 최초 관측 연도(2020) 값을 하한으로 -> 40
    //   - 2024 년 1 건: 관측이 없고 최초 연도보다 <b>뒤</b> 이므로 셈에서 빠진다
    for (int i = 0; i < 8; i++) {
      trades.add(buyOn(feeless, "2020-05-1" + i, "10000", 100, "0"));
    }
    trades.add(buyOn(feeless, "2015-06-01", "10000", 100, "0"));
    trades.add(buyOn(feeless, "2024-05-31", "10000", 100, "0"));

    LedgerIntegrityFinding found = finding(run(List.of(), trades), "ACCOUNT_WITHOUT_ANY_FEE");
    assertThat(found).as("수수료가 한 건도 없는 계좌를 찾지 못했다").isNotNull();
    // (8 + 1) 건 x 1,000,000 x 0.004% = 360. 2024 년 건은 빠진다.
    assertThat(found.examples().get(0).detail())
        .as("최저 요율로, 관측 이전 거래까지만 세어야 한다")
        .contains("최소 360원")
        .contains("실제는 이보다 크다");
  }

  /** 수수료가 기록된 거래가 아예 없으면 하한을 말할 수 없다. */
  @Test
  void 관측된_수수료가_없으면_하한을_말하지_않는다() {
    java.util.UUID feeless = java.util.UUID.randomUUID();
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 10; i++) {
      trades.add(buyOn(feeless, "2020-05-1" + i, "10000", 100, "0"));
    }

    LedgerIntegrityFinding found = finding(run(List.of(), trades), "ACCOUNT_WITHOUT_ANY_FEE");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail()).doesNotContain("최소");
  }

  /**
   * 예시 개수를 호출자가 정할 수 있어야 한다.
   *
   * <p>기본 3 건만으로는 조치할 수 없는 발견이 생긴다 &mdash; 실측 2026-08-23: 발견 45 건 중 25 건(55%)이 예시 밖이라 화면에서 어느 행인지 볼
   * 수 없었다(수수료·거래세가 없는 매도 12 건 중 9 건 등).
   */
  @Test
  void 예시_개수를_늘리면_더_많이_담는다() {
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 8; i++) {
      // 매수인데 실현손익이 0 이 아닌 거래 - 규칙 하나로 8 건을 만든다.
      trades.add(trade(TradeType.BUY, 10, "1000", "10", "0", "500"));
    }
    when(dividendService.findDividends(any())).thenReturn(List.of());
    when(tradeService.findByUserId(USER_ID)).thenReturn(trades);

    assertThat(finding(service().check(USER_ID), "TRADE_BUY_WITH_REALIZED_PROFIT").examples())
        .as("기본값은 3 건이다")
        .hasSize(3);
    assertThat(finding(service().check(USER_ID, 8), "TRADE_BUY_WITH_REALIZED_PROFIT").examples())
        .as("요청한 만큼 담아야 조치할 수 있다")
        .hasSize(8);
  }

  @Test
  void 예시_개수는_기본값과_상한을_지킨다() {
    java.util.List<Trade> trades = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
      trades.add(trade(TradeType.BUY, 10, "1000", "10", "0", "500"));
    }
    when(dividendService.findDividends(any())).thenReturn(List.of());
    when(tradeService.findByUserId(USER_ID)).thenReturn(trades);

    assertThat(finding(service().check(USER_ID, 0), "TRADE_BUY_WITH_REALIZED_PROFIT").examples())
        .as("0 이나 음수는 기본값으로 본다")
        .hasSize(3);
    assertThat(finding(service().check(USER_ID, -5), "TRADE_BUY_WITH_REALIZED_PROFIT").examples())
        .hasSize(3);
    // 상한을 넘겨도 있는 만큼만 담긴다(응답이 무한정 커지지 않는다).
    assertThat(
            finding(
                    service().check(USER_ID, LedgerIntegrityService.MAX_EXAMPLES_LIMIT + 500),
                    "TRADE_BUY_WITH_REALIZED_PROFIT")
                .examples())
        .hasSize(5);
  }

  /**
   * 수수료·거래세가 없는 매도에 계좌명을 적는다.
   *
   * <p>실측 2026-08-23: 12 건이 <b>전부 동양증권</b>이었고, 누락 하한은 그 계좌 실현손익의 2.40% 다(수익률 12.01% -> 11.73%).
   * 계좌명이 없으면 같은 계좌의 {@code ACCOUNT_WITHOUT_ANY_FEE} 와 이어지지 않아, 한 계좌에 몰린 문제인지 흩어진 문제인지 알 수 없다.
   */
  @Test
  void 수수료가_없는_매도에_계좌명을_적는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    var account = new net.luversof.api.stock.domain.Account();
    account.setId(accountId);
    account.setUserId(USER_ID);
    account.setName("동양증권");
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));

    Trade baseline = sellOn("2020-01-31", 100, "10000", "1000", "2500");
    baseline.setAccountId(java.util.UUID.randomUUID());
    Trade zero = sellOn("2020-01-28", 100, "10000", "0", "0");
    zero.setAccountId(accountId);

    LedgerIntegrityFinding found =
        finding(run(List.of(), List.of(baseline, zero)), "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail())
        .as("어느 계좌인지 알아야 같은 계좌의 다른 발견과 이어진다")
        .contains("[동양증권]");
  }

  @Test
  void 계좌를_모르면_대괄호를_붙이지_않는다() {
    Trade baseline = sellOn("2020-01-31", 100, "10000", "1000", "2500");
    Trade zero = sellOn("2020-01-28", 100, "10000", "0", "0");

    LedgerIntegrityFinding found =
        finding(run(List.of(), List.of(baseline, zero)), "SELL_WITHOUT_FEE_AND_TAX");
    assertThat(found).isNotNull();
    assertThat(found.examples().get(0).detail()).doesNotContain("[");
  }

  /**
   * 계좌를 모르는 행은 계좌별 집계에 넣지 않는다.
   *
   * <p>실데이터에는 accountId 없는 거래·배당이 0 건이라(실측 2026-08-23: 매매 250 / 배당 193 모두 계좌가 있다) 이 분기는 원장으로 확인할 수
   * 없다. 그래도 도메인상 nullable 이므로 여기서 지킨다 &mdash; "?" 같은 가짜 계좌가 집계에 섞이면 "어느 계좌를 고칠지" 가 흐려진다.
   */
  @Test
  void 계좌를_모르는_행은_계좌별_집계에_넣지_않는다() {
    java.util.UUID accountId = java.util.UUID.randomUUID();
    var account = new net.luversof.api.stock.domain.Account();
    account.setId(accountId);
    account.setUserId(USER_ID);
    account.setName("동양증권");
    when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));

    Trade baseline = sellOn("2020-01-31", 100, "10000", "1000", "2500");
    baseline.setAccountId(accountId);
    Trade withAccount = sellOn("2020-01-28", 100, "10000", "0", "0");
    withAccount.setAccountId(accountId);
    Trade withoutAccount = sellOn("2020-01-27", 100, "10000", "0", "0");
    withoutAccount.setAccountId(null);

    LedgerIntegrityResponse response =
        run(List.of(), List.of(baseline, withAccount, withoutAccount));

    assertThat(finding(response, "SELL_WITHOUT_FEE_AND_TAX").count())
        .as("발견 자체는 두 건이다")
        .isEqualTo(2);
    assertThat(response.accountSummary()).hasSize(1);
    assertThat(response.accountSummary().get(0).accountName()).isEqualTo("동양증권");
    assertThat(response.accountSummary().get(0).findingCount())
        .as("계좌를 아는 한 건만 세야 한다")
        .isEqualTo(1);
  }
}
