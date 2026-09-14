package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 가격제한폭 이탈은 조회 하나로 낸다 &mdash; 행과 총 개수를 같은 스캔에서 COUNT(*) OVER () 로 함께 받는다. 예전에는 개수 전용 조회가 따로 있었는데
 * 아무도 부르지 않는 죽은 메서드였고(2026-09-10 제거), 그런 게 남아 있으면 기준값이 갈려 관리 화면이 "5건" 이라 말하면서 4줄만 보여 주는 식으로 조용히
 * 어긋난다. 그래서 기준값이 한 곳에만 있는지도 함께 지킨다.
 *
 * <p>기준값이 0.30 이 아니라 0.301 인 이유도 함께 지킨다. 상·하한가는 기준가에 0.7/1.3 을 곱한 뒤 호가단위로 맞추므로 정상적인 하한가도 30% 를 아주
 * 조금 넘길 수 있다 &mdash; 실측 2026-09-10: 한화오션 2015-07-15 은 55,526 -&gt; 38,868 로 -30.00036% 였고, 이론 하한가
 * 38,868.2 를 호가단위로 내린 값이다. 호가단위/기준가 최대비는 모든 가격대에서 0.100%p 라 0.1%p 여유면 충분하고, 같은 원장의 진짜 이탈은 34.78% ·
 * 42.15% · 67.10% · 80.00% 로 한참 떨어져 있다.
 */
class PriceLimitThresholdGuardTest {

  private static final Path REPOSITORY =
      Path.of("src/main/java/net/luversof/api/stock/repository/StockPriceHistoryRepository.java");

  private static final String MARKER = "prev_close::numeric - 1) > ";

  private static List<String> thresholds() throws IOException {
    String source = Files.readString(REPOSITORY, StandardCharsets.UTF_8);
    List<String> found = new ArrayList<>();
    int at = source.indexOf(MARKER);
    while (at >= 0) {
      int from = at + MARKER.length();
      int to = from;
      while (to < source.length()
          && (Character.isDigit(source.charAt(to)) || source.charAt(to) == '.')) {
        to++;
      }
      found.add(source.substring(from, to));
      at = source.indexOf(MARKER, to);
    }
    return found;
  }

  @Test
  void keepsTheThresholdInOnePlace() throws IOException {
    List<String> found = thresholds();

    assertThat(found)
        .as("개수는 같은 조회의 COUNT(*) OVER () 로 내므로 기준값은 한 곳뿐이어야 한다 - 두 벌이 되면 갈린다")
        .hasSize(1);
  }

  @Test
  void leavesRoomForTickRounding() throws IOException {
    double threshold = Double.parseDouble(thresholds().get(0));

    assertThat(threshold)
        .as("호가단위로 내린 정상 하한가는 30% 를 최대 0.100%p 넘길 수 있어 이탈로 잡으면 안 된다")
        .isGreaterThanOrEqualTo(0.301);
    assertThat(threshold).as("진짜 이탈 중 가장 작은 것이 34.78% 이므로 그보다는 한참 낮아야 한다").isLessThan(0.32);
  }
}
