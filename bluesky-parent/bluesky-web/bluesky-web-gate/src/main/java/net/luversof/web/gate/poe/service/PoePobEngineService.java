package net.luversof.web.gate.poe.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * Path of Building 커뮤니티 계산 엔진을 헤드리스(LuaJIT)로 구동해 빌드 스탯을 실제 재계산한다.
 *
 * <p>실행 = {@code luajit tools/poe-pob/calc.lua <build.xml>} (cwd 는 PoB 소스의 src). PoB 소스는 파생 산출물이라
 * git 밖({@code ~/.poe-gamedata/work/pob-src})에 클론해 둔다. 러너는 계산 결과를 {@code @@POB_RESULT@@{json}} 한 줄로
 * 출력한다. 회당 약 1.5초라 코드 sha-256 키 LRU 캐시를 둔다.
 */
@Service
public class PoePobEngineService {

  private static final Logger logger = LoggerFactory.getLogger(PoePobEngineService.class);

  private static final String RESULT_MARKER = "@@POB_RESULT@@";
  private static final String ERROR_MARKER = "@@POB_ERROR@@";
  private static final int CACHE_SIZE = 64;

  /** 표시 순서. uiMessage 의 poe.build.stat.<소문자 키> 와 짝을 이룬다. */
  private static final List<String> STAT_KEYS =
      List.of(
          "CombinedDPS",
          "TotalDPS",
          "FullDPS",
          "AverageDamage",
          "Speed",
          "Life",
          "EnergyShield",
          "Mana",
          "Armour",
          "Evasion",
          "TotalEHP",
          "FireResist",
          "ColdResist",
          "LightningResist",
          "ChaosResist",
          "CritChance");

  public record EngineResult(List<PoeBuild.PlayerStat> stats, long durationMs) {}

  private final Path sourceDir;
  private final Path runnerScript;
  private final String luajitPath;

  private final Map<String, EngineResult> cache =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EngineResult> eldest) {
          return size() > CACHE_SIZE;
        }
      };

  public PoePobEngineService(
      @Value("${poe.pob.src-dir:${user.home}/.poe-gamedata/work/pob-src}") String sourceDir,
      @Value("${poe.pob.runner:tools/poe-pob/calc.lua}") String runnerScript,
      @Value("${poe.pob.luajit-path:}") String luajitPath) {
    this.sourceDir = Path.of(sourceDir);
    this.runnerScript = Path.of(runnerScript).toAbsolutePath();
    this.luajitPath = resolveLuajit(luajitPath);
  }

  private String resolveLuajit(String configured) {
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    Path wingetInstall =
        Path.of(
            System.getProperty("user.home"),
            "AppData",
            "Local",
            "Programs",
            "LuaJIT",
            "bin",
            "luajit.exe");
    return Files.exists(wingetInstall) ? wingetInstall.toString() : "luajit";
  }

  /** 이 서버에서 엔진 실행이 가능한지 (PoB 소스 + 러너 존재 여부) */
  public boolean isAvailable() {
    return Files.exists(sourceDir.resolve("src").resolve("HeadlessWrapper.lua"))
        && Files.exists(runnerScript);
  }

  /**
   * 빌드 XML 을 PoB 엔진으로 재계산한다. 같은 XML 은 캐시에서 즉시 반환.
   *
   * @throws IllegalStateException 엔진 실행 실패/시간 초과/결과 누락
   */
  public EngineResult recalculate(String buildXml) {
    String cacheKey = sha256(buildXml);
    synchronized (this) {
      EngineResult cached = cache.get(cacheKey);
      if (cached != null) {
        return cached;
      }
      EngineResult result = run(buildXml);
      cache.put(cacheKey, result);
      return result;
    }
  }

  /**
   * 배치 평가용 — 캐시/락 없이 계산해 숫자 스탯 맵을 그대로 돌려준다. luajit 프로세스는 서로 독립이라 여러 스레드에서 병렬 호출해도 된다.
   *
   * @throws IllegalStateException 엔진 실행 실패/시간 초과/결과 누락
   */
  public Map<String, Double> calculateValues(String buildXml) {
    return runRaw(buildXml);
  }

  private EngineResult run(String buildXml) {
    long startedAt = System.currentTimeMillis();
    return new EngineResult(toStats(runRaw(buildXml)), System.currentTimeMillis() - startedAt);
  }

  private Map<String, Double> runRaw(String buildXml) {
    Path xmlFile = null;
    try {
      xmlFile = Files.createTempFile("pob-build", ".xml");
      Files.writeString(xmlFile, buildXml, StandardCharsets.UTF_8);

      ProcessBuilder processBuilder =
          new ProcessBuilder(luajitPath, runnerScript.toString(), xmlFile.toString());
      processBuilder.directory(sourceDir.resolve("src").toFile());
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      // PoB 는 시작 오류 시 io.read 로 stdin 을 기다린다 — 먼저 닫아 프롬프트 대기(무한 멈춤)를 차단
      process.getOutputStream().close();

      // stdout 은 별도 스레드로 읽고, 본 스레드는 시간 제한을 강제한다 (멈춘 프로세스는 강제 종료).
      // readLine 을 본 스레드에서 하면 멈춘 프로세스에 무한 블록되어 배치/잡 전체가 멈춘다.
      List<String> outputLines =
          java.util.Collections.synchronizedList(new java.util.ArrayList<>());
      Thread outputReader =
          new Thread(
              () -> {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                  }
                } catch (Exception ignored) {
                  // 강제 종료 시 스트림 예외는 무시
                }
              },
              "pob-engine-output");
      outputReader.setDaemon(true);
      outputReader.start();

      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        outputReader.join(5000);
        throw new IllegalStateException("PoB 엔진 시간 초과 (120초) — 프로세스 강제 종료");
      }
      outputReader.join(10000);

      String resultJson = null;
      String errorMessage = null;
      for (String line : List.copyOf(outputLines)) {
        if (line.startsWith(RESULT_MARKER)) {
          resultJson = line.substring(RESULT_MARKER.length());
        } else if (line.startsWith(ERROR_MARKER)) {
          errorMessage = line.substring(ERROR_MARKER.length());
        } else {
          logger.debug("PoB engine: {}", line);
        }
      }
      if (errorMessage != null) {
        throw new IllegalStateException("PoB 엔진 오류: " + errorMessage);
      }
      if (resultJson == null) {
        throw new IllegalStateException("PoB 엔진 결과 누락 (exit " + process.exitValue() + ")");
      }
      return parseValues(resultJson);
    } catch (IllegalStateException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("PoB 엔진 실행 중단", e);
    } catch (Exception e) {
      throw new IllegalStateException("PoB 엔진 실행 실패", e);
    } finally {
      if (xmlFile != null) {
        try {
          Files.deleteIfExists(xmlFile);
        } catch (Exception ignored) {
          // 임시 파일 삭제 실패는 무시
        }
      }
    }
  }

  private Map<String, Double> parseValues(String json) {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    @SuppressWarnings("unchecked")
    Map<String, Object> raw = jsonMapper.readValue(json, Map.class);
    Map<String, Double> values = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      if (entry.getValue() instanceof Number number) {
        values.put(entry.getKey(), number.doubleValue());
      }
    }
    return values;
  }

  /** 숫자 스탯 맵 → 표시용 PlayerStat 목록 (STAT_KEYS 순서/포맷) — 최적화 결과 표시에도 재사용 */
  public List<PoeBuild.PlayerStat> formatStats(Map<String, Double> values) {
    return toStats(values);
  }

  private List<PoeBuild.PlayerStat> toStats(Map<String, Double> values) {
    List<PoeBuild.PlayerStat> stats = new ArrayList<>();
    for (String key : STAT_KEYS) {
      Double value = values.get(key);
      if (value == null) {
        continue;
      }
      if ("FullDPS".equals(key) && value == 0) {
        continue; // 풀 DPS 미설정 빌드는 0 이라 표시하지 않는다
      }
      stats.add(new PoeBuild.PlayerStat(key.toLowerCase(Locale.ROOT), format(value)));
    }
    return stats;
  }

  private String format(double value) {
    if (Math.abs(value) >= 100) {
      return String.format(Locale.ROOT, "%,.0f", value);
    }
    return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
  }

  private String sha256(String value) {
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of()
          .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
