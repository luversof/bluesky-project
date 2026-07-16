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
      Boolean ascendancyStart) {}

  /** 직업의 전직 목록 — 배열 순서가 PoB Spec 의 ascendClassId(1부터)와 일치 */
  public record ClassInfo(String name, List<String> ascendancies) {}

  private record PoeTreeFile(
      List<ClassInfo> classes, List<TreeNode> nodes, List<List<Integer>> edges) {}

  private final Path dataFile;
  private volatile Map<Integer, TreeNode> nodeById = Map.of();
  private volatile Map<Integer, List<Integer>> adjacency = Map.of();
  private volatile Map<String, Integer> classStartByName = Map.of();
  private volatile Map<String, List<String>> ascendanciesByClass = Map.of();
  private volatile Map<String, Integer> ascendancyStartByName = Map.of();

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

      Map<Integer, TreeNode> nodes = new HashMap<>();
      for (TreeNode node : tree.nodes()) {
        nodes.put(node.id(), node);
      }
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

  /** PoB className → 트리 시작 노드 id (없으면 null) */
  public Integer classStart(String className) {
    return classStartByName.get(className);
  }

  /** 비전직 노터블/키스톤 후보 (최적화 탐색 대상). id 정렬로 실행 간 결정적 순서 보장(동점 타이브레이크 안정화) */
  public List<TreeNode> searchCandidates() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null)
        .filter(node -> "notable".equals(node.type()) || "keystone".equals(node.type()))
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
  }

  /** 비전직 주얼 소켓 노드 id (id 정렬로 결정적) */
  public List<Integer> jewelSockets() {
    return nodeById.values().stream()
        .filter(node -> node.ascendancy() == null && "jewel".equals(node.type()))
        .map(TreeNode::id)
        .sorted()
        .toList();
  }

  /** 직업의 전직 목록 (순서 = PoB ascendClassId - 1) */
  public List<String> ascendancies(String className) {
    return ascendanciesByClass.getOrDefault(className, List.of());
  }

  /** PoB Spec 의 ascendClassId (1부터). 목록에 없으면 0 */
  public int ascendClassId(String className, String ascendancy) {
    return ascendancies(className).indexOf(ascendancy) + 1;
  }

  /** 전직 시작 노드 id (없으면 null) */
  public Integer ascendancyStart(String ascendancy) {
    return ascendancyStartByName.get(ascendancy);
  }

  /** 해당 전직의 노터블 후보 (id 정렬로 결정적 순서) */
  public List<TreeNode> ascendancyCandidates(String ascendancy) {
    return nodeById.values().stream()
        .filter(node -> ascendancy.equals(node.ascendancy()))
        .filter(node -> "notable".equals(node.type()))
        .sorted(java.util.Comparator.comparingInt(TreeNode::id))
        .toList();
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
