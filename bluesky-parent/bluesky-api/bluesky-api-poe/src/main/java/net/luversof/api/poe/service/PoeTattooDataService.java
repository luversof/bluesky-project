package net.luversof.api.poe.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 문신(Tattoo) 정의(tools/poe-extract parse-tattoos.mjs 산출 tattoos.json).
 *
 * <p>문신은 <b>이미 할당한 패시브를 다른 노드로 교체</b>하는 아이템이다. 반경 주얼(예: 삿된 붉은 악몽 — "반경 내 화염/모든 원소 저항을 주는 패시브"를 변환)과
 * 짝지어, 반경 안 소형 패시브를 저항 문신으로 바꿔 효과 대상을 늘리는 것이 실전 용법이다.
 *
 * <p>PoB 는 빌드 XML 의 {@code <Spec><Overrides><Override nodeId dn .../></Overrides>} 를 {@code
 * tree.tattoo.nodes[dn]}(Data/TattooPassives.lua) 로 찾아 적용한다 — 헤드리스에서도 동작함을 실측 확인(소형 지능 패시브를
 * "Honoured Tattoo of the Sky" 로 덮으니 원소 저항 -60 → -57).
 */
@Service
public class PoeTattooDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeTattooDataService.class);

  /**
   * @param targetType 교체 가능한 패시브 종류 — "Small Strength"/"Small Dexterity"/"Small
   *     Intelligence"/"Small Attribute"/"Notable"/"Keystone"/"Mastery"
   * @param minConnected 인접 할당 패시브 수 하한(게임 규칙, 대부분 0)
   * @param maxConnected 인접 할당 패시브 수 상한(대부분 100 = 사실상 무제한)
   */
  public record Tattoo(
      String dn,
      String name,
      String nameKo,
      String icon,
      String targetType,
      int minConnected,
      int maxConnected,
      boolean notable,
      boolean keystone,
      List<String> stats,
      List<String> statsKo) {}

  private volatile List<Tattoo> tattoos = List.of();
  private volatile Map<String, Tattoo> byDn = Map.of();

  private final Path dataFile;

  public PoeTattooDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "tattoos.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    Path file = dataFile;
    if (!Files.isReadable(file)) {
      logger.info("문신 정의 없음 — 문신 적용은 비활성: {}", file);
      return;
    }
    try {
      var root = JsonMapper.builder().build().readTree(Files.readString(file));
      var parsed = new ArrayList<Tattoo>();
      for (var node : root.path("tattoos")) {
        var stats = new ArrayList<String>();
        node.path("stats").forEach(s -> stats.add(s.asText()));
        var statsKo = new ArrayList<String>();
        node.path("statsKo").forEach(s -> statsKo.add(s.asText()));
        parsed.add(
            new Tattoo(
                node.path("dn").asText(),
                node.path("name").asText(),
                node.path("nameKo").asText(null),
                node.path("icon").asText(""),
                node.path("targetType").asText(""),
                node.path("minConnected").asInt(0),
                node.path("maxConnected").asInt(100),
                node.path("notable").asBoolean(),
                node.path("keystone").asBoolean(),
                List.copyOf(stats),
                List.copyOf(statsKo.isEmpty() ? stats : statsKo)));
      }
      this.tattoos = List.copyOf(parsed);
      var map = new LinkedHashMap<String, Tattoo>();
      parsed.forEach(t -> map.put(t.dn(), t));
      this.byDn = Map.copyOf(map);
      logger.info("문신 {}종 로드", tattoos.size());
    } catch (Exception e) {
      logger.warn("문신 정의 로드 실패: {}", e.toString());
    }
  }

  public boolean hasData() {
    return !tattoos.isEmpty();
  }

  public List<Tattoo> all() {
    return tattoos;
  }

  public Optional<Tattoo> findByDn(String dn) {
    return Optional.ofNullable(dn == null ? null : byDn.get(dn.trim()));
  }

  /**
   * 어떤 패시브에 붙일 수 있는 문신 목록. 게임은 소형 패시브의 <b>속성 종류</b>까지 구분한다(힘 소형에는 힘 문신) — 속성 소형이면 그 속성 + "Small
   * Attribute", 그 외 소형이면 붙일 수 있는 문신이 없다.
   *
   * @param nodeType 트리 노드 종류("normal"/"notable"/"keystone"/"mastery")
   * @param attribute 소형 속성 패시브면 "Strength"/"Dexterity"/"Intelligence", 아니면 null
   */
  /**
   * 현재 리그에서 구할 수 없는 <b>레거시 문신</b> 접두 — "명예로운(Honoured)" 계열.
   *
   * <p>근거(실측): poe.ninja 현재 리그 실빌드 패싯에서 일반 문신 43종이 30.5만 회 쓰이는 동안 <b>명예로운 18종은 0회</b>다. 명예로운은 일반의
   * 상위 호환(노터블을 +30 속성으로 대체)이라, 구할 수 있다면 상위 목록에서 통째로 빠질 이유가 없다. 게임 테이블엔 획득 가능 여부 플래그가 없어(둘 다
   * DropLevel 1) 이 이름 규칙이 현재로선 유일한 판별 수단이다.
   *
   * <p>목록/사전에서는 계속 보여준다 — 못 구한다고 존재까지 감출 이유는 없다. <b>최적화기 후보에서만</b> 뺀다.
   */
  private static final String LEGACY_TATTOO_PREFIX = "Honoured ";

  /** 최적화기가 써도 되는 문신인가(현재 리그 획득 가능). */
  public static boolean isCurrentLeague(Tattoo tattoo) {
    return tattoo.name() == null || !tattoo.name().startsWith(LEGACY_TATTOO_PREFIX);
  }

  public List<Tattoo> candidates(String nodeType, String attribute) {
    String target =
        switch (nodeType == null ? "" : nodeType) {
          case "notable" -> "Notable";
          case "keystone" -> "Keystone";
          case "mastery" -> "Mastery";
          default -> attribute == null ? null : "Small " + attribute;
        };
    if (target == null) {
      return List.of();
    }
    boolean smallAttr = target.startsWith("Small ");
    return tattoos.stream()
        .filter(PoeTattooDataService::isCurrentLeague)
        .filter(
            t ->
                t.targetType().equals(target)
                    || (smallAttr && "Small Attribute".equals(t.targetType())))
        .toList();
  }
}
