package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import net.luversof.api.stock.domain.StockPrediction;
import net.luversof.api.stock.domain.StockPredictionGrade;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockPredictionGradeRepository;
import net.luversof.api.stock.repository.StockPredictionRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;

/** 시세가 KRX 종가로 고쳐지면 이미 매긴 채점도 다시 매긴다(2026-11-21, 저녁 채점이 NXT 가 섞인 종가로 굳었다). */
class StockAnalysisRegradeTest {

  private final StockPredictionRepository predictions = mock(StockPredictionRepository.class);
  private final StockPredictionGradeRepository grades = mock(StockPredictionGradeRepository.class);
  private final StockPriceHistoryRepository histories = mock(StockPriceHistoryRepository.class);
  private final StockAnalysisService service = new StockAnalysisService();
  private final UUID itemId = UUID.randomUUID();

  StockAnalysisRegradeTest() {
    ReflectionTestUtils.setField(service, "stockPredictionRepository", predictions);
    ReflectionTestUtils.setField(service, "stockPredictionGradeRepository", grades);
    ReflectionTestUtils.setField(service, "stockPriceHistoryRepository", histories);
    when(predictions.findUngraded()).thenReturn(List.of());
  }

  private static LocalDate d(String s) {
    return LocalDate.parse(s);
  }

  private StockPriceHistory close(String date, String price) {
    StockPriceHistory h = new StockPriceHistory();
    h.setStockItemId(itemId);
    h.setTradeDate(d(date));
    h.setClosePrice(new BigDecimal(price));
    return h;
  }

  /** 11-18 기준 D+1(만기 11-19): 기준가 196,400(NXT 종가)로 저장 · 채점됐다. */
  private StockPredictionGrade setUp(String baseClose, String targetClose, String gradedClose) {
    StockPrediction p = new StockPrediction();
    p.setId(UUID.randomUUID());
    p.setStockItemId(itemId);
    p.setBaseDate(d("2026-11-18"));
    p.setBasePrice(new BigDecimal("196400"));
    p.setHorizonDays(1);
    p.setDirection("FLAT");
    p.setCreatedDate(java.time.Instant.parse("2026-11-18T23:30:00Z"));
    StockPredictionGrade g = new StockPredictionGrade();
    g.setPredictionId(p.getId());
    g.setTargetDate(d("2026-11-19"));
    g.setActualClose(new BigDecimal(gradedClose));
    g.setActualDirection("FLAT");
    g.setHit(true);
    when(grades.findByTargetDateGreaterThanEqual(any())).thenReturn(List.of(g));
    when(predictions.findAllById(List.of(p.getId()))).thenReturn(List.of(p));
    when(histories.findTop2ByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
            eq(itemId), eq(d("2026-11-19"))))
        .thenReturn(List.of(close("2026-11-19", targetClose), close("2026-11-18", baseClose)));
    when(histories.findTop2ByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
            eq(itemId), eq(d("2026-11-18"))))
        .thenReturn(List.of(close("2026-11-18", baseClose), close("2026-11-17", "190300")));
    return g;
  }

  @Test
  void 기준가가_KRX_종가로_고쳐지면_다시_매긴다() {
    // 196,400 대비 196,900 은 +0.25% FLAT 이었지만 KRX 종가 194,700 대비 +1.13% UP
    StockPredictionGrade g = setUp("194700", "196900", "196900");
    assertThat(service.grade(10).regraded()).isEqualTo(1);
    assertThat(g.getActualDirection()).isEqualTo("UP");
    assertThat(g.isHit()).isFalse();
    verify(predictions).save(any());
    verify(grades).save(g);
  }

  @Test
  void 종가가_그대로면_건드리지_않는다() {
    setUp("196400", "196900", "196900");
    assertThat(service.grade(10).regraded()).isZero();
    verify(grades, never()).save(any());
    verify(predictions, never()).save(any());
  }
}
