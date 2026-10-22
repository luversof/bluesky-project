package net.luversof.api.poe.poe2;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 poe.ninja 실빌드 — 아키타입(전직×메인 스킬) 집계와 실빌드 출발점. 산출: tools/poe2-extract/fetch-ninja-builds2.mjs →
 * ninja-archetypes2.json, fetch-ninja-start2.mjs →
 * ninja-start-builds2.json(~/.poe-gamedata/poe2/ninja). 파일이 바뀌면 다음 조회에서 다시 읽는다(재시작 불필요).
 */
@Service
public class Poe2NinjaService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2NinjaService.class);

  public record Count(String name, int count) {}

  /**
   * 아키타입 — lean = 전체 중앙값 대비 성향(dps 공격 특화 · ehp 생존 특화 · balanced), facets = 그 전직 전체 모집단의 아이템·키스톤·도유
   * 사용 수.
   */
  public record Archetype(
      String ascendancy,
      String mainSkill,
      int sample,
      Double medianLevel,
      Double medianLife,
      Double medianEs,
      Double medianEhp,
      Double medianDps,
      String lean,
      List<Count> topKeystones,
      List<Count> topCoSkills,
      Integer facetTotal,
      List<Count> topItems,
      List<Count> topAnointed,
      RealStart start) {}

  /**
   * 실빌드 출발점 — 여러 명을 우리 엔진으로 재계산해 (DPS, EHP) 중앙값에 가장 가까운 사람. code = PoB 코드(플레이어 Config 그대로, 메인 그룹만
   * 맞춤).
   */
  public record RealStart(
      Integer level,
      Double dps,
      Double ehp,
      Double life,
      Double es,
      int measured,
      Double medianDps,
      Double medianEhp,
      String code) {}

  public record Overview(
      String league, Double globalMedianDps, Double globalMedianEhp, List<Archetype> archetypes) {}

  /** 실빌드 캐릭터 한 명(ninja 검색 행) — 시뮬레이터가 후보를 고르고 캐릭터 상세(PoB 코드)를 받는 데 쓴다. */
  public record NinjaBuild(
      String name,
      String account,
      String ascendancy,
      Integer level,
      Double life,
      Double es,
      Double ehp,
      Double dps,
      String mainSkill) {}

  /**
   * 아키타입 공통 특성(10-14 사용자 요청 — 시뮬레이터가 실빌드 한 명이 아니라 그 직업/스킬 실빌드들의 특성을 참고하도록). 산출:
   * tools/poe2-extract/archetype-consensus2.mjs → ninja-archetype-consensus2.json. n = 실빌드 수, tree
   * = 빈도로 키운 공통 트리(본 · 전직 · 무기 세트 1/2 — 세트 전용 포인트를 따로 센다), supports = 메인 스킬 보조 젬별 인원, slotUniques =
   * 장비 칸별 고유 이름별 인원.
   */
  public record Consensus(
      int n,
      String className,
      String ascendancy,
      List<Integer> nodes,
      java.util.Map<Integer, Integer> attrs,
      java.util.Map<Integer, Integer> sets,
      double overlap,
      java.util.Map<String, Integer> supports,
      java.util.Map<String, java.util.Map<String, Integer>> slotUniques,
      // 본 트리 노드별 인원(10-14 C315 — 공통 트리 통째 교체는 8/8 거절돼, 많이 찍는 노드를 하나씩 재분배로 넣어 본다)
      java.util.Map<Integer, Integer> nodeCounts,
      // 무기 세트 성격(10-15 C332) — 세트를 나눠 쓰는 실빌드가 절반 미만이거나 옛 파일이면 null
      SetRoles setRoles) {

    public Consensus(
        int n,
        String className,
        String ascendancy,
        List<Integer> nodes,
        java.util.Map<Integer, Integer> attrs,
        java.util.Map<Integer, Integer> sets,
        double overlap,
        java.util.Map<String, Integer> supports,
        java.util.Map<String, java.util.Map<String, Integer>> slotUniques,
        java.util.Map<Integer, Integer> nodeCounts) {
      this(
          n,
          className,
          ascendancy,
          nodes,
          attrs,
          sets,
          overlap,
          supports,
          slotUniques,
          nodeCounts,
          null);
    }

    /** 그 세트의 역할(attack · defense · support) — 모르면 null. */
    public String roleOf(int set) {
      return setRoles == null
          ? null
          : setRoles.sets().stream()
              .filter(r -> r.set() == set)
              .map(SetRole::kind)
              .findFirst()
              .orElse(null);
    }
  }

  /**
   * 무기 세트 성격(10-15 C332 사용자 요청 "세트 1 에서 버프, 세트 2 에서 공격력 · 한쪽은 광역, 한쪽은 보스 · 한쪽은 지속 피해, 한쪽은 치명타 — 닌자
   * 실빌드들의 세트 1 · 2 성격을 파악해 시뮬레이션하고 보여 달라"). archetype-consensus2.mjs setRoles 산출 — n = 실빌드 수, used
   * = 세트 전용 노드를 찍은 실빌드 수.
   */
  public record SetRoles(int n, int used, List<SetRole> sets) {}

  /**
   * 세트 하나 — kind = attack(공격) · defense(방어) · support(버프 · 소환 · 속도 · 명중), focus = 상대 세트보다 더 치중한
   * 특성(damage · crit · dot · area · boss · speed · accuracy · buff · minion · defense, 큰 순 최대 3),
   * weapon = 그 세트 무기 종류 다수결("Bow+Quiver", 비면 "none") · weaponKo = 그 한국어("활 + 화살통", 없으면 "") ·
   * weaponCount = 그 인원, active = 저장 당시 이 세트를 켜 둔 인원.
   */
  public record SetRole(
      int set,
      String kind,
      List<String> focus,
      String weapon,
      String weaponKo,
      int weaponCount,
      int active) {}

  /** 선택지 한 줄 — 이름(영문, 요청 값), 한국어 이름. 인원은 싣지 않는다(순서에만 쓴다). */
  public record Choice(String name, String nameKo) {}

  /** 전직 셀렉트의 직업별 묶음(PoE1 시뮬 폼과 같은 optgroup) — 트리 순서. */
  public record ClassGroup(String name, String nameKo, List<Choice> ascendancies) {}

  /**
   * 시뮬레이터 폼 선택지 — 목록은 게임 데이터 전부, 순서만 스냅샷 집계. snapshot·fetchedAt = 순서의 근거(poe.ninja 버전, 받은 시각 — 새
   * 버전이 나오면 Poe2NinjaSyncService 가 다시 받는다).
   *
   * <p>ascendancies = 많이 쓰는 순 전체, popularAscendancies = 그중 실제로 쓰이는 상위 5개(이름만 — "많이 쓰는 직업" 묶음),
   * classes = 직업별 묶음.
   */
  public record Options(
      String league,
      String snapshot,
      String fetchedAt,
      List<Choice> skills,
      List<Choice> ascendancies,
      List<String> popularAscendancies,
      List<ClassGroup> classes) {}

  private final Path archFile;
  private final Path startFile;
  private final Path buildsFile;
  private final Path consensusFile;
  private final Path membersFile;
  private volatile FileTime membersAt;
  private volatile java.util.Map<String, String> memberCodes = java.util.Map.of();
  private volatile FileTime consensusAt;
  private volatile java.util.Map<String, Consensus> consensus = java.util.Map.of();
  private final Poe2DataService data;
  private volatile FileTime buildsAt;
  private volatile List<NinjaBuild> builds = List.of();
  private volatile String buildsLeague;
  private volatile String buildsSnapshot;
  private volatile String buildsFetchedAt;

  /** 전직별 전체 모집단 수(개요 패싯) — 표본은 전직마다 top-100 이라, 순서는 이걸로 가중해 추정한다. 없으면(옛 파일) 가중 없이. */
  private volatile java.util.Map<String, Integer> classPopulation = java.util.Map.of();

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** 스킬 단위 집계(전직 무관) — 전직을 "자동"으로 두면 이걸로 성향·중앙값을 본다. */
  private volatile java.util.Map<String, Archetype> skillAgg = java.util.Map.of();

  private final JsonMapper json = JsonMapper.builder().build();
  private volatile FileTime archAt;
  private volatile FileTime startAt;
  private volatile Overview cached = new Overview(null, null, null, List.of());

  public Poe2NinjaService(
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir,
      Poe2DataService data) {
    this.archFile = Path.of(dataDir, "ninja", "ninja-archetypes2.json");
    this.startFile = Path.of(dataDir, "ninja", "ninja-start-builds2.json");
    this.buildsFile = Path.of(dataDir, "ninja", "ninja-builds2.json");
    this.consensusFile = Path.of(dataDir, "ninja", "ninja-archetype-consensus2.json");
    this.membersFile = Path.of(dataDir, "ninja", "ninja-archetype-members2.json");
    this.data = data;
  }

  /**
   * 저장된 실빌드 코드(10-14 C319) — fetch-ninja-start2 가 아키타입마다 레벨 상위 8명을 재며 남긴 PoB 코드(메인 그룹을 아키타입 스킬로 맞춘
   * 것). 시뮬레이터 후보도 같은 기준(레벨 순 상위)이라 거의 겹친다 — 있으면 poe.ninja 실시간 요청(캐릭터당 1회, 레이트리밋 위험)을 건너뛴다. 받은 지
   * 36시간이 넘었거나 없으면 null. 스냅샷 일치를 요구하지 않는다 — 스냅샷은 하루에도 여러 번 바뀌어(10-14 수집 0421 → 동기 0629, 적중 0명) 같은
   * 사람의 몇 시간 전 코드를 버리게 된다.
   */
  static final java.time.Duration MEMBER_MAX_AGE = java.time.Duration.ofHours(36);

  public String memberCode(String ascendancy, String skill, String account, String name) {
    if (ascendancy == null || skill == null || account == null || name == null) {
      return null;
    }
    try {
      FileTime t = Files.exists(membersFile) ? Files.getLastModifiedTime(membersFile) : null;
      if (t != null && !t.equals(membersAt)) {
        synchronized (this) {
          JsonNode root = json.readTree(Files.readString(membersFile));
          java.util.Map<String, String> map = new java.util.HashMap<>();
          if (t.toInstant().isAfter(java.time.Instant.now().minus(MEMBER_MAX_AGE))) {
            for (var e : root.path("archetypes").properties()) {
              for (JsonNode m : e.getValue()) {
                if (m.hasNonNull("account") && m.hasNonNull("name") && m.hasNonNull("code")) {
                  map.put(
                      e.getKey() + "|" + m.path("account").asText() + "|" + m.path("name").asText(),
                      m.path("code").asText());
                }
              }
            }
          }
          memberCodes = java.util.Map.copyOf(map);
          membersAt = t;
          logger.info("PoE2 저장 실빌드 코드 로드: {}명", map.size());
        }
      }
    } catch (Exception e) {
      logger.warn("PoE2 저장 실빌드 코드 로드 실패", e);
    }
    FileTime at = membersAt;
    if (at == null || at.toInstant().isBefore(java.time.Instant.now().minus(MEMBER_MAX_AGE))) {
      return null; // 읽은 뒤 시간이 지나 오래된 판
    }
    return memberCodes.get(ascendancy + "|" + skill + "|" + account + "|" + name);
  }

  /** 공통 특성 파일의 setRoles → 기록(없거나 모양이 다르면 null). */
  static SetRoles setRoles(JsonNode sr) {
    if (sr == null || !sr.isObject() || !sr.path("sets").isArray()) {
      return null;
    }
    List<SetRole> sets = new ArrayList<>();
    for (JsonNode x : sr.path("sets")) {
      List<String> focus = new ArrayList<>();
      x.path("focus").forEach(f -> focus.add(f.asText()));
      sets.add(
          new SetRole(
              x.path("set").asInt(0),
              x.path("kind").isTextual() ? x.path("kind").asText() : null,
              List.copyOf(focus),
              x.path("weapon").asText("none"),
              x.path("weaponKo").asText(""),
              x.path("weaponCount").asInt(0),
              x.path("active").asInt(0)));
    }
    return new SetRoles(sr.path("n").asInt(0), sr.path("used").asInt(0), List.copyOf(sets));
  }

  /** (전직, 스킬) 공통 특성 — 파일이 없거나 그 아키타입이 없으면 null(시뮬레이터는 공통 특성 없이 돈다). 파일이 바뀌면 다시 읽는다. */
  public Consensus consensus(String ascendancy, String skill) {
    if (ascendancy == null || ascendancy.isBlank() || skill == null) {
      return null;
    }
    try {
      FileTime t = Files.exists(consensusFile) ? Files.getLastModifiedTime(consensusFile) : null;
      if (t != null && !t.equals(consensusAt)) {
        synchronized (this) {
          JsonNode root = json.readTree(Files.readString(consensusFile));
          java.util.Map<String, Consensus> map = new java.util.HashMap<>();
          for (var e : root.path("archetypes").properties()) {
            JsonNode a = e.getValue();
            JsonNode tree = a.path("tree");
            if (!tree.isObject()) {
              continue;
            }
            List<Integer> nodes = new ArrayList<>();
            tree.path("nodes").forEach(n -> nodes.add(n.asInt()));
            java.util.Map<Integer, Integer> attrs = new java.util.HashMap<>();
            tree.path("attrs")
                .properties()
                .forEach(x -> attrs.put(Integer.valueOf(x.getKey()), x.getValue().asInt()));
            java.util.Map<Integer, Integer> sets = new java.util.HashMap<>();
            tree.path("sets")
                .properties()
                .forEach(x -> sets.put(Integer.valueOf(x.getKey()), x.getValue().asInt()));
            java.util.Map<String, Integer> supports = new java.util.LinkedHashMap<>();
            a.path("supports")
                .properties()
                .forEach(x -> supports.put(x.getKey(), x.getValue().asInt()));
            java.util.Map<String, java.util.Map<String, Integer>> slotUniques =
                new java.util.LinkedHashMap<>();
            for (var sl : a.path("slots").properties()) {
              java.util.Map<String, Integer> u = new java.util.LinkedHashMap<>();
              sl.getValue()
                  .path("unique")
                  .properties()
                  .forEach(x -> u.put(x.getKey(), x.getValue().asInt()));
              if (!u.isEmpty()) {
                slotUniques.put(sl.getKey(), u);
              }
            }
            java.util.Map<Integer, Integer> nodeCounts = new java.util.HashMap<>();
            a.path("nodes")
                .properties()
                .forEach(x -> nodeCounts.put(Integer.valueOf(x.getKey()), x.getValue().asInt()));
            map.put(
                e.getKey(),
                new Consensus(
                    a.path("n").asInt(0),
                    tree.path("className").asText(""),
                    tree.path("ascendancy").asText(""),
                    List.copyOf(nodes),
                    java.util.Map.copyOf(attrs),
                    java.util.Map.copyOf(sets),
                    tree.path("overlap").asDouble(0),
                    supports,
                    slotUniques,
                    java.util.Map.copyOf(nodeCounts),
                    setRoles(a.path("setRoles"))));
          }
          consensus = java.util.Map.copyOf(map);
          consensusAt = t;
          logger.info("PoE2 아키타입 공통 특성 로드: {}개", map.size());
        }
      }
    } catch (Exception e) {
      logger.warn("PoE2 아키타입 공통 특성 로드 실패", e);
    }
    return consensus.get(ascendancy + "|" + skill);
  }

  /** 실빌드 캐릭터 전체(ninja-builds2.json) — 파일이 바뀌면(스냅샷 동기가 새로 받으면) 다시 읽는다. */
  public List<NinjaBuild> builds() {
    try {
      FileTime t = Files.exists(buildsFile) ? Files.getLastModifiedTime(buildsFile) : null;
      if (t != null && !t.equals(buildsAt)) {
        synchronized (this) {
          JsonNode root = json.readTree(Files.readString(buildsFile));
          List<NinjaBuild> list = new ArrayList<>();
          for (JsonNode b : root.path("builds")) {
            list.add(
                new NinjaBuild(
                    b.path("name").asText(null),
                    b.path("account").asText(null),
                    b.path("ascendancy").asText(null),
                    b.path("level").isNumber() ? b.path("level").asInt() : null,
                    num(b, "life"),
                    num(b, "energyShield"),
                    num(b, "ehp"),
                    num(b, "dps"),
                    b.path("mainSkill").asText(null)));
          }
          builds = List.copyOf(list);
          buildsLeague = root.path("league").asText(null);
          buildsSnapshot = root.path("snapshot").asText(null);
          buildsFetchedAt = root.path("fetchedAt").asText(null);
          java.util.Map<String, Integer> pop = new java.util.HashMap<>();
          for (var e : root.path("classPopulation").properties()) {
            pop.put(e.getKey(), e.getValue().asInt(0));
          }
          classPopulation = java.util.Map.copyOf(pop);
          buildsAt = t;
          logger.info(
              "PoE2 poe.ninja 캐릭터 로드: {}명 ({} {})", builds.size(), buildsLeague, buildsSnapshot);
        }
      }
    } catch (Exception e) {
      logger.warn("PoE2 poe.ninja 캐릭터 로드 실패", e);
    }
    return builds;
  }

  public String league() {
    builds();
    return buildsLeague;
  }

  public String snapshot() {
    builds();
    return buildsSnapshot;
  }

  /**
   * 폼 선택지 — PoE1 시뮬 폼과 같은 방식: 목록은 게임 데이터의 액티브 스킬(스킬·메타 젬)·전직 전부, 순서는 스냅샷 캐릭터의 메인 스킬 분포(고른 전직 안에서 /
   * 고른 스킬을 메인으로 쓰는 전직). 게임 데이터에 없는 메인 스킬(동료·아이템 부여 스킬 등)은 실빌드 메인으로 쓰일 때만 덧붙인다. 인원은 내보내지 않는다.
   *
   * <p>표본은 전직마다 top-100 이라 그대로 세면 전직 인기가 사라진다(스킬을 안 고르면 전직이 전부 동률). 캐릭터 한 명을 "그 전직 전체 인구 / 그 전직 표본
   * 수"로 가중해 실제 인원을 추정한다 — 스킬을 안 고르면 전직 순서 = 전체 인구 순.
   */
  public Options options(String ascendancy, String skill) {
    List<NinjaBuild> all = builds();
    boolean hasAsc = ascendancy != null && !ascendancy.isBlank();
    boolean hasSkill = skill != null && !skill.isBlank();
    java.util.Map<String, Integer> sampleByAsc = new java.util.HashMap<>();
    for (NinjaBuild b : all) {
      if (b.mainSkill() != null && b.ascendancy() != null) {
        sampleByAsc.merge(b.ascendancy(), 1, Integer::sum);
      }
    }
    java.util.Map<String, Integer> pop = classPopulation;
    java.util.Map<String, Double> skillCount = new java.util.HashMap<>();
    java.util.Map<String, Double> ascCount = new java.util.HashMap<>();
    for (NinjaBuild b : all) {
      if (b.mainSkill() == null || b.ascendancy() == null) {
        continue;
      }
      Integer p = pop.get(b.ascendancy());
      double w = p == null ? 1d : (double) p / sampleByAsc.get(b.ascendancy());
      if (!hasAsc || ascendancy.equals(b.ascendancy())) {
        skillCount.merge(b.mainSkill(), w, Double::sum);
      }
      if (!hasSkill || skill.equals(b.mainSkill())) {
        ascCount.merge(b.ascendancy(), w, Double::sum);
      }
    }
    java.util.Map<String, Choice> skills = new java.util.LinkedHashMap<>();
    for (String kind : List.of("skill", "meta")) {
      for (Poe2.Gem g : data.searchGems(null, kind, null, null)) {
        skills.putIfAbsent(g.name(), new Choice(g.name(), g.nameKo()));
      }
    }
    for (NinjaBuild b : all) {
      if (b.mainSkill() != null) {
        skills.putIfAbsent(
            b.mainSkill(),
            new Choice(
                b.mainSkill(), data.gemByName(b.mainSkill()).map(Poe2.Gem::nameKo).orElse(null)));
      }
    }
    List<Choice> skillList = new ArrayList<>(skills.values());
    skillList.sort(
        Comparator.comparingDouble((Choice c) -> -skillCount.getOrDefault(c.name(), 0d))
            .thenComparing(Choice::name));
    Poe2DataService.TreeIndex tree = data.treeIndex();
    java.util.Map<String, Choice> ascs = new java.util.LinkedHashMap<>();
    tree.ascendancyKo().forEach((id, ko) -> ascs.put(id, new Choice(id, ko)));
    for (String name : ascCount.keySet()) {
      if (!tree.classKo().containsKey(name)) {
        ascs.putIfAbsent(
            name, new Choice(name, null)); // ninja class 차원의 기본 직업(Ranger 등)은 전직 선택지가 아니다
      }
    }
    List<Choice> ascList = new ArrayList<>(ascs.values());
    ascList.sort(
        Comparator.comparingDouble((Choice c) -> -ascCount.getOrDefault(c.name(), 0d))
            .thenComparing(Choice::name));
    // PoE1 시뮬 폼과 같은 전직 셀렉트: 많이 쓰는 상위 5 + 직업별 묶음(트리 순서). 트리에 없는 전직은 "기타" 묶음.
    List<String> popular =
        ascList.stream()
            .map(Choice::name)
            .filter(n -> ascCount.getOrDefault(n, 0d) > 0)
            .limit(5)
            .toList();
    List<ClassGroup> classes = new ArrayList<>();
    java.util.Set<String> grouped = new java.util.HashSet<>();
    tree.classAscendancies()
        .forEach(
            (cls, ids) -> {
              List<Choice> members =
                  ids.stream().map(id -> ascs.getOrDefault(id, new Choice(id, null))).toList();
              grouped.addAll(ids);
              classes.add(new ClassGroup(cls, tree.classKo().get(cls), members));
            });
    List<Choice> others = ascList.stream().filter(c -> !grouped.contains(c.name())).toList();
    if (!others.isEmpty()) {
      classes.add(new ClassGroup("Other", "기타", others));
    }
    return new Options(
        buildsLeague,
        buildsSnapshot,
        buildsFetchedAt,
        List.copyOf(skillList),
        List.copyOf(ascList),
        popular,
        List.copyOf(classes));
  }

  /** 캐릭터 상세의 PoB 코드 — 스냅샷 버전으로 요청한다(후보를 고른 그 스냅샷의 캐릭터 상태). */
  public String characterCode(NinjaBuild b) {
    String url =
        "https://poe.ninja/poe2/api/builds/"
            + snapshot()
            + "/character?account="
            + URLEncoder.encode(b.account(), StandardCharsets.UTF_8)
            + "&name="
            + URLEncoder.encode(b.name(), StandardCharsets.UTF_8)
            + "&overview="
            + league()
            + "&timeMachine=";
    try {
      HttpResponse<String> res =
          http.send(
              HttpRequest.newBuilder(URI.create(url))
                  .header("User-Agent", "Mozilla/5.0 bluesky-poe2-sim")
                  .timeout(Duration.ofSeconds(20))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (res.statusCode() == 429) {
        throw new IllegalStateException("poe.ninja 요청 한도 초과(잠시 뒤 다시)");
      }
      if (res.statusCode() != 200) {
        throw new IllegalStateException("poe.ninja 응답 " + res.statusCode());
      }
      String code = json.readTree(res.body()).path("pathOfBuildingExport").asText("");
      if (code.isBlank()) {
        throw new IllegalStateException("PoB 코드 없음");
      }
      return code;
    } catch (java.io.IOException e) {
      throw new IllegalStateException("poe.ninja 요청 실패: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("중단됨", e);
    }
  }

  /** (전직, 스킬) 아키타입 — 전직이 비었거나 표본이 적어 없으면 스킬 단위 집계. 둘 다 없으면 null. */
  public Archetype findOrSkill(String ascendancy, String skill) {
    if (ascendancy != null && !ascendancy.isBlank()) {
      Archetype a = find(ascendancy, skill);
      if (a != null) {
        return a;
      }
    }
    overview();
    return skillAgg.get(skill);
  }

  /** 아키타입 전체(표본 수 내림차순). 데이터 없으면 빈 목록. */
  public Overview overview() {
    try {
      FileTime a = Files.exists(archFile) ? Files.getLastModifiedTime(archFile) : null;
      FileTime s = Files.exists(startFile) ? Files.getLastModifiedTime(startFile) : null;
      if (a != null && (!a.equals(archAt) || (s != null && !s.equals(startAt)))) {
        synchronized (this) {
          cached = load();
          archAt = a;
          startAt = s;
          logger.info("PoE2 poe.ninja 아키타입 로드: {}개", cached.archetypes().size());
        }
      }
    } catch (Exception e) {
      logger.warn("PoE2 poe.ninja 데이터 로드 실패", e);
    }
    return cached;
  }

  /** (전직, 스킬) 한 건 — 없으면 null. */
  public Archetype find(String ascendancy, String skill) {
    for (Archetype a : overview().archetypes()) {
      if (a.ascendancy().equals(ascendancy) && a.mainSkill().equals(skill)) {
        return a;
      }
    }
    return null;
  }

  private Overview load() throws java.io.IOException {
    JsonNode root = json.readTree(Files.readString(archFile));
    JsonNode starts = Files.exists(startFile) ? json.readTree(Files.readString(startFile)) : null;
    List<Archetype> list = new ArrayList<>();
    for (var e : root.path("archetypes").properties()) {
      JsonNode a = e.getValue();
      JsonNode st = starts == null ? null : starts.get(e.getKey());
      JsonNode groups = a.path("facets").path("groups");
      list.add(
          new Archetype(
              a.path("ascendancy").asText(""),
              a.path("mainSkill").asText(""),
              a.path("sample").asInt(0),
              num(a, "medianLevel"),
              num(a, "medianLife"),
              num(a, "medianES"),
              num(a, "medianEHP"),
              num(a, "medianDPS"),
              a.path("lean").asText("balanced"),
              counts(a.path("topKeystones"), 6),
              counts(a.path("topCoSkills"), 8),
              a.path("facets").path("total").isNumber()
                  ? a.path("facets").path("total").asInt()
                  : null,
              counts(groups.path("items"), 10),
              counts(groups.path("anointed"), 5),
              st == null || !st.path("code").isString()
                  ? null
                  : new RealStart(
                      st.path("level").isNumber() ? st.path("level").asInt() : null,
                      num(st, "dps"),
                      num(st, "ehp"),
                      num(st, "life"),
                      num(st, "es"),
                      st.path("n").asInt(0),
                      num(st, "medianDps"),
                      num(st, "medianEhp"),
                      st.path("code").asText())));
    }
    list.sort(Comparator.comparingInt(Archetype::sample).reversed());
    java.util.Map<String, Archetype> bySkill = new java.util.HashMap<>();
    for (var e : root.path("skillArchetypes").properties()) {
      JsonNode a = e.getValue();
      bySkill.put(
          e.getKey(),
          new Archetype(
              "",
              e.getKey(),
              a.path("sample").asInt(0),
              num(a, "medianLevel"),
              num(a, "medianLife"),
              num(a, "medianES"),
              num(a, "medianEHP"),
              num(a, "medianDPS"),
              a.path("lean").asText("balanced"),
              counts(a.path("topKeystones"), 6),
              counts(a.path("topCoSkills"), 8),
              null,
              List.of(),
              List.of(),
              null));
    }
    skillAgg = java.util.Map.copyOf(bySkill);
    return new Overview(
        root.path("league").asText(null),
        num(root, "globalMedianDps"),
        num(root, "globalMedianEhp"),
        List.copyOf(list));
  }

  private static List<Count> counts(JsonNode arr, int limit) {
    List<Count> out = new ArrayList<>();
    for (JsonNode c : arr) {
      if (out.size() >= limit) {
        break;
      }
      out.add(new Count(c.path("name").asText(""), c.path("count").asInt(0)));
    }
    return out;
  }

  private static Double num(JsonNode v, String key) {
    return v.path(key).isNumber() ? v.path(key).asDouble() : null;
  }
}
