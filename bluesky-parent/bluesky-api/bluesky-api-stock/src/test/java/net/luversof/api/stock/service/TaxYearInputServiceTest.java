package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.TaxYearInput;
import net.luversof.api.stock.repository.TaxYearInputRepository;
import net.luversof.api.stock.web.dto.request.TaxYearInputUpsertRequest;
import net.luversof.api.stock.web.dto.response.TaxYearInputResponse;

/** 귀속연도 세금 입력값: 사용자 · 연도마다 한 줄, 비운 값은 null 로 남는다. */
class TaxYearInputServiceTest {

  private static final UUID USER = UUID.randomUUID();

  private final Map<String, TaxYearInput> store = new HashMap<>();
  private TaxYearInputService service;

  @BeforeEach
  void setUp() {
    TaxYearInputRepository repository = mock(TaxYearInputRepository.class);
    when(repository.findByUserIdAndYear(any(), any(Integer.class)))
        .thenAnswer(
            inv -> Optional.ofNullable(store.get(inv.getArgument(0) + "@" + inv.getArgument(1))));
    when(repository.save(any(TaxYearInput.class)))
        .thenAnswer(inv -> {
          TaxYearInput row = inv.getArgument(0);
          if (row.getId() == null) {
            row.setId(UUID.randomUUID());
          }
          store.put(row.getUserId() + "@" + row.getYear(), row);
          return row;
        });
    service = new TaxYearInputService();
    ReflectionTestUtils.setField(service, "taxYearInputRepository", repository);
  }

  @Test
  void 저장한_적이_없으면_두_값이_null_이다() {
    TaxYearInputResponse response = service.find(USER, 2026);

    assertThat(response.year()).isEqualTo(2026);
    assertThat(response.otherIncomeTaxBase()).isNull();
    assertThat(response.otherFinancialIncome()).isNull();
  }

  @Test
  void 같은_해에_다시_저장하면_한_줄을_고친다() {
    service.upsert(new TaxYearInputUpsertRequest(USER, 2026, new BigDecimal("1000"), null));
    UUID firstId = store.get(USER + "@2026").getId();

    service.upsert(
        new TaxYearInputUpsertRequest(USER, 2026, new BigDecimal("2000"), new BigDecimal("300")));

    assertThat(store).hasSize(1);
    assertThat(store.get(USER + "@2026").getId()).isEqualTo(firstId);
    TaxYearInputResponse response = service.find(USER, 2026);
    assertThat(response.otherIncomeTaxBase()).isEqualByComparingTo("2000");
    assertThat(response.otherFinancialIncome()).isEqualByComparingTo("300");
  }

  @Test
  void 비운_값은_0_이_아니라_null_로_남는다() {
    service.upsert(new TaxYearInputUpsertRequest(USER, 2026, BigDecimal.ZERO, null));

    TaxYearInputResponse response = service.find(USER, 2026);
    assertThat(response.otherIncomeTaxBase()).isEqualByComparingTo("0");
    assertThat(response.otherFinancialIncome()).isNull();
  }

  @Test
  void 해마다_따로_저장한다() {
    service.upsert(new TaxYearInputUpsertRequest(USER, 2025, new BigDecimal("1"), null));
    service.upsert(new TaxYearInputUpsertRequest(USER, 2026, new BigDecimal("2"), null));

    assertThat(service.find(USER, 2025).otherIncomeTaxBase()).isEqualByComparingTo("1");
    assertThat(service.find(USER, 2026).otherIncomeTaxBase()).isEqualByComparingTo("2");
  }

  @Test
  void 잘못된_요청은_거절한다() {
    assertThatThrownBy(() -> service.find(null, 2026)).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> service.find(USER, null)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() ->
            service.upsert(new TaxYearInputUpsertRequest(USER, 2026, new BigDecimal("-1"), null)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.upsert(new TaxYearInputUpsertRequest(null, 2026, null, null)))
        .isInstanceOf(RuntimeException.class);
  }
}
