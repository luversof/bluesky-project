package net.luversof.api.poe.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 클러스터 주얼 정의(tools/poe-extract parse-cluster-jewels.mjs 산출 cluster-jewels.json).
 *
 * <p>PoB 는 소켓에 꽂힌 <b>주얼 아이템의 모드 문구</b>를 파싱해 서브트리를 생성한다. 문구가 그 크기의 실제 스킬 문구와 다르면 {@code
 * clusterJewelValid} 가 nil 이 되어 <b>서브트리를 조용히 만들지 않는다</b> — 그래서 아이템 텍스트를 이 정의에서 그대로 만들어야 한다.
 */
@Service
public class PoeClusterJewelDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeClusterJewelDataService.class);

  /** hasMastery = 스킬에 masteryIcon 존재(PoB BuildSubgraph 와 동일하게, 있을 때만 중앙 마스터리 노드 생성). */
  public record ClusterSkill(String name, List<String> stats, boolean hasMastery) {}

  /**
   * 인덱스 배열은 자동 클러스터 채택의 가상 노드 id 산출용(프론트 buildClusterSubgraph 파리티) — templateIndex 가 곧 id 의 하위 성분이라
   * 순서까지 정의 그대로 보존해야 한다.
   */
  public record ClusterJewelDef(
      int minNodes,
      int maxNodes,
      Map<String, ClusterSkill> skills,
      int sizeIndex,
      int totalIndicies,
      List<Integer> smallIndicies,
      List<Integer> notableIndicies,
      List<Integer> socketIndicies) {}

  private volatile Map<String, ClusterJewelDef> jewels = Map.of();

  /** 노터블 영문명 → PoB notableSortOrder(자리 배정은 이 값 오름차순 — 어긋나면 엔진과 다른 노드를 찍는다). */
  private volatile Map<String, Integer> notableSortOrder = Map.of();

  /** 노터블 합법성(cluster-jewels.json notableOptions) — tags=허용 skillKey 목록, sizes=허용 크기. */
  public record NotableOption(List<String> tags, List<String> sizes) {}

  private volatile Map<String, NotableOption> notableOptions = Map.of();

  public PoeClusterJewelDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    Path file = Path.of(dataDir, "cluster-jewels.json");
    if (!Files.isReadable(file)) {
      logger.info("클러스터 주얼 정의 없음 — 클러스터 평가는 비활성: {}", file);
      return;
    }
    try {
      var root = JsonMapper.builder().build().readTree(Files.readString(file));
      var parsed = new java.util.LinkedHashMap<String, ClusterJewelDef>();
      var jewelsNode = root.path("jewels");
      jewelsNode
          .fieldNames()
          .forEachRemaining(
              size -> {
                var def = jewelsNode.path(size);
                var skills = new java.util.LinkedHashMap<String, ClusterSkill>();
                def.path("skills")
                    .fieldNames()
                    .forEachRemaining(
                        key -> {
                          var skill = def.path("skills").path(key);
                          var stats = new java.util.ArrayList<String>();
                          skill.path("stats").forEach(s -> stats.add(s.asText()));
                          skills.put(
                              key,
                              new ClusterSkill(
                                  skill.path("name").asText(),
                                  stats,
                                  !skill.path("masteryIcon").asText("").isEmpty()));
                        });
                java.util.function.Function<String, List<Integer>> ints =
                    field -> {
                      var out = new java.util.ArrayList<Integer>();
                      def.path(field).forEach(v -> out.add(v.asInt()));
                      return List.copyOf(out);
                    };
                parsed.put(
                    size,
                    new ClusterJewelDef(
                        def.path("minNodes").asInt(),
                        def.path("maxNodes").asInt(),
                        skills,
                        def.path("sizeIndex").asInt(),
                        def.path("totalIndicies").asInt(),
                        ints.apply("smallIndicies"),
                        ints.apply("notableIndicies"),
                        ints.apply("socketIndicies")));
              });
      var sortOrder = new java.util.LinkedHashMap<String, Integer>();
      root.path("notableSortOrder")
          .fieldNames()
          .forEachRemaining(
              name -> sortOrder.put(name, root.path("notableSortOrder").path(name).asInt()));
      this.notableSortOrder = Map.copyOf(sortOrder);
      var options = new java.util.LinkedHashMap<String, NotableOption>();
      root.path("notableOptions")
          .fieldNames()
          .forEachRemaining(
              name -> {
                var opt = root.path("notableOptions").path(name);
                var tags = new java.util.ArrayList<String>();
                opt.path("tags").forEach(t -> tags.add(t.asText()));
                var sizes = new java.util.ArrayList<String>();
                opt.path("sizes").forEach(s -> sizes.add(s.asText()));
                options.put(name, new NotableOption(List.copyOf(tags), List.copyOf(sizes)));
              });
      this.notableOptions = Map.copyOf(options);
      this.jewels = Map.copyOf(parsed);
      logger.info("클러스터 주얼 정의 {}종 로드 (노터블 순서 {}건)", jewels.size(), notableSortOrder.size());
    } catch (Exception e) {
      logger.warn("클러스터 주얼 정의 로드 실패: {}", e.toString());
    }
  }

  public boolean hasData() {
    return !jewels.isEmpty();
  }

  public Optional<ClusterJewelDef> def(String sizeName) {
    return Optional.ofNullable(jewels.get(sizeName + " Cluster Jewel"));
  }

  /** 노터블 자리 배정 순서(PoB notableSortOrder) — 미등록 이름은 MAX(맨 뒤). */
  public int notableSortOrder(String notableName) {
    return notableSortOrder.getOrDefault(notableName, Integer.MAX_VALUE);
  }

  /** 이 (skillKey × 크기)에 합법인 노터블 이름들 — 어긋난 조합은 PoB 가 서브트리를 통째로 버린다. */
  public List<String> legalNotables(String skillKey, String sizeName) {
    return notableOptions.entrySet().stream()
        .filter(e -> e.getValue().tags().contains(skillKey))
        .filter(e -> e.getValue().sizes().contains(sizeName))
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * 클러스터 서브그래프 할당 계획 — 소켓에 이 클러스터를 꽂았을 때 <b>Spec nodes 에 넣어야 할 가상 노드 id</b>(노터블+소형, 마스터리는 스탯이 없어
   * 미할당)와 포인트 비용. 프론트 tree.ts buildClusterSubgraph 의 id 산식 포트(PoB 파리티 — cluster-port-check.js 로 검증된
   * 규칙): {@code id = 0x10000 + (size==2? index<<6 : size==1? index<<9 : 0) + (sizeIndex<<4) +
   * templateIndex}. 좌표·링크는 할당에 불필요해 생략.
   */
  public record SubgraphPlan(List<Integer> nodeIds, int pointCost) {}

  public Optional<SubgraphPlan> subgraphPlan(
      PoeTreeGraphService.ExpansionJewel exp,
      String sizeName,
      int nodeCount,
      List<String> notables,
      int socketCount) {
    if (exp == null || socketCount > 0) {
      // 중첩(자식 소켓 재사용)은 실제 트리 소켓 노드 id 조회가 필요해 미지원 — 자동 채택은 단순 클러스터만
      return Optional.empty();
    }
    return def(sizeName)
        .map(
            def -> {
              int base = 0x10000;
              if (exp.size() == 2) {
                base += exp.index() << 6;
              } else if (exp.size() == 1) {
                base += exp.index() << 9;
              }
              int nodeId = base + (def.sizeIndex() << 4);
              int notableCount = notables == null ? 0 : notables.size();
              var used = new java.util.HashSet<Integer>();
              var notableIdx = new java.util.ArrayList<Integer>();
              for (int raw : def.notableIndicies()) {
                if (notableIdx.size() == notableCount) {
                  break;
                }
                int idx = raw;
                if ("Medium".equals(sizeName)) {
                  // 프론트/PoB 와 동일한 중형 배치 보정(소켓0·노터블2, 4노드)
                  if (notableCount == 2) {
                    idx = idx == 6 ? 4 : idx == 10 ? 8 : idx;
                  } else if (nodeCount == 4) {
                    idx = idx == 10 ? 9 : idx == 2 ? 3 : idx;
                  }
                }
                if (used.add(idx)) {
                  notableIdx.add(idx);
                }
              }
              java.util.Collections.sort(notableIdx);
              int smallCount = nodeCount - socketCount - notableCount;
              var smallIdx = new java.util.ArrayList<Integer>();
              for (int raw : def.smallIndicies()) {
                if (smallIdx.size() == smallCount) {
                  break;
                }
                int idx = raw;
                if ("Medium".equals(sizeName)) {
                  if (nodeCount == 5 && idx == 4) {
                    idx = 3;
                  } else if (nodeCount == 4) {
                    idx = idx == 8 ? 9 : idx == 4 ? 3 : idx;
                  }
                }
                if (used.add(idx)) {
                  smallIdx.add(idx);
                }
              }
              var ids = new java.util.ArrayList<Integer>();
              for (int idx : notableIdx) {
                ids.add(nodeId + idx);
              }
              for (int idx : smallIdx) {
                ids.add(nodeId + idx);
              }
              return new SubgraphPlan(List.copyOf(ids), ids.size());
            });
  }

  /**
   * PoB 가 인식하는 클러스터 주얼 아이템 텍스트. 노드 수는 크기별 허용 범위로 클램프하고, 작은 패시브 문구는 정의의 stats 를 그대로 쓴다(임의 문구면 PoB 가
   * 서브트리를 만들지 않는다).
   */
  public Optional<String> itemText(
      String sizeName, int nodeCount, String skillKey, List<String> notables, int socketCount) {
    return def(sizeName)
        .map(
            def -> {
              int nodes = Math.max(def.minNodes(), Math.min(def.maxNodes(), nodeCount));
              ClusterSkill skill = def.skills().get(skillKey);
              String grant =
                  skill != null && !skill.stats().isEmpty()
                      ? skill.stats().get(0)
                      : def.skills().values().stream()
                          .filter(s -> !s.stats().isEmpty())
                          .findFirst()
                          .map(s -> s.stats().get(0))
                          .orElse(null);
              if (grant == null) {
                return null;
              }
              // 노터블은 "1 Added Passive Skill is <영문 이름>" 한 줄씩. PoB 는 이 이름으로 트리의
              // 클러스터 노터블(clusterNodeMap)을 찾고, **하나라도 못 찾으면 서브트리를 통째로 버린다**.
              StringBuilder added = new StringBuilder();
              // 주얼 소켓은 별도 문구다(PoB ItemsTab:CraftClusterJewel 과 동일한 표현).
              // 소켓이 있어야 그 안에 다시 클러스터를 꽂을 수 있다(중첩).
              if (socketCount == 1) {
                added.append("1 Added Passive Skill is a Jewel Socket\n");
              } else if (socketCount > 1) {
                added.append(socketCount).append(" Added Passive Skills are Jewel Sockets\n");
              }
              for (String notable : notables == null ? List.<String>of() : notables) {
                if (notable != null && !notable.isBlank()) {
                  added.append("1 Added Passive Skill is ").append(notable.trim()).append("\n");
                }
              }
              return "Rarity: RARE\nSim Cluster\n"
                  + sizeName
                  + " Cluster Jewel\nItem Level: 84\nImplicits: 0\nAdds "
                  + nodes
                  + " Passive Skills\n"
                  + added
                  + "Added Small Passive Skills grant: "
                  + grant
                  + "\n";
            });
  }
}
