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

  public record ClusterSkill(String name, List<String> stats) {}

  public record ClusterJewelDef(int minNodes, int maxNodes, Map<String, ClusterSkill> skills) {}

  private volatile Map<String, ClusterJewelDef> jewels = Map.of();

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
                          skills.put(key, new ClusterSkill(skill.path("name").asText(), stats));
                        });
                parsed.put(
                    size,
                    new ClusterJewelDef(
                        def.path("minNodes").asInt(), def.path("maxNodes").asInt(), skills));
              });
      this.jewels = Map.copyOf(parsed);
      logger.info("클러스터 주얼 정의 {}종 로드", jewels.size());
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
