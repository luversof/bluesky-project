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

  // 10-14 3 → 5: 공통 고유 · 보조젬 · 패시브 재분배가 후보에 더해져, 3단계면 예측이 큰 보조젬 교체만 채택되고 재분배는 순서가 안 왔다(얼음 화살 실측)
  static final int ROUNDS = 5;
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

  /**
   * phase = 단계 코드(baseline · unique · guide · apply — 게이트가 poe2.refine.phase.* 로 옮긴다, 10-12 C269).
   * evals = 이번 실행의 엔진 계산 수(PoE1 최적화 "평가 N회" 의 짝).
   */
  public record Status(
      boolean running,
      int round,
      int rounds,
      String phase,
      Result result,
      String error,
      int evals) {}

  /** 사용자 중지(시뮬레이터 중지 단추) — 다음 단계·후보 경계에서 던져 실행을 끝낸다. */
  static final class Cancelled extends RuntimeException {
    Cancelled() {
      super("사용자 요청으로 중지됨", null, false, false);
    }
  }

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
  private volatile boolean cancelRequested;
  private volatile int evals;
  // 마지막 measure 의 주 스킬 비용 경고 자원(10-14 C309) — 마나 1 · 생명력 2 · 보호막 4 · 분노 8(calc.lua
  // MainSkillCostWarningMask). 잠금을 잡은 스레드 하나만 쓴다
  private long lastCostMask;

  /** 마지막 측정의 능력치 부족(힘 1 · 민첩 2 · 지능 4) — 요구치를 못 채우면 인게임에선 장비 · 젬을 못 쓴다(PoB 는 경고만 하고 계산한다). */
  private long lastAttrMask;

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
    return new Status(running.get(), round, ROUNDS, phase, lastResult, lastError, evals);
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
    cancelRequested = false;
    evals = 0;
    phase = "baseline";
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
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    round = 0;
    cancelRequested = false;
    evals = 0;
    return true;
  }

  /** 중지 요청 — 돌고 있지 않으면 false. 실제 종료는 다음 경계({@link #checkCancel()})에서. */
  boolean requestCancel() {
    if (!running.get()) {
      return false;
    }
    cancelRequested = true;
    return true;
  }

  /** 중지가 요청됐으면 {@link Cancelled} 를 던진다 — 단계·후보 경계에서 부른다. */
  void checkCancel() {
    if (cancelRequested) {
      throw new Cancelled();
    }
  }

  int evals() {
    return evals;
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

  /** consensus = 그 아키타입 실빌드들의 공통 특성(없으면 null) — 공통 트리를 견주고, 공통 고유 · 보조 젬을 후보에 넣는다. */
  Result refineWith(
      String startXml, String objective, Forced forced, Poe2NinjaService.Consensus consensus) {
    return refine(startXml, objective, forced, consensus);
  }

  private Result refine(String startXml, String objective, Forced forced) {
    return refine(startXml, objective, forced, null);
  }

  private Result refine(
      String startXml, String objective, Forced forced, Poe2NinjaService.Consensus consensus) {
    long startedAt = System.currentTimeMillis();
    phase = "baseline";
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
      checkCancel();
      phase = "unique";
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
    // 주 스킬 비용(10-14 C309, PoE1 최적화기 C307 · C308 의 짝) — 교체가 마나 · 생명력 · 보호막 비용 경고를 **새로** 만들면 건너뛴다.
    //   PoB 는 경고만 하고 DPS 를 그대로 계산해, 비용을 못 치르는 교체(소모 배율 큰 보조 · 비용을 보호막으로 바꾸는 고유)가 이득으로 보인다.
    //   실빌드에서 출발하므로 원래 있던 경고는 탓하지 않는다(상태가 아니라 전이를 본다 — 상태로 막으면 탐색이 틀어진다, C307 1차)
    long costMask = measure(xml) == null ? 0L : lastCostMask;
    // 능력치 요구(10-14) — 비용과 같은 전이 규칙: 교체가 힘 · 민첩 · 지능 부족을 **새로** 만들면 건너뛴다(트리를 바꾸면 능력치 노드가 달라져 장비를 못 낄
    // 수 있다)
    long attrMask = lastAttrMask;
    int mainSet = start.set();
    // 닌자 공통 트리(10-14 사용자 요청 "특정 1명이 아니라 그 직업/스킬의 특성을 참고") — 실빌드 N명의 노드 빈도로 키운 트리(본 · 전직 · 무기 세트 1/2
    //   를 따로)를 고른 실빌드에 덮어 잰다. 목표 점수가 오르고 비용 · 능력치를 새로 깨지 않으면 쓴다. 못 쓰면 이유를 단계로 남긴다(왜 안 바꿨는지 보이게)
    if (consensus != null && consensus.nodes().size() > 1) {
      checkCancel();
      phase = "tree";
      String label = COMMON_PREFIX + " 트리(" + consensus.n() + "명)";
      String treeXml = null;
      try {
        treeXml =
            builds.withTree(
                xml,
                consensus.className(),
                consensus.ascendancy(),
                consensus.nodes(),
                consensus.attrs(),
                consensus.sets());
        treeXml = keepSockets(xml, treeXml, new java.util.HashSet<>(consensus.nodes()));
      } catch (RuntimeException e) {
        logger.info("PoE2 공통 트리 적용 실패: {}", e.getMessage());
      }
      Best tb = treeXml == null ? null : measureBest(treeXml);
      if (tb != null && tb.set() > 0) {
        treeXml = Poe2WeaponSets.withSet(treeXml, tb.set());
        measure(treeXml); // 비용 · 능력치 표시는 마지막 측정 것 — 고른 세트로 한 번 더
      }
      if (tb == null) {
        steps.add(new Step(label + " — 계산 실패, 지금 트리 유지", 0, 0));
      } else {
        double dps = pct(current.dps(), tb.metrics().dps());
        double ehp = pct(current.ehp(), tb.metrics().ehp());
        logger.info(
            "PoE2 공통 트리({}명, 겹침 {}): DPS {} EHP {} 비용 {}→{} 능력치 {}→{}",
            consensus.n(),
            consensus.overlap(),
            String.format("%+.1f", dps),
            String.format("%+.1f", ehp),
            costMask,
            lastCostMask,
            attrMask,
            lastAttrMask);
        if (!acceptable(objective, dps, ehp)) {
          steps.add(new Step(label + " — 지금 트리가 나아 유지", dps, ehp));
        } else if ((lastCostMask & ~costMask & 7L) != 0L) {
          steps.add(new Step(label + " — 주 스킬 비용을 못 치르게 돼 유지", dps, ehp));
        } else if ((lastAttrMask & ~attrMask) != 0L) {
          steps.add(new Step(label + " — 능력치 요구를 못 채우게 돼 유지", dps, ehp));
        } else {
          steps.add(new Step(label, dps, ehp));
          xml = treeXml;
          current = tb.metrics();
          costMask = lastCostMask;
          attrMask = lastAttrMask;
          if (tb.set() > 0) {
            mainSet = tb.set();
          }
          sets = Poe2WeaponSets.uses(xml);
        }
      }
    }
    java.util.Set<String> triedConsensus = new java.util.HashSet<>();
    for (int r = 1; r <= ROUNDS; r++) {
      checkCancel();
      round = r;
      phase = "guide";
      // 세트를 나눠 쓰면 주 세트를 지정해 부른다(지정 안 하면 가이드가 단계마다 두 세트를 다시 재 주 세트를 고른다 — 엔진 2회 낭비)
      Poe2.BuildGuide guide = builds.guide(encode(xml), sets ? mainSet : null);
      if (guide == null || guide.error() != null) {
        logger.info("PoE2 자동 다듬기 {}단계: 가이드 실패 {} — 종료", r, guide == null ? null : guide.error());
        break;
      }
      List<Candidate> candidates = candidates(guide, objective);
      // 닌자 공통 고유 · 보조 젬 — 가이드 예측이 없어 맨 앞에서 한 번씩만 실측해 본다(채택 조건 · 비용 · 능력치 규칙은 같다)
      List<Candidate> common = consensusCandidates(xml, guide, consensus);
      common.addAll(consensusNodeCandidates(xml, guide, consensus, objective));
      // 같은 고유 · 젬은 한 번만 — 빼는 보조젬이 단계마다 달라 변경 JSON 으로 견주면 같은 젬을 매 단계 다시 쟀다(10-14: 가루칸의 투지 4번)
      common.removeIf(
          c -> !triedConsensus.add(c.label().replaceFirst(":.*→", "→").replaceFirst(":.*←", "←")));
      candidates.addAll(0, common);
      if (lockedSlot != null) {
        final String locked = "\"slot\":\"" + lockedSlot + "\"";
        candidates.removeIf(c -> c.change().contains(locked)); // 고정한 칸은 다시 바꾸지 않는다
      }
      phase = "apply";
      boolean applied = false;
      int tries = 0;
      for (Candidate c : candidates) {
        if (!c.label().startsWith(COMMON_PREFIX) && tries++ >= TRIES_PER_ROUND) {
          break;
        }
        checkCancel();
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
        long nextMask = lastCostMask;
        if ((nextMask & ~costMask & 7L) != 0L) {
          logger.info(
              "PoE2 자동 다듬기 {}단계 후보 {}: 주 스킬 비용을 못 치르게 된다(경고 {} → {}) — 건너뜀",
              r,
              c.label(),
              costMask,
              nextMask);
          continue;
        }
        if ((lastAttrMask & ~attrMask) != 0L) {
          logger.info(
              "PoE2 자동 다듬기 {}단계 후보 {}: 능력치 요구를 못 채우게 된다({} → {}) — 건너뜀",
              r,
              c.label(),
              attrMask,
              lastAttrMask);
          continue;
        }
        costMask = nextMask;
        attrMask = lastAttrMask;
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
        sets ? mainSet : null,
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

  /** 공통 특성 후보 이름 머리 — 가이드 후보의 단계당 시도 한도(TRIES_PER_ROUND)에 세지 않는다(예측이 없어 실측해야 안다). */
  static final String COMMON_PREFIX = "닌자 공통";

  /** 공통으로 보는 몫 — 실빌드의 절반 이상이 쓰는 고유 · 보조 젬만. */
  static final double COMMON_SHARE = 0.5;

  /**
   * 공통 특성 → 적용 후보: 칸마다 절반 이상이 끼는 고유(이미 끼고 있지 않으면), 메인 스킬 보조 젬 중 절반 이상이 쓰는데 내 빌드에 없는 것(가장 약한 보조와
   * 바꾼다). 예측은 0 — 실측으로만 고른다.
   */
  List<Candidate> consensusCandidates(
      String xml, Poe2.BuildGuide g, Poe2NinjaService.Consensus consensus) {
    List<Candidate> out = new ArrayList<>();
    if (consensus == null || consensus.n() < 2) {
      return out;
    }
    // XML 은 작은따옴표를 &apos; 로 쓴다 — 그대로 견주면 "Rakiata's Flow" 를 못 찾아 이미 낀 보조젬을 또 넣었다(10-14 실측 DPS −16%)
    xml = unescapeXml(xml);
    for (Map.Entry<String, Map<String, Integer>> sl : consensus.slotUniques().entrySet()) {
      Map.Entry<String, Integer> top =
          sl.getValue().entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
      if (top == null || top.getValue() < consensus.n() * COMMON_SHARE) {
        continue;
      }
      if (equipsUnique(xml, top.getKey())) {
        continue; // 이미 끼고 있다(어느 칸이든 — 반지 두 칸에 같은 고유를 두 번 끼지 않게)
      }
      String raw = uniqueRaw(top.getKey());
      if (raw == null) {
        continue;
      }
      ObjectNode ch = json.createObjectNode();
      ch.put("type", "unique");
      ch.put("slot", sl.getKey());
      ch.put("raw", raw);
      ch.put("quality20", true);
      String ko = uniqueKo(top.getKey());
      out.add(
          new Candidate(
              COMMON_PREFIX
                  + " 고유("
                  + top.getValue()
                  + "/"
                  + consensus.n()
                  + "명): "
                  + sl.getKey()
                  + " ← "
                  + ko,
              ch.toString(),
              0,
              0));
    }
    if (g != null && g.gemWeakest() != null) {
      for (Map.Entry<String, Integer> sp : consensus.supports().entrySet()) {
        String name = sp.getKey();
        if (sp.getValue() < consensus.n() * COMMON_SHARE
            || xml.contains("nameSpec=\"" + name + "\"")
            || CONDITIONAL_SUPPORTS.stream().anyMatch(name::startsWith)) {
          continue;
        }
        ObjectNode ch = json.createObjectNode();
        ch.put("type", "gem");
        ch.put("from", g.gemWeakest());
        ch.put("name", name);
        String ko = gemKo(name);
        out.add(
            new Candidate(
                COMMON_PREFIX
                    + " 보조젬("
                    + sp.getValue()
                    + "/"
                    + consensus.n()
                    + "명): "
                    + (g.gemWeakestKo() != null ? g.gemWeakestKo() : g.gemWeakest())
                    + " → "
                    + ko,
                ch.toString(),
                0,
                0));
      }
    }
    return out;
  }

  /** 공통 패시브로 보는 몫 — 실빌드 4분의 3 이상이 찍는 본 트리 특화 · 핵심. */
  static final double COMMON_NODE_SHARE = 0.75;

  /** 공통 패시브 후보 수(인원 순). */
  static final int COMMON_NODES = 3;

  private static final java.util.regex.Pattern SPEC_NODES =
      java.util.regex.Pattern.compile("<Spec\\b[^>]*\\bnodes=\"([^\"]*)\"");

  /**
   * 공통 패시브(10-14 C315, 사용자 요청 "닌자의 해당 직업/스킬 특성을 참고해 최선의 결과") — 공통 트리를 통째로 덮으면 실빌드마다 장비에 맞춘 트리가 달라
   * 8/8 거절됐다(DPS −7 ~ −98%). 대신 실빌드 4분의 3 이상이 찍는데 내 빌드에 없는 특화 · 핵심을 하나씩 재분배로 넣어 본다: 기여가 작은 본 트리 특화를
   * 빼고(가이드 free) 그 점수로 찍는다(apply2 respec — 닿지 않으면 엔진이 거절). 예측이 없어 실측으로만 고른다.
   */
  List<Candidate> consensusNodeCandidates(
      String xml, Poe2.BuildGuide g, Poe2NinjaService.Consensus consensus, String objective) {
    List<Candidate> out = new ArrayList<>();
    if (consensus == null
        || consensus.n() < 2
        || consensus.nodeCounts() == null
        || g == null
        || g.nodes() == null) {
      return out;
    }
    java.util.Set<Integer> mine = new java.util.HashSet<>();
    java.util.regex.Matcher sm = SPEC_NODES.matcher(xml);
    if (sm.find()) {
      for (String id : sm.group(1).split(",")) {
        if (!id.isBlank()) {
          mine.add(Integer.valueOf(id.trim()));
        }
      }
    }
    // 뺄 특화 — 기여가 거의 없는 것(두 축 합 ≤ TOLERANCE_PCT) 중 풀리는 점수가 가장 큰 것(공통 패시브는 길이를 몰라 넉넉한 쪽). 기여가 큰 특화를
    // 빼면 공통
    //   패시브 몫보다 잃는 게 커서 실측만 낭비한다(시험이 잡음: 약한 셋에 기여 40% 특화가 섞이면 그걸 골랐다)
    Poe2.GuideNode weak =
        g.nodes().stream()
            .filter(
                n ->
                    n.id() != null
                        && (n.ascendancy() == null || n.ascendancy().isBlank())
                        && n.free() != null
                        && n.free() > 0
                        && n.free() <= RESPEC_MAX_FREE
                        && n.set() == null
                        && Math.abs(nz(n.dps())) + Math.abs(nz(n.ehp())) <= TOLERANCE_PCT)
            .max(Comparator.comparingInt(Poe2.GuideNode::free))
            .orElse(null);
    if (weak == null) {
      return out;
    }
    consensus.nodeCounts().entrySet().stream()
        .filter(e -> e.getValue() >= consensus.n() * COMMON_NODE_SHARE)
        .filter(e -> !mine.contains(e.getKey()))
        .filter(e -> isNotableOrKeystone(e.getKey()))
        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
        .limit(COMMON_NODES)
        .forEach(
            e -> {
              ObjectNode ch = json.createObjectNode();
              ch.put("type", "respec");
              ch.put("remove", weak.id());
              ch.put("add", e.getKey());
              out.add(
                  new Candidate(
                      COMMON_PREFIX
                          + " 패시브("
                          + e.getValue()
                          + "/"
                          + consensus.n()
                          + "명): "
                          + (weak.nameKo() != null ? weak.nameKo() : weak.name())
                          + "(풀리는 "
                          + weak.free()
                          + "점) 빼고 → "
                          + nodeKo(e.getKey())
                          + " 찍기",
                      ch.toString(),
                      0,
                      0));
            });
    return out;
  }

  boolean isNotableOrKeystone(int id) {
    Poe2DataService.TreeNodeLite n = data.treeIndex().nodes().get(id);
    return n != null
        && (n.ascendancy() == null || n.ascendancy().isBlank())
        && ("notable".equals(n.kind()) || "keystone".equals(n.kind()));
  }

  String nodeKo(int id) {
    Poe2DataService.TreeNodeLite n = data.treeIndex().nodes().get(id);
    return n == null ? String.valueOf(id) : n.nameKo() != null ? n.nameKo() : n.name();
  }

  String uniqueKo(String name) {
    return data.uniqueByName(name).map(Poe2.Unique::nameKo).orElse(name);
  }

  String gemKo(String name) {
    return data.gemByName(name).map(Poe2.Gem::nameKo).orElse(name);
  }

  /** 재분배에서 뺄 후보 수(기여가 가장 작은 본 트리 특화 · 핵심) × 찍을 후보 수(가이드 "다음에 찍을 패시브" 상위). */
  static final int RESPEC_REMOVE = 3;

  static final int RESPEC_ADD = 3;

  /**
   * 재분배로 뺄 수 있는 최대 점수 — 더 크면 다리 노드(매달린 트리가 끊긴다, 10-14 free 123 → DPS −97.7%). guide2 · apply2 도 같은
   * 선을 둔다.
   */
  static final int RESPEC_MAX_FREE = 8;

  /**
   * 패시브 재분배(10-14 P4, 사용자 요청 "최선의 시뮬레이션 결과") — 다 찍은 빌드는 새 패시브를 찍을 점수가 없어 가이드의 "다음에 찍을 패시브"를 그대로 못
   * 쓴다. 기여가 가장 작은 본 트리 특화 하나와 그것만 잇던 길을 빼고 그 점수로 다음 패시브까지 찍는다(apply2 respec — 사용 점수가 늘면 엔진이 거절). 예측
   * = 찍을 것의 증감 − 뺄 것의 기여(길의 작은 노드 몫은 빠져 있어 실측으로 거른다). 전직 노드는 점수가 따로라 뺀다.
   */
  List<Candidate> respecCandidates(Poe2.BuildGuide g, String objective) {
    List<Candidate> out = new ArrayList<>();
    if (g == null || g.nodes() == null) {
      return out;
    }
    List<Poe2.GuideNode> weak =
        g.nodes().stream()
            .filter(
                n ->
                    n.id() != null
                        && (n.ascendancy() == null || n.ascendancy().isBlank())
                        && n.free() != null
                        && n.free() > 0
                        && n.free() <= RESPEC_MAX_FREE)
            // 잃는 몫 = 두 축 합 + 목표 축 한 번 더 — 목표 축만 보면 생존을 크게 주는 특화(EHP 14.9%)를 골라 맞바꿈 한도에 걸렸다(10-14
            // 실측)
            .sorted(
                Comparator.comparingDouble(
                    n -> nz(n.dps()) + nz(n.ehp()) + score(objective, nz(n.dps()), nz(n.ehp()))))
            // 본 트리 · 무기 세트 칸마다 따로 약한 셋(세트 칸끼리만 짝지어지므로 한 줄 세움이면 세트 쪽이 밀려 안 남는다, 10-14 C316)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    n -> String.valueOf(n.set()),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()))
            .values()
            .stream()
            .flatMap(l -> l.stream().limit(RESPEC_REMOVE))
            .toList();
    Map<Integer, Poe2.GuideNextNode> nextById = new java.util.LinkedHashMap<>();
    for (List<Poe2.GuideNextNode> l : java.util.Arrays.asList(g.nextDps(), g.nextEhp())) {
      if (l != null) {
        l.stream().filter(n -> n.id() != null).forEach(n -> nextById.putIfAbsent(n.id(), n));
      }
    }
    List<Poe2.GuideNextNode> next =
        nextById.values().stream()
            .sorted(
                Comparator.comparingDouble(
                        (Poe2.GuideNextNode n) -> score(objective, nz(n.dps()), nz(n.ehp())))
                    .reversed())
            .collect(
                java.util.stream.Collectors.groupingBy(
                    n -> String.valueOf(n.set()),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()))
            .values()
            .stream()
            .flatMap(l -> l.stream().limit(RESPEC_ADD))
            .toList();
    for (Poe2.GuideNode w : weak) {
      for (Poe2.GuideNextNode n : next) {
        if (!java.util.Objects.equals(w.set(), n.set())) {
          continue; // 점수 칸이 다르다(본 트리 ↔ 무기 세트) — 세트 점수로 뺀 자리는 세트 길에만(10-14 C316)
        }
        if (n.points() == null || n.points() > w.free()) {
          continue; // 빼서 풀리는 점수로 닿지 않는다(엔진이 "점수 초과"로 거절 — 10-14 실측 3번 헛시도)
        }
        ObjectNode ch = json.createObjectNode();
        ch.put("type", "respec");
        ch.put("remove", w.id());
        ch.put("add", n.id());
        out.add(
            new Candidate(
                "패시브 재분배: "
                    + (w.nameKo() != null ? w.nameKo() : w.name())
                    + "(풀리는 "
                    + w.free()
                    + "점) 빼고 → "
                    + (n.nameKo() != null ? n.nameKo() : n.name())
                    + " 찍기(가는 길 "
                    + n.points()
                    + "점"
                    + (n.set() == null ? "" : " · 세트 " + n.set() + " 점수")
                    + ")",
                ch.toString(),
                nz(n.dps()) - nz(w.dps()),
                nz(n.ehp()) - nz(w.ehp())));
      }
    }
    return out;
  }

  private static final java.util.regex.Pattern SOCKET =
      java.util.regex.Pattern.compile("<Socket\\b[^>]*\\bnodeId=\"(\\d+)\"[^>]*/>");

  /**
   * 트리를 갈아 끼운 빌드에 원래 빌드의 주얼 소켓 장착(&lt;Sockets&gt;)을 되살린다 — 새 트리에도 찍힌 소켓만. withTree 는 &lt;Tree&gt; 를
   * 통째로 바꿔 소켓 장착이 사라졌다(10-14 실측: 얼음 화살 패스파인더 주얼 15개가 빠져 공통 트리 DPS −96.8%). 새 트리에 없는 소켓의 주얼은
   * 뺀다(인게임에서도 그 소켓을 안 찍으면 못 낀다).
   */
  static String keepSockets(String oldXml, String newXml, java.util.Set<Integer> nodes) {
    if (oldXml == null || newXml == null) {
      return newXml;
    }
    int a = oldXml.indexOf("<Sockets>");
    int b = oldXml.indexOf("</Sockets>");
    if (a < 0 || b < a || newXml.contains("<Sockets>")) {
      return newXml;
    }
    StringBuilder kept = new StringBuilder("<Sockets>");
    java.util.regex.Matcher m = SOCKET.matcher(oldXml.substring(a, b));
    while (m.find()) {
      if (nodes.contains(Integer.valueOf(m.group(1)))) {
        kept.append(m.group());
      }
    }
    kept.append("</Sockets>");
    int at = newXml.indexOf("<Overrides>");
    if (at < 0) {
      at = newXml.indexOf("</Spec>");
    }
    return at < 0 ? newXml : newXml.substring(0, at) + kept + newXml.substring(at);
  }

  static String unescapeXml(String s) {
    return s.replace("&apos;", "'")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&");
  }

  /** 아이템 본문 첫 줄들("Rarity: UNIQUE" 다음 줄이 이름)에 그 고유 이름이 있는가. */
  static boolean equipsUnique(String xml, String name) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("Rarity:\\s*UNIQUE\\s*\\n\\s*([^\\n]+)").matcher(xml);
    while (m.find()) {
      if (m.group(1).trim().equals(name)) {
        return true;
      }
    }
    return false;
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
    out.addAll(respecCandidates(g, objective));
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
    evals++; // 잠금을 잡은 스레드 하나만 쓴다
    Poe2PobEngineService.Result r = engine.calc(xml);
    if (r.error() != null || r.values().isEmpty()) {
      return null;
    }
    Map<String, Double> v = r.values();
    lastCostMask = Math.round(v.getOrDefault("MainSkillCostWarningMask", 0d));
    lastAttrMask =
        (v.getOrDefault("ReqStr", 0d) > v.getOrDefault("Str", 0d) ? 1L : 0L)
            | (v.getOrDefault("ReqDex", 0d) > v.getOrDefault("Dex", 0d) ? 2L : 0L)
            | (v.getOrDefault("ReqInt", 0d) > v.getOrDefault("Int", 0d) ? 4L : 0L);
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
