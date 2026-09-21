package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.kis.KisStockPriceUpdateService.DateRange;

/**
 * 시세 이력을 어느 구간으로 받을지(사용자 결정 2026-09-21).
 *
 * <p>예전 대상은 (매매 · 배당 이력이 있는 종목) ∪ (현재 보유) ∪ (갱신 표시된 날) 뿐이라, 매매도 배당도 없는 월배당 프로필 종목은 아예 갱신되지 않았다
 * &mdash; 실측: 등록 12 종목 중 미보유 4 종목이 시세 없음 2 · 171 일 전 1 · 173 일 전 1. 목록 화면은 "현재가 기준 연배당 수익률" 로 종목을
 * 견주므로 보유하지 않아도 현재가가 있어야 한다.
 */
class KisStockPriceWindowTest {

  /** 2026-09-21 은 월요일. */
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

  @Test
  void 월배당_프로필_종목은_이력이_없어도_두해치를_받는다() {
    DateRange window =
        KisStockPriceUpdateService.resolvePriceWindow(TODAY, null, null, false, true);

    assertThat(window.end()).as("현재가가 목적이니 끝은 오늘").isEqualTo(TODAY);
    assertThat(window.start())
        .as("1 · 3 · 6 · 12 개월 수익률을 내려면 그만큼의 이력이 있어야 한다(사용자 결정 2026-09-21)")
        .isEqualTo(LocalDate.of(2024, 9, 21));
    assertThat(java.time.temporal.ChronoUnit.DAYS.between(window.start(), window.end()))
        .as("2 년")
        .isEqualTo(365 * 2);
  }

  @Test
  void 프로필이_아니고_이력도_없으면_예전처럼_오늘_하루다() {
    DateRange window =
        KisStockPriceUpdateService.resolvePriceWindow(TODAY, null, null, false, false);

    assertThat(window.start()).isEqualTo(TODAY);
    assertThat(window.end()).isEqualTo(TODAY);
  }

  /** 미보유 종목은 마지막 거래 · 배당일까지만 받는 게 원래 규칙이다(그 뒤 가격은 쓸 데가 없다). */
  @Test
  void 미보유_종목은_마지막_거래일까지만_받는다() {
    LocalDate first = LocalDate.of(2016, 3, 2);
    LocalDate last = LocalDate.of(2026, 4, 1);

    DateRange plain =
        KisStockPriceUpdateService.resolvePriceWindow(TODAY, first, last, false, false);
    assertThat(plain.start()).isEqualTo(first);
    assertThat(plain.end()).as("예전 규칙 그대로").isEqualTo(last);

    DateRange profile =
        KisStockPriceUpdateService.resolvePriceWindow(TODAY, first, last, false, true);
    assertThat(profile.start()).as("원장이 2 년보다 앞서면 그 첫날부터").isEqualTo(first);

    // 원장이 최근이면(올해 샀으면) 그래도 2 년 전부터 받는다 - 기간 수익률의 기준을 종목마다 맞추기 위해서다.
    DateRange recentLedger =
        KisStockPriceUpdateService.resolvePriceWindow(
            TODAY, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 9, 18), true, true);
    assertThat(recentLedger.start()).isEqualTo(LocalDate.of(2024, 9, 21));
    assertThat(profile.end())
        .as("프로필 종목은 미보유라도 오늘까지 - 끊긴 날부터 오늘까지 한 번에 메워진다(실측 0052D0 은 2026-04-01 에서 멈춰 있었다)")
        .isEqualTo(TODAY);
  }

  @Test
  void 보유_중이면_프로필이_아니어도_오늘까지다() {
    DateRange window =
        KisStockPriceUpdateService.resolvePriceWindow(
            TODAY, LocalDate.of(2020, 1, 2), LocalDate.of(2026, 9, 18), true, false);

    assertThat(window.end()).isEqualTo(TODAY);
  }
}
