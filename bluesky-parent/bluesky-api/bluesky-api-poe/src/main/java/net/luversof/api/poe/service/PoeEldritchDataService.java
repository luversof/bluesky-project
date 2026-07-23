package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 엘드리치 임플리싯(총주교/포식자) 풀 — tools/poe-extract/parse-eldritch.mjs 가 만든 {@code
 * ~/.poe-gamedata/eldritch-implicits.json}. 특정 방어/장신구 슬롯에 화폐로 부여하는 임플리싯을 아이템 클래스별로 팩션(exarch=총주교,
 * eater=포식자) × 계열(티어 사다리)로 제공한다.
 */
@Service
public class PoeEldritchDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeEldritchDataService.class);

  /** 티어 한 줄 — tier(클수록 강함) + 최대롤 문장(en/ko). */
  public record EldritchTier(int tier, List<String> en, List<String> ko) {}

  /** 한 계열 = 티어 사다리(강→약). */
  public record EldritchFamily(String key, List<EldritchTier> tiers) {}

  /** 팩션 이름. */
  public record Faction(String name, String nameKo) {}

  private record SlotPools(List<EldritchFamily> exarch, List<EldritchFamily> eater) {}

  private record EldritchData(
      String patch, Map<String, Faction> factions, Map<String, SlotPools> bySlot) {}

  /** 한 아이템 클래스의 엘드리치 풀 — 팩션 정보 + 총주교/포식자 계열. */
  public record ClassEldritch(
      String itemClass,
      Faction exarchFaction,
      Faction eaterFaction,
      List<EldritchFamily> exarch,
      List<EldritchFamily> eater) {}

  private final Path dataFile;
  private volatile EldritchData data = new EldritchData("", Map.of(), Map.of());

  public PoeEldritchDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "eldritch-implicits.json");
    reload();
  }

  public synchronized void reload() {
    EldritchData loaded = new EldritchData("", Map.of(), Map.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, EldritchData.class);
        logger.info("PoE 엘드리치 임플리싯 로드: {} (슬롯 {}종)", dataFile, loaded.bySlot().size());
      } catch (Exception e) {
        logger.warn("PoE 엘드리치 임플리싯 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 엘드리치 임플리싯 없음: {} — parse-eldritch.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return !data.bySlot().isEmpty();
  }

  /** 이 아이템 클래스가 엘드리치 임플리싯을 받을 수 있는지. */
  public boolean supports(String itemClass) {
    return data.bySlot().containsKey(itemClass);
  }

  /** 아이템 클래스의 엘드리치 풀 — 없으면 null. */
  public ClassEldritch forItemClass(String itemClass) {
    SlotPools pools = data.bySlot().get(itemClass);
    if (pools == null) {
      return null;
    }
    return new ClassEldritch(
        itemClass,
        data.factions().get("exarch"),
        data.factions().get("eater"),
        pools.exarch(),
        pools.eater());
  }
}
