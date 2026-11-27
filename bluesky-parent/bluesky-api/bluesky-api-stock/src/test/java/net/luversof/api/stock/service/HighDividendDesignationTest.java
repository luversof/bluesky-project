package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.HighDividendDesignation;
import net.luversof.api.stock.web.dto.request.HighDividendDesignationUpsertRequest;

/** 고배당기업 지정: 지급일 기간 판정과 저장 검증. */
class HighDividendDesignationTest {

  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID OTHER_ITEM = UUID.randomUUID();

  private static HighDividendDesignation designation(LocalDate from, LocalDate to) {
    HighDividendDesignation d = new HighDividendDesignation();
    d.setStockItemId(ITEM);
    d.setBusinessYear(2026);
    d.setPayDateFrom(from);
    d.setPayDateTo(to);
    return d;
  }

  @Test
  void 기간의_시작일과_끝일을_포함한다() {
    HighDividendDesignation d = designation(LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30));

    assertThat(d.covers(ITEM, LocalDate.of(2026, 5, 1))).isTrue();
    assertThat(d.covers(ITEM, LocalDate.of(2027, 4, 30))).isTrue();
    assertThat(d.covers(ITEM, LocalDate.of(2026, 4, 30))).isFalse();
    assertThat(d.covers(ITEM, LocalDate.of(2027, 5, 1))).isFalse();
  }

  @Test
  void 다른_종목이나_빈_값은_덮지_않는다() {
    HighDividendDesignation d = designation(LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30));

    assertThat(d.covers(OTHER_ITEM, LocalDate.of(2026, 8, 1))).isFalse();
    assertThat(d.covers(null, LocalDate.of(2026, 8, 1))).isFalse();
    assertThat(d.covers(ITEM, null)).isFalse();
  }

  @Test
  void 목록_중_하나라도_덮으면_고배당이다() {
    List<HighDividendDesignation> list =
        List.of(
            designation(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)),
            designation(LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30)));

    assertThat(HighDividendDesignation.isHighDividend(list, ITEM, LocalDate.of(2026, 4, 17)))
        .isTrue();
    assertThat(HighDividendDesignation.isHighDividend(list, ITEM, LocalDate.of(2027, 8, 1)))
        .isFalse();
    assertThat(HighDividendDesignation.isHighDividend(null, ITEM, LocalDate.of(2026, 4, 17)))
        .isFalse();
  }

  @Test
  void 시작일이_끝일보다_늦으면_거절한다() {
    var request =
        new HighDividendDesignationUpsertRequest(
            null, "000000", 2026, LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1), null);

    assertThatThrownBy(() -> HighDividendDesignationService.validate(request))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void 필수값이_빠지면_거절한다() {
    assertThatThrownBy(() ->
            HighDividendDesignationService.validate(
                new HighDividendDesignationUpsertRequest(
                    null, " ", 2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() ->
            HighDividendDesignationService.validate(
                new HighDividendDesignationUpsertRequest(
                    null,
                    "000000",
                    null,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    null)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void 같은_날_하루짜리_기간은_받는다() {
    HighDividendDesignationService.validate(
        new HighDividendDesignationUpsertRequest(
            null, "000000", 2026, LocalDate.of(2026, 4, 17), LocalDate.of(2026, 4, 17), "memo"));
  }
}
