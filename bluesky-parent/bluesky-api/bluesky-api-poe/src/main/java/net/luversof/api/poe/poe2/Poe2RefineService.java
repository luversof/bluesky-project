package net.luversof.api.poe.poe2;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoePobImportService;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * PoE2 자동 다듬기 — 빌드(보통 poe.ninja 실빌드 출발점)에 업그레이드 가이드(guide2.lua)가 실측한 교체안 중 가장 이득이 큰 것을 **실제로
 * 적용**하고(apply2.lua) 다시 가이드를 돌리기를 반복한다(PoE1 PoeUpgradeGuideService.startRefine 의 PoE2 판).
 *
 * <p>적용하는 교체는 **고유 교체·보조젬 교체**뿐이다. 옵션 목표("이 칸에 이 옵션을 더하면")는 접사 칸 제한을 보지 않아 자동 적용하면 만들 수 없는 아이템이 될 수
 * 있고, 다음 패시브는 남은 포인트가 있어야 해서 레벨 100 빌드엔 공짜 변경이 아니다.
 *
 * <p>가이드 예측을 그대로 믿지 않고, 적용한 빌드를 엔진으로 **다시 계산**해 실제로 오른 것만 채택한다(예측과 다르면 다음 후보). 두 축 모두 {@link
 * #TOLERANCE_PCT} 넘게 깎이면 버린다 — 자동 적용은 되돌리는 사람이 없어 실빌드의 강점을 조금씩 깎는 표류를 막아야 한다.
 */
@Service
public class Poe2RefineService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2RefineService.class);

  static final int ROUNDS = 3;
  static final double TOLERANCE_PCT = 2.0;
  static final double MIN_GAIN_PCT = 1.0;

  /** 한 축 목표(dps·ehp)에서 다른 축이 깎여도 되는 한도(%) — 시뮬레이터 목표가 공격·생존일 때(PoE1 가이드 맞바꿈 기준과 같은 5%). */
  static final double TRADE_PCT = 5.0;

  /**
   * 목표별 채택 조건 — balanced: 두 축 모두 −{@link #TOLERANCE_PCT} 이내·합 ≥ 최소 이득, dps/ehp: 그 축 ≥ 최소 이득·다른 축
   * −{@link #TRADE_PCT} 이내.
   */
  static boolean acceptable(String objective, double dps, double ehp) {
    if ("dps".equals(objective)) {
      return dps >= MIN_GAIN_PCT && ehp >= -TRADE_PCT;
    }
    if ("ehp".equals(objective)) {
      return ehp >= MIN_GAIN_PCT && dps >= -TRADE_PCT;
    }
    return dps >= -TOLERANCE_PCT && ehp >= -TOLERANCE_PCT && dps + ehp >= MIN_GAIN_PCT;
  }

  /** 목표별 순위 점수 — balanced 는 DPS%+EHP%, 한 축 목표는 그 축(%). */
  static double score(String objective, double dps, double ehp) {
    return "dps".equals(objective) ? dps : "ehp".equals(objective) ? ehp : dps + ehp;
  }

  /**
   * 자동 적용에서 빼는 조건부 보조젬(영문 이름 앞부분) — 효과가 게임 상황에 달렸는데 PoB 가 조건을 켠 채로 재서 크게 부풀린다. 가이드 화면엔 그대로 보이지만(사람이
   * 조건을 보고 고른다) 자동 다듬기는 되돌리는 사람이 없어 뺀다. 2026-09-30 일괄 검증 실측: 거침없는 치명타(치명타 없이 흐른 초마다 누적 — 최대 누적 가정,
   * 위치헌터 +333.6%), 소환수 맹약(주변 소환수를 희생 — 소환수 없는 패스파인더에도 +9.7%).
   */
  static final List<String> CONDITIONAL_SUPPORTS = List.of("Inexorable Critical", "Minion Pact");

  /** 한 단계에서 예측 순으로 시도할 최대 후보 수 — 예측과 실측이 어긋나면 다음 후보로. */
  static final int TRIES_PER_ROUND = 3;

  public record Metrics(double dps, double ehp, double life, double es) {}

  public record Step(String label, double dpsPct, double ehpPct) {}

  /**
   * 고정 고유(10-01, PoE1 시뮬 "고유 고정"의 짝) — 다듬기 전에 그 칸에 끼우고, 이후 교체 후보에서 그 칸은 뺀다. slots = 끼워 볼 칸(반지는 두 칸
   * 중 목표 점수가 나은 쪽), raw = PoB 고유 DB 원문.
   */
  public record Forced(String name, String label, List<String> slots, String raw) {}

  /** 고정할 수 있는 고유 분류 → PoB 장비 칸. 무기·보조 장비는 스킬 무기 요구와 얽혀 빌드를 깨뜨릴 수 있어 뺐다(방어구·장신구만). */
  static final Map<String, List<String>> FORCE_SLOTS =
      Map.of(
          "Helmet", List.of("Helmet"),
          "Body Armour", List.of("Body Armour"),
          "Gloves", List.of("Gloves"),
          "Boots", List.of("Boots"),
          "Amulet", List.of("Amulet"),
          "Belt", List.of("Belt"),
          "Ring", List.of("Ring 1", "Ring 2"));

  /**
   * 다듬기 결과. mainSet·sets 는 무기 세트를 나눠 쓰는 빌드일 때만(아니면 null) — 다듬기는 주 세트(DPS 가 큰 쪽)를 켠 채 가이드·적용을 돌리고, 두
   * 세트의 전/후를 따로 잰다({@link Poe2WeaponSets}).
   */
  public record Result(
      Metrics before,
      Metrics after,
      List<Step> steps,
      String code,
      long durationMs,
      Integer mainSet,
      List<SetMetrics> sets) {}

  /** 무기 세트 하나의 다듬기 전/후. */
  public record SetMetrics(int set, Metrics before, Metrics after) {}

  /** 세트 하나를 켠 측정값 — set = 1·2(세트를 안 나누는 빌드면 0 = 저장된 그대로). */
  record Best(int set, Metrics metrics) {}

  public record Status(
      boolean running, int round, int rounds, String phase, Result result, String error) {}

  /** 적용 후보 — change 는 apply2.lua 변경 JSON, 예측 증감(%)은 가이드 값. */
  record Candidate(String label, String change, double dpsPct, double ehpPct) {
    double gain() {
      return dpsPct + ehpPct;
    }
  }

  private final Poe2BuildService builds;
  private final Poe2DataService data;
  private final Poe2PobEngineService engine;
  private final PoePobImportService decoder;
  private final JsonMapper json = JsonMapper.builder().build();
  private final AtomicBoolean running = new AtomicBoolean();
  private volatile int round;
  private volatile String phase = "";
  private volatile Result lastResult;
  private volatile String lastError;

  public Poe2RefineService(
      Poe2BuildService builds,
      Poe2DataService data,
      Poe2PobEngineService engine,
      PoePobImportService decoder,
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.builds = builds;
    this.data = data;
    this.engine = engine;
    this.decoder = decoder;
  }

  public Status status() {
    return new Status(running.get(), round, ROUNDS, phase, lastResult, lastError);
  }

  /** 시작 — 이미 돌고 있거나 코드를 못 읽으면 false(사유는 status().error). */
  public boolean start(String code) {
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    String xml;
    try {
      xml = decoder.decodeToXml(code.trim());
    } catch (RuntimeException e) {
      lastError = "PoB 코드를 읽지 못했습니다: " + e.getMessage();
      lastResult = null;
      running.set(false);
      return false;
    }
    lastError = null;
    lastResult = null;
    round = 0;
    phase = "기준선";
    Thread thread =
        new Thread(
            () -> {
              try {
                lastResult = refine(xml, "balanced", null);
              } catch (Throwable e) {
                logger.warn("PoE2 자동 다듬기 실패", e);
                lastError = "다듬기 실패: " + e.getMessage();
              } finally {
                phase = "";
                running.set(false);
              }
            },
            "poe2-refine");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  /** 시뮬레이터(Poe2SimService)가 다듬기와 동시에 돌지 않게 같은 잠금을 쓴다 — 둘 다 가이드·엔진을 수십 번 부른다. */
  boolean tryLock() {
    return running.compareAndSet(false, true);
  }

  void unlock() {
    phase = "";
    running.set(false);
  }

  int round() {
    return round;
  }

  String phase() {
    return phase;
  }

  /** 잠금을 잡은 쪽(시뮬레이터)이 목표를 정해 부른다 — objective = balanced | dps | ehp. */
  Result refineWith(String startXml, String objective) {
    return refine(startXml, objective, null);
  }

  Result refineWith(String startXml, String objective, Forced forced) {
    return refine(startXml, objective, forced);
  }

  private Result refine(String startXml, String objective, Forced forced) {
    long startedAt = System.currentTimeMillis();
    // 기준선도 저장 왕복(apply2 noop = 바꾸지 않고 SaveDB)을 거친 빌드로 잰다 — 왕복 자체가 수치를 바꾸는 빌드가 있어(10-01 60빌드 점검:
    //   워브링어 집속 수류탄 DPS +4.4%, 오라클 정신력) 그대로 두면 첫 단계 이득에 그 몫이 섞인다. 왕복이 실패하면 원본 그대로.
    String roundTrip = engine.apply(startXml, "{\"type\":\"noop\"}");
    String xml = roundTrip != null ? roundTrip : startXml;
    // 무기 세트를 나눠 쓰면 주 세트(DPS 가 큰 쪽)를 켠 채 다듬는다 — 저장 당시 켜진 세트가 주 세트가 아닐 수 있다(실빌드 60개 중 18개가 세트 2 로 저장)
    boolean sets = Poe2WeaponSets.uses(xml);
    Best start = measureBest(xml);
    if (start == null) {
      throw new IllegalStateException("기준선 계산 실패");
    }
    Metrics[] setBefore =
        sets
            ? new Metrics[] {
              measure(Poe2WeaponSets.withSet(xml, 1)), measure(Poe2WeaponSets.withSet(xml, 2))
            }
            : null;
    if (sets) {
      xml = Poe2WeaponSets.withSet(xml, start.set());
    }
    Metrics before = start.metrics();
    Metrics current = before;
    List<Step> steps = new ArrayList<>();
    // 고정 고유 — 목표 점수와 상관없이 먼저 끼운다(사용자가 고른 것). 반지는 두 칸 중 나은 쪽. 끼우기가 실패하면 고정 없이 진행하고 단계 이름으로 알린다
    String lockedSlot = null;
    if (forced != null && forced.raw() != null) {
      phase = "고유 고정";
      String bestNext = null;
      Metrics bestM = null;
      String bestSlot = null;
      for (String slot : forced.slots()) {
        ObjectNode ch = json.createObjectNode();
        ch.put("type", "unique");
        ch.put("slot", slot);
        ch.put("raw", forced.raw());
        ch.put("quality20", true);
        String next = engine.apply(xml, ch.toString());
        Metrics m = next == null ? null : measure(next);
        if (m != null
            && (bestM == null
                || score(objective, pct(current.dps(), m.dps()), pct(current.ehp(), m.ehp()))
                    > score(
                        objective,
                        pct(current.dps(), bestM.dps()),
                        pct(current.ehp(), bestM.ehp())))) {
          bestNext = next;
          bestM = m;
          bestSlot = slot;
        }
      }
      if (bestNext != null) {
        steps.add(
            new Step(
                "고유 고정: " + bestSlot + " ← " + forced.label(),
                pct(current.dps(), bestM.dps()),
                pct(current.ehp(), bestM.ehp())));
        xml = bestNext;
        current = bestM;
        lockedSlot = bestSlot;
      } else {
        steps.add(new Step("고유 고정 실패(끼울 수 없음): " + forced.label(), 0, 0));
      }
    }
    for (int r = 1; r <= ROUNDS; r++) {
      round = r;
      phase = "가이드 측정";
      // 세트를 나눠 쓰면 주 세트를 지정해 부른다(지정 안 하면 가이드가 단계마다 두 세트를 다시 재 주 세트를 고른다 — 엔진 2회 낭비)
      Poe2.BuildGuide guide = builds.guide(encode(xml), sets ? start.set() : null);
      if (guide == null || guide.error() != null) {
        logger.info("PoE2 자동 다듬기 {}단계: 가이드 실패 {} — 종료", r, guide == null ? null : guide.error());
        break;
      }
      List<Candidate> candidates = candidates(guide, objective);
      if (lockedSlot != null) {
        final String locked = "\"slot\":\"" + lockedSlot + "\"";
        candidates.removeIf(c -> c.change().contains(locked)); // 고정한 칸은 다시 바꾸지 않는다
      }
      phase = "적용·재계산";
      boolean applied = false;
      int tries = 0;
      for (Candidate c : candidates) {
        if (tries++ >= TRIES_PER_ROUND) {
          break;
        }
        String next = engine.apply(xml, c.change());
        Metrics m = next == null ? null : measure(next);
        if (m == null) {
          continue;
        }
        double dps = pct(current.dps(), m.dps());
        double ehp = pct(current.ehp(), m.ehp());
        logger.info(
            "PoE2 자동 다듬기 {}단계 후보 {}: 예측 DPS {} EHP {} / 실측 DPS {} EHP {}",
            r,
            c.label(),
            String.format("%+.1f", c.dpsPct()),
            String.format("%+.1f", c.ehpPct()),
            String.format("%+.1f", dps),
            String.format("%+.1f", ehp));
        if (!acceptable(objective, dps, ehp)) {
          continue; // 적용해 보니 예측만큼 안 오르거나 다른 축을 한도 넘게 깎는다
        }
        xml = next;
        current = m;
        steps.add(new Step(c.label(), dps, ehp));
        applied = true;
        break;
      }
      if (!applied) {
        logger.info("PoE2 자동 다듬기 {}단계: 적용할 교체 없음(후보 {}개) — 종료", r, candidates.size());
        break;
      }
    }
    List<SetMetrics> setList = null;
    if (sets) {
      setList =
          List.of(
              new SetMetrics(1, setBefore[0], measure(Poe2WeaponSets.withSet(xml, 1))),
              new SetMetrics(2, setBefore[1], measure(Poe2WeaponSets.withSet(xml, 2))));
    }
    return new Result(
        before,
        current,
        List.copyOf(steps),
        encode(xml),
        System.currentTimeMillis() - startedAt,
        sets ? start.set() : null,
        setList);
  }

  /**
   * 더 나은 무기 세트로 잰 값 — 세트를 안 나누는 빌드면 저장된 그대로(set 0). 나누면 두 세트를 다 재고 DPS 가 큰 쪽(같으면 저장된 세트). 둘 다 실패면
   * null.
   */
  Best measureBest(String xml) {
    if (!Poe2WeaponSets.uses(xml)) {
      Metrics m = measure(xml);
      return m == null ? null : new Best(0, m);
    }
    Metrics m1 = measure(Poe2WeaponSets.withSet(xml, 1));
    Metrics m2 = measure(Poe2WeaponSets.withSet(xml, 2));
    if (m1 == null && m2 == null) {
      return null;
    }
    if (m1 == null) {
      return new Best(2, m2);
    }
    if (m2 == null) {
      return new Best(1, m1);
    }
    if (m1.dps() == m2.dps()) {
      int saved = Poe2WeaponSets.saved(xml);
      return new Best(saved, saved == 2 ? m2 : m1);
    }
    return m2.dps() > m1.dps() ? new Best(2, m2) : new Best(1, m1);
  }

  /** 가이드 → 적용 후보(목표 점수 순). 목표별 채택 조건({@link #acceptable})을 예측으로 먼저 거른다. */
  List<Candidate> candidates(Poe2.BuildGuide g, String objective) {
    List<Candidate> out = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    List<Poe2.GuideSwap> swaps = new ArrayList<>();
    if (g.swapsDps() != null) {
      swaps.addAll(g.swapsDps());
    }
    if (g.swapsEhp() != null) {
      swaps.addAll(g.swapsEhp());
    }
    for (Poe2.GuideSwap s : swaps) {
      String raw = uniqueRaw(s.item());
      if (raw == null || !seen.add(s.slot() + "|" + s.item())) {
        continue;
      }
      ObjectNode ch = json.createObjectNode();
      ch.put("type", "unique");
      ch.put("slot", s.slot());
      ch.put("raw", raw);
      ch.put("quality20", true); // 가이드도 PoB 고유 DB 아이템(품질 20%)을 끼워 잰다
      out.add(
          new Candidate(
              "고유 교체: " + s.slot() + " ← " + (s.itemKo() != null ? s.itemKo() : s.item()),
              ch.toString(),
              nz(s.dps()),
              nz(s.ehp())));
    }
    if (g.gemSwaps() != null && g.gemWeakest() != null) {
      for (Poe2.GuideSupport s : g.gemSwaps()) {
        if (s.name() != null && CONDITIONAL_SUPPORTS.stream().anyMatch(s.name()::startsWith)) {
          continue;
        }
        ObjectNode ch = json.createObjectNode();
        ch.put("type", "gem");
        ch.put("from", g.gemWeakest());
        ch.put("name", s.name());
        out.add(
            new Candidate(
                "보조젬 교체: "
                    + (g.gemWeakestKo() != null ? g.gemWeakestKo() : g.gemWeakest())
                    + " → "
                    + (s.nameKo() != null ? s.nameKo() : s.name())
                    // 계보 젬은 드물게 얻는다 — 60빌드 일괄에서 DPS 중앙 +114% 의 상당 부분이 계보 젬(제르피의 악명 18 · 라키아타의 흐름
                    // 13)이라
                    //   "쉽게 할 수 있는 교체"로 읽히지 않게 표시한다(2026-10-01)
                    + (isLineage(s.slug()) ? " · 계보 젬(드묾)" : ""),
                ch.toString(),
                nz(s.dps()),
                nz(s.ehp())));
      }
    }
    out.removeIf(c -> !acceptable(objective, c.dpsPct(), c.ehpPct()));
    out.sort(
        Comparator.comparingDouble((Candidate c) -> score(objective, c.dpsPct(), c.ehpPct()))
            .reversed());
    return out;
  }

  private boolean isLineage(String slug) {
    if (slug == null) {
      return false;
    }
    try {
      return data.gem(slug).map(gem -> Boolean.TRUE.equals(gem.lineage())).orElse(false);
    } catch (RuntimeException e) {
      return false;
    }
  }

  Metrics measure(String xml) {
    Poe2PobEngineService.Result r = engine.calc(xml);
    if (r.error() != null || r.values().isEmpty()) {
      return null;
    }
    Map<String, Double> v = r.values();
    double full = v.getOrDefault("FullDPS", 0d);
    double dps = full > 0 ? full : v.getOrDefault("CombinedDPS", 0d);
    return new Metrics(
        dps,
        v.getOrDefault("TotalEHP", 0d),
        v.getOrDefault("Life", 0d),
        v.getOrDefault("EnergyShield", 0d));
  }

  /**
   * PoB 고유 원문 — 읽기는 Poe2DataService 로 옮겼다(트리 평가의 주얼 끼우기도 쓴다 · Build 가 Refine 을 부르면 순환, 10-03 C73).
   */
  String uniqueRaw(String title) {
    return data.uniqueRaw(title);
  }

  private static double nz(Double v) {
    return v == null ? 0d : v;
  }

  private static double pct(double base, double now) {
    return base > 0 ? (now / base - 1) * 100 : 0d;
  }

  /** PoB 공유 코드 인코딩(zlib deflate → base64url). */
  static String encode(String xml) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    deflater.setInput(xml.getBytes(StandardCharsets.UTF_8));
    deflater.finish();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8 * 1024];
    while (!deflater.finished()) {
      output.write(buffer, 0, deflater.deflate(buffer));
    }
    deflater.end();
    return Base64.getEncoder()
        .encodeToString(output.toByteArray())
        .replace('+', '-')
        .replace('/', '_');
  }
}
