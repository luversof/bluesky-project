package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 제작 계산기 단가(10-15 C336) — tools/poe-extract/fetch-ninja-prices.mjs 가 받은 poe.ninja 거래소 시세 {@code
 * ninja/craft-prices.json}(카오스 기준). 파일이 바뀌면 다시 읽는다. 시세가 없으면 null(계산기는 시도 수만 보여 준다).
 */
@Service
public class PoeCraftPriceService {

  private static final Logger logger = LoggerFactory.getLogger(PoeCraftPriceService.class);

  /** 시세 파일 — chaos = 이름 → 카오스 값. */
  public record Prices(String league, String fetchedAt, Map<String, Double> chaos) {}

  private final Path file;
  private volatile Prices prices = new Prices(null, null, Map.of());
  private volatile FileTime loadedAt;

  public PoeCraftPriceService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.file = Path.of(dataDir, "ninja", "craft-prices.json");
  }

  public Prices prices() {
    try {
      FileTime t = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
      if (t != null && !t.equals(loadedAt)) {
        synchronized (this) {
          try (InputStream in = Files.newInputStream(file)) {
            Prices p = JsonMapper.builder().build().readValue(in, Prices.class);
            prices = p.chaos() == null ? new Prices(p.league(), p.fetchedAt(), Map.of()) : p;
            loadedAt = t;
            logger.info("PoE 제작 시세 로드: {} ({}종, {})", file, prices.chaos().size(), prices.league());
          }
        }
      }
    } catch (Exception e) {
      logger.warn("PoE 제작 시세 로드 실패: {}", file, e);
    }
    return prices;
  }

  /**
   * 짐승 제작 레시피 → 필요한 짐승(C457, 게임 데이터 BestiaryRecipes · BestiaryRecipeComponent — 레시피 시세는 없고 짐승 시세는
   * poe.ninja 짐승 개요에 있다). 레시피 단가 = 짐승 시세 합(하나라도 모르면 null).
   */
  static final Map<String, List<String>> BEAST_OF_RECIPE =
      Map.ofEntries(
          Map.entry("Bestiary: Add a Suffix, Remove a Random Prefix", List.of("Farric Lynx Alpha")),
          Map.entry("Bestiary: Add a Prefix, Remove a Random Suffix", List.of("Farric Wolf Alpha")),
          Map.entry(
              "Bestiary: Add a Mod to a Shaper Item", List.of("Fenumal Devourer", "Craicic Maw")),
          Map.entry(
              "Bestiary: Add a Mod to an Elder Item",
              List.of("Saqawine Blood Viper", "Craicic Maw")),
          Map.entry(
              "Bestiary: Add a Mod to a Redeemer Item", List.of("Fenumal Queen", "Craicic Maw")),
          Map.entry(
              "Bestiary: Add a Mod to a Hunter Item", List.of("Craicic Watcher", "Craicic Maw")),
          Map.entry(
              "Bestiary: Add a Mod to a Crusader Item", List.of("Farric Goliath", "Craicic Maw")),
          Map.entry(
              "Bestiary: Add a Mod to a Warlord Item", List.of("Fenumal Scrabbler", "Craicic Maw")),
          Map.entry(
              "Bestiary: Level 30 Aspect of the Cat skill",
              List.of("Farrul, First of the Plains", "Black Mórrigan")),
          Map.entry(
              "Bestiary: Level 30 Aspect of the Avian skill",
              List.of("Saqawal, First of the Sky", "Black Mórrigan")),
          Map.entry(
              "Bestiary: Level 30 Aspect of the Spider skill",
              List.of("Fenumus, First of the Night", "Black Mórrigan")),
          Map.entry(
              "Bestiary: Level 30 Aspect of the Crab skill",
              List.of("Craiceann, First of the Deep", "Black Mórrigan")),
          // 각인(EinharMasterCraft27 "Of a Magic Item")
          Map.entry("Bestiary: Imprint", List.of("Craicic Croaker")));

  /** 이름 → 카오스 값. 모르면 null. 짐승 제작 레시피는 필요한 짐승 시세 합(C457). */
  public Double chaos(String name) {
    if (name == null) {
      return null;
    }
    Map<String, Double> c = prices().chaos();
    Double v = c.get(name);
    List<String> beasts = v == null ? BEAST_OF_RECIPE.get(name) : null;
    if (beasts != null) {
      double sum = 0;
      for (String b : beasts) {
        Double bv = c.get(b);
        if (bv == null) {
          return null;
        }
        sum += bv;
      }
      return sum;
    }
    return v;
  }
}
