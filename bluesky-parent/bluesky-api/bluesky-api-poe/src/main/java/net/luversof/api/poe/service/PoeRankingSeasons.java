package net.luversof.api.poe.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 젬 DPS 랭킹 시즌 보관(2026-10-02 사용자 요청: "시즌이 바뀔 때마다 이전 시즌과 비교해서 랭킹 변화를 보고 싶어", PoE1·PoE2 둘 다).
 *
 * <p>랭킹 파일(gem-ranking*.json)은 배치마다 덮어쓴다 — 그래서 끝날 때마다 같은 내용을 {@code <dir>/<시즌>.json} 에도 남긴다. 시즌 =
 * 패치의 앞 두 마디(PoE1 3.29.3.2 → 3.29, PoE2 4.5.5.4 → 4.5): 같은 시즌의 작은 패치는 덮어쓰고(마지막 측정이 그 시즌 값), 시즌이 바뀌면
 * 새 파일이 생겨 이전 시즌이 그대로 남는다. 내용(JSON)은 해석하지 않는다 — 각 서비스의 RankingData 를 그대로 쓴다.
 */
public final class PoeRankingSeasons {

  private static final Logger logger = LoggerFactory.getLogger(PoeRankingSeasons.class);

  private final Path dir;

  public PoeRankingSeasons(Path dir) {
    this.dir = dir;
  }

  /** 패치 → 시즌 키(앞 두 마디). 비었거나 숫자 마디가 없으면 null. */
  public static String seasonOf(String patch) {
    if (patch == null || patch.isBlank()) {
      return null;
    }
    String[] parts = patch.trim().split("\\.");
    if (parts.length < 2 || !parts[0].matches("\\d+") || !parts[1].matches("\\d+")) {
      return null;
    }
    return parts[0] + "." + parts[1];
  }

  /** 시즌 키를 버전 순(새 것 먼저)으로 — "3.29" 가 "3.3" 보다 새 것(문자열 순이 아니라 숫자 순). */
  static final Comparator<String> NEWEST_FIRST =
      Comparator.<String>comparingInt(s -> Integer.parseInt(s.split("\\.")[0]))
          .thenComparingInt(s -> Integer.parseInt(s.split("\\.")[1]))
          .reversed();

  /** 이 패치의 시즌 파일로 남긴다(같은 시즌이면 덮어쓴다). 실패해도 본 랭킹 저장은 그대로 — 경고만. */
  public void archive(String patch, String json) {
    String season = seasonOf(patch);
    if (season == null || json == null) {
      return;
    }
    try {
      Files.createDirectories(dir);
      Files.writeString(dir.resolve(season + ".json"), json, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.warn("젬 랭킹 시즌 보관 실패: {} {}", dir, season, e);
    }
  }

  /** 기존 랭킹 파일이 있는데 그 시즌 보관본이 없으면 만든다(기능을 넣기 전에 돌린 랭킹도 첫 시즌으로 남게). */
  public void seedFrom(Path rankingFile, String patch) {
    String season = seasonOf(patch);
    if (season == null
        || !Files.exists(rankingFile)
        || Files.exists(dir.resolve(season + ".json"))) {
      return;
    }
    try {
      archive(patch, Files.readString(rankingFile, StandardCharsets.UTF_8));
      logger.info("젬 랭킹 시즌 보관 시작: {} → {}/{}.json", rankingFile, dir, season);
    } catch (IOException e) {
      logger.warn("젬 랭킹 시즌 보관 시작 실패: {}", rankingFile, e);
    }
  }

  /** 보관된 시즌들(새 것 먼저). */
  public List<String> seasons() {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    try (Stream<Path> files = Files.list(dir)) {
      files
          .map(p -> p.getFileName().toString())
          .filter(n -> n.matches("\\d+\\.\\d+\\.json"))
          .map(n -> n.substring(0, n.length() - ".json".length()))
          .forEach(out::add);
    } catch (IOException e) {
      logger.warn("젬 랭킹 시즌 목록 실패: {}", dir, e);
    }
    out.sort(NEWEST_FIRST);
    return out;
  }

  /** 그 시즌 보관본(JSON 원문). 없거나 키가 이상하면 빈 값. */
  public Optional<String> read(String season) {
    if (season == null || !season.matches("\\d+\\.\\d+")) {
      return Optional.empty();
    }
    Path file = dir.resolve(season + ".json");
    try {
      return Files.exists(file)
          ? Optional.of(Files.readString(file, StandardCharsets.UTF_8))
          : Optional.empty();
    } catch (IOException e) {
      logger.warn("젬 랭킹 시즌 읽기 실패: {}", file, e);
      return Optional.empty();
    }
  }
}
