package net.luversof.api.poe.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 스킬별 사용 가능한 무기 종류(tools/poe-extract parse-skill-weapons.mjs 산출 skill-weapons.json).
 *
 * <p>젬 태그로는 알 수 없다 — 예: 마력 착취(Power Siphon)의 태그는 [Critical, Attack, Projectile] 뿐이지만 실제로는 <b>완드
 * 전용</b>이다. 이걸 모르면 최적화기가 완드 전용 스킬에 도끼를 쥐여주고, PoB 는 스킬을 못 쓰는 것으로 처리해 수치가 무너진다.
 */
@Service
public class PoeSkillWeaponDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeSkillWeaponDataService.class);

  /** PoB 무기 종류 → 유니크 카테고리(우리 데이터 기준). 셉터는 category 가 mace 다. */
  private static final Map<String, String> CATEGORY_BY_TYPE =
      Map.ofEntries(
          Map.entry("Wand", "wand"),
          Map.entry("Bow", "bow"),
          Map.entry("Staff", "staff"),
          Map.entry("Warstaff", "staff"),
          Map.entry("Claw", "claw"),
          Map.entry("Dagger", "dagger"),
          Map.entry("Rune Dagger", "dagger"),
          Map.entry("One Handed Sword", "sword"),
          Map.entry("Two Handed Sword", "sword"),
          Map.entry("Thrusting One Handed Sword", "sword"),
          Map.entry("One Handed Axe", "axe"),
          Map.entry("Two Handed Axe", "axe"),
          Map.entry("One Handed Mace", "mace"),
          Map.entry("Two Handed Mace", "mace"),
          Map.entry("Sceptre", "mace"));

  /** PoB 무기 종류 → 베이스 아이템의 itemClass(크래프트 무기 후보용). 표기가 미묘하게 다르다. */
  private static final Map<String, String> ITEM_CLASS_BY_TYPE =
      Map.ofEntries(
          Map.entry("Wand", "Wand"),
          Map.entry("Bow", "Bow"),
          Map.entry("Staff", "Staff"),
          Map.entry("Warstaff", "Warstaff"),
          Map.entry("Claw", "Claw"),
          Map.entry("Dagger", "Dagger"),
          Map.entry("Rune Dagger", "Rune Dagger"),
          Map.entry("One Handed Sword", "One Hand Sword"),
          Map.entry("Two Handed Sword", "Two Hand Sword"),
          Map.entry("Thrusting One Handed Sword", "Thrusting One Hand Sword"),
          Map.entry("One Handed Axe", "One Hand Axe"),
          Map.entry("Two Handed Axe", "Two Hand Axe"),
          Map.entry("One Handed Mace", "One Hand Mace"),
          Map.entry("Two Handed Mace", "Two Hand Mace"),
          Map.entry("Sceptre", "Sceptre"));

  private volatile Map<String, List<String>> bySkill = Map.of();

  /** 방패가 있어야 쓸 수 있는 스킬(방패 강타·방패 돌진 등) — 없으면 PoB 가 스킬을 비활성 처리한다. */
  private volatile Set<String> shieldSkills = Set.of();

  /** 쌍수(양손 무기)여야 쓸 수 있는 스킬(듀얼 스트라이크 등) — 오프핸드 무기가 없으면 PoB 가 스킬을 비활성 처리한다. */
  private volatile Set<String> dualWieldSkills = Set.of();

  public PoeSkillWeaponDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    Path file = Path.of(dataDir, "skill-weapons.json");
    if (!Files.isReadable(file)) {
      logger.info("스킬 무기 제한 데이터 없음 — 무기 후보는 태그 추정으로 동작: {}", file);
      return;
    }
    try {
      var full = JsonMapper.builder().build().readTree(Files.readString(file));
      // 형식: { weapons: {스킬:[무기종류]}, requiresShield: [스킬] }
      var root = full.has("weapons") ? full.path("weapons") : full;
      var shields = new java.util.LinkedHashSet<String>();
      full.path("requiresShield").forEach(n -> shields.add(n.asText()));
      this.shieldSkills = Set.copyOf(shields);
      var duals = new java.util.LinkedHashSet<String>();
      full.path("requiresDualWield").forEach(n -> duals.add(n.asText()));
      this.dualWieldSkills = Set.copyOf(duals);
      var parsed = new java.util.LinkedHashMap<String, List<String>>();
      root.fieldNames()
          .forEachRemaining(
              name -> {
                var types = new java.util.ArrayList<String>();
                root.path(name).forEach(t -> types.add(t.asText()));
                parsed.put(name, List.copyOf(types));
              });
      this.bySkill = Map.copyOf(parsed);
      logger.info("스킬 무기 제한 {}종 · 방패 필요 {}종 로드", bySkill.size(), shieldSkills.size());
    } catch (Exception e) {
      logger.warn("스킬 무기 제한 로드 실패: {}", e.toString());
    }
  }

  /** 이 스킬이 쓸 수 있는 유니크 카테고리(제한이 없으면 빈 목록 → 호출부가 기존 추정을 쓴다). */
  public List<String> categories(String skillName) {
    return bySkill.getOrDefault(skillName, List.of()).stream()
        .map(CATEGORY_BY_TYPE::get)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
  }

  /** 이 스킬이 쓸 수 있는 베이스 itemClass(제한 없으면 빈 목록). */
  public List<String> itemClasses(String skillName) {
    return bySkill.getOrDefault(skillName, List.of()).stream()
        .map(ITEM_CLASS_BY_TYPE::get)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
  }

  /** 셉터를 쓸 수 있는 스킬인지 — 유니크 category 로는 셉터를 구분할 수 없어 베이스 이름으로 따로 판정한다. */
  public boolean allowsSceptre(String skillName) {
    return Set.copyOf(bySkill.getOrDefault(skillName, List.of())).contains("Sceptre");
  }

  /**
   * 무기를 <b>들면 안 되는</b> 스킬인지 — 제한 목록이 {@code ["None"]} 뿐인 경우(독성 혼합물 등 맨손 전용).
   *
   * <p>표준 무기를 쥐여 주면 PoB 가 스킬을 비활성 처리해 수치가 통째로 0 이 된다(실측).
   */
  public boolean requiresUnarmed(String skillName) {
    List<String> types = bySkill.getOrDefault(skillName, List.of());
    return !types.isEmpty() && types.stream().noneMatch(CATEGORY_BY_TYPE::containsKey);
  }

  /** 방패가 있어야 동작하는 스킬인지 — 없으면 PoB 가 스킬을 비활성 처리해 수치가 0 이 된다. */
  public boolean requiresShield(String skillName) {
    return shieldSkills.contains(skillName);
  }

  /** 쌍수(오프핸드 무기)여야 동작하는 스킬인지 — 없으면 PoB 가 스킬을 비활성 처리해 수치가 0 이 된다. */
  public boolean requiresDualWield(String skillName) {
    return dualWieldSkills.contains(skillName);
  }

  public boolean hasData() {
    return !bySkill.isEmpty();
  }
}
