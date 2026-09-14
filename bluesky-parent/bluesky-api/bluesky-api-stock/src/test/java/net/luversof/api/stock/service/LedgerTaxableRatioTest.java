package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;

/**
 * 과세비율은 그 사용자의 <b>원장 실적</b>으로 낸다 - 합을 먼저 내고 한 번 나눈다.
 *
 * <p>참조 지급 이력의 (주당 과세표준 / 주당 배당)은 종목 하나에 값 하나뿐이라 계좌별 혜택을 담지 못한다. 비과세 계좌(ISA·연금저축)의 배당은 과세금액이 0 이고,
 * 리츠 ETF 는 계좌에 따라 분리과세가 걸린다. 그래서 같은 종목이라도 참조 기준과 원장 기준이 크게 갈린다 - 서비스 주석의 실측(2026-08-24)으로 KODEX
 * 한국부동산리츠인프라는 참조 77.64% 대 원장 17.35% 였다.
 *
 * <p>이 계산에 테스트가 없었다. 시뮬레이터의 예상 과세표준액이 이 값을 그대로 곱해 쓰므로, 나누는 방식이 바뀌면(행별 비율의 평균 같은 것으로) 화면의 세금 추정이 조용히
 * 달라진다.
 */
@ExtendWith(MockitoExtension.class)
class LedgerTaxableRatioTest {

  @Mock private DividendService dividendService;

  @InjectMocks private MonthlyDividendPayoutService monthlyDividendPayoutService;

  private final UUID userId = UUID.randomUUID();
  private final UUID stockItemId = UUID.randomUUID();

  private Dividend dividend(String gross, String taxable) {
    Dividend dividend = new Dividend();
    dividend.setId(UUID.randomUUID());
    dividend.setStockItemId(stockItemId);
    dividend.setGrossAmount(new BigDecimal(gross));
    dividend.setTaxableAmount(taxable == null ? null : new BigDecimal(taxable));
    dividend.setPayDate(Instant.parse("2026-08-19T00:00:00Z"));
    return dividend;
  }

  private BigDecimal ratioOf(List<Dividend> rows) {
    when(dividendService.findDividends(any(DividendSearchRequest.class))).thenReturn(rows);
    return monthlyDividendPayoutService.ledgerTaxableBaseRatio1y(userId, stockItemId);
  }

  /**
   * 합을 먼저 내고 한 번 나눈다.
   *
   * <p>행별 비율의 평균이면 (100 + 0) / 2 = 50% 지만, 금액 가중으로는 100 / 1,100 = 9.09% 다. 큰 배당 한 건이 작은 건과 같은 무게로
   * 취급되면 세금 추정이 통째로 틀어진다.
   */
  @Test
  void 행별_평균이_아니라_금액_합으로_나눈다() {
    BigDecimal ratio = ratioOf(List.of(dividend("100", "100"), dividend("1000", "0")));
    assertThat(ratio).isEqualByComparingTo(new BigDecimal("9.09"));
  }

  /** 비과세 계좌만 있으면 0% 다 - 값이 없는 것과 다르다. */
  @Test
  void 과세금액이_모두_0이면_0퍼센트다() {
    assertThat(ratioOf(List.of(dividend("500", "0"), dividend("700", "0"))))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** 과세금액이 비어 있으면 0 으로 읽는다 - 한 행이 널이라고 전체가 터지면 안 된다. */
  @Test
  void 과세금액이_널이면_0으로_읽는다() {
    assertThat(ratioOf(List.of(dividend("400", null), dividend("600", "300"))))
        .isEqualByComparingTo(new BigDecimal("30.00"));
  }

  /**
   * 세전 합이 0 이면 널이다.
   *
   * <p>0% 를 돌려주면 호출부가 그 값을 저장해 "과세표준 없음"으로 굳는다. 널이어야 참조 지급 이력의 값을 그대로 쓴다.
   */
  @Test
  void 세전_합이_0이면_널이다() {
    assertThat(ratioOf(List.of(dividend("0", "0")))).isNull();
    assertThat(ratioOf(List.of())).isNull();
  }

  @Test
  void 사용자나_종목이_없으면_널이다() {
    assertThat(monthlyDividendPayoutService.ledgerTaxableBaseRatio1y(null, stockItemId)).isNull();
    assertThat(monthlyDividendPayoutService.ledgerTaxableBaseRatio1y(userId, null)).isNull();
  }

  /** 창은 최근 1 년이고, 그 종목만 본다 - 다른 종목이 섞이면 비율이 남의 것이 된다. */
  @Test
  void 최근_1년_그_종목만_묻는다() {
    Instant before = Instant.now();
    ratioOf(List.of(dividend("100", "50")));

    ArgumentCaptor<DividendSearchRequest> captor =
        ArgumentCaptor.forClass(DividendSearchRequest.class);
    verify(dividendService).findDividends(captor.capture());
    DividendSearchRequest request = captor.getValue();
    assertThat(request.getUserId()).isEqualTo(userId);
    assertThat(request.getStockItemIdList()).containsExactly(stockItemId);
    assertThat(request.getStartDate())
        .as("최근 1 년")
        .isBetween(before.minus(Duration.ofDays(366)), Instant.now().minus(Duration.ofDays(364)));
  }
}
