package net.luversof.api.poe.poe2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoePobImportService;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 시뮬레이터 — PoE1 시뮬레이터(/poe/sim)와 같은 흐름: 메인 스킬·전직(자동 가능)·적 시나리오를 넣고 실행하면 빌드를 만들어 준다.
 *
 * <p>PoE1 처럼 빈 빌드에서 트리·장비를 고르지 않는다 — PoE1 검증(2026-09-30)에서 그 방식은 생존력이 실빌드의 0.17~0.55배였다. 대신
 * poe.ninja 에서 그 조건의 **실빌드 후보 여러 명**을 받아 우리 엔진으로 같은 시나리오에서 재계산하고, 목표(실빌드 성향으로 자동 — PoE1 시뮬과 같은 규칙)에
 * 가장 맞는 한 명을 골라 **목표에 맞춰 자동으로 다듬는다**(Poe2RefineService: 공격 = DPS, 생존 = EHP, 균형 = 합).
 *
 * <p>다듬기와 같은 잠금을 쓴다(가이드·엔진을 수십 번 부른다).
 */
@Service
public class Poe2SimService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2SimService.class);

  /** 후보 인원 — 캐릭터 상세 요청 수(ninja 레이트리밋)와 재계산 시간의 타협. */
  static final int CANDIDATES = 5;

  /** 전직 자동일 때 비교할 전직 수(많이 쓰는 순) · 전직마다 후보 수 — C324. */
  static final int AUTO_ASCENDANCIES = 5;

  static final int AUTO_PER_ASCENDANCY = 2;

  /** 후보 한 명 — 캐릭터 이름은 내보내지 않는다(화면에 실을 이유가 없다). */
  /** set = 이 후보를 잰 무기 세트(1·2, DPS 가 큰 쪽) — 세트를 안 나누는 빌드면 null. */
  public record SimCandidate(
      Integer level,
      Double ninjaDps,
      Double ninjaEhp,
      Double dps,
      Double ehp,
      boolean chosen,
      String error,
      Integer set,
      // 이 후보의 전직(영문 · 한국어, 10-14 C311 — 전직 자동일 때 "왜 이 전직인가"를 전직별 비교로 보인다)
      String ascendancy,
      String ascendancyKo) {}

  /**
   * 시뮬레이션 결과.
   *
   * @param objective 실제로 쓴 목표(dps·ehp·balanced) — 실빌드 성향(lean)에서 정한다
   * @param start 고른 후보를 저장 왕복한 기준선, refined = 다듬은 뒤
   * @param code 다듬은 빌드 PoB 코드
   */
  public record SimResult(
      String skill,
      String skillKo,
      String ascendancy,
      String objective,
      String lean,
      String scenario,
      List<SimCandidate> candidates,
      Poe2NinjaService.Archetype benchmark,
      Poe2RefineService.Metrics start,
      Poe2RefineService.Metrics refined,
      List<Poe2RefineService.Step> steps,
      String code,
      long durationMs,
      // 무기 세트를 나눠 쓰는 빌드면 주 세트와 세트별 전/후(다듬기 결과 그대로), 아니면 null
      Integer mainSet,
      List<Poe2RefineService.SetMetrics> sets,
      // 고정 고유(영문 이름 · 한국어) — 없으면 null(10-01, 옛 이력엔 없다)
      String forcedUnique,
      String forcedUniqueKo,
      // "내 트리에서 출발"이면 그 트리의 노드 수(10-02) — 아니면 null
      Integer fromTreeNodes,
      // 고른 전직 한국어 · 전직을 자동으로 골랐는지(10-14 C311 사용자 요청 "어떤 전직인지 표시가 안 보인다") — 옛 이력엔 없다
      String ascendancyKo,
      Boolean ascendancyAuto,
      // 참고한 닌자 공통 특성(10-14 사용자 요청 "특정 1명이 아니라 그 직업/스킬의 특성을 참고") — 실빌드 수 · 공통 트리와 실빌드 트리의 겹침(0~1).
      //   공통 특성이 없는 아키타입(상위 60 밖)이면 0, 이 기능 전 이력은 null
      Integer consensusN,
      Double consensusOverlap,
      // 전직 자동일 때 전직별 비교(10-14 P5) — 전직마다 가장 나은 후보와, 거기에 그 전직의 닌자 공통 트리를 덮은 값. 고른 전직 = 둘 중 나은 점수가
      //   가장 큰 전직. 전직을 정했거나 옛 이력이면 null
      List<AscCompare> ascCompare,
      // 무기 세트 성격 + 그 세트를 다듬은 목표(10-15 C332 사용자 요청 "세트 1 · 2 가 어떤 특성에 치중해 계산한 것인지 보여 달라") — 공통 특성에
      //   세트 성격이 없거나(세트를 안 나누는 아키타입) 옛 이력이면 null
      List<SetRoleView> setRoles) {}

  /**
   * 세트 하나의 성격(C332) — kind · focus · weapon · weaponCount · active 는 닌자 공통 특성
   * 그대로(Poe2NinjaService.SetRole), n = 실빌드 수, objective = 시뮬레이터가 이 세트를 다듬은 목표(주 세트 = 고른 목표, 다른 세트 =
   * 역할 목표), main = 주 세트(DPS 가 큰 쪽)인지.
   */
  public record SetRoleView(
      int set,
      String kind,
      List<String> focus,
      String weapon,
      String weaponKo,
      int weaponCount,
      int active,
      int n,
      String objective,
      boolean main) {}

  /**
   * 전직 하나의 비교 줄(P5) — dps/ehp = 그 전직 최고 후보(엔진), treeDps/treeEhp = 그 후보에 공통 트리를 덮은 값(공통 특성이 없거나 계산
   * 실패면 null), consensusN = 공통 특성 실빌드 수, chosen = 고른 전직.
   */
  public record AscCompare(
      String ascendancy,
      String ascendancyKo,
      Double dps,
      Double ehp,
      Double treeDps,
      Double treeEhp,
      Integer consensusN,
      boolean chosen) {}

  /**
   * 내 트리(10-02, PoE1 /poe/sim?treeNodes= 의 짝) — 트리 화면에서 넘어온 직업·전직·노드·능력치 선택. 시뮬은 같은 직업의 실빌드에서 장비·젬을
   * 가져오고 트리만 이것으로 바꾼 뒤 다듬는다(다듬기는 고유·보조젬만 바꾸므로 트리는 그대로 남는다).
   */
  public record UserTree(
      String className,
      String ascendancy,
      List<Integer> nodes,
      java.util.Map<Integer, Integer> attrs,
      // 무기 세트 전용 노드(노드 → 1·2) — 10-02
      java.util.Map<Integer, Integer> sets) {}

  /**
   * phase = 단계 코드(fetch · measure · refine.&lt;다듬기 단계&gt; — 게이트가 옮긴다), round/rounds = 그 단계의 진행(후보
   * i/N, 다듬기 단계 r/3), evals = 엔진 계산 수, cancelled = 사용자가 중지한 마지막 실행(PoE1 simOptimizeStatus 의 중지 · 평가
   * 수 · 진행 막대 짝, 10-12 C269).
   */
  public record SimStatus(
      boolean running,
      String phase,
      int round,
      int rounds,
      SimResult result,
      String error,
      int evals,
      boolean cancelled) {}

  /** 결과 이력 상한 — PoE1 최적화 이력(HISTORY_LIMIT)과 같다. */
  static final int HISTORY_LIMIT = 30;

  /** 결과 이력 한 건의 목록용 요약(PoE1 OptimizeHistoryEntry 와 같은 역할). id = 저장 시각 epochMs. */
  public record HistoryEntry(
      long id,
      String skill,
      String skillKo,
      String ascendancy,
      String objective,
      String scenario,
      Double startDps,
      Double startEhp,
      Double refinedDps,
      Double refinedEhp,
      long durationMs,
      // 전직 한국어 이름 — 목록 배지가 한국어 화면에서도 "Deadeye" 로 보였다(10-03 C57, PoE1 ascendancyKo 와 같다)
      String ascendancyKo) {}

  private final Poe2NinjaService ninja;
  private final Poe2RefineService refine;
  private final Poe2DataService data;
  private final PoePobImportService decoder;
  private final Poe2BuildService builds;
  private final Path historyDir;
  private final JsonMapper json = JsonMapper.builder().build();
  private volatile boolean running;
  private volatile String phase = "";
  private volatile SimResult lastResult;
  private volatile String lastError;
  private volatile boolean lastCancelled;
  private volatile int candidate;
  private volatile int candidates;

  public Poe2SimService(
      Poe2NinjaService ninja,
      Poe2RefineService refine,
      Poe2DataService data,
      PoePobImportService decoder,
      Poe2BuildService builds,
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.builds = builds;
    this.ninja = ninja;
    this.refine = refine;
    this.data = data;
    this.decoder = decoder;
    this.historyDir = Path.of(dataDir, "sim", "history");
  }

  // ── 결과 이력 — PoE1 PoeOptimizeService 의 이력과 같은 방식(sim/history/<epochMs>.json, 최신 30건, 전역) ──

  /** 완료 결과를 이력 파일로 남기고 상한을 넘는 오래된 건을 정리한다. */
  private void saveHistory(SimResult result) {
    try {
      Files.createDirectories(historyDir);
      Files.writeString(
          historyDir.resolve(System.currentTimeMillis() + ".json"),
          json.writeValueAsString(result),
          StandardCharsets.UTF_8);
      try (var stream = Files.list(historyDir)) {
        List<Path> files =
            stream
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing((Path f) -> f.getFileName().toString()).reversed())
                .toList();
        for (int i = HISTORY_LIMIT; i < files.size(); i++) {
          try {
            Files.deleteIfExists(files.get(i));
          } catch (java.io.IOException ignore) {
            // 개별 삭제 실패는 무시 — 다음 저장에서 다시 정리된다
          }
        }
      }
    } catch (Exception e) {
      logger.warn("PoE2 시뮬 이력 저장 실패", e);
    }
  }

  /** 최근 결과 목록(최신순, 최대 HISTORY_LIMIT). */
  public List<HistoryEntry> history() {
    if (!Files.exists(historyDir)) {
      return List.of();
    }
    try (var stream = Files.list(historyDir)) {
      return stream
          .filter(f -> f.getFileName().toString().endsWith(".json"))
          .sorted(Comparator.comparing((Path f) -> f.getFileName().toString()).reversed())
          .limit(HISTORY_LIMIT)
          .map(
              f -> {
                try {
                  long id = Long.parseLong(f.getFileName().toString().replace(".json", ""));
                  SimResult r = json.readValue(Files.readString(f), SimResult.class);
                  return new HistoryEntry(
                      id,
                      r.skill(),
                      r.skillKo(),
                      r.ascendancy(),
                      r.objective(),
                      r.scenario(),
                      r.start() == null ? null : r.start().dps(),
                      r.start() == null ? null : r.start().ehp(),
                      r.refined() == null ? null : r.refined().dps(),
                      r.refined() == null ? null : r.refined().ehp(),
                      r.durationMs(),
                      r.ascendancy() == null
                          ? null
                          : data.treeIndex().ascendancyKo().get(r.ascendancy()));
                } catch (Exception e) {
                  return null; // 깨진 파일 한 건이 목록 전체를 막지 않게
                }
              })
          .filter(java.util.Objects::nonNull)
          .toList();
    } catch (java.io.IOException e) {
      logger.warn("PoE2 시뮬 이력 조회 실패", e);
      return List.of();
    }
  }

  /** 이력 결과 한 건 전체(id = 저장 시각 epochMs) — 없거나 깨졌으면 null. */
  public SimResult historyResult(long id) {
    Path file = historyDir.resolve(id + ".json");
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return json.readValue(Files.readString(file), SimResult.class);
    } catch (Exception e) {
      logger.warn("PoE2 시뮬 이력 로드 실패: {}", id, e);
      return null;
    }
  }

  /** 이력 한 건 삭제 — 디렉토리 이탈 방지(PoE1 과 같은 가드). 삭제했으면 true. */
  public boolean deleteHistory(long id) {
    Path file = historyDir.resolve(id + ".json");
    if (!file.normalize().startsWith(historyDir.normalize())) {
      return false;
    }
    try {
      return Files.deleteIfExists(file);
    } catch (Exception e) {
      logger.warn("PoE2 시뮬 이력 삭제 실패: {}", id, e);
      return false;
    }
  }

  public SimStatus status() {
    boolean refining = running && "refine".equals(phase);
    return new SimStatus(
        running,
        refining ? "refine." + refine.phase() : phase,
        refining ? refine.round() : "measure".equals(phase) ? candidate : 0,
        refining ? Poe2RefineService.ROUNDS : "measure".equals(phase) ? candidates : 0,
        lastResult,
        lastError,
        refine.evals(),
        lastCancelled);
  }

  /** 실행 중인 시뮬레이션 중지 — 돌고 있지 않으면 false. 다음 후보·다듬기 단계 경계에서 끝나고 cancelled 로 남는다. */
  public boolean cancel() {
    return running && refine.requestCancel();
  }

  /**
   * 시작 — 이미 (시뮬·다듬기가) 돌고 있으면 false. scenario = None | Boss | Pinnacle | Uber | keep(플레이어 설정 그대로).
   */
  public boolean start(String skill, String ascendancy, String scenario) {
    return start(skill, ascendancy, scenario, null);
  }

  /** uniqueSlug = 고정할 고유(방어구·장신구, {@link Poe2RefineService#FORCE_SLOTS}) — 고정할 수 없는 고유면 false. */
  public boolean start(String skill, String ascendancy, String scenario, String uniqueSlug) {
    return start(skill, ascendancy, scenario, uniqueSlug, null);
  }

  /** tree = 내 트리에서 출발(없으면 null). 전직은 트리의 전직을 쓴다. */
  public boolean start(
      String skill, String ascendancy, String scenario, String uniqueSlug, UserTree tree) {
    if (tree != null) {
      if (tree.className() == null
          || !data.treeIndex().classIntegerId().containsKey(tree.className())) {
        lastError = "알 수 없는 직업의 트리입니다: " + tree.className();
        return false;
      }
      ascendancy = tree.ascendancy() == null ? "" : tree.ascendancy();
      // 다른 직업의 전직을 고르면 후보마다 트리 교체가 실패한다 — 시작 전에 거절
      if (!ascendancy.isEmpty()
          && !data.treeIndex()
              .classAscendancies()
              .getOrDefault(tree.className(), List.of())
              .contains(ascendancy)) {
        lastError = "트리 직업(" + tree.className() + ")의 전직이 아닙니다: " + ascendancy;
        return false;
      }
    }
    final UserTree userTree = tree;
    Poe2RefineService.Forced forced = null;
    if (uniqueSlug != null && !uniqueSlug.isBlank()) {
      // "slug:변형번호" — 고유 상세에서 고른 변형(10-04 C96, PoE1 C95 짝). 변형이면 그 변형 줄로 원문을
      // 짠다(Poe2BuildService.variantText)
      String us = uniqueSlug.trim();
      Integer variantIndex =
          us.matches(".+:[0-9]+") ? Integer.valueOf(us.substring(us.lastIndexOf(':') + 1)) : null;
      Poe2.Unique u =
          data.unique(variantIndex != null ? us.substring(0, us.lastIndexOf(':')) : us)
              .orElse(null);
      Poe2.UniqueVariant variant =
          u == null || variantIndex == null || u.variants() == null
              ? null
              : u.variants().stream()
                  .filter(v -> variantIndex.equals(v.index()))
                  .findFirst()
                  .orElse(null);
      List<String> slots = u == null ? null : Poe2RefineService.FORCE_SLOTS.get(u.itemClass());
      // 같은 이름 고유가 여럿이면 이 고유의 원문을(C73 — 이름만으로는 마지막 변형이 나왔다)
      String raw =
          u == null || slots == null
              ? null
              : variant != null ? Poe2BuildService.variantText(u, variant) : data.uniqueRaw(u);
      if (raw == null) {
        lastError = "고정할 수 없는 고유입니다(방어구·장신구만): " + uniqueSlug;
        return false;
      }
      String label = variant == null ? "" : " (" + variant.name() + ")";
      String labelKo =
          variant == null
              ? ""
              : " (" + (variant.nameKo() != null ? variant.nameKo() : variant.name()) + ")";
      forced =
          new Poe2RefineService.Forced(
              u.name() + label, (u.nameKo() != null ? u.nameKo() : u.name()) + labelKo, slots, raw);
    }
    if (skill == null || skill.isBlank() || !refine.tryLock()) {
      return false;
    }
    final Poe2RefineService.Forced lockedUnique = forced;
    running = true;
    lastResult = null;
    lastError = null;
    lastCancelled = false;
    candidate = 0;
    candidates = 0;
    phase = "fetch";
    String asc = ascendancy == null ? "" : ascendancy.trim();
    String scen = scenario == null || scenario.isBlank() ? "Pinnacle" : scenario.trim();
    Thread thread =
        new Thread(
            () -> {
              try {
                lastResult = run(skill.trim(), asc, scen, lockedUnique, userTree);
                saveHistory(lastResult);
              } catch (Poe2RefineService.Cancelled e) {
                logger.info("PoE2 시뮬레이션 중지(사용자 요청)");
                lastCancelled = true;
              } catch (Throwable e) {
                logger.warn("PoE2 시뮬레이션 실패", e);
                lastError = e.getMessage();
              } finally {
                phase = "";
                running = false;
                refine.unlock();
              }
            },
            "poe2-sim");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private SimResult run(
      String skill,
      String ascendancy,
      String scenario,
      Poe2RefineService.Forced forced,
      UserTree tree) {
    // 내 트리면 같은 직업의 실빌드만 후보로(다른 직업 장비·젬에 트리만 바꾸면 시작점이 달라 트리가 끊긴다)
    java.util.Set<String> treeClassAscs =
        tree == null
            ? null
            : new java.util.HashSet<>(
                data.treeIndex().classAscendancies().getOrDefault(tree.className(), List.of()));
    long startedAt = System.currentTimeMillis();
    // 후보·성향은 poe.ninja 스냅샷(새 버전이 나오면 Poe2NinjaSyncService 가 다시 받는다)의 캐릭터에서 — 메인 스킬 기준
    Poe2NinjaService.Archetype bench = ninja.findOrSkill(ascendancy, skill);
    String lean = bench != null ? bench.lean() : "balanced";
    String objective = "dps".equals(lean) || "ehp".equals(lean) ? lean : "balanced";
    List<Poe2NinjaService.NinjaBuild> pool =
        ninja.builds().stream()
            .filter(b -> skill.equals(b.mainSkill()))
            .filter(b -> ascendancy.isEmpty() || ascendancy.equals(b.ascendancy()))
            .filter(b -> treeClassAscs == null || treeClassAscs.contains(b.ascendancy()))
            .filter(b -> b.account() != null && b.name() != null)
            .sorted(
                Comparator.comparing(
                        (Poe2NinjaService.NinjaBuild b) -> b.level() == null ? 0 : b.level())
                    .reversed()
                    .thenComparing(
                        Comparator.comparingDouble(
                                (Poe2NinjaService.NinjaBuild b) -> ninjaScore(objective, b))
                            .reversed()))
            .toList();
    if (ascendancy.isEmpty()) {
      // 전직 자동(10-14 C324): 레벨 순 상위 5명이면 전직 2~3개만 비교됐다(불꽃: 공통 특성이 있는 전직 5개 중 3개) — 전직 5개에서 2명씩.
      //   전직 순위는 인원이 아니라 그 전직 최고 후보의 닌자 점수(목표 기준) — 인원 순이면 인원은 적지만 가장 센 젬링 군단병(불꽃 후보 9.7M)이 빠져
      //   최종이 19M → 13.4M 으로 낮아졌다
      pool = pool.stream().filter(b -> b.ascendancy() != null).toList();
      java.util.Map<String, Double> bestOfAsc = new java.util.HashMap<>();
      for (Poe2NinjaService.NinjaBuild b : pool) {
        bestOfAsc.merge(b.ascendancy(), ninjaScore(objective, b), Math::max);
      }
      java.util.Set<String> topAscs =
          bestOfAsc.entrySet().stream()
              .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
              .limit(AUTO_ASCENDANCIES)
              .map(java.util.Map.Entry::getKey)
              .collect(java.util.stream.Collectors.toSet());
      java.util.Map<String, Integer> taken = new java.util.HashMap<>();
      pool =
          pool.stream()
              .filter(b -> topAscs.contains(b.ascendancy()))
              .filter(b -> taken.merge(b.ascendancy(), 1, Integer::sum) <= AUTO_PER_ASCENDANCY)
              .toList();
    } else {
      pool = pool.stream().limit(CANDIDATES).toList();
    }
    if (pool.isEmpty()) {
      throw new IllegalStateException(
          "이 조건의 poe.ninja 실빌드가 없습니다"
              + (ascendancy.isEmpty() ? "" : " — 전직을 '자동'으로 두면 다른 전직 실빌드에서 찾습니다"));
    }
    List<SimCandidate> cands = new ArrayList<>();
    String bestXml = null;
    Poe2NinjaService.NinjaBuild bestBuild = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    int i = 0;
    int storedHits = 0;
    // 전직별 최고 후보 {xml, Metrics, NinjaBuild} — P5 전직 비교용
    java.util.Map<String, Object[]> ascBest = new java.util.LinkedHashMap<>();
    candidates = pool.size();
    phase = "measure";
    // 후보 준비 · 측정을 동시에(10-15 C330 — 저장 코드라 poe.ninja 요청이 거의 없다). 고르는 규칙은 아래에서 원래 순서대로
    final String scen = scenario;
    final UserTree userTree = tree;
    List<java.util.concurrent.Future<Object[]>> prepared = new ArrayList<>();
    for (Poe2NinjaService.NinjaBuild b : pool) {
      prepared.add(measurePool.submit(() -> prepareCandidate(b, skill, scen, userTree)));
    }
    for (int pi = 0; pi < pool.size(); pi++) {
      Poe2NinjaService.NinjaBuild b = pool.get(pi);
      refine.checkCancel();
      i++;
      candidate = i;
      Object[] prep;
      try {
        prep = prepared.get(pi).get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("중단됨", e);
      } catch (java.util.concurrent.ExecutionException e) {
        prep = new Object[] {null, null, null, String.valueOf(e.getCause()), false};
      }
      String xml = (String) prep[0];
      Poe2RefineService.Metrics m = (Poe2RefineService.Metrics) prep[1];
      Integer set = (Integer) prep[2];
      String error = (String) prep[3];
      if (Boolean.TRUE.equals(prep[4])) {
        storedHits++;
      }
      boolean pick = false;
      if (m != null && b.ascendancy() != null) {
        Object[] prev = ascBest.get(b.ascendancy());
        if (prev == null
            || candidateScore(objective, m)
                > candidateScore(objective, (Poe2RefineService.Metrics) prev[1])) {
          ascBest.put(b.ascendancy(), new Object[] {xml, m, b});
        }
      }
      if (m != null) {
        double sc = candidateScore(objective, m);
        if (sc > bestScore) {
          bestScore = sc;
          bestXml = xml;
          bestBuild = b;
          pick = true;
        }
      }
      cands.add(
          new SimCandidate(
              b.level(),
              b.dps(),
              b.ehp(),
              m == null ? null : m.dps(),
              m == null ? null : m.ehp(),
              pick,
              error,
              set,
              b.ascendancy(),
              b.ascendancy() == null ? null : data.treeIndex().ascendancyKo().get(b.ascendancy())));
    }
    logger.info("PoE2 시뮬 후보 {}명 중 저장 코드 {}명(나머지는 poe.ninja 실시간)", pool.size(), storedHits);
    if (bestXml == null) {
      throw new IllegalStateException("실빌드 후보를 하나도 계산하지 못했습니다(poe.ninja 응답·엔진 확인)");
    }
    // P5(10-14 사용자 요청 "내가 선택한 직업/스킬에 대해 닌자 특성을 참고해 최선의 결과") — 전직 자동이면 전직마다 최고 후보에 그 전직의 공통 트리를 덮어
    //   재 보고, 둘 중 나은 점수로 전직을 고른다(후보만 보면 공통 트리로 크게 오르는 전직을 놓친다). 전직당 엔진 1~2회
    List<AscCompare> ascCompare = null;
    if (ascendancy.isEmpty() && tree == null && ascBest.size() >= 2) {
      phase = "compare";
      ascCompare = new ArrayList<>();
      double topScore = Double.NEGATIVE_INFINITY;
      String topAsc = null;
      for (java.util.Map.Entry<String, Object[]> e : ascBest.entrySet()) {
        refine.checkCancel();
        String asc = e.getKey();
        String candXml = (String) e.getValue()[0];
        Poe2RefineService.Metrics cm = (Poe2RefineService.Metrics) e.getValue()[1];
        Poe2NinjaService.Consensus c = ninja.consensus(asc, skill);
        Poe2RefineService.Metrics tm = null;
        if (c != null && c.nodes().size() > 1) {
          try {
            String treeXml =
                Poe2RefineService.keepSockets(
                    candXml,
                    builds.withTree(
                        candXml, c.className(), c.ascendancy(), c.nodes(), c.attrs(), c.sets()),
                    new java.util.HashSet<>(c.nodes()));
            Poe2RefineService.Best tb = refine.measureBest(treeXml);
            tm = tb == null ? null : tb.metrics();
          } catch (RuntimeException ex) {
            logger.info("PoE2 전직 비교 공통 트리 실패({}): {}", asc, ex.getMessage());
          }
        }
        double sc =
            Math.max(
                candidateScore(objective, cm),
                tm == null || tm.dps() <= 0
                    ? Double.NEGATIVE_INFINITY
                    : candidateScore(objective, tm));
        if (sc > topScore) {
          topScore = sc;
          topAsc = asc;
        }
        ascCompare.add(
            new AscCompare(
                asc,
                data.treeIndex().ascendancyKo().get(asc),
                cm.dps(),
                cm.ehp(),
                tm == null ? null : tm.dps(),
                tm == null ? null : tm.ehp(),
                c == null ? null : c.n(),
                false));
      }
      if (topAsc != null && bestBuild != null && !topAsc.equals(bestBuild.ascendancy())) {
        logger.info("PoE2 전직 비교: 공통 트리까지 견주면 {} → {}", bestBuild.ascendancy(), topAsc);
        bestXml = (String) ascBest.get(topAsc)[0];
        bestBuild = (Poe2NinjaService.NinjaBuild) ascBest.get(topAsc)[2];
      }
      final String picked = topAsc;
      ascCompare =
          ascCompare.stream()
              .map(
                  a ->
                      new AscCompare(
                          a.ascendancy(),
                          a.ascendancyKo(),
                          a.dps(),
                          a.ehp(),
                          a.treeDps(),
                          a.treeEhp(),
                          a.consensusN(),
                          a.ascendancy().equals(picked)))
              .toList();
    }
    // 고른 후보만 chosen=true 로 남긴다(루프 중 갱신된 앞 후보의 표시는 지운다)
    final Poe2NinjaService.NinjaBuild chosen = bestBuild;
    List<SimCandidate> marked = new ArrayList<>();
    for (int k = 0; k < cands.size(); k++) {
      SimCandidate c = cands.get(k);
      marked.add(
          new SimCandidate(
              c.level(),
              c.ninjaDps(),
              c.ninjaEhp(),
              c.dps(),
              c.ehp(),
              pool.get(k) == chosen,
              c.error(),
              c.set(),
              c.ascendancy(),
              c.ascendancyKo()));
    }
    refine.checkCancel();
    phase = "refine";
    String chosenAsc = chosen.ascendancy();
    // 고른 실빌드의 전직 · 스킬 아키타입 공통 특성 — 내 트리에서 출발하면 트리는 견주지 않는다(고유 · 보조 젬만)
    Poe2NinjaService.Consensus consensus = ninja.consensus(chosenAsc, skill);
    if (consensus != null && tree != null) {
      consensus =
          new Poe2NinjaService.Consensus(
              consensus.n(),
              consensus.className(),
              consensus.ascendancy(),
              List.of(),
              consensus.attrs(),
              consensus.sets(),
              consensus.overlap(),
              consensus.supports(),
              consensus.slotUniques(),
              consensus.nodeCounts(),
              consensus.setRoles());
    }
    Poe2RefineService.Result r = refine.refineWith(bestXml, objective, forced, consensus);
    if (ascendancy.isEmpty() && chosenAsc != null) {
      Poe2NinjaService.Archetype exact = ninja.find(chosenAsc, skill);
      if (exact != null) {
        bench = exact; // 전직 자동이면 고른 후보의 전직 아키타입과 견준다(없으면 스킬 단위 그대로)
      }
    }
    return new SimResult(
        skill,
        data.gemByName(skill).map(Poe2.Gem::nameKo).orElse(null),
        chosenAsc,
        objective,
        lean,
        scenario,
        List.copyOf(marked),
        bench,
        r.before(),
        r.after(),
        r.steps(),
        r.code(),
        System.currentTimeMillis() - startedAt,
        r.mainSet(),
        r.sets(),
        forced == null ? null : forced.name(),
        forced == null ? null : forced.label(),
        tree == null ? null : tree.nodes().size(),
        chosenAsc == null ? null : data.treeIndex().ascendancyKo().get(chosenAsc),
        ascendancy.isEmpty(),
        consensus == null ? 0 : consensus.n(), // 0 = 공통 특성 집계 대상 아님(null 은 그 전 이력)
        consensus == null || tree != null ? null : consensus.overlap(),
        ascCompare,
        setRoleViews(consensus, r.mainSet(), objective));
  }

  /** 공통 특성의 세트 성격 + 다듬은 목표(C332) — 결과가 세트를 안 나누거나(mainSet null) 성격이 없으면 null. */
  static List<SetRoleView> setRoleViews(
      Poe2NinjaService.Consensus consensus, Integer mainSet, String objective) {
    if (consensus == null || consensus.setRoles() == null || mainSet == null) {
      return null;
    }
    Poe2NinjaService.SetRoles sr = consensus.setRoles();
    return sr.sets().stream()
        .map(
            x ->
                new SetRoleView(
                    x.set(),
                    x.kind(),
                    x.focus(),
                    x.weapon(),
                    x.weaponKo(),
                    x.weaponCount(),
                    x.active(),
                    sr.n(),
                    x.set() == mainSet ? objective : Poe2RefineService.roleObjective(x.kind()),
                    x.set() == mainSet))
        .toList();
  }

  /** 후보 측정 풀(C330) — 엔진 자체 상한 안에서. poe.ninja 실시간 요청이 섞여도 넷 이하. */
  private final java.util.concurrent.ExecutorService measurePool =
      java.util.concurrent.Executors.newFixedThreadPool(
          4,
          r -> {
            Thread t = new Thread(r, "poe2-sim-measure");
            t.setDaemon(true);
            return t;
          });

  /** 후보 하나 준비 + 측정 — {xml, Metrics, 세트, 오류, 저장 코드 여부}. 동시에 불린다. */
  private Object[] prepareCandidate(
      Poe2NinjaService.NinjaBuild b, String skill, String scenario, UserTree tree) {
    String error = null;
    Poe2RefineService.Metrics m = null;
    Integer set = null;
    String xml = null;
    boolean storedHit = false;
    try {
      // 저장된 실빌드 코드가 있으면 그것(같은 스냅샷 · 같은 사람 — fetch-ninja-start2 가 재며 남김, 10-14 C319), 없으면 poe.ninja
      // 실시간
      String stored = ninja.memberCode(b.ascendancy(), skill, b.account(), b.name());
      String code = stored != null ? stored : ninja.characterCode(b);
      storedHit = stored != null;
      xml = withScenario(alignMainGroup(decoder.decodeToXml(code), skill), scenario);
      if (tree != null) {
        // 실빌드 주얼은 내 트리에도 찍힌 소켓에서만 유지(10-14 — withTree 가 소켓 장착을 지워 주얼이 다 빠졌다)
        xml =
            Poe2RefineService.keepSockets(
                xml,
                builds.withTree(
                    xml,
                    tree.className(),
                    tree.ascendancy(),
                    tree.nodes(),
                    tree.attrs(),
                    tree.sets()),
                new java.util.HashSet<>(tree.nodes()));
      }
      // 무기 세트를 나눠 쓰는 빌드는 두 세트를 재고 DPS 가 큰 쪽으로 견준다(저장 당시 켜진 세트가 주 세트가 아닐 수 있다)
      Poe2RefineService.Best best = refine.measureBest(xml);
      m = best == null ? null : best.metrics();
      if (best != null && best.set() > 0) {
        set = best.set();
        xml = Poe2WeaponSets.withSet(xml, set);
      }
      if (m == null || m.dps() <= 0) {
        error = "계산 실패";
        m = null;
      }
    } catch (RuntimeException e) {
      error = e.getMessage();
    }
    return new Object[] {xml, m, set, error, storedHit};
  }

  /** 후보 정렬용 ninja 표기값 점수 — 같은 레벨 안에서 목표 축이 큰 사람부터. */
  private static double ninjaScore(String objective, Poe2NinjaService.NinjaBuild b) {
    double dps = b.dps() == null ? 0 : b.dps();
    double ehp = b.ehp() == null ? 0 : b.ehp();
    if ("dps".equals(objective)) {
      return dps;
    }
    if ("ehp".equals(objective)) {
      return ehp;
    }
    return Math.log(Math.max(dps, 1)) + Math.log(Math.max(ehp, 1));
  }

  /** 우리 엔진 재계산값으로 고를 때의 점수 — 균형은 두 축의 로그 합(한 축만 큰 사람을 피한다). */
  static double candidateScore(String objective, Poe2RefineService.Metrics m) {
    if ("dps".equals(objective)) {
      return m.dps();
    }
    if ("ehp".equals(objective)) {
      return m.ehp();
    }
    return Math.log(Math.max(m.dps(), 1)) + Math.log(Math.max(m.ehp(), 1));
  }

  private static final Pattern SKILL_GROUP = Pattern.compile("<Skill\\b[^>]*>[\\s\\S]*?</Skill>");
  private static final Pattern NAME_SPEC = Pattern.compile("nameSpec=\"([^\"]+)\"");
  private static final Pattern SAVED_MAIN =
      Pattern.compile("<Build\\b[^>]*\\bmainSocketGroup=\"(\\d+)\"");

  /**
   * 메인 그룹을 그 스킬로 — tools/poe-extract/ninja-normalize.mjs alignMainGroup(preferSavedMain)과 같은 규칙: ①
   * 저장 메인 그룹에 그 스킬이 있으면 그대로 ② 그 스킬이 첫 젬인 그룹(아이템·트리 부여 그룹 제외) ③ 없으면 저장 메인 그대로(보조젬이 만드는 스킬 등).
   */
  static String alignMainGroup(String xml, String skill) {
    List<String> groups = new ArrayList<>();
    Matcher g = SKILL_GROUP.matcher(xml);
    while (g.find()) {
      groups.add(g.group());
    }
    Matcher sm = SAVED_MAIN.matcher(xml);
    int saved = sm.find() ? Integer.parseInt(sm.group(1)) - 1 : -1;
    if (saved >= 0 && saved < groups.size() && gems(groups.get(saved)).contains(skill)) {
      return xml;
    }
    for (int i = 0; i < groups.size(); i++) {
      String tag = groups.get(i).substring(0, groups.get(i).indexOf('>') + 1);
      List<String> gs = gems(groups.get(i));
      if (!tag.contains("source=\"") && !gs.isEmpty() && gs.get(0).equals(skill)) {
        return xml.replaceFirst(
            "(<Build\\b[^>]*?)mainSocketGroup=\"\\d+\"", "$1mainSocketGroup=\"" + (i + 1) + "\"");
      }
    }
    return xml;
  }

  private static List<String> gems(String group) {
    List<String> out = new ArrayList<>();
    Matcher m = NAME_SPEC.matcher(group);
    while (m.find()) {
      out.add(m.group(1));
    }
    return out;
  }

  /** 적 시나리오(enemyIsBoss) — 활성 설정 묶음에 넣거나 바꾼다. keep 이면 플레이어 설정 그대로. */
  static String withScenario(String xml, String scenario) {
    if (scenario == null || "keep".equals(scenario)) {
      return xml;
    }
    String input = "<Input name=\"enemyIsBoss\" string=\"" + scenario + "\"/>";
    String replaced = xml.replaceAll("<Input\\b[^>]*\\bname=\"enemyIsBoss\"[^>]*/>", input);
    if (!replaced.equals(xml)) {
      return replaced;
    }
    Matcher active = Pattern.compile("<Config\\b[^>]*\\bactiveConfigSet=\"([^\"]*)\"").matcher(xml);
    if (active.find()) {
      Matcher set =
          Pattern.compile(
                  "<ConfigSet\\b[^>]*\\bid=\"" + Pattern.quote(active.group(1)) + "\"[^>]*>")
              .matcher(xml);
      if (set.find()) {
        return xml.substring(0, set.end()) + input + xml.substring(set.end());
      }
    }
    Matcher config = Pattern.compile("<Config\\b[^>]*>").matcher(xml);
    if (config.find()) {
      return xml.substring(0, config.end()) + input + xml.substring(config.end());
    }
    return xml;
  }
}
