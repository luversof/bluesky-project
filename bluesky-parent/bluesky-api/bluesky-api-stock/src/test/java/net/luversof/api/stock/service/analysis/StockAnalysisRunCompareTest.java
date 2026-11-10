package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPrediction;
import net.luversof.api.stock.domain.StockPredictionGrade;
import net.luversof.api.stock.service.analysis.StockAnalysisService.RunComparison;
import net.luversof.api.stock.service.analysis.StockAnalysisService.RunRow;

class StockAnalysisRunCompareTest {

  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID RUN1 = UUID.randomUUID();
  private static final UUID RUN2 = UUID.randomUUID();

  private static StockPrediction prediction(UUID run, String created, int horizon, String dir) {
    StockPrediction p = new StockPrediction();
    p.setId(UUID.randomUUID());
    p.setRunId(run);
    p.setStockItemId(ITEM);
    p.setCreatedDate(Instant.parse(created));
    p.setHorizonDays(horizon);
    p.setDirection(dir);
    p.setConfidence(40);
    p.setPromptVersion("v2");
    p.setModelId("sonnet");
    p.setBaseDate(LocalDate.of(2026, 11, 10));
    return p;
  }

  @Test
  void 회차를_열로_놓고_방향이_갈린_행을_표시한다() {
    StockPrediction a1 = prediction(RUN1, "2026-11-10T11:07:00Z", 1, "FLAT");
    StockPrediction a5 = prediction(RUN1, "2026-11-10T11:07:00Z", 5, "UP");
    StockPrediction b1 = prediction(RUN2, "2026-11-10T11:18:00Z", 1, "UP");
    StockPrediction b5 = prediction(RUN2, "2026-11-10T11:18:00Z", 5, "UP");
    StockPredictionGrade g = new StockPredictionGrade();
    g.setPredictionId(b1.getId());
    g.setHit(true);
    g.setActualDirection("UP");
    StockItem item = new StockItem();
    item.setId(ITEM);
    item.setSymbol("329200");
    item.setName("TIGER 리츠부동산인프라");

    RunComparison result =
        StockAnalysisService.compare(
            LocalDate.of(2026, 11, 10),
            List.of(LocalDate.of(2026, 11, 10)),
            List.of(b5, a1, b1, a5),
            Map.of(RUN1, 1, RUN2, 2),
            Map.of(b1.getId(), g),
            Map.of(ITEM, item));

    assertThat(result.runs()).extracting(r -> r.seq()).containsExactly(1, 2);
    assertThat(result.runs().get(0).flatPct()).isEqualByComparingTo("50");
    assertThat(result.runs().get(1).hitRatePct()).isEqualByComparingTo("100");
    assertThat(result.runs().get(0).hitRatePct()).isNull();

    RunRow d1 = result.rows().get(0);
    RunRow d5 = result.rows().get(1);
    assertThat(d1.horizonDays()).isEqualTo(1);
    assertThat(d1.disagree()).isTrue();
    assertThat(d1.cells().get(RUN2.toString()).hit()).isTrue();
    assertThat(d1.cells().get(RUN1.toString()).hit()).isNull();
    assertThat(d5.disagree()).isFalse();
  }
}
