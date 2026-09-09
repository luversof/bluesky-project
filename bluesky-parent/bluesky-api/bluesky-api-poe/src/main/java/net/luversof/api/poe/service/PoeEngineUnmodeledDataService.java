package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
 * PoB 엔진이 <b>모델링하지 않는</b> 전직 노드 — tools/poe-extract/parse-engine-unmodeled.mjs 가 만든 {@code
 * ~/.poe-gamedata/engine-unmodeled.json}.
 *
 * <p>발단(2026-09-09): 사이온 루미너리는 스탯 노드 17 개 중 15 개가 "Your Mercenary ..." 인데 PoB 계산 엔진에는 용병 액터가 아예 없다
 * — src/Modules · src/Classes 통틀어 mercenary 참조 0 건이고, Data.lua 가 ModMercenary 를
 * Explicit/Corrupted/Delve/Eldritch 와 나란히 <b>아이템 모드 풀</b>로만 등록한다. 그래서 루미너리를 고르면 전직 8pt 가 값을 <b>정확히
 * 0</b> 으로 내는데 화면엔 아무 표시가 없었다.
 *
 * <p>판별 근거는 PoB 자신의 기록이다 — ModCache 항목은 {모드, 미해석텍스트} 형태이고 모드가 nil 이면 그 문장을 엔진이 modifier 하나로도 바꾸지
 * 못했다는 뜻이다(실측: 파싱 20,393 · 미파싱 2,814 · 미모델링 전직 노드 17 개, 최다 Luminary 11/17).
 */
@Service
public class PoeEngineUnmodeledDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeEngineUnmodeledDataService.class);

  /** 엔진이 모델링하지 않는 노드 한 줄. */
  public record UnmodeledNode(int id, String name, String nameKo, String ascendancy) {}

  /** 전직별 집계 — total = 스탯을 가진 노드 수, unmodeled = 그중 전 스탯이 미파싱인 노드 수. */
  public record AscendancySummary(int total, int unmodeled) {}

  private record UnmodeledData(
      String source,
      int parsed,
      int unparsed,
      List<UnmodeledNode> unmodeledNodes,
      Map<String, AscendancySummary> ascendancySummary) {}

  private static final UnmodeledData EMPTY = new UnmodeledData("", 0, 0, List.of(), Map.of());

  private final Path dataFile;
  private volatile UnmodeledData data = EMPTY;
  private volatile Set<Integer> nodeIds = Set.of();

  public PoeEngineUnmodeledDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "engine-unmodeled.json");
    reload();
  }

  public synchronized void reload() {
    UnmodeledData loaded = EMPTY;
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, UnmodeledData.class);
        logger.info(
            "PoE 엔진 미모델링 노드 로드: {} (노드 {}개 · 전직 {}종)",
            dataFile,
            loaded.unmodeledNodes().size(),
            loaded.ascendancySummary().size());
      } catch (Exception e) {
        logger.warn("PoE 엔진 미모델링 노드 로드 실패: {}", dataFile, e);
      }
    } else {
      // 없어도 동작에는 지장이 없다 — 경고를 못 띄울 뿐이다. 파이프라인을 한 번 돌리면 생긴다.
      logger.warn("PoE 엔진 미모델링 노드 없음: {} — parse-engine-unmodeled.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
    Set<Integer> ids = new HashSet<>();
    for (UnmodeledNode node : loaded.unmodeledNodes()) {
      ids.add(node.id());
    }
    this.nodeIds = Set.copyOf(ids);
  }

  /** 할당한 노드 중 엔진이 모델링하지 않는 것들(id 순). 데이터가 없으면 빈 목록. */
  public List<UnmodeledNode> unmodeledIn(Collection<Integer> allocatedNodeIds) {
    if (nodeIds.isEmpty() || allocatedNodeIds == null || allocatedNodeIds.isEmpty()) {
      return List.of();
    }
    List<UnmodeledNode> hits = new ArrayList<>();
    for (UnmodeledNode node : data.unmodeledNodes()) {
      if (allocatedNodeIds.contains(node.id())) {
        hits.add(node);
      }
    }
    return List.copyOf(hits);
  }

  /** 그 전직의 집계(스탯 노드 수 / 그중 미모델링 수). 모르면 null. */
  public AscendancySummary summaryOf(String ascendancy) {
    return data.ascendancySummary().get(ascendancy);
  }

  /** 그 전직의 스탯 노드 중 미모델링 비율(0~1). 데이터가 없거나 모르는 전직이면 0. */
  public double unmodeledShare(String ascendancy) {
    AscendancySummary summary = data.ascendancySummary().get(ascendancy);
    if (summary == null || summary.total() <= 0) {
      return 0d;
    }
    return (double) summary.unmodeled() / summary.total();
  }
}
