package net.luversof.api.poe.poe2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoB-PoE2 헤드리스 계산 — PoE1 엔진과 같은 러너({@code tools/poe-pob/calc.lua})를 PoB-PoE2 소스({@code
 * ~/.poe-gamedata/poe2/work/pob2-src}, tools/poe2-extract/patch-pob2.mjs 가 받아 표준 LuaJIT 용으로 고친 것)에서
 * 돌린다. 한 번에 약 2초(차가운 기동) — 상주 워커 없이 요청마다 프로세스 하나. 동시에 둘까지.
 *
 * <p>PoE1 PoePobEngineService 와 따로 둔 까닭: PoE1 은 최적화기용 상주 워커 풀·타임리스 .bin 등 PoE1 전용 장치가 얽혀 있고, PoE2 는
 * 빌드 요약의 재계산 한 가지뿐이다.
 */
@Service
public class Poe2PobEngineService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2PobEngineService.class);
  private static final String RESULT = "@@POB_RESULT@@";
  private static final String GUIDE = "@@POB_GUIDE@@";
  private static final String ERROR = "@@POB_ERROR@@";
  private static final String APPLIED = "@@APPLIED@@";

  private final Path sourceDir;
  private final Path script;
  private final Path guideScript;
  private final Path applyScript;
  private final String luajit;

  /**
   * 가이드 조각 수 — 실빌드(레벨 100) 가이드가 한 프로세스로 22초, 3조각 12.8초·4조각 10.8초·6조각 11.0초(09-30 실측, 로드 2~3초가 바닥).
   */
  // 6조각(10-02, 4 → 6): ⑦ 나눔 뒤 실빌드 11.5 → 10.2초. 결과는 조각 수와 무관(4·6 조각 가이드 전체 JSON 동일 확인). 동시 실행
  // 한도(slots)도 8 → 12 —
  //   본 가이드 6 + 레어 칸 6 이 함께 돈다(28 코어 로컬 기준 — k8s 엔 엔진이 없다).
  private static final int GUIDE_SHARDS = 6;

  /** 동시 프로세스 상한 — 한 사람이 재계산 1 + 가이드 4 를 한꺼번에 띄운다. 프로세스당 PoB 데이터 적재. */
  private final Semaphore slots = new Semaphore(12);

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  public Poe2PobEngineService(
      @Value("${poe2.pob.src-dir:${user.home}/.poe-gamedata/poe2/work/pob2-src}") String sourceDir,
      @Value("${poe2.pob.script:tools/poe-pob/calc.lua}") String script,
      @Value("${poe2.pob.guide-script:tools/poe2-pob/guide2.lua}") String guideScript,
      @Value("${poe2.pob.apply-script:tools/poe2-pob/apply2.lua}") String applyScript,
      @Value("${poe.pob.luajit-path:}") String luajitPath) {
    this.sourceDir = Path.of(sourceDir);
    this.script = Path.of(script).toAbsolutePath();
    this.guideScript = Path.of(guideScript).toAbsolutePath();
    this.applyScript = Path.of(applyScript).toAbsolutePath();
    Path winget =
        Path.of(
            System.getProperty("user.home"),
            "AppData",
            "Local",
            "Programs",
            "LuaJIT",
            "bin",
            "luajit.exe");
    this.luajit =
        luajitPath != null && !luajitPath.isBlank()
            ? luajitPath
            : Files.exists(winget) ? winget.toString() : "luajit";
  }

  /** 이 서버에서 계산할 수 있는가(PoB-PoE2 소스 + 러너). */
  public boolean available() {
    return Files.isDirectory(sourceDir.resolve("src")) && Files.exists(script);
  }

  /** 계산 결과 — values 는 PoB mainOutput 에서 추린 숫자(calc.lua 목록), 실패면 error. */
  public record Result(Map<String, Double> values, String error, long elapsedMs) {}

  /** 빌드 XML → PoB 계산 스탯. 예외 대신 Result.error 로 사유를 돌려준다(요약 화면은 실패해도 나머지를 보여야 한다). */
  public Result calc(String buildXml) {
    Raw raw = run(script, RESULT, buildXml, List.of());
    if (raw.error() != null) {
      return new Result(Map.of(), raw.error(), raw.elapsedMs());
    }
    try {
      Map<String, Double> values = new LinkedHashMap<>();
      JsonNode node = jsonMapper.readTree(raw.payload());
      for (Map.Entry<String, JsonNode> e : node.properties()) {
        if (e.getValue().isNumber()) {
          values.put(e.getKey(), e.getValue().asDouble());
        }
      }
      logger.info("PoE2 엔진 계산 {}ms · 스탯 {}개", raw.elapsedMs(), values.size());
      return new Result(values, null, raw.elapsedMs());
    } catch (Exception e) {
      return new Result(Map.of(), "engine-bad-json", raw.elapsedMs());
    }
  }

  /**
   * 가이드 추천 한 건을 PoB 안에서 실제로 적용하고 저장한 빌드 XML(tools/poe2-pob/apply2.lua) — 자동 다듬기용. 적용이 실패하면 null(사유는
   * 로그).
   *
   * @param changeJson apply2.lua 머리 주석의 변경 형식({type:"unique"|"mod"|"gem"|"nodes", …})
   */
  public String apply(String buildXml, String changeJson) {
    if (!Files.exists(applyScript)) {
      logger.warn("PoE2 적용 스크립트 없음: {}", applyScript);
      return null;
    }
    Path change = null;
    Path out = null;
    try {
      change = Files.createTempFile("poe2-change-", ".json");
      out = Files.createTempFile("poe2-applied-", ".xml");
      Files.writeString(change, changeJson, StandardCharsets.UTF_8);
      Raw raw = run(applyScript, APPLIED, buildXml, List.of(change.toString(), out.toString()));
      if (raw.error() != null) {
        logger.warn(
            "PoE2 적용 실패: {} ({})",
            raw.error(),
            changeJson.length() > 200 ? changeJson.substring(0, 200) : changeJson);
        return null;
      }
      return Files.readString(out, StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      logger.warn("PoE2 적용 입출력 오류", e);
      return null;
    } finally {
      for (Path f : new Path[] {change, out}) {
        if (f != null) {
          try {
            Files.deleteIfExists(f);
          } catch (Exception ignore) {
            // 임시 파일
          }
        }
      }
    }
  }

  /** 러너 한 번의 원 출력 — payload 는 마커 뒤 JSON 문자열. */
  public record Raw(String payload, String error, long elapsedMs) {}

  /** 업그레이드 가이드(tools/poe2-pob/guide2.lua) — 빌드를 한 번 싣고 PoB 비교 계산기로 칸·보조젬·패시브·고유 교체를 잰다(약 3초). */
  public Raw guide(String buildXml, String candidatesJson) {
    if (!Files.exists(guideScript)) {
      return new Raw(null, "guide-unavailable", 0);
    }
    Path candidates = null;
    Raw raw;
    try {
      // 옵션 후보(칸별 풀 최고 등급 줄) — 러너의 두 번째 인자(없으면 옵션 목표 단계를 건너뛴다)
      if (candidatesJson != null) {
        candidates = Files.createTempFile("poe2-guide-cand-", ".json");
        Files.writeString(candidates, candidatesJson, StandardCharsets.UTF_8);
      }
      String candArg = candidates != null ? candidates.toString() : "-";
      raw = runGuideShards(buildXml, candArg);
    } catch (java.io.IOException e) {
      return new Raw(null, "guide-candidates-io", 0);
    } finally {
      if (candidates != null) {
        try {
          Files.deleteIfExists(candidates);
        } catch (Exception ignore) {
          // 임시 파일
        }
      }
    }
    logger.info(
        "PoE2 가이드 계산 {}ms{}", raw.elapsedMs(), raw.error() != null ? " · 실패 " + raw.error() : "");
    return raw;
  }

  /**
   * 가이드를 GUIDE_SHARDS 조각으로 나란히 돌려 합친다(guide2.lua 머리 주석의 조각 규칙). 합치기: 공통(기준·칸·보조젬 기여·경고)은 조각 0, 한
   * 조각만 맡는 목록(패시브 기여·다음 패시브·옵션 목표)은 가진 조각에서, 나눠 잰 상위 목록(고유 교체·보조젬 교체)은 모아 다시 줄 세운 뒤 같은 기준으로 중복을 걸러
   * 상위 8.
   */
  private Raw runGuideShards(String buildXml, String candArg) {
    long t0 = System.currentTimeMillis();
    List<java.util.concurrent.CompletableFuture<Raw>> futures = new java.util.ArrayList<>();
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < GUIDE_SHARDS; i++) {
        String shard = i + "/" + GUIDE_SHARDS;
        futures.add(
            java.util.concurrent.CompletableFuture.supplyAsync(
                () -> run(guideScript, GUIDE, buildXml, List.of(candArg, shard)), executor));
      }
      List<JsonNode> parts = new java.util.ArrayList<>();
      for (var f : futures) {
        Raw r = f.join();
        if (r.error() != null) {
          return new Raw(null, r.error(), System.currentTimeMillis() - t0);
        }
        parts.add(jsonMapper.readTree(r.payload()));
      }
      long elapsed = System.currentTimeMillis() - t0;
      // 조각별 단계 시간(guide2.lua lap, 초) — 가장 느린 조각 · 단계를 찾는 근거(10-02 속도 점검)
      logger.info(
          "PoE2 가이드 {}ms — 조각별 단계(초) {}",
          elapsed,
          parts.stream().map(p -> p.path("timing").toString()).toList());
      return new Raw(mergeGuide(parts).toString(), null, elapsed);
    } catch (Exception e) {
      logger.warn("PoE2 가이드 조각 합치기 실패", e);
      return new Raw(null, "guide-merge-error", System.currentTimeMillis() - t0);
    }
  }

  static tools.jackson.databind.node.ObjectNode mergeGuide(List<JsonNode> parts) {
    tools.jackson.databind.node.ObjectNode m =
        (tools.jackson.databind.node.ObjectNode) parts.get(0).deepCopy();
    for (String key : List.of("nodes", "modTargets", "nextWeaponSetSkipped")) {
      for (JsonNode p : parts) {
        if (p.has(key) && !p.get(key).isNull()) {
          m.set(key, p.get(key));
          break;
        }
      }
    }
    m.put("tried", parts.stream().mapToInt(p -> p.path("tried").asInt(0)).sum());
    m.put("gemTried", parts.stream().mapToInt(p -> p.path("gemTried").asInt(0)).sum());
    // 다음 패시브(⑦)는 후보를 조각마다 나눠 잰다(10-02) — 조각별 상위 8 을 모아 guide2.lua topPer 와 같은 기준으로 다시 상위 8
    m.put("nextTried", parts.stream().mapToInt(p -> p.path("nextTried").asInt(0)).sum());
    m.set("nextDps", mergePerPoint(parts, "nextDps", "dps"));
    m.set("nextEhp", mergePerPoint(parts, "nextEhp", "ehp"));
    m.set("swapsDps", mergeTop(parts, "swapsDps", "dps"));
    m.set("swapsEhp", mergeTop(parts, "swapsEhp", "ehp"));
    m.set("gemSwaps", mergeTop(parts, "gemSwaps", "dps"));
    // 레어 목표는 칸을 조각마다 나눠 잰다 — 모든 조각 것을 잇는다(10-02)
    tools.jackson.databind.node.ArrayNode rare =
        tools.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    for (JsonNode p : parts) {
      p.path("rareTargets").forEach(rare::add);
    }
    m.set("rareTargets", rare);
    m.remove("shard");
    return m;
  }

  /**
   * 다음 패시브 목록 합치기 — guide2.lua topPer 와 같은 기준: 점수당(key / points) 내림차순, 동점은 노드 id 오름차순, 상위 8. 조각마다
   * 이미 같은 기준으로 거른(합계 key ≥ 1 · 다른 축 ≥ -5) 상위 8 이라, 모아 다시 줄 세우면 한 프로세스로 잰 상위 8 과 같다.
   */
  static tools.jackson.databind.node.ArrayNode mergePerPoint(
      List<JsonNode> parts, String list, String key) {
    List<JsonNode> all = new java.util.ArrayList<>();
    for (JsonNode p : parts) {
      p.path(list).forEach(all::add);
    }
    all.sort(
        java.util.Comparator.<JsonNode>comparingDouble(
                n -> -n.path(key).asDouble() / Math.max(1, n.path("points").asDouble(1)))
            .thenComparingLong(n -> n.path("id").asLong()));
    tools.jackson.databind.node.ArrayNode out =
        tools.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    for (int i = 0; i < Math.min(8, all.size()); i++) {
      out.add(all.get(i));
    }
    return out;
  }

  /**
   * 조각별 상위 목록을 모아 key 내림차순(동점은 이름·칸) → 계열(family) 또는 아이템 이름으로 중복 제거 → 상위 8 (guide2.lua top() 과 같은
   * 규칙).
   */
  private static tools.jackson.databind.node.ArrayNode mergeTop(
      List<JsonNode> parts, String list, String key) {
    List<JsonNode> all = new java.util.ArrayList<>();
    for (JsonNode p : parts) {
      p.path(list).forEach(all::add);
    }
    all.sort(
        java.util.Comparator.<JsonNode>comparingDouble(n -> -n.path(key).asDouble())
            .thenComparing(n -> n.path("item").asString(""))
            .thenComparing(n -> n.path("slot").asString("")));
    tools.jackson.databind.node.ArrayNode out =
        tools.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (JsonNode n : all) {
      String k = n.hasNonNull("family") ? n.get("family").asString() : n.path("item").asString("");
      if (seen.add(k)) {
        out.add(n);
        if (out.size() >= 8) {
          break;
        }
      }
    }
    return out;
  }

  private Raw run(Path runner, String marker, String buildXml, List<String> extraArgs) {
    long t0 = System.currentTimeMillis();
    if (!available()) {
      return new Raw(null, "engine-unavailable", 0);
    }
    Path xml = null;
    boolean acquired = false;
    try {
      acquired = slots.tryAcquire(30, TimeUnit.SECONDS);
      if (!acquired) {
        return new Raw(null, "engine-busy", System.currentTimeMillis() - t0);
      }
      xml = Files.createTempFile("poe2-build-", ".xml");
      Files.writeString(xml, buildXml, StandardCharsets.UTF_8);
      java.util.List<String> cmd =
          new java.util.ArrayList<>(List.of(luajit, runner.toString(), xml.toString()));
      cmd.addAll(extraArgs);
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(sourceDir.resolve("src").toFile());
      pb.redirectErrorStream(true);
      Process process = pb.start();
      String resultLine = null;
      String errorLine = null;
      try (BufferedReader out =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = out.readLine()) != null) {
          if (line.startsWith(marker)) {
            resultLine = line.substring(marker.length());
          } else if (line.startsWith(ERROR)) {
            errorLine = line.substring(ERROR.length());
          }
        }
      }
      if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return new Raw(null, "engine-timeout", System.currentTimeMillis() - t0);
      }
      long elapsed = System.currentTimeMillis() - t0;
      if (resultLine == null) {
        logger.warn("PoE2 엔진 실행 실패({}, {}ms): {}", runner.getFileName(), elapsed, errorLine);
        return new Raw(null, errorLine != null ? errorLine : "engine-no-result", elapsed);
      }
      return new Raw(resultLine, null, elapsed);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new Raw(null, "engine-interrupted", System.currentTimeMillis() - t0);
    } catch (Exception e) {
      logger.warn("PoE2 엔진 실행 오류", e);
      return new Raw(null, "engine-error", System.currentTimeMillis() - t0);
    } finally {
      if (acquired) {
        slots.release();
      }
      if (xml != null) {
        try {
          Files.deleteIfExists(xml);
        } catch (Exception ignore) {
          // 임시 파일 — 못 지워도 OS 가 치운다
        }
      }
    }
  }
}
