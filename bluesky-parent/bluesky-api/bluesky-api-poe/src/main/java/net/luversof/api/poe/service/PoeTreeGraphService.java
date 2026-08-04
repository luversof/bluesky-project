package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 패시브 트리 그래프 — passive-tree.json(뷰어와 동일 파일)을 그래프로 로드해 최적화 탐색(BFS 경로 할당)에 쓴다.
 *
 * <p>노드 id 는 GGG 트리 익스포트/PoB Spec nodes 와 동일하다.
 */
@Service
public class PoeTreeGraphService {

  private static final Logger logger = LoggerFactory.getLogger(PoeTreeGraphService.class);

  /** GGG 트리의 클래스 시작 노드 이름 → PoB className */
  private static final Map<String, String> CLASS_START_NAMES =
      Map.of(
          "Seven", "Scion",
          "MARAUDER", "Marauder",
          "RANGER", "Ranger",
          "WITCH", "Witch",
          "DUELIST", "Duelist",
          "TEMPLAR", "Templar",
          "SIX", "Shadow");

  public record TreeNode(
      int id,
      String name,
      String nameKo,
      String type,
      List<String> stats,
      List<String> statsKo,
      String ascendancy,
      // Jackson 3 는 primitive 에 null(필드 부재) 매핑을 거부하므로 래퍼 타입이어야 한다
      Boolean ascendancyStart,
      // 마스터리 노드만 보유 — 효과 하나를 골라야 스탯이 붙는다(PoB Spec masteryEffects)
      List<MasteryEffect> masteryEffects,
      // 클러스터 주얼 소켓만 보유(0=소형 1=중형 2=대형). 일반 유니크 주얼은 이 소켓에 **못 넣는다**.
      Integer clusterSize,
      // 트리 좌표 — 반경 주얼이 어떤 패시브를 덮는지 계산할 때 쓴다(문신 자리 선정 등)
      Double x,
      Double y,
      // 아뮬렛 도유로 이 노터블을 부여할 때 드는 성유 3종(slug). 있으면 = 도유 가능 노터블.
      List<String> anoint,
      // 클러스터 서브그래프 참조(GGG 원본 expansionJewel) — 자동 클러스터 채택의 가상 노드 id 산출에
      // 필요(id = 0x10000 + size/index 비트 + sizeIndex<<4 + templateIndex, 프론트 buildClusterSubgraph
      // 파리티).
      ExpansionJewel expansionJewel) {}

  /** 클러스터 소켓의 확장 참조 — size(0소/1중/2대), index(부모 내 자리), proxy(생성 노드 부착 기준), parent(중첩 상위 소켓). */
  public record ExpansionJewel(int size, int index, Integer proxy, Integer parent) {}

  /** 마스터리가 제공하는 효과 하나. id 는 PoB/GGG 인코딩에 쓰이는 effect id. */
  public record MasteryEffect(int id, List<String> stats, List<String> statsKo) {}

  /** 직업의 전직 목록 — 배열 순서가 PoB Spec 의 ascendClassId(1부터)와 일치 */
  public record ClassInfo(String name, List<String> ascendancies) {}

  /** 성유(오일) 하나 — 도유 레시피 표시용. icon 은 /poe-assets/ 상대 경로. */
  public record Oil(String name, String nameKo, String icon) {}

  /** 도유 목록 한 줄 — poedb 속성부여식 표(노터블 + 성유 3개). */
  public record AnointEntry(
      int nodeId,
      String name,
      String nameKo,
      List<String> stats,
      List<String> statsKo,
      List<OilRef> oils) {}

  public record OilRef(String slug, String name, String nameKo, String icon) {}

  /** 클러스터 주얼 노터블 정의 — 트리 노드가 아니라 생성 노드용 사전(passive-tree.json clusterNotables). */
  public record ClusterNotable(
      String name, String nameKo, List<String> stats, List<String> statsKo, String icon) {}

  private record PoeTreeFile(
      List<ClassInfo> classes,
      List<TreeNode> nodes,
      List<List<Integer>> edges,
      java.util.Map<String, Oil> oils,
      List<ClusterNotable> clusterNotables) {}

  private final Path dataFile;
  private volatile Map<Integer, TreeNode> nodeById = Map.of();
  private volatile Map<Integer, List<Integer>> adjacency = Map.of();
  private volatile Map<String, Integer> classStartByName = Map.of();
  private volatile Map<String, List<String>> ascendanciesByClass = Map.of();
  private volatile Map<String, Integer> ascendancyStartByName = Map.of();
  private volatile Map<String, Oil> oils = Map.of();
  private volatile List<ClusterNotable> clusterNotables = List.of();

  public PoeTreeGraphService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "passive-tree.json");
    reload();
  }

  /** 트리 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    if (!Files.exists(dataFile)) {
      logger.warn("PoE 패시브 트리 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
      return;
    }
    try (InputStream inputStream = Files.newInputStream(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      PoeTreeFile tree = jsonMapper.readValue(inputStream, PoeTreeFile.class);

      this.oils = tree.oils() != null ? tree.oils() : Map.of();
      this.clusterNotables =
          tree.clusterNotables() != null
              ? tree.clusterNotables().stream()
                  .sorted(java.util.Comparator.comparing(ClusterNotable::name))
                  .toList()
              : List.of();
      Map<Integer, TreeNode> nodes = new HashMap<>();
      for (TreeNode node : tree.nodes()) {
        nodes.put(node.id(), node);
      }
      // 주얼 소켓 후보 구성이 바뀌면(=클러스터 소켓 제외) 로그로 확인할 수 있게 남긴다
      long clusterSockets =
          tree.nodes().stream()
              .filter(
                  n ->
                      n.ascendancy() == null && "jewel".equals(n.type()) && n.clusterSize() != null)
              .count();
      long plainSockets =
          tree.nodes().stream()
              .filter(
                  n ->
                      n.ascendancy() == null && "jewel".equals(n.type()) && n.clusterSize() == null)
              .count();
      logger.info("주얼 소켓 {}개 사용(클러스터 전용 소켓 {}개 제외)", plainSockets, clusterSockets);

      Map<Integer, List<Integer>> edges = new HashMap<>();
      for (List<Integer> edge : tree.edges()) {
        edges.computeIfAbsent(edge.get(0), k -> new ArrayList<>()).add(edge.get(1));
        edges.computeIfAbsent(edge.get(1), k -> new ArrayList<>()).add(edge.get(0));
      }
      Map<String, Integer> starts = new HashMap<>();
      Map<String, Integer> ascendancyStarts = new HashMap<>();
      for (TreeNode node : tree.nodes()) {
        if ("class".equals(node.type()) && CLASS_START_NAMES.containsKey(node.name())) {
          starts.put(CLASS_START_NAMES.get(node.name()), node.id());
        }
        if (Boolean.TRUE.equals(node.ascendancyStart()) && node.ascendancy() != null) {
          ascendancyStarts.put(node.ascendancy(), node.id());
        }
      }
      Map<String, List<String>> classAscendancies = new HashMap<>();
      for (ClassInfo classInfo : tree.classes() != null ? tree.classes() : List.<ClassInfo>of()) {
        classAscendancies.put(classInfo.name(), List.copyOf(classInfo.ascendancies()));
      }
      this.nodeById = Map.copyOf(nodes);
      this.adjacency = Map.copyOf(edges);
      this.classStartByName = Map.copyOf(starts);
      this.ascendanciesByClass = Map.copyOf(classAscendancies);
      this.ascendancyStartByName = Map.copyOf(ascendancyStarts);
      logger.info(
          "PoE 패시브 트리 그래프 로드: 노드 {}, 시작점 {}, 전직 {}",
          nodes.size(),
          starts.keySet(),
          ascendancyStarts.size());
    } catch (Exception e) {
      logger.warn("PoE 패시브 트리 그래프 로드 실패: {}", dataFile, e);
    }
  }

  public boolean hasData() {
    return !nodeById.isEmpty();
  }

  public TreeNode node(int id) {
    return nodeById.get(id);
  }

  /**
   * {@code start} 에서 {@code allowed} 안의 간선만 따라 도달 가능한 노드 집합. 고정 트리가 실제로 그 직업 시작점과 이어져 있는지 검사하는 용도 —
   * PoB 는 연결되지 않은 노드를 조용히 버린다.
   */
  public java.util.Set<Integer> reachableFrom(int start, java.util.Set<Integer> allowed) {
    java.util.Set<Integer> seen = new java.util.LinkedHashSet<>();
    if (!allowed.contains(start)) {
      return seen;
    }
    java.util.Deque<Integer> queue = new java.util.ArrayDeque<>();
    queue.add(start);
    seen.add(start);
    while (!queue.isEmpty()) {
      for (int next : adjacency.getOrDefault(queue.poll(), List.of())) {
        if (allowed.contains(next) && seen.add(next)) {
          queue.add(next);
        }
      }
    }
    return seen;
  }

  /** PoB className → 트리 시작 노드 id (없으면 null) */
  public Integer classStart(String className) {
    return classStartByName.get(className);
  }

  /** 클러스터 주얼 전용 소켓(트리 외곽, expansionJewel 보유) — 자동 클러스터 채택 후보. id 정렬로 결정적. */
  public List<TreeNode> clusterSockets() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null)
        .filter(node -> "jewel".equals(node.type()) && node.expansionJewel() != null)
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /** 비전직 노터블/키스톤 후보 (최적화 탐색 대상). id 정렬로 실행 간 결정적 순서 보장(동점 타이브레이크 안정화) */
  public List<TreeNode> searchCandidates() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null)
        .filter(node -> "notable".equals(node.type()) || "keystone".equals(node.type()))
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /**
   * 아뮬렛 도유로 부여할 수 있는 노터블 — 트리에 연결된 것(470개)과 도유로만 얻는 고립 노터블(30개)이 모두 포함된다. id 정렬로 실행 간 결정적 순서 보장.
   */
  public List<TreeNode> anointableNotables() {
    return nodeById.values().stream()
        .filter(node -> node.anoint() != null && !node.anoint().isEmpty())
        .filter(node -> node.ascendancy() == null)
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /** 성유 slug 의 등급 — mushruneuber 최상(99), 그 외 숫자부. 도유 목록 정렬(비싼 성유 우선)에 쓴다. */
  private static int oilRank(String slug) {
    if (slug == null) {
      return -1;
    }
    if (slug.endsWith("uber")) {
      return 99;
    }
    String digits = slug.replaceAll("\\D+", "");
    return digits.isEmpty() ? -1 : Integer.parseInt(digits);
  }

  /** poedb 속성부여식 도유 목록 — 최고 성유 등급 내림차순(동급은 id). */
  /** 클러스터 주얼 노터블 사전(이름 오름차순) — 브라우징 페이지용. */
  public List<ClusterNotable> clusterNotables() {
    return clusterNotables;
  }

  public List<AnointEntry> anointList() {
    Map<String, Oil> oilMap = oils;
    List<AnointEntry> out = new ArrayList<>();
    for (TreeNode node : anointableNotables()) {
      List<OilRef> refs = new ArrayList<>();
      for (String slug : node.anoint()) {
        Oil oil = oilMap.get(slug);
        refs.add(
            new OilRef(
                slug,
                oil != null ? oil.name() : slug,
                oil != null ? oil.nameKo() : slug,
                oil != null ? oil.icon() : null));
      }
      out.add(
          new AnointEntry(
              node.id(), node.name(), node.nameKo(), node.stats(), node.statsKo(), refs));
    }
    out.sort(
        java.util.Comparator.<AnointEntry>comparingInt(
                e -> e.oils().stream().mapToInt(o -> oilRank(o.slug())).max().orElse(-1))
            .reversed()
            .thenComparingInt(AnointEntry::nodeId));
    return out;
  }

  /**
   * 반경 주얼이 덮는 패시브 id — 소켓/마스터리/클래스 시작/전직 노드는 반경 효과 대상이 아니라 제외(공식 뷰어와 동일).
   *
   * @param radius 월드 단위 반경(3.16+: 소형 960 / 중형 1440 / 대형 1800 / 초대형 2400 / 거대 2880)
   */
  public List<Integer> nodesWithinRadius(int centerId, double radius) {
    TreeNode center = nodeById.get(centerId);
    if (center == null || center.x() == null || center.y() == null) {
      return List.of();
    }
    double squared = radius * radius;
    List<Integer> found = new ArrayList<>();
    for (TreeNode node : nodeById.values()) {
      if (node.id() == centerId
          || node.x() == null
          || node.ascendancy() != null
          || "jewel".equals(node.type())
          || "mastery".equals(node.type())
          || "class".equals(node.type())) {
        continue;
      }
      double dx = node.x() - center.x();
      double dy = node.y() - center.y();
      if (dx * dx + dy * dy <= squared) {
        found.add(node.id());
      }
    }
    // ⚠ nodeById 는 Map.copyOf — 자바의 불변 맵은 **JVM 실행마다 순회 순서가 무작위화**된다(ImmutableCollections SALT).
    // 정렬하지 않으면 반경 내 후보 순서가 실행마다 달라져 그리디 채택 순서·결과가 흔들린다.
    found.sort(null);
    return found;
  }

  /** 이 노드와 선으로 이어진 노드들. 문신의 "인접 할당 수" 규칙 판정처럼 이웃이 필요할 때 쓴다. */
  public List<Integer> neighbors(int id) {
    return adjacency.getOrDefault(id, List.of());
  }

  /**
   * 일반 유니크 주얼을 꽂을 수 있는 비전직 주얼 소켓 id (id 정렬로 결정적).
   *
   * <p><b>클러스터 주얼 소켓은 제외</b>한다 — 게임에서 그 소켓엔 클러스터 주얼만 들어가는데, PoB 는 검증하지 않아 일반 주얼을 꽂으면 스탯이 그대로 반영된다(=
   * 게임에서 만들 수 없는 빌드가 더 높은 점수를 받는다). 전체 57개 중 42개가 클러스터 소켓이라 그냥 두면 후보의 대부분이 가짜다.
   */
  public List<Integer> jewelSockets() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null && "jewel".equals(node.type()))
        .filter(node -> node.clusterSize() == null)
        .map(TreeNode::id)
        .sorted()
        .toList();
  }

  /**
   * 비전직 마스터리 노드 (효과를 가진 것만, id 정렬로 결정적).
   *
   * <p>마스터리는 같은 무리의 패시브에만 연결돼 있어, 그 무리를 이미 찍었다면 1포인트로 추가할 수 있다. 효과를 골라야 스탯이 붙으므로 최적화기는 효과까지 함께 평가해야
   * 한다.
   */
  public List<TreeNode> masteryNodes() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null && "mastery".equals(node.type()))
        .filter(node -> node.masteryEffects() != null && !node.masteryEffects().isEmpty())
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /** 직업의 전직 목록 (순서 = PoB ascendClassId - 1) */
  public List<String> ascendancies(String className) {
    return ascendanciesByClass.getOrDefault(className, List.of());
  }

  /** 전직 이름 → 소속 직업 (역매핑). 없으면 null. 전직만 선택해도 직업을 도출하기 위함. */
  public String classForAscendancy(String ascendancy) {
    if (ascendancy == null || ascendancy.isBlank()) {
      return null;
    }
    for (Map.Entry<String, List<String>> entry : ascendanciesByClass.entrySet()) {
      if (entry.getValue().contains(ascendancy)) {
        return entry.getKey();
      }
    }
    return null;
  }

  /** PoB Spec 의 ascendClassId (1부터). 목록에 없으면 0 */
  public int ascendClassId(String className, String ascendancy) {
    return ascendancies(className).indexOf(ascendancy) + 1;
  }

  /** 전직 시작 노드 id (없으면 null) */
  public Integer ascendancyStart(String ascendancy) {
    return ascendancyStartByName.get(ascendancy);
  }

  /**
   * 해당 전직의 탐색 후보 노드 (id 정렬로 결정적 순서).
   *
   * <p>일반 전직은 notable 노드가 선택지다. 그러나 사이온 <b>렐리쿼리언</b>처럼 렐릭(유니크) 효과를 스탯으로 가진 노드가 대부분 {@code
   * type:"normal"} 인 렐릭형 전직은 notable 만 잡으면 핵심 선택지를 통째로 놓친다. 스탯을 가진 normal 노드가 notable 보다 많으면(렐릭형)
   * normal 노드도 후보에 포함한다. 이 판정은 전직별 노드 구성에서 자동 도출되므로 기존 6개 일반 전직은 동작이 바뀌지 않는다.
   */
  /**
   * 해당 전직의 <b>스탯을 가진 모든 노드</b>(작은 노드 포함, 마스터리·시작 노드 제외).
   *
   * <p>노터블만 후보로 두면 남은 포인트로 닿는 노터블이 없을 때 전직 포인트가 그냥 남는다(실측 6/8). 잔여 포인트를 작은 노드로 마저 쓰기 위한 목록.
   */
  public List<TreeNode> ascendancyAllNodes(String ascendancy) {
    return nodeById.values().stream()
        .filter(node -> ascendancy.equals(node.ascendancy()))
        .filter(
            node -> !"mastery".equals(node.type()) && !Boolean.TRUE.equals(node.ascendancyStart()))
        .filter(node -> node.stats() != null && !node.stats().isEmpty())
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  public List<TreeNode> ascendancyCandidates(String ascendancy) {
    List<TreeNode> all =
        nodeById.values().stream().filter(node -> ascendancy.equals(node.ascendancy())).toList();
    long notables = all.stream().filter(node -> "notable".equals(node.type())).count();
    long relicNormals = all.stream().filter(this::isRelicNormal).count();
    // 렐리쿼리언만 격리: notable 이 극히 적고(≤5) 렐릭 normal 이 이를 압도(>3배). 일반 전직(notable 7~12)·
    // 어센던트(12)·모든 혈맹(렐릭 normal ≤6)은 이 조건에 안 걸려 기존 동작(notable 후보)이 유지된다.
    boolean relicStyle = notables <= 5 && relicNormals > notables * 3L;
    return all.stream()
        .filter(node -> "notable".equals(node.type()) || (relicStyle && isRelicNormal(node)))
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /** 렐릭형 선택 노드 = 스탯을 가진 normal 노드 (패시브포인트 부여·무기전시 등 노이즈 제외) */
  private boolean isRelicNormal(TreeNode node) {
    if (!"normal".equals(node.type()) || node.stats() == null || node.stats().isEmpty()) {
      return false;
    }
    String name = node.name() == null ? "" : node.name();
    return !"Passive Point".equals(name) && !name.contains("Display");
  }

  /** 전직 서브그래프 안에서의 최단 경로 (해당 전직 노드만 통과) */
  public List<Integer> shortestPathInAscendancy(
      Set<Integer> allocated, int targetId, String ascendancy) {
    return shortestPath(allocated, targetId, ascendancy);
  }

  /**
   * 혈맹(Bloodline / PoB 2차 전직) 레지스트리 — 전직 그룹 id → PoB {@code secondaryAscendClassId}
   * (alternate_ascendancies 순서). 일반 클래스 전직과 구분해 최적화기에서 별도 배분한다. Wildwood(Warden/Warlock/Primalist)는
   * 혈맹이 아니라 제외.
   */
  private static final Map<String, Integer> BLOODLINE_SECONDARY_ID =
      Map.ofEntries(
          Map.entry("Trialmaster", 4),
          Map.entry("Oshabi", 5),
          Map.entry("KingInTheMists", 6),
          Map.entry("Catarina", 7),
          Map.entry("Aul", 8),
          Map.entry("Lycia", 9),
          Map.entry("Olroth", 10),
          Map.entry("Farrul", 11),
          Map.entry("Delirious", 12),
          Map.entry("Breachlord", 13),
          Map.entry("Necromantic", 14));

  /** 데이터에 존재하는 혈맹 그룹 id 목록 (시작 노드가 있는 것만) */
  public List<String> bloodlines() {
    return BLOODLINE_SECONDARY_ID.keySet().stream()
        .filter(id -> ascendancyStartByName.containsKey(id))
        .sorted()
        .toList();
  }

  /** 혈맹 그룹의 PoB secondaryAscendClassId (없으면 0) */
  public int secondaryAscendClassId(String bloodline) {
    return bloodline == null ? 0 : BLOODLINE_SECONDARY_ID.getOrDefault(bloodline, 0);
  }

  /**
   * 할당 집합에서 목표 노드까지의 최단 경로(BFS, 미할당 구간만 반환. 목표 포함, 시작 집합 제외).
   *
   * <p>마스터리/전직/다른 클래스 시작 노드는 지나갈 수 없다.
   *
   * @return 목표까지 갈 수 없으면 null
   */
  public List<Integer> shortestPath(Set<Integer> allocated, int targetId) {
    return shortestPath(allocated, targetId, null);
  }

  private List<Integer> shortestPath(Set<Integer> allocated, int targetId, String ascendancyScope) {
    if (allocated.contains(targetId)) {
      return List.of();
    }
    Map<Integer, Integer> previous = new HashMap<>();
    Deque<Integer> queue = new ArrayDeque<>();
    Set<Integer> visited = new HashSet<>(allocated);
    for (int start : allocated) {
      queue.addLast(start);
    }
    while (!queue.isEmpty()) {
      int current = queue.removeFirst();
      for (int next : adjacency.getOrDefault(current, List.of())) {
        if (visited.contains(next) || !isTraversable(next, targetId, ascendancyScope)) {
          continue;
        }
        visited.add(next);
        previous.put(next, current);
        if (next == targetId) {
          List<Integer> path = new ArrayList<>();
          for (int step = targetId; previous.containsKey(step); step = previous.get(step)) {
            path.add(step);
          }
          java.util.Collections.reverse(path);
          return path;
        }
        queue.addLast(next);
      }
    }
    return null;
  }

  private boolean isTraversable(int nodeId, int targetId, String ascendancyScope) {
    TreeNode node = nodeById.get(nodeId);
    if (node == null) {
      return false;
    }
    if (nodeId == targetId) {
      return true;
    }
    if (ascendancyScope != null) {
      // 전직 서브그래프: 해당 전직 노드만 통과 (마스터리 제외)
      return ascendancyScope.equals(node.ascendancy()) && !"mastery".equals(node.type());
    }
    return node.ascendancy() == null
        && !"mastery".equals(node.type())
        && !"class".equals(node.type());
  }
}
