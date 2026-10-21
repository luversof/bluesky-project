package net.luversof.web.gate.poe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.luversof.web.gate.poe.dto.PoeCraft;

/**
 * 가져온 아이템의 수치 → 에뮬레이터 굴림 씨앗(10-20 C485). 툴팁 수치는 씨앗으로 굴리므로({@link PoeModRoll}), 복사 글의 수치와 가장 가까운 값을
 * 내는 씨앗을 찾는다 — 정수 범위면 대개 정확히 같은 값.
 */
public final class PoeCraftRollFit {

  private PoeCraftRollFit() {}

  private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

  /** 찾아볼 씨앗 수 — 범위 둘(최소 · 최대 피해)이 각각 20 칸 남짓이면 400 조합, 넉넉히. */
  static final int TRIES = 6000;

  /**
   * 옵션 줄마다 씨앗 — key = 옵션 id(임플리싯은 "imp:" + id), values = 옵션 id → 복사 글 수치(줄 순서). 수치 개수가 안 맞는 옵션은
   * 건너뛴다.
   */
  public static Map<String, Long> fit(
      List<PoeCraft.Line> lines, List<PoeCraft.Line> implicits, Map<String, List<Double>> values) {
    Map<String, Long> out = new LinkedHashMap<>();
    if (values == null || values.isEmpty()) {
      return out;
    }
    for (PoeCraft.Line l : lines == null ? List.<PoeCraft.Line>of() : lines) {
      Long s = seed(l, values.get(l.id()));
      if (s != null) {
        out.put(l.id(), s);
      }
    }
    for (PoeCraft.Line l : implicits == null ? List.<PoeCraft.Line>of() : implicits) {
      Long s = seed(l, values.get(l.id()));
      if (s != null) {
        out.put("imp:" + l.id(), s);
      }
    }
    return out;
  }

  /** 한 옵션의 씨앗 — 줄 i 는 씨앗 + i 로 굴린다(craftEmu.jte 와 같은 규칙). 범위가 없으면 null. */
  static Long seed(PoeCraft.Line l, List<Double> want) {
    if (want == null || want.isEmpty() || l.en() == null || l.en().isEmpty()) {
      return null;
    }
    List<String> ranges = new ArrayList<>();
    for (int i = 0; i < l.en().size(); i++) {
      String min = l.enMin() != null && i < l.enMin().size() ? l.enMin().get(i) : null;
      ranges.add(PoeModRange.range(min, l.en().get(i)));
    }
    return seed(ranges, want);
  }

  /** PoE2(C486) — 옵션 id → 범위 문장(영어 "(a-b)" 꼴, 줄 순서). */
  public static Map<String, Long> fitRanges(
      Map<String, List<String>> ranges, Map<String, List<Double>> values) {
    Map<String, Long> out = new LinkedHashMap<>();
    if (values == null) {
      return out;
    }
    ranges.forEach(
        (id, r) -> {
          Long s = r == null || r.isEmpty() ? null : seed(r, values.get(id));
          if (s != null) {
            out.put(id, s);
          }
        });
    return out;
  }

  /** 범위 문장들(줄 i 는 씨앗 + i)로 복사 글 수치에 가장 가까운 씨앗. 범위가 없거나 수치 개수가 다르면 null. */
  static Long seed(List<String> ranges, List<Double> want) {
    if (want == null || want.isEmpty()) {
      return null;
    }
    if (ranges.stream().noneMatch(r -> r.contains("("))) {
      return null;
    }
    long best = 0;
    double bestD = Double.MAX_VALUE;
    for (int k = 0; k < TRIES; k++) {
      long s = k * 0x9E3779B97F4A7C15L;
      List<Double> got = new ArrayList<>();
      for (int i = 0; i < ranges.size(); i++) {
        Matcher m = NUMBER.matcher(PoeModRoll.roll(ranges.get(i), s + i));
        while (m.find()) {
          got.add(Math.abs(Double.parseDouble(m.group())));
        }
      }
      if (got.size() != want.size()) {
        return null;
      }
      double d = 0;
      for (int i = 0; i < got.size(); i++) {
        d += Math.abs(got.get(i) - Math.abs(want.get(i)));
      }
      if (d < bestD) {
        bestD = d;
        best = s;
        if (d == 0) {
          break;
        }
      }
    }
    return best;
  }
}
