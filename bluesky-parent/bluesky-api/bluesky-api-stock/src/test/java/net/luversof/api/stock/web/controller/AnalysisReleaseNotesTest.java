package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import net.luversof.api.stock.web.controller.StockAnalysisController.Release;
import net.luversof.api.stock.web.controller.StockAnalysisController.ReleaseNotes;
import tools.jackson.databind.json.JsonMapper;

/** 버전 기록 파일(손으로 고친다)이 읽히고, 판단 · 우선순위 값이 화면이 아는 것뿐이고, 최신순인지. */
class AnalysisReleaseNotesTest {

  @Test
  void 버전_기록_파일이_읽히고_값이_맞다() throws Exception {
    ReleaseNotes notes;
    try (InputStream in = new ClassPathResource("analysis/releases.json").getInputStream()) {
      notes = JsonMapper.builder().build().readValue(in, ReleaseNotes.class);
    }
    assertThat(notes.releases()).isNotEmpty();
    assertThat(notes.releases())
        .allSatisfy(r -> {
          assertThat(r.date()).isNotNull();
          assertThat(r.version()).isNotBlank();
          assertThat(r.change()).isNotBlank();
          assertThat(r.verdict()).isIn("BASELINE", "ADOPTED", "REJECTED", "HOLD");
        });
    assertThat(notes.releases())
        .extracting(Release::date)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(notes.next())
        .allSatisfy(n -> assertThat(n.priority()).isIn("HIGH", "MEDIUM", "LOW"));
  }
}
