package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 기간 프리셋(이번달/올해/N개월) 규칙이 서버와 화면에서 같은 구간을 내는지 본다.
 *
 * <p>같은 규칙이 두 곳에 따로 구현돼 있다 &mdash; 서버는 {@code StockBaseHtmxController.resolvePresetRange}, 화면은
 * {@code date-range-picker.ts} 다. 이 저장소는 같은 날짜 규칙의 복제본이 조용히 갈린 전례가 있어(날짜→instant 변환이 세 벌로 갈려 한 벌이
 * 하루 어긋난 값을 냈다) 표로 맞춰 둔다.
 *
 * <p>{@code date-range-preset-frontend.txt} 는 화면 쪽 함수({@code addMonthsClamped} + 하루 더하기)를 node 로
 * 그대로 돌려 뽑은 값이다. 형식은 {@code 기준일|모드|시작일|종료일(포함)} 이고, 말일·윤년 경계를 포함한다.
 */
class PresetRangeParityTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  /** 서버 규칙. resolvePresetRange 는 protected 라 같은 계산을 여기 옮겨 두고, 아래에서 실물과 대조한다. */
  private static String[] backendRange(LocalDate today, String mode) {
    LocalDate from;
    if ("mtd".equalsIgnoreCase(mode)) {
      from = today.withDayOfMonth(1);
    } else if (mode.matches("[1-9][0-9]{0,3}") && Long.parseLong(mode) <= 1200L) {
      from = today.minusMonths(Long.parseLong(mode)).plusDays(1);
    } else {
      from = LocalDate.of(today.getYear(), 1, 1);
    }
    // 서버가 내는 종료일은 배타적(today + 1)이므로 포함 종료일은 today 다.
    return new String[] {from.toString(), today.toString()};
  }

  private List<String[]> frontendRows() throws IOException {
    List<String[]> rows = new ArrayList<>();
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("date-range-preset-frontend.txt")) {
      assertThat(in).as("화면 쪽 기준표가 없다").isNotNull();
      for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty()) {
          rows.add(trimmed.split("[|]"));
        }
      }
    }
    return rows;
  }

  @Test
  void 서버와_화면의_프리셋_구간이_같다() throws IOException {
    List<String[]> rows = frontendRows();
    // 표가 조용히 비면 검사가 무력해진다(기준일 8 x 모드 7).
    assertThat(rows).hasSize(56);

    List<String> mismatches = new ArrayList<>();
    for (String[] row : rows) {
      LocalDate today = LocalDate.parse(row[0]);
      String[] backend = backendRange(today, row[1]);
      if (!backend[0].equals(row[2]) || !backend[1].equals(row[3])) {
        mismatches.add(
            row[0]
                + " "
                + row[1]
                + " — 화면 "
                + row[2]
                + "~"
                + row[3]
                + " / 서버 "
                + backend[0]
                + "~"
                + backend[1]);
      }
    }
    assertThat(mismatches).as("프리셋 구간이 서버와 화면에서 다르다").isEmpty();
  }

  /** 위 backendRange 가 실물 resolvePresetRange 와 같은지. 옮겨 적은 계산이 낡으면 이 검사가 깨진다. */
  @Test
  void 옮겨_적은_서버_규칙이_실물과_같다() {
    StockBaseHtmxController controller =
        new StockBaseHtmxController(null, null, null, null, null, null) {};
    LocalDate today = LocalDate.now(ZONE);
    for (String mode : List.of("mtd", "ytd", "1", "3", "6", "12", "36")) {
      var preset = controller.resolvePresetRange(mode, ZONE);
      String[] copied = backendRange(today, mode);

      assertThat(preset.start()).as(mode + " 시작").isEqualTo(instantOf(copied[0]));
      // 실물의 종료는 배타적이므로 포함 종료일 + 1 일이다.
      assertThat(preset.end())
          .as(mode + " 종료(배타적)")
          .isEqualTo(instantOf(LocalDate.parse(copied[1]).plusDays(1).toString()));
    }
  }

  private static Instant instantOf(String localDate) {
    return LocalDate.parse(localDate).atStartOfDay(ZONE).toInstant();
  }
}
